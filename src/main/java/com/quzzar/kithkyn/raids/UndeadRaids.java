package com.quzzar.kithkyn.raids;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.configuration.KithkynConfig;
import com.quzzar.kithkyn.entities.Kind;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.VillageManager;
import com.quzzar.kithkyn.wrongdoing.Standing;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

/**
 * When the dead follow someone home (docs/undead.md). Two omens start a raid,
 * and both are the player's doing: the grudge of an undead village whose
 * standing with the player has fallen far enough, carried into the next living
 * village they enter, and an ominous bottle, whose Raid Omen the pillager
 * switch refuses and answers with the undead instead. Neither happens while
 * pillagers are not replaced, in peace, or for a creative player.
 */
public final class UndeadRaids {

  /** How often the grudge scan runs, in ticks. */
  private static final int SCAN_INTERVAL = 100;
  /** Who the dead are when no undead village stands behind them. */
  static final String NAMELESS_DEAD = "the restless dead";

  private UndeadRaids() {
  }

  /**
   * Every few seconds: a player standing in a living village, who has earned
   * the grudge of an undead village, brings its dead down on their hosts.
   */
  public static void scanGrudges(ServerLevel level, Collection<Village> villages) {
    if (!KithkynConfig.ReplacePillagers || level.getGameTime() % SCAN_INTERVAL != 0
        || level.getDifficulty() == Difficulty.PEACEFUL) {
      return;
    }
    for (ServerPlayer player : level.players()) {
      if (player.isCreative() || player.isSpectator()) {
        continue;
      }
      Village target = livingVillageAround(villages, player.blockPosition());
      if (target == null || target.isUnderRaid()) {
        continue;
      }
      Village source = grudgeSource(villages, level, player);
      if (source != null && begin(level, target, source, player, false)) {
        source.recordGrudgeRaid(player.getUUID(), level.getGameTime() + cooldownTicks());
      }
    }
  }

  /**
   * The bottle's purpose: the drinker stood in a village and the omen ripened.
   * The nearest undead village lends the dead their name; with none in the
   * world, the restless dead come nameless.
   */
  public static void omen(ServerPlayer player) {
    if (!(player.level() instanceof ServerLevel level) || level.getDifficulty() == Difficulty.PEACEFUL) {
      return;
    }
    Collection<Village> villages = VillageManager.get(level).getVillages().values();
    Village target = livingVillageAround(villages, player.blockPosition());
    if (target == null || target.isUnderRaid()) {
      player.displayClientMessage(Component.literal("The omen finds no one here to answer it."), true);
      return;
    }
    begin(level, target, nearestUndead(villages, target), player, false);
  }

  /**
   * The dead set out against a living village. Returns false in peace or
   * while the village is already under raid. The residents remember who
   * brought this on them; what they make of it is reflection's business.
   */
  public static boolean begin(ServerLevel level, Village target, @Nullable Village source,
      @Nullable ServerPlayer culprit, boolean atOnce) {
    int waves = UndeadRaidPlan.waves(level.getDifficulty());
    if (waves == 0 || target.getKind() != Kind.LIVING) {
      return false;
    }
    String sourceName = source == null ? NAMELESS_DEAD : "the dead of " + source.getName();
    UndeadRaid raid = UndeadRaid.begin(sourceName, culprit == null ? null : culprit.getUUID(), waves,
        level.getGameTime(), atOnce);
    if (!target.beginRaid(raid)) {
      return false;
    }
    String culpritName = culprit == null ? "" : culprit.getGameProfile().getName();
    for (UUID residentId : target.getPopulation()) {
      RealPerson resident = target.getPerson(level, residentId);
      if (resident != null) {
        resident.logMemory(UndeadRaid.capitalize(sourceName) + " came for " + target.getName()
            + (culprit == null ? "." : ", following " + culpritName + "."),
            Optional.ofNullable(culprit == null ? null : culprit.getUUID()));
      }
    }
    raid.tell(target, level, UndeadRaid.capitalize(sourceName)
        + (culprit == null ? " are coming for " : " have followed " + culpritName + " to ") + target.getName() + ".");
    Kithkyn.LOGGER.info("[raid] {} set out for '{}'{}", sourceName, target.getName(),
        culprit == null ? "" : " following " + culpritName);
    return true;
  }

  /** A raider fell; the raid crosses them off and credits the player who did it. */
  public static void onRaiderDeath(RealPerson raider, DamageSource source) {
    if (!(raider.level() instanceof ServerLevel level)) {
      return;
    }
    Village target = VillageManager.get(level).getVillage(raider.getRaidTargetVillageId());
    UndeadRaid raid = target == null ? null : target.getRaid();
    if (raid == null) {
      return;
    }
    UUID killer = null;
    if (raider.getKillCredit() instanceof Player player) {
      killer = player.getUUID();
    } else if (source.getEntity() instanceof Player player) {
      killer = player.getUUID();
    }
    raid.raiderDied(raider.getUUID(), killer);
  }

  /** The living village whose edge the position lies within, the nearest when edges overlap. */
  @Nullable
  public static Village livingVillageAround(Collection<Village> villages, BlockPos pos) {
    return villages.stream()
        .filter(village -> village.getKind() == Kind.LIVING && village.contains(pos))
        .min(Comparator.comparingDouble(village -> village.centerPosition().distSqr(pos)))
        .orElse(null);
  }

  /** The nearest undead village whose dead this player has earned, or null. */
  @Nullable
  static Village grudgeSource(Collection<Village> villages, ServerLevel level, ServerPlayer player) {
    long now = level.getGameTime();
    return villages.stream()
        .filter(village -> village.getKind() == Kind.UNDEAD && village.centerPosition() != null)
        .filter(village -> !village.grudgeOnCooldown(player.getUUID(), now))
        .filter(village -> Standing.of(village, level, player.getUUID()) <= KithkynConfig.UndeadRaidStandingBelow)
        .min(Comparator.comparingDouble(village -> village.centerPosition().distSqr(player.blockPosition())))
        .orElse(null);
  }

  @Nullable
  public static Village nearestUndead(Collection<Village> villages, Village target) {
    BlockPos center = target.centerPosition();
    if (center == null) {
      return null;
    }
    return villages.stream()
        .filter(village -> village.getKind() == Kind.UNDEAD && village.centerPosition() != null)
        .min(Comparator.comparingDouble(village -> village.centerPosition().distSqr(center)))
        .orElse(null);
  }

  static long cooldownTicks() {
    return KithkynConfig.UndeadRaidCooldownDays * 24000L;
  }

  static String playerName(ServerLevel level, UUID player) {
    ServerPlayer online = level.getServer().getPlayerList().getPlayer(player);
    return online == null ? "someone" : online.getGameProfile().getName();
  }
}
