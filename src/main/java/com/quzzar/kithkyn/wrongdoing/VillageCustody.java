package com.quzzar.kithkyn.wrongdoing;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.village.Occupation;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.VillageGolems;
import com.quzzar.kithkyn.village.VillageManager;
import com.quzzar.kithkyn.village.buildings.Building;
import com.quzzar.kithkyn.village.buildings.WorkerFooting;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** One saved custody ledger, shared by guard combat, player login, sentence expiry and escape. */
public final class VillageCustody extends SavedData {
  public static final int SENTENCE_TICKS = 5 * 60 * 20;
  public static final int GRACE_TICKS = 30 * 20;
  private static final String DATA_NAME = Kithkyn.MODID + "~custody";
  public static final Factory<VillageCustody> FACTORY = new Factory<>(VillageCustody::new, VillageCustody::load, null);
  private final Map<UUID, Sentence> sentences = new HashMap<>();

  /** Game time continues while the server runs, including time the prisoner spends logged out. */
  public record Sentence(UUID player, String village, String castle, String dimension, BlockPos cell,
      BlockPos release, long startedAt, long releaseAt, long graceUntil, int announcedMinutes,
      @Nullable CompoundTag previousFatigue) {
    public boolean jailed() { return graceUntil == 0; }
    public boolean protects(String villageId, long now) {
      return village.equals(villageId) && (jailed() || now < graceUntil);
    }
    public boolean occupies(String villageId, String castleId, long now) {
      return jailed() && now < releaseAt && village.equals(villageId) && castle.equals(castleId);
    }
    public Sentence released(long now) {
      return new Sentence(player, village, castle, dimension, cell, release, startedAt, releaseAt, now + GRACE_TICKS, 0, null);
    }
    public Sentence announced(int minutes) {
      return new Sentence(player, village, castle, dimension, cell, release, startedAt, releaseAt, graceUntil, minutes, previousFatigue);
    }
  }

  public VillageCustody() { }

  public static VillageCustody get(ServerLevel level) {
    return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
  }

  private static long now(ServerLevel level) { return level.getServer().overworld().getGameTime(); }

  /** Only actual village defenders may apprehend someone or invoke the custody ceasefire. */
  @Nullable
  public static Village guardVillage(@Nullable Entity entity) {
    if (entity instanceof RealPerson person && person.getOccupation() == Occupation.GUARD) return person.getVillage();
    return entity instanceof AbstractGolem golem ? VillageGolems.village(golem) : null;
  }

  /** The damage boundary has already applied armor and magic; absorption remains to be deducted. */
  public static boolean isLethal(float damageBeforeAbsorption, float health, float absorption) {
    return health > 0 && damageBeforeAbsorption > 0 && damageBeforeAbsorption - Math.max(0, absorption) >= health;
  }

  public static boolean protects(Village village, ServerPlayer player) {
    Sentence sentence = get(player.serverLevel()).sentences.get(player.getUUID());
    return sentence != null && sentence.protects(village.getID(), now(player.serverLevel()));
  }

  /** No usable vacant cell means ordinary guard damage retains its normal result. */
  public static boolean apprehend(Village village, ServerPlayer player, DamageSource source) {
    ServerLevel level = village.getLevel();
    if (level == null || level != player.serverLevel() || player.isCreative() || player.isSpectator()
        || Standing.tierOf(village, level, player.getUUID()) != Standing.Tier.HOSTILE) return false;
    VillageCustody ledger = get(level);
    Sentence prior = ledger.sentences.get(player.getUUID());
    long now = now(level);
    if (prior != null && (prior.jailed() || prior.protects(village.getID(), now))) return false;
    for (Building castle : village.getBuildings()) {
      var info = castle.getInfo();
      var layout = info == null ? null : info.getCastleLayout();
      if (layout == null || !info.getCategory().equals("castle") || village.isBeingRebuilt(castle.getUUID())) continue;
      BlockPos cell = world(castle, layout.custodyCell());
      if (!validCell(level, cell) || ledger.sentences.values().stream()
          .anyMatch(sentence -> sentence.occupies(village.getID(), castle.getUUID().toString(), now))) continue;
      BlockPos release = safeReleasePoint(level, cell, world(castle, layout.releasePoint()));
      if (release == null) continue;
      List<Container> evidence = layout.evidenceContainers().stream().map(local -> level.getBlockEntity(world(castle, local)))
          .filter(entity -> entity instanceof BarrelBlockEntity).map(entity -> (Container) entity).distinct().toList();
      if (!EvidenceInventory.canArrestWithoutDrops(player, evidence)) continue;
      MobEffectInstance fatigue = player.getEffect(MobEffects.DIG_SLOWDOWN);
      CompoundTag previous = fatigue == null ? null : (CompoundTag) fatigue.save();
      Sentence sentence = new Sentence(player.getUUID(), village.getID(), castle.getUUID().toString(),
          level.dimension().location().toString(), cell, release, now, now + SENTENCE_TICKS, 0, 5, previous);
      ledger.sentences.put(player.getUUID(), sentence);
      ledger.setDirty();
      player.setHealth(Math.max(1.0F, player.getHealth()));
      teleport(player, level, cell);
      int confiscated = EvidenceInventory.confiscate(player, source, evidence);
      if (fatigue != null && fatigue.getAmplifier() <= 2) player.removeEffect(MobEffects.DIG_SLOWDOWN);
      applyFatigue(player, sentence, now);
      clearGuardTargets(level, village, player);
      player.sendSystemMessage(Component.literal("The guards of " + village.getName()
          + " have taken you into custody. You have 5 minutes remaining. "
          + confiscated + " items were placed in the evidence barrels; anything that did not fit remains with you."));
      Kithkyn.LOGGER.info("[custody] {} apprehended by {} until game tick {}", player.getUUID(), village.getID(), sentence.releaseAt());
      return true;
    }
    return false;
  }

