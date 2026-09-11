package com.quzzar.kithkyn.entities.ai.goals.work;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import com.quzzar.kithkyn.village.buildings.MineShaft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * The mine's planned interior in mine-local coordinates.
 *
 * <p>The descending ramp and its horizontal prospecting ribs are one connected
 * volume. Construction, waterproofing, and ore exposure must agree on that
 * volume. In particular, the first cell of a rib is an intentional doorway
 * through the ramp wall, while a neighbour outside both shapes is mine lining.
 */
final class MineTopology {

  /** Diagnostic detail for a planned-mine connectivity search. */
  record StandPath(boolean connected, int reachableCount, BlockPos furthest) {
  }

  static final int FAN_LENGTH = MineShaft.RIB_LENGTH;
  static final int FAN_PITCH = MineShaft.RIB_PITCH;
  static final int FAN_HEIGHT = MineShaft.RIB_HEIGHT;
  static final int FAN_MIN_LINE = MineShaft.RIB_MIN_LINE;

  /**
   * The two wall positions that can mark the top of an underground child shaft.
   * They sit at head height above its first full-width stair. The selector uses
   * the first supported side and treats either occupied light position as the
   * one entrance marker, so a child gets a visible threshold without wasting a
   * torch on both walls.
   */
  private final int minX;
  private final int maxX;

  MineTopology(int radius) {
    this(-radius, radius);
  }

  MineTopology(int minX, int maxX) {
    if (minX > 0 || maxX < 0 || minX > maxX) {
      throw new IllegalArgumentException("Mine corridor must contain local x 0");
    }
    this.minX = minX;
    this.maxX = maxX;
  }

  List<BlockPos> entranceTorchCells() {
    return List.of(new BlockPos(minX, -1, 0), new BlockPos(maxX, -1, 0));
  }

  /** The walk-cell Y shared by a ramp column and any rib cut from it. */
  static int floorY(int z) {
    return z < 0 ? -1 : -(z + 2);
  }

  /**
   * Ramp cells in entrance-first order through a selected face. A stalled
   * frontier search is about the shaft's progress, never the worker's current
   * position: an idle miner is free to wander without making deep work disappear.
   */
  List<BlockPos> rampCellsThrough(int lastZ) {
    List<BlockPos> cells = new ArrayList<>();
    for (int z = MineShaft.ENTRY_COLUMN; z <= lastZ; z++) {
      int floor = floorY(z);
      int ceiling = Math.min(floor + MineShaft.RAMP_HEIGHT - 1, -1);
      for (int y = floor; y <= ceiling; y++) {
        for (int x = minX; x <= maxX; x++) {
          cells.add(new BlockPos(x, y, z));
        }
      }
    }
    return cells;
  }

  /**
   * Places inside the already-planned ramp from which a missing floor support
   * can be laid. Candidates may be beside the gap or one block above it on the
   * preceding stair, but never below the ramp or farther down the unfinished
   * shaft. A cave floor beneath the bridge is therefore not mistaken for safe
   * footing.
   */
  List<BlockPos> floorStandCandidates(BlockPos floorCell) {
    List<BlockPos> candidates = new ArrayList<>();
    candidates.add(floorCell.above());
    for (Direction direction : Direction.Plane.HORIZONTAL) {
      candidates.add(floorCell.relative(direction));
      candidates.add(floorCell.above().relative(direction));
    }
    return candidates.stream()
        .filter(this::isRamp)
        .filter(candidate -> candidate.getZ() <= floorCell.getZ())
        .toList();
  }

  /**
   * Supported footholds that may anchor a route into the mine. The structure's
   * visible threshold may be a stair or another walkable partial block, so the
   * route audit must not require that single decorative block to expose a full
   * sturdy top face. The first ordinary supported ramp cell just beyond it is
   * an equally valid connectivity anchor.
   */
  List<BlockPos> entranceStandCandidates() {
    List<BlockPos> candidates = new ArrayList<>();
    int[] across = {0, -1, 1, -2, 2};
    for (int z = MineShaft.ENTRY_COLUMN; z <= 1; z++) {
      int y = floorY(z);
      for (int x : across) {
        BlockPos candidate = new BlockPos(x, y, z);
        if (isRamp(candidate)) {
          candidates.add(candidate);
        }
      }
    }
    return List.copyOf(candidates);
  }

  /**
   * Planned mine cells close enough to work one target. A descending front can
   * leave its nearest dry footing both higher and behind the target, so this is
   * a reach sphere rather than only the target's cardinal neighbours.
   */
  List<BlockPos> workStandCandidates(BlockPos target, double reachSqr) {
    List<BlockPos> candidates = new ArrayList<>();
    int search = (int) Math.ceil(Math.sqrt(reachSqr));
    for (int dz = -search; dz <= search; dz++) {
      for (int dy = -search; dy <= search; dy++) {
        for (int dx = -search; dx <= search; dx++) {
          BlockPos candidate = target.offset(dx, dy, dz);
          if (!candidate.equals(target)
              && candidate.distSqr(target) <= reachSqr
              && isNavigableStand(candidate)) {
            candidates.add(candidate);
          }
        }
      }
    }
    return candidates;
  }

