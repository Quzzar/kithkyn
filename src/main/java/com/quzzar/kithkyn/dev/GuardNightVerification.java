package com.quzzar.kithkyn.dev;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.PersonEntityType;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.entities.ai.GuardNightRoutine;
import com.quzzar.kithkyn.entities.ai.goals.GuardPatrolGoal;
import com.quzzar.kithkyn.entities.ai.goals.GuardPostGoal;
import com.quzzar.kithkyn.entities.ai.goals.BedtimeWithoutBedGoal;
import com.quzzar.kithkyn.entities.ai.goals.SleepAtNightGoal;
import com.quzzar.kithkyn.entities.ai.goals.StrollAroundVillage;
import com.quzzar.kithkyn.village.BedAssignment;
import com.quzzar.kithkyn.village.GuardDuty;
import com.quzzar.kithkyn.village.JobAssignment;
import com.quzzar.kithkyn.village.LocationManager;
import com.quzzar.kithkyn.village.Occupation;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.buildings.Building;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Opt-in native routine regression. Use -Dkithkyn.guardNight.verify=true only in a disposable world. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class GuardNightVerification {
  private static int ticks;

  private GuardNightVerification() { }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (!Boolean.getBoolean("kithkyn.guardNight.verify") || ++ticks != 40) return;
    try {
      verify(event.getServer().overworld());
      Kithkyn.LOGGER.info("[guard-night-verify] RESULT PASS: installed sleep/post/patrol gates, retained sentry loadout, reload, bed pose, combat wake-up and captain sleep");
    } catch (Exception | AssertionError failure) {
      Kithkyn.LOGGER.error("[guard-night-verify] RESULT FAIL", failure);
    } finally {
      event.getServer().halt(false);
    }
  }

  private static void verify(ServerLevel level) {
    FixtureVillage village = new FixtureVillage();
    FixtureGuard guard = new FixtureGuard(level, village);
    guard.setUUID(UUID.fromString("3d362f8e-96a3-4a43-9f34-0ffbd9f3b3b7"));
    guard.setOccupation(Occupation.GUARD);
    guard.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
    guard.reloadState();
    ItemStack crossbow = guard.getMainHandItem();
    GuardDuty assignedPost = GuardDuty.of(guard);
    check(assignedPost != null && assignedPost.ranged(), "fixture did not resolve a ranged tower post");

    GuardPostGoal post = guard.goal(GuardPostGoal.class);
    GuardPatrolGoal patrol = guard.goal(GuardPatrolGoal.class);
    SleepAtNightGoal sleep = guard.goal(SleepAtNightGoal.class);
    BedtimeWithoutBedGoal restock = guard.goal(BedtimeWithoutBedGoal.class);

    clock(level, 6000L);
    check(post.canUse() && !patrol.canUse() && !sleep.canUse() && guard.isFixedRangedGuard(),
        "daytime guard did not hold their post");
    clock(level, nightFor(guard, GuardNightRoutine.POST));
    check(post.canUse() && !patrol.canUse() && !sleep.canUse() && restock.canUse(),
        "ordinary watch night did not retain the post and restock");
    clock(level, nightFor(guard, GuardNightRoutine.PATROL));
    check(!post.canContinueToUse() && patrol.canUse() && !sleep.canUse()
        && !guard.isFixedRangedGuard() && restock.canUse(), "posted guard did not take a mobile night patrol");
    check(assignedPost.equals(GuardDuty.of(guard)) && crossbow == guard.getMainHandItem(),
        "night patrol rewrote the guard's assignment or equipment");

    CompoundTag saved = new CompoundTag();
    guard.saveWithoutId(saved);
    FixtureGuard restored = new FixtureGuard(level, village);
    restored.load(saved);
    restored.reloadState();
    check(restored.guardRoutine() == GuardNightRoutine.PATROL
        && restored.goal(GuardPatrolGoal.class).canUse(), "reload rerolled patrol or omitted its goal");
    check(assignedPost.equals(GuardDuty.of(restored))
        && restored.getMainHandItem().is(Items.CROSSBOW), "reload changed sentry assignment or loadout");

    clock(level, Math.floorDiv(level.getDayTime(), 24_000L) * 24_000L + 24_000L);
    check(!patrol.canContinueToUse() && post.canUse() && guard.isFixedRangedGuard(),
        "dawn did not restore fixed watch");

    clock(level, nightFor(guard, GuardNightRoutine.SLEEP));
    check(sleep.canUse() && !post.canUse() && !patrol.canUse() && !restock.canUse(),
        "sleep choice did not release watch goals");
    check(!guard.goal(StrollAroundVillage.class).canUse(), "ambient strolling overrode guard bedtime");
    BlockPos bed = LocationManager.getBedLocation(guard);
    check(!bed.equals(BlockPos.ZERO), "guard has no assigned fixture bed");
    BlockPos head = bed.relative(Blocks.RED_BED.defaultBlockState().getValue(BedBlock.FACING));
    level.setBlock(bed.below(), Blocks.STONE.defaultBlockState(), 2);
    level.setBlock(head.below(), Blocks.STONE.defaultBlockState(), 2);
    level.setBlock(bed, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.PART, BedPart.FOOT), 2);
    level.setBlock(head, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.PART, BedPart.HEAD), 2);
    guard.setPos(bed.getX() + 0.5D, bed.getY(), bed.getZ() + 0.5D);
    // The ordinary restock path is tested separately; isolate actual sleeping here.
    guard.callToBedCoolDown = 100;
    sleep.start();
    sleep.tick();
    check(guard.isSleeping(), "guard never lay in its bed");
    guard.setTarget(EntityType.ZOMBIE.create(level));
    check(!sleep.canContinueToUse(), "combat did not interrupt guard sleep");
    sleep.stop();
    check(!guard.isSleeping(), "combat left guard in the sleeping pose");
    guard.setTarget(null);
    check(sleep.canUse(), "guard did not resume the same sleeping night after combat");

    village.captain = true;
    guard.reloadState();
    check(guard.getRoleLabel().equals("Guard Captain"), "fixture did not become captain");
    check(guard.goal(SleepAtNightGoal.class).canUse(), "captain was exempt from the 20% sleep choice");
    clock(level, nightFor(guard, GuardNightRoutine.PATROL));
    check(guard.goal(GuardPatrolGoal.class).canUse() && !guard.goal(SleepAtNightGoal.class).canUse(),
        "awake captain did not patrol");
    guard.discard();
    restored.discard();
  }

  private static long nightFor(RealPerson guard, GuardNightRoutine wanted) {
    for (long day = 0; day < 1000; day++) {
      long time = day * 24_000L + 18_000L;
      if (GuardNightRoutine.choose(guard.getUUID(), time, true, GuardDuty.isCaptain(guard),
          GuardDuty.of(guard) != null) == wanted) return time;
    }
    throw new AssertionError("No fixture night found for " + wanted);
  }

  private static void clock(ServerLevel level, long time) {
    level.setDayTime(time);
    level.updateSkyBrightness();
  }

  private static void check(boolean condition, String reason) {
    if (!condition) throw new AssertionError(reason);
  }

  private static final class FixtureGuard extends RealPerson {
    private final FixtureVillage village;

    private FixtureGuard(ServerLevel level, FixtureVillage village) {
      super(PersonEntityType.PERSON.get(), level);
      this.village = village;
      setNoAi(true);
    }

    @Override public Village getVillage() { return village; }

    private <T extends Goal> T goal(Class<T> type) {
      return goalSelector.getAvailableGoals().stream().map(wrapped -> wrapped.getGoal())
          .filter(type::isInstance).map(type::cast).findFirst().orElseThrow();
    }
  }

  private static final class FixtureVillage extends Village {
    private final Building center = new Building(new BlockPos(-7000, 150, -7000),
        "village_center_birch_forest_1", Rotation.NONE);
    private final Building tower = new Building(new BlockPos(-7025, 150, -7000),
        "watchtower_birch_forest_1", Rotation.NONE);
    private boolean captain;

    private FixtureVillage() { super("Guard routine fixture"); }

    @Override public Building getBuilding(UUID id) { return center.getUUID().equals(id) ? center : tower; }
    @Override public Building getTownCenter() { return center; }
    @Override public Collection<Building> getBuildings() { return List.of(center, tower); }
    @Override public JobAssignment getJobAssignment(UUID id) {
      return new JobAssignment(id, Occupation.GUARD, captain ? center.getUUID() : tower.getUUID(), captain ? 3 : 0);
    }
    @Override public BedAssignment getBedAssignment(UUID id) { return new BedAssignment(id, center.getUUID(), 0); }
  }
}