  /** Read-only status used by diagnostics and native verification. */
  @Nullable
  public Sentence getSentence(UUID player) { return sentences.get(player); }

  /** Offline completed sentences remain until login; expired grace records can be discarded immediately. */
  public static void tickServer(ServerLevel level) {
    VillageCustody ledger = get(level);
    long now = now(level);
    if (ledger.sentences.values().removeIf(sentence -> !sentence.jailed() && now >= sentence.graceUntil())) ledger.setDirty();
  }

  /** Login and the ordinary player tick share recovery, so neither requires a second custody state. */
  public static void tickPlayer(ServerPlayer player) {
    VillageCustody ledger = get(player.serverLevel());
    Sentence sentence = ledger.sentences.get(player.getUUID());
    if (sentence == null) return;
    long now = now(player.serverLevel());
    if (!sentence.jailed()) {
      if (now >= sentence.graceUntil()) {
        ledger.sentences.remove(player.getUUID());
        ledger.setDirty();
      }
      return;
    }
    ResourceLocation dimension = ResourceLocation.tryParse(sentence.dimension());
    ServerLevel custodyLevel = dimension == null ? null : player.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
    if (now >= sentence.releaseAt()) {
      ledger.release(player, sentence, custodyLevel, true, "Your sentence is complete. You have thirty seconds to leave peacefully.");
      return;
    }
    if (custodyLevel != player.serverLevel()) {
      ledger.release(player, sentence, custodyLevel, false, "You escaped custody. You have thirty seconds before the village guards pursue you again.");
      return;
    }
    Village village = VillageManager.get(player.serverLevel()).getVillage(sentence.village());
    Building castle = village == null ? null : village.getBuildings().stream()
        .filter(building -> building.getUUID().toString().equals(sentence.castle()) && building.getInfo() != null
            && building.getInfo().getCastleLayout() != null)
        .findFirst().orElse(null);
    if (castle == null || village.isBeingRebuilt(castle.getUUID()) || !validCell(player.serverLevel(), sentence.cell())) {
      ledger.release(player, sentence, custodyLevel, true, "The cell is no longer usable. You have been released with thirty seconds to leave.");
      return;
    }
    if (escaped(player.serverLevel(), player.position(), sentence.cell())) {
      ledger.release(player, sentence, custodyLevel, false, "You escaped custody. You have thirty seconds before the village guards pursue you again.");
      return;
    }
    applyFatigue(player, sentence, now);
    int minutes = (int) Math.ceil((sentence.releaseAt() - now) / 1200.0D);
    if (minutes > 0 && minutes < sentence.announcedMinutes()) {
      player.sendSystemMessage(Component.literal("You have " + minutes + (minutes == 1 ? " minute" : " minutes") + " remaining in custody."));
      ledger.sentences.put(player.getUUID(), sentence.announced(minutes));
      ledger.setDirty();
    }
  }

  /** The authored cell is one standing column; jumping inside it is allowed, leaving it ends custody. */
  static boolean escaped(Vec3 position, BlockPos cell) {
    return Math.abs(position.x - (cell.getX() + 0.5D)) > 0.95D
        || Math.abs(position.z - (cell.getZ() + 0.5D)) > 0.95D
        || position.y < cell.getY() - 0.5D || position.y > cell.getY() + 2.0D;
  }

