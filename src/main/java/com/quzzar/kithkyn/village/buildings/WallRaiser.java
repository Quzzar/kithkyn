package com.quzzar.kithkyn.village.buildings;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.quzzar.kithkyn.savedata.PlacedBlockStore;
import com.quzzar.kithkyn.village.TreeFelling;
import com.quzzar.kithkyn.village.VillageIdentity;
import net.minecraft.world.level.block.AbstractBannerBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Executes compiled wall cells against the live world.
 *
 * The catalog owns appearance, {@link WallProject} owns progress and claims,
 * and this class owns the world-sensitive questions: terrain, occupancy,
 * placement and the exact material bill.
 */
public final class WallRaiser {

  private WallRaiser() {
  }

  /**
   * The y a block sits at to rest on the real ground of this column. Trees,
   * brush and placed structures are obstacles over the terrain, not terrain:
   * reading the top of a trunk as ground made a wall target the top of a tree.
   */
  public static int surfaceY(Level level, int x, int z) {
    int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    PlacedBlockStore placed = level instanceof ServerLevel serverLevel
        ? PlacedBlockStore.get(serverLevel)
        : null;
    return WallTerrain.surfaceY(top, level.getMinBuildHeight(), y -> {
      cursor.set(x, y, z);
      BlockState state = level.getBlockState(cursor);
      return !state.is(SitePreparation.CLEARABLE)
          && (placed == null || (!placed.isPlayerPlaced(cursor) && !placed.isVillagePlaced(cursor)))
          && state.isFaceSturdy(level, cursor, Direction.UP);
    });
  }

  /**
   * Each ring column's natural ground height, read once before any wall is placed.
   * Seam-closing drops a column to a lower neighbour's ground, so it has to know
   * that neighbour's ORIGINAL surface: read live during the build, a neighbour
   * already raised reads its own wall back as the surface and the profile creeps.
   * Captured up front and kept on the {@link WallProject}, it stays the true ground.
   */
  public static List<Integer> groundProfile(Level level, List<Long> ring) {
    List<Integer> ground = new ArrayList<>(ring.size());
    for (long column : ring) {
      ground.add(surfaceY(level, BlockPos.getX(column), BlockPos.getZ(column)));
    }
    return ground;
  }

  /**
   * Plans a deck against land or the open waterline, whichever is higher. The
   * separate natural-ground profile still lets the wall seal down to the seabed.
   */
  public static List<Integer> deckProfile(Level level, List<Long> ring,
      List<Integer> ground, int wallHeight) {
    if (ring.size() != ground.size()) {
      throw new IllegalArgumentException(
          "Wall ring and ground profile must have the same length");
    }
    List<Integer> defensiveSurface = new ArrayList<>(ring.size());
    for (int index = 0; index < ring.size(); index++) {
      long column = ring.get(index);
      int naturalGround = ground.get(index);
      defensiveSurface.add(WallTerrain.defensiveSurfaceY(
          naturalGround, BlockPos.getX(column), BlockPos.getZ(column),
          (x, z) -> waterSurfaceY(level, x, z, naturalGround)));
    }
    return WallTerraces.deckProfile(ground, defensiveSurface, wallHeight);
  }

  /** First air block above open water, or the natural ground on a dry column. */
  private static int waterSurfaceY(Level level, int x, int z, int naturalGround) {
    int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    PlacedBlockStore placed = level instanceof ServerLevel serverLevel
        ? PlacedBlockStore.get(serverLevel)
        : null;
    for (int y = top - 1; y >= naturalGround; y--) {
      cursor.set(x, y, z);
      BlockState state = level.getBlockState(cursor);
      if (!state.getFluidState().isEmpty()) {
        return y + 1;
      }
      boolean owned = placed != null
          && (placed.isPlayerPlaced(cursor) || placed.isVillagePlaced(cursor));
      if (state.isAir() || state.is(SitePreparation.CLEARABLE) || owned) {
        continue;
      }
      break;
    }
    return naturalGround;
  }

  /**
   * The lowest ground of a ring column and its two neighbours (the ring is a closed
   * loop). A segment drops its foot to this floor so it overlaps a lower neighbour
   * and leaves no vertical seam where the ground steps down (docs/walls.md, "No
   * gaps"): the older run only closed a void it hung over, never a solid step.
   */
  public static int seamFloor(List<Integer> ground, int index) {
    int n = ground.size();
    int prev = ground.get((index - 1 + n) % n);
    int next = ground.get((index + 1) % n);
    return Math.min(ground.get(index), Math.min(prev, next));
  }

