package com.quzzar.kithkyn.raids;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.PersonEntityType;
import com.quzzar.kithkyn.entities.Kind;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.village.Village;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * One undead raid on one living village (docs/undead.md): the dead announce
 * themselves, then come in waves from the village's edge until every wave has
 * been beaten or they give up and withdraw. The village owns the raid and ticks
 * it once a second; the state is a compound in the village's strategy tag, so
 * a raid survives a restart with its raiders still in the world.
 *
 * Who the dead are for is remembered by the people, not enforced here: the
 * residents log who brought the raid down on them and who fought it off, and
 * reflection decides what that was worth.
 */
public final class UndeadRaid {

  public static final String TAG = "undead_raid";

  /** Vanilla's raid omen countdown: half a minute between the warning and the first wave. */
  static final int COUNTDOWN_TICKS = 600;
  /** A wave that will not die is reinforced after three minutes. */
  static final int WAVE_TIMEOUT_TICKS = 20 * 60 * 3;
  /** The dead give up twelve minutes after the first wave. */
  static final int RAID_TIMEOUT_TICKS = 20 * 60 * 12;
  /** How long the bar lingers with the outcome. */
  static final int AFTERMATH_TICKS = 200;
  /** How far beyond the village edge the bar is shown. */
  static final double BAR_MARGIN = 32.0D;
  /** A wave spawns scattered this many blocks around one edge point. */
  static final int SPAWN_SCATTER = 3;

  public enum Phase {
    COMING,
    FIGHTING,
    BEATEN,
    WITHDRAWN
  }

  private final String sourceName;
  @Nullable
  private final UUID culprit;
  private final int waves;
  private Phase phase;
  private int wave;
  private int waveSize;
  private long phaseAt;
  private long firstWaveAt;
  private final Set<UUID> raiders = new LinkedHashSet<>();
  private final Map<UUID, Integer> kills = new HashMap<>();
  @Nullable
  private ServerBossEvent bar;

  private UndeadRaid(String sourceName, @Nullable UUID culprit, int waves, Phase phase, int wave, int waveSize,
      long phaseAt, long firstWaveAt) {
    this.sourceName = sourceName;
    this.culprit = culprit;
    this.waves = waves;
    this.phase = phase;
    this.wave = wave;
    this.waveSize = waveSize;
    this.phaseAt = phaseAt;
    this.firstWaveAt = firstWaveAt;
  }

  /**
   * The dead set out. {@code sourceName} is who they are, as the village will
   * speak of them ("the dead of Barrowdown", "the restless dead"); the culprit
   * is who they followed, if anyone. {@code atOnce} skips the countdown, which
   * only the dev command wants.
   */
  public static UndeadRaid begin(String sourceName, @Nullable UUID culprit, int waves, long gameTime, boolean atOnce) {
    return new UndeadRaid(sourceName, culprit, waves, Phase.COMING, 0, 0,
        atOnce ? gameTime - COUNTDOWN_TICKS : gameTime, 0L);
  }

  public static UndeadRaid load(CompoundTag tag) {
    UndeadRaid raid = new UndeadRaid(
        tag.getString("source"),
        tag.hasUUID("culprit") ? tag.getUUID("culprit") : null,
        tag.getInt("waves"),
        Phase.valueOf(tag.getString("phase")),
        tag.getInt("wave"),
        tag.getInt("wave_size"),
        tag.getLong("phase_at"),
        tag.getLong("first_wave_at"));
    for (Tag entry : tag.getList("raiders", Tag.TAG_INT_ARRAY)) {
      raid.raiders.add(NbtUtils.loadUUID(entry));
    }
    CompoundTag killTag = tag.getCompound("kills");
    for (String key : killTag.getAllKeys()) {
      raid.kills.put(UUID.fromString(key), killTag.getInt(key));
    }
    return raid;
  }

  public CompoundTag save() {
    CompoundTag tag = new CompoundTag();
    tag.putString("source", sourceName);
    if (culprit != null) {
      tag.putUUID("culprit", culprit);
    }
    tag.putInt("waves", waves);
    tag.putString("phase", phase.name());
    tag.putInt("wave", wave);
    tag.putInt("wave_size", waveSize);
    tag.putLong("phase_at", phaseAt);
    tag.putLong("first_wave_at", firstWaveAt);
    ListTag raiderTag = new ListTag();
    for (UUID raider : raiders) {
      raiderTag.add(NbtUtils.createUUID(raider));
    }
    tag.put("raiders", raiderTag);
    CompoundTag killTag = new CompoundTag();
    kills.forEach((player, count) -> killTag.putInt(player.toString(), count));
    tag.put("kills", killTag);
    return tag;
  }

