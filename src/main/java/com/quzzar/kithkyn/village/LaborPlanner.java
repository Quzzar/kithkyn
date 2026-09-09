package com.quzzar.kithkyn.village;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.configuration.KithkynConfig;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.llm.LlmDecision;
import com.quzzar.kithkyn.llm.LlmService;
import com.quzzar.kithkyn.village.buildings.Building;
import com.quzzar.kithkyn.village.buildings.BuildingInfo;
import com.quzzar.kithkyn.village.buildings.Buildings;
import com.quzzar.kithkyn.village.buildings.ConstructionQuote;
import com.quzzar.kithkyn.village.buildings.MaterialProduction;
import com.quzzar.kithkyn.village.buildings.VillageGoal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * When a village is going hungry beside an empty field, or saving for a project
 * whose material producer stands vacant, the brain is asked to put someone on
 * that work. The claiming pass ({@link JobClaiming}) fills an open post
 * from the IDLE only, and the swap pass only trades posts to raise aptitude;
 * neither ever takes a settled worker off a job the village can spare and moves
 * them to one it cannot. So a village that built a farm but grew no farmer
 * starves next to it forever, and starving is exactly what stops it growing the
 * newcomer who would farm (docs/population-and-labor.md): a deadlock a hungry
 * village cannot break on its own.
 *
 * <p>Food reprioritization remains a midnight decision. A saved project blocked
 * on a vacant material producer may be handled on the ordinary labor cadence,
 * because a saved goal can expire before another midnight window. The brain is
 * shown the shortage and the crew and picks who moves, or that no one should.
 * Candidates are ordered by aptitude so the same safe fallback used by ordinary
 * job claiming can act when the model is unavailable. Only loaded workers can be moved,
 * since a reassignment awakens them, brings them to the campfire, and rebuilds
 * their goals in the world. One decision is in flight per village, like the
 * build planner and the marriage verdict, and a village whose brain leaves the
 * crew as it is sits the question out a while rather than asking it every pass.
 */
public final class LaborPlanner {

  private LaborPlanner() {
  }

  /** Phase-staggered per village, like the build decision, since the verdict is a model call. */
  public static final int LABOR_INTERVAL_SECONDS = 60;

  /** The jobs that put food in the stores. */
  private static final Set<Occupation> FOOD_PRODUCERS =
      EnumSet.of(Occupation.FARMER, Occupation.FISHER, Occupation.HUNTER);

  /**
   * The trades a village always keeps at least one of: its miner (no miner, no
   * stone, and the build economy stalls) and its builder (no builder, and a
   * project in progress never finishes, so the village sits stuck on it and
   * never advances to houses). The last of either is never moved off to the
   * field, however hungry the village; a second, if there is one, may still go
   * (Aaron, 2026-09-03).
   */
  private static final Set<Occupation> ALWAYS_STAFFED =
      EnumSet.of(Occupation.MINER, Occupation.BUILDER);

  /** Village-seconds a village waits out after the brain leaves the crew as it is. */
  private static final int QUIET_SECONDS = 300;

  /** Village-time each village may be asked again, so a lasting shortage is not re-asked every pass. */
  private static final ConcurrentHashMap<String, Integer> quietUntil = new ConcurrentHashMap<>();

  private static final String LEAVE_AS_IS = "Leave the crew as it is for now";

  /** One vacant post plus the factual reason filling it matters now. */
  private record LaborNeed(JobAssignment vacancy, String situation, String memoryReason) {
  }