  /** Exact number of wall-material blocks the current world still needs. */
  public static int requiredBlocks(Level level, List<Long> ring, Set<Long> gates,
      List<Integer> ground, WallTier tier) {
    return requiredBlocks(level, ring, gates, ground, tier, VillageStyle.PLAINS);
  }

  /** Exact material count using the village's saved regional wall palette. */
  public static int requiredBlocks(Level level, List<Long> ring, Set<Long> gates,
      List<Integer> ground, WallTier tier, VillageStyle style) {
    return requiredBlocks(level, ring, gates, ground, tier, style, Set.of());
  }

  public static int requiredBlocks(Level level, List<Long> ring, Set<Long> gates,
      List<Integer> ground, WallTier tier, VillageStyle style,
      Set<Long> towerExclusions) {
    List<Integer> deck = deckProfile(level, ring, ground, tier.height());
    WallProject plan = new WallProject(
        ring, gates, ground, deck, tier, style, towerExclusions);
    return requiredBlocks(level, plan);
  }

  /** Exact material count for one already compiled candidate project. */
  public static int requiredBlocks(Level level, WallProject plan) {
    int required = 0;
    for (int i = 0; i < plan.sectionCount(); i++) {
      for (WallBlockPlan block : plan.section(i).blocks()) {
        if (!isSatisfied(level, block, plan)) {
          required++;
        }
      }
    }
    return required;
  }

  /** The exact construction cell currently leased to a builder. */
  public record WallWork(int section, WallBlockPlan block) {
  }

  /**
   * Claims a nearby section and skips cells the world already satisfies. This
   * is the wall equivalent of a building project's next template block.
   */
  @Nullable
  public static WallWork nextWork(Level level, WallProject wall, UUID builder, BlockPos from) {
    int attempts = Math.max(1, wall.sectionCount());
    for (int attempt = 0; attempt < attempts; attempt++) {
      int sectionIndex = wall.claimSection(builder, from, level.getGameTime());
      if (sectionIndex < 0) {
        return null;
      }
      WallSection section = wall.section(sectionIndex);
      while (!section.isComplete()
          && isSatisfied(level, section.next(), wall)) {
        wall.advance(builder, sectionIndex);
      }
      if (!section.isComplete()) {
        return new WallWork(sectionIndex, section.next());
      }
    }
    return null;
  }

  /** Whether this is still the cell a builder owns after travelling to it. */
  public static boolean isCurrent(WallProject wall, UUID builder, WallWork work) {
    return wall.owns(builder, work.section(), work.block());
  }

  /**
   * Every occupied wall cell cares first that collision closes it. Exact cells
   * get their authored state when the cell is open, but any existing solid is
   * preserved. Natural vegetation is cleared so it cannot count as a permanent
   * barrier.
   */
  public static boolean isSatisfied(Level level, WallBlockPlan block, WallTier tier) {
    return isSatisfied(level, block, tier, VillageStyle.PLAINS);
  }

  /** Whether a cell is satisfied under the village's regional wall palette. */
  public static boolean isSatisfied(Level level, WallBlockPlan block, WallTier tier,
      VillageStyle style) {
    return isSatisfied(level, block, tier, style, null);
  }

  /** Matching includes flag layers, not just the banner's colored block state. */
  public static boolean isSatisfied(Level level, WallBlockPlan block, WallProject wall) {
    return isSatisfied(level, block, wall.getTier(), wall.getStyle(), wall.getIdentity());
  }

  private static boolean isSatisfied(Level level, WallBlockPlan block, WallTier tier,
      VillageStyle style, @Nullable VillageIdentity identity) {
    BlockPos pos = block.pos();
    BlockState state = level.getBlockState(pos);
    BlockState desired = desiredState(block, tier, style, identity);
    if (block.isBanner() && level instanceof ServerLevel serverLevel) {
      if (PlacedBlockStore.get(serverLevel).isPlayerPlaced(pos)) return true;
      if (state.getBlock() instanceof AbstractBannerBlock) {
        return state.equals(desired) && (identity == null
            || VillageIdentityApplier.bannerMatches(serverLevel, pos, identity));
      }
    }
    if (state.equals(desired)) {
      return true;
    }
    PlacedBlockStore placed = level instanceof ServerLevel serverLevel
        ? PlacedBlockStore.get(serverLevel)
        : null;
    // A revised solid join may replace our own old battlement, never a player's edit.
    if (placed != null && placed.isVillagePlaced(pos) && !placed.isPlayerPlaced(pos)
        && isStructuralFoundationPiece(block.piece())
        && (state.getBlock() instanceof net.minecraft.world.level.block.WallBlock
            || state.getBlock() instanceof net.minecraft.world.level.block.FenceBlock
            || state.getBlock() instanceof net.minecraft.world.level.block.SlabBlock)) return false;
    boolean hasCollision = !state.getCollisionShape(level, pos).isEmpty();
    boolean isClearableVegetation = isNaturalClearable(level, pos, state);
    return WallOccupancy.isSatisfied(
        hasCollision, isClearableVegetation);
  }

