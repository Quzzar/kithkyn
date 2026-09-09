package com.quzzar.kithkyn.dev;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.PersonEntityType;
import com.quzzar.kithkyn.entities.AgeStage;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.village.buildings.WorkerFooting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Opt-in physical walking regression. Use -Dkithkyn.lowPassage.verify=true in a disposable world. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class LowPassageVerification {
  private static final BlockPos START = new BlockPos(-7500, 151, -7500);
  private static final BlockPos END = START.east(10);
  private static VerificationWalker walker;
  private static int ticks;
  private static int stageIndex;
  private static boolean stoppedUnderRoof;

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
      if (ticks == 40) {
        verifyDimensions(level);
        start(level);
      }
      if (walker == null) return;
      check(walker.getPose() == Pose.STANDING, "navigation changed the standing pose");
      check(level.noCollision(walker), "walker collided with the passage ceiling");
      if (!stoppedUnderRoof && walker.getX() >= START.getX() + 4.5D) {
        check(walker.getX() < START.getX() + 6.0D, "missed the roof stopping check");
        walker.getNavigation().stop();
        walker.getNavigation().tick();
        check(walker.getPose() == Pose.STANDING && level.noCollision(walker),
            "stopping under the ceiling changed posture or collided");
        var exit = walker.getNavigation().createPath(END, 0);
        check(exit != null && exit.canReach(), "could not plan an exit beneath the roof");
        check(walker.getNavigation().moveTo(exit, 0.5D), "exit route did not start");
        stoppedUnderRoof = true;
      }
      if (walker.position().distanceToSqr(Vec3.atBottomCenterOf(END)) < 0.4D) {
        check(stoppedUnderRoof, "walk did not cross the passage");
        Kithkyn.LOGGER.info("[low-passage-verify] {} walked through a {}-block opening standing",
            walker.getLifeStage(), walker.getLifeStage().collisionHeightLimit());
        walker.discard();
        walker = null;
        stageIndex++;
        if (stageIndex < AgeStage.values().length) {
          start(level);
        } else {
          Kithkyn.LOGGER.info("[low-passage-verify] RESULT PASS: all ages crossed their capped opening; shorter openings rejected; natural widths and visual attachments preserved across scales");
          event.getServer().halt(false);
        }
      }
      if (ticks > 1600) throw new AssertionError("Physical route stalled at " + walker.position());
    } catch (Exception | AssertionError failure) {
      Kithkyn.LOGGER.error("[low-passage-verify] RESULT FAIL", failure);
      event.getServer().halt(false);
    }
  }

  /** Exercise actual LivingEntity scaling, including division/multiplication rounding at the cap. */
  private static void verifyDimensions(ServerLevel level) {
    VerificationWalker sample = new VerificationWalker(level);
    sample.setPos(Vec3.atBottomCenterOf(START));
    for (AgeStage stage : AgeStage.values()) {
      sample.setLifeStage(stage);
      for (int percent = 65; percent <= 150; percent++) {
        sample.scale = percent / 100.0F;
        for (Pose pose : new Pose[] {Pose.STANDING, Pose.CROUCHING, Pose.SWIMMING}) {
          float baseHeight = pose == Pose.STANDING ? 1.95F : pose == Pose.CROUCHING ? 1.75F : 0.6F;
          EntityDimensions natural = EntityDimensions.scalable(0.6F, baseHeight)
              .scale(stage.dimensionsScale()).scale(sample.getScale());
          EntityDimensions actual = sample.getDimensions(pose);
          float expectedHeight = Math.min(natural.height(), stage.collisionHeightLimit());
          check(actual.height() <= stage.collisionHeightLimit(), "scaled height exceeded " + stage);
          check(Math.abs(actual.height() - expectedHeight) < 0.000001F, "smaller body was enlarged");
          check(actual.width() == natural.width(), "collision width changed");
          check(actual.eyeHeight() < actual.height(), "eyes exceeded the collision ceiling");
          check(actual.attachments().get(EntityAttachment.NAME_TAG, 0, 0)
              .equals(natural.attachments().get(EntityAttachment.NAME_TAG, 0, 0)),
              "visual nameplate anchor moved with the capped body");
        }
        check(sample.getDimensions(Pose.SLEEPING).height() == 0.2F, "sleeping body changed");
      }
    }
    sample.discard();
  }

  private static void start(ServerLevel level) {
    level.setDayTime(6000);
    level.updateSkyBrightness();
    AgeStage stage = AgeStage.values()[stageIndex];
    int openingHeight = (int)stage.collisionHeightLimit();
    for (BlockPos pos : BlockPos.betweenClosed(START.offset(-2, -1, -5), END.offset(2, 5, 5))) {
      int x = pos.getX() - START.getX();
      boolean floor = pos.getY() == START.getY() - 1;
      boolean wall = x >= 3 && x <= 6;
      boolean tunnel = pos.getZ() == START.getZ()
          && pos.getY() >= START.getY() && pos.getY() < START.getY() + openingHeight;
      level.setBlock(pos, floor || (wall && !tunnel)
          ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
    }
    walker = new VerificationWalker(level);
    walker.scale = 1.28F;
    walker.setLifeStage(stage);
    walker.refreshDimensions();
    walker.setPos(Vec3.atBottomCenterOf(START));
    walker.setOnGround(true);
    level.addFreshEntity(walker);
    stoppedUnderRoof = false;
    check(Math.abs(walker.getBbHeight() - openingHeight) < 0.000001F,
        "fixture did not exercise the collision cap");
    check(WorkerFooting.canStand(walker, START.east(4)), "low work footing was rejected");

    // Reject a passage one block too short before opening the intended route.
    BlockPos ceiling = START.east(4).above(openingHeight - 1);
    level.setBlock(ceiling, Blocks.STONE.defaultBlockState(), 2);
    check(!WorkerFooting.canStand(walker, START.east(4)), "solid ceiling passed footing checks");
    var blocked = walker.getNavigation().createPath(END, 0);
    check(blocked == null || !blocked.canReach(), "navigator routed through a solid ceiling");
    level.setBlock(ceiling, Blocks.AIR.defaultBlockState(), 2);
    var route = walker.getNavigation().createPath(END, 0);
    check(route != null && route.canReach(), "navigator rejected the capped standing route");
    check(walker.getNavigation().moveTo(route, 0.5D), "route did not start");
  }

  private static void check(boolean condition, String reason) {
    if (!condition) throw new AssertionError(reason);
  }

  private static final class VerificationWalker extends RealPerson {
    private float scale = 1.0F;

    private VerificationWalker(ServerLevel level) {
      super(PersonEntityType.PERSON.get(), level);
      reloadState();
    }

    @Override public float getScale() { return scale == 0.0F ? 1.0F : scale; }

    @Override public void reloadState() {
      super.reloadState();
      this.goalSelector.removeAllGoals(goal -> true);
      this.targetSelector.removeAllGoals(goal -> true);
    }
  }
}
