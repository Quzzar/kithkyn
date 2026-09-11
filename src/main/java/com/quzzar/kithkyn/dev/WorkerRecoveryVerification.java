package com.quzzar.kithkyn.dev;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.PersonEntityType;
import com.quzzar.kithkyn.entities.FishingCast;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.entities.ai.goals.ApproachWatch;
import com.quzzar.kithkyn.entities.ai.goals.work.FishStep;
import com.quzzar.kithkyn.entities.ai.goals.work.MineStep;
import com.quzzar.kithkyn.entities.ai.goals.work.WorkLoopGoal;
import com.quzzar.kithkyn.village.JobAssignment;
import com.quzzar.kithkyn.village.LocationManager;
import com.quzzar.kithkyn.village.Occupation;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.buildings.Building;
import com.quzzar.kithkyn.village.buildings.MineShaft;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Opt-in native fixtures. Use -Dkithkyn.workers.verify=true only in a disposable world. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class WorkerRecoveryVerification {
  private static int ticks;

  private WorkerRecoveryVerification() { }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (!Boolean.getBoolean("kithkyn.workers.verify") || ++ticks != 40) return;
    try {
      ServerLevel level = event.getServer().overworld();
      level.setDayTime(6000);
      level.updateSkyBrightness();
      verifyUnavailableMine(level);
      for (Rotation rotation : Rotation.values()) verifyMine(level, rotation);
      verifyMine(level, Rotation.NONE, true);
      verifyFishing(level);
      Kithkyn.LOGGER.info("[workers-verify] RESULT PASS: unavailable mine reports survive reload and clear after work; fresh second mine offered; flooded frontier seals then cuts ribs in all rotations; occupied offhand and full-pack bucket exchange; fishing catch and interruption lifecycle");
    } catch (Exception | AssertionError failure) {
      Kithkyn.LOGGER.error("[workers-verify] RESULT FAIL", failure);
    } finally {
      event.getServer().halt(false);
    }
  }

  private static void verifyMine(ServerLevel level, Rotation rotation) {
    verifyMine(level, rotation, false);
  }

  /** A mine that cannot select any work must tell the village, even before travel starts. */
  private static void verifyUnavailableMine(ServerLevel level) {
    FixtureVillage village = new FixtureVillage(new BlockPos(-6300, 150, -6000),
        "mine_birch_forest_1", Rotation.NONE, Occupation.MINER);
    RealPerson miner = worker(level, village);
    MineShaft shaft = MineShaft.root(village.building, LocationManager.getJobLocation(miner));
    for (BlockPos local : BlockPos.betweenClosed(-12, -12, -3, 12, 3, 12)) {
      level.setBlock(world(shaft, local), Blocks.BEDROCK.defaultBlockState(), 2);
    }
    standAt(miner, shaft.entry());
    MineStep mine = new MineStep();
    check(mine.select(miner) == null, "sealed mine unexpectedly offered work");
    check(blockers(miner).stream().anyMatch(text -> text.contains("mine")
        && (text.contains("exhausted") || text.contains("cannot reach"))),
        "mine selected no work without reporting a current blocker to the village");
    check(com.quzzar.kithkyn.village.buildings.UrbanPlanner.optionsFor(village).buildable().stream()
        .anyMatch(candidate -> candidate.info().getName().equals("mine_birch_forest_1")
            && candidate.mode() == com.quzzar.kithkyn.village.buildings.ConstructionMode.FRESH
            && candidate.description().contains("new shaft on a separate site")),
        "a staffed exhausted mine prevented the planner from offering another mine");
    net.minecraft.nbt.CompoundTag saved = new net.minecraft.nbt.CompoundTag();
    miner.saveWithoutId(saved);
    RealPerson restored = worker(level, village);
    restored.load(saved);
    check(blockers(restored).stream().anyMatch(text -> text.contains("exhausted")),
        "mine exhaustion report did not survive saving");
    for (BlockPos local : BlockPos.betweenClosed(-12, -12, -3, 12, 3, 12)) {
      level.setBlock(world(shaft, local), Blocks.STONE.defaultBlockState(), 2);
    }
    for (int z = -1; z <= 3; z++) {
      for (BlockPos local : BlockPos.betweenClosed(-2, -z - 2, z, 2, Math.min(-1, -z + 2), z)) {
        level.setBlock(world(shaft, local), Blocks.AIR.defaultBlockState(), 2);
      }
    }
    standAt(restored, world(shaft, new BlockPos(0, -2, 0)));
    restored.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_PICKAXE));
    restored.personMainInv.setItem(0, new ItemStack(Items.DIRT, 64));
    MineStep resumed = new MineStep();
    BlockPos stand = resumed.select(restored);
    check(stand != null, "reopened mine offered no work");
    check(blockers(restored).stream().anyMatch(text -> text.contains("exhausted")),
        "merely selecting a destination cleared a mine failure before physical work");
    standAt(restored, stand);
    for (int act = 0; act < 2000 && resumed.act(restored, stand); act++) { }
    check(blockers(restored).stream().noneMatch(text -> text.contains("exhausted")),
        "successful work retained the exhausted-mine report after reload");
    restored.discard();
    miner.discard();
  }

  private static void verifyMine(ServerLevel level, Rotation rotation, boolean bucket) {
    FixtureVillage village = new FixtureVillage(new BlockPos(-6000 + rotation.ordinal() * 100, 150, bucket ? -6100 : -6000),
        "mine_birch_forest_1", rotation, Occupation.MINER);
    RealPerson miner = worker(level, village);
    MineShaft shaft = MineShaft.root(village.building, LocationManager.getJobLocation(miner));
    if (rotation == Rotation.NONE && !bucket) verifyMineMessages(level, village, miner, shaft);
    // Intact ceiling beyond a dry ramp hides the lower water in row-sweep order.
    for (BlockPos local : BlockPos.betweenClosed(-12, -28, -3, 12, 3, 25)) {
      level.setBlock(world(shaft, local), Blocks.STONE.defaultBlockState(), 2);
    }
    for (int z = -1; z <= 16; z++) {
      for (BlockPos local : BlockPos.betweenClosed(-2, -z - 2, z, 2, Math.min(-1, -z + 2), z)) {
        level.setBlock(world(shaft, local), Blocks.AIR.defaultBlockState(), 2);
      }
    }
    // The upper rows were already cut before the miner lost the flooded lower footing.
    for (int z = 17; z <= 19; z++) {
      for (BlockPos local : BlockPos.betweenClosed(-2, -18, z, 2, -z + 2, z)) {
        level.setBlock(world(shaft, local), Blocks.AIR.defaultBlockState(), 2);
      }
    }
    for (int z = 17; z <= 21; z++) {
      for (BlockPos local : BlockPos.betweenClosed(-3, -z - 2, z, 3, -19, z)) {
        level.setBlock(world(shaft, local), Blocks.WATER.defaultBlockState(), 2);
      }
    }
    miner.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_PICKAXE));
    // Twenty dirt against thirty leaks and seventy-five wet cells: without a bucket
    // the support runs out mid-pocket, so the ribs must be cut for stone on the way.
    miner.personMainInv.setItem(0, new ItemStack(Items.DIRT, bucket ? 64 : 20));
    if (bucket) {
      for (int slot = 1; slot < miner.personMainInv.getContainerSize(); slot++) {
        miner.personMainInv.setItem(slot, new ItemStack(Items.APPLE, 64));
      }
      miner.personMainInv.setItem(1, new ItemStack(Items.BUCKET, 16));
      miner.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.COOKED_SALMON, 6));
    }
    standAt(miner, world(shaft, new BlockPos(0, -2, 0)));
    MineStep mine = new MineStep();
    BlockPos leftRib = world(shaft, new BlockPos(-3, -18, 16));
    BlockPos rightRib = world(shaft, new BlockPos(3, -18, 16));
    BlockPos flooded = world(shaft, new BlockPos(0, -19, 17));
    if (bucket) {
      for (int pick = 0; pick < 50 && !level.getFluidState(flooded).isEmpty(); pick++) {
        pick(level, mine, miner, "flooded ramp selected no work: " + rotation);
      }
    } else {
      // Without a bucket the pocket is plugged source by source, never quarried while
      // wet, and the plugs come out as ordinary rock once it is dry, back into the
      // pack. The loop that laid and dug the same plug 700 times in an hour at
      // Calirra (2026-09-11) reached neither milestone, and never ran short.
      int picks = 0;
      while (picks < 600 && (wetPocket(level, shaft) || !level.getBlockState(flooded).isAir())) {
        pick(level, mine, miner, "plugging pocket selected no work: " + rotation + " at pick " + picks);
        picks++;
      }
      check(!wetPocket(level, shaft), "flooded frontier never dried without a bucket: " + rotation);
      check(level.getBlockState(flooded).isAir(), "dry plugs were not quarried back out: " + rotation);
      BlockPos lining = world(shaft, new BlockPos(3, -19, 17));
      check(!level.getBlockState(lining).isAir() && level.getFluidState(lining).isEmpty(),
          "a sealed leak outside the corridor was quarried with the plugs: " + rotation);
    }
    if (bucket) {
      check(level.getFluidState(flooded).isEmpty(), "food in the offhand prevented bailing");
      check(miner.personMainInv.countItem(Items.COOKED_SALMON) == 6, "bucket exchange lost the held meal");
      int buckets = miner.personMainInv.countItem(Items.BUCKET)
          + (miner.getOffhandItem().is(Items.BUCKET) ? miner.getOffhandItem().getCount() : 0);
      int droppedBuckets = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
          miner.getBoundingBox().inflate(4.0D)).stream()
          .filter(item -> item.getItem().is(Items.BUCKET)).mapToInt(item -> item.getItem().getCount()).sum();
      check(buckets == 16 && droppedBuckets == 0, "full-pack exchange lost or copied buckets: carried="
          + buckets + ", dropped=" + droppedBuckets + ", offhand=" + miner.getOffhandItem());
      check(miner.getMainHandItem().is(Items.STONE_PICKAXE), "bailing displaced the pickaxe");
      miner.discard();
      return;
    }
    check(level.getBlockState(leftRib).isAir() || level.getBlockState(rightRib).isAir(),
        "miner never resumed dry side cuts: " + rotation);
    check(miner.personMainInv.countItem(Items.DIRT) < 20, "miner skipped reachable lining: " + rotation);
    check(miner.personMainInv.countItem(Items.BUCKET) == 0, "fixture unexpectedly supplied a bucket");
    miner.discard();
  }

  private static void verifyMineMessages(ServerLevel level, FixtureVillage village, RealPerson miner, MineShaft shaft) {
    BlockPos target = world(shaft, new BlockPos(0, -18, 16));
    standAt(miner, shaft.mouth().offset(-20, 2, -20));
    ApproachWatch approach = new ApproachWatch(miner, "the mine");
    for (int tick = 0; tick <= 200; tick++) approach.giveUp(target);
    check(blockers(miner).stream().anyMatch(text -> text.contains("mine entrance")),
        "outside approach did not explain the entrance failure");
    standAt(miner, world(shaft, new BlockPos(0, -10, 8)));
    approach.begin();
    for (int tick = 0; tick <= 200; tick++) approach.giveUp(target);
    check(blockers(miner).stream().anyMatch(text -> text.contains("mine ramp"))
        && blockers(miner).stream().noneMatch(text -> text.contains("mine entrance")),
        "ramp failure retained the old entrance explanation");
    miner.logBlocker("I cannot get to the mine.");
    net.minecraft.nbt.CompoundTag saved = new net.minecraft.nbt.CompoundTag();
    miner.saveWithoutId(saved);
    RealPerson restored = worker(level, village);
    restored.load(saved);
    new ApproachWatch(restored, "the mine").arrived();
    check(blockers(restored).stream().noneMatch(text -> text.contains("mine")),
        "arrival after reload retained an old access complaint");
    restored.discard();
    approach.arrived();
  }

  /** One pick of the mine loop, stood where it asked, acted through and released. */
  private static void pick(ServerLevel level, MineStep mine, RealPerson miner, String failure) {
    BlockPos stand = mine.select(miner);
    check(stand != null, failure);
    standAt(miner, stand);
    mine.acquired(miner, stand);
    for (int act = 0; act < 2000 && mine.act(miner, stand); act++) { }
    mine.released(miner, stand);
  }

  /** Whether any interior cell of the fixture's flooded frontier rows still holds fluid. */
  private static boolean wetPocket(ServerLevel level, MineShaft shaft) {
    for (int z = 17; z <= 21; z++) {
      for (BlockPos local : BlockPos.betweenClosed(-2, -z - 2, z, 2, -19, z)) {
        if (!level.getFluidState(world(shaft, local)).isEmpty()) {
          return true;
        }
      }
    }
    return false;
  }

  private static java.util.List<String> blockers(RealPerson person) {
    return person.getData(com.quzzar.kithkyn.entities.KithkynAttachments.PERSONAL_LOG.get()).entries().stream()
        .filter(entry -> entry.kind().equals(com.quzzar.kithkyn.entities.PersonalLogData.KIND_BLOCKER))
        .map(com.quzzar.kithkyn.entities.PersonalLogData.Entry::text).toList();
  }

  private static void verifyFishing(ServerLevel level) {
    FixtureVillage village = new FixtureVillage(new BlockPos(-5600, 150, -6000),
        "fishery_birch_forest_1", Rotation.NONE, Occupation.FISHER);
    RealPerson fisher = worker(level, village);
    BlockPos station = LocationManager.getJobLocation(fisher);
    for (BlockPos pos : BlockPos.betweenClosed(station.offset(-6, -2, -6), station.offset(6, 3, 6))) {
      level.setBlock(pos, pos.getY() < station.getY() ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
    }
    BlockPos water = station.offset(0, -1, 3);
    level.setBlock(water, Blocks.WATER.defaultBlockState(), 2);
    standAt(fisher, station);
    fisher.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.FISHING_ROD));
    FishStep fish = new FishStep();
    check(station.equals(fish.select(fisher)), "fisher cannot find open water");
    for (int tick = 0; tick < FishingCast.CATCH_TICKS - 1; tick++) fish.act(fisher, station);
    check(fisher.personMainInv.isEmpty(), "fish appeared before the full wait");
    check(fisher.fishingTarget().orElseThrow().equals(water), "cast points outside the selected water");
    fish.act(fisher, station);
    check(fisher.personMainInv.countItem(Items.COD) + fisher.personMainInv.countItem(Items.SALMON) == 1,
        "one full cast must produce exactly one fish");
    check(fisher.fishingTarget().isEmpty(), "catch left a stale line");
    fish.act(fisher, station);
    fish.released(fisher, station);
    check(fisher.fishingTarget().isEmpty(), "interruption left a stale line");
    fish.select(fisher);
    fish.act(fisher, station);
    standAt(fisher, station.north(5));
    check(!fish.inReach(fisher, station) && fisher.fishingTarget().isEmpty(), "walking away left a stale line");
    standAt(fisher, station);
    fish.released(fisher, station);
    level.setBlock(water.above(), Blocks.STONE.defaultBlockState(), 2);
    check(fish.select(fisher) == null, "fisher cast through a covered water surface");
    level.setBlock(water.above(), Blocks.AIR.defaultBlockState(), 2);
    WorkLoopGoal<BlockPos> loop = new WorkLoopGoal<>(fisher, new FishStep());
    check(loop.requiresUpdateEveryTick(), "fishing clock can drift from the bobber animation");
    check(loop.canUse(), "fishing loop did not start in daylight");
    loop.start();
    loop.tick();
    check(fisher.fishingTarget().isPresent(), "active loop did not cast");
    level.setDayTime(18000);
    level.updateSkyBrightness();
    check(!loop.canContinueToUse(), "fisher kept working at night");
    loop.stop();
    check(fisher.fishingTarget().isEmpty(), "night left a stale line");
    fisher.discard();
  }

  private static RealPerson worker(ServerLevel level, FixtureVillage village) {
    village.attach(level);
    RealPerson person = new RealPerson(PersonEntityType.PERSON.get(), level) {
      @Override public Village getVillage() { return village; }
    };
    person.setNoAi(true);
    return person;
  }

  private static BlockPos world(MineShaft shaft, BlockPos local) {
    return shaft.mouth().offset(local.rotate(shaft.rotation()));
  }

  private static void standAt(RealPerson person, BlockPos stand) {
    person.setPos(stand.getX() + 0.5D, stand.getY(), stand.getZ() + 0.5D);
    person.setOnGround(true);
  }

  private static void check(boolean condition, String reason) {
    if (!condition) throw new AssertionError(reason);
  }

  private static final class FixtureVillage extends Village {
    private final Building building;
    private final Occupation occupation;

    private FixtureVillage(BlockPos origin, String name, Rotation rotation, Occupation occupation) {
      super("Worker recovery fixture");
      this.building = new Building(origin, name, rotation);
      this.occupation = occupation;
      setStyle(com.quzzar.kithkyn.village.buildings.VillageStyle.BIRCH_FOREST);
    }

    @Override public Building getBuilding(UUID id) { return building; }
    @Override public java.util.Map<net.minecraft.world.item.Item, Integer> stockTally() {
      return java.util.Map.of(Items.OAK_LOG, 64, Items.COBBLESTONE, 64);
    }
    @Override public java.util.Collection<Building> getBuildings() { return java.util.List.of(building); }
    @Override public JobAssignment getJobAssignment(UUID id) {
      return new JobAssignment(id, occupation, building.getUUID(), 0);
    }
  }
}
