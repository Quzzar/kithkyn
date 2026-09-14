package com.quzzar.kithkyn.dev;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.entities.AgeStage;
import com.quzzar.kithkyn.entities.ClericPotions;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.village.JobAssignment;
import com.quzzar.kithkyn.village.JobClaiming;
import com.quzzar.kithkyn.village.LocationManager;
import com.quzzar.kithkyn.village.Occupation;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.buildings.Building;
import com.quzzar.kithkyn.village.buildings.BuildingInfo;
import com.quzzar.kithkyn.village.buildings.Buildings;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Opt-in proof that the ordinary occupation goals complete physical work. This is deliberately
 * stronger than calling a work step directly: every worker claims a vacancy from a real building,
 * receives the normal starting kit, reloads the normal occupation goals, and walks under ordinary
 * entity ticks. Run only in a disposable world with {@code -Dkithkyn.occupationWork.verify=true}.
 */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class OccupationWorkVerification {
  private static final String PREFIX = "[occupation-work-verify]";
  private static final BlockPos BAKER_ORIGIN = new BlockPos(-9000, 150, -9000);
  private static final BlockPos SMITH_ORIGIN = new BlockPos(-8900, 150, -9000);
  private static final BlockPos CLERIC_ORIGIN = new BlockPos(-8800, 150, -9000);
  private static final int TIMEOUT_TICKS = 6000;

  private static int ticks;
  private static boolean finished;
  private static WorkerRun baker;
  private static WorkerRun smith;
  private static WorkerRun cleric;
  private static ApprovedStructureAccess.Person patient;
  private static BlockPos patientStart;
  private static boolean patientWalked;
  private static boolean patientTended;

  private OccupationWorkVerification() { }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (!Boolean.getBoolean("kithkyn.occupationWork.verify") || finished || ++ticks < 40) return;
    try {
      ServerLevel level = event.getServer().overworld();
      if (baker == null) setup(level, event);
      observe(level);
      if (complete()) {
        finished = true;
        Kithkyn.LOGGER.info("{} RESULT PASS: normal vacancy claims and registered goals completed "
            + "physical baker wheat-to-bread, blacksmith iron-to-bucket, and cleric brew-and-tend loops", PREFIX);
        event.getServer().halt(false);
      } else if (ticks > TIMEOUT_TICKS) {
        throw new AssertionError("Work timed out: " + status());
      }
    } catch (Exception | AssertionError failure) {
      finished = true;
      Kithkyn.LOGGER.error(PREFIX + " RESULT FAIL", failure);
      event.getServer().halt(false);
    }
  }

  private static void setup(ServerLevel level, ServerTickEvent.Post event) {
    level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, event.getServer());
    level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, event.getServer());
    level.setDayTime(6000);
    level.updateSkyBrightness();
    event.getServer().tickRateManager().setTickRate(100.0F);

    baker = setupWorker(level, BAKER_ORIGIN, "bakery_birch_forest_1", Occupation.BAKER,
        Items.WHEAT, 6);
    smith = setupWorker(level, SMITH_ORIGIN, "blacksmith_birch_forest_1", Occupation.BLACKSMITH,
        Items.IRON_INGOT, 6);
    cleric = setupWorker(level, CLERIC_ORIGIN, "church_birch_forest_1", Occupation.CLERIC,
        null, 0);
    check(ClericPotions.stock(cleric.worker).stream().allMatch(stock -> stock.count() == 1),
        "Cleric starting stock no longer begins with one seed per brew");
    Kithkyn.LOGGER.info("{} START: real Birch bakery, blacksmith and church vacancies claimed", PREFIX);
  }

  private static WorkerRun setupWorker(ServerLevel level, BlockPos origin, String buildingId,
      Occupation occupation, Item supply, int count) {
    BuildingInfo info = Buildings.getByName(buildingId);
    check(info != null, "Missing building definition " + buildingId);
    BuildingInfo homeInfo = Buildings.getByName("house_birch_forest_1");
    check(homeInfo != null, "Missing Birch housing definition");
    force(level, origin, 128);
    Building building = ApprovedStructureAccess.place(level, origin, info, Rotation.NONE);
    // The native fixture clears a broad pad around each placement. Keep ordinary housing far
    // enough away that seating the worker cannot erase the workplace we are about to exercise.
    Building home = ApprovedStructureAccess.place(level, origin.offset(0, 0, 80), homeInfo, Rotation.NONE);
    Village village = new ApprovedStructureAccess.VillageFixture(level, building, true, home);
    JobAssignment vacancy = village.getUnassignedJobs().stream()
        .filter(job -> job.getOccupation() == occupation)
        .findFirst().orElseThrow(() -> new AssertionError(buildingId + " did not publish " + occupation));
    ApprovedStructureAccess.Person worker = new ApprovedStructureAccess.Person(level, village);
    worker.setLifeStage(AgeStage.ADULT);
    worker.setOccupation(Occupation.WANDERER);
    village.getPopulation().add(worker.getUUID());
    BlockPos start = origin.offset(-10, 1, BlockPos.of(info.getWorkLocations().keySet().stream()
        .skip(vacancy.getStationIndex()).findFirst().orElseThrow()).getZ());
    ApprovedStructureAccess.moveTo(worker, start);
    check(level.addFreshEntity(worker), "Could not spawn " + occupation + " worker");
    JobClaiming.tick(village, level);
    JobAssignment assignment = village.getJobAssignment(worker.getUUID());
    check(assignment != null && assignment.getOccupation() == occupation,
        occupation + " vacancy was not automatically claimed");
    check(assignment.getBuildingUUID().equals(building.getUUID()), occupation + " claimed another building");
    BlockPos station = LocationManager.getJobLocation(worker);
    check(!station.equals(BlockPos.ZERO), occupation + " assignment did not resolve its station");
    BlockPos storagePosition = world(building, info.getContainerLocations().getFirst());
    check(level.getBlockEntity(storagePosition) instanceof Container,
        occupation + " building has no physical shared container at " + storagePosition);
    Container storage = (Container) level.getBlockEntity(storagePosition);
    storage.clearContent();
    if (supply != null) storage.setItem(0, new ItemStack(supply, count));
    return new WorkerRun(village, worker, building, station, storagePosition, storage, start);
  }

  private static void observe(ServerLevel level) {
    observeTrip(baker, Items.WHEAT);
    observeTrip(smith, Items.IRON_INGOT);
    observeTrip(cleric, null);

    boolean brewedHealing = ClericPotions.stock(cleric.worker).stream()
        .anyMatch(stock -> ClericPotions.has(stock.sample(), MobEffects.HEAL) && stock.count() >= 4);
    if (brewedHealing && patient == null) {
      patientStart = cleric.station.offset(-9, 0, 0);
      patient = new ApprovedStructureAccess.Person(level, cleric.village);
      patient.setLifeStage(AgeStage.ADULT);
      patient.setOccupation(Occupation.WANDERER);
      cleric.village.getPopulation().add(patient.getUUID());
      patient.setHealth(2.0F);
      patient.reloadState();
      ApprovedStructureAccess.moveTo(patient, patientStart);
      check(level.addFreshEntity(patient), "Could not spawn cleric patient");
      Kithkyn.LOGGER.info("{} CLERIC BREW PASS: real goal reached station and raised healing stock to four", PREFIX);
    }
    if (patient != null) {
      patientWalked |= patient.distanceToSqr(patientStart.getX() + 0.5D, patientStart.getY(),
          patientStart.getZ() + 0.5D) > 1.0D;
      patientTended |= patient.getHealth() > 2.0F || patient.hasEffect(MobEffects.REGENERATION);
    }
  }

  private static void observeTrip(WorkerRun run, Item carried) {
    run.reachedStation |= run.worker.distanceToSqr(run.station.getX() + 0.5D, run.station.getY(),
        run.station.getZ() + 0.5D) <= 9.0D;
    run.leftStart |= run.worker.distanceToSqr(run.start.getX() + 0.5D, run.start.getY(),
        run.start.getZ() + 0.5D) > 1.0D;
    if (carried != null) run.carriedSupply |= run.worker.personMainInv.countItem(carried) > 0;
  }

  private static boolean complete() {
    boolean bakerDone = baker.leftStart && baker.carriedSupply && baker.reachedStation
        && baker.storage.countItem(Items.BREAD) >= 1 && baker.storage.countItem(Items.WHEAT) <= 3;
    boolean smithDone = smith.leftStart && smith.carriedSupply && smith.reachedStation
        && smith.storage.countItem(Items.BUCKET) >= 1 && smith.storage.countItem(Items.IRON_INGOT) <= 3;
    boolean clericDone = cleric.leftStart && cleric.reachedStation && patientWalked && patientTended;
    if (bakerDone && !baker.reported) {
      baker.reported = true;
      Kithkyn.LOGGER.info("{} BAKER PASS: walked storage -> bakery station -> storage and deposited bread", PREFIX);
    }
    if (smithDone && !smith.reported) {
      smith.reported = true;
      Kithkyn.LOGGER.info("{} BLACKSMITH PASS: walked storage -> forge -> storage and deposited a bucket", PREFIX);
    }
    if (clericDone && !cleric.reported) {
      cleric.reported = true;
      Kithkyn.LOGGER.info("{} CLERIC TEND PASS: hurt resident sought the cleric and received a real potion", PREFIX);
    }
    return bakerDone && smithDone && clericDone;
  }

  private static String status() {
    List<String> details = new ArrayList<>();
    details.add("baker=" + baker.describe(Items.WHEAT, Items.BREAD));
    details.add("blacksmith=" + smith.describe(Items.IRON_INGOT, Items.BUCKET));
    details.add("cleric={pos=" + cleric.worker.position() + ", reachedStation=" + cleric.reachedStation
        + ", stock=" + ClericPotions.stock(cleric.worker) + ", patient="
        + (patient == null ? "not spawned" : patient.position() + "/health=" + patient.getHealth()) + "}");
    return String.join(", ", details);
  }

  private static BlockPos world(Building building, long local) {
    return BlockPos.of(building.getOriginLocation()).offset(BlockPos.of(local).rotate(building.getRotation()));
  }

  private static void force(ServerLevel level, BlockPos origin, int radius) {
    for (int x = (origin.getX() - radius) >> 4; x <= (origin.getX() + radius) >> 4; x++) {
      for (int z = (origin.getZ() - radius) >> 4; z <= (origin.getZ() + radius) >> 4; z++) {
        level.setChunkForced(x, z, true);
      }
    }
  }

  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  private static final class WorkerRun {
    private final Village village;
    private final ApprovedStructureAccess.Person worker;
    private final Building building;
    private final BlockPos station;
    private final BlockPos storagePosition;
    private final Container storage;
    private final BlockPos start;
    private boolean leftStart;
    private boolean carriedSupply;
    private boolean reachedStation;
    private boolean reported;

    private WorkerRun(Village village, ApprovedStructureAccess.Person worker, Building building,
        BlockPos station, BlockPos storagePosition, Container storage, BlockPos start) {
      this.village = village;
      this.worker = worker;
      this.building = building;
      this.station = station;
      this.storagePosition = storagePosition;
      this.storage = storage;
      this.start = start;
    }

    private String describe(Item input, Item output) {
      return "{building=" + building.getName() + ", pos=" + worker.position() + ", station=" + station
          + ", storage=" + storagePosition + ", moved=" + leftStart + ", carried=" + carriedSupply
          + ", reachedStation=" + reachedStation + ", input=" + storage.countItem(input)
          + ", output=" + storage.countItem(output) + "}";
    }
  }
}
