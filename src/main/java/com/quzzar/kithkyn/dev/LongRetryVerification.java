package com.quzzar.kithkyn.dev;

import java.util.ArrayList;
import java.util.List;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.PersonEntityType;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.entities.ai.PersonPathNavigation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.Path;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Opt-in regression for what the long path retry costs a tick. Use
 * -Dkithkyn.longRetry.verify=true in a disposable flat world.
 *
 * <p>Eight people stand at the closed end of eight walled lanes that run 56
 * blocks, past the ordinary search's 48-block horizon, onto open ground. In one
 * tick each asks for twelve sealed cells beside their lane, the way a villager
 * tries the cells round a chest. Every ordinary search stops cheaply at the
 * horizon and every long retry floods the open ground to its node limit, so
 * without a budget that one tick pays for 96 long searches: the per-tick cost
 * behind the watchdog kills of 2026-09-12. The same requests at accuracy 1,
 * which never retry, give the tick's cost without them.
 *
 * <p>A ninth person's exact target is four blocks away through a wall, and the
 * only way to it is a U-shaped corridor whose far end lies 56 blocks out, like a
 * watch platform whose way in is a long detour: only a long retry finds it. It
 * must be found with the budget full, including at the start of the burst tick;
 * it must not be found once the burst has spent that tick's budget, which is
 * what shows the budget bounds the tick; and it must be found again within a
 * few re-plans, which shows the budget does not starve a long route.
 */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class LongRetryVerification {
  private static final String TAG = "[long-retry-verify]";
  private static final int LANES = 8;
  private static final int CELLS = 12;
  private static final int LANE_LENGTH = 56;
  /** West end of the lanes, which run east onto open ground. */
  private static final int BURST_X = 64;
  private static final int BURST_Z = 64;
  /** West end of the U-shaped corridor, closed on every side. */
  private static final int DETOUR_X = 64;
  private static final int DETOUR_Z = -64;
  /** Ticks after the fixture is built at which the burst comes, with the budget long refilled. */
  private static final int BURST_AT = 30;
  /** How soon after the burst the corridor target must be found again: ten seconds. */
  private static final int REFOUND_WITHIN_TICKS = 200;
  /** A work loop's re-plan cadence. */
  private static final int REPLAN_TICKS = 10;

  private static final List<RealPerson> askers = new ArrayList<>();
  private static final List<List<BlockPos>> sealedCells = new ArrayList<>();
  private static RealPerson detourWalker;
  private static BlockPos detourTarget;
  private static int surfaceY;
  private static int ticks;
  private static int builtTick;
  private static int burstTick;
  private static long baselineNanos;

  private LongRetryVerification() { }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (!Boolean.getBoolean("kithkyn.longRetry.verify")) return;
    ticks++;
    ServerLevel level = event.getServer().overworld();
    try {
      if (ticks == 1) {
        forEachFixtureChunk((x, z) -> level.setChunkForced(x, z, true));
        return;
      }
      if (builtTick == 0) {
        if (ticks >= 60 && fixtureLoaded(level)) {
          build(level);
          builtTick = ticks;
        } else if (ticks > 1200) {
          throw new AssertionError("fixture chunks never loaded");
        }
        return;
      }
      int since = ticks - builtTick;
      if (since == 10) {
        check(foundDetour(), "the corridor target was not found with the budget full");
        Kithkyn.LOGGER.info("{} corridor target, {} blocks of walking away, found at the first ask",
            TAG, 2 * LANE_LENGTH + 4);
      } else if (since == 11) {
        ask(1); // warms the search code, so the baseline and the burst compare like with like
      } else if (since == 12) {
        baselineNanos = ask(1);
        Kithkyn.LOGGER.info("{} baseline: {} requests at accuracy 1, which never retry, took {} ms",
            TAG, LANES * CELLS, baselineNanos / 1_000_000L);
      } else if (since == BURST_AT) {
        check(foundDetour(), "the first long retry of a tick did not run with the budget full");
        Kithkyn.LOGGER.info("{} burst begins", TAG);
        long burstNanos = ask(0);
        Kithkyn.LOGGER.info("{} burst ends", TAG);
        Kithkyn.LOGGER.info("{} burst: {} exact requests in one tick took {} ms, {} ms more than the baseline",
            TAG, LANES * CELLS, burstNanos / 1_000_000L, (burstNanos - baselineNanos) / 1_000_000L);
        check(!foundDetour(), "a long retry still ran after the burst, in the same tick: nothing bounds them");
        burstTick = ticks;
      } else if (burstTick > 0 && (ticks - burstTick) % REPLAN_TICKS == 0) {
        if (foundDetour()) {
          Kithkyn.LOGGER.info("{} RESULT PASS: the burst's long retries stopped once the tick's budget was spent; "
              + "the corridor target was found with the budget full, deferred once it was spent, and found again "
              + "{} ticks later", TAG, ticks - burstTick);
          event.getServer().halt(false);
        } else if (ticks - burstTick >= REFOUND_WITHIN_TICKS) {
          throw new AssertionError("the corridor target was not found again within "
              + REFOUND_WITHIN_TICKS + " ticks of the burst");
        }
      }
    } catch (Exception | AssertionError failure) {
      Kithkyn.LOGGER.error("{} RESULT FAIL", TAG, failure);
      event.getServer().halt(false);
    }
  }

  /** Every asker asks for each of their sealed cells; returns how long that took. */
  private static long ask(int accuracy) {
    long started = System.nanoTime();
    for (int lane = 0; lane < LANES; lane++) {
      RealPerson asker = askers.get(lane);
      for (BlockPos cell : sealedCells.get(lane)) {
        Path path = asker.getNavigation().createPath(cell, accuracy);
        check(path == null || !path.canReach(), "a sealed cell was reached: " + cell);
      }
    }
    return System.nanoTime() - started;
  }

  private static boolean foundDetour() {
    Path path = detourWalker.getNavigation().createPath(detourTarget, 0);
    return path != null && path.canReach() && path.getEndNode() != null
        && path.getEndNode().asBlockPos().equals(detourTarget);
  }

  private static void build(ServerLevel level) {
    surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, BURST_X, BURST_Z);
    check(level.getHeight(Heightmap.Types.MOTION_BLOCKING, DETOUR_X, DETOUR_Z) == surfaceY, "the world is not flat");
    // Lanes along every fourth row of a two-high stone block, open at the east
    // end, with a row of sealed single cells between each lane and the next.
    fill(level, BURST_X - 1, BURST_Z - 1, BURST_X + LANE_LENGTH - 1, BURST_Z + 4 * LANES - 1, true);
    for (int lane = 0; lane < LANES; lane++) {
      int z = BURST_Z + 4 * lane;
      fill(level, BURST_X, z, BURST_X + LANE_LENGTH - 1, z, false);
      List<BlockPos> cells = new ArrayList<>();
      for (int cell = 0; cell < CELLS; cell++) {
        BlockPos sealed = new BlockPos(BURST_X + 1 + 2 * cell, surfaceY, z + 2);
        level.setBlock(sealed, Blocks.AIR.defaultBlockState(), 2);
        cells.add(sealed);
      }
      sealedCells.add(cells);
      askers.add(person(level, new BlockPos(BURST_X, surfaceY, z)));
    }
    // The corridor: east from the walker, north at the far end, and back west
    // to a target four blocks from where the walker stands.
    fill(level, DETOUR_X - 1, DETOUR_Z - 1, DETOUR_X + LANE_LENGTH + 1, DETOUR_Z + 5, true);
    fill(level, DETOUR_X, DETOUR_Z, DETOUR_X + LANE_LENGTH, DETOUR_Z, false);
    fill(level, DETOUR_X + LANE_LENGTH, DETOUR_Z, DETOUR_X + LANE_LENGTH, DETOUR_Z + 4, false);
    fill(level, DETOUR_X, DETOUR_Z + 4, DETOUR_X + LANE_LENGTH, DETOUR_Z + 4, false);
    detourTarget = new BlockPos(DETOUR_X, surfaceY, DETOUR_Z + 4);
    detourWalker = person(level, new BlockPos(DETOUR_X, surfaceY, DETOUR_Z));
    check(PersonPathNavigation.searchRange(detourWalker) < LANE_LENGTH,
        "the corridor's far end is within one ordinary search");
  }

  /** Stone or air two blocks high over a rectangle of ground. */
  private static void fill(ServerLevel level, int minX, int minZ, int maxX, int maxZ, boolean stone) {
    BlockState state = stone ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState();
    for (BlockPos pos : BlockPos.betweenClosed(minX, surfaceY, minZ, maxX, surfaceY + 1, maxZ)) {
      level.setBlock(pos, state, 2);
    }
  }

  /** A person standing at {@code feet}, never added to the level, so no goal of theirs moves them. */
  private static RealPerson person(ServerLevel level, BlockPos feet) {
    RealPerson person = PersonEntityType.PERSON.get().create(level);
    check(person != null, "no person entity");
    person.moveTo(feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D, 0.0F, 0.0F);
    person.setOnGround(true);
    return person;
  }

  private static boolean fixtureLoaded(ServerLevel level) {
    boolean[] loaded = {true};
    forEachFixtureChunk((x, z) -> loaded[0] &= level.getChunkSource().hasChunk(x, z));
    return loaded[0];
  }

  /** The chunks under the lanes, the ground a long retry floods past them, and the corridor. */
  private static void forEachFixtureChunk(ChunkVisitor visitor) {
    for (int x = 3; x <= 11; x++) for (int z = 0; z <= 9; z++) visitor.visit(x, z);
    for (int x = 3; x <= 8; x++) for (int z = -5; z <= -4; z++) visitor.visit(x, z);
  }

  @FunctionalInterface
  private interface ChunkVisitor {
    void visit(int x, int z);
  }

  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
}