  /** Places one planned cell. */
  public static void place(Level level, WallBlockPlan block, WallTier tier) {
    place(level, block, tier, VillageStyle.PLAINS);
  }

  /** Places one planned cell using the village's regional wall palette. */
  public static void place(Level level, WallBlockPlan block, WallTier tier,
      VillageStyle style) {
    place(level, block, tier, style, null);
  }

  /** Incremental and instant wall construction share the same identity-aware placement. */
  public static void place(Level level, WallBlockPlan block, WallProject wall) {
    place(level, block, wall.getTier(), wall.getStyle(), wall.getIdentity());
  }

  private static BlockState desiredState(WallBlockPlan block, WallTier tier,
      VillageStyle style, @Nullable VillageIdentity identity) {
    BlockState state = block.desiredState(tier, style);
    return block.isBanner() && identity != null
        ? VillageIdentityApplier.bannerState(state, identity.primaryColor()) : state;
  }

  private static void place(Level level, WallBlockPlan block, WallTier tier,
      VillageStyle style, @Nullable VillageIdentity identity) {
    BlockPos pos = block.pos();
    if (block.isBanner() && level instanceof ServerLevel serverLevel
        && PlacedBlockStore.get(serverLevel).isPlayerPlaced(pos)) return;
    BlockState state = desiredState(block, tier, style, identity);
    if (block.piece() == WallBlockPlan.Piece.POST
        || block.role() == WallCellRole.FOUNDATION) {
      extendFoundationToGround(level, pos, state);
    } else if (isStructuralFoundationPiece(block.piece())) {
      embedExposedSurface(level, pos, state);
    }
    // Placement notifications update neighbors, not the new block's own
    // connection arms when its supports were already there.
    level.setBlock(pos, Block.updateFromNeighbourShapes(state, level, pos), 3);
    markVillagePlaced(level, pos);
    if (block.isBanner() && identity != null && level instanceof ServerLevel serverLevel) {
      VillageIdentityApplier.applyBanner(serverLevel, pos, identity, "gatehouse");
    }
  }

  /**
   * Rigid authored features can project several blocks away from the sampled
   * route. Resolve their foundations against the exact live terrain column so
   * a downhill corner cannot leave a watchtower or its access hanging in air.
   */
  private static void extendFoundationToGround(Level level, BlockPos foundation,
      BlockState state) {
    int surface = surfaceY(level, foundation.getX(), foundation.getZ());
    boolean embedSurface = shouldEmbedSurface(
        level, foundation.getX(), foundation.getZ(), surface);
    for (BlockPos support : foundationPositions(foundation, surface, embedSurface)) {
      BlockState existing = level.getBlockState(support);
      if (!existing.getCollisionShape(level, support).isEmpty()
          && !isNaturalClearable(level, support, existing)
          && !(support.getY() == surface - 1 && embedSurface)) {
        continue;
      }
      level.setBlock(support, state, 3);
      markVillagePlaced(level, support);
    }
  }

  static List<BlockPos> foundationPositions(BlockPos post, int surfaceY) {
    return foundationPositions(post, surfaceY, false);
  }

  /** Foundation cells optionally replace the exposed soil course below the surface. */
  static List<BlockPos> foundationPositions(BlockPos post, int surfaceY,
      boolean embedSurface) {
    List<BlockPos> positions = new ArrayList<>();
    int firstY = embedSurface ? surfaceY - 1 : surfaceY;
    for (int y = firstY; y < post.getY(); y++) {
      positions.add(new BlockPos(post.getX(), y, post.getZ()));
    }
    return List.copyOf(positions);
  }

