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
      for (Rotation rotation : Rotation.values()) verifyMine(level, rotation);
      verifyMine(level, Rotation.NONE, true);
      verifyFishing(level);
      Kithkyn.LOGGER.info("[workers-verify] RESULT PASS: flooded frontier seals then cuts ribs in all rotations; occupied offhand and full-pack bucket exchange; fishing catch and interruption lifecycle");
    } catch (Exception | AssertionError failure) {
      Kithkyn.LOGGER.error("[workers-verify] RESULT FAIL", failure);
    } finally {
      event.getServer().halt(false);
    }
  }

  private static void verifyMine(ServerLevel level, Rotation rotation) {
    verifyMine(level, rotation, false);
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
    miner.personMainInv.setItem(0, new ItemStack(Items.DIRT, 64));
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
    for (int pick = 0; pick < 50 && !level.getBlockState(leftRib).isAir()
        && !level.getBlockState(rightRib).isAir(); pick++) {
      BlockPos stand = mine.select(miner);
      check(stand != null, "flooded ramp selected no work: " + rotation);
      standAt(miner, stand);
      mine.acquired(miner, stand);
      for (int act = 0; act < 2000 && mine.act(miner, stand); act++) { }
      mine.released(miner, stand);
      if (bucket && level.getFluidState(flooded).isEmpty()) break;
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
    check(miner.personMainInv.countItem(Items.DIRT) < 64, "miner skipped reachable lining: " + rotation);
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
    }

    @Override public Building getBuilding(UUID id) { return building; }
    @Override public java.util.Collection<Building> getBuildings() { return java.util.List.of(building); }
    @Override public JobAssignment getJobAssignment(UUID id) {
      return new JobAssignment(id, occupation, building.getUUID(), 0);
    }
  }
}
