package com.quzzar.kithkyn.raids;

import java.util.Collection;
import java.util.Comparator;

import javax.annotation.Nullable;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.quzzar.kithkyn.entities.Kind;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.VillageManager;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /kkdev raid}: bring the dead down on a village without earning it.
 * {@code start} raids the village around the caller (or the nearest living
 * one), {@code start now} skips the countdown, {@code stop} calls the dead
 * off, {@code status} reports every raid in the level, and {@code preview}
 * is the UI preview harness's hook: it stands the caller at a vantage point
 * over the nearest living village and starts a raid on it at once.
 */
public final class RaidCommands {

  private static final double NEAREST_VILLAGE_REACH = 128.0D;

  private RaidCommands() {
  }

  public static LiteralArgumentBuilder<CommandSourceStack> branch() {
    return Commands.literal("raid")
        .then(Commands.literal("start")
            .executes(context -> start(context.getSource(), false))
            .then(Commands.literal("now")
                .executes(context -> start(context.getSource(), true))))
        .then(Commands.literal("stop")
            .executes(context -> stop(context.getSource())))
        .then(Commands.literal("status")
            .executes(context -> status(context.getSource())))
        .then(Commands.literal("preview")
            .executes(context -> preview(context.getSource())));
  }

  private static int start(CommandSourceStack source, boolean atOnce) {
    ServerLevel level = source.getLevel();
    Collection<Village> villages = VillageManager.get(level).getVillages().values();
    BlockPos here = BlockPos.containing(source.getPosition());
    Village target = UndeadRaids.livingVillageAround(villages, here);
    if (target == null) {
      target = nearestLiving(villages, here, NEAREST_VILLAGE_REACH);
    }
    if (target == null) {
      source.sendFailure(Component.literal("No living village within reach to raid."));
      return 0;
    }
    ServerPlayer culprit = source.getEntity() instanceof ServerPlayer player ? player : null;
    if (!UndeadRaids.begin(level, target, UndeadRaids.nearestUndead(villages, target), culprit, atOnce)) {
      source.sendFailure(Component.literal("'" + target.getName()
          + "' cannot be raided: peaceful difficulty, or the dead are already at its gate."));
      return 0;
    }
    String name = target.getName();
    source.sendSuccess(() -> Component.literal("The dead set out for '" + name + "'" + (atOnce ? " at once." : ".")), true);
    return 1;
  }

  private static int stop(CommandSourceStack source) {
    int stopped = 0;
    for (Village village : VillageManager.get(source.getLevel()).getVillages().values()) {
      if (village.isUnderRaid()) {
        village.endRaid();
        stopped++;
      }
    }
    int count = stopped;
    source.sendSuccess(() -> Component.literal("Called the dead off " + count + " village(s)."), true);
    return stopped;
  }

  private static int status(CommandSourceStack source) {
    StringBuilder report = new StringBuilder();
    for (Village village : VillageManager.get(source.getLevel()).getVillages().values()) {
      UndeadRaid raid = village.getRaid();
      if (raid == null) {
        continue;
      }
      report.append(village.getName()).append(": ").append(raid.sourceName()).append(", ")
          .append(raid.phase().name().toLowerCase(java.util.Locale.ROOT))
          .append(", wave ").append(raid.wave()).append(" of ").append(raid.waves())
          .append(", ").append(raid.raiders().size()).append(" standing\n");
    }
    String text = report.isEmpty() ? "No village is under raid." : report.toString().strip();
    source.sendSuccess(() -> Component.literal(text), false);
    return 1;
  }

  /** The preview harness's hook: a vantage point over the nearest living village, and the dead at once. */
  private static int preview(CommandSourceStack source) {
    if (!(source.getEntity() instanceof ServerPlayer player)) {
      source.sendFailure(Component.literal("The raid preview needs a player to stand somewhere."));
      return 0;
    }
    ServerLevel level = source.getLevel();
    Collection<Village> villages = VillageManager.get(level).getVillages().values();
    Village target = nearestLiving(villages, player.blockPosition(), Double.MAX_VALUE);
    BlockPos center = target == null ? null : target.centerPosition();
    if (target == null || center == null) {
      source.sendFailure(Component.literal("No living village to preview a raid on."));
      return 0;
    }
    // Aimed at the middle of the buildings themselves, not the centre's stored
    // anchor, from high enough and far enough back for the village's whole
    // reach to share the frame with the edge the dead walk in from.
    double focusX = 0.0D;
    double focusZ = 0.0D;
    int counted = 0;
    for (com.quzzar.kithkyn.village.buildings.Building building : target.getBuildings()) {
      BlockPos middle = BlockPos.of(building.getCenterLocation());
      focusX += middle.getX();
      focusZ += middle.getZ();
      counted++;
    }
    focusX = counted == 0 ? center.getX() : focusX / counted;
    focusZ = counted == 0 ? center.getZ() : focusZ / counted;
    double reach = Math.max(24.0D, target.edgeRadius());
    double x = focusX + 0.5D;
    double y = center.getY() + reach * 0.9D;
    double z = focusZ - reach - 8.0D;
    double dx = focusX + 0.5D - x;
    double dz = focusZ + 0.5D - z;
    double dy = center.getY() - (y + player.getEyeHeight());
    float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
    float pitch = (float) Math.toDegrees(-Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
    // The vanilla command, not a direct teleport: a direct one issued this early
    // in a join is lost, and the command is what the other previews rely on.
    source.getServer().getCommands().performPrefixedCommand(source, String.format(java.util.Locale.ROOT,
        "tp %s %.2f %.2f %.2f %.1f %.1f", player.getGameProfile().getName(), x, y, z, yaw, pitch));
    // A creative player teleported into the sky falls out of it unless already
    // flying; the first raid shots were taken from the lawn under the vantage.
    if (player.getAbilities().mayfly) {
      player.getAbilities().flying = true;
      player.onUpdateAbilities();
    }
    if (!UndeadRaids.begin(level, target, UndeadRaids.nearestUndead(villages, target), player, true)) {
      source.sendFailure(Component.literal("'" + target.getName() + "' cannot be raided right now."));
      return 0;
    }
    return 1;
  }

  @Nullable
  private static Village nearestLiving(Collection<Village> villages, BlockPos pos, double reach) {
    return villages.stream()
        .filter(village -> village.getKind() == Kind.LIVING && village.centerPosition() != null)
        .filter(village -> village.centerPosition().distSqr(pos) <= reach * reach)
        .min(Comparator.comparingDouble(village -> village.centerPosition().distSqr(pos)))
        .orElse(null);
  }
}