  /**
   * One look on the caller's cadence. Food can trigger only in the midnight
   * rebalance window; a saved goal blocked by a vacant material producer may
   * trigger at any hour so the goal has time to make progress.
   */
  public static void tick(Village village, ServerLevel level) {
    boolean midnight = JobClaiming.isMidnightRebalanceTime(level.getDayTime());
    if (village.isLaborDecisionPending() || level.getServer() == null) {
      return;
    }
    Integer quiet = quietUntil.get(village.getID());
    if (quiet != null && village.getVillageTime() < quiet) {
      return;
    }
    // Nothing can be reassigned in a village nobody is near: the worker has to
    // be loaded to have its goals rebuilt, and asking the brain would spend a
    // model call on an answer no one could act on.
    Building centre = village.getTownCenter();
    if (centre == null || !level.hasChunkAt(BlockPos.of(centre.getCenterLocation()))) {
      return;
    }
    // An idle hand is claimed by the ordinary claiming pass; only step in when
    // the village has no one spare and would otherwise leave the field empty.
    if (!village.idlePeople().isEmpty()) {
      return;
    }
    LaborNeed need = currentNeed(village, midnight);
    if (need == null) {
      return;
    }
    List<RealPerson> crew = movableWorkers(village, level, need.vacancy(), false);
    if (crew.isEmpty()) {
      // The normal job cooldown is longer than a saved goal's lifetime. An
      // otherwise impossible food or material deadlock gets one emergency pass
      // that may move a recently assigned worker, while retaining all protected
      // trade and housing checks.
      crew = movableWorkers(village, level, need.vacancy(), true);
    }
    if (crew.isEmpty()) {
      return;
    }
    if (!LlmService.get().isReady()) {
      reassign(village, level, crew.get(0).getUUID(), need,
          "the village brain was unavailable, so the best-suited movable worker took the urgent post");
      return;
    }
    ask(village, level, need, crew);
  }

  /** Food stored is below the per-capita target the newcomer math wants (VillageAttractiveness). */
  private static boolean isHungry(Village village) {
    VillageAttractiveness report = village.getAttractiveness();
    return report != null
        && report.foodCount() < report.population() * KithkynConfig.AttractivenessFoodTargetPerCapita;
  }

  /** The first open food post whose building actually stands, or null when none is going wanting. */
  private static JobAssignment openFoodPost(Village village) {
    for (JobAssignment job : village.claimableJobs()) {
      if (FOOD_PRODUCERS.contains(job.getOccupation()) && village.getBuilding(job.getBuildingUUID()) != null) {
        return job;
      }
    }
    return null;
  }

  /** The first open post of one occupation whose workplace still stands. */
  @javax.annotation.Nullable
  private static JobAssignment openPost(Village village, Occupation occupation) {
    for (JobAssignment job : village.claimableJobs()) {
      if (job.getOccupation() == occupation && village.getBuilding(job.getBuildingUUID()) != null) {
        return job;
      }
    }
    return null;
  }

  /** Food first, then backed-up storage, then the saved project's material producer. */
  @javax.annotation.Nullable
  private static LaborNeed currentNeed(Village village, boolean allowFoodReprioritization) {
    JobAssignment food = allowFoodReprioritization && isHungry(village) ? openFoodPost(village) : null;
    if (food != null) {
      VillageAttractiveness report = village.getAttractiveness();
      String field = food.getOccupation().name().toLowerCase();
      String situation = village.getName() + " is going hungry. It has " + report.foodCount()
          + " food stored for " + report.population() + " people, below what keeps them fed, and the "
          + field + " post stands open even though no one is idle to take it.";
      return new LaborNeed(food, situation, "the village needed food");
    }

    boolean storageBackedUp = village.isStorageBackedUp();
    JobAssignment quartermaster = storageBackedUp ? openPost(village, Occupation.QUARTERMASTER) : null;
    if (quartermaster != null) {
      String situation = village.getName() + " has shared storage backed up with goods its containers cannot accept. "
          + "Its existing quartermaster post stands open, so no one is actively clearing or organizing that backlog, "
          + "and no one is idle to take the post.";
      return new LaborNeed(quartermaster, situation, "the village needed its storage backlog managed");
    }

    String goal = VillageGoal.current(village);
    BuildingInfo wanted = goal == null ? null : Buildings.getByName(goal);
    if (wanted == null) {
      return null;
    }
    ConstructionQuote quote = ConstructionQuote.captureGoal(village, wanted, village.stockTally());
    JobAssignment producer = openProjectProducerPost(quote.missing(), village.claimableJobs(), buildingId -> {
      Building building = village.getBuilding(buildingId);
      return building == null || building.getInfo() == null ? List.of() : building.getInfo().getGrants();
    });
    if (producer == null) {
      return null;
    }
    String trade = producer.getOccupation().name().toLowerCase();
    String situation = village.getName() + " is saving for " + wanted.displayLabel() + " but still needs "
        + quote.describeMissing() + ". Its existing " + trade + " post stands open, so the village has the "
        + "means to produce the blocked material but no one is doing that work and no one is idle to take it.";
    return new LaborNeed(producer, situation, "the village needed materials for " + wanted.displayLabel());
  }

