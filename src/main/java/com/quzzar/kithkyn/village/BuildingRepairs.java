package com.quzzar.kithkyn.village;

import java.util.List;
import javax.annotation.Nullable;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.savedata.PlacedBlockStore;
import com.quzzar.kithkyn.savedata.RepairStore;
import com.quzzar.kithkyn.savedata.RepairStore.Repair;
import com.quzzar.kithkyn.village.buildings.Building;
import com.quzzar.kithkyn.village.buildings.MineShaft;
import com.quzzar.kithkyn.village.buildings.RedevelopmentPlanner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/** Conservative damage evidence and physical replacement rules shared by surveying and the repair worker. */
public final class BuildingRepairs {
  private BuildingRepairs() { }

  public static String revision(Building building) {
    return building.getName() + ":" + building.getOriginLocation() + ":" + building.getRotation();
  }

  /** Only simple structural blocks with an exact purchasable block item have a faithful repair operation. */
  public static ItemStack cost(ServerLevel level, BlockPos pos, BlockState state) {
    if (state.isAir() || state.hasBlockEntity() || !state.getFluidState().isEmpty()
        || BlockOwnership.isPlanted(state) || state.is(BlockTags.LEAVES)
        || !(state.getBlock().asItem() instanceof BlockItem item) || item.getBlock() != state.getBlock()) {
      return ItemStack.EMPTY;
    }
    boolean ordinary = state.isCollisionShapeFullBlock(level, pos)
        || state.getBlock() instanceof StairBlock || state.getBlock() instanceof SlabBlock
        || state.getBlock() instanceof FenceBlock || state.getBlock() instanceof WallBlock
        || state.getBlock() instanceof IronBarsBlock;
    if (!ordinary) return ItemStack.EMPTY;
    int count = state.getBlock() instanceof SlabBlock && state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE ? 2 : 1;
    return new ItemStack(item, count);
  }

  /** Turf and worn paths can be restored with real dirt, which grass and path maintenance can finish later. */
  public static BlockState groundState(BlockState before) {
    return before.is(Blocks.GRASS_BLOCK) || before.is(Blocks.DIRT_PATH) || before.is(Blocks.FARMLAND)
        ? Blocks.DIRT.defaultBlockState() : before;
  }

  /** The mine owns its planned excavation, including the headroom where a child shaft joins its parent. */
  public static boolean inExcavation(Village village, BlockPos pos) {
    for (Building building : village.getBuildings()) {
      for (MineShaft shaft : MineShaft.of(building)) {
        Rotation inverse = switch (shaft.rotation()) {
          case CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90;
          case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
          default -> shaft.rotation();
        };
        if (MineShaft.withinExcavation(pos.subtract(shaft.mouth()).rotate(inverse))) return true;
      }
    }
    return false;
  }

  /** Structural repairs require their original owner; natural earth is supported by the explosion snapshot. */
  public static boolean stillWanted(Village village, Repair repair) {
    ServerLevel level = village.getLevel();
    if (!level.hasChunkAt(repair.pos())) return false;
    Building building = village.getBuilding(repair.building());
    PlacedBlockStore placed = PlacedBlockStore.get(level);
    return building != null && repair.revision().equals(revision(building))
        && level.getBlockState(repair.pos()).isAir() && !placed.isPlayerPlaced(repair.pos())
        && (repair.ground() || placed.isVillagePlaced(repair.pos())) && !inExcavation(village, repair.pos());
  }

  /** Never build a floating fill layer or unsupported falling block. Other structure blocks may join a side. */
  public static boolean supported(ServerLevel level, Repair repair) {
    BlockPos pos = repair.pos();
    if (!repair.state().canSurvive(level, pos)) return false;
    for (Direction direction : Direction.values()) {
      if ((repair.ground() || repair.state().getBlock() instanceof FallingBlock) && direction != Direction.DOWN) continue;
      BlockPos neighbour = pos.relative(direction);
      if (level.hasChunkAt(neighbour)
          && level.getBlockState(neighbour).isFaceSturdy(level, neighbour, direction.getOpposite())) return true;
    }
    return false;
  }

  /** Authored alternatives have no persisted palette identity, so old damage in those templates stays untouched. */
  public static List<StructureTemplate.StructureBlockInfo> shell(ServerLevel level, Building building) {
    if (building.getInfo() == null) return List.of();
    StructureTemplate template = level.getStructureManager().get(ResourceLocation.fromNamespaceAndPath(
        Kithkyn.MODID, building.getInfo().getPath())).orElse(null);
    return template == null || template.palettes.size() != 1 ? List.of() : template.palettes.getFirst().blocks();
  }

  /** The nearest building supplies both a local scope and an identity that removal can invalidate. */
  @Nullable
  public static Building nearbyBuilding(Village village, BlockPos pos, int apron, int below) {
    Building nearest = null;
    double nearestDistance = Double.MAX_VALUE;
    for (Building building : village.getBuildings()) {
      BoundingBox bounds = RedevelopmentPlanner.worldBounds(village, building);
      if (bounds == null || pos.getX() < bounds.minX() - apron || pos.getX() > bounds.maxX() + apron
          || pos.getZ() < bounds.minZ() - apron || pos.getZ() > bounds.maxZ() + apron
          || pos.getY() < bounds.minY() - below || pos.getY() > bounds.maxY() + 1) continue;
      double distance = pos.distSqr(BlockPos.of(building.getCenterLocation()));
      if (distance < nearestDistance) {
        nearestDistance = distance;
        nearest = building;
      }
    }
    return nearest;
  }

  /** Capture only ordinary nearby earth or village-owned structure, never a player's construction. */
  public static void observeExplosion(ServerLevel level, List<BlockPos> affected) {
    PlacedBlockStore placed = PlacedBlockStore.get(level);
    RepairStore repairs = RepairStore.get(level);
    List<BlockPos> paths = affected.stream()
        .filter(pos -> level.hasChunkAt(pos) && level.getBlockState(pos).is(Blocks.DIRT_PATH)).toList();
    for (Village village : VillageManager.get(level).getVillages().values()) {
      if (village.getLevel() != level) continue;
      for (BlockPos pos : affected) {
        if (!level.hasChunkAt(pos) || placed.isPlayerPlaced(pos) || inExcavation(village, pos)) continue;
        BlockState before = level.getBlockState(pos);
        boolean structural = placed.isVillagePlaced(pos);
        boolean earth = before.is(GradingSurvey.GRADEABLE) || before.is(Blocks.DIRT_PATH);
        if (!structural && !earth) continue;
        Building building = nearbyBuilding(village, pos, 4, 5);
        if (building == null && earth) {
          // A destroyed path is direct evidence of a local surface; limit it to the settlement's buildings.
          for (BlockPos path : paths) {
            if (Math.abs(path.getX() - pos.getX()) <= 3 && Math.abs(path.getZ() - pos.getZ()) <= 3
                && pos.getY() <= path.getY() && pos.getY() >= path.getY() - 5) {
              building = nearbyBuilding(village, path, 24, 5);
              if (building != null) break;
            }
          }
        }
        if (building == null) continue;
        BlockState desired = structural ? before : groundState(before);
        if (cost(level, pos, desired).isEmpty()) continue;
        repairs.observeExplosion(new Repair(village.getID(), building.getUUID(), revision(building),
            pos.immutable(), desired, !structural));
      }
    }
  }
}