  /** Embeds the first ordinary body cell without letting later cells dig repeatedly. */
  private static void embedExposedSurface(Level level, BlockPos wallBlock,
      BlockState wallState) {
    int surface = surfaceY(level, wallBlock.getX(), wallBlock.getZ());
    if (wallBlock.getY() != surface
        || !shouldEmbedSurface(level, wallBlock.getX(), wallBlock.getZ(), surface)) {
      return;
    }
    BlockPos soil = wallBlock.below();
    level.setBlock(soil, wallState, 3);
    markVillagePlaced(level, soil);
  }

  private static boolean shouldEmbedSurface(Level level, int x, int z, int surfaceY) {
    return shouldEmbedSoil(level, new BlockPos(x, surfaceY - 1, z));
  }

  private static boolean shouldEmbedSoil(Level level, BlockPos soil) {
    BlockState soilState = level.getBlockState(soil);
    PlacedBlockStore placed = level instanceof ServerLevel serverLevel
        ? PlacedBlockStore.get(serverLevel)
        : null;
    boolean isOwned = placed != null
        && (placed.isPlayerPlaced(soil) || placed.isVillagePlaced(soil));
    boolean hasExposedSide = false;
    for (Direction direction : Direction.Plane.HORIZONTAL) {
      BlockPos neighbor = soil.relative(direction);
      BlockState neighborState = level.getBlockState(neighbor);
      if (neighborState.getFluidState().isEmpty()
          && neighborState.getCollisionShape(level, neighbor).isEmpty()) {
        hasExposedSide = true;
        break;
      }
    }
    return WallTerrain.shouldEmbedSurface(
        soilState.is(BlockTags.DIRT), isOwned, hasExposedSide);
  }

  /** Places a compiled project instantly for the dev preview command. */
  public static int placeAll(Level level, WallProject wall) {
    prepareWall(level, wall);
    int placed = 0;
    for (int i = 0; i < wall.sectionCount(); i++) {
      for (WallBlockPlan block : wall.section(i).blocks()) {
        if (!isSatisfied(level, block, wall)) {
          place(level, block, wall);
          placed++;
        }
      }
    }
    finishWall(level, wall);
    return placed;
  }

  /** Applies the terrain and vegetation cleanup shared by previews and builders. */
  public static void finishWall(Level level, WallProject wall) {
    fellTreesNearWall(level, wall);
    clearVegetationBuffer(level, wall);
    settleFoundations(level, wall);
  }

  /** Clears the natural route before any worker selects a cell; never loads missing chunks to do so. */
  public static boolean prepareWall(Level level, WallProject wall) {
    if (wall.isSiteCleared()) return true;
    if (!(level instanceof ServerLevel serverLevel)) return false;
    Set<Long> clearance = SiteClearance.horizontalReach(occupiedColumns(wall), SiteClearance.TREE_RADIUS);
    for (long packed : clearance) {
      if (!level.hasChunkAt(BlockPos.of(packed))) return false;
    }
    int trees = fellTreesNearWall(level, wall);
    int foliage = clearVegetationBuffer(level, wall);
    wall.markSiteCleared();
    com.quzzar.kithkyn.village.VillageManager.get(serverLevel).setDirty();
    com.quzzar.kithkyn.Kithkyn.LOGGER.debug("[wall] prepared route before construction: {} trees, {} foliage blocks", trees, foliage);
    return true;
  }

  /**
   * Uses the shared lumberjack rules to open a three-block tree line around the
   * wall. Only natural trees come down, and their wood remains in the world for
   * villagers to collect.
   */
  public static int fellTreesNearWall(Level level, WallProject wall) {
    if (!(level instanceof ServerLevel serverLevel)) {
      return 0;
    }
    List<TreeFelling.FelledTree> felled = SiteClearance.fellTrees(serverLevel, occupiedColumns(wall));
    for (TreeFelling.FelledTree tree : felled) {
      for (ItemStack drop : tree.drops()) {
        Block.popResource(serverLevel, tree.struck(), drop);
      }
    }
    return felled.size();
  }

  /**
   * Clears natural vegetation through the wall's own columns and one block on
   * both sides. Including occupied columns removes canopy above the palisade;
   * the vertical scan stops at the wall, real ground, or protected construction.
   */
  public static int clearVegetationBuffer(Level level, WallProject wall) {
    return level instanceof ServerLevel serverLevel
        ? SiteClearance.clearFoliage(serverLevel, occupiedColumns(wall), List.of()) : 0;
  }