  /**
   * Finds an existing vacancy that can produce one of a saved project's missing
   * materials. The producer is derived from building grants, so datapack
   * definitions remain authoritative and no occupation-to-item table can drift.
   */
  static JobAssignment openProjectProducerPost(List<ItemStack> missing,
      List<JobAssignment> openPosts, Function<UUID, List<String>> grantsForBuilding) {
    for (ItemStack shortage : missing) {
      String capability = MaterialProduction.capabilityFor(shortage.getItem());
      if (capability == null) {
        continue;
      }
      for (JobAssignment post : openPosts) {
        if (grantsForBuilding.apply(post.getBuildingUUID()).contains(capability)) {
          return post;
        }
      }
    }
    return null;
  }

  /**
   * Loaded workers a reassignment could actually move onto the field. Three are
   * held back: whoever already does the wanted job; the last miner and the last
   * builder, the trades a village must always keep ({@link #ALWAYS_STAFFED});
   * every active food producer while the village is hungry;
   * and anyone still inside their job-swap cooldown, so a person just placed or
   * moved is left to settle rather than yanked straight onto the field
   * (the same per-person cooldown the aptitude swap pass respects,
   * {@link JobClaiming#isOnCooldown}).
   */
  private static List<RealPerson> movableWorkers(Village village, ServerLevel level,
      JobAssignment vacancy, boolean ignoreCooldown) {
    long now = level.getGameTime();
    boolean hungry = isHungry(village);
    boolean storageBackedUp = village.isStorageBackedUp();
    List<RealPerson> crew = new ArrayList<>();
    for (var entry : village.getJobAssignmentsView().entrySet()) {
      Occupation occupation = entry.getValue().getOccupation();
      if (occupation == vacancy.getOccupation()) {
        continue;
      }
      if (mustKeep(occupation, countOf(village, occupation), hungry, storageBackedUp)) {
        continue;
      }
      if (!ignoreCooldown && JobClaiming.isOnCooldown(village, entry.getKey(), now)) {
        continue; // recently placed, swapped, or displaced: let them settle
      }
      if (!village.canHouseForJob(entry.getKey(), vacancy.getBuildingUUID())) {
        continue; // their current workplace bed cannot follow them to this post
      }
      RealPerson worker = village.getPerson(level, entry.getKey());
      if (worker != null) {
        crew.add(worker);
      }
    }
    crew.sort(Comparator
        .comparingDouble((RealPerson worker) -> JobAptitudes.score(
            worker.getStatBlock(), vacancy.getOccupation()))
        .reversed()
        .thenComparing(RealPerson::getFullName));
    return crew;
  }

  /** How many villagers hold this occupation right now. */
  private static int countOf(Village village, Occupation occupation) {
    int count = 0;
    for (JobAssignment job : village.getJobAssignmentsView().values()) {
      if (job.getOccupation() == occupation) {
        count++;
      }
    }
    return count;
  }

  /** How many villagers currently work in any occupation in this group. */
  private static int countOf(Village village, Set<Occupation> occupations) {
    int count = 0;
    for (JobAssignment job : village.getJobAssignmentsView().values()) {
      if (occupations.contains(job.getOccupation())) {
        count++;
      }
    }
    return count;
  }

  /** Pure protection rule used before any urgent reassignment is offered. */
  static boolean mustKeep(Occupation occupation, int sameOccupationCount,
      boolean hungry, boolean storageBackedUp) {
    if (ALWAYS_STAFFED.contains(occupation) && sameOccupationCount <= 1) {
      return true;
    }
    if (FOOD_PRODUCERS.contains(occupation) && hungry) {
      return true;
    }
    return occupation == Occupation.QUARTERMASTER && storageBackedUp && sameOccupationCount <= 1;
  }

  private static void ask(Village village, ServerLevel level, LaborNeed need, List<RealPerson> crew) {
    String field = need.vacancy().getOccupation().name().toLowerCase();
    List<String> options = new ArrayList<>();
    for (RealPerson worker : crew) {
      options.add("Move " + worker.getFullName() + " off " + worker.getOccupation().name().toLowerCase()
          + " to work as the " + field);
    }
    options.add(LEAVE_AS_IS);

    List<UUID> crewIds = crew.stream().map(RealPerson::getUUID).toList();
    String situation = situationOf(need, crew);
    village.setLaborDecisionPending(true);
    LlmService.get().decide("who " + village.getName() + " assigns to urgent " + field + " work", situation, options)
        .whenComplete((result, error) -> {
          if (level.getServer() == null) {
            return;
          }
          level.getServer().execute(() -> apply(village, level, crewIds, need, result, error));
        });
  }