  public String sourceName() {
    return sourceName;
  }

  @Nullable
  public UUID culprit() {
    return culprit;
  }

  public Phase phase() {
    return phase;
  }

  public int wave() {
    return wave;
  }

  public int waves() {
    return waves;
  }

  public Set<UUID> raiders() {
    return Set.copyOf(raiders);
  }

  public Map<UUID, Integer> kills() {
    return Map.copyOf(kills);
  }

  long phaseAt() {
    return phaseAt;
  }

  /** Whether the countdown has run out at this game time. */
  boolean countdownOver(long gameTime) {
    return gameTime - phaseAt >= COUNTDOWN_TICKS;
  }

  /** One of the dead has fallen; the killer, if a player, is remembered as a defender. */
  public void raiderDied(UUID raider, @Nullable UUID killer) {
    raiders.remove(raider);
    if (killer != null) {
      kills.merge(killer, 1, Integer::sum);
    }
  }

  /**
   * Advances the raid by one second of village time. Returns true once the
   * raid is over and the village may forget it.
   */
  public boolean tick(Village village, ServerLevel level) {
    long now = level.getGameTime();
    switch (phase) {
      case COMING -> {
        if (countdownOver(now) && spawnWave(village, level, 1)) {
          phase = Phase.FIGHTING;
          firstWaveAt = now;
        }
      }
      case FIGHTING -> {
        prune(level);
        if (raiders.isEmpty()) {
          if (wave >= waves) {
            end(village, level, Phase.BEATEN);
          } else {
            spawnWave(village, level, wave + 1);
          }
        } else if (wave < waves && now - phaseAt >= WAVE_TIMEOUT_TICKS) {
          spawnWave(village, level, wave + 1);
        } else if (now - firstWaveAt >= RAID_TIMEOUT_TICKS) {
          withdraw(level);
          end(village, level, Phase.WITHDRAWN);
        }
      }
      case BEATEN, WITHDRAWN -> {
        if (now - phaseAt >= AFTERMATH_TICKS) {
          bar().removeAllPlayers();
          return true;
        }
      }
    }
    updateBar(village, level, now);
    return false;
  }

  /** Sends every raider still standing back to the dark: the raid is over without them. */
  public void withdraw(ServerLevel level) {
    for (UUID id : List.copyOf(raiders)) {
      Entity raider = level.getEntity(id);
      if (raider != null) {
        raider.discard();
      }
    }
    raiders.clear();
    bar().removeAllPlayers();
  }

  private void prune(ServerLevel level) {
    raiders.removeIf(id -> {
      Entity raider = level.getEntity(id);
      return raider == null || !raider.isAlive();
    });
  }

  /**
   * The next wave rises at the village's edge, scattered around one point so
   * they arrive as a band. Returns false when the edge is not loaded there, in
   * which case the same wave is tried again next second.
   */
  private boolean spawnWave(Village village, ServerLevel level, int number) {
    BlockPos anchor = village.edgeSpawnPosition();
    if (anchor == null) {
      return false;
    }
    int size = UndeadRaidPlan.waveSize(village.getPopulation().size(), number);
    int spawned = 0;
    for (int index = 0; index < size; index++) {
      BlockPos column = anchor.offset(
          level.random.nextInt(SPAWN_SCATTER * 2 + 1) - SPAWN_SCATTER, 0,
          level.random.nextInt(SPAWN_SCATTER * 2 + 1) - SPAWN_SCATTER);
      if (!level.isLoaded(column)) {
        continue;
      }
      BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
      RealPerson raider = spawnRaider(village, level, pos, number);
      if (raider != null) {
        raiders.add(raider.getUUID());
        spawned++;
      }
    }
    if (spawned == 0) {
      return false;
    }
    wave = number;
    waveSize = spawned;
    phaseAt = level.getGameTime();
    horn(village, level);
    Kithkyn.LOGGER.info("[raid] wave {} of {} on '{}': {} of {} rise at the edge", wave, waves, village.getName(),
        spawned, sourceName);
    return true;
  }

