package com.quzzar.kithkyn.village;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.chat.VillagerConversation;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.entities.ai.behavior.AllayQuartermasterBehavior;
import com.quzzar.kithkyn.village.buildings.Building;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * Adopted allays as free keepers of the stores (docs/allay-quartermasters.md): the
 * recruitment, the persistent tie between a vanilla allay and its village, and the
 * minding loop it runs. An allay has no goal selector, so the loop is a brain
 * behaviour added beside vanilla's, re-added whenever the entity loads. Nothing
 * here touches human quartermaster posts, beds or rations.
 */
public final class VillageAllays {
  public static final String VILLAGE_KEY = "kithkyn:storeVillage";
  public static final String RECRUITER_KEY = "kithkyn:storeRecruiter";
  /** Written on an allay a storehouse spawned, so it joins that building's village without a guard. */
  public static final String SPAWNED_BY_BUILDING_KEY = "kithkyn:storehouseBuilding";
  public static final double ADOPTION_RADIUS = VillageGolems.ADOPTION_RADIUS;
  /** A storehouse's counter is authored next to its containers; look no further than this. */
  private static final int NOTE_BLOCK_SEARCH = 4;
  private static final List<String> NAMES = List.of(
      "Wisp", "Fen", "Reed", "Sedge", "Mote", "Lumen", "Dewdrop", "Rush", "Tallow", "Pip");
  /** Entities whose live brain already carries the keeper's loop; a reload builds a fresh brain and a fresh entity. */
  private static final Set<Allay> KEEPING = Collections.newSetFromMap(new WeakHashMap<>());

  private VillageAllays() {
  }

  /** A bounded encounter check, called every five seconds per active guard, staggered from the golem scan. */
  public static void considerAdoption(RealPerson guard) {
    if (!(guard.level() instanceof ServerLevel level) || !VillageGolems.eligibleGuard(guard)) {
      return;
    }
    level.getEntitiesOfClass(Allay.class, guard.getBoundingBox().inflate(ADOPTION_RADIUS),
        allay -> canAdopt(guard, allay)).stream()
        .min(Comparator.comparingDouble(guard::distanceToSqr))
        .ifPresent(allay -> adopt(guard, allay));
  }

  /** Leashed, travelling, already claimed and unreachable allays are left alone. */
  public static boolean canAdopt(RealPerson guard, Allay allay) {
    return VillageGolems.eligibleGuard(guard) && allay.level() == guard.level()
        && eligibleAllay(allay) && villageId(allay).isEmpty()
        && guard.distanceToSqr(allay) <= ADOPTION_RADIUS * ADOPTION_RADIUS
        && guard.hasLineOfSight(allay);
  }

  private static boolean eligibleAllay(Allay allay) {
    return allay.isAlive() && !allay.isRemoved() && !allay.isNoAi()
        && !allay.isLeashed() && !allay.isPassenger() && !allay.isVehicle();
  }

  /** Commit the claim synchronously, so two guards cannot adopt the same allay. */
  public static boolean adopt(RealPerson guard, Allay allay) {
    if (!(guard.level() instanceof ServerLevel level) || !canAdopt(guard, allay)) {
      return false;
    }
    Village village = guard.getVillage();
    join(level, village, allay, guard);
    guard.logMemory("I welcomed " + allay.getName().getString() + " the allay to keep our stores.",
        Optional.of(allay.getUUID()));
    VillagerConversation.speak(guard, "Welcome to the stores, " + allay.getName().getString() + ".");
    Kithkyn.LOGGER.info("[village allay] {} welcomes {} into {}", guard.getFullName(),
        allay.getName().getString(), village.getName());
    return true;
  }

  /** An authored storehouse allay joins its building's village on its first tick, before any guard passes. */
  public static boolean adoptSpawned(ServerLevel level, Allay allay) {
    if (!allay.getPersistentData().hasUUID(SPAWNED_BY_BUILDING_KEY)) {
      return false;
    }
    UUID buildingId = allay.getPersistentData().getUUID(SPAWNED_BY_BUILDING_KEY);
    allay.getPersistentData().remove(SPAWNED_BY_BUILDING_KEY);
    if (villageId(allay).isPresent() || !eligibleAllay(allay)) {
      return false;
    }
    for (Village village : VillageManager.get(level).getVillages().values()) {
      Building building = village.getBuilding(buildingId);
      if (building != null) {
        join(level, village, allay, null);
        Kithkyn.LOGGER.info("[village allay] {} keeps the stores of {} from {}", allay.getName().getString(),
            village.getName(), building.getName());
        return true;
      }
    }
    return false;
  }

