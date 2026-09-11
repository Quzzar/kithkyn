package com.quzzar.kithkyn.dev;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.PersonEntityType;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.entities.ai.goals.BedtimeWithoutBedGoal;
import com.quzzar.kithkyn.entities.ai.goals.SleepAtNightGoal;
import com.quzzar.kithkyn.village.BedAssignment;
import com.quzzar.kithkyn.village.JobAssignment;
import com.quzzar.kithkyn.village.LocationManager;
import com.quzzar.kithkyn.village.Occupation;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.buildings.Building;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Opt-in native routine regression for the unhoused resident's bedtime. Use
 * -Dkithkyn.unhousedBedtime.verify=true only in a disposable world.
 */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class UnhousedBedtimeVerification {
  private static int ticks;

  private UnhousedBedtimeVerification() { }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (!Boolean.getBoolean("kithkyn.unhousedBedtime.verify") || ++ticks != 40) return;
    try {
      verify(event.getServer().overworld());
      Kithkyn.LOGGER.info("[unhoused-bedtime-verify] RESULT PASS: daytime hold, standing bedtime empties the pack"
          + " into the stores, shared cooldown and refire, unslept night still counted, a bed hands the night to sleep");
    } catch (Exception | AssertionError failure) {
      Kithkyn.LOGGER.error("[unhoused-bedtime-verify] RESULT FAIL", failure);
    } finally {
      event.getServer().halt(false);
    }
  }

  private static void verify(ServerLevel level) {
    FixtureVillage village = new FixtureVillage();
    FixturePerson camper = new FixturePerson(level, village);
    camper.setUUID(UUID.fromString("7c1d2a44-5b0e-4f3a-9c2d-1e8f6a0b4c21"));
    camper.setOccupation(Occupation.WANDERER);
    camper.reloadState();
    check(LocationManager.getNightRestLocation(camper).equals(BlockPos.ZERO), "fixture camper is housed");
    BedtimeWithoutBedGoal bedtime = camper.goal(BedtimeWithoutBedGoal.class);
    SleepAtNightGoal sleep = camper.goal(SleepAtNightGoal.class);
    camper.personMainInv.setItem(0, new ItemStack(Items.APPLE, 5));
    camper.personMainInv.setItem(7, new ItemStack(Items.OAK_LOG, 3));
    camper.setDaysSinceSleep(2);

    clock(level, 6000L);
    check(!bedtime.canUse() && !sleep.canUse(), "daytime camper turned in");
    clock(level, 18000L);
    check(!sleep.canUse(), "bedless camper tried to sleep");
    check(bedtime.canUse(), "unhoused camper had no bedtime at night");
    bedtime.start();
    check(camper.personMainInv.isEmpty(), "pack was not emptied into the stores");
    check(village.stored.stream().mapToInt(ItemStack::getCount).sum() == 8
        && village.stored.stream().anyMatch(stack -> stack.is(Items.APPLE) && stack.getCount() == 5)
        && village.stored.stream().anyMatch(stack -> stack.is(Items.OAK_LOG) && stack.getCount() == 3),
        "stores did not receive the whole pack: " + village.stored);
    check(camper.callToBedCoolDown == 100, "standing bedtime did not take the shared cooldown");
    check(!bedtime.canUse(), "standing bedtime refired inside its cooldown");
    check(camper.getDaysSinceSleep() == 2, "an unhoused night was counted as slept");
    camper.callToBedCoolDown = 0;
    check(bedtime.canUse(), "standing bedtime did not refire after the cooldown");

    village.housed = true;
    check(!LocationManager.getNightRestLocation(camper).equals(BlockPos.ZERO), "fixture bed did not resolve");
    check(!bedtime.canUse() && sleep.canUse(), "a housed resident kept the standing bedtime");
    camper.discard();
  }

  private static void clock(ServerLevel level, long time) {
    level.setDayTime(time);
    level.updateSkyBrightness();
  }

  private static void check(boolean condition, String reason) {
    if (!condition) throw new AssertionError(reason);
  }

  private static final class FixturePerson extends RealPerson {
    private final FixtureVillage village;

    private FixturePerson(ServerLevel level, FixtureVillage village) {
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

  /** A village whose stores accept everything, so the fixture measures exactly what bedtime hands over. */
  private static final class FixtureVillage extends Village {
    private final Building center = new Building(new BlockPos(-7000, 150, -7000),
        "village_center_birch_forest_1", Rotation.NONE);
    private final List<ItemStack> stored = new ArrayList<>();
    private boolean housed;

    private FixtureVillage() { super("Unhoused bedtime fixture"); }

    @Override public Building getBuilding(UUID id) { return center; }
    @Override public Building getTownCenter() { return center; }
    @Override public Collection<Building> getBuildings() { return List.of(center); }
    @Override public JobAssignment getJobAssignment(UUID id) { return null; }
    @Override public BedAssignment getBedAssignment(UUID id) {
      return housed ? new BedAssignment(id, center.getUUID(), 0) : null;
    }
    @Override public ItemStack storeAwayFrom(ItemStack stack, Collection<BlockPos> excluding, BlockPos near) {
      stored.add(stack.copy());
      return ItemStack.EMPTY;
    }
  }
}