  @Nullable
  private RealPerson spawnRaider(Village village, ServerLevel level, BlockPos pos, int number) {
    RealPerson raider = PersonEntityType.PERSON.get().create(level);
    if (raider == null) {
      Kithkyn.LOGGER.error("Could not create a raider for the raid on '{}'", village.getName());
      return null;
    }
    raider.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
    raider.setKind(Kind.UNDEAD);
    raider.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
    raider.becomeRaider(village.getID(), sourceName);
    RaiderKit.arm(raider, UndeadRaidPlan.kit(raider.getUUID().getLeastSignificantBits(), number));
    level.addFreshEntity(raider);
    return raider;
  }

  private void end(Village village, ServerLevel level, Phase outcome) {
    phase = outcome;
    phaseAt = level.getGameTime();
    if (outcome == Phase.BEATEN) {
      Kithkyn.LOGGER.info("[raid] '{}' beat {}", village.getName(), sourceName);
      rememberDefenders(village, level);
      tell(village, level, capitalize(sourceName) + " are beaten at " + village.getName() + ".");
    } else {
      Kithkyn.LOGGER.info("[raid] {} withdrew from '{}'", sourceName, village.getName());
      tell(village, level, capitalize(sourceName) + " withdrew from " + village.getName() + ".");
    }
  }

  /** Every player who cut one of the dead down is remembered for it by every resident. */
  private void rememberDefenders(Village village, ServerLevel level) {
    for (Map.Entry<UUID, Integer> entry : kills.entrySet()) {
      String name = UndeadRaids.playerName(level, entry.getKey());
      for (UUID residentId : village.getPopulation()) {
        RealPerson resident = village.getPerson(level, residentId);
        if (resident != null) {
          resident.logMemory(name + " fought off " + sourceName + " when they came for " + village.getName() + ".",
              Optional.of(entry.getKey()));
        }
      }
    }
  }

  private void horn(Village village, ServerLevel level) {
    BlockPos center = village.centerPosition();
    if (center != null) {
      level.playSound(null, center.getX(), center.getY(), center.getZ(), SoundEvents.RAID_HORN, SoundSource.NEUTRAL,
          64.0F, 1.0F);
    }
  }

  /** A line to every player close enough to be part of it. */
  void tell(Village village, ServerLevel level, String line) {
    for (ServerPlayer player : playersNear(village, level)) {
      player.displayClientMessage(Component.literal(line), false);
    }
  }

  private ServerBossEvent bar() {
    if (bar == null) {
      bar = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
    }
    return bar;
  }

  private void updateBar(Village village, ServerLevel level, long now) {
    ServerBossEvent event = bar();
    switch (phase) {
      case COMING -> {
        event.setName(Component.literal(capitalize(sourceName) + " are coming for " + village.getName()));
        event.setColor(BossEvent.BossBarColor.RED);
        event.setProgress(Mth.clamp(1.0F - (now - phaseAt) / (float) COUNTDOWN_TICKS, 0.0F, 1.0F));
      }
      case FIGHTING -> {
        event.setName(Component.literal("Raid on " + village.getName() + ", wave " + wave + " of " + waves));
        event.setColor(BossEvent.BossBarColor.RED);
        event.setProgress(waveSize == 0 ? 0.0F : Mth.clamp(raiders.size() / (float) waveSize, 0.0F, 1.0F));
      }
      case BEATEN -> {
        event.setName(Component.literal("The dead are beaten"));
        event.setColor(BossEvent.BossBarColor.GREEN);
        event.setProgress(1.0F);
      }
      case WITHDRAWN -> {
        event.setName(Component.literal("The dead withdrew"));
        event.setColor(BossEvent.BossBarColor.YELLOW);
        event.setProgress(0.0F);
      }
    }
    Set<ServerPlayer> near = Set.copyOf(playersNear(village, level));
    for (ServerPlayer player : level.players()) {
      if (near.contains(player)) {
        event.addPlayer(player);
      } else {
        event.removePlayer(player);
      }
    }
  }

  private static List<ServerPlayer> playersNear(Village village, ServerLevel level) {
    BlockPos center = village.centerPosition();
    if (center == null) {
      return List.of();
    }
    double range = village.edgeRadius() + BAR_MARGIN;
    return level.players().stream()
        .filter(player -> player.blockPosition().distSqr(center) <= range * range)
        .toList();
  }

  static String capitalize(String text) {
    return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
  }
}