  /** An enclosed room may contain several standing tiles; crossing its physical boundary is escape. */
  static boolean escaped(ServerLevel level, Vec3 position, BlockPos cell) {
    if (position.y < cell.getY() - 0.5D || position.y > cell.getY() + 2.0D) return true;
    BlockPos occupied = BlockPos.containing(position.x, cell.getY(), position.z);
    return !cellInterior(level, cell).contains(occupied);
  }

  /** Death from another source ends custody rather than sending a respawned player back into prison. */
  public static void died(ServerPlayer player) {
    VillageCustody ledger = get(player.serverLevel());
    Sentence sentence = ledger.sentences.get(player.getUUID());
    if (sentence != null && sentence.jailed()) {
      ledger.sentences.put(player.getUUID(), sentence.released(now(player.serverLevel())));
      ledger.setDirty();
    }
  }

  /** A missing wall, unsupported floor, flooded room or blocked body prevents an arrest. */
  public static boolean validCell(ServerLevel level, BlockPos cell) {
    return !cellInterior(level, cell).isEmpty();
  }

  /** Flood-fills one flat jail room, bounded to keep an open castle or courtyard from becoming a cell. */
  private static Set<BlockPos> cellInterior(ServerLevel level, BlockPos cell) {
    if (!level.hasChunkAt(cell) || !WorkerFooting.canStand(level, cell)) return Set.of();
    Set<BlockPos> interior = new HashSet<>();
    ArrayDeque<BlockPos> open = new ArrayDeque<>();
    interior.add(cell.immutable());
    open.add(cell.immutable());
    while (!open.isEmpty()) {
      BlockPos current = open.removeFirst();
      for (Direction side : Direction.Plane.HORIZONTAL) {
        BlockPos neighbor = current.relative(side);
        if (!level.hasChunkAt(neighbor)) return Set.of();
        if (blocksCellBoundary(level, neighbor)) continue;
        if (WorkerFooting.canStand(level, neighbor)) {
          if (interior.add(neighbor.immutable())) {
            if (interior.size() > 64) return Set.of();
            open.addLast(neighbor.immutable());
          }
          continue;
        }
        return Set.of();
      }
    }
    return Set.copyOf(interior);
  }

