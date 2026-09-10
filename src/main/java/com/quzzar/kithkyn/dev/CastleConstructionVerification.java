package com.quzzar.kithkyn.dev;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.entities.AgeStage;
import com.quzzar.kithkyn.entities.ai.goals.work.BuildStep;
import com.quzzar.kithkyn.entities.ai.goals.work.GatherStep;
import com.quzzar.kithkyn.entities.ai.goals.work.WorkLoopGoal;
import com.quzzar.kithkyn.village.Occupation;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.VillageManager;
import com.quzzar.kithkyn.village.buildings.BuildProgress;
import com.quzzar.kithkyn.village.buildings.Building;
import com.quzzar.kithkyn.village.buildings.Buildings;
import com.quzzar.kithkyn.village.buildings.InstantBuildStructure;
import com.quzzar.kithkyn.village.buildings.StructureInProgress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Paid castle construction by a walking builder; opt in only in a disposable private-catalog world. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class CastleConstructionVerification {
  private static final String PREFIX = "[castle-construction-verify]";
  private static final int MAX_TICKS = 300_000;
  private static int ticks;
  private static int started;
  private static boolean finished;
  private static Village village;
  private static ApprovedStructureAccess.Person builder;
  private static StructureInProgress project;
  private static BuildProgress lastPhase;
  private static int originalBeds;
  private static int originalBuildings;
  private static boolean paid;
  private static final List<Container> stores = new ArrayList<>();
  private static final List<ItemStack> recipe = new ArrayList<>();
  private static final HashSet<BlockPos> walked = new HashSet<>();

  private CastleConstructionVerification() { }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (!Boolean.getBoolean("kithkyn.castleConstruction.verify") || finished || ++ticks < 40) return;
    ServerLevel level = event.getServer().overworld();
    try {
      if (village == null) {
        setup(level);
        started = ticks;
        return;
      }
      walked.add(builder.blockPosition());
      check(builder.isAlive(), "Builder died during construction");
      check(village.getBuildings().size() == originalBuildings, "Castle published before completion callback");
      check(village.getTotalBeds() == originalBeds, "Unfinished castle advertised housing");
      check(village.getUnassignedJobs().stream().noneMatch(job -> job.getBuildingUUID().equals(project.getBuilding().getUUID())),
          "Unfinished castle advertised jobs");
      if (project.isGathering()) {
        for (ItemStack cost : recipe) {
          int stock = stores.stream().mapToInt(store -> store.countItem(cost.getItem())).sum();
          int pack = builder.personMainInv.countItem(cost.getItem());
          check(stock + pack == cost.getCount(), "Unpaid recipe lost items: " + cost + " stores=" + stock + " pack=" + pack);
        }
      }
      if (project.getProgress() != lastPhase) {
        lastPhase = project.getProgress();
        Kithkyn.LOGGER.info("{} PHASE {} after {} ticks; builder={} walked={}", PREFIX, lastPhase,
            ticks - started, builder.position(), walked.size());
      }
      if (!project.isGathering() && !paid) {
        paid = true;
        check(walked.size() >= 8, "Recipe was committed without the builder walking to storage and site");
        check(recipe.stream().allMatch(stack -> stores.stream().mapToInt(store -> store.countItem(stack.getItem())).sum() == 0),
            "Paid castle left recipe materials in fixture stores");
        check(recipe.stream().allMatch(stack -> builder.personMainInv.countItem(stack.getItem()) == 0),
            "Committed recipe remains in builder inventory");
      }
      if (project.getProgress() == BuildProgress.COMPLETE) {
        complete(level);
        finished = true;
        Kithkyn.LOGGER.info("{} RESULT PASS: physical two-storehouse recipe collection, ordinary walking GatherStep/BuildStep, "
            + "incremental paid castle, eight jobs and eight beds published only after completion, protected evidence; ticks={} walked={}",
            PREFIX, ticks - started, walked.size());
        event.getServer().halt(false);
      } else {
        check(ticks - started < MAX_TICKS, "Construction exceeded bound: " + project.getProgress()
            + " builder=" + builder.position() + " next=" + project.peekNextBlockPos());
        if (!event.getServer().tickRateManager().isSprinting()) {
          event.getServer().tickRateManager().requestGameToSprint(Math.min(24_000, MAX_TICKS - (ticks - started)));
        }
        if ((ticks - started) % 12_000 == 0) {
          Kithkyn.LOGGER.info("{} PROGRESS ticks={} phase={} next={} builder={} pack={} stores={}", PREFIX, ticks - started,
              project.getProgress(), project.peekNextBlockPos(), builder.position(), builder.personMainInv,
              recipe.stream().map(cost -> cost.getItem() + "=" + stores.stream().mapToInt(store -> store.countItem(cost.getItem())).sum()).toList());
        }
      }
    } catch (Exception | AssertionError failure) {
      finished = true;
      Kithkyn.LOGGER.error(PREFIX + " RESULT FAIL", failure);
      event.getServer().halt(false);
    }
  }

  private static void setup(ServerLevel level) throws ReflectiveOperationException {
    level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
    level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, level.getServer());
    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
    level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, level.getServer());
    level.setDayTime(6000);
    for (int x = 185; x <= 194; x++) for (int z = 185; z <= 194; z++) level.setChunkForced(x, z, true);
    for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(2960, 157, 2960), new BlockPos(3119, 185, 3119))) {
      level.setBlock(pos, pos.getY() <= 159 ? Blocks.SANDSTONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
    }
    Building center = stamp(level, "village_center_desert_1", new BlockPos(3000, 159, 3000));
    Building first = stamp(level, "storehouse_desert_1", new BlockPos(3035, 159, 3000));
    Building second = stamp(level, "storehouse_desert_1", new BlockPos(3000, 159, 3035));
    village = new ApprovedStructureAccess.VillageFixture(level, center, true, first, second);
    VillageManager.get(level).getVillages().put(village.getID(), village);
    for (BlockPos position : village.getVillageContainerPositions()) {
      if (level.getBlockEntity(position) instanceof Container container) container.clearContent();
    }
    stores.add(container(level, first));
    stores.add(container(level, second));
    check(village.startProjectAt(Buildings.getByName("castle_desert_1"), new BlockPos(3060, 159, 3060)),
        "Ordinary startProjectAt refused flat castle site");
    project = village.getCurrentProject();
    check(project != null && project.isGathering(), "Castle skipped its unpaid gathering phase");
    originalBeds = village.getTotalBeds();
    originalBuildings = village.getBuildings().size();
    recipe.addAll(project.requiredMaterials().stream().map(ItemStack::copy).toList());
    check(!recipe.isEmpty(), "Castle recipe was empty");
    int itemIndex = 0;
    for (ItemStack required : recipe) {
      Container store = stores.get(itemIndex++ % stores.size());
      int remaining = required.getCount();
      for (int slot = 0; remaining > 0 && slot < store.getContainerSize(); slot++) {
        if (!store.getItem(slot).isEmpty()) continue;
        int count = Math.min(remaining, required.getMaxStackSize());
        store.setItem(slot, required.copyWithCount(count));
        remaining -= count;
      }
      check(remaining == 0, "Fixture could not hold exact recipe");
      store.setChanged();
    }
    check(stores.stream().noneMatch(Container::isEmpty), "Fixture must require both real storehouses");
    builder = new ApprovedStructureAccess.Person(level, village);
    builder.setLifeStage(AgeStage.ADULT);
    village.getPopulation().add(builder.getUUID());
    village.assignJob(builder.getUUID(), village.getUnassignedJobs().stream()
        .filter(job -> job.getOccupation() == Occupation.BUILDER).findFirst().orElseThrow());
    builder.setOccupation(Occupation.BUILDER);
    builder.issueStartingKit();
    ApprovedStructureAccess.moveTo(builder, new BlockPos(2998, 160, 2998));
    check(level.addFreshEntity(builder), "Could not spawn construction worker");
    ApprovedStructureAccess.assignSingle(village, builder.getUUID(), center.getUUID());
    ApprovedStructureAccess.enableWalking(builder);
    builder.goalSelector.addGoal(3, new WorkLoopGoal<>(builder, new GatherStep()));
    builder.goalSelector.addGoal(4, new WorkLoopGoal<>(builder, new BuildStep()));
    Kithkyn.LOGGER.info("{} START recipe={} rotation={} bedCount={}", PREFIX, recipe, project.getRotation(), originalBeds);
  }

  private static Building stamp(ServerLevel level, String name, BlockPos origin) {
    Building building = new Building(origin, name, Rotation.NONE);
    check(new InstantBuildStructure(building, new Random(1), level).seatAtOrigin(origin, new HashSet<>()).buildInstantly(),
        "Could not seed existing " + name);
    return building;
  }

  private static Container container(ServerLevel level, Building building) {
    BlockPos local = BlockPos.of(building.getInfo().getContainerLocations().getFirst());
    BlockPos world = BlockPos.of(building.getOriginLocation()).offset(local.rotate(building.getRotation()));
    check(level.getBlockEntity(world) instanceof Container, "Authored storehouse container missing: " + world);
    return (Container) level.getBlockEntity(world);
  }

  /** Invoke the ordinary publication boundary only after the ordinary builder completes every block. */
  private static void complete(ServerLevel level) throws ReflectiveOperationException {
    check(paid, "Castle finished without a paid recipe");
    var publication = Village.class.getDeclaredMethod("checkCurrentProject");
    publication.setAccessible(true);
    publication.invoke(village);
    Building castle = village.getBuilding(project.getBuilding().getUUID());
    check(castle != null && village.getCurrentProject() == null, "Completed castle was not published");
    check(village.getBuildings().size() == originalBuildings + 1 && village.getTotalBeds() == originalBeds + 8,
        "Completion did not add exactly one castle and eight beds");
    check(village.getUnassignedJobs().stream().filter(job -> job.getBuildingUUID().equals(castle.getUUID())).count() == 8,
        "Castle completion did not publish all eight jobs");
    BlockPos origin = BlockPos.of(castle.getOriginLocation());
    for (long bed : castle.getInfo().getBedLocations()) {
      check(level.getBlockState(origin.offset(BlockPos.of(bed).rotate(castle.getRotation()))).getBlock() instanceof BedBlock,
          "Published bed is physically missing");
    }
    for (BlockPos local : castle.getInfo().getCastleLayout().evidenceContainers()) {
      BlockPos world = origin.offset(local.rotate(castle.getRotation()));
      check(level.getBlockState(world).is(Blocks.BARREL), "Incremental build lost an evidence barrel");
      check(!village.getVillageContainerPositions().contains(world), "Evidence became shared storage after completion");
    }
  }

  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
}
