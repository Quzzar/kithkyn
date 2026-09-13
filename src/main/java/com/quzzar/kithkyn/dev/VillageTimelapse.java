package com.quzzar.kithkyn.dev;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.configuration.KithkynConfig;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.VillageManager;
import com.quzzar.kithkyn.village.buildings.StructureInProgress;

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
 * Runs a village through its real work loops quickly for development testing.
 *
 * <p>This owns no simulated building, path, grading, movement, or resource
 * logic. It asks Minecraft to sprint ordinary logical ticks, watches the
 * villages' normal completion records, and pauses whenever a target village
 * brain is waiting on a wall-clock answer. The result is the same progression
 * the world would have produced slowly, including the builder's maintenance
 * cooldowns.
 */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class VillageTimelapse {

  private static final int TICKS_PER_DAY = 24_000;
  private static final int SPRINT_CHUNK_TICKS = TICKS_PER_DAY;
  private static final int DEFAULT_DAYS_PER_BUILD = 6;
  private static final int MIN_DEFAULT_DAYS = 10;
  private static final int MAX_DAYS = 365;

  private static Session active;

  private VillageTimelapse() {
  }

  /** Whether this server currently owns the one runtime-only tick sprint session. */
  public static boolean isRunning(MinecraftServer server) {
    return active != null && active.server == server;
  }

  /** Command branch mounted at {@code /kkdev village timelapse}. */
  public static LiteralArgumentBuilder<CommandSourceStack> branch() {
    return Commands.literal("timelapse")
        .then(Commands.literal("monitored")
            .then(Commands.argument("builds-each", IntegerArgumentType.integer(1, 64))
                .executes(ctx -> startMonitored(ctx.getSource(),
                    IntegerArgumentType.getInteger(ctx, "builds-each"),
                    defaultMaxDays(IntegerArgumentType.getInteger(ctx, "builds-each"))))
                .then(Commands.argument("max-days", IntegerArgumentType.integer(1, MAX_DAYS))
                    .executes(ctx -> startMonitored(ctx.getSource(),
                        IntegerArgumentType.getInteger(ctx, "builds-each"),
                        IntegerArgumentType.getInteger(ctx, "max-days"))))))
        .then(Commands.literal("start")
            .then(Commands.argument("builds", IntegerArgumentType.integer(1, 64))
                .executes(ctx -> start(ctx.getSource(),
                    BlockPos.containing(ctx.getSource().getPosition()),
                    IntegerArgumentType.getInteger(ctx, "builds"),
                    defaultMaxDays(IntegerArgumentType.getInteger(ctx, "builds"))))
                .then(Commands.argument("max-days", IntegerArgumentType.integer(1, MAX_DAYS))
                    .executes(ctx -> start(ctx.getSource(),
                        BlockPos.containing(ctx.getSource().getPosition()),
                        IntegerArgumentType.getInteger(ctx, "builds"),
                        IntegerArgumentType.getInteger(ctx, "max-days")))
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(ctx -> start(ctx.getSource(),
                            BlockPosArgument.getBlockPos(ctx, "pos"),
                            IntegerArgumentType.getInteger(ctx, "builds"),
                            IntegerArgumentType.getInteger(ctx, "max-days")))))))
        .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
        .then(Commands.literal("stop").executes(ctx -> stop(ctx.getSource())));
  }

  private static int defaultMaxDays(int builds) {
    return Math.min(MAX_DAYS, Math.max(MIN_DEFAULT_DAYS, builds * DEFAULT_DAYS_PER_BUILD));
  }

  private static int start(CommandSourceStack source, BlockPos near, int builds, int maxDays) {
    if (source.getLevel().dimension() != Level.OVERWORLD) {
      source.sendFailure(Component.literal("Village timelapse must be started in the Overworld."));
      return 0;
    }
    if (active != null) {
      source.sendFailure(Component.literal("A village timelapse is already running: "
          + describe(active)));
      return 0;
    }
    MinecraftServer server = source.getServer();
    if (VillageCleanup.isRunning(server)) {
      source.sendFailure(Component.literal("Stop village cleanup before starting a village timelapse."));
      return 0;
    }
    if (server.tickRateManager().isSprinting()) {
      source.sendFailure(Component.literal(
          "Minecraft is already tick-sprinting. Stop that sprint before starting a village timelapse."));
      return 0;
    }
    ServerLevel level = server.overworld();
    Village village = VillageManager.get(level).getNearestVillage(near);
    if (village == null) {
      source.sendFailure(Component.literal("No villages exist yet."));
      return 0;
    }
    var manager = VillageManager.get(level);
    if (manager.isAuditIsolationActive() && !manager.isVillageAudited(village.getID())) {
      source.sendFailure(Component.literal("'" + village.getName()
          + "' is suspended by audit isolation. Monitor it before starting its timelapse."));
      return 0;
    }
    if (village.getTownCenter() == null) {
      source.sendFailure(Component.literal("'" + village.getName() + "' has no completed village center yet."));
      return 0;
    }
    BlockPos centre = BlockPos.of(village.getTownCenter().getCenterLocation());
    if (!level.hasChunkAt(centre)) {
      source.sendFailure(Component.literal("'" + village.getName() + "' is not loaded. Stand near it or enable "
          + "village chunk loading, then try again."));
      return 0;
    }

    active = new Session(server, List.of(village), builds,
        (long) maxDays * TICKS_PER_DAY, level.getGameTime());
    int villages = VillageManager.get(level).getVillages().size();
    source.sendSuccess(() -> Component.literal("Timelapsing '" + village.getName() + "' until " + builds
        + (builds == 1 ? " building completes" : " buildings complete") + " or " + maxDays
        + " game days pass. This advances real ticks for the entire server"
        + (villages > 1 ? ", including " + villages + " villages" : "")
        + "; use '/kkdev village timelapse stop' to stop early."), true);
    advance(active);
    return 1;
  }

  private static int startMonitored(CommandSourceStack source, int builds, int maxDays) {
    if (source.getLevel().dimension() != Level.OVERWORLD) {
      source.sendFailure(Component.literal("Village timelapse must be started in the Overworld."));
      return 0;
    }
    if (active != null) {
      source.sendFailure(Component.literal("A village timelapse is already running: " + describe(active)));
      return 0;
    }
    MinecraftServer server = source.getServer();
    if (VillageCleanup.isRunning(server)) {
      source.sendFailure(Component.literal("Stop village cleanup before starting a village timelapse."));
      return 0;
    }
    if (server.tickRateManager().isSprinting()) {
      source.sendFailure(Component.literal(
          "Minecraft is already tick-sprinting. Stop that sprint before starting a village timelapse."));
      return 0;
    }
    ServerLevel level = server.overworld();
    var manager = VillageManager.get(level);
    if (!manager.isAuditIsolationActive()) {
      source.sendFailure(Component.literal(
          "No monitored-village allowlist is active. Use '/kkdev village audit monitor' at each target first."));
      return 0;
    }
    List<Village> targets = manager.getAuditedVillageIds().stream().map(manager::getVillage)
        .filter(java.util.Objects::nonNull)
        .sorted(java.util.Comparator.comparing(Village::getName, String.CASE_INSENSITIVE_ORDER))
        .toList();
    if (targets.isEmpty()) {
      source.sendFailure(Component.literal("No monitored villages still exist."));
      return 0;
    }
    for (Village village : targets) {
      if (village.getTownCenter() == null) {
        source.sendFailure(Component.literal("'" + village.getName()
            + "' has no completed village center yet."));
        return 0;
      }
      com.quzzar.kithkyn.village.VillageChunkLoader.reconcile(level, village);
    }

    active = new Session(server, targets, builds, (long) maxDays * TICKS_PER_DAY, level.getGameTime());
    source.sendSuccess(() -> Component.literal("Timelapsing " + targets.size() + " monitored village(s) until each "
        + "completes " + builds + (builds == 1 ? " building" : " buildings") + " or " + maxDays
        + " game days pass. Other villages remain suspended; use '/kkdev village timelapse stop' to stop early."), true);
    advance(active);
    return 1;
  }

  private static int status(CommandSourceStack source) {
    Session session = active;
    if (session == null) {
      source.sendSuccess(() -> Component.literal("No village timelapse is running."), false);
      return 1;
    }
    source.sendSuccess(() -> Component.literal(describe(session)), false);
    return 1;
  }

  private static int stop(CommandSourceStack source) {
    Session session = active;
    if (session == null) {
      source.sendFailure(Component.literal("No village timelapse is running."));
      return 0;
    }
    if (session.server != source.getServer()) {
      source.sendFailure(Component.literal("The active village timelapse belongs to another server."));
      return 0;
    }
    finish(session, "stopped by developer");
    return 1;
  }

  @SubscribeEvent
  public static void onServerTick(ServerTickEvent.Post event) {
    Session session = active;
    if (session != null && session.server == event.getServer()) {
      advance(session);
    }
  }

  /** An integrated server can stop without ending the client JVM. */
  @SubscribeEvent
  public static void onServerStopping(ServerStoppingEvent event) {
    Session session = active;
    if (session != null && session.server == event.getServer()) {
      active = null;
      session.server.tickRateManager().stopSprinting();
    }
  }

  private static void advance(Session session) {
    long now = session.server.overworld().getGameTime();
    for (Target target : session.targets.values()) {
      Village village = findVillage(session, target.villageId);
      if (village == null) {
        finish(session, "target village '" + target.villageName + "' no longer exists");
        return;
      }
      long completedAt = village.getLastBuildCompletedTime();
      if (target.completedBuilds < session.requestedBuilds && completedAt > target.lastCompletionTime) {
        target.completedBuilds++;
        target.lastCompletionTime = completedAt;
        String milestone = "Village timelapse: '" + village.getName() + "' completed build "
            + target.completedBuilds + "/" + session.requestedBuilds + " after "
            + formatDays(now - session.startedAt) + " game days.";
        session.server.createCommandSourceStack().sendSuccess(() -> Component.literal(milestone), true);
      }
    }
    if (session.targets.values().stream()
        .allMatch(target -> target.completedBuilds >= session.requestedBuilds)) {
      finish(session, "requested builds completed");
      return;
    }
    long elapsed = now - session.startedAt;
    if (elapsed < 0L) {
      finish(session, "world game time moved backwards");
      return;
    }
    if (elapsed >= session.maxTicks) {
      finish(session, "maximum simulated time reached");
      return;
    }

    Village waiting = firstWaitingVillage(session);
    if (waiting != null) {
      session.waitingOnVillage = waiting.getName();
      session.server.tickRateManager().stopSprinting();
      return;
    }
    session.waitingOnVillage = null;
    if (!session.server.tickRateManager().isSprinting()) {
      long remaining = session.maxTicks - elapsed;
      session.server.tickRateManager().requestGameToSprint((int) Math.min(SPRINT_CHUNK_TICKS, remaining));
    }
  }

  private static Village firstWaitingVillage(Session session) {
    return session.targets.keySet().stream().map(id -> findVillage(session, id))
        .filter(java.util.Objects::nonNull)
        .filter(Village::hasPendingBrainDecision)
        .findFirst()
        .orElse(null);
  }

  private static Village findVillage(Session session, String villageId) {
    return VillageManager.get(session.server.overworld()).getVillages().values().stream()
        .filter(village -> village.getID().equals(villageId))
        .findFirst()
        .orElse(null);
  }

  private static void finish(Session session, String reason) {
    if (active != session) {
      return;
    }
    active = null;
    session.server.tickRateManager().stopSprinting();
    long elapsed = session.server.overworld().getGameTime() - session.startedAt;
    int completed = session.targets.values().stream().mapToInt(target -> target.completedBuilds).sum();
    int requested = session.requestedBuilds * session.targets.size();
    String message = "Village timelapse finished: " + completed + "/" + requested
        + " target builds across " + session.targets.size() + " village(s) in "
        + formatDays(Math.max(0L, elapsed)) + " game days (" + reason + ")."
        + targetPhases(session);
    session.server.createCommandSourceStack().sendSuccess(() -> Component.literal(message), true);
  }

  private static String describe(Session session) {
    long elapsed = session.server.overworld().getGameTime() - session.startedAt;
    String waiting = session.waitingOnVillage == null ? ""
        : " Waiting for the brain of '" + session.waitingOnVillage + "'.";
    return "Village timelapse: " + session.targets.size() + " target(s), "
        + formatDays(Math.max(0L, elapsed)) + "/" + formatDays(session.maxTicks)
        + " game days." + waiting + targetPhases(session);
  }

  private static String targetPhases(Session session) {
    StringBuilder report = new StringBuilder();
    for (Target target : session.targets.values()) {
      Village village = findVillage(session, target.villageId);
      report.append("\n  ").append(target.villageName).append(": ")
          .append(target.completedBuilds).append('/').append(session.requestedBuilds).append(" builds. ")
          .append(village == null ? "Target village no longer exists."
              : phase(village, session.server.overworld()));
    }
    return report.toString();
  }

  private static String phase(Village village, ServerLevel level) {
    if (village.hasPendingBrainDecision()) {
      return "Waiting for a village brain decision.";
    }
    if (village.getWallProject() != null && !village.getWallProject().isComplete()) {
      return "The builders are raising a wall.";
    }
    StructureInProgress project = village.getCurrentProject();
    if (project != null) {
      return "Current project: " + project.getBuilding().getName() + " ("
          + project.getProgress().name().toLowerCase().replace('_', ' ') + ").";
    }
    long cooldownEnd = village.getLastBuildCompletedTime()
        + (long) (KithkynConfig.BuildCooldownDays * TICKS_PER_DAY);
    if (level.getGameTime() < cooldownEnd) {
      return "The builders are in their maintenance cooldown for another "
          + formatDays(cooldownEnd - level.getGameTime()) + " game days.";
    }
    return "The village is ready to plan its next project.";
  }

  private static String formatDays(long ticks) {
    return String.format(java.util.Locale.ROOT, "%.2f", ticks / (double) TICKS_PER_DAY);
  }

  private static final class Session {
    private final MinecraftServer server;
    private final Map<String, Target> targets;
    private final int requestedBuilds;
    private final long maxTicks;
    private final long startedAt;
    private String waitingOnVillage;

    private Session(MinecraftServer server, List<Village> villages,
        int requestedBuilds, long maxTicks, long startedAt) {
      this.server = server;
      this.targets = new LinkedHashMap<>();
      for (Village village : villages) {
        this.targets.put(village.getID(),
            new Target(village.getID(), village.getName(), village.getLastBuildCompletedTime()));
      }
      this.requestedBuilds = requestedBuilds;
      this.maxTicks = maxTicks;
      this.startedAt = startedAt;
    }
  }

  private static final class Target {
    private final String villageId;
    private final String villageName;
    private long lastCompletionTime;
    private int completedBuilds;

    private Target(String villageId, String villageName, long lastCompletionTime) {
      this.villageId = villageId;
      this.villageName = villageName;
      this.lastCompletionTime = lastCompletionTime;
    }
  }
}
