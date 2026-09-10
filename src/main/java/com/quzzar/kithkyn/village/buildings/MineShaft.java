package com.quzzar.kithkyn.village.buildings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.quzzar.kithkyn.village.Occupation;
import com.quzzar.kithkyn.village.Village;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;

/**
 * One frame in a mine's bounded shaft network, and the ramps and parent ribs
 * that form the way in and out.
 *
 * <p>The shaft is the corridor the miner's cursor digs (MineStep): {@link #RADIUS}
 * either side of the mouth's centre line, ramping one block down for every
 * block forward, so the walk cell of column {@code z} sits {@code z + 2} below
 * the mouth. In the mine's own frame the mouth is the origin, forward is +z,
 * and a world position is read into that frame by undoing the shaft's rotation
 * (the building rotation composed with its authored descent direction).
 *
 * <p>Why the ramp has to be spelled out to the navigator: a villager's
 * pathfinder only expands nodes within their follow range, twenty blocks, of
 * where they stand, and past that it hands back a partial path to whichever
 * node lies closest to the target as the crow flies. For a face twenty layers
 * down that node is the surface directly above the shaft bottom, which is
 * where the miner kept walking, standing over his own work with no way down;
 * and from the face the walk to bed or the storehouse failed the same way in
 * reverse, until the stranded recovery teleported him home. {@link #waypoint}
 * gives the navigator the ramp a hop at a time instead. A child frame first
 * routes through its root ramp and source rib, and leaving reverses that chain.
 * Between two points of one shaft it walks straight when they are within a hop
 * of each other. Every goal gets it, since it sits under {@code moveTo}
 * ({@code entities/ai/PersonPathNavigation}).
 */
public final class MineShaft {

  /** Cells either side of the centre line the shaft is dug to: five wide. */
  public static final int RADIUS = 2;

  /** The threshold and ramp headroom do not shrink with the corridor width. */
  public static final int ENTRY_COLUMN = -1;
  public static final int RAMP_HEIGHT = 5;

  /** Geometry shared with the miner's fixed branch-mine pattern. */
  public static final int RIB_LENGTH = 8;
  public static final int RIB_PITCH = 4;
  public static final int RIB_HEIGHT = 3;
  public static final int RIB_MIN_LINE = 4;

  /**
   * Ramp columns one hop spans. A longer A* hop can cut across a natural cave
   * beneath the ramp even when both endpoints are valid walk cells. One column
   * makes every hop the adjacent, already-dug stair, so entering and leaving the
   * mine cannot shortcut off the built ramp.
   */
  private static final int HOP = 1;

  /** How near the mouth counts as standing at it, squared. */
  private static final double AT_MOUTH_SQR = 9.0D;

  private final int radius;
  private final BlockPos mouth;
  private final Rotation rotation;
  private final long rootStation;
  @Nullable
  private final MineShaft parent;
  @Nullable
  private final MineBranch branch;

  private MineShaft(BlockPos mouth, Rotation rotation, long rootStation, int radius,
      @Nullable MineShaft parent, @Nullable MineBranch branch) {
    this.radius = radius;
    this.mouth = mouth;
    this.rotation = rotation;
    this.rootStation = rootStation;
    this.parent = parent;
    this.branch = branch;
  }

  /** The original shaft anchored at a mine work station. */
  public static MineShaft root(BlockPos mouth, Rotation rotation, long rootStation) {
    return root(mouth, rotation, rootStation, 5);
  }

  /** A shaft with an authored odd corridor width. */
  public static MineShaft root(BlockPos mouth, Rotation rotation, long rootStation, int width) {
    if (width != 3 && width != 5) throw new IllegalArgumentException("Mine width must be 3 or 5");
    return new MineShaft(mouth, rotation, rootStation, width / 2, null, null);
  }

  public int radius() { return radius; }

  /** The same authored frame for excavation, navigation, and child-shaft planning. */
  public static MineShaft root(Building building, BlockPos station) {
    BuildingInfo.MineEntrance entrance = building.getMineEntrance();
    BlockPos mouth = station.offset(entrance.offset().rotate(building.getRotation()));
    var forward = building.getRotation().rotate(entrance.facing());
    for (Rotation rotation : Rotation.values()) {
      if (rotation.rotate(net.minecraft.core.Direction.SOUTH) == forward) {
        return root(mouth, rotation, rootStation(building, station), entrance.width());
      }
    }
    throw new IllegalArgumentException("Mine entrances must face horizontally");
  }