  private static void join(ServerLevel level, Village village, Allay allay, @Nullable RealPerson recruiter) {
    allay.getPersistentData().putString(VILLAGE_KEY, village.getID());
    if (recruiter != null) {
      allay.getPersistentData().putUUID(RECRUITER_KEY, recruiter.getUUID());
    }
    allay.setPersistenceRequired();
    if (!allay.hasCustomName()) {
      allay.setCustomName(Component.literal(NAMES.get(allay.getRandom().nextInt(NAMES.size()))));
    }
    attach(allay);
    remember(level, village, allay);
  }

  /**
   * Adoption and every reload restore the crosshair-only nameplate and the keeper's
   * loop. The loop is added once per live entity, beside vanilla's idle behaviours;
   * a reloaded entity is a new object with a new brain and gets it again.
   */
  public static void attach(Allay allay) {
    if (villageId(allay).isEmpty()) {
      return;
    }
    allay.setCustomNameVisible(false);
    if (allay.getNavigation() instanceof FlyingPathNavigation flying) {
      flying.setCanOpenDoors(true); // paths may route through doors; the keeper opens them itself
    }
    if (KEEPING.add(allay)) {
      allay.getBrain().addActivity(Activity.IDLE, 0, ImmutableList.of(new AllayQuartermasterBehavior()));
    }
  }

  /** A backlink is enough to resume the duty when an entity loads. */
  public static Optional<String> villageId(Entity entity) {
    String id = entity.getPersistentData().getString(VILLAGE_KEY);
    return id.isBlank() ? Optional.empty() : Optional.of(id);
  }

  public static boolean supports(Entity entity) {
    return entity instanceof Allay;
  }

  @Nullable
  public static Village village(Allay allay) {
    if (!(allay.level() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
      return null;
    }
    return villageId(allay).map(id -> VillageManager.get(level).getVillage(id)).orElse(null);
  }

  /** Refresh last-known location without counting an unloaded entity as dead. */
  public static void tickMember(Allay allay) {
    Village village = village(allay);
    if (village != null && allay.isAlive() && allay.level() instanceof ServerLevel level) {
      attach(allay);
      remember(level, village, allay);
    }
  }

  /** Every declared container of every storage building, in building and container order. */
  public static List<BlockPos> shelfPositions(Village village) {
    List<BlockPos> out = new ArrayList<>();
    for (Building building : Storehouse.buildings(village)) {
      out.addAll(Storehouse.chests(building));
    }
    return out;
  }

  /**
   * Where a keeper waits between rounds: the note block authored beside the
   * storehouse's containers, or the first shelf when there is none. Null when the
   * village has no storage building yet.
   */
  @Nullable
  public static BlockPos home(ServerLevel level, Village village) {
    for (Building building : Storehouse.buildings(village)) {
      List<BlockPos> chests = Storehouse.chests(building);
      if (chests.isEmpty()) {
        continue;
      }
      BlockPos min = chests.getFirst();
      BlockPos max = chests.getFirst();
      for (BlockPos chest : chests) {
        min = new BlockPos(Math.min(min.getX(), chest.getX()), Math.min(min.getY(), chest.getY()), Math.min(min.getZ(), chest.getZ()));
        max = new BlockPos(Math.max(max.getX(), chest.getX()), Math.max(max.getY(), chest.getY()), Math.max(max.getZ(), chest.getZ()));
      }
      for (BlockPos pos : BlockPos.betweenClosed(min.offset(-NOTE_BLOCK_SEARCH, -1, -NOTE_BLOCK_SEARCH),
          max.offset(NOTE_BLOCK_SEARCH, NOTE_BLOCK_SEARCH, NOTE_BLOCK_SEARCH))) {
        if (level.isLoaded(pos) && level.getBlockState(pos).is(Blocks.NOTE_BLOCK)) {
          return pos.immutable();
        }
      }
      return chests.getFirst();
    }
    return null;
  }

  public static void remember(ServerLevel level, Village village, Allay allay) {
    village.getAllays().remember(allay.getUUID(), allay.getName().getString(),
        ChunkPos.asLong(allay.chunkPosition().x, allay.chunkPosition().z));
    VillageManager.get(level).setDirty();
  }

  /** Death is explicit; merely unloading is not removal. */
  public static void died(Allay allay) {
    if (!(allay.level() instanceof ServerLevel level)) {
      return;
    }
    KEEPING.remove(allay);
    villageId(allay).map(id -> VillageManager.get(level).getVillage(id)).ifPresent(village -> {
      village.getAllays().remove(allay.getUUID());
      VillageManager.get(level).setDirty();
    });
  }
}
