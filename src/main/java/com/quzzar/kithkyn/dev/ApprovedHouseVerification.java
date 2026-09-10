package com.quzzar.kithkyn.dev;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.entities.AgeStage;
import com.quzzar.kithkyn.entities.JobTool;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.entities.ai.GuardNightRoutine;
import com.quzzar.kithkyn.entities.ai.goals.SleepAtNightGoal;
import com.quzzar.kithkyn.entities.ai.goals.StashAtHomeGoal;
import com.quzzar.kithkyn.entities.ai.goals.work.ContainerAccess;
import com.quzzar.kithkyn.entities.ai.goals.work.ConsolidateStep;
import com.quzzar.kithkyn.entities.ai.goals.work.PackLogistics;
import com.quzzar.kithkyn.entities.genetics.Stat;
import com.quzzar.kithkyn.entities.genetics.StatBlock;
import com.quzzar.kithkyn.relationships.RelationshipPair;
import com.quzzar.kithkyn.village.LocationManager;
import com.quzzar.kithkyn.village.FarmedStock;
import com.quzzar.kithkyn.village.GuardDuty;
import com.quzzar.kithkyn.village.GuardRole;
import com.quzzar.kithkyn.village.JobAssignment;
import com.quzzar.kithkyn.village.Occupation;
import com.quzzar.kithkyn.village.PersonalChest;
import com.quzzar.kithkyn.village.buildings.Building;
import com.quzzar.kithkyn.village.buildings.BuildingInfo;
import com.quzzar.kithkyn.village.buildings.Buildings;
import com.quzzar.kithkyn.village.buildings.BuildingUpgrade;
import com.quzzar.kithkyn.village.buildings.MineShaft;
import com.quzzar.kithkyn.village.buildings.WorkerFooting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Native workplace access, room allocation and sleep checks for private structure review datapacks. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class ApprovedHouseVerification {
  private static final BlockPos ORIGIN = new BlockPos(2400, 159, 2400);
  private static List<String> ids;
  private static Rotation selectedRotation;
  private static JsonObject reviewTargets;
  private static JsonObject reviewEntities;
  private static List<UUID> initialEntities = List.of();
  private static final Map<UUID, Vec3> lastLivestockPosition = new HashMap<>();
  private static final Set<UUID> tracedEscapes = new HashSet<>();
  private static Animal livestockProbe;
  private static BlockPos livestockProbeTarget;
  private static int livestockProbeStarted;
  private static boolean checkedEntities;
  private static final List<ApprovedStructureAccess.Person> residents = new ArrayList<>();
  private static final List<Visit> visits = new ArrayList<>();
  private static int visitIndex;
  private static int stationWalks;
  private static int sharedDeposits;
  private static int mineWalks;
  private static int reviewWalks;
  private static BlockPos approach;
  private static BlockPos openingDoor;
  private static ConsolidateStep consolidation;
  private static int transferStarted;
  private static int ticks;
  private static long nightTime = 13000;
  private static int placementIndex;
  private static int residentIndex;
  private static int started;
  private static int routes;
  private static int slept;
  private static int placements;
  private static int stashed;
  private static int previousChestCount;
  private static int failures;
  private static Building building;
  private static ApprovedStructureAccess.VillageFixture village;
  private static ApprovedStructureAccess.Person walker;
  private static BlockPos entrance;
  private static BlockPos bed;
  private static SleepAtNightGoal sleep;
  private static StashAtHomeGoal stash;
  private static BlockPos chest;

  private enum VisitKind { STATION, REVIEW_TARGET, SHARED_CONTAINER, MINE_DESCENT, MINE_RETURN }
  private record Visit(VisitKind kind, BlockPos target, int stationIndex, Occupation occupation) { }

  private ApprovedHouseVerification() { }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (!Boolean.getBoolean("kithkyn.approvedHouses.verify")) return;
    ticks++;
    ServerLevel level = event.getServer().overworld();
    try {
      if (ticks < 40) return;
      if (ticks == 40) {
        ids = Arrays.stream(System.getProperty("kithkyn.approvedHouses.ids", "").split(","))
            .map(String::trim).toList();
        check(ids.stream().noneMatch(String::isEmpty) && ids.stream().distinct().count() == ids.size(),
            "Supply distinct private house IDs in kithkyn.approvedHouses.ids");
        String rotation = System.getProperty("kithkyn.approvedHouses.rotation");
        selectedRotation = rotation == null ? null : Rotation.valueOf(rotation);
        reviewTargets = JsonParser.parseString(System.getProperty("kithkyn.approvedHouses.reviewTargets", "{}"))
            .getAsJsonObject();
        reviewEntities = JsonParser.parseString(System.getProperty("kithkyn.approvedHouses.reviewEntities", "{}"))
            .getAsJsonObject();
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, event.getServer());
        level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, event.getServer());
        for (int x = 147; x <= 153; x++) {
          for (int z = 147; z <= 153; z++) level.setChunkForced(x, z, true);
        }
        event.getServer().tickRateManager().setTickRate(100.0F);
      }
      // Let fresh chunk tickets become entity-visible before the first household claim.
      if (ticks < 45) return;
      level.setDayTime(visits.isEmpty() ? nightTime : 6000L);
      level.updateSkyBrightness();
      if (building == null) {
        if (placementIndex == ids.size() * 4) {
          Kithkyn.LOGGER.info("[approved-house-verify] RESULT {}: {} rotated structures, {} access routes, {} physical room walks and assigned-bed sleeps, {} physical personal-container deposits, {} failed placements; probe size={}; station walks={}, shared deposits={}, mine walks={}, review walks={}",
              failures == 0 ? "PASS" : "FAIL", placements, routes, slept, stashed, failures,
              Integer.getInteger("kithkyn.approvedHouses.probeSize", 18), stationWalks, sharedDeposits, mineWalks, reviewWalks);
          event.getServer().halt(false);
          return;
        }
        prepare(level);
        if (building != null && Boolean.getBoolean("kithkyn.approvedHouses.probeLivestock")) probeLivestockRoutes(level);
      } else {
        if (Boolean.getBoolean("kithkyn.approvedHouses.traceLivestock")) traceLivestock(level);
        if (livestockProbe != null) walkLivestockProbe(level);
        else walk(level);
      }
    } catch (Exception | AssertionError failure) {
      Kithkyn.LOGGER.error("[approved-house-verify] PLACEMENT FAIL index {}", placementIndex, failure);
      failures++;
      if (ids == null) {
        event.getServer().halt(false);
        return;
      }
      if (consolidation != null && walker != null) consolidation.released(walker, visits.get(visitIndex).target());
      consolidation = null;
      if (walker != null) walker.discard();
      residents.forEach(RealPerson::discard);
      residents.clear();
      visits.clear();
      clearInitialEntities(level);
      sleep = null;
      stash = null;
      building = null;
      placementIndex++;
    }
  }

  private static void prepare(ServerLevel level) throws ReflectiveOperationException {
    if (selectedRotation != null && Rotation.values()[placementIndex % 4] != selectedRotation) {
      placementIndex++;
      return;
    }
    BuildingInfo info = Buildings.getByName(ids.get(placementIndex / 4));
    check(info != null, "Missing house review datapack: " + ids.get(placementIndex / 4));
    check(info.validate() == null, "Invalid house metadata: " + info.validate());
    if (Boolean.getBoolean("kithkyn.approvedHouses.coupleRoomsOnly") && info.getCoupleBeds().isEmpty()) {
      placementIndex += 4;
      return;
    }
    placements++;
    Set<UUID> beforeEntities = ApprovedStructureAccess.entityIds(level);
    building = ApprovedStructureAccess.place(level, ORIGIN, info, Rotation.values()[placementIndex % 4]);
    initialEntities = ApprovedStructureAccess.entityIds(level).stream().filter(id -> !beforeEntities.contains(id)).toList();
    checkedEntities = false;
    village = new ApprovedStructureAccess.VillageFixture(level, building, false);
    for (String id : ids) {
      BuildingInfo target = Buildings.getByName(id);
      if (target == null || !info.getName().equals(target.getUpgradesFrom())) continue;
      var placement = BuildingUpgrade.findPlacement(village, target);
      check(placement != null, "Upgrade cannot contain the standing footprint: " + info.getName() + " -> " + id);
      Kithkyn.LOGGER.info("[approved-house-verify] UPGRADE PASS {} -> {}: ground {}, bounds {}", label(), id,
          placement.ground().subtract(ORIGIN), placement.bounds());
    }
    visits.clear();
    var entry = LocationManager.getEntrance(level, building);
    check(entry != null, "No entrance found for " + label());
    entrance = entry.doorstep();
    ApprovedStructureAccess.Person probe = new ApprovedStructureAccess.Person(level, village);
    setProbeBody(probe);
    probe.setNoAi(true);
    if (placements == 1) verifyPartialFloor(level, probe);
    for (long position : info.getBedLocations()) {
      BlockPos target = world(BlockPos.of(position));
      check(level.getBlockState(target).getBlock() instanceof BedBlock, "Missing bed " + target.subtract(ORIGIN));
      ApprovedStructureAccess.moveTo(probe, entrance);
      if (ContainerAccess.approachTo(probe, target, 9.0D,
          standing -> standing.distSqr(target) <= 4.0D) == null) {
        logDiagnosticRoutes(probe);
        throw new AssertionError("No supported sleep approach " + target.subtract(ORIGIN) + " " + label());
      }
      routes++;
    }
    List<Long> containers = new ArrayList<>(info.getPersonalContainerLocations());
    containers.addAll(info.getContainerLocations());
    for (long position : containers) {
      BlockPos target = world(BlockPos.of(position));
      check(level.getBlockEntity(target) instanceof Container, "Missing container " + target.subtract(ORIGIN));
      ApprovedStructureAccess.moveTo(probe, entrance);
      double reach = info.getPersonalContainerLocations().contains(position) ? 9.0D : 6.0D;
      if (ContainerAccess.approachTo(probe, target, reach) == null) {
        throw new AssertionError("No reachable container approach " + target.subtract(ORIGIN) + " " + label());
      }
      routes++;
    }
    int stationIndex = 0;
    for (var station : info.getWorkLocations().entrySet()) {
      BlockPos target = world(BlockPos.of(station.getKey()));
      check(WorkerFooting.canStand(probe, target), "Station lacks supported body clearance "
          + target.subtract(ORIGIN) + " " + label());
      var path = ApprovedStructureAccess.route(probe, entrance, target, 0, 0);
      check(path != null && path.getEndNode() != null && path.getEndNode().asBlockPos().equals(target),
          "Unreachable station " + target.subtract(ORIGIN) + " " + label());
      visits.add(new Visit(VisitKind.STATION, target, stationIndex++, station.getValue()));
      routes++;
    }
    if (reviewTargets.has(info.getName())) {
      for (BlockPos local : BlockPos.CODEC.listOf().parse(JsonOps.INSTANCE, reviewTargets.get(info.getName())).getOrThrow()) {
        BlockPos target = world(local);
        check(WorkerFooting.canStand(probe, target), "Review target lacks supported body clearance " + local);
        visits.add(new Visit(VisitKind.REVIEW_TARGET, target, -1, Occupation.WANDERER));
      }
    }
    for (long position : info.getContainerLocations()) {
      visits.add(new Visit(VisitKind.SHARED_CONTAINER, world(BlockPos.of(position)), -1, Occupation.WANDERER));
    }
    for (MineShaft shaft : MineShaft.of(building)) {
      check(level.getBlockState(shaft.entranceClearance()).getCollisionShape(level, shaft.entranceClearance()).isEmpty(),
          "Mine mouth needs an authored opening at " + shaft.mouth().subtract(ORIGIN));
      if (Boolean.getBoolean("kithkyn.approvedHouses.excavateAuthoredMine")) {
        verifyAuthoredExcavation(level, probe, shaft);
      }
      ApprovedStructureAccess.excavateMine(level, shaft, ORIGIN.getY());
      BlockPos depth = shaft.mouth().offset(new BlockPos(0, -7, 5).rotate(shaft.rotation()));
      visits.add(new Visit(VisitKind.MINE_DESCENT, depth, -1, Occupation.WANDERER));
      visits.add(new Visit(VisitKind.MINE_RETURN, entrance, -1, Occupation.WANDERER));
    }
    probe.discard();
    allocateResidents(level, info);
    for (RealPerson resident : residents) {
      int index = village.getBedAssignment(resident.getUUID()).getBedIndex();
      BlockPos localBed = BlockPos.of(info.getBedLocations().get(index));
      check(info.getBedContainers() != null, "Reviewed rooms must explicitly bind personal storage");
      List<BlockPos> declared = info.getBedContainers().stream().filter(room -> room.bed().equals(localBed))
          .findFirst().map(BuildingInfo.BedContainers::containers).orElse(List.of());
      BlockPos expected = declared.isEmpty() ? null : world(declared.getFirst());
      check(java.util.Objects.equals(expected, PersonalChest.of(resident)), "Wrong personal room storage in " + label());
    }
    check(residents.size() == info.getBedLocations().size(), "Not every bed was allocated");
    Kithkyn.LOGGER.info("[approved-house-verify] ACCESS PASS {}: {} singles, {} couple rooms, {} containers; body {} wide x {} high", label(),
        info.getSingleBedCount(), info.getCoupleBeds().size(), containers.size(), probe.getBbWidth(), probe.getBbHeight());
    if (Boolean.getBoolean("kithkyn.approvedHouses.coupleRoomsOnly")) {
      residents.removeIf(person -> {
        if (info.isCoupleBed(village.getBedAssignment(person.getUUID()).getBedIndex())) return false;
        person.discard();
        return true;
      });
    }
    residentIndex = 0;
    if (visits.isEmpty()) {
      if (residents.isEmpty()) finishPlacement();
      else beginWalk(level);
    } else {
      boolean temporary = residents.isEmpty();
      walker = temporary ? new ApprovedStructureAccess.Person(level, village) : residents.getFirst();
      walker.setLifeStage(AgeStage.ADULT);
      setProbeBody(walker);
      ApprovedStructureAccess.moveTo(walker, entrance);
      ApprovedStructureAccess.enableWalking(walker);
      if (temporary) check(level.addFreshEntity(walker), "Could not spawn workplace walker");
      visitIndex = 0;
      beginVisit();
    }
  }

  /** Claim live-in rooms through actual worker hiring before filling ordinary housing. */
  private static void allocateResidents(ServerLevel level, BuildingInfo info) throws ReflectiveOperationException {
    int generalSingles = info.getSingleBedCount() - info.getWorkerSingleBedCount();
    check(village.getFreeGeneralBedCount() == generalSingles, "Worker rooms leaked into general housing");
    for (BuildingInfo.CoupleBeds pair : info.getCoupleBeds()) {
      if (!info.isWorkerBed(info.getBedLocations().indexOf(pair.first().asLong()))) continue;
      var spouses = marriedResidents(level);
      check(village.getBedAssignment(spouses.getFirst().getUUID()) == null
          && village.getBedAssignment(spouses.get(1).getUUID()) == null, "Worker couple was pre-housed");
      claimWorkplace(spouses.getFirst());
      checkCouple(spouses.getFirst(), spouses.get(1), pair);
    }
    for (int single = 0; single < info.getWorkerSingleBedCount(); single++) {
      var worker = resident(level);
      claimWorkplace(worker);
      var assigned = village.getBedAssignment(worker.getUUID());
      check(assigned != null && info.isWorkerBed(assigned.getBedIndex()) && !info.isCoupleBed(assigned.getBedIndex()),
          "Worker did not receive a reserved single bed");
    }
    for (BuildingInfo.CoupleBeds pair : info.getCoupleBeds()) {
      if (info.isWorkerBed(info.getBedLocations().indexOf(pair.first().asLong()))) continue;
      var spouses = marriedResidents(level);
      check(village.houseCouple(spouses.getFirst().getUUID(), spouses.get(1).getUUID(), building.getUUID()),
          "Could not house general couple in " + label());
      checkCouple(spouses.getFirst(), spouses.get(1), pair);
    }
    List<ApprovedStructureAccess.Person> generalResidents = new ArrayList<>();
    for (int single = 0; single < generalSingles; single++) generalResidents.add(resident(level));
    ApprovedStructureAccess.reconcileBeds(village);
    for (RealPerson person : generalResidents) {
      var assigned = village.getBedAssignment(person.getUUID());
      check(assigned != null && !info.isWorkerBed(assigned.getBedIndex()) && !info.isCoupleBed(assigned.getBedIndex()),
          "General resident took a reserved worker or couple bed");
    }
    Kithkyn.LOGGER.info("[approved-house-verify] ALLOCATION PASS {}: {} worker singles, {} worker couple rooms, {} general singles", label(),
        info.getWorkerSingleBedCount(), info.getWorkerCoupleRoomCount(), generalSingles);
  }

  private static List<ApprovedStructureAccess.Person> marriedResidents(ServerLevel level) {
    var first = resident(level);
    var second = resident(level);
    first.setSpouseId(second.getUUID());
    second.setSpouseId(first.getUUID());
    village.putRelationship(RelationshipPair.create(first.getUUID(), second.getUUID(), 80, 0, 0, false, "", true));
    return List.of(first, second);
  }

  private static void claimWorkplace(ApprovedStructureAccess.Person worker) {
    JobAssignment job = village.getUnassignedJobs().stream()
        .filter(open -> open.getBuildingUUID().equals(building.getUUID())).findFirst()
        .orElseThrow(() -> new AssertionError("More worker rooms than stations in " + label()));
    check(village.canHouseForJob(worker.getUUID(), building.getUUID()), "Unhoused worker cannot claim the authored room");
    village.assignJob(worker.getUUID(), job);
    worker.setOccupation(job.getOccupation());
    check(village.getJobAssignment(worker.getUUID()) != null, "Worker claim did not book its station");
  }

  private static void checkCouple(RealPerson first, RealPerson second, BuildingInfo.CoupleBeds pair) {
    check(village.sharesCoupleHome(first.getUUID(), second.getUUID()), "Spouses did not share one authored room");
    check(LocationManager.getBedLocation(first).equals(world(pair.first()))
        && LocationManager.getBedLocation(second).equals(world(pair.second())), "Wrong couple room assigned");
    check(java.util.Objects.equals(PersonalChest.of(first), PersonalChest.of(second)), "Spouses do not share their room's chest");
  }

  private static ApprovedStructureAccess.Person resident(ServerLevel level) {
    ApprovedStructureAccess.Person person = new ApprovedStructureAccess.Person(level, village);
    person.setNoAi(true);
    person.setLifeStage(AgeStage.ADULT);
    setProbeBody(person);
    person.setOccupation(Occupation.WANDERER);
    village.getPopulation().add(person.getUUID());
    ApprovedStructureAccess.moveTo(person, ORIGIN.offset(-33, building.getPlacedSink() + 1, -33 + residents.size() * 2));
    check(level.addFreshEntity(person), "Could not load the resident before its housing claim");
    residents.add(person);
    return person;
  }

  private static void setProbeBody(RealPerson person) {
    CompoundTag stats = new CompoundTag();
    stats.putInt(Stat.SIZE.getNbtKey(), Integer.getInteger("kithkyn.approvedHouses.probeSize", 18));
    person.setStatBlock(StatBlock.load(stats));
  }

  private static void beginWalk(ServerLevel level) throws ReflectiveOperationException {
    walker = residents.get(residentIndex);
    ApprovedStructureAccess.moveTo(walker, entrance);
    ApprovedStructureAccess.enableWalking(walker);
    check(level.getEntity(walker.getUUID()) == walker, "Resident unloaded before its sleep trip");
    beginHome();
  }

  /** A workplace resident returns from their last task without resetting their physical location. */
  private static void beginHome() throws ReflectiveOperationException {
    if (walker.getOccupation() == Occupation.GUARD) {
      // Retain the post and its reserved bed, choosing one of the guard's real sleeping nights.
      for (int day = 0; day < 1000; day++) {
        nightTime = day * 24000L + 13000L;
        if (GuardNightRoutine.choose(walker.getUUID(), nightTime, true,
            GuardDuty.isCaptain(walker), GuardDuty.of(walker) != null) == GuardNightRoutine.SLEEP) break;
      }
      ServerLevel level = (ServerLevel) walker.level();
      level.setDayTime(nightTime);
      level.updateSkyBrightness();
      check(walker.shouldSleepAtNight(), "Could not select a real guard sleeping night");
    }
    ServerLevel level = (ServerLevel) walker.level();
    level.setDayTime(nightTime);
    level.updateSkyBrightness();
    ApprovedStructureAccess.enableWalking(walker);
    bed = LocationManager.getBedLocation(walker);
    check(!bed.equals(BlockPos.ZERO), "Workplace return lost the resident's bed assignment");
    walker.callToBedCoolDown = 100_000;
    if (!beginStash()) beginSleep();
  }

  /** Exercise the real mining selector against the authored entrance before the navigation replay. */
  private static void verifyAuthoredExcavation(ServerLevel level, RealPerson miner, MineShaft shaft) {
    JobAssignment job = village.getUnassignedJobs().stream()
        .filter(candidate -> candidate.getOccupation() == Occupation.MINER).findFirst().orElseThrow();
    village.assignJob(miner.getUUID(), job);
    miner.setOccupation(Occupation.MINER);
    miner.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_PICKAXE));
    miner.personMainInv.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
    ApprovedStructureAccess.moveTo(miner, LocationManager.getJobLocation(miner));
    Map<BlockPos, net.minecraft.world.level.block.state.BlockState> entranceEdges = new HashMap<>();
    for (int side : new int[]{-1, 1}) {
      for (int z = -1; z <= 2; z++) {
        BlockPos pos = shaft.mouth().offset(new BlockPos(side * (shaft.radius() + 1), -1, z).rotate(shaft.rotation()));
        entranceEdges.put(pos, level.getBlockState(pos));
      }
    }
    var mine = new com.quzzar.kithkyn.entities.ai.goals.work.MineStep();
    BlockPos firstStep = shaft.mouth().offset(new BlockPos(0, -2, 0).rotate(shaft.rotation()));
    check(!level.getBlockState(firstStep).isAir(), "Excavation fixture must start with solid terrain");
    for (int pick = 0; pick < 50 && !level.getBlockState(firstStep).isAir(); pick++) {
      BlockPos stand = mine.select(miner);
      check(stand != null, "Authored mine selected no excavation work " + label());
      check(WorkerFooting.canStand(miner, stand), "Mine selected unsupported footing " + stand);
      ApprovedStructureAccess.moveTo(miner, stand);
      mine.acquired(miner, stand);
      for (int act = 0; act < 2000 && mine.act(miner, stand); act++) { }
      mine.released(miner, stand);
    }
    check(level.getBlockState(firstStep).isAir(), "Miner did not dig the intended first descending step");
    entranceEdges.forEach((pos, state) -> check(level.getBlockState(pos).equals(state),
        "Excavation crossed the authored entrance edge at " + pos));
    village.removePerson(miner.getUUID());
    Kithkyn.LOGGER.info("[approved-house-verify] EXCAVATION PASS {}: width {}, mouth {}",
        label(), shaft.radius() * 2 + 1, shaft.mouth().subtract(ORIGIN));
  }

  /** Assign the real role and kit, then walk to its authored station or a visible storage approach. */
  private static void beginVisit() throws ReflectiveOperationException {
    Visit visit = visits.get(visitIndex);
    started = ticks;
    approach = visit.target();
    openingDoor = null;
    consolidation = null;
    transferStarted = -1;
    if (visit.kind() == VisitKind.STATION) {
      village.assignJob(walker.getUUID(), new JobAssignment(walker.getUUID(), visit.occupation(),
          building.getUUID(), visit.stationIndex()));
      walker.setOccupation(visit.occupation());
      walker.issueStartingKit();
      ApprovedStructureAccess.enableWalking(walker);
      if (visit.occupation() == Occupation.GUARD
          && building.getInfo().getGuardRole(visit.stationIndex()) == GuardRole.CROSSBOW_POST) {
        GuardDuty duty = GuardDuty.of(walker);
        check(duty != null && duty.position().equals(visit.target()) && duty.ranged() && duty.backupSword(),
            "Crossbow station lacks its fixed ranged duty");
        check(JobTool.of(walker) == JobTool.CROSSBOW && walker.getMainHandItem().is(Items.CROSSBOW),
            "Crossbow post did not receive its crossbow");
        // This probe reuses one resident across stations; a previous sword post can leave another sword.
        check(walker.personMainInv.countItem(Items.STONE_SWORD) >= 1, "Crossbow post lacks its backup sword");
      }
    } else if (visit.kind() == VisitKind.SHARED_CONTAINER) {
      approach = ContainerAccess.approachTo(walker, visit.target(), 6.0D);
      if (approach == null) {
        for (BlockPos candidate : BlockPos.betweenClosed(visit.target().offset(-2, -1, -2), visit.target().offset(2, 1, 2))) {
          Vec3 feet = WorkerFooting.standingPosition(walker, candidate);
          if (feet == null || !ContainerAccess.canReach(walker, feet.add(0, walker.getEyeHeight(), 0), visit.target(), 6.0D)) continue;
          var path = walker.getNavigation().createPath(candidate.immutable(), 0);
          Kithkyn.LOGGER.info("[approved-house-verify] CONTAINER DIAGNOSTIC from {} to {}: reachable={}, end={}, nodes={}",
              walker.position().subtract(Vec3.atLowerCornerOf(ORIGIN)), candidate.subtract(ORIGIN),
              path != null && path.canReach(), path == null || path.getEndNode() == null ? null : path.getEndNode().asBlockPos().subtract(ORIGIN),
              path == null ? 0 : path.getNodeCount());
        }
      }
      check(approach != null, "No physical shared-container approach " + visit.target().subtract(ORIGIN));
      var state = walker.level().getBlockState(approach);
      if (state.getBlock() instanceof DoorBlock && !state.getValue(DoorBlock.OPEN)) openingDoor = approach;
      previousChestCount = PackLogistics.containerAt(walker, visit.target()).countItem(Items.GOLD_NUGGET);
      // Arrange two reviewed delivery stacks; production moves one every thirty ticks.
      walker.personMainInv.clearContent();
      walker.personMainInv.setItem(0, new ItemStack(Items.GOLD_NUGGET, 4));
      walker.personMainInv.setItem(1, new ItemStack(Items.GOLD_NUGGET, 4));
      consolidation = new ConsolidateStep();
      var phase = ConsolidateStep.class.getDeclaredField("phase");
      phase.setAccessible(true);
      phase.set(consolidation, Arrays.stream(phase.getType().getEnumConstants())
          .filter(value -> value.toString().equals("DELIVER")).findFirst().orElseThrow());
      var selectedApproach = ConsolidateStep.class.getDeclaredField("approach");
      selectedApproach.setAccessible(true);
      selectedApproach.set(consolidation, approach);
    }
  }

  private static void walkVisit(ServerLevel level) throws ReflectiveOperationException {
    Visit visit = visits.get(visitIndex);
    boolean handArrival = consolidation != null && consolidation.inReach(walker, visit.target());
    if (consolidation != null) {
      check(transferStarted < 0 || handArrival, "Quartermaster lost sustained hand access after "
          + (ticks - transferStarted) + " ticks at " + visit.target().subtract(ORIGIN) + "; " + movementDetails());
      BlockPos resolved = consolidation.positionOf(visit.target());
      if (!resolved.equals(approach)) {
        if (openingDoor != null) {
          check(level.getBlockState(openingDoor).getValue(DoorBlock.OPEN), "Closet approach changed before opening its door");
          Kithkyn.LOGGER.info("[approved-house-verify] DOOR PASS {}: real walk opened closet {} then selected hand stance {}",
              label(), openingDoor.subtract(ORIGIN), resolved.subtract(ORIGIN));
          openingDoor = null;
        }
        approach = resolved;
        walker.getNavigation().stop();
      }
    }
    boolean arrived = walker.onGround() && walker.distanceToSqr(Vec3.atBottomCenterOf(approach)) <= 0.75D
        && Math.abs(walker.getY() - approach.getY()) < 0.51D;
    if (visit.kind() == VisitKind.SHARED_CONTAINER) {
      arrived &= handArrival;
    }
    if (openingDoor != null) arrived &= level.getBlockState(openingDoor).getValue(DoorBlock.OPEN);
    if (arrived) {
      walker.getNavigation().stop();
      if (visit.kind() == VisitKind.SHARED_CONTAINER) {
        if (openingDoor != null) {
          var state = level.getBlockState(openingDoor);
          check(state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.OPEN),
              "The real walk did not open the planned closet door");
          Kithkyn.LOGGER.info("[approved-house-verify] DOOR PASS {}: real walk opened closet {}",
              label(), openingDoor.subtract(ORIGIN));
        }
        check(ContainerAccess.canReach(walker, walker.getEyePosition(), visit.target(), 6.0D),
            "Shared-container transfer would cross a wall or ceiling");
        Container container = PackLogistics.containerAt(walker, visit.target());
        if (transferStarted < 0) transferStarted = ticks;
        boolean working = consolidation.act(walker, visit.target());
        int elapsed = ticks - transferStarted;
        int deposited = container.countItem(Items.GOLD_NUGGET) - previousChestCount;
        check(deposited == (elapsed < 30 ? 0 : elapsed < 60 ? 4 : 8),
            "Quartermaster transfer did not preserve its thirty-tick cadence");
        if (working) return;
        check(elapsed >= 60 && walker.personMainInv.isEmpty()
            && container.countItem(Items.GOLD_NUGGET) == previousChestCount + 8,
            "Shared-container transfer lost or duplicated items");
        consolidation.released(walker, visit.target());
        consolidation = null;
        Kithkyn.LOGGER.info("[approved-house-verify] TRANSFER PASS {}: sustained hand access for {} ticks and two real quartermaster transfers at {}",
            label(), elapsed, visit.target().subtract(ORIGIN));
        sharedDeposits++;
      } else if (visit.kind() == VisitKind.STATION) {
        stationWalks++;
      } else if (visit.kind() == VisitKind.REVIEW_TARGET) {
        reviewWalks++;
      } else {
        mineWalks++;
      }
      Kithkyn.LOGGER.info("[approved-house-verify] {} PASS {} target {} in {} ticks, reached {}", visit.kind(), label(),
          visit.target().subtract(ORIGIN), ticks - started, walker.position().subtract(Vec3.atLowerCornerOf(ORIGIN)));
      if (++visitIndex < visits.size()) {
        beginVisit();
      } else {
        visits.clear();
        if (residents.isEmpty()) {
          walker.discard();
          finishPlacement();
        } else beginHome();
      }
      return;
    }
    check(ticks - started < 1800, "Physical " + visit.kind() + " stalled " + label() + " at "
        + walker.position().subtract(Vec3.atLowerCornerOf(ORIGIN)) + " toward " + approach.subtract(ORIGIN)
        + "; " + movementDetails());
    if (ticks % 10 == 0 || walker.getNavigation().isDone()) {
      walker.getNavigation().moveTo(walker.getNavigation().createPath(approach, 0), 0.6D);
    }
  }

  /** Continue naturally from the personal-container visit into the assigned bed. */
  private static void beginSleep() {
    sleep = new SleepAtNightGoal(walker);
    check(sleep.canUse(), "The housed adult cannot start sleeping");
    sleep.start();
    started = ticks;
  }

  /** Walk home to the personal container using the ordinary keepsake transfer goal. */
  private static boolean beginStash() throws ReflectiveOperationException {
    chest = PersonalChest.of(walker);
    if (chest != null) {
      Container container = PersonalChest.container(walker, chest);
      check(container != null, "Personal container disappeared in " + label());
      previousChestCount = container.countItem(Items.GOLD_NUGGET);
      walker.personMainInv.setItem(0, new ItemStack(Items.GOLD_NUGGET, 4));
      // Arrange the completed keepsake decision without starting an unrelated LLM request or restock.
      var keeping = RealPerson.class.getDeclaredField("keepingForHome");
      keeping.setAccessible(true);
      keeping.set(walker, Set.of(Items.GOLD_NUGGET));
      stash = new StashAtHomeGoal(walker);
      check(stash.canUse(), "The housed adult cannot start their personal-container trip");
      stash.start();
      started = ticks;
      return true;
    }
    return false;
  }

  private static void walk(ServerLevel level) throws ReflectiveOperationException {
    if (!checkedEntities) {
      verifyEntities(level);
      checkedEntities = true;
    }
    if (!visits.isEmpty()) {
      walkVisit(level);
      return;
    }
    if (stash != null) {
      // Match the ordinary selector cadence for this goal's headway and give-up budgets.
      if ((ticks & 1) == 0) stash.tick();
      if (walker.personMainInv.countItem(Items.GOLD_NUGGET) == 0) {
        check(ContainerAccess.canReach(walker, walker.getEyePosition(), chest, 9.0D),
            "Personal-container transfer crossed a wall or ceiling in " + label());
        check(PersonalChest.container(walker, chest).countItem(Items.GOLD_NUGGET) == previousChestCount + 4,
            "Personal-container transfer lost or duplicated items in " + label());
        Kithkyn.LOGGER.info("[approved-house-verify] STASH PASS {} chest {} in {} ticks, reached from {}", label(),
            chest.subtract(ORIGIN), ticks - started, walker.position().subtract(Vec3.atLowerCornerOf(ORIGIN)));
        stash.stop();
        stash = null;
        stashed++;
        beginSleep();
      } else {
        if (!stash.canContinueToUse() || ticks - started >= 3600) {
          throw new AssertionError("Physical personal-container access stalled " + label() + " at "
                + walker.position().subtract(Vec3.atLowerCornerOf(ORIGIN)) + " toward " + chest.subtract(ORIGIN)
                + " using StashAtHomeGoal; " + movementDetails());
        }
      }
      return;
    }
    Vec3 beforeSleep = walker.position();
    sleep.tick();
    if (walker.isSleeping()) {
      check(walker.getSleepingPos().orElseThrow().equals(bed), "Slept in the wrong bed in " + label());
      Kithkyn.LOGGER.info("[approved-house-verify] SLEEP PASS {} bed {} in {} ticks, approached from {}", label(),
          bed.subtract(ORIGIN), ticks - started, beforeSleep.subtract(Vec3.atLowerCornerOf(ORIGIN)));
      sleep.stop();
      slept++;
      finishWalk(level);
      return;
    }
    if (ticks - started >= 1600) throw new AssertionError("Physical room access stalled " + label() + " at "
        + walker.position().subtract(Vec3.atLowerCornerOf(ORIGIN)) + " toward " + bed.subtract(ORIGIN)
        + " using SleepAtNightGoal; " + movementDetails());
  }

  /** Capture movement state only after a failed trip, before discarding its probe. */
  private static String movementDetails() {
    var path = walker.getNavigation().getPath();
    List<String> nodes = new ArrayList<>();
    if (path != null) {
      for (int i = Math.max(0, path.getNextNodeIndex() - 1);
          i < Math.min(path.getNodeCount(), path.getNextNodeIndex() + 4); i++) {
        nodes.add(i + ":" + path.getNode(i).asBlockPos().subtract(ORIGIN));
      }
    }
    var doors = walker.goalSelector.getAvailableGoals().stream()
        .filter(goal -> goal.getGoal() instanceof OpenDoorGoal)
        .map(goal -> "running=" + goal.isRunning() + ",canUse=" + goal.getGoal().canUse()).toList();
    return "collision=" + walker.horizontalCollision + ",canOpenDoors="
        + ((GroundPathNavigation) walker.getNavigation()).canOpenDoors() + ",doorGoals=" + doors
        + ",pathDone=" + walker.getNavigation().isDone() + ",nextNode="
        + (path == null ? "none" : path.getNextNodeIndex()) + ",nearbyNodes=" + nodes;
  }

  private static void finishWalk(ServerLevel level) throws ReflectiveOperationException {
    walker.discard();
    residentIndex++;
    if (residentIndex == residents.size()) {
      finishPlacement();
    } else {
      beginWalk(level);
    }
  }

  private static void finishPlacement() {
    verifyEntities(village.getLevel());
    clearInitialEntities(village.getLevel());
    residents.forEach(RealPerson::discard);
    residents.clear();
    visits.clear();
    walker = null;
    building = null;
    placementIndex++;
  }

  /** Verify exactly the authored initial population, including its farmed mark and pen boundaries. */
  private static void verifyEntities(ServerLevel level) {
    if (!reviewEntities.has(building.getName())) return;
    JsonObject specification = reviewEntities.getAsJsonObject(building.getName());
    Map<String, Integer> expected = new HashMap<>();
    specification.getAsJsonObject("counts").entrySet().forEach(entry -> expected.put(entry.getKey(), entry.getValue().getAsInt()));
    BoundingBox pen = animalBounds(specification);
    Map<String, Integer> actual = new HashMap<>();
    for (UUID id : initialEntities) {
      var entity = level.getEntity(id);
      check(entity != null && entity.isAlive(), "An authored entity disappeared in " + label());
      actual.merge(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(), 1, Integer::sum);
      if (entity instanceof Animal animal) {
        check(FarmedStock.isStock(animal) && FarmedStock.isFarmed(animal), "Authored livestock was not marked as farmed");
        check(pen != null && pen.isInside(animal.blockPosition()), "Livestock escaped its authored pen at "
            + animal.blockPosition().subtract(ORIGIN) + " " + label());
      }
    }
    check(actual.equals(expected), "Wrong initial entity population in " + label() + ": " + actual + " expected " + expected);
    Kithkyn.LOGGER.info("[approved-house-verify] ENTITIES PASS {}: {} retained and contained", label(), actual);
  }

  private static BoundingBox animalBounds(JsonObject specification) {
    if (specification.has("animal_bounds") && !specification.get("animal_bounds").isJsonNull()) {
      var bounds = specification.getAsJsonArray("animal_bounds");
      check(bounds.size() == 6, "Animal review bounds require six coordinates");
      return BoundingBox.fromCorners(world(new BlockPos(bounds.get(0).getAsInt(), bounds.get(1).getAsInt(), bounds.get(2).getAsInt())),
          world(new BlockPos(bounds.get(3).getAsInt(), bounds.get(4).getAsInt(), bounds.get(5).getAsInt())));
    }
    return null;
  }

  /** Optional first-crossing evidence distinguishes an open gate from a climbable pen edge. */
  private static void traceLivestock(ServerLevel level) {
    if (!reviewEntities.has(building.getName())) return;
    BoundingBox pen = animalBounds(reviewEntities.getAsJsonObject(building.getName()));
    if (pen == null) return;
    for (UUID id : initialEntities) {
      var entity = level.getEntity(id);
      if (!(entity instanceof Animal animal)) continue;
      Vec3 position = animal.position().subtract(Vec3.atLowerCornerOf(ORIGIN));
      Vec3 previous = lastLivestockPosition.put(id, position);
      if (!pen.isInside(animal.blockPosition()) && tracedEscapes.add(id)) {
        List<String> gates = new ArrayList<>();
        for (BlockPos candidate : BlockPos.betweenClosed(pen.minX() - 1, pen.minY(), pen.minZ() - 1,
            pen.maxX() + 1, pen.maxY(), pen.maxZ() + 1)) {
          var state = level.getBlockState(candidate);
          if (state.getBlock() instanceof FenceGateBlock) gates.add(candidate.subtract(ORIGIN) + ":" + state);
        }
        Kithkyn.LOGGER.error("[approved-house-verify] LIVESTOCK FIRST EXIT {} {} tick {}: previous={}, current={}, velocity={}, grounded={}, below={}, gates={}",
            label(), id, ticks, previous, position, animal.getDeltaMovement(), animal.onGround(),
            level.getBlockState(animal.blockPosition().below()), gates);
      }
    }
  }

  private static void clearInitialEntities(ServerLevel level) {
    livestockProbe = null;
    livestockProbeTarget = null;
    lastLivestockPosition.clear();
    tracedEscapes.clear();
    for (UUID id : initialEntities) {
      var entity = level.getEntity(id);
      if (entity != null) entity.discard();
    }
    initialEntities = List.of();
  }

  /** An optional native sheep route tests the enclosure while its authored gate remains closed. */
  private static void probeLivestockRoutes(ServerLevel level) {
    if (!reviewEntities.has(building.getName())) return;
    BoundingBox pen = animalBounds(reviewEntities.getAsJsonObject(building.getName()));
    if (pen == null) return;
    var blocks = JsonParser.parseString(System.getProperty("kithkyn.approvedHouses.diagnosticTargets", "[]")).getAsJsonArray();
    for (var value : blocks) {
      BlockPos position = world(BlockPos.CODEC.parse(JsonOps.INSTANCE, value).getOrThrow());
      var state = level.getBlockState(position);
      Kithkyn.LOGGER.info("[approved-house-verify] PEN BLOCK {} {}: {}, collision={}", label(),
          position.subtract(ORIGIN), state, state.getCollisionShape(level, position).toAabbs());
    }
    checkClosedGates(level, pen);
    int shortestExit = Integer.MAX_VALUE;
    for (UUID id : initialEntities) {
      if (!(level.getEntity(id) instanceof Animal animal)) continue;
      int x = (pen.minX() + pen.maxX()) / 2;
      int z = (pen.minZ() + pen.maxZ()) / 2;
      int y = animal.blockPosition().getY();
      for (BlockPos target : List.of(new BlockPos(x, y, pen.minZ() - 2), new BlockPos(x, y, pen.maxZ() + 2),
          new BlockPos(pen.minX() - 2, y, z), new BlockPos(pen.maxX() + 2, y, z))) {
        var path = animal.getNavigation().createPath(target, 0);
        List<BlockPos> nodes = new ArrayList<>();
        if (path != null) for (int i = 0; i < path.getNodeCount(); i++) nodes.add(path.getNode(i).asBlockPos().subtract(ORIGIN));
        Kithkyn.LOGGER.info("[approved-house-verify] PEN ROUTE {} {} from {} toward {}: reached={}, nodes={}", label(), id,
            animal.position().subtract(Vec3.atLowerCornerOf(ORIGIN)), target.subtract(ORIGIN), path != null && path.canReach(), nodes);
        // An ordinary partial endpoint can already lie outside the enclosure.
        if (path != null && path.getEndNode() != null && !pen.isInside(path.getEndNode().asBlockPos())
            && path.getNodeCount() < shortestExit) {
          livestockProbe = animal;
          livestockProbeTarget = path.getEndNode().asBlockPos();
          shortestExit = path.getNodeCount();
        }
      }
    }
    if (livestockProbe == null) return;
    for (UUID id : initialEntities) {
      if (level.getEntity(id) instanceof Animal animal) animal.setNoAi(animal != livestockProbe);
    }
    walker.setNoAi(true);
    livestockProbe.goalSelector.removeAllGoals(goal -> true);
    livestockProbe.targetSelector.removeAllGoals(goal -> true);
    livestockProbeStarted = ticks;
    livestockProbe.getNavigation().moveTo(livestockProbe.getNavigation().createPath(livestockProbeTarget, 0), 1.0D);
  }

  private static void walkLivestockProbe(ServerLevel level) {
    BoundingBox pen = animalBounds(reviewEntities.getAsJsonObject(building.getName()));
    checkClosedGates(level, pen);
    if (ticks % 10 == 0) Kithkyn.LOGGER.info("[approved-house-verify] PEN WALK {} {}: position={}, velocity={}, grounded={}",
        label(), livestockProbe.getUUID(), livestockProbe.position().subtract(Vec3.atLowerCornerOf(ORIGIN)),
        livestockProbe.getDeltaMovement(), livestockProbe.onGround());
    check(pen.isInside(livestockProbe.blockPosition()), "Closed-gate native sheep walk exited authored bounds at "
        + livestockProbe.position().subtract(Vec3.atLowerCornerOf(ORIGIN)));
    check(ticks - livestockProbeStarted < 1800, "Closed-gate planned sheep route physically stalled at "
        + livestockProbe.position().subtract(Vec3.atLowerCornerOf(ORIGIN)));
    if (ticks % 20 == 0 || livestockProbe.getNavigation().isDone()) {
      livestockProbe.getNavigation().moveTo(livestockProbe.getNavigation().createPath(livestockProbeTarget, 0), 1.0D);
    }
  }

  private static void checkClosedGates(ServerLevel level, BoundingBox pen) {
    for (BlockPos position : BlockPos.betweenClosed(pen.minX() - 1, pen.minY(), pen.minZ() - 1,
        pen.maxX() + 1, pen.maxY(), pen.maxZ() + 1)) {
      var state = level.getBlockState(position);
      check(!(state.getBlock() instanceof FenceGateBlock) || !state.getValue(FenceGateBlock.OPEN),
          "Closed-gate probe found an open gate at " + position.subtract(ORIGIN));
    }
  }

  /** Optional failure-only stage probes; physical acceptance still requires the unbroken resident trip. */
  private static void logDiagnosticRoutes(RealPerson probe) {
    var targets = JsonParser.parseString(System.getProperty("kithkyn.approvedHouses.diagnosticTargets", "[]")).getAsJsonArray();
    BlockPos previous = entrance;
    for (var element : targets) {
      BlockPos target = world(BlockPos.CODEC.parse(JsonOps.INSTANCE, element).getOrThrow());
      Vec3 start = WorkerFooting.standingPosition(probe, previous);
      if (start == null) start = Vec3.atBottomCenterOf(previous);
      probe.moveTo(start.x, start.y, start.z, 0, 0);
      var path = probe.getNavigation().createPath(target, 0);
      List<BlockPos> nodes = new ArrayList<>();
      if (path != null) for (int index = 0; index < path.getNodeCount(); index++) {
        nodes.add(path.getNode(index).asBlockPos().subtract(ORIGIN));
      }
      Vec3 feet = WorkerFooting.standingPosition(probe, target);
      Kithkyn.LOGGER.info("[approved-house-verify] DIAGNOSTIC {} -> {}: standing={}, reached={}, nodes={}",
          previous.subtract(ORIGIN), target.subtract(ORIGIN),
          feet == null ? null : feet.subtract(Vec3.atLowerCornerOf(ORIGIN)), path != null && path.canReach(), nodes);
      previous = target;
    }
  }

  /** Test actual collision shapes at the largest adult size, including rejection of a shorter room. */
  private static void verifyPartialFloor(ServerLevel level, RealPerson person) {
    BlockPos floor = ORIGIN.offset(-28, 12, -28);
    BlockPos node = floor.above();
    level.setBlock(floor, Blocks.SPRUCE_SLAB.defaultBlockState(), 2);
    level.setBlock(floor.above(2), Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP), 2);
    Vec3 standing = WorkerFooting.standingPosition(person, node);
    check(standing != null && standing.y == floor.getY() + 0.5D,
        "Two-block adult cannot stand between the half-slab floor and top-slab ceiling");
    level.setBlock(floor.above(2), Blocks.SPRUCE_SLAB.defaultBlockState(), 2);
    check(WorkerFooting.standingPosition(person, node) == null,
        "Adult admitted into a room with only 1.5 blocks of clearance");
    level.setBlock(floor.above(2), Blocks.AIR.defaultBlockState(), 2);
    level.setBlock(floor, Blocks.OAK_FENCE.defaultBlockState(), 2);
    check(WorkerFooting.standingPosition(person, node) == null, "Fence tip admitted as standing ground");
    level.setBlock(floor, Blocks.WATER.defaultBlockState(), 2);
    check(WorkerFooting.standingPosition(person, node) == null, "Water admitted as dry standing ground");
    level.setBlock(floor, Blocks.STONE.defaultBlockState(), 2);
    level.setBlock(floor.east(), Blocks.STONE.defaultBlockState(), 2);
    level.setBlock(floor.east().above(), Blocks.SPRUCE_SLAB.defaultBlockState(), 2);
    level.setBlock(floor.above(3), Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP), 2);
    ApprovedStructureAccess.moveTo(person, node);
    var halfStep = person.getNavigation().createPath(floor.east().above(2), 0);
    check(halfStep != null && halfStep.canReach(), "Clear half-block step below a top-slab bridge was rejected");
    level.setBlock(floor.above(3), Blocks.SPRUCE_SLAB.defaultBlockState(), 2);
    var blockedStep = person.getNavigation().createPath(floor.east().above(2), 0);
    check(blockedStep == null || !blockedStep.canReach(), "Half-block step admitted through a lowered bridge");
    for (BlockPos position : BlockPos.betweenClosed(floor, floor.east().above(3))) {
      level.setBlock(position, Blocks.AIR.defaultBlockState(), 2);
    }
    verifyClosetPlanning(level, person, floor.east(5));
    verifyTrapdoorCounterPlanning(level, person, floor.east(5));
    Kithkyn.LOGGER.info("[approved-house-verify] FOOTING PASS: fractional floor, exact body clearance, low ceiling/fence/water rejection, clear half-step and blocked low-bridge step");
  }

  /** Counter panels must obstruct a low passage while an open side panel leaves its aperture usable. */
  private static void verifyTrapdoorCounterPlanning(ServerLevel level, RealPerson person, BlockPos origin) {
    for (BlockPos position : BlockPos.betweenClosed(origin, origin.offset(3, 3, 2))) {
      BlockPos local = position.subtract(origin);
      boolean solid = local.getY() == 0 || local.getY() == 3
          || local.getX() > 0 && (local.getZ() != 1 || local.getX() == 3);
      level.setBlock(position, solid ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
    }
    BlockPos counter = origin.offset(1, 1, 1);
    BlockPos target = origin.offset(2, 1, 1);
    var panel = Blocks.OAK_TRAPDOOR.defaultBlockState()
        .setValue(TrapDoorBlock.HALF,
            Half.TOP)
        .setValue(TrapDoorBlock.FACING, net.minecraft.core.Direction.NORTH);
    level.setBlock(counter, panel, 2);
    ApprovedStructureAccess.moveTo(person, origin.offset(0, 1, 1));
    var blocked = person.getNavigation().createPath(target, 0);
    check(blocked == null || !blocked.canReach(), "Closed market counter admitted a route through its panel");
    level.setBlock(counter, panel.setValue(TrapDoorBlock.OPEN, true), 2);
    var open = person.getNavigation().createPath(target, 0);
    check(open != null && open.canReach(), "Open side trapdoor blocked its clear aperture");
    for (BlockPos position : BlockPos.betweenClosed(origin, origin.offset(3, 3, 2))) {
      level.setBlock(position, Blocks.AIR.defaultBlockState(), 2);
    }
    Kithkyn.LOGGER.info("[approved-house-verify] COUNTER PASS: closed panel rejected, open side aperture reachable");
  }

  /** Closet planning may open one wooden door, but must never bypass iron or the wall behind it. */
  private static void verifyClosetPlanning(ServerLevel level, RealPerson person, BlockPos origin) {
    for (BlockPos position : BlockPos.betweenClosed(origin, origin.offset(3, 3, 2))) {
      BlockPos local = position.subtract(origin);
      boolean solid = local.getY() == 0 || local.getY() == 3
          || local.getX() > 0 && (local.getZ() != 1 || local.getX() == 3);
      level.setBlock(position, solid ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
    }
    BlockPos door = origin.offset(1, 1, 1);
    BlockPos target = origin.offset(2, 1, 1);
    level.setBlock(target, Blocks.CHEST.defaultBlockState(), 2);
    for (var block : List.of(Blocks.SPRUCE_DOOR, Blocks.IRON_DOOR)) {
      var lower = block.defaultBlockState().setValue(DoorBlock.FACING, net.minecraft.core.Direction.WEST);
      level.setBlock(door, lower, 2);
      level.setBlock(door.above(), lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 2);
      ApprovedStructureAccess.moveTo(person, origin.offset(0, 1, 1));
      BlockPos approach = ContainerAccess.approachTo(person, target, 6.0D);
      check(block == Blocks.SPRUCE_DOOR ? door.equals(approach) : approach == null,
          "Wrong closet approach for " + BuiltInRegistries.BLOCK.getKey(block) + ": " + approach);
      check(!ContainerAccess.canReach(person, person.getEyePosition(), target, 6.0D),
          "A closed closet door allowed an actual item transfer");
    }
    var wooden = Blocks.SPRUCE_DOOR.defaultBlockState().setValue(DoorBlock.FACING, net.minecraft.core.Direction.WEST);
    level.setBlock(door, wooden, 2);
    level.setBlock(door.above(), wooden.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 2);
    var opening = person.goalSelector.getAvailableGoals().stream().map(goal -> goal.getGoal())
        .filter(goal -> goal instanceof OpenDoorGoal).findFirst().orElseThrow();
    for (int attempt = 0; attempt < 2; attempt++) {
      person.getNavigation().stop();
      var requested = person.getNavigation().createPath(door, 0);
      check(requested != null && requested.canReach(), "Fresh closet doorway request was unreachable");
      person.getNavigation().moveTo(requested, 0.5D);
      requested.setNextNodeIndex(requested.getNodeCount());
      check(opening.canUse(), "A fresh completed closet request could not open its door");
      opening.start();
      check(level.getBlockState(door).getValue(DoorBlock.OPEN), "Endpoint goal did not open its wooden door");
      BlockPos resolved = ContainerAccess.resolveOpenedDoor(person, target, door, 6.0D);
      Vec3 stance = WorkerFooting.standingPosition(person, resolved);
      check(!door.equals(resolved) && stance != null
              && ContainerAccess.canReach(person, stance.add(0, person.getEyeHeight(), 0), target, 6.0D),
          "Opened closet did not resolve to a supported visible hand stance");
      opening.stop();
      check(!opening.canUse(), "A retained completed path would repeatedly reopen the closet door");
    }
    person.getNavigation().stop();
    level.setBlock(target.above(), Blocks.STONE.defaultBlockState(), 2);
    check(ContainerAccess.approachTo(person, target, 6.0D) == null,
        "Closet planning ignored a solid block beyond its door");
    for (BlockPos position : BlockPos.betweenClosed(origin, origin.offset(3, 3, 2))) {
      level.setBlock(position, Blocks.AIR.defaultBlockState(), 2);
    }
    Kithkyn.LOGGER.info("[approved-house-verify] CLOSET PASS: wooden approach, closed-door hand rejection, iron/blocked-interior rejection, endpoint opens once and fresh request reopens");
  }

  private static BlockPos world(BlockPos local) { return ORIGIN.offset(local.rotate(building.getRotation())); }
  private static String label() { return building.getName() + " " + building.getRotation(); }
  private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