  /** A bounded second-generation shaft descending outward from a root rib. */
  public static MineShaft child(MineShaft root, MineBranch branch) {
    if (root.parent != null || root.rootStation != branch.rootStation()) {
      throw new IllegalArgumentException("A child shaft must belong to its root work station");
    }
    BlockPos mouth = childMouth(root.mouth, root.rotation, branch.sourceDepth(), branch.side(), root.radius);
    Rotation rotation = outwardRotation(root.rotation, branch.side());
    return new MineShaft(mouth, rotation, root.rootStation, root.radius, root, branch);
  }

  public BlockPos mouth() {
    return mouth;
  }

  public Rotation rotation() {
    return rotation;
  }

  public long rootStation() {
    return rootStation;
  }

  public int generation() {
    return parent == null ? 0 : 1;
  }

  @Nullable
  public MineBranch branch() {
    return branch;
  }

  /** The parent-rib walk cell where this child shaft begins. */
  public BlockPos entry() {
    return walkCell(ENTRY_COLUMN);
  }

  /**
   * The extra headroom needed to step from the parent rib onto this shaft's
   * first descending stair. The entry is one block higher than that stair, so
   * a two-high opening measured from the lower floor is not enough while the
   * walker's body is still at entry height.
   */
  public BlockPos entranceClearance() {
    return mouth;
  }

  /** The work station's stable local key, reconstructed from its world mouth. */
  public static long rootStation(Building building, BlockPos mouth) {
    return mouth.subtract(BlockPos.of(building.getOriginLocation()))
        .rotate(inverse(building.getRotation())).asLong();
  }

  /**
   * Child anchor one block above and one block beyond the completed rib end.
   * Its first ramp walk cell is therefore exactly the rib's far-end cell.
   */
  public static BlockPos childMouth(BlockPos rootMouth, Rotation rootRotation,
      int sourceDepth, int side) {
    return childMouth(rootMouth, rootRotation, sourceDepth, side, RADIUS);
  }

  public static BlockPos childMouth(BlockPos rootMouth, Rotation rootRotation,
      int sourceDepth, int side, int radius) {
    int end = side * (radius + RIB_LENGTH + 1);
    int y = floorY(sourceDepth) + 1;
    return rootMouth.offset(new BlockPos(end, y, sourceDepth).rotate(rootRotation));
  }

  /** Rotation whose local +Z points outward along the chosen root rib. */
  public static Rotation outwardRotation(Rotation rootRotation, int side) {
    if (side != -1 && side != 1) {
      throw new IllegalArgumentException("A mine branch side must be -1 or 1");
    }
    int turn = side > 0 ? 3 : 1;
    return fromQuarterTurns((quarterTurns(rootRotation) + turn) % 4);
  }

  /**
   * Whether a cell in the mine's frame lies in the corridor the cursor digs:
   * {@link #RADIUS} to either side of the centre line, from the entrance
   * (local z) down and forward. The one definition of the shaft's shape;
   * MineStep digs to it, and the navigator reads it.
   */
  public static boolean withinCorridor(BlockPos local) {
    return withinCorridor(local, RADIUS);
  }

  public static boolean withinCorridor(BlockPos local, int radius) {
    int floorY = local.getZ() < 0 ? -1 : -(local.getZ() + 2);
    int topY = Math.min(floorY + RAMP_HEIGHT - 1, -1);
    return Math.abs(local.getX()) <= radius
        && local.getZ() >= ENTRY_COLUMN
        && local.getY() >= floorY
        && local.getY() <= topY;
  }

  /** A work or standing cell in one of the shaft's planned prospecting ribs. */
  public static boolean withinRib(BlockPos local) {
    return withinRib(local, RADIUS);
  }

  public static boolean withinRib(BlockPos local, int radius) {
    int z = local.getZ();
    if (z < RIB_MIN_LINE || z % RIB_PITCH != 0) {
      return false;
    }
    int ax = Math.abs(local.getX());
    if (ax < radius + 1 || ax > radius + RIB_LENGTH) {
      return false;
    }
    int floorY = z < 0 ? -1 : -(z + 2);
    return local.getY() >= floorY && local.getY() <= floorY + (RIB_HEIGHT - 1);
  }

  /**
   * Every position navigation should treat as belonging to this mine,
   * including the one-block transition between diagonal ramp steps.
   */
  public static boolean withinExcavation(BlockPos local) {
    return withinExcavation(local, RADIUS);
  }

