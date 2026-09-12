package com.quzzar.kithkyn.dev;

import java.util.ArrayDeque;
import java.util.Comparator;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.VillageManager;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Safely thins a development world to the single village containing the operator. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class VillageCleanup {

  private static final long TICKS_BETWEEN_REMOVALS = 20L;

  private static Session active;

  private VillageCleanup() {
  }

  /** Whether this server currently owns the one gradual cleanup session. */
  public static boolean isRunning(MinecraftServer server) {
    return active != null && active.server == server;
  }

  /** Command branch mounted at {@code /kkdev village cleanup}. */
  public static LiteralArgumentBuilder<CommandSourceStack> branch() {
    return Commands.literal("cleanup")
        .then(Commands.literal("start").executes(ctx -> start(ctx.getSource())))
        .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
        .then(Commands.literal("stop").executes(ctx -> stop(ctx.getSource())));
  }

  private static int start(CommandSourceStack source) {
    if (source.getLevel().dimension() != Level.OVERWORLD) {
      source.sendFailure(Component.literal("Village cleanup must be started in the Overworld."));
      return 0;
    }
    if (!(source.getEntity() instanceof ServerPlayer player)) {
      source.sendFailure(Component.literal("Stand inside the village to keep, then run this as a player."));
      return 0;
    }
    if (active != null) {
      source.sendFailure(Component.literal("A village cleanup is already running: " + describe(active)));
      return 0;
    }
    MinecraftServer server = source.getServer();
    if (server.tickRateManager().isSprinting() || VillageTimelapse.isRunning(server)) {
      source.sendFailure(Component.literal(
          "Stop the tick sprint or village timelapse before starting village cleanup."));
      return 0;
    }

    ServerLevel level = server.overworld();
    var villages = VillageManager.get(level);
    BlockPos position = player.blockPosition();
    var containing = villages.getVillages().values().stream()
        .filter(village -> village.contains(position))
        .toList();
    if (containing.isEmpty()) {
      Village nearest = villages.getNearestVillage(position);
      source.sendFailure(Component.literal(nearest == null
          ? "No villages exist yet."
          : "You are not inside a village. The nearest is '" + nearest.getName() + "'."));
      return 0;
    }
    if (containing.size() > 1) {
      source.sendFailure(Component.literal("Cleanup refused: your position is inside more than one village ("
          + containing.stream().map(Village::getName).sorted(String.CASE_INSENSITIVE_ORDER)
              .collect(java.util.stream.Collectors.joining(", "))
          + ")."));
      return 0;
    }

    Village kept = containing.getFirst();
    ArrayDeque<String> removals = villages.getVillages().values().stream()
        .filter(village -> !village.getID().equals(kept.getID()))
        .sorted(Comparator.comparing(Village::getName, String.CASE_INSENSITIVE_ORDER))
        .map(Village::getID)
        .collect(java.util.stream.Collectors.toCollection(ArrayDeque::new));
    if (removals.isEmpty()) {
      source.sendSuccess(() -> Component.literal("'" + kept.getName()
          + "' is already the only village in this world."), false);
      return 1;
    }

    active = new Session(server, kept.getID(), kept.getName(), removals, level.getGameTime());
    source.sendSuccess(() -> Component.literal("Keeping '" + kept.getName() + "' and deleting "
        + removals.size() + " other villages, one per second. Use '/kkdev village cleanup stop' to cancel."), true);
    return 1;
  }

  private static int status(CommandSourceStack source) {
    Session session = active;
    if (session == null) {
      source.sendSuccess(() -> Component.literal("No village cleanup is running."), false);
      return 1;
    }
    source.sendSuccess(() -> Component.literal(describe(session)), false);
    return 1;
  }

  private static int stop(CommandSourceStack source) {
    Session session = active;
    if (session == null) {
      source.sendFailure(Component.literal("No village cleanup is running."));
      return 0;
    }
    if (session.server != source.getServer()) {
      source.sendFailure(Component.literal("The active village cleanup belongs to another server."));
      return 0;
    }
    active = null;
    source.sendSuccess(() -> Component.literal("Village cleanup stopped after deleting " + session.removed
        + " of " + session.total + " villages. '" + session.keptName + "' was preserved."), true);
    return 1;
  }

  /** Advances one queued teardown after each normal one-second interval. */
  @SubscribeEvent
  public static void onServerTick(ServerTickEvent.Post event) {
    Session session = active;
    if (session == null || session.server != event.getServer()
        || session.server.tickRateManager().isSprinting()) {
      return;
    }
    ServerLevel level = session.server.overworld();
    long now = level.getGameTime();
    if (now < session.nextRemovalAt) {
      return;
    }
    var villages = VillageManager.get(level);
    if (villages.getVillage(session.keptId) == null) {
      finish(session, "stopped because the village being kept no longer exists");
      return;
    }

    String villageId = session.pending.pollFirst();
    if (villageId == null) {
      finish(session, "complete");
      return;
    }
    Village village = villages.getVillage(villageId);
    if (village != null) {
      Village.Removal removal = villages.removeVillage(level, village);
      session.removed++;
      announce(session.server, "Village cleanup: deleted '" + village.getName() + "' ("
          + removal.people() + " people, " + removal.blocks() + " placed blocks). "
          + session.pending.size() + " villages remain to remove.");
    }
    session.nextRemovalAt = now + TICKS_BETWEEN_REMOVALS;
    if (session.pending.isEmpty()) {
      finish(session, "complete");
    }
  }

  /** An integrated server can stop without ending the client JVM. */
  @SubscribeEvent
  public static void onServerStopping(ServerStoppingEvent event) {
    if (active != null && active.server == event.getServer()) {
      active = null;
    }
  }

  private static void finish(Session session, String reason) {
    if (active != session) {
      return;
    }
    active = null;
    announce(session.server, "Village cleanup " + reason + ": deleted " + session.removed + " of "
        + session.total + " villages and preserved '" + session.keptName + "'.");
  }

  private static String describe(Session session) {
    return "Village cleanup is preserving '" + session.keptName + "': " + session.removed + "/"
        + session.total + " villages deleted, " + session.pending.size() + " queued.";
  }

  private static void announce(MinecraftServer server, String message) {
    server.createCommandSourceStack().sendSuccess(() -> Component.literal(message), true);
  }

  private static final class Session {
    private final MinecraftServer server;
    private final String keptId;
    private final String keptName;
    private final ArrayDeque<String> pending;
    private final int total;
    private long nextRemovalAt;
    private int removed;

    private Session(MinecraftServer server, String keptId, String keptName,
        ArrayDeque<String> pending, long nextRemovalAt) {
      this.server = server;
      this.keptId = keptId;
      this.keptName = keptName;
      this.pending = pending;
      this.total = pending.size();
      this.nextRemovalAt = nextRemovalAt;
    }
  }
}
