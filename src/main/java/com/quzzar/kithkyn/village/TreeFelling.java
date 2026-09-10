package com.quzzar.kithkyn.village;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import com.quzzar.kithkyn.savedata.PlacedBlockStore;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Bringing a natural tree down whole. One log is struck and every log
 * connected to it comes away at once, a bounded flood fill over the trunk and
 * its branches, each log yielding its drops. Unowned bee nests or hives attached
 * to removed logs come down too, with normal bee release and Silk Touch loot.
 * Natural leaves that belonged to the removed logs decay with the fell. The
 * cleanup deliberately ignores player- and village-owned logs as support, so
 * a timber building cannot hold a severed canopy over a lumberjack's next
 * sapling. Persistent or ownership-marked leaves remain, as does canopy still
 * supported by another natural tree.
 *
 * <b>It only ever cuts real trees.</b> Two guards keep it off the village's
 * own timber and off anything a player built:
 * <ul>
 * <li>A log counts as a tree only with a <i>natural</i> canopy nearby, leaves
 * whose {@code persistent} flag is false. Player-placed leaves are persistent
 * and a building's timber has none of its own, so this tells a living tree
 * from authored timber far more reliably than leaf-proximity alone did.</li>
 * <li>{@link BlockOwnership#mayFell} vetoes any block a player or a village
 * placed. The flood fill re-checks every log, so a wild tree overhanging a
 * roof drops its own wood and spares the building's.</li>
 * </ul>
 *
 * Workers and construction cleanup share it: the lumberjack's and guard's
 * {@code ChopStep} fells one tree per chop and pockets the wood, building
 * placement and wall completion open a shared narrow tree line, including
 * overhead trunks ({@link #fellWithin}). Where the drops
 * go is each caller's business. The one
 * tree the ownership guard does not cover is the lumberjack's own stand, the
 * lumberjack's to cut whatever the store says of it ({@link #fellStand}).
 */
public final class TreeFelling {

  /** Natural leaves this close mark a trunk as a living tree, not authored timber. */
  private static final int LEAF_RADIUS = 3;

  /** Most logs one fell removes: a ceiling for giant trees so the flood fill always ends. */
  private static final int TREE_LOG_CAP = 256;

  /** Vanilla leaves live at most this many leaf steps from a supporting log. */
  private static final int CANOPY_REACH = LeavesBlock.DECAY_DISTANCE - 1;

  /** A ceiling for modded giant canopies so one fell always has bounded work. */
  private static final int CANOPY_LEAF_CAP = 4096;

  /** One tree brought down: the log it was struck at, and what its logs dropped. */
  public record FelledTree(BlockPos struck, List<ItemStack> drops) {
  }

  /** One position in a bounded canopy/support walk and its distance from the start. */
  private record CanopySearch(BlockPos pos, int distance) {
  }

  private TreeFelling() {
  }

  /**
   * Whether the block here is a log any feller may bring down: a burnable log
   * with a natural canopy near it, that nobody placed. Unloaded chunks are
   * never paged in to answer; a position in one is simply not fellable.
   */
  public static boolean isFellableLog(ServerLevel level, BlockPos pos) {
    return level.hasChunkAt(pos)
        && level.getBlockState(pos).is(BlockTags.LOGS_THAT_BURN)
        && !level.getBlockState(pos).hasBlockEntity()
        && BlockOwnership.mayFell(level, pos)
        && hasNaturalCanopy(level, pos);
  }

  /**
   * Brings the whole tree down from the struck log and returns what it
   * dropped. Each connected log the village and players do not own is removed
   * and its drops taken, as though {@code feller} broke it with {@code tool};
   * one break sound plays for the lot. A null feller with an empty tool is a
   * plain break, which is what site clearing does.
   */
  public static List<ItemStack> fell(ServerLevel level, BlockPos struck, @Nullable Entity feller,
      ItemStack tool) {
    return fell(level, struck, feller, tool, false);
  }

  /**
   * Brings down the tree at a lumberjack's stand. Ownership is not asked at
   * all for logs here (Aaron, 2026-09-02): every log connected to the station comes
   * down, whoever placed it, whatever its height and however far its branches
   * reach, and each one's record goes with it. The lodge once shipped a grown
   * tree, stamped as the village's, and under {@link #fell} the lumberjack
   * struck that trunk, nothing came away, and the loop offered the same log
   * again; the lodge now ships a sapling, and the exemption stays so that
   * nothing anyone places against the stand can jam the loop. Otherwise
   * identical: whole tree, drops returned, and its orphaned natural canopy
   * decayed. Attached hives still respect ownership, so a nearby apiary cannot
   * be lost to stand work.
   */
  public static List<ItemStack> fellStand(ServerLevel level, BlockPos struck, @Nullable Entity feller,
      ItemStack tool) {
    return fell(level, struck, feller, tool, true);
  }

  /**
   * Every log removed has its ownership record dropped with it, as a break
   * does: the stand's next tree grows on the first one's positions, and must
   * not inherit the template's protection.
   */
  private static List<ItemStack> fell(ServerLevel level, BlockPos struck, @Nullable Entity feller,
      ItemStack tool, boolean stand) {
    List<ItemStack> drops = new ArrayList<>();
    BlockState soundFrom = null;
    LongOpenHashSet attachedHives = new LongOpenHashSet();
    PlacedBlockStore placed = PlacedBlockStore.get(level);
    List<BlockPos> logs = new ArrayList<>();
    for (BlockPos pos : treeLogs(level, struck)) {
      BlockState state = level.getBlockState(pos);
      if (!state.is(BlockTags.LOGS_THAT_BURN) || state.hasBlockEntity()
          || (!stand && !BlockOwnership.mayFell(level, pos))) {
        continue;
      }
      logs.add(pos);
    }
    List<BlockPos> canopy = canopyLeaves(level, logs, placed);
    for (BlockPos pos : logs) {
      BlockState state = level.getBlockState(pos);
      drops.addAll(Block.getDrops(state, level, pos, level.getBlockEntity(pos), feller, tool));
      if (soundFrom == null) {
        soundFrom = state;
      }
      level.removeBlock(pos, false);
      placed.clearPlaced(pos);
      for (Direction direction : Direction.values()) {
        BlockPos adjacent = pos.relative(direction);
        if (level.hasChunkAt(adjacent) && level.getBlockState(adjacent).is(BlockTags.BEEHIVES)) {
          attachedHives.add(adjacent.asLong());
        }
      }
    }
    for (long hive : attachedHives) {
      fellAttachedHive(level, BlockPos.of(hive), feller, tool, drops);
    }
    decayOrphanedCanopy(level, canopy, placed);
    if (soundFrom != null) {
      level.playSound((Player) null, struck.getX(), struck.getY(), struck.getZ(),
          soundFrom.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.0F,
          level.getRandom().nextFloat() * 0.4F + 0.8F);
    }
    return drops;
  }

  /**
   * Captures the natural leaves this tree supports before its logs disappear.
   * This is the same six-direction distance model vanilla leaves use, bounded
   * both by vanilla's maximum support distance and a hard leaf count. Owned or
   * persistent leaves are barriers, not part of a natural canopy.
   */
  private static List<BlockPos> canopyLeaves(ServerLevel level, List<BlockPos> logs,
      PlacedBlockStore placed) {
    List<BlockPos> leaves = new ArrayList<>();
    ArrayDeque<CanopySearch> frontier = new ArrayDeque<>();
    LongOpenHashSet visited = new LongOpenHashSet();
    for (BlockPos log : logs) {
      BlockPos start = log.immutable();
      if (visited.add(start.asLong())) {
        frontier.add(new CanopySearch(start, 0));
      }
    }
    while (!frontier.isEmpty() && leaves.size() < CANOPY_LEAF_CAP) {
      CanopySearch current = frontier.poll();
      if (current.distance() >= CANOPY_REACH) {
        continue;
      }
      for (Direction direction : Direction.values()) {
        BlockPos next = current.pos().relative(direction);
        if (!visited.add(next.asLong()) || !level.hasChunkAt(next)) {
          continue;
        }
        BlockState state = level.getBlockState(next);
        if (!isNaturalLeaf(state, next, placed)) {
          continue;
        }
        BlockPos leaf = next.immutable();
        leaves.add(leaf);
        int distance = current.distance() + 1;
        if (distance < CANOPY_REACH) {
          frontier.add(new CanopySearch(leaf, distance));
        }
      }
    }
    return leaves;
  }

  /**
   * Forces only unsupported natural leaves through the ordinary decay result.
   * Drops are spawned where each leaf stood, just as a vanilla random decay
   * would spawn them, so saplings remain physical things for workers to pick up.
   */
  private static void decayOrphanedCanopy(ServerLevel level, List<BlockPos> canopy,
      PlacedBlockStore placed) {
    for (BlockPos leaf : canopy) {
      BlockState state = level.getBlockState(leaf);
      if (!isNaturalLeaf(state, leaf, placed)
          || hasNaturalSupportOrUnknown(level, leaf, placed)) {
        continue;
      }
      Block.dropResources(state, level, leaf);
      level.removeBlock(leaf, false);
    }
  }

  /** Natural, removable foliage rather than player or village decoration. */
  private static boolean isNaturalLeaf(BlockState state, BlockPos pos, PlacedBlockStore placed) {
    return state.is(BlockTags.LEAVES)
        && state.hasProperty(LeavesBlock.PERSISTENT)
        && !state.getValue(LeavesBlock.PERSISTENT)
        && !placed.isPlayerPlaced(pos)
        && !placed.isVillagePlaced(pos);
  }

  /**
   * Whether another natural log can support this leaf through vanilla's leaf
   * distance. Player- and village-owned logs are structures, so they do not
   * preserve a canopy whose tree was felled. An unloaded neighbor is unknown
   * and therefore preserves the leaf rather than risking another tree's canopy.
   */
  private static boolean hasNaturalSupportOrUnknown(ServerLevel level, BlockPos start,
      PlacedBlockStore placed) {
    ArrayDeque<CanopySearch> frontier = new ArrayDeque<>();
    LongOpenHashSet visited = new LongOpenHashSet();
    frontier.add(new CanopySearch(start, 0));
    visited.add(start.asLong());
    while (!frontier.isEmpty()) {
      CanopySearch current = frontier.poll();
      if (current.distance() >= CANOPY_REACH) {
        continue;
      }
      for (Direction direction : Direction.values()) {
        BlockPos next = current.pos().relative(direction);
        if (!level.hasChunkAt(next)) {
          return true;
        }
        BlockState state = level.getBlockState(next);
        if (state.is(BlockTags.LOGS)
            && !placed.isPlayerPlaced(next)
            && !placed.isVillagePlaced(next)) {
          return true;
        }
        int distance = current.distance() + 1;
        if (distance < CANOPY_REACH && state.is(BlockTags.LEAVES)
            && visited.add(next.asLong())) {
          frontier.add(new CanopySearch(next.immutable(), distance));
        }
      }
    }
    return false;
  }

  /** Only hives touching a removed log come down; even stand felling preserves owned apiaries. */
  private static void fellAttachedHive(ServerLevel level, BlockPos pos, @Nullable Entity feller,
      ItemStack tool, List<ItemStack> drops) {
    BlockState state = level.getBlockState(pos);
    if (!state.is(BlockTags.BEEHIVES) || !BlockOwnership.mayFell(level, pos)
        || !(level.getBlockEntity(pos) instanceof BeehiveBlockEntity hive)) return;
    if (!EnchantmentHelper.hasTag(tool, EnchantmentTags.PREVENTS_BEE_SPAWNS_WHEN_MINING)) {
      hive.emptyAllLivingFromHive(feller instanceof Player player ? player : null,
          state, BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
    }
    drops.addAll(Block.getDrops(state, level, pos, hive, feller, tool));
    level.removeBlock(pos, false);
    PlacedBlockStore.get(level).clearPlaced(pos);
    level.updateNeighbourForOutputSignal(pos, state.getBlock());
  }

  /**
   * Fells every natural tree with a trunk crossing one of the supplied columns.
   * Callers can clear a building footprint or follow a wall route without also
   * cutting every tree inside its enclosing rectangle. Reads canopy downwards,
   * covering tall remnants and overhead branches, and never loads unseen chunks.
   */
  public static List<FelledTree> fellWithin(ServerLevel level, Set<Long> columns) {
    List<FelledTree> felled = new ArrayList<>();
    for (long column : columns) {
      fellColumn(level, BlockPos.getX(column), BlockPos.getZ(column),
          level.getMinBuildHeight(), felled);
    }
    return felled;
  }

  /** Scans one loaded column from its canopy down and fells every tree it meets. */
  private static void fellColumn(ServerLevel level, int x, int z, int minimumY,
      List<FelledTree> felled) {
    BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, minimumY, z);
    if (!level.hasChunkAt(cursor)) {
      return;
    }
    // The heightmap's value is the first air above the tallest block in the column.
    int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
    for (int y = top; y >= minimumY; y--) {
      if (isFellableLog(level, cursor.set(x, y, z))) {
        BlockPos struck = cursor.immutable();
        felled.add(new FelledTree(struck, fell(level, struck, null, ItemStack.EMPTY)));
      }
    }
  }

  /**
   * The connected logs of one tree, reached from any of its logs by a bounded
   * breadth-first walk over all twenty-six neighbours so branches and leaning
   * trunks come with it. Capped so a giant tree cannot make the fill unbounded.
   * This is also how a tree's base is found: the lowest of these. Walking
   * straight down from a log is not enough, because a mangrove is all
   * branches and the walk stops at a log in the canopy with leaves under it.
   */
  public static List<BlockPos> treeLogs(ServerLevel level, BlockPos start) {
    List<BlockPos> logs = new ArrayList<>();
    ArrayDeque<BlockPos> frontier = new ArrayDeque<>();
    LongOpenHashSet visited = new LongOpenHashSet();
    frontier.add(start);
    visited.add(start.asLong());
    while (!frontier.isEmpty() && logs.size() < TREE_LOG_CAP) {
      BlockPos pos = frontier.poll();
      if (!level.hasChunkAt(pos) || !level.getBlockState(pos).is(BlockTags.LOGS_THAT_BURN)) {
        continue;
      }
      logs.add(pos);
      for (int dx = -1; dx <= 1; dx++) {
        for (int dy = -1; dy <= 1; dy++) {
          for (int dz = -1; dz <= 1; dz++) {
            if (dx == 0 && dy == 0 && dz == 0) {
              continue;
            }
            BlockPos next = pos.offset(dx, dy, dz);
            if (visited.add(next.asLong())) {
              frontier.add(next);
            }
          }
        }
      }
    }
    return logs;
  }

  /**
   * Whether naturally grown leaves ({@code persistent == false}) sit close to a
   * log. This is what tells a living tree from a stack of authored timber: a
   * player's placed leaves are persistent, and a building carries none.
   */
  private static boolean hasNaturalCanopy(ServerLevel level, BlockPos log) {
    BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    for (int x = -LEAF_RADIUS; x <= LEAF_RADIUS; ++x) {
      for (int y = -LEAF_RADIUS; y <= LEAF_RADIUS; ++y) {
        for (int z = -LEAF_RADIUS; z <= LEAF_RADIUS; ++z) {
          cursor.setWithOffset(log, x, y, z);
          if (!level.hasChunkAt(cursor)) {
            continue;
          }
          BlockState state = level.getBlockState(cursor);
          if (state.is(BlockTags.LEAVES) && state.hasProperty(LeavesBlock.PERSISTENT)
              && !state.getValue(LeavesBlock.PERSISTENT)) {
            return true;
          }
        }
      }
    }
    return false;
  }

}