  private static String situationOf(LaborNeed need, List<RealPerson> crew) {
    StringBuilder situation = new StringBuilder(need.situation())
        .append(" Filling it means moving someone off their current work, which leaves that work open in turn. " )
        .append("The crew: ");
    for (RealPerson worker : crew) {
      situation.append(worker.getFullName()).append(" works as the ")
          .append(worker.getOccupation().name().toLowerCase()).append("; ");
    }
    return situation.toString();
  }

  /**
   * Applies the brain's pick on the server thread: move the chosen worker onto
   * the food post, or wait. The post is looked up fresh, since a claiming pass
   * may have filled it while the model thought; a worker who unloaded in the
   * meantime is simply skipped.
   */
  private static void apply(Village village, ServerLevel level, List<UUID> crewIds, LaborNeed need,
      Optional<LlmDecision> result, Throwable error) {
    village.setLaborDecisionPending(false);
    if (error != null) {
      Kithkyn.LOGGER.error("[labor] '{}' could not decide who fills urgent work; using aptitude fallback",
          village.getName(), error);
      reassign(village, level, crewIds.get(0), need, "the village brain call failed, so aptitude broke the deadlock");
      return;
    }
    if (result == null || result.isEmpty()) {
      reassign(village, level, crewIds.get(0), need,
          "the village brain returned no choice, so aptitude broke the deadlock");
      return;
    }
    LlmDecision decision = result.get();
    if (decision.choiceIndex() == crewIds.size()) {
      quietUntil.put(village.getID(), village.getVillageTime() + QUIET_SECONDS);
      Kithkyn.LOGGER.info("[labor] '{}' leaves its crew as it is: {}", village.getName(), decision.reason());
      return;
    }
    int selected = decision.choiceIndex() < 0 || decision.choiceIndex() > crewIds.size()
        ? 0 : decision.choiceIndex();
    reassign(village, level, crewIds.get(selected), need, decision.reason());
  }

  /** Revalidates both sides of the transfer and performs one count-preserving job move. */
  private static void reassign(Village village, ServerLevel level, UUID workerId,
      LaborNeed need, String reason) {
    RealPerson worker = village.getPerson(level, workerId);
    if (worker == null) {
      return;
    }
    JobAssignment post = matchingOpenPost(village, need.vacancy());
    if (post == null || !village.canHouseForJob(worker.getUUID(), post.getBuildingUUID())) {
      return;
    }
    Occupation from = worker.getOccupation();
    if (mustKeep(from, countOf(village, from), isHungry(village),
        village.isStorageBackedUp())) {
      // The model may answer after the crew changes. Never apply a stale choice
      // that would now remove the last essential worker.
      return;
    }
    JobAssignment vacated = village.releaseJob(worker.getUUID());
    if (vacated == null) {
      return;
    }
    JobClaiming.prepareForJobChange(village, worker);
    village.assignJob(worker.getUUID(), post);
    JobClaiming.startJob(village, worker, post);
    JobClaiming.markCooldown(village, worker.getUUID(), level.getGameTime());
    String field = post.getOccupation().name().toLowerCase();
    worker.logMemory("Moved off " + from.name().toLowerCase() + " to work as the " + field
        + "; " + need.memoryReason() + ".", Optional.empty());
    Kithkyn.LOGGER.info("[labor] '{}' moved '{}' from {} to {} because {}: {}",
        village.getName(), worker.getFullName(), from, post.getOccupation(), need.memoryReason(), reason);
  }

  @javax.annotation.Nullable
  private static JobAssignment matchingOpenPost(Village village, JobAssignment wanted) {
    for (JobAssignment post : village.claimableJobs()) {
      if (post.getBuildingUUID().equals(wanted.getBuildingUUID())
          && post.getStationIndex() == wanted.getStationIndex()
          && post.getOccupation() == wanted.getOccupation()) {
        return post;
      }
    }
    return null;
  }
}
