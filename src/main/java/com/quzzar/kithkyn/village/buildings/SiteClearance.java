package com.quzzar.kithkyn.village.buildings;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.savedata.PlacedBlockStore;
import com.quzzar.kithkyn.village.BlockOwnership;
import com.quzzar.kithkyn.village.TreeFelling;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.VillageManager;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Natural-tree and foliage clearance shared by completed buildings and wall routes. */
public final class SiteClearance {
  public static final int TREE_RADIUS = 3;
  public static final int FOLIAGE_RADIUS = 1;

  private SiteClearance() { }

  /** Whole natural trees within the wall/building tree line, with ordinary ownership guards. */
  public static List<TreeFelling.FelledTree> fellTrees(ServerLevel level, Set<Long> footprint) {
    return TreeFelling.fellWithin(level, horizontalReach(footprint, TREE_RADIUS));
  }

  /** Inclusive horizontal footprint; this does not reserve land or modify the ground. */
  public static Set<Long> columns(BoundingBox bounds) {
    Set<Long> result = new HashSet<>();
    for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
      for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) result.add(BlockPos.asLong(x, 0, z));
    }
    return Set.copyOf(result);
  }

  /** Square horizontal radius, retaining irregular wall shapes rather than filling their interior. */
  public static Set<Long> horizontalReach(Set<Long> footprint, int radius) {
    if (radius < 0) throw new IllegalArgumentException("Horizontal radius cannot be negative");
    Set<Long> result = new HashSet<>();
    for (long column : footprint) {
      int x = BlockPos.getX(column), z = BlockPos.getZ(column);
      for (int dx = -radius; dx <= radius; dx++) {
        for (int dz = -radius; dz <= radius; dz++) result.add(BlockPos.asLong(x + dx, 0, z + dz));
      }
    }
    return Set.copyOf(result);
  }

  /**
   * Clears the roof and one-block verge, stopping at terrain, construction or authored planting.
   * Unloaded columns are skipped. This never cuts or fills soil and never clears inventories.
   */
  public static int clearFoliage(ServerLevel level, Set<Long> footprint, Collection<Building> localBuildings) {
    Set<Long> clearance = horizontalReach(footprint, FOLIAGE_RADIUS);
    Set<Long> plants = authoredPlants(level, clearance, localBuildings);
    int cleared = 0;
    BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    for (long column : clearance) {
      int x = BlockPos.getX(column), z = BlockPos.getZ(column);
      if (!level.hasChunkAt(cursor.set(x, level.getMinBuildHeight(), z))) continue;
      int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
      for (int y = top - 1; y >= level.getMinBuildHeight(); y--) {
        cursor.set(x, y, z);
        BlockState state = level.getBlockState(cursor);
        if (state.isAir() || !state.getFluidState().isEmpty()) continue;
        if (plants.contains(cursor.asLong()) || !isNaturalClearable(level, cursor, state)) break;
        level.removeBlock(cursor, false);
        cleared++;
      }
    }
    return cleared;
  }

  /** Shared vegetation verdict; owned blocks and block entities are never vegetation to erase. */
  public static boolean isNaturalClearable(Level level, BlockPos pos, BlockState state) {
    if (!state.is(SitePreparation.CLEARABLE) || state.hasBlockEntity()) return false;
    if (!(level instanceof ServerLevel serverLevel)) return true;
    PlacedBlockStore placed = PlacedBlockStore.get(serverLevel);
    return !placed.isPlayerPlaced(pos) && !placed.isVillagePlaced(pos);
  }

  /**
   * Saplings/crops/flowers must remain unowned so grown trees can later be harvested.
   * Protect their template positions for this one sweep instead, including neighboring buildings.
   * Local buildings include the just-founded village before it enters the global registry.
   */
  private static Set<Long> authoredPlants(ServerLevel level, Set<Long> clearance,
      Collection<Building> localBuildings) {
    Set<Building> buildings = new HashSet<>(localBuildings);
    for (Village village : VillageManager.get(level).getVillages().values()) buildings.addAll(village.getBuildings());
    Set<Long> result = new HashSet<>();
    for (Building building : buildings) {
      if (building.getInfo() == null) continue;
      var template = level.getStructureManager().getOrCreate(
          ResourceLocation.fromNamespaceAndPath(Kithkyn.MODID, building.getInfo().getPath()));
      BlockPos origin = BlockPos.of(building.getOriginLocation());
      for (var palette : template.palettes) {
        for (var info : palette.blocks()) {
          if (!BlockOwnership.isPlanted(info.state())) continue;
          BlockPos pos = origin.offset(info.pos().rotate(building.getRotation()));
          if (clearance.contains(BlockPos.asLong(pos.getX(), 0, pos.getZ()))) result.add(pos.asLong());
        }
      }
    }
    return result;
  }
}