  public static boolean withinExcavation(BlockPos local, int radius) {
    int z = local.getZ();
    int floorY = z < 0 ? -1 : -(z + 2);
    boolean betweenRampSteps = Math.abs(local.getX()) <= radius
        && z >= ENTRY_COLUMN
        && local.getY() == floorY - 1;
    return withinCorridor(local, radius) || withinRib(local, radius)
        || withinEntranceClearance(local) || betweenRampSteps;
  }

  /** The single non-standing cell that gives a descending entrance headroom. */
  public static boolean withinEntranceClearance(BlockPos local) {
    return local.equals(BlockPos.ZERO);
  }

  /**
   * Whether the feet have fallen beneath the planned ramp or a rib. Such a
   * position is horizontally inside the mine, but no longer connected to its
   * navigable volume. The ramp keeps one block of tolerance because a normal
   * diagonal step can cross the lower Y boundary just before it crosses into
   * the next forward column; that transition is still on the stair, not a cave
   * fall. A miner below that margin is recovered instead of being routed to a
   * waypoint overhead.
   */
  public static boolean belowExcavation(BlockPos local) {
    return belowExcavation(local, RADIUS);
  }

  public static boolean belowExcavation(BlockPos local, int radius) {
    int z = local.getZ();
    int floorY = z < 0 ? -1 : -(z + 2);
    boolean belowRamp = Math.abs(local.getX()) <= radius
        && z >= ENTRY_COLUMN
        && local.getY() < floorY - 1;
    int ax = Math.abs(local.getX());
    boolean belowRib = z >= RIB_MIN_LINE
        && z % RIB_PITCH == 0
        && ax >= radius + 1
        && ax <= radius + RIB_LENGTH
        && local.getY() < floorY;
    return belowRamp || belowRib;
  }

  /** Every shaft in the village: one per miner station of every mine standing. */
  public static List<MineShaft> of(Village village) {
    List<MineShaft> out = new ArrayList<>();
    for (Building building : village.getBuildings()) {
      out.addAll(of(building));
    }
    return out;
  }

  /** Root and child frames owned by one building, in root-then-children order. */
  public static List<MineShaft> of(@Nullable Building building) {
    BuildingInfo info = building == null ? null : building.getInfo();
    if (info == null) {
      return List.of();
    }
    List<MineShaft> out = new ArrayList<>();
    for (Map.Entry<Long, Occupation> station : info.getWorkLocations().entrySet()) {
      if (station.getValue() != Occupation.MINER) {
        continue;
      }
      BlockPos mouth = BlockPos.of(building.getOriginLocation())
          .offset(BlockPos.of(station.getKey()).rotate(building.getRotation()));
      MineShaft root = root(building, mouth);
      out.add(root);
      building.getMineBranches().stream()
          .filter(branch -> branch.rootStation() == station.getKey())
          .map(branch -> child(root, branch))
          .forEach(out::add);
    }
    return List.copyOf(out);
  }

  /**
   * Whether a child plan would remove a cell already owned by another planned
   * shaft. Its one intentional intersection with its parent, the far-end rib
   * entry, is allowed. The finite scan ends at the world's minimum build height
   * and includes ordinary ribs, not only central ramps.
   */
  public static boolean overlapsPlannedExcavation(MineShaft first, MineShaft second,
      int minimumBuildY) {
    if (first.generation() == 0) {
      throw new IllegalArgumentException("The candidate must be a child shaft");
    }
    HashSet<Long> occupied = new HashSet<>();
    plannedCells(first, minimumBuildY, cell -> occupied.add(cell.asLong()));
    boolean ownParent = second.generation() == 0
        && second.rootStation == first.rootStation
        && second.mouth.equals(first.parent.mouth);
    long allowedEntry = first.entry().asLong();
    final boolean[] overlap = {false};
    plannedCells(second, minimumBuildY, cell -> {
      long packed = cell.asLong();
      if (!overlap[0] && occupied.contains(packed)
          && !(ownParent && packed == allowedEntry)) {
        overlap[0] = true;
      }
    });
    return overlap[0];
  }

  /**
   * The next place to walk toward on the way from {@code from} to {@code to}
   * when either lies down a shaft of this village, or null when the ordinary
   * path will do: neither is in a shaft, or both are in the same one within a
   * hop of each other.
   */
  @Nullable
  public static BlockPos waypoint(Village village, BlockPos from, BlockPos to) {
    return waypoint(of(village), from, to);
  }