  private static boolean blocksCellBoundary(ServerLevel level, BlockPos position) {
    for (BlockPos part : List.of(position, position.above())) {
      BlockState state = level.getBlockState(part);
      if ((state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.OPEN))
          || (state.getBlock() instanceof FenceGateBlock && state.getValue(FenceGateBlock.OPEN))) {
        continue;
      }
      if (!state.getCollisionShape(level, part).isEmpty()) return true;
    }
    return false;
  }

  private void release(ServerPlayer player, Sentence sentence, @Nullable ServerLevel level, boolean relocate, String message) {
    if (relocate && level != null) {
      level.getChunkAt(sentence.release());
      BlockPos exit = safeReleasePoint(level, sentence.cell(), sentence.release());
      if (exit == null) {
        Village village = VillageManager.get(level).getVillage(sentence.village());
        if (village != null && village.getTownCenter() != null && village.getTownCenter().getInfo() != null) {
          level.getChunkAt(village.getCenterPosition());
          exit = safeReleasePoint(level, sentence.cell(), village.getCenterPosition());
        }
      }
      if (exit == null) {
        level = level.getServer().overworld();
        BlockPos spawn = level.getSharedSpawnPos();
        level.getChunkAt(spawn);
        exit = safeReleasePoint(level, sentence.cell(), level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn));
      }
      if (exit != null) teleport(player, level, exit);
      else Kithkyn.LOGGER.error("[custody] No safe castle, village or world-spawn release footing for {}; ending custody at their current position", player.getUUID());
    }
    restoreFatigue(player, sentence, now(player.serverLevel()));
    sentences.put(player.getUUID(), sentence.released(now(player.serverLevel())));
    setDirty();
    player.sendSystemMessage(Component.literal(message));
    Kithkyn.LOGGER.info("[custody] {} released from {}", player.getUUID(), sentence.village());
  }

  private static void applyFatigue(ServerPlayer player, Sentence sentence, long now) {
    int duration = (int) Math.max(1, Math.min(SENTENCE_TICKS, sentence.releaseAt() - now));
    MobEffectInstance current = player.getEffect(MobEffects.DIG_SLOWDOWN);
    if (current != null && current.getAmplifier() > 2) return;
    if (current == null || current.getAmplifier() < 2 || current.getDuration() < duration - 20) {
      // The ambient, particle-free effect identifies this sentence's contribution on early release.
      player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration, 2, true, false, true));
    }
  }

  private static void restoreFatigue(ServerPlayer player, Sentence sentence, long now) {
    MobEffectInstance current = player.getEffect(MobEffects.DIG_SLOWDOWN);
    if (current == null || current.getAmplifier() == 2 && current.isAmbient() && !current.isVisible()) {
      if (current != null) player.removeEffect(MobEffects.DIG_SLOWDOWN);
      if (sentence.previousFatigue() != null) {
        CompoundTag previous = sentence.previousFatigue().copy();
        int duration = previous.getInt("duration");
        if (duration != -1) previous.putInt("duration", Math.max(0, duration - (int) (now - sentence.startedAt())));
        MobEffectInstance restored = MobEffectInstance.load(previous);
        if (restored != null && (restored.isInfiniteDuration() || restored.getDuration() > 0)) player.addEffect(restored);
      }
    }
  }

  private static void clearGuardTargets(ServerLevel level, Village village, ServerPlayer player) {
    for (Entity entity : level.getAllEntities()) {
      if (entity instanceof Mob mob && mob.getTarget() == player && guardVillage(entity) == village) {
        mob.setTarget(null);
        mob.setLastHurtByMob(null);
        mob.getNavigation().stop();
      }
    }
  }

  private static void teleport(ServerPlayer player, ServerLevel level, BlockPos position) {
    double floor = position.getY() - 1 + level.getBlockState(position.below())
        .getCollisionShape(level, position.below()).max(Direction.Axis.Y);
    player.stopRiding();
    player.teleportTo(level, position.getX() + 0.5D, floor, position.getZ() + 0.5D, player.getYRot(), player.getXRot());
    player.setDeltaMovement(Vec3.ZERO);
    player.fallDistance = 0;
  }

  @Nullable
  private static BlockPos safeReleasePoint(ServerLevel level, BlockPos cell, BlockPos requested) {
    Set<BlockPos> interior = cellInterior(level, cell);
    for (BlockPos candidate : BlockPos.withinManhattan(requested, 6, 4, 6)) {
      boolean outsideCell = interior.isEmpty() || interior.stream().allMatch(tile ->
          Math.abs(candidate.getX() - tile.getX()) > 1 || Math.abs(candidate.getZ() - tile.getZ()) > 1);
      if (outsideCell
          && level.hasChunkAt(candidate) && WorkerFooting.canStand(level, candidate)) return candidate.immutable();
    }
    return null;
  }

  private static BlockPos world(Building building, BlockPos local) {
    return BlockPos.of(building.getOriginLocation()).offset(local.rotate(building.getRotation()));
  }

  public static VillageCustody load(CompoundTag tag, HolderLookup.Provider registries) {
    VillageCustody ledger = new VillageCustody();
    for (var entry : tag.getList("sentences", 10)) {
      CompoundTag saved = (CompoundTag) entry;
      if (!saved.hasUUID("player")) continue;
      UUID player = saved.getUUID("player");
      ledger.sentences.put(player, new Sentence(player, saved.getString("village"), saved.getString("castle"),
          saved.getString("dimension"), BlockPos.of(saved.getLong("cell")), BlockPos.of(saved.getLong("release")),
          saved.getLong("started"), saved.getLong("until"), saved.getLong("grace"), saved.getInt("announced_minutes"),
          saved.contains("previous_fatigue", 10) ? saved.getCompound("previous_fatigue").copy() : null));
    }
    return ledger;
  }

  @Override
  public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
    ListTag saved = new ListTag();
    for (Sentence sentence : sentences.values()) {
      CompoundTag entry = new CompoundTag();
      entry.putUUID("player", sentence.player());
      entry.putString("village", sentence.village());
      entry.putString("castle", sentence.castle());
      entry.putString("dimension", sentence.dimension());
      entry.putLong("cell", sentence.cell().asLong());
      entry.putLong("release", sentence.release().asLong());
      entry.putLong("started", sentence.startedAt());
      entry.putLong("until", sentence.releaseAt());
      entry.putLong("grace", sentence.graceUntil());
      entry.putInt("announced_minutes", sentence.announcedMinutes());
      if (sentence.previousFatigue() != null) entry.put("previous_fatigue", sentence.previousFatigue().copy());
      saved.add(entry);
    }
    tag.put("sentences", saved);
    return tag;
  }
}
