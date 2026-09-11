package com.quzzar.kithkyn.dev;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.PersonEntityType;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.entities.StashOffer;
import com.quzzar.kithkyn.entities.ai.goals.StashAtHomeGoal;
import com.quzzar.kithkyn.village.BedAssignment;
import com.quzzar.kithkyn.village.JobAssignment;
import com.quzzar.kithkyn.village.Occupation;
import com.quzzar.kithkyn.village.PersonalChest;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.buildings.Building;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Opt-in native routine regression for the two-way bedtime chest visit. Use
 * -Dkithkyn.chestTakeOut.verify=true only in a disposable world.
 */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class ChestTakeOutVerification {
  private static int ticks;

  private ChestTakeOutVerification() { }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (!Boolean.getBoolean("kithkyn.chestTakeOut.verify") || ++ticks != 40) return;
    try {
      verify(event.getServer().overworld());
      Kithkyn.LOGGER.info("[chest-take-out-verify] RESULT PASS: option list shape, question raised by a stocked chest"
          + " alone, keep and take out in one visit, take out with nothing carried, a vanished chest closes the visit");
    } catch (Exception | AssertionError failure) {
      Kithkyn.LOGGER.error("[chest-take-out-verify] RESULT FAIL", failure);
    } finally {
      event.getServer().halt(false);
    }
  }

  private static void verify(ServerLevel level) throws ReflectiveOperationException {
    // The question's options: each non-kit kind carried, then each kind in the chest.
    Map<Item, Integer> carried = new LinkedHashMap<>();
    carried.put(Items.OAK_LOG, 3);
    carried.put(Items.STONE_PICKAXE, 1);
    Map<Item, Integer> stored = new LinkedHashMap<>();
    stored.put(Items.APPLE, 6);
    List<StashOffer.Pick> picks = StashOffer.picks(carried, stored);
    check(picks.size() == 2 && !picks.get(0).fromChest() && picks.get(0).item() == Items.OAK_LOG
        && picks.get(1).fromChest() && picks.get(1).item() == Items.APPLE,
        "options are not carried-then-chest with the kit left out: " + picks);
    check(picks.get(0).option().equals("Hold back the 3 oak log from the village stores")
        && picks.get(1).option().equals("Take the 6 apple out of your chest to carry"), "option wording: " + picks);

    FixtureVillage village = new FixtureVillage();
    FixturePerson sleeper = new FixturePerson(level, village);
    sleeper.setUUID(UUID.fromString("9a4f1c02-6d3b-4e8a-b57c-2f0e9d1a6c43"));
    sleeper.setOccupation(Occupation.WANDERER);
    sleeper.reloadState();
    BlockPos chestPos = PersonalChest.of(sleeper);
    check(chestPos != null, "fixture bed resolved no chest of its own");
    level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
    Container chest = PersonalChest.container(sleeper, chestPos);
    check(chest != null, "placed chest is not readable at " + chestPos);
    sleeper.setPos(chestPos.getX() + 0.5D, chestPos.getY(), chestPos.getZ() + 2.5D);
    clock(level, 18000L);

    // Nothing carried beside an empty chest raises no question; a stocked chest does, with nothing carried.
    Field offerDay = RealPerson.class.getDeclaredField("stashOfferDay");
    offerDay.setAccessible(true);
    sleeper.goToBed(0.5D);
    check(offerDay.getLong(sleeper) == -1L, "question went out with nothing to keep or take");
    chest.setItem(0, new ItemStack(Items.APPLE, 6));
    sleeper.callToBedCoolDown = 0;
    sleeper.goToBed(0.5D);
    check(offerDay.getLong(sleeper) == 0L, "a stocked chest raised no question for an empty pack");

    // Keep and take out in one visit: the logs go in, the apples come out.
    sleeper.personMainInv.setItem(0, new ItemStack(Items.OAK_LOG, 3));
    settle(sleeper, Set.of(Items.OAK_LOG), Set.of(Items.APPLE));
    StashAtHomeGoal stash = sleeper.goal(StashAtHomeGoal.class);
    check(stash.canUse(), "the chest visit did not start");
    stash.start();
    stash.tick();
    check(sleeper.personMainInv.countItem(Items.APPLE) == 6 && sleeper.personMainInv.countItem(Items.OAK_LOG) == 0,
        "pack after the visit: " + PersonalChest.summarize(sleeper.personMainInv));
    check(chest.countItem(Items.OAK_LOG) == 3 && chest.countItem(Items.APPLE) == 0,
        "chest after the visit: " + PersonalChest.summarize(chest));
    check(sleeper.keepingForHome().isEmpty() && sleeper.takingFromHome().isEmpty() && !stash.canUse(),
        "the visit did not close out");

    // Taking out with nothing carried still makes the trip.
    sleeper.personMainInv.clearContent();
    settle(sleeper, Set.of(), Set.of(Items.OAK_LOG));
    check(stash.canUse(), "a take-out with an empty pack did not start the visit");
    stash.start();
    stash.tick();
    check(sleeper.personMainInv.countItem(Items.OAK_LOG) == 3 && chest.countItem(Items.OAK_LOG) == 0,
        "take-out alone moved nothing: pack " + PersonalChest.summarize(sleeper.personMainInv)
            + ", chest " + PersonalChest.summarize(chest));

    // A chest that is gone closes the visit with nothing moved and nothing pending.
    chest.setItem(0, new ItemStack(Items.BREAD, 2));
    settle(sleeper, Set.of(Items.OAK_LOG), Set.of(Items.BREAD));
    level.setBlock(chestPos, Blocks.AIR.defaultBlockState(), 3);
    check(stash.canUse(), "the visit to a vanished chest did not start");
    stash.start();
    stash.tick();
    check(sleeper.personMainInv.countItem(Items.OAK_LOG) == 3 && sleeper.personMainInv.countItem(Items.BREAD) == 0
        && sleeper.keepingForHome().isEmpty() && sleeper.takingFromHome().isEmpty(),
        "a vanished chest did not close the visit cleanly");
    sleeper.discard();
  }

  /** The bedtime answer as settleStash records it, without an LLM in the loop. */
  private static void settle(RealPerson person, Set<Item> keep, Set<Item> take) throws ReflectiveOperationException {
    Field keeping = RealPerson.class.getDeclaredField("keepingForHome");
    keeping.setAccessible(true);
    keeping.set(person, keep);
    Field taking = RealPerson.class.getDeclaredField("takingFromHome");
    taking.setAccessible(true);
    taking.set(person, take);
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

  /** The birch camp circle, whose bed 0 owns the camp chest; the stores accept everything. */
  private static final class FixtureVillage extends Village {
    private final Building center = new Building(new BlockPos(-7000, 150, -7000),
        "village_center_birch_forest_1", Rotation.NONE);
    private final List<ItemStack> stored = new ArrayList<>();

    private FixtureVillage() { super("Chest take-out fixture"); }

    @Override public Building getBuilding(UUID id) { return center; }
    @Override public Building getTownCenter() { return center; }
    @Override public Collection<Building> getBuildings() { return List.of(center); }
    @Override public JobAssignment getJobAssignment(UUID id) { return null; }
    @Override public BedAssignment getBedAssignment(UUID id) { return new BedAssignment(id, center.getUUID(), 0); }
    @Override public ItemStack storeAwayFrom(ItemStack stack, Collection<BlockPos> excluding, BlockPos near) {
      stored.add(stack.copy());
      return ItemStack.EMPTY;
    }
  }
}