  /** Every horizontal column occupied by the compiled wall, including features. */
  private static Set<Long> occupiedColumns(WallProject wall) {
    Set<Long> occupiedColumns = new java.util.HashSet<>();
    for (int sectionIndex = 0; sectionIndex < wall.sectionCount(); sectionIndex++) {
      for (WallBlockPlan block : wall.section(sectionIndex).blocks()) {
        BlockPos pos = block.pos();
        occupiedColumns.add(BlockPos.asLong(pos.getX(), 0, pos.getZ()));
      }
    }
    return Set.copyOf(occupiedColumns);
  }

  /**
   * Rechecks the built silhouette after neighboring cells have cleared its
   * final sightlines. This catches a bank edge that was concealed while its
   * first post was placed, then became exposed later in the build.
   */
  public static void settleFoundations(Level level, WallProject wall) {
    settleConnections(level, wall);
    java.util.Map<Long, BlockPos> bases = new java.util.HashMap<>();
    java.util.Map<Long, Integer> plannedFeet = new java.util.HashMap<>();
    for (int sectionIndex = 0; sectionIndex < wall.sectionCount(); sectionIndex++) {
      for (WallBlockPlan block : wall.section(sectionIndex).blocks()) {
        if (!isStructuralFoundationPiece(block.piece())) {
          continue;
        }
        BlockPos base = block.pos();
        long column = BlockPos.asLong(base.getX(), 0, base.getZ());
        plannedFeet.merge(column, base.getY(), Math::min);
        if (!level.hasChunkAt(base)) continue;
        if (!isVillageStructuralBlock(level, base)) {
          continue;
        }
        while (base.getY() > level.getMinBuildHeight()
            && isVillageStructuralBlock(level, base.below())) {
          base = base.below();
        }
        bases.merge(column, base,
            (left, right) -> left.getY() <= right.getY() ? left : right);
      }
    }
    for (BlockPos base : bases.values()) {
      long column = BlockPos.asLong(base.getX(), 0, base.getZ());
      // A repeated maintenance pass must never dig below the original foot.
      // Live extensions below planned feet already embed their own first soil course.
      if (base.getY() < plannedFeet.get(column)) continue;
      if (!shouldEmbedSoil(level, base.below())) {
        continue;
      }
      BlockPos soil = base.below();
      level.setBlock(soil, level.getBlockState(base), 3);
      markVillagePlaced(level, soil);
    }
  }

  /** Reconnect old village-owned masonry/fences without repainting player edits. */
  public static void settleConnections(Level level, WallProject wall) {
    if (!(level instanceof ServerLevel serverLevel)) return;
    PlacedBlockStore owned = PlacedBlockStore.get(serverLevel);
    for (WallBlockPlan cell : wall.plannedBlocks()) {
      BlockPos pos = cell.pos();
      if (!level.hasChunkAt(pos) || owned.isPlayerPlaced(pos) || !owned.isVillagePlaced(pos)) continue;
      BlockState current = level.getBlockState(pos);
      if (!(current.getBlock() instanceof net.minecraft.world.level.block.WallBlock)
          && !(current.getBlock() instanceof net.minecraft.world.level.block.FenceBlock)) continue;
      if (!current.is(cell.desiredState(wall.getTier(), wall.getStyle()).getBlock())) continue;
      BlockState connected = Block.updateFromNeighbourShapes(current, level, pos);
      if (!connected.equals(current)) level.setBlock(pos, connected, 3);
    }
  }

  private static boolean isVillageStructuralBlock(Level level, BlockPos pos) {
    return isVillagePlaced(level, pos)
        && !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
  }

  private static boolean isStructuralFoundationPiece(WallBlockPlan.Piece piece) {
    return piece == WallBlockPlan.Piece.BODY
        || piece == WallBlockPlan.Piece.POST
        || piece == WallBlockPlan.Piece.COBBLE_POST
        || piece == WallBlockPlan.Piece.MOSSY_POST
        || piece == WallBlockPlan.Piece.BEAM_NORTH_SOUTH
        || piece == WallBlockPlan.Piece.BEAM_EAST_WEST;
  }

  private static void markVillagePlaced(Level level, BlockPos pos) {
    if (level instanceof ServerLevel serverLevel) {
      PlacedBlockStore.get(serverLevel).markVillagePlaced(pos);
    }
  }

  private static boolean isVillagePlaced(Level level, BlockPos pos) {
    return level instanceof ServerLevel serverLevel
        && PlacedBlockStore.get(serverLevel).isVillagePlaced(pos);
  }

  /** Whether a tree or plant is world vegetation rather than owned construction. */
  private static boolean isNaturalClearable(Level level, BlockPos pos, BlockState state) {
    return SiteClearance.isNaturalClearable(level, pos, state);
  }

}
