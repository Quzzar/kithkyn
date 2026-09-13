package com.quzzar.kithkyn.dev;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.PersonEntityType;
import com.quzzar.kithkyn.entities.RealPerson;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Opt-in physical walking regression. Use -Dkithkyn.vinePassage.verify=true in a disposable world. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class VinePassageVerification {
  private static final String TAG = "[vine-passage-verify]";
  private static final BlockPos START = new BlockPos(-7600, 151, -7600);
  private static final BlockPos VINE = START.east(4);
  private static final BlockPos END = START.east(9);
  private static VerificationWalker walker;
  private static int ticks;
  private static int startedAt;

  private VinePassageVerification() { }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (!Boolean.getBoolean("kithkyn.vinePassage.verify")) return;
    ticks++;
    ServerLevel level = event.getServer().overworld();
    try {
      if (ticks == 1) {
        level.setChunkForced(START.getX() >> 4, START.getZ() >> 4, true);
        event.getServer().tickRateManager().setTickRate(100);
      }
      if (walker == null && ticks >= 40) {
        start(level);
        startedAt = ticks;
      }
      if (walker == null) return;
      if (walker.position().distanceToSqr(Vec3.atBottomCenterOf(END)) < 0.4D) {
        Kithkyn.LOGGER.info("{} RESULT PASS: move control evaluated open footing after a path search, and the villager crossed a wall-mounted vine to {} in {} ticks",
            TAG, END.toShortString(), ticks - startedAt);
        walker.discard();
        walker = null;
        event.getServer().halt(false);
        return;
      }
      if (ticks - startedAt > 180) {
        BlockPos next = walker.getNavigation().isDone()
            ? null : walker.getNavigation().getPath().getNextNodePos();
        throw new AssertionError("villager stalled at " + walker.position()
            + " beside vine " + VINE.toShortString() + ", next=" + next);
      }
    } catch (Exception | AssertionError failure) {
      Kithkyn.LOGGER.error("{} RESULT FAIL", TAG, failure);
      event.getServer().halt(false);
    }
  }

  private static void start(ServerLevel level) {
    level.setDayTime(6000);
    level.updateSkyBrightness();
    for (BlockPos pos : BlockPos.betweenClosed(START.offset(-2, -1, -1), END.offset(2, 3, 1))) {
      boolean floor = pos.getY() == START.getY() - 1;
      boolean wall = Math.abs(pos.getZ() - START.getZ()) == 1;
      level.setBlock(pos, floor || wall ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
    }
    for (int height = 0; height < 3; height++) {
      level.setBlock(VINE.above(height),
          Blocks.VINE.defaultBlockState().setValue(VineBlock.NORTH, true), 2);
    }

    walker = new VerificationWalker(level);
    walker.setPos(Vec3.atBottomCenterOf(START));
    walker.setOnGround(true);
    level.addFreshEntity(walker);
    Path decorativeClimb = walker.getNavigation().createPath(VINE.above(2), 0);
    check(decorativeClimb == null || !decorativeClimb.canReach(),
        "planner treated decorative vines as an authored ladder");
    Path route = walker.getNavigation().createPath(END, 0);
    check(route != null && route.canReach(), "no route through the vine corridor");
    check(contains(route, VINE), "fixture route went around the vine");
    // MoveControl asks the navigation evaluator about local footing outside a
    // path search. Its temporary PathfindingContext has already been cleared.
    walker.getNavigation().getNodeEvaluator().getPathType(walker, VINE.above(2));
    check(walker.getNavigation().moveTo(route, 0.5D), "vine corridor route did not start");
  }

  private static boolean contains(Path path, BlockPos position) {
    for (int index = 0; index < path.getNodeCount(); index++) {
      if (path.getNodePos(index).equals(position)) return true;
    }
    return false;
  }

  private static void check(boolean condition, String reason) {
    if (!condition) throw new AssertionError(reason);
  }

  private static final class VerificationWalker extends RealPerson {
    private VerificationWalker(ServerLevel level) {
      super(PersonEntityType.PERSON.get(), level);
      reloadState();
    }

    @Override
    public void reloadState() {
      super.reloadState();
      this.goalSelector.removeAllGoals(goal -> true);
      this.targetSelector.removeAllGoals(goal -> true);
    }
  }
}