  /**
   * Whether two open footholds are connected through the planned mine. This is
   * deliberately a geometry walk rather than a Minecraft path search: the mine
   * navigator searches one ramp waypoint at a time, so a successful first hop
   * says nothing about a solid column farther down the requested route.
   *
   * <p>A ramp step changes one horizontal coordinate and may rise or fall one
   * block. Restricting neighbours to that shape avoids cutting diagonally through
   * a solid corner while still joining every ordinary stair and horizontal rib.
   */
  boolean standPathExists(BlockPos start, BlockPos target,
      Predicate<BlockPos> isOpenStand, int maxCells) {
    return standPath(start, target, isOpenStand, maxCells).connected();
  }

  /**
   * The route result plus its farthest reachable foothold. Keeping this detail
   * beside the geometry search makes a live damaged mine explain where its
   * connected walkable volume actually ends, rather than merely saying that a
   * deep face is unreachable.
   */
  StandPath standPath(BlockPos start, BlockPos target,
      Predicate<BlockPos> isOpenStand, int maxCells) {
    if (!isNavigableStand(start) || !isNavigableStand(target)
        || !isOpenStand.test(start) || !isOpenStand.test(target)) {
      return new StandPath(false, 0, start);
    }
    Deque<BlockPos> frontier = new ArrayDeque<>();
    Set<BlockPos> seen = new HashSet<>();
    frontier.add(start);
    seen.add(start);
    BlockPos furthest = start;
    while (!frontier.isEmpty() && seen.size() <= maxCells) {
      BlockPos current = frontier.poll();
      if (current.getZ() > furthest.getZ()
          || current.getZ() == furthest.getZ() && current.getY() < furthest.getY()) {
        furthest = current;
      }
      if (current.equals(target)) {
        return new StandPath(true, seen.size(), furthest);
      }
      for (Direction direction : Direction.Plane.HORIZONTAL) {
        for (int dy = -1; dy <= 1; dy++) {
          BlockPos next = current.relative(direction).offset(0, dy, 0);
          if (isNavigableStand(next) && seen.add(next) && isOpenStand.test(next)) {
            frontier.add(next);
          }
        }
      }
    }
    return new StandPath(false, seen.size(), furthest);
  }

  /** The five-cell-high descending shaft, capped below the surface structure. */
  boolean isRamp(BlockPos local) {
    return MineShaft.withinCorridor(local, minX, maxX)
        && local.getY() >= -(local.getZ() + 2)
        && local.getY() <= -(local.getZ() - 2);
  }

  /** A one-cell-wide horizontal prospecting rib on one of the fixed grid lines. */
  boolean isRib(BlockPos local) {
    return MineShaft.withinRib(local, minX, maxX);
  }

  /**
   * The first cell of the rib outside the ramp wall, at the same height as a
   * flooded cell farther along that rib. This is the safe place for a temporary
   * bulkhead: it gives up the wet branch without filling or narrowing the ramp.
   */
  BlockPos ribDoorway(BlockPos local) {
    if (!isRib(local)) {
      return null;
    }
    int doorwayX = local.getX() < minX ? minX - 1 : maxX + 1;
    return new BlockPos(doorwayX, local.getY(), local.getZ());
  }

  /** Every cell the mine deliberately opens, whether it belongs to the ramp or a rib. */
  boolean isInterior(BlockPos local) {
    return isRamp(local) || isRib(local) || MineShaft.withinEntranceClearance(local);
  }

  /**
   * Whether a miner may use this cell as footing. Work stands must belong to
   * the planned ramp or ribs, which shaft navigation also recognizes. Even a
   * cell opened while following ore is not safe footing outside that geometry:
   * navigation would treat the work as an exit and send the miner up the ramp.
   */
  boolean isNavigableStand(BlockPos local) {
    return isInterior(local);
  }

  /**
   * Whether two adjacent cells cross from the planned mine interior into its
   * exterior lining. The caller decides whether the exterior block's current
   * state is solid, air, or fluid.
   */
  boolean crossesExteriorBoundary(BlockPos inside, BlockPos neighbour) {
    int distance = Math.abs(inside.getX() - neighbour.getX())
        + Math.abs(inside.getY() - neighbour.getY())
        + Math.abs(inside.getZ() - neighbour.getZ());
    return distance == 1 && isInterior(inside) && !isInterior(neighbour);
  }
}