  /** Package-visible pure routing seam used by the geometry tests. */
  @Nullable
  static BlockPos waypoint(List<MineShaft> shafts, BlockPos from, BlockPos to) {
    for (MineShaft root : shafts) {
      if (root.parent != null) {
        continue;
      }
      List<MineShaft> children = shafts.stream()
          .filter(shaft -> shaft.parent == root)
          .toList();
      MineShaft fromChild = deepestContaining(children, from, true);
      MineShaft toChild = deepestContaining(children, to, false);
      boolean involved = fromChild != null || toChild != null
          || root.contains(from) || root.contains(to);
      if (!involved) {
        continue;
      }

      if (fromChild != null && fromChild != toChild) {
        BlockPos hop = fromChild.hop(from, fromChild.entry());
        if (hop != null) {
          return hop;
        }
        if (!from.equals(fromChild.entry())) {
          return fromChild.entry();
        }
      }

      if (toChild != null) {
        if (fromChild != toChild) {
          BlockPos hop = root.hop(from, toChild.entry());
          if (hop != null) {
            return hop;
          }
          if (!from.equals(toChild.entry())) {
            return toChild.entry();
          }
        }
        return toChild.hop(from, to);
      }
      return root.hop(from, to);
    }
    return null;
  }

  /** Whether a world position has fallen below any planned shaft in this village. */
  public static boolean belowExcavation(Village village, BlockPos world) {
    return belowExcavation(of(village), world);
  }

