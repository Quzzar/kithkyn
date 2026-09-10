package com.quzzar.kithkyn.village.buildings;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.village.FarmedStock;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

/** Initial template inhabitants, shared by instant and incremental construction. */
public final class BuildingEntities {

  /** Completed entries are never replenished after death, removal, reload, or an upgrade. */
  public record State(boolean complete, List<Integer> spawned) {
    public static final State PENDING = new State(false, List.of());
    public static final State COMPLETE = new State(true, List.of());
    public static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.fieldOf("complete").forGetter(State::complete),
        Codec.INT.listOf().optionalFieldOf("spawned", List.of()).forGetter(State::spawned)
    ).apply(instance, State::new));

    public State {
      spawned = List.copyOf(spawned);
    }

    public State withSpawned(int index) {
      if (spawned.contains(index)) {
        return this;
      }
      List<Integer> updated = new ArrayList<>(spawned);
      updated.add(index);
      return new State(complete, updated);
    }
  }

  private BuildingEntities() {
  }

  /**
   * Places each initial entry once. Failed entries remain pending for an ordinary
   * construction retry; completed entries retain their receipt on the building.
   * Entity and village files are not one transaction, so this is not a promise of
   * atomic recovery from a crash during Minecraft's multi-file save.
   */
  public static boolean placeOnce(ServerLevelAccessor access, Building building,
      StructureTemplate template, BlockPos origin, StructurePlaceSettings settings) {
    State state = building.getInitialEntities();
    if (state.complete() || settings.isIgnoreEntities()) {
      return true;
    }
    ServerLevel level = access.getLevel();
    List<StructureTemplate.StructureEntityInfo> entries =
        StructureTemplate.processEntityInfos(template, access, origin, settings, template.entityInfoList);
    boolean success = true;
    for (int index = 0; index < entries.size(); index++) {
      if (state.spawned().contains(index)) {
        continue;
      }
      StructureTemplate.StructureEntityInfo info = entries.get(index);
      if (settings.getBoundingBox() != null && !settings.getBoundingBox().isInside(info.blockPos)) {
        continue;
      }
      UUID id = entityId(building.getUUID(), index);
      if (level.getEntity(id) == null && !place(level, access, building, info, settings, id)) {
        success = false;
        continue;
      }
      state = state.withSpawned(index);
      building.setInitialEntities(state);
    }
    if (success) {
      building.setInitialEntities(new State(true, state.spawned()));
    }
    return success;
  }

  /** Stable only within one building's initial population, not shared across village instances. */
  static UUID entityId(UUID building, int entry) {
    return UUID.nameUUIDFromBytes(("kithkyn:building/" + building + "/entity/" + entry)
        .getBytes(StandardCharsets.UTF_8));
  }

  /** Copies runtime placement coordinates into NBT before block-attached entity validation runs. */
  static CompoundTag positionedTag(StructureTemplate.StructureEntityInfo info, UUID id) {
    CompoundTag tag = info.nbt.copy();
    Vec3 position = info.pos;
    ListTag coordinates = new ListTag();
    coordinates.add(DoubleTag.valueOf(position.x));
    coordinates.add(DoubleTag.valueOf(position.y));
    coordinates.add(DoubleTag.valueOf(position.z));
    tag.put("Pos", coordinates);
    tag.putUUID("UUID", id);
    if (tag.contains("TileX") || tag.contains("TileY") || tag.contains("TileZ")) {
      tag.putInt("TileX", info.blockPos.getX());
      tag.putInt("TileY", info.blockPos.getY());
      tag.putInt("TileZ", info.blockPos.getZ());
    }
    return tag;
  }

  private static boolean place(ServerLevel level, ServerLevelAccessor access, Building building,
      StructureTemplate.StructureEntityInfo info, StructurePlaceSettings settings, UUID id) {
    try {
      Entity entity = EntityType.create(positionedTag(info, id), level).orElse(null);
      if (entity == null) {
        Kithkyn.LOGGER.error("Could not create initial entity {} for {}", info.nbt.getString("id"), building.getName());
        return false;
      }
      float yaw = entity.rotate(settings.getRotation());
      yaw += entity.mirror(settings.getMirror()) - entity.getYRot();
      // A frame's rendered position is offset from its attachment cell. Use the
      // cell's center here, then its own setPos recalculates the visual bounds.
      Vec3 destination = entity instanceof BlockAttachedEntity
          ? Vec3.atCenterOf(info.blockPos) : info.pos;
      entity.moveTo(destination.x, destination.y, destination.z, yaw, entity.getXRot());
      if (settings.shouldFinalizeEntities() && entity instanceof Mob mob) {
        mob.finalizeSpawn(access, access.getCurrentDifficultyAt(info.blockPos), MobSpawnType.STRUCTURE, null);
      }
      if (entity instanceof Animal animal && FarmedStock.isStock(animal)) {
        FarmedStock.mark(animal);
      }
      if (entity instanceof Mob mob) {
        mob.setPersistenceRequired();
      }
      if (entity instanceof net.minecraft.world.entity.animal.allay.Allay) {
        // An authored storehouse allay joins the village as soon as it ticks, guard or no guard.
        entity.getPersistentData().putUUID(com.quzzar.kithkyn.village.VillageAllays.SPAWNED_BY_BUILDING_KEY, building.getUUID());
      }
      if (entity instanceof BlockAttachedEntity attached && !attached.survives()) {
        Kithkyn.LOGGER.error("Initial decoration in {} has no valid support at {}",
            building.getName(), info.blockPos);
        return false;
      }
      boolean added = level.addFreshEntity(entity);
      if (!added) {
        Kithkyn.LOGGER.error("Initial entity {} for {} could not join the world", id, building.getName());
      }
      return added;
    } catch (RuntimeException exception) {
      Kithkyn.LOGGER.error("Initial entity for {} failed at {}", building.getName(), info.blockPos, exception);
      return false;
    }
  }
}
