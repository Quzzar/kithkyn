package com.quzzar.kithkyn.village.buildings;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.entities.RealPerson;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;

/**
 * Worker-specific approaches to a redevelopment site. Positions stay outside both
 * the replacement and the buildings being removed, including their ground work.
 * Routes are transient: reloads and replacement builders find their own way in.
 */
public final class ConstructionAccess {
  private static final int PATHS_PER_SCAN = 8;
  private static final long RETRY_TICKS = 200;
  /** Longer than the shared work loop's 600-tick stand-down after physical failure. */
  private static final long STALLED_RETRY_TICKS = 1200;
  private static final String BLOCKER = "I cannot reach a clear position outside the redevelopment site.";

  private final Map<UUID, Approach> approaches = new HashMap<>();
  private List<BoundingBox> footprints;
  private Set<Long> groundWork;

  private static final class Approach {
    private BlockPos position;
    private int cursor;
    private long nextScan;
    private boolean blocked;
    private final Map<BlockPos, Long> failedUntil = new HashMap<>();
  }

  /** Selects a bounded batch of paths; later scans continue around the perimeter. */
  @Nullable
  public BlockPos select(RealPerson person, StructureInProgress project) {
    if (!initialize(person, project)) {
      person.logBlocker(BLOCKER);
      return null;
    }
    Approach approach = approaches.computeIfAbsent(person.getUUID(), ignored -> new Approach());
    long now = person.level().getGameTime();
    approach.failedUntil.entrySet().removeIf(entry -> entry.getValue() <= now);
    if (approach.position != null && safe(person, approach.position)) {
      return approach.position;
    }
    approach.position = null;
    if (now < approach.nextScan) {
      return null;
    }
    List<BlockPos> candidates = candidates(person, project);
    int checked = 0;
    int visited = 0;
    while (visited < candidates.size() && checked < PATHS_PER_SCAN) {
      BlockPos candidate = candidates.get(Math.floorMod(approach.cursor++, candidates.size()));
      visited++;
      if (approach.failedUntil.containsKey(candidate)) {
        continue;
      }
      checked++;
      Path path = person.getNavigation().createPath(candidate, 0);
      if (path == null || !path.canReach() || path.getEndNode() == null
          || !path.getEndNode().asBlockPos().equals(candidate)) {
        approach.failedUntil.put(candidate, now + RETRY_TICKS);
        continue;
      }
      approach.position = candidate;
      approach.blocked = false;
      person.clearBlocker(BLOCKER);
      Kithkyn.LOGGER.debug("[construction-access] {} will work on {} from outside at {}",
          person.getFullName(), project.getBuilding().getName(), candidate.toShortString());
      return candidate;
    }
    approach.nextScan = now + 20;
    approach.blocked = true;
    person.logBlocker(BLOCKER);
    return null;
  }

  /** A route that looked feasible but physically stalled must yield to another approach. */
  public void unreachable(RealPerson person, BlockPos position) {
    Approach approach = approaches.computeIfAbsent(person.getUUID(), ignored -> new Approach());
    approach.failedUntil.put(position, person.level().getGameTime() + STALLED_RETRY_TICKS);
    approach.position = null;
    approach.nextScan = 0;
    approach.blocked = true;
    person.logBlocker(BLOCKER);
  }

  /** Both the selected spot and the worker's body must remain outside the changing site. */
  public boolean inReach(RealPerson person, BlockPos position) {
    return person.blockPosition().distSqr(position) <= 1.0D
        && safe(person, person.blockPosition())
        && outside(footprints, person.getX(), person.getZ(), person.getBbWidth());
  }

