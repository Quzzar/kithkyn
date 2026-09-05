package com.quzzar.kithkyn.dev;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.savedata.PlacedBlockStore;
import com.quzzar.kithkyn.village.Occupation;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.buildings.*;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Repeatable real-world lifecycle checks, enabled only by an explicit isolated-server JVM flag. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class RedevelopmentVerification {
  private RedevelopmentVerification() {
  }

  @SubscribeEvent
  public static void started(ServerStartedEvent event) {
    if (!Boolean.getBoolean("kithkyn.redevelopment.verify")) {
      return;
    }
    try {
      verify(event.getServer().overworld());
      Kithkyn.LOGGER.info("[redevelopment-verify] PASS: blocked upgrade, fresh replacement, player protection, "
          + "zero spare beds, full storage, displaced builder continuity, relaxed food/builder gates, "
          + "own reservation, commitment, halfway save/reload, demolition, construction and single item delivery");
    } catch (Exception | AssertionError failure) {
      Kithkyn.LOGGER.error("[redevelopment-verify] FAIL", failure);
    } finally {
      event.getServer().halt(false);
    }
  }

  private static void verify(ServerLevel level) throws Exception {
    Fixture fixture = fixture(level);
    Village village = fixture.village();
    BlockPos ground = fixture.ground();
    Building source = fixture.source();
    Building north = fixture.north();
    Building south = fixture.south();
    Building survivingFarm = fixture.survivingFarm();
    UUID builderId = UUID.randomUUID();
    village.assignJob(builderId, village.getUnassignedJobs().stream()
        .filter(job -> job.getOccupation() == Occupation.BUILDER).findFirst().orElseThrow());
    village.assignJob(UUID.randomUUID(), village.getUnassignedJobs().stream()
        .filter(job -> job.getBuildingUUID().equals(survivingFarm.getUUID())).findFirst().orElseThrow());
    // Model-independent observed housing demand: missing residents still count when their chunks are unloaded.
    for (int resident = 0; resident < 6; resident++) {
      village.getPopulation().add(UUID.randomUUID());
    }
    // Occupy every bed, putting the builder in the source house. These saved residents represent unloaded people.
    CompoundTag occupiedState = (CompoundTag) Village.CODEC.encodeStart(NbtOps.INSTANCE, village).getOrThrow();
    CompoundTag occupiedBeds = new CompoundTag();
    List<UUID> residents = new ArrayList<>(village.getPopulation());
    for (Building home : village.getBuildings()) {
      for (int bedIndex = 0; bedIndex < home.getInfo().getBedLocations().size(); bedIndex++) {
        UUID resident = home.getUUID().equals(source.getUUID()) ? builderId : UUID.randomUUID();
        residents.add(resident);
        occupiedBeds.put(resident.toString(), com.quzzar.kithkyn.village.BedAssignment.CODEC.encodeStart(NbtOps.INSTANCE,
            new com.quzzar.kithkyn.village.BedAssignment(resident, home.getUUID(), bedIndex)).getOrThrow());
      }
    }
    occupiedState.put("bed_assignments", occupiedBeds);
    occupiedState.put("unassigned_beds", new net.minecraft.nbt.ListTag());
    occupiedState.put("people", UUIDUtil.CODEC.listOf().encodeStart(NbtOps.INSTANCE, residents).getOrThrow());
    village = Village.CODEC.parse(NbtOps.INSTANCE, occupiedState).getOrThrow();
    village.attach(level);
    source = village.getBuilding(source.getUUID());
    check(village.getFreeGeneralBedCount() == 0, "fixture must have no spare beds");
    check(BuildingUpgrade.findPlacement(village, Buildings.getByName("house_plains_2")) == null,
        "ordinary upgrade unexpectedly fits through farms");
    var assessment = RedevelopmentPlanner.assess(village, Buildings.getByName("house_plains_2"), source,
        ground.offset(0, 0, -2), Rotation.NONE);
    check(assessment.plan().isPresent(), "blocked upgrade refused: " + assessment.reason());
    RedevelopmentPlan plan = assessment.plan().orElseThrow();
    check(plan.removed().size() == 2, "both blocking farms must be named");
    check(!plan.salvage().isEmpty(), "paid fixture farms must yield a material refund");
    java.nio.file.Files.writeString(java.nio.file.Path.of("redevelopment-proposal.txt"),
        RedevelopmentPlanner.label(plan) + "\n" + RedevelopmentPlanner.describe(village, plan));
    long surveyStart = System.nanoTime();
    var search = RedevelopmentPlanner.find(village);
    check(search.choices().stream().anyMatch(choice -> choice.info().getName().equals("house_plains_2")),
        "blocked upgrade was not discovered by the normal candidate search");
    Kithkyn.LOGGER.info("[redevelopment-verify] examined={} generated={} searchMs={}", search.examined(),
        search.choices().size(), (System.nanoTime() - surveyStart) / 1_000_000.0);
    for (int warm = 0; warm < 4; warm++) {
      var repeated = RedevelopmentPlanner.find(village);
      Kithkyn.LOGGER.info("[redevelopment-verify] warm={} examined={} generated={} searchMs={}", warm,
          repeated.examined(), repeated.choices().size(), repeated.nanos() / 1_000_000.0);
    }
    CompoundTag noFoodState = (CompoundTag) Village.CODEC.encodeStart(NbtOps.INSTANCE, village).getOrThrow();
    var jobs = noFoodState.getCompound("job_assignments");
    village.getJobAssignmentsView().forEach((id, job) -> {
      if (job.getOccupation() != Occupation.BUILDER) {
        jobs.remove(id.toString());
      }
    });
    Village noFood = Village.CODEC.parse(NbtOps.INSTANCE, noFoodState).getOrThrow();
    noFood.attach(level);
    check(RedevelopmentPlanner.assess(noFood, Buildings.getByName("house_plains_2"),
        noFood.getBuilding(source.getUUID()), ground.offset(0, 0, -2), Rotation.NONE).plan().isPresent(),
        "lack of surviving staffed food still prevents redevelopment");
    noFoodState.put("job_assignments", new CompoundTag());
    Village noBuilder = Village.CODEC.parse(NbtOps.INSTANCE, noFoodState).getOrThrow();
    noBuilder.attach(level);
    check(RedevelopmentPlanner.assess(noBuilder, Buildings.getByName("house_plains_2"),
        noBuilder.getBuilding(source.getUUID()), ground.offset(0, 0, -2), Rotation.NONE).plan().isPresent(),
        "outside-builder requirement still prevents proposals");
    var northBounds = RedevelopmentPlanner.worldBounds(village, north);
    CompoundTag noNeedState = (CompoundTag) Village.CODEC.encodeStart(NbtOps.INSTANCE, village).getOrThrow();
    noNeedState.put("people", new net.minecraft.nbt.ListTag());
    Village noNeed = Village.CODEC.parse(NbtOps.INSTANCE, noNeedState).getOrThrow();
    noNeed.attach(level);
    check(RedevelopmentPlanner.assess(noNeed, Buildings.getByName("house_plains_2"),
        noNeed.getBuilding(source.getUUID()), ground.offset(0, 0, -2), Rotation.NONE).plan().isEmpty(),
        "unneeded housing redevelopment was offered");
    var farmBlock = plan.blocks().stream().filter(block -> !block.state().isAir()
        && !com.quzzar.kithkyn.village.BlockOwnership.isPlanted(block.state())
        && northBounds.isInside(BlockPos.of(block.position())))
        .findFirst().orElseThrow();
    BlockPos damaged = BlockPos.of(farmBlock.position());
    level.setBlock(damaged, Blocks.AIR.defaultBlockState(), 2 | 16 | 32);
    PlacedBlockStore.get(level).clearPlaced(damaged);
    var damagedPlan = RedevelopmentPlanner.assess(village, Buildings.getByName("house_plains_2"), source,
        ground.offset(0, 0, -2), Rotation.NONE).plan().orElseThrow();
    check(!damagedPlan.salvage().equals(plan.salvage()), "damaged building still refunded its full investment");
    level.setBlock(damaged, farmBlock.state(), 2 | 16 | 32);
    PlacedBlockStore.get(level).markVillagePlaced(damaged);
    village.setStorageStrained(true);
    var fresh = RedevelopmentPlanner.assess(village, Buildings.getByName("storehouse_plains_2"), null,
        ground.offset(-2, 0, -2), Rotation.NONE);
    check(fresh.plan().isPresent(), "fresh replacement refused: " + fresh.reason());
    java.nio.file.Files.writeString(java.nio.file.Path.of("redevelopment-proposal.txt"),
        "\n" + RedevelopmentPlanner.label(fresh.plan().orElseThrow()) + "\n"
            + RedevelopmentPlanner.describe(village, fresh.plan().orElseThrow()),
        java.nio.file.StandardOpenOption.APPEND);

    BlockPos edit = ground.offset(1, 1, -2);
    var original = level.getBlockState(edit);
    PlacedBlockStore ownership = PlacedBlockStore.get(level);
    boolean wasVillage = ownership.isVillagePlaced(edit);
    level.setBlock(edit, Blocks.DIAMOND_BLOCK.defaultBlockState(), 2);
    ownership.markPlayerPlaced(edit);
    check(!RedevelopmentPlanner.stillValid(village, plan), "player modification was not protected");
    level.setBlock(edit, original, 2);
    ownership.clearPlaced(edit);
    if (wasVillage) {
      ownership.markVillagePlaced(edit);
    }

    // Fill all shared storage, including victim farms, so every displaced stack must be retained.
    for (BlockPos at : village.getBrain().containerPositions()) {
      if (level.getBlockEntity(at) instanceof net.minecraft.world.Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
          container.setItem(slot, new ItemStack(net.minecraft.world.item.Items.COBBLESTONE, 64));
        }
        container.setChanged();
      }
    }
    List<Building> affected = new ArrayList<>(plan.removed());
    affected.add(source);
    BlockPos personalChest = StorageEvacuation.sources(village, List.of(source)).iterator().next();
    var personal = (net.minecraft.world.Container) level.getBlockEntity(personalChest);
    ItemStack namedTool = new ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE);
    namedTool.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
        net.minecraft.network.chat.Component.literal("Displaced builder's pick"));
    namedTool.setDamageValue(73);
    personal.setItem(0, namedTool);
    List<ItemStack> displacedContents = new ArrayList<>();
    for (BlockPos at : StorageEvacuation.sources(village, affected)) {
      var container = (net.minecraft.world.Container) level.getBlockEntity(at);
      for (int slot = 0; slot < container.getContainerSize(); slot++) {
        if (!container.getItem(slot).isEmpty()) {
          displacedContents.add(container.getItem(slot).copy());
        }
      }
    }
    var itemOps = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
    var expectedContents = RedevelopmentItems.CODEC.encodeStart(itemOps, displacedContents).getOrThrow();
    check(village.startRedevelopment(new ConstructionChoice(Buildings.getByName(plan.target()), plan.mode(), plan)),
        "could not start exact proposal");
    check(village.getBuilding(north.getUUID()) != null && village.getBuilding(south.getUUID()) != null,
        "buildings were removed before material commitment");
    check(RedevelopmentPlanner.stillValid(village, plan), "own reserved claim invalidated the proposal");
    StructureInProgress project = village.getCurrentProject();
    SimpleContainer pack = new SimpleContainer(project.requiredMaterials().toArray(ItemStack[]::new));
    check(project.commitFromBuilder(pack, village), "secured recipe did not commit");
    check(project.getProgress() == BuildProgress.DEMOLISHING, "missing demolition phase");
    check(village.getBuilding(north.getUUID()) == null && village.getBuilding(south.getUUID()) == null,
        "removed services still registered");
    check(expectedContents.equals(RedevelopmentItems.CODEC.encodeStart(itemOps, village.pendingRedevelopmentItems()).getOrThrow()),
        "full storage lost or changed displaced contents");
    check(village.getBedAssignment(builderId) == null && village.keepsWorkDuringRedevelopment(builderId),
        "bedless builder was not protected during construction");
    com.quzzar.kithkyn.village.JobClaiming.tick(village, level);
    check(village.getJobAssignment(builderId) != null, "displaced builder lost the job needed to finish construction");
    int structuralIndex = 0;
    while (com.quzzar.kithkyn.village.BlockOwnership.isPlanted(plan.blocks().get(structuralIndex).state())) {
      check(project.demolishStep(village).isEmpty(), "plant removal failed");
      structuralIndex++;
    }
    var promised = plan.blocks().get(structuralIndex);
    BlockPos stolen = BlockPos.of(promised.position());
    int securedCursor = project.getRedevelopment().remainingBlocks();
    level.destroyBlock(stolen, true);
    ownership.clearPlaced(stolen);
    check(!project.demolishStep(village).isEmpty(), "missing committed material was silently credited");
    check(project.getRedevelopment().remainingBlocks() == securedCursor, "missing structural block consumed its queue entry");
    check(expectedContents.equals(RedevelopmentItems.CODEC.encodeStart(itemOps, village.pendingRedevelopmentItems()).getOrThrow()),
        "missing material changed the pending contents or paid salvage early");
    var pausedState = Village.CODEC.encodeStart(NbtOps.INSTANCE, village).getOrThrow();
    Village paused = Village.CODEC.parse(NbtOps.INSTANCE, pausedState).getOrThrow();
    paused.attach(level);
    check(!paused.getCurrentProject().demolishStep(paused).isEmpty(), "reload bypassed missing-material pause");
    // Restore the fixture's secured material; remove the intentional test loot before resuming.
    level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
        new net.minecraft.world.phys.AABB(stolen).inflate(2)).forEach(net.minecraft.world.entity.Entity::discard);
    level.setBlock(stolen, promised.state(), 2 | 16 | 32);
    ownership.markVillagePlaced(stolen);
    int halfway = Math.max(1, project.getRedevelopment().remainingBlocks() / 2);
    for (int index = 0; index < halfway; index++) {
      check(project.demolishStep(village).isEmpty(), "demolition blocked before reload");
    }
    int remaining = project.getRedevelopment().remainingBlocks();
    var saved = Village.CODEC.encodeStart(NbtOps.INSTANCE, village).getOrThrow();
    village = Village.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
    village.attach(level);
    project = village.getCurrentProject();
    check(project.getRedevelopment().remainingBlocks() == remaining, "removal cursor lost on reload");
    check(village.keepsWorkDuringRedevelopment(builderId), "temporary work continuity lost on reload");
    check(expectedContents.equals(RedevelopmentItems.CODEC.encodeStart(itemOps, village.pendingRedevelopmentItems()).getOrThrow()),
        "displaced contents changed across village save/reload");
    for (int index = 0; index < 100_000 && project.getProgress() != BuildProgress.COMPLETE; index++) {
      if (project.getProgress() == BuildProgress.DEMOLISHING) {
        String blocker = project.demolishStep(village);
        check(blocker.isEmpty(), blocker);
      } else if (project.getProgress() == BuildProgress.PREPARING) {
        check(project.prepareStep(village, null), "paid ground work requested more resources");
        project.startBuilding();
      } else {
        project.startBuilding();
        project.updateBuilding();
      }
    }
    check(project.getProgress() == BuildProgress.COMPLETE, "construction did not complete");
    List<MaterialAmount> refund = MaterialAmount.fromStacks(village.pendingRedevelopmentItems());
    project.demolishStep(village);
    project.getRedevelopment().finish(village);
    check(refund.equals(MaterialAmount.fromStacks(village.pendingRedevelopmentItems())), "refund was paid twice");
    check(project.getBuilding().getUUID().equals(source.getUUID()), "upgrade lost source identity");
    check(village.getBuilding(survivingFarm.getUUID()) != null, "surviving farm was removed");
    check(!village.hasClaimed(ground.offset(0, 0, -10)), "removed outer parcel remained claimed");
    // Finish registration, then open real space and exercise the same release method used by village ticks.
    var projectTick = Village.class.getDeclaredMethod("checkCurrentProject");
    projectTick.setAccessible(true);
    projectTick.invoke(village);
    check(village.getCurrentProject() == null, "finished redevelopment was not registered");
    check(village.getBedAssignment(builderId) != null, "displaced builder did not reclaim a replacement bed");
    check(!village.keepsWorkDuringRedevelopment(builderId), "temporary employment exception survived the project");
    var beforeRelease = RedevelopmentItems.CODEC.encodeStart(itemOps, village.pendingRedevelopmentItems()).getOrThrow();
    var release = Village.class.getDeclaredMethod("releaseRedevelopmentItems");
    release.setAccessible(true);
    release.invoke(village);
    check(beforeRelease.equals(RedevelopmentItems.CODEC.encodeStart(itemOps, village.pendingRedevelopmentItems()).getOrThrow()),
        "full-storage retry lost queued items");
    for (BlockPos at : village.getBrain().containerPositions()) {
      if (level.getBlockEntity(at) instanceof net.minecraft.world.Container container) {
        container.clearContent();
      }
    }
    List<ItemStack> retained = village.pendingRedevelopmentItems();
    release.invoke(village);
    check(village.pendingRedevelopmentItems().isEmpty(), "available storage did not receive retained contents");
    List<MaterialAmount> delivered = MaterialAmount.fromStacks(village.getVillageInventory());
    check(MaterialAmount.tally(MaterialAmount.fromStacks(retained)).equals(MaterialAmount.tally(delivered)),
        "delivered contents or salvage totals changed");
    check(village.getVillageInventory().stream().anyMatch(stack -> ItemStack.isSameItemSameComponents(namedTool, stack)),
        "delivered personal tool lost its name or damage");
    release.invoke(village);
    check(MaterialAmount.tally(delivered).equals(MaterialAmount.tally(MaterialAmount.fromStacks(village.getVillageInventory()))),
        "retry delivered items twice");
    Kithkyn.LOGGER.info("[redevelopment-verify] blocks={} net={} recovered={} pendingRefund={}",
        plan.blocks().size(), MaterialAmount.describe(plan.netRequired()), MaterialAmount.describe(plan.salvage()),
        MaterialAmount.describe(refund));
  }

  record Fixture(Village village, BlockPos ground, Building source, Building north, Building south,
      Building survivingFarm) {
  }

  static Fixture fixture(ServerLevel level) {
    for (int x = 4; x <= 14; x++) {
      for (int z = 4; z <= 14; z++) {
        level.getChunk(x, z);
      }
    }
    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 128, 128);
    BlockPos ground = new BlockPos(128, y, 128);
    Village village = new FixtureVillage();
    village.attach(level);
    village.setStyle(VillageStyle.PLAINS);
    Building center = place(village, "village_center_plains_1", ground.offset(-35, 0, -25));
    CompoundTag initial = (CompoundTag) Village.CODEC.encodeStart(NbtOps.INSTANCE, village).getOrThrow();
    initial.put("town_center", UUIDUtil.CODEC.encodeStart(NbtOps.INSTANCE, center.getUUID()).getOrThrow());
    village = Village.CODEC.parse(NbtOps.INSTANCE, initial).getOrThrow();
    village.attach(level);
    place(village, "storehouse_plains_1", ground.offset(-25, 0, 10));
    Building source = place(village, "house_plains_1", ground);
    Building north = place(village, "farm_plains_1", ground.offset(0, 0, -10));
    Building south = place(village, "farm_plains_1", ground.offset(0, 0, 8));
    Building survivingFarm = place(village, "farm_plains_1", ground.offset(25, 0, 0));
    return new Fixture(village, ground, source, north, south, survivingFarm);
  }

  static Building place(Village village, String definition, BlockPos at) {
    List<UUID> before = village.getBuildings().stream().map(Building::getUUID).toList();
    if (village.getTownCenter() == null) {
      Building building = new Building(definition, Rotation.NONE);
      check(new InstantBuildStructure(building, new java.util.Random(1), village.getLevel())
          .seatAtOrigin(at.below(building.getInfo().getSink()), new java.util.HashSet<>()).buildInstantly(),
          "fixture center placement failed");
      ((FixtureVillage) village).register(building);
    } else {
      check(village.devPlaceBuildingAt(definition, at), "fixture placement failed: " + definition);
    }
    Building added = village.getBuildings().stream().filter(building -> !before.contains(building.getUUID()))
        .findFirst().orElseThrow();
    // Fixtures represent previously paid construction; normal free placements retain zero investment.
    added.recordInvestment(MaterialAmount.fromStacks(ConstructionQuote.requiredFor(added.getInfo(), ConstructionMode.FRESH)));
    return added;
  }

  private static final class FixtureVillage extends Village {
    FixtureVillage() {
      super("Redevelopment verification");
    }

    void register(Building building) {
      addBuilding(building);
      rebuildBuildingClaims();
    }
  }

  private static void check(boolean condition, String message) {
    if (!condition) {
      throw new AssertionError(message);
    }
  }
}
