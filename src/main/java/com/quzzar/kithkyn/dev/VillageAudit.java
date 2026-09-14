package com.quzzar.kithkyn.dev;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.configuration.KithkynConfig;
import com.quzzar.kithkyn.entities.KithkynAttachments;
import com.quzzar.kithkyn.entities.PersonalLogData;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.entities.SimulationClock;
import com.quzzar.kithkyn.savedata.VillageManagerSaveData;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.VillageChunkLoader;
import com.quzzar.kithkyn.village.VillageManager;
import com.quzzar.kithkyn.village.Occupation;
import com.quzzar.kithkyn.village.buildings.Buildings;
import com.quzzar.kithkyn.village.buildings.StructureInProgress;
import com.quzzar.kithkyn.village.buildings.UrbanPlanner;
import com.quzzar.kithkyn.village.buildings.VillageContextSnapshot;
import com.quzzar.kithkyn.village.buildings.VillageStyle;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Persistent, selective village supervision for long-running development
 * worlds. The allowlist itself lives in {@link VillageManagerSaveData}; this
 * class supplies commands, progress observation, and actionable reports.
 */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class VillageAudit {

  /** Sample each monitored village once per game minute, including during a tick sprint. */
  static final long SAMPLE_INTERVAL_TICKS = 1_200L;
  /** A worker blocker becomes an audit finding after one game day. */
  static final long BLOCKER_ATTENTION_TICKS = 24_000L;
  /** Active construction or saving with no visible progress for two game days is stalled. */
  static final long WORK_STALL_TICKS = 48_000L;
  /** A planner-ready village with no project for a day needs attention. */
  static final long IDLE_STALL_TICKS = 24_000L;
  /** Allow tickets several game minutes to wake persisted resident entities. */
  static final long WAKE_GRACE_TICKS = 6_000L;

  private static final Map<String, Observation> observations = new HashMap<>();
  private static long nextSampleAt = Long.MIN_VALUE;
  private static MinecraftServer observedServer;

  private VillageAudit() {
  }

  /** Command branch mounted at {@code /kkdev village audit}. */
  public static LiteralArgumentBuilder<CommandSourceStack> branch() {
    return Commands.literal("audit")
        .executes(ctx -> status(ctx.getSource()))
        .then(Commands.literal("monitor")
            .executes(ctx -> setMonitored(ctx.getSource(), sourcePosition(ctx.getSource()), true))
            .then(Commands.argument("pos", BlockPosArgument.blockPos())
                .executes(ctx -> setMonitored(ctx.getSource(), BlockPosArgument.getBlockPos(ctx, "pos"), true))))
        .then(Commands.literal("unmonitor")
            .executes(ctx -> setMonitored(ctx.getSource(), sourcePosition(ctx.getSource()), false))
            .then(Commands.argument("pos", BlockPosArgument.blockPos())
                .executes(ctx -> setMonitored(ctx.getSource(), BlockPosArgument.getBlockPos(ctx, "pos"), false))))
        .then(Commands.literal("clear").executes(ctx -> clear(ctx.getSource())))
        .then(Commands.literal("report")
            .executes(ctx -> report(ctx.getSource(), sourcePosition(ctx.getSource())))
            .then(Commands.argument("pos", BlockPosArgument.blockPos())
                .executes(ctx -> report(ctx.getSource(), BlockPosArgument.getBlockPos(ctx, "pos")))))
        .then(Commands.literal("villagers")
            .executes(ctx -> villagers(ctx.getSource(), sourcePosition(ctx.getSource())))
            .then(Commands.argument("pos", BlockPosArgument.blockPos())
                .executes(ctx -> villagers(ctx.getSource(), BlockPosArgument.getBlockPos(ctx, "pos")))));
  }

  private static BlockPos sourcePosition(CommandSourceStack source) {
    return BlockPos.containing(source.getPosition());
  }

  private static int setMonitored(CommandSourceStack source, BlockPos near, boolean monitored) {
    if (!inOverworld(source)) {
      return 0;
    }
    if (!canChangeSelection(source)) {
      return 0;
    }
    ServerLevel level = source.getServer().overworld();
    VillageManagerSaveData manager = VillageManager.get(level);
    Village village = manager.getNearestVillage(near);
    if (village == null) {
      source.sendFailure(Component.literal("No villages exist yet."));
      return 0;
    }
    boolean changed = manager.setVillageAudited(village.getID(), monitored);
    observations.remove(village.getID());
    if (monitored) {
      for (Village other : manager.getVillages().values()) {
        if (!manager.isVillageAudited(other.getID())) {
          VillageChunkLoader.release(level, other.getID());
        }
      }
      VillageChunkLoader.reconcile(level, village);
      source.sendSuccess(() -> Component.literal((changed ? "Now monitoring " : "Already monitoring ")
          + "'" + village.getName() + "' (" + village.getStyle().id() + "). "
          + manager.getAuditedVillageIds().size() + " village(s) will run; "
          + suspendedCount(manager) + " will be suspended."), true);
    } else {
      VillageChunkLoader.release(level, village.getID());
      if (!manager.isAuditIsolationActive()) {
        for (Village other : manager.getVillages().values()) {
          VillageChunkLoader.reconcile(level, other);
        }
      }
      source.sendSuccess(() -> Component.literal((changed ? "Stopped monitoring " : "Was not monitoring ")
          + "'" + village.getName() + "'. "
          + (manager.isAuditIsolationActive()
              ? manager.getAuditedVillageIds().size() + " village(s) remain monitored."
              : "Audit isolation is off; the configured village-loading mode applies again.")), true);
    }
    return 1;
  }

  private static int clear(CommandSourceStack source) {
    if (!inOverworld(source)) {
      return 0;
    }
    if (!canChangeSelection(source)) {
      return 0;
    }
    ServerLevel level = source.getServer().overworld();
    VillageManagerSaveData manager = VillageManager.get(level);
    Set<String> previouslyMonitored = manager.getAuditedVillageIds();
    int removed = manager.clearAuditedVillages();
    observations.clear();
    for (String villageId : previouslyMonitored) {
      VillageChunkLoader.release(level, villageId);
    }
    for (Village village : manager.getVillages().values()) {
      VillageChunkLoader.reconcile(level, village);
    }
    source.sendSuccess(() -> Component.literal(removed == 0
        ? "Audit isolation was already off."
        : "Stopped monitoring " + removed
            + " village(s). The configured village-loading mode applies again."), true);
    return 1;
  }

  private static int status(CommandSourceStack source) {
    if (!inOverworld(source)) {
      return 0;
    }
    ServerLevel level = source.getServer().overworld();
    VillageManagerSaveData manager = VillageManager.get(level);
    long now = level.getGameTime();
    StringBuilder report = new StringBuilder();
    if (!manager.isAuditIsolationActive()) {
      report.append("Village audit isolation is OFF. All ")
          .append(manager.getVillages().size()).append(" village(s) follow the normal simulation and loading rules.");
    } else {
      report.append("Village audit isolation is ON: ")
          .append(manager.getAuditedVillageIds().size()).append(" monitored village(s) run and stay loaded; ")
          .append(suspendedCount(manager)).append(" other village(s) are suspended.");
      for (Village village : monitoredVillages(manager)) {
        Observation observation = observe(village, now, false);
        report.append("\n  ").append(village.getName()).append(" [")
            .append(village.getStyle().id()).append(", ")
            .append(village.getKind().id()).append("]: ")
            .append(observation.assessment.state().label).append("; ")
            .append(observation.assessment.summary()).append("; ")
            .append(VillageChunkLoader.heldChunkCount(village.getID())).append(" chunks held");
      }
    }
    appendCoverage(report, manager);
    source.sendSuccess(() -> Component.literal(report.toString()), false);
    return 1;
  }

  private static int report(CommandSourceStack source, BlockPos near) {
    if (!inOverworld(source)) {
      return 0;
    }
    ServerLevel level = source.getServer().overworld();
    Village village = VillageManager.get(level).getNearestVillage(near);
    if (village == null) {
      source.sendFailure(Component.literal("No villages exist yet."));
      return 0;
    }
    Observation observation = observe(village, level.getGameTime(), false);
    VillageContextSnapshot snapshot = observation.snapshot;
    String text = "Audit report for '" + village.getName() + "' [" + village.getStyle().id() + ", "
        + village.getKind().id() + "]: " + observation.assessment.state().label + ". "
        + observation.assessment.summary()
        + "\nResident briefing:\n" + snapshot.chatBriefing()
        + "\nCollective briefing:\n" + snapshot.plannerBriefing()
        + "\nPlanning options:\n" + UrbanPlanner.buildableCatalogue(village);
    source.sendSuccess(() -> Component.literal(text), false);
    return 1;
  }

  private static int villagers(CommandSourceStack source, BlockPos near) {
    if (!inOverworld(source)) {
      return 0;
    }
    ServerLevel level = source.getServer().overworld();
    Village village = VillageManager.get(level).getNearestVillage(near);
    if (village == null) {
      source.sendFailure(Component.literal("No villages exist yet."));
      return 0;
    }
    long now = level.getGameTime();
    List<PersonReport> reports = village.getPopulation().stream()
        .map(id -> personReport(village, level, id, now))
        .sorted(Comparator.comparing(PersonReport::name, String.CASE_INSENSITIVE_ORDER))
        .toList();
    StringBuilder text = new StringBuilder("Residents of '").append(village.getName()).append("' (")
        .append(reports.size()).append("):");
    for (PersonReport resident : reports) {
      text.append("\n  ").append(resident.text());
    }
    Set<Occupation> gaps = unsupportedRoles(village);
    if (!gaps.isEmpty()) {
      text.append("\n  IMPLEMENTATION GAP: standing posts for ")
          .append(gaps.stream().map(role -> role.name().toLowerCase(Locale.ROOT))
              .sorted().collect(java.util.stream.Collectors.joining(", ")))
          .append(" have no job behavior yet; staffing them cannot produce activity.");
    }
    source.sendSuccess(() -> Component.literal(text.toString()), false);
    return 1;
  }

  private static PersonReport personReport(Village village, ServerLevel level, UUID id, long now) {
    RealPerson person = village.getPerson(level, id);
    if (person == null) {
      return new PersonReport(id.toString(), id + ": UNLOADED; the resident entity is not currently available");
    }
    String activity = person.recentActivity().orElse("no recent work activity");
    Optional<PersonalLogData.ActiveBlocker> blocker = person
        .getData(KithkynAttachments.PERSONAL_LOG.get()).standingBlocker(now);
    String blockerText = blocker.map(value -> "; BLOCKER for "
        + formatTicks(SimulationClock.elapsed(now, value.since()).orElse(0L)) + ": " + value.text()).orElse("");
    String text = person.getFullName() + ": "
        + person.getLifeStage().name().toLowerCase(Locale.ROOT) + " "
        + person.getOccupation().name().toLowerCase(Locale.ROOT) + ", "
        + person.getPersonality().displayName() + ", health "
        + String.format(Locale.ROOT, "%.1f/%.1f", person.getHealth(), person.getMaxHealth())
        + ", at " + person.blockPosition().toShortString() + "; " + activity + blockerText;
    return new PersonReport(person.getFullName(), text);
  }

  private static boolean inOverworld(CommandSourceStack source) {
    if (source.getLevel().dimension() == Level.OVERWORLD) {
      return true;
    }
    source.sendFailure(Component.literal("Village auditing must be managed from the Overworld."));
    return false;
  }

  private static boolean canChangeSelection(CommandSourceStack source) {
    if (VillageTimelapse.isRunning(source.getServer())) {
      source.sendFailure(Component.literal("Stop the village timelapse before changing its monitored targets."));
      return false;
    }
    if (VillageCleanup.isRunning(source.getServer())) {
      source.sendFailure(Component.literal("Stop village cleanup before changing the monitored targets."));
      return false;
    }
    return true;
  }

  private static int suspendedCount(VillageManagerSaveData manager) {
    return Math.max(0, manager.getVillages().size() - manager.getAuditedVillageIds().size());
  }

  private static List<Village> monitoredVillages(VillageManagerSaveData manager) {
    return manager.getAuditedVillageIds().stream().map(manager::getVillage)
        .filter(java.util.Objects::nonNull)
        .sorted(Comparator.comparing(Village::getName, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  private static void appendCoverage(StringBuilder report, VillageManagerSaveData manager) {
    report.append("\nVariant coverage:");
    for (VillageStyle style : VillageStyle.values()) {
      List<String> monitored = monitoredVillages(manager).stream()
          .filter(village -> village.getStyle() == style).map(Village::getName).toList();
      long total = manager.getVillages().values().stream().filter(village -> village.getStyle() == style).count();
      report.append("\n  ").append(style.id()).append(": ")
          .append(monitored.isEmpty() ? "MISSING" : "monitored " + String.join(", ", monitored))
          .append("; ").append(total).append(" in world; founding content ")
          .append(Buildings.hasFoundingSet(style) ? "loaded" : "missing");
    }
  }

  @SubscribeEvent
  public static void onServerTick(ServerTickEvent.Post event) {
    MinecraftServer server = event.getServer();
    if (!KithkynConfig.DeveloperCommands) {
      resetRuntime(server);
      return;
    }
    ServerLevel level = server.overworld();
    VillageManagerSaveData manager = VillageManager.get(level);
    if (!manager.isAuditIsolationActive()) {
      resetRuntime(server);
      return;
    }
    long now = level.getGameTime();
    if (observedServer != server || now < nextSampleAt - SAMPLE_INTERVAL_TICKS) {
      observations.clear();
      observedServer = server;
      nextSampleAt = now;
    }
    if (now < nextSampleAt) {
      return;
    }
    nextSampleAt = now + SAMPLE_INTERVAL_TICKS;
    Set<String> monitored = manager.getAuditedVillageIds();
    observations.keySet().removeIf(id -> !monitored.contains(id));
    for (Village village : monitoredVillages(manager)) {
      observe(village, now, true);
    }
  }

  @SubscribeEvent
  public static void onServerStopping(ServerStoppingEvent event) {
    if (observedServer == event.getServer()) {
      observations.clear();
      observedServer = null;
      nextSampleAt = Long.MIN_VALUE;
    }
  }

  private static void resetRuntime(MinecraftServer server) {
    if (observedServer == server) {
      observations.clear();
      observedServer = null;
      nextSampleAt = Long.MIN_VALUE;
    }
  }

  private static Observation observe(Village village, long now, boolean announce) {
    VillageContextSnapshot snapshot = VillageContextSnapshot.capture(village);
    ProgressMark mark = ProgressMark.capture(village, snapshot);
    Observation prior = observations.get(village.getID());
    long lastProgressAt = prior == null || !prior.mark.equals(mark) ? now : prior.lastProgressAt;
    long observedFor = prior == null ? 0L : Math.max(0L, now - prior.firstObservedAt);
    long stalledFor = Math.max(0L, now - lastProgressAt);
    int unloadedResidents = (int) village.getPopulation().stream()
        .filter(id -> village.getPerson(village.getLevel(), id) == null).count();
    Assessment assessment = assess(village, snapshot, stalledFor, observedFor, unloadedResidents);
    Observation current = new Observation(mark, snapshot, assessment,
        prior == null ? now : prior.firstObservedAt, lastProgressAt);
    observations.put(village.getID(), current);
    if (announce && prior != null && prior.assessment.state() != assessment.state()) {
      if (assessment.state() == AuditState.ATTENTION) {
        announce(village.getLevel().getServer(), "Village audit ALERT for '" + village.getName()
            + "': " + assessment.summary());
        Kithkyn.LOGGER.warn("Village audit alert for '{}': {}", village.getName(), assessment.summary());
      } else if (prior.assessment.state() == AuditState.ATTENTION) {
        announce(village.getLevel().getServer(), "Village audit: '" + village.getName()
            + "' is moving again. " + assessment.summary());
        Kithkyn.LOGGER.info("Village audit recovery for '{}': {}", village.getName(), assessment.summary());
      }
    }
    return current;
  }

  private static Assessment assess(Village village, VillageContextSnapshot snapshot,
      long stalledFor, long observedFor, int unloadedResidents) {
    Optional<VillageContextSnapshot.WorkerBlocker> oldBlocker = snapshot.workerBlockers().stream()
        .filter(blocker -> blocker.standingTicks() >= BLOCKER_ATTENTION_TICKS)
        .max(Comparator.comparingLong(VillageContextSnapshot.WorkerBlocker::standingTicks));
    if (oldBlocker.isPresent()) {
      VillageContextSnapshot.WorkerBlocker blocker = oldBlocker.get();
      return new Assessment(AuditState.ATTENTION, blocker.workerName() + " (" + blocker.occupation()
          + ") has reported for " + formatTicks(blocker.standingTicks()) + ": " + blocker.text());
    }
    if (unloadedResidents > 0 && observedFor >= WAKE_GRACE_TICKS) {
      return new Assessment(AuditState.ATTENTION, unloadedResidents
          + " resident(s) remain unloaded after " + formatTicks(observedFor)
          + "; their member chunk tickets may not be waking them");
    }
    Set<Occupation> unsupported = unsupportedRoles(village);
    if (!unsupported.isEmpty()) {
      return new Assessment(AuditState.ATTENTION, "standing "
          + unsupported.stream().map(role -> role.name().toLowerCase(Locale.ROOT))
              .sorted().collect(java.util.stream.Collectors.joining(", "))
          + " post(s) have no implemented job behavior; staffing them cannot produce activity");
    }
    if (village.hasPendingBrainDecision()) {
      return new Assessment(AuditState.WAITING, "waiting for a wall-clock village brain decision");
    }

    String work = workSummary(village, snapshot);
    boolean hasWork = village.getCurrentProject() != null
        || village.getWallProject() != null && !village.getWallProject().isComplete()
        || snapshot.savedGoal().isPresent();
    if (hasWork && stalledFor >= WORK_STALL_TICKS) {
      return new Assessment(AuditState.ATTENTION, work + " No observable progress for "
          + formatTicks(stalledFor) + "; inspect the resident blockers and missing materials");
    }
    if (!hasWork && planningCooldownComplete(village) && stalledFor >= IDLE_STALL_TICKS) {
      return new Assessment(AuditState.ATTENTION, "ready to plan, but no project or saving goal appeared for "
          + formatTicks(stalledFor));
    }
    if (!snapshot.workerBlockers().isEmpty()) {
      return new Assessment(AuditState.HEALTHY, work + " " + snapshot.workerBlockers().size()
          + " temporary worker blocker(s) are being watched");
    }
    return new Assessment(AuditState.HEALTHY, work);
  }

  private static String workSummary(Village village, VillageContextSnapshot snapshot) {
    if (village.getWallProject() != null && !village.getWallProject().isComplete()) {
      return "raising a wall with " + village.getWallProject().remainingBlocks() + " blocks remaining.";
    }
    if (snapshot.currentProject().isPresent()) {
      VillageContextSnapshot.ConstructionPlan project = snapshot.currentProject().get();
      String missing = project.quote().describeMissing();
      return "working on " + project.label() + " (" + project.stage().name().toLowerCase(Locale.ROOT) + ")"
          + (missing.isEmpty() ? "." : "; still missing " + missing + ".");
    }
    if (snapshot.savedGoal().isPresent()) {
      VillageContextSnapshot.ConstructionPlan goal = snapshot.savedGoal().get();
      String missing = goal.quote().describeMissing();
      return "saving for " + goal.label() + (missing.isEmpty() ? "." : "; still missing " + missing + ".");
    }
    if (!planningCooldownComplete(village)) {
      long remaining = village.getLastBuildCompletedTime()
          + (long) (KithkynConfig.BuildCooldownDays * 24_000L) - village.getLevel().getGameTime();
      return "in the post-build maintenance cooldown for another " + formatTicks(Math.max(0L, remaining)) + ".";
    }
    return "ready to plan the next project.";
  }

  private static boolean planningCooldownComplete(Village village) {
    long end = village.getLastBuildCompletedTime() + (long) (KithkynConfig.BuildCooldownDays * 24_000L);
    return village.getLevel().getGameTime() >= end;
  }

  /** Roles advertised by standing buildings that cannot currently do job work. */
  static Set<Occupation> unsupportedRoles(Village village) {
    return village.getBuildings().stream()
        .filter(building -> building.getInfo() != null)
        .flatMap(building -> building.getInfo().getWorkLocations().values().stream())
        .filter(Occupation::lacksImplementedJobBehavior)
        .collect(java.util.stream.Collectors.toCollection(
            () -> java.util.EnumSet.noneOf(Occupation.class)));
  }

  private static void announce(MinecraftServer server, String message) {
    server.createCommandSourceStack().sendSuccess(() -> Component.literal(message), true);
  }

  private static String formatTicks(long ticks) {
    if (ticks < 24_000L) {
      return String.format(Locale.ROOT, "%.1f game hours", ticks / 1_000.0D);
    }
    return String.format(Locale.ROOT, "%.2f game days", ticks / 24_000.0D);
  }

  enum AuditState {
    HEALTHY("healthy"),
    WAITING("waiting"),
    ATTENTION("NEEDS ATTENTION");

    private final String label;

    AuditState(String label) {
      this.label = label;
    }
  }

  record Assessment(AuditState state, String summary) {
  }

  private record ProgressMark(
      int buildings,
      int population,
      int employed,
      long lastBuild,
      String project,
      int projectCursor,
      int prepRemaining,
      int wallRemaining,
      String shortfall) {

    static ProgressMark capture(Village village, VillageContextSnapshot snapshot) {
      StructureInProgress project = village.getCurrentProject();
      String projectName = snapshot.currentProject().map(plan -> plan.label() + ":" + plan.stage())
          .or(() -> snapshot.savedGoal().map(plan -> plan.label() + ":" + plan.stage())).orElse("");
      String shortfall = snapshot.currentProject().map(plan -> plan.quote().describeMissing())
          .or(() -> snapshot.savedGoal().map(plan -> plan.quote().describeMissing())).orElse("");
      return new ProgressMark(village.getBuildings().size(), snapshot.population(), snapshot.employed(),
          village.getLastBuildCompletedTime(), projectName, project == null ? -1 : project.getBuildCursor(),
          project == null ? -1 : project.remainingPrepWork(),
          village.getWallProject() == null ? -1 : village.getWallProject().remainingBlocks(), shortfall);
    }
  }

  private static final class Observation {
    private final ProgressMark mark;
    private final VillageContextSnapshot snapshot;
    private final Assessment assessment;
    private final long firstObservedAt;
    private final long lastProgressAt;

    private Observation(ProgressMark mark, VillageContextSnapshot snapshot, Assessment assessment,
        long firstObservedAt, long lastProgressAt) {
      this.mark = mark;
      this.snapshot = snapshot;
      this.assessment = assessment;
      this.firstObservedAt = firstObservedAt;
      this.lastProgressAt = lastProgressAt;
    }
  }

  private record PersonReport(String name, String text) {
  }
}