  /** Package-visible pure fall-classification seam used by geometry tests. */
  static boolean belowExcavation(List<MineShaft> shafts, BlockPos world) {
    if (shafts.stream().anyMatch(shaft -> shaft.contains(world))) {
      return false;
    }
    for (MineShaft shaft : shafts) {
      if (belowExcavation(shaft.local(world), shaft.radius)) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  private BlockPos hop(BlockPos from, BlockPos to) {
    BlockPos localFrom = local(from);
    BlockPos localTo = local(to);
    boolean fromIn = withinExcavation(localFrom, radius);
    // The root mouth is also the surface work-station anchor. Its clearance
    // belongs to excavation, but approaching that station from outside must
    // not require walking into the first stair before it has been dug.
    if (this.parent == null && !fromIn && localTo.equals(BlockPos.ZERO)) {
      return null;
    }
    boolean toIn = withinExcavation(localTo, radius);
    if (!fromIn && !toIn) {
      return null;
    }
    if (fromIn && toIn) {
      // A child entrance sits at the far end of its parent rib. Treat that
      // horizontal connection like the ramp itself: one adjacent, known-open
      // footing at a time. Looking only at forward (z) distance made the two
      // points appear colocated in the shaft frame, so vanilla was asked to
      // solve the whole ten-block rib in one path. It can reject that exact
      // route even though every planned footing is open, leaving the miner to
      // repeat the child target from the village center forever.
      if (localFrom.getZ() == localTo.getZ()
          && (withinRib(localFrom, radius) || withinRib(localTo, radius))
          && Math.abs(localTo.getX() - localFrom.getX()) > HOP) {
        return ribWalkCell(localFrom, localTo.getX());
      }
      int ahead = localTo.getZ() - localFrom.getZ();
      if (Math.abs(ahead) <= HOP) {
        // The last diagonal hop can leave the walker one level and one lateral
        // cell from the source line. The target is still far down the rib, so
        // first land on the ramp's centre doorway; the next request will begin
        // the adjacent horizontal hops above.
        if (withinRib(localTo, radius)
            && Math.abs(localTo.getX() - localFrom.getX()) > HOP) {
          return walkCell(localTo.getZ());
        }
        return null; // the ordinary path reaches it
      }
      return walkCell(localFrom.getZ() + (ahead > 0 ? HOP : -HOP));
    }
    if (fromIn) {
      if (withinRib(localFrom, radius)) {
        // First leave a rib through its doorway. Treating every non-corridor
        // target as outside the mine sent a miner UP the ramp while their work
        // waited four blocks farther along this same branch.
        return ribWalkCell(localFrom, 0);
      }
      // Climbing out: up the ramp a hop at a time, and within a hop of the mouth
      // hand back to the ordinary path so it climbs the last steps out to the real
      // target. Returning this.mouth here pinned anyone standing below the mouth:
      // the walk to the mouth is a no-op once they reach it, and the goal's real
      // up-top target (a bed, the day's work) never got a path, so a villager who
      // wandered onto the ramp could never climb back out of the shaft.
      int z = localFrom.getZ() - HOP;
      return z <= ENTRY_COLUMN ? null : walkCell(z);
    }
    // Going in: the first walk cell first, from wherever they are, then down
    // the ramp. The work-station anchor itself can be open over the entrance;
    // asking for that exact block made an accuracy-zero path impossible even
    // though the real stair one block behind and below was sound.
    if (from.distSqr(this.mouth) > AT_MOUTH_SQR) {
      return entry();
    }
    return walkCell(Math.min(localTo.getZ(), ENTRY_COLUMN + HOP));
  }

  /** The cell a walker's feet occupy on the ramp at column {@code z}, in the world. */
  private BlockPos walkCell(int z) {
    int column = Math.max(z, ENTRY_COLUMN);
    int y = column < 0 ? -1 : -(column + 2);
    return this.mouth.offset(new BlockPos(0, y, column).rotate(this.rotation));
  }

  /** The adjacent supported footing along a rib toward {@code targetX}. */
  private BlockPos ribWalkCell(BlockPos localFrom, int targetX) {
    int step = Integer.compare(targetX, localFrom.getX());
    int nextX = localFrom.getX() + step * HOP;
    int y = floorY(localFrom.getZ());
    return this.mouth.offset(new BlockPos(nextX, y, localFrom.getZ()).rotate(this.rotation));
  }

  /** A world position in the mine's frame: the mouth at the origin, forward along +z. */
  private BlockPos local(BlockPos world) {
    return world.subtract(this.mouth).rotate(inverse(this.rotation));
  }

  private static Rotation inverse(Rotation rotation) {
    return switch (rotation) {
      case CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90;
      case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
      default -> rotation;
    };
  }

  private boolean contains(BlockPos world) {
    return withinExcavation(local(world), radius);
  }

  @Nullable
  private static MineShaft deepestContaining(List<MineShaft> children, BlockPos world,
      boolean leaving) {
    for (MineShaft child : children) {
      BlockPos local = child.local(world);
      if (withinExcavation(local, child.radius) && (!leaving || local.getZ() >= 0)) {
        return child;
      }
    }
    return null;
  }

  private static int floorY(int z) {
    return z < 0 ? -1 : -(z + 2);
  }

  private static int quarterTurns(Rotation rotation) {
    return switch (rotation) {
      case NONE -> 0;
      case CLOCKWISE_90 -> 1;
      case CLOCKWISE_180 -> 2;
      case COUNTERCLOCKWISE_90 -> 3;
    };
  }

  private static Rotation fromQuarterTurns(int turns) {
    return switch (turns) {
      case 0 -> Rotation.NONE;
      case 1 -> Rotation.CLOCKWISE_90;
      case 2 -> Rotation.CLOCKWISE_180;
      case 3 -> Rotation.COUNTERCLOCKWISE_90;
      default -> throw new IllegalArgumentException("Quarter turns must be in [0, 3]");
    };
  }

  private static void plannedCells(MineShaft shaft, int minimumBuildY,
      Consumer<BlockPos> consumer) {
    if (shaft.mouth.getY() >= minimumBuildY) {
      consumer.accept(shaft.entranceClearance());
    }
    int deepest = Math.max(0, shaft.mouth.getY() - minimumBuildY + 2);
    for (int z = ENTRY_COLUMN; z <= deepest; z++) {
      int floor = floorY(z);
      int ceiling = Math.min(floor + RAMP_HEIGHT - 1, -1);
      for (int y = floor; y <= ceiling; y++) {
        for (int x = -shaft.radius; x <= shaft.radius; x++) {
          BlockPos world = shaft.mouth.offset(new BlockPos(x, y, z).rotate(shaft.rotation));
          if (world.getY() >= minimumBuildY) {
            consumer.accept(world);
          }
        }
      }
      if (z < RIB_MIN_LINE || z % RIB_PITCH != 0) {
        continue;
      }
      for (int side = 1; side >= -1; side -= 2) {
        for (int step = 1; step <= RIB_LENGTH; step++) {
          int x = side * (shaft.radius + step);
          for (int y = floor; y < floor + RIB_HEIGHT; y++) {
            BlockPos world = shaft.mouth.offset(new BlockPos(x, y, z).rotate(shaft.rotation));
            if (world.getY() >= minimumBuildY) {
              consumer.accept(world);
            }
          }
        }
      }
    }
  }
}
