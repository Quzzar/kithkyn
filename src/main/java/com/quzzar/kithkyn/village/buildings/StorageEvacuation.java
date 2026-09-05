package com.quzzar.kithkyn.village.buildings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.quzzar.kithkyn.village.Village;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Evacuates all affected containers together, including personal and workplace inventories. */
public final class StorageEvacuation {
  private StorageEvacuation() {
  }

  /** Reads actual container blocks, rather than assuming only declared shared chests hold items. */
  public static Set<BlockPos> sources(Village village, Collection<Building> buildings) {
    Set<BlockPos> sources = new LinkedHashSet<>();
    for (Building building : buildings) {
      BoundingBox local = BuildingUpgrade.footprintOf(village.getLevel(), building);
      if (local == null) {
        continue;
      }
      BlockPos origin = BlockPos.of(building.getOriginLocation());
      for (BlockPos offset : BlockPos.betweenClosed(local.minX(), local.minY(), local.minZ(),
          local.maxX(), local.maxY(), local.maxZ())) {
        BlockPos at = origin.offset(offset);
        if (village.getLevel().getLevel().hasChunkAt(at)
            && village.getLevel().getBlockEntity(at) instanceof Container) {
          sources.add(at.immutable());
        }
      }
    }
    return sources;
  }

  /** Ordinary upgrades still wait for physical capacity; redevelopment can retain overflow. */
  public static boolean evacuate(Village village, Collection<Building> buildings) {
    return evacuate(village, buildings, null);
  }

  /** Overflow is durably retained before its source slot is cleared. Loaded source footprints remain required. */
  public static boolean evacuate(Village village, Collection<Building> buildings, @Nullable Consumer<ItemStack> overflow) {
    if (village.getLevel() == null || !allLoaded(village, buildings)) {
      return false;
    }
    Set<BlockPos> sources = sources(village, buildings);
    return transfer(containers(village, sources), destinations(village, sources), overflow);
  }

  private static boolean allLoaded(Village village, Collection<Building> buildings) {
    for (Building building : buildings) {
      BoundingBox bounds = RedevelopmentPlanner.worldBounds(village, building);
      if (bounds == null) {
        return false;
      }
      for (int x = bounds.minX() >> 4; x <= bounds.maxX() >> 4; x++) {
        for (int z = bounds.minZ() >> 4; z <= bounds.maxZ() >> 4; z++) {
          if (!village.getLevel().getLevel().hasChunk(x, z)) {
            return false;
          }
        }
      }
    }
    return true;
  }

  private static List<Container> destinations(Village village, Set<BlockPos> sources) {
    Set<BlockPos> positions = new LinkedHashSet<>(village.getBrain().containerPositions());
    positions.removeAll(sources);
    return containers(village, positions);
  }

  private static List<Container> containers(Village village, Collection<BlockPos> positions) {
    List<Container> containers = new ArrayList<>();
    for (BlockPos at : positions) {
      if (village.getLevel().getLevel().hasChunkAt(at)
          && village.getLevel().getBlockEntity(at) instanceof Container container) {
        containers.add(container);
      }
    }
    return containers;
  }

  static boolean transfer(List<Container> sources, List<Container> destinations) {
    return transfer(sources, destinations, null);
  }

  /** Every item either reaches a destination, remains in its source, or enters the supplied overflow store. */
  static boolean transfer(List<Container> sources, List<Container> destinations, @Nullable Consumer<ItemStack> overflow) {
    for (Container source : sources) {
      for (int slot = 0; slot < source.getContainerSize(); slot++) {
        ItemStack remaining = source.getItem(slot).copy();
        if (remaining.isEmpty()) {
          continue;
        }
        for (Container destination : destinations) {
          remaining = HopperBlockEntity.addItem(null, destination, remaining, null);
          if (remaining.isEmpty()) {
            break;
          }
        }
        source.setItem(slot, remaining);
        source.setChanged();
        if (!remaining.isEmpty()) {
          if (overflow == null) {
            return false;
          }
          overflow.accept(remaining.copy());
          source.setItem(slot, ItemStack.EMPTY);
          source.setChanged();
        }
      }
    }
    return true;
  }
}
