package com.quzzar.kithkyn.dev;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.PersonEntityType;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.village.buildings.WorkerFooting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Opt-in physical walking regression. Use -Dkithkyn.lowPassage.verify=true in a disposable world. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class LowPassageVerification {
  private static final BlockPos START = new BlockPos(-7500, 151, -7500);
  private static final BlockPos END = START.east(10);
  private static TallWalker walker;
  private static int ticks;
  private static int arrivedAt;

  private LowPassageVerification() { }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (!Boolean.getBoolean("kithkyn.lowPassage.verify")) return;
    ticks++;
    ServerLevel level = event.getServer().overworld();
    try {
      if (ticks == 1) {
        for (int x = -470; x <= -467; x++) for (int z = -470; z <= -467; z++) {
          level.setChunkForced(x, z, true);
        }
        event.getServer().tickRateManager().setTickRate(100);
      }
      if (ticks == 40) start(level);
      if (walker == null) return;
      if (walker.position().distanceToSqr(net.minecraft.world.phys.Vec3.atBottomCenterOf(END)) < 0.4D) {
        if (arrivedAt == 0) arrivedAt = ticks;
        if (ticks - arrivedAt > 30) {
          check(walker.getPose() == Pose.STANDING, "walker did not stand after leaving the passage");
          verifyStoppedUnderRoof(level);
          Kithkyn.LOGGER.info("[low-passage-verify] RESULT PASS: tall resident walked through a two-high opening, stood outside and stayed ducked beneath a roof");
          walker.discard();
          event.getServer().halt(false);
        }
      }
      if (ticks > 1200) throw new AssertionError("Physical route stalled at " + walker.position());
    } catch (Exception | AssertionError failure) {
      Kithkyn.LOGGER.error("[low-passage-verify] RESULT FAIL", failure);
      event.getServer().halt(false);
    }
  }

  private static void start(ServerLevel level) {
    level.setDayTime(6000);
    level.updateSkyBrightness();
    for (BlockPos pos : BlockPos.betweenClosed(START.offset(-2, -1, -5), END.offset(2, 5, 5))) {
      int x = pos.getX() - START.getX();
      boolean floor = pos.getY() == START.getY() - 1;
      boolean wall = x >= 3 && x <= 6;
      boolean tunnel = pos.getZ() == START.getZ()
          && pos.getY() >= START.getY() && pos.getY() < START.getY() + 2;
      level.setBlock(pos, floor || (wall && !tunnel)
          ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
    }
    walker = new TallWalker(level);
    walker.refreshDimensions();
    walker.setPos(START.getX() + 0.5D, START.getY(), START.getZ() + 0.5D);
    walker.setOnGround(true);
    level.addFreshEntity(walker);
    check(walker.getBbHeight() > 2.0F, "fixture was not taller than the doorway");
    var standing = new GroundPathNavigation(walker, level).createPath(END, 0);
    check(standing == null || !standing.canReach(), "standing control unexpectedly crossed the wall");
    check(WorkerFooting.canStand(walker, START.east(4)), "low work footing was rejected before navigation");
    var ducked = walker.getNavigation().createPath(END, 0);
    check(ducked != null && ducked.canReach() && walker.getPose() == Pose.CROUCHING,
        "navigator did not choose a reachable ducking route");
    check(walker.getNavigation().moveTo(ducked, 0.5D), "route did not start");
  }

  private static void verifyStoppedUnderRoof(ServerLevel level) {
    walker.setPos(START.getX() + 4.5D, START.getY(), START.getZ() + 0.5D);
    walker.setOnGround(true);
    walker.setPose(Pose.STANDING);
    var exit = walker.getNavigation().createPath(END, 0);
    check(exit != null && exit.canReach(), "could not plan an exit from beneath the roof");
    walker.getNavigation().stop();
    walker.tickCount += 40;
    walker.getNavigation().tick();
    check(walker.getPose() == Pose.CROUCHING && level.noCollision(walker),
        "stopping stood the worker inside the ceiling");
  }

  private static void check(boolean condition, String reason) {
    if (!condition) throw new AssertionError(reason);
  }

  private static final class TallWalker extends RealPerson {
    private TallWalker(ServerLevel level) {
      super(PersonEntityType.PERSON.get(), level);
      reloadState();
    }

    @Override public float getScale() { return 1.03F; }

    @Override public void reloadState() {
      super.reloadState();
      this.goalSelector.removeAllGoals(goal -> true);
      this.targetSelector.removeAllGoals(goal -> true);
    }
  }
}