  /** Rechecked before every builder swing, so a changed approach releases its target. */
  public boolean safe(RealPerson person, BlockPos position) {
    if (footprints == null || !person.level().hasChunkAt(position)
        || !outside(footprints, position.getX() + 0.5D, position.getZ() + 0.5D, person.getBbWidth())) {
      return false;
    }
    BlockPos below = position.below();
    if (groundWork.contains(new BlockPos(position.getX(), 0, position.getZ()).asLong())
        || !person.level().getFluidState(position).isEmpty()
        || !person.level().getBlockState(below).isFaceSturdy(person.level(), below, Direction.UP)) {
      return false;
    }
    double half = person.getBbWidth() / 2.0D;
    AABB body = new AABB(position.getX() + 0.5D - half, position.getY(), position.getZ() + 0.5D - half,
        position.getX() + 0.5D + half, position.getY() + person.getBbHeight(), position.getZ() + 0.5D + half);
    return person.level().noCollision(person, body);
  }

  /** Explains a gather timeout without confusing inaccessible work with missing materials. */
  public String failureReason() {
    return approaches.values().stream().anyMatch(approach -> approach.blocked)
        ? "builders could not reach a clear position outside the redevelopment site" : "";
  }

  /** Geometry includes the replacement, victims, and all changed ground columns. */
  private boolean initialize(RealPerson person, StructureInProgress project) {
    if (footprints != null) {
      return true;
    }
    var village = person.getVillage();
    if (village == null || project.getRedevelopment() == null) {
      return false;
    }
    List<Building> affected = new ArrayList<>(project.getRedevelopment().plan().removed());
    affected.add(project.getBuilding());
    List<BoundingBox> boxes = new ArrayList<>();
    for (Building building : affected) {
      BoundingBox bounds = RedevelopmentPlanner.worldBounds(village, building);
      if (bounds == null) {
        return false;
      }
      boxes.add(bounds);
    }
    Set<Long> columns = new java.util.HashSet<>();
    var plan = project.getRedevelopment().plan();
    for (long packed : java.util.stream.Stream.concat(plan.prepBreak().stream(), plan.prepFill().stream()).toList()) {
      BlockPos at = BlockPos.of(packed);
      columns.add(new BlockPos(at.getX(), 0, at.getZ()).asLong());
    }
    footprints = List.copyOf(boxes);
    groundWork = Set.copyOf(columns);
    return true;
  }

  /** Nearby ground heights, without assuming the surface is the roof of an old building. */
  private List<BlockPos> candidates(RealPerson person, StructureInProgress project) {
    Set<BlockPos> candidates = new LinkedHashSet<>();
    int ground = BlockPos.of(project.getRedevelopment().plan().ground()).getY();
    for (BlockPos column : perimeter(footprints)) {
      for (int y = ground + 3; y >= ground - 4; y--) {
        BlockPos at = new BlockPos(column.getX(), y, column.getZ());
        if (safe(person, at)) {
          candidates.add(at);
        }
      }
    }
    return candidates.stream().sorted(Comparator.comparingDouble(at -> at.distSqr(person.blockPosition()))).toList();
  }

  /** Columns beside each footprint, excluding overlapping footprints even for wide workers. */
  static List<BlockPos> perimeter(List<BoundingBox> boxes) {
    Set<BlockPos> columns = new LinkedHashSet<>();
    for (BoundingBox box : boxes) {
      for (int margin = 1; margin <= 2; margin++) {
        for (int x = box.minX() - margin; x <= box.maxX() + margin; x++) {
          columns.add(new BlockPos(x, 0, box.minZ() - margin));
          columns.add(new BlockPos(x, 0, box.maxZ() + margin));
        }
        for (int z = box.minZ() - margin + 1; z < box.maxZ() + margin; z++) {
          columns.add(new BlockPos(box.minX() - margin, 0, z));
          columns.add(new BlockPos(box.maxX() + margin, 0, z));
        }
      }
    }
    return columns.stream().filter(at -> outside(boxes, at.getX() + 0.5D, at.getZ() + 0.5D, 0)).toList();
  }

  /** Checks full horizontal body clearance, rather than only the feet's block coordinate. */
  static boolean outside(List<BoundingBox> boxes, double x, double z, double width) {
    double half = width / 2.0D;
    return boxes.stream().noneMatch(box -> x + half > box.minX() && x - half < box.maxX() + 1
        && z + half > box.minZ() && z - half < box.maxZ() + 1);
  }
}
