package com.quzzar.kithkyn.dev;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.PersonEntityType;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.entities.ai.goals.CampfireRecoveryGoal;
import com.quzzar.kithkyn.entities.ai.goals.work.CampfireAccess;
import com.quzzar.kithkyn.entities.ai.goals.work.CampfireRoast;
import com.quzzar.kithkyn.entities.ai.goals.work.ContainerAccess;
import com.quzzar.kithkyn.entities.ai.goals.work.CookStep;
import com.quzzar.kithkyn.entities.ai.goals.work.FishCookStep;
import com.quzzar.kithkyn.entities.ai.goals.work.BlockWorkStep;
import com.quzzar.kithkyn.entities.ai.goals.work.WorkLoopGoal;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.buildings.Building;
import com.quzzar.kithkyn.village.buildings.BuildingInfo;
import com.quzzar.kithkyn.village.buildings.Buildings;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Opt-in real navigation and cooking checks, only in a disposable world. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class CampfireAccessVerification {
  private static int ticks;
  private static RealPerson walkingCook;
  private static net.minecraft.world.Container pantry;
  private static BlockPos pantryPosition;
  private static boolean fetched;
  private CampfireAccessVerification() { }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (!Boolean.getBoolean("kithkyn.campfires.verify")) return;
    ticks++;
    try {
      ServerLevel level = event.getServer().overworld();
      if (ticks == 40) {
        level.setDayTime(6000);
        level.updateSkyBrightness();
        event.getServer().tickRateManager().setTickRate(100);
        verifyCooking(level, new CookStep(), 0);
        verifyCooking(level, new FishCookStep(), 80);
        if (Boolean.getBoolean("kithkyn.campfires.verifyPueblo")) {
          for (Rotation rotation : Rotation.values()) verifyPueblo(level, rotation);
        }
        beginCookingWalk(level);
      }
      if (walkingCook == null) return;
      if (walkingCook.personMainInv.countItem(Items.COD) > 0) fetched = true;
      if (pantry.countItem(Items.COOKED_COD) == 4) {
        check(fetched && walkingCook.personMainInv.isEmpty() && pantry.countItem(Items.COD) == 0,
            "fetch/cook/deposit walk lost or duplicated food");
        check(ContainerAccess.canReach(walkingCook, walkingCook.getEyePosition(), pantryPosition, 9.0D),
            "cooked food was deposited remotely");
        walkingCook.discard();
        walkingCook = null;
        Kithkyn.LOGGER.info("[campfires-verify] RESULT PASS: independent plaza, reachable/free fire fallback, stable batches, interrupted-food conservation, recovery, authored Pueblo approaches, physical fetch/cook/deposit");
        event.getServer().halt(false);
      }
      if (ticks > 3000) throw new AssertionError("Cook trip stalled at " + walkingCook.position());
    } catch (Exception | AssertionError failure) {
      Kithkyn.LOGGER.error("[campfires-verify] RESULT FAIL", failure);
      event.getServer().halt(false);
    }
  }

  /** Install the real work loop and let normal server ticks perform every trip and transfer. */
  private static void beginCookingWalk(ServerLevel level) {
    BlockPos origin = new BlockPos(-7400, 150, -7000);
    BuildingInfo info = BuildingInfo.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
        {"structure":"village_center_birch_forest_1", "meeting_point":[0,1,0], "campfires":[[12,1,0]]}
        """)).getOrThrow();
    pantryPosition = origin.offset(-10,1,0);
    FixtureVillage village = new FixtureVillage(level, info, origin, Rotation.NONE) {
      @Override public BlockPos getNearestContainer(BlockPos from, java.util.Collection<BlockPos> skip) {
        return skip.contains(pantryPosition) ? BlockPos.ZERO : pantryPosition;
      }
    };
    for (int x = (origin.getX()-18)>>4; x <= (origin.getX()+18)>>4; x++) {
      for (int z = (origin.getZ()-8)>>4; z <= (origin.getZ()+8)>>4; z++) level.setChunkForced(x,z,true);
    }
    for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-18,0,-8), origin.offset(18,5,8))) {
      level.setBlock(pos, pos.getY() == origin.getY() ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
    }
    level.setBlock(pantryPosition, Blocks.CHEST.defaultBlockState(), 2);
    pantry = (net.minecraft.world.Container) level.getBlockEntity(pantryPosition);
    pantry.setItem(0, new ItemStack(Items.COD, 4));
    level.setBlock(village.getCampfirePositions().getFirst(), Blocks.CAMPFIRE.defaultBlockState(), 2);
    walkingCook = new RealPerson(PersonEntityType.PERSON.get(), level) {
      @Override public Village getVillage() { return village; }
      @Override public void reloadState() {
        super.reloadState();
        this.goalSelector.removeAllGoals(goal -> true);
        this.targetSelector.removeAllGoals(goal -> true);
      }
    };
    walkingCook.reloadState();
    walkingCook.goalSelector.addGoal(1, new WorkLoopGoal<>(walkingCook, new CookStep()));
    standAt(walkingCook, origin.above());
    level.addFreshEntity(walkingCook);
  }

  private static void verifyCooking(ServerLevel level, BlockWorkStep cook, int offset) throws Exception {
    BuildingInfo info = BuildingInfo.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
        {"structure":"village_center_birch_forest_1", "meeting_point":[0,1,0],
         "campfires":[[4,1,0],[10,1,0]]}
        """)).getOrThrow();
    BlockPos origin = new BlockPos(-7000 + offset, 150, -7000);
    FixtureVillage village = new FixtureVillage(level, info, origin, Rotation.NONE);
    for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-5,0,-6), origin.offset(17,5,6))) {
      level.setBlock(pos, pos.getY() == origin.getY() ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
    }
    BlockPos meeting = origin.above();
    BlockPos bell = meeting.north();
    level.setBlock(bell, Blocks.BELL.defaultBlockState(), 2);
    for (BlockPos fire : village.getCampfirePositions()) level.setBlock(fire, Blocks.CAMPFIRE.defaultBlockState(), 2);
    check(village.getGatheringPoint().equals(meeting), "meeting point changed to a campfire");
    Method placeFallback = Village.class.getDeclaredMethod("placeCampfireIfMissing");
    placeFallback.setAccessible(true);
    placeFallback.invoke(village);
    check(level.getBlockState(bell).is(Blocks.BELL) && level.getBlockState(meeting).isAir(), "founding replaced the bell/plaza");
    RealPerson person = person(level, village, meeting);
    person.personMainInv.setItem(0, new ItemStack(Items.COD, 5));
    BlockPos first = village.getCampfirePositions().getFirst();
    BlockPos second = village.getCampfirePositions().getLast();
    CampfireBlockEntity firstFire = CampfireRoast.litFireAt(level, first);
    for (int slot = 0; slot < 4; slot++) check(firstFire.placeFood(null, new ItemStack(Items.BEEF), 12000), "fill first fire");
    BlockPos chosen = cook.select(person);
    check(second.equals(chosen), "cook did not skip the full nearest fire");
    standAt(person, cook.positionOf(chosen));
    check(cook.inReach(person, chosen), "chosen standing ground cannot reach its fire");
    check(cook.act(person, chosen), "roast did not start");
    firstFire.getItems().clear();
    check(second.equals(cook.select(person)), "mid-roast switched to the newly available nearer fire");
    person.tickCount += 160;
    check(cook.act(person, chosen), "roast did not finish");
    check(person.personMainInv.countItem(Items.COOKED_COD) == 1, "cooked result missing");
    check(cook.act(person, chosen), "second roast did not start");
    level.setBlock(second, level.getBlockState(second).setValue(CampfireBlock.LIT, false), 2);
    check(!cook.act(person, chosen), "doused fire kept its cooking job");
    cook.released(person, chosen);
    check(person.personMainInv.countItem(Items.COD) == 4, "interrupted raw food lost or duplicated");
    check(((CampfireBlockEntity) level.getBlockEntity(second)).getItems().stream().allMatch(ItemStack::isEmpty),
        "interrupted roast remained in doused fire");
    check(first.equals(cook.select(person)), "cook did not fall back after the second fire went out");
    cook.released(person, first);
    person.personMainInv.clearContent();
    person.setHealth(1.0F);
    standAt(person, meeting);
    CampfireRecoveryGoal recovery = new CampfireRecoveryGoal(person);
    check(recovery.canUse(), "hurt resident could not choose the remaining lit fire");
    recovery.start();
    level.setBlock(first, level.getBlockState(first).setValue(CampfireBlock.LIT, false), 2);
    check(!recovery.canContinueToUse(), "recovery followed a doused fire");
    recovery.stop();
    level.setBlock(first, Blocks.CAMPFIRE.defaultBlockState(), 2);
    level.setBlock(second, Blocks.CAMPFIRE.defaultBlockState(), 2);
    for (BlockPos pos : BlockPos.betweenClosed(first.offset(-2,0,-2), first.offset(2,3,2))) {
      if (!pos.equals(first)) level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
    }
    CampfireAccess.Target accessible = CampfireAccess.select(person, village, true, 30);
    check(accessible != null && second.equals(accessible.fire()), "blocked closest fire prevented fallback");
    person.discard();
  }

  private static void verifyPueblo(ServerLevel level, Rotation rotation) {
    BuildingInfo info = Buildings.getByName("village_center_pueblo_1");
    check(info != null, "private Pueblo definition missing");
    BlockPos origin = new BlockPos(-7300 + rotation.ordinal() * 100, 150, -7300);
    var template = level.getStructureManager().getOrCreate(
        ResourceLocation.fromNamespaceAndPath(Kithkyn.MODID, info.getName()));
    check(template.placeInWorld(level, origin, origin,
        new StructurePlaceSettings().setRotation(rotation).setIgnoreEntities(true), level.random, 18), "Pueblo placement failed");
    FixtureVillage village = new FixtureVillage(level, info, origin, rotation);
    RealPerson person = person(level, village, village.getGatheringPoint());
    for (BlockPos fire : village.getCampfirePositions()) {
      BlockPos approach = ContainerAccess.approachTo(person, fire, CampfireAccess.REACH_SQR);
      check(approach != null, "Pueblo fire unreachable from plaza in " + rotation + " at " + fire);
      Kithkyn.LOGGER.info("[campfires-verify] Pueblo {} fire {} approach {}", rotation, fire, approach);
    }
    person.discard();
  }

  private static RealPerson person(ServerLevel level, FixtureVillage village, BlockPos position) {
    RealPerson person = new RealPerson(PersonEntityType.PERSON.get(), level) {
      @Override public Village getVillage() { return village; }
    };
    person.setNoAi(true);
    standAt(person, position);
    return person;
  }

  private static void standAt(RealPerson person, BlockPos position) {
    person.setPos(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D);
    person.setOnGround(true);
  }

  private static void check(boolean condition, String failure) {
    if (!condition) throw new AssertionError(failure);
  }

  private static class FixtureVillage extends Village {
    private final Building center;
    private FixtureVillage(ServerLevel level, BuildingInfo info, BlockPos origin, Rotation rotation) {
      super("Campfire fixture");
      this.center = new Building(origin, info.getName(), rotation) {
        @Override public BuildingInfo getInfo() { return info; }
      };
      attach(level);
    }
    @Override public Building getTownCenter() { return center; }
    @Override public boolean hasResident(UUID person) { return true; }
  }
}
