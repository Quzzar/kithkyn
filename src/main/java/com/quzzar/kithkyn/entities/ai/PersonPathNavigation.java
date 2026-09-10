package com.quzzar.kithkyn.entities.ai;

import java.util.Set;

import javax.annotation.Nullable;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.village.buildings.MineShaft;
import com.quzzar.kithkyn.village.buildings.WorkerFooting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Ground navigation for a person, including village doors, ladders and mine ramps.
 *
 * <b>Closed fence gates are closed wooden doors.</b> Vanilla reads a closed
 * gate as a fence, so no mob ever plans a route through one: a butchery pen
 * with two gates and a door was, to its own butcher, a yard with one exit, and
 * every trip to the storehouse three blocks beyond the fence became a
 * thirty-block walk out through the building and round it, which his
 * pathfinder could not see the end of. He stood at the fence until the loop
 * gave up on him. A closed gate is now a node the route may pass through at
 * the door's cost, with the opening left to
 * {@link com.quzzar.kithkyn.entities.ai.goals.OpenFenceGateGoal} when they
 * reach it, and the door rules (no diagonal moves through it) apply to gates
 * unchanged because they hang off the node type, not the block.
 *
 * <b>Ladders are places to be.</b> Vanilla mobs can climb - a zombie pressed
 * against a ladder goes up - but never plan to: the search only ever looks
 * sideways, one step up, and down a drop, so a ladder shaft is invisible to it
 * and a watchtower's bed at the top of one was a bed nobody could reach. Here
 * a rung (anything in the climbable tag) is a node the feet can stand in, its
 * floor is its own height rather than whatever lies under the ladder, and it
 * is joined to the rungs above and below it. Reaching the top is vanilla's
 * step-up onto the landing; leaving from the top is vanilla's one-block drop
 * into the top rung. What vanilla will not do on its own is the climb itself
 * with nobody pressing into the wall, so {@link #tick()} supplies the vanilla
 * climbing speed while the next node is straight up, and holds the walk still
 * meanwhile: walking into the wall is what vanilla reads as "climb up", which
 * is the wrong answer on the way down.
 *
 * <b>Route sight is not eyesight.</b> Vanilla uses the follow-range attribute
 * both for sensing and as the maximum distance a path search may expand. That
 * made ordinary routes end after about twenty blocks even though the search
 * still had nodes left. People search at least forty-eight blocks instead,
 * while their genetically varied follow range remains their perception range.
 * The vanilla node ceiling still bounds the work, and its navigation region
 * still substitutes empty chunks rather than loading missing ones.
 *
 * <b>A mine shaft is walked by its ramp.</b> A long vertical target also makes
 * vanilla's best partial path end at the surface directly above it. Any route
 * that starts or ends down a shaft is therefore routed by the ramp a hop at a
 * time ({@link MineShaft#waypoint}), under {@code moveTo}, so every goal gets
 * it. Child shafts use the same hook, with the root ramp and source rib as an
 * explicit parent chain.
 */
public final class PersonPathNavigation extends GroundPathNavigation {

  /** Vanilla's climb, which LivingEntity grants only to a mob pressing into the ladder. */
  private static final double CLIMB_SPEED = 0.2D;

  /** Maximum horizontal error before committing to a ladder descent. */
  private static final double MAX_LADDER_OFFSET = 0.025D;

  /** Bounded route-planning horizon, independent of a person's perception range. */
  private static final float MINIMUM_SEARCH_RANGE = 48.0F;

  /** Exact work posts can require a detour to a ladder before climbing back toward the target. */
  private static final float EXACT_SEARCH_RANGE = 96.0F;

  /** A one-step ramp waypoint must not accept the current cell as close enough. */
  private static final int MINE_WAYPOINT_ACCURACY = 0;

  /** Hand-access stances need a closer finish than vanilla's broad waypoint tolerance. */
  private static final double FINAL_POSITION_TOLERANCE = 0.04D;

  /** Squared distance of one diagonal stair step in the mine. */
  private static final double ADJACENT_MINE_STEP_SQR = 3.0D;

  @Nullable
  private String lastMinePathFailure;

  public PersonPathNavigation(Mob mob, Level level) {
    super(mob, level);
  }

  @Override
  protected PathFinder createPathFinder(int maxVisitedNodes) {
    PersonNodeEvaluator evaluator = new PersonNodeEvaluator();
    evaluator.setCanPassDoors(true);
    this.nodeEvaluator = evaluator;
    return new MeasuredPathFinder(evaluator, maxVisitedNodes);
  }

  /**
   * Keep perception local without forcing route planning to stop at the same
   * radius. The five-argument vanilla implementation retains both its node
   * budget and its loaded-chunk-only {@link PathNavigationRegion}.
   */
  @Override
  @Nullable
  protected Path createPath(Set<BlockPos> targets, int regionOffset, boolean offsetUpward, int accuracy) {
    float perceptionRange = (float)this.mob.getAttributeValue(Attributes.FOLLOW_RANGE);
    float range = Math.max(MINIMUM_SEARCH_RANGE, perceptionRange);
    Path route = super.createPath(targets, regionOffset, offsetUpward, accuracy, range);
    // Reaching a watch platform can take more than 48 blocks of walking even
    // when it is nearby in a straight line. Retry exact work destinations with
    // a longer horizon, retaining the same hard node budget and loaded chunks.
    if (accuracy == 0 && range < EXACT_SEARCH_RANGE && (route == null || !route.canReach())) {
      Path longer = super.createPath(targets, regionOffset, offsetUpward, accuracy, EXACT_SEARCH_RANGE);
      if (longer != null && (route == null || longer.canReach()
          || longer.getDistToTarget() < route.getDistToTarget())) route = longer;
    }
    return route;
  }

  /**
   * The path to {@code pos}, or to the next ramp waypoint on the way when this
   * walk starts or ends down one of the village's mine shafts. The goal keeps
   * asking for its real target, and each answer is the next hop from wherever
   * the walker has got to, so the shaft is descended and climbed by the ramp.
   */
  @Override
  @Nullable
  public Path createPath(BlockPos pos, int accuracy) {
    if (this.mob instanceof RealPerson person && person.getVillage() != null) {
      if (MineShaft.belowExcavation(person.getVillage(), this.mob.blockPosition())) {
        Kithkyn.LOGGER.info(
            "[mine] {} fell below the planned shaft and has been brought back to the village center",
            person.getFullName());
        person.tpToHome();
        return null;
      }
      BlockPos hop = MineShaft.waypoint(person.getVillage(), this.mob.blockPosition(), pos);
      if (hop != null) {
        Path path = super.createPath(hop, MINE_WAYPOINT_ACCURACY);
        if (path == null || !path.canReach()) {
          BlockState at = this.level.getBlockState(hop);
          BlockState above = this.level.getBlockState(hop.above());
          BlockState below = this.level.getBlockState(hop.below());
          String failure = this.mob.blockPosition().toShortString() + " toward "
              + pos.toShortString() + " via " + hop.toShortString() + ": path="
              + (path == null ? "none" : "partial to "
                  + path.getEndNode().asBlockPos().toShortString())
              + ", at=" + at.getBlock().getName().getString()
              + ", above=" + above.getBlock().getName().getString()
              + ", below=" + below.getBlock().getName().getString();
          if (!failure.equals(this.lastMinePathFailure)) {
            this.lastMinePathFailure = failure;
            Kithkyn.LOGGER.info("[mine-path] {} cannot route from {}",
                person.getFullName(), failure);
          }
        } else {
          this.lastMinePathFailure = null;
        }
        // A partial route to a mine waypoint is actively harmful: vanilla's
        // closest node is commonly the solid roof directly above the ramp.
        // Walking that partial path strands the miner over her own work and the
        // next hop is then an even steeper drop through stone. A waypoint is a
        // required link in the shaft route, so unlike an ordinary destination it
        // is useful only when the pathfinder can actually reach it.
        return path != null && path.canReach() ? path : null;
      }
    }
    BlockState target = this.level.getBlockState(pos);
    if (canOpenDoors() && target.getBlock() instanceof DoorBlock door
        && door.type().canOpenByHand() && target.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
      // A doorway can be the final approach to a closet container. Ground
      // navigation otherwise lifts this solid target above the door and roof.
      return super.createPath(Set.of(pos), accuracy);
    }
    return super.createPath(pos, accuracy);
  }

  /**
   * Route coordinate-based movement through the same block-position entry point.
   * PathNavigation's vanilla implementation calls its protected set-based path
   * search directly, bypassing {@link #createPath(BlockPos, int)} and therefore
   * every mine waypoint above. WorkLoopGoal uses this overload, so without this
   * bridge miners appeared to have waypoint routing while their actual work walk
   * still followed vanilla's partial path onto the roof over a deep target.
   */
  @Override
  public boolean moveTo(double x, double y, double z, double speed) {
    BlockPos requested = BlockPos.containing(x, y, z);
    if (this.mob instanceof RealPerson person && person.getVillage() != null) {
      BlockPos hop = MineShaft.waypoint(
          person.getVillage(), this.mob.blockPosition(), requested);
      if (hop != null
          && this.mob.blockPosition().distSqr(hop) <= ADJACENT_MINE_STEP_SQR
          && isOpenFooting(hop)) {
        // Vanilla occasionally refuses an exact path to the next one-down ramp
        // cell while the walker is between stair heights. This is already the
        // validated adjacent waypoint, so keep collision handling and hand that
        // single step directly to the ordinary move control.
        this.path = null;
        this.lastMinePathFailure = null;
        this.mob.getMoveControl().setWantedPosition(
            hop.getX() + 0.5D, hop.getY(), hop.getZ() + 0.5D, speed);
        return true;
      }
    }
    Path path = createPath(requested, 1);
    return moveTo(path, speed);
  }

  private boolean isOpenFooting(BlockPos pos) {
    BlockState support = this.level.getBlockState(pos.below());
    return this.level.getBlockState(pos).isAir()
        && this.level.getBlockState(pos.above()).isAir()
        && support.entityCanStandOnFace(
            this.level, pos.below(), this.mob, net.minecraft.core.Direction.UP);
  }

  /** A climb is a path in progress, though the feet are off the ground. */
  @Override
  protected boolean canUpdatePath() {
    return super.canUpdatePath() || this.mob.onClimbable()
        || isClimbable(this.level.getBlockState(this.mob.blockPosition().below()));
  }

  /** A vertical rung must actually be reached, never skipped by ground corner-cutting. */
  @Override
  protected void followThePath() {
    if (this.path != null && !this.path.isDone()
        && isClimbable(this.level.getBlockState(this.path.getNextNodePos()))) {
      Vec3 next = this.path.getNextEntityPos(this.mob);
      if (!descendingTowards(next)) next = ladderApproach(next,
          this.level.getBlockState(this.path.getNextNodePos()));
      if (Math.abs(this.mob.getX() - next.x) < 0.45D
          && Math.abs(this.mob.getZ() - next.z) < 0.45D
          && Math.abs(this.mob.getY() - next.y) < 0.35D) this.path.advance();
      this.doStuckDetection(this.getTempMobPos());
      return;
    }
    if (this.path != null && !this.path.isDone() && this.path.canReach()
        && this.path.getNextNodeIndex() == this.path.getNodeCount() - 1
        && this.path.getNextNodePos().equals(this.path.getTarget())) {
      Vec3 destination = openPanelApproach(this.path.getNextEntityPos(this.mob));
      if (Math.abs(this.mob.getX() - destination.x) < FINAL_POSITION_TOLERANCE
          && Math.abs(this.mob.getZ() - destination.z) < FINAL_POSITION_TOLERANCE
          && Math.abs(this.mob.getY() - getGroundY(destination)) < 0.51D) this.path.advance();
      this.doStuckDetection(this.getTempMobPos());
      return;
    }
    super.followThePath();
  }

  /** On a rung the feet go at the rung, not on the floor beneath the ladder. */
  @Override
  protected double getGroundY(Vec3 vec) {
    return isClimbable(this.level.getBlockState(BlockPos.containing(vec))) ? vec.y : super.getGroundY(vec);
  }

  @Override
  public void tick() {
    super.tick();
    if (this.path == null || this.isDone()) {
      return;
    }
    Vec3 next = this.path.getNextEntityPos(this.mob);
    Vec3 doorway = openPanelApproach(next);
    if (!doorway.equals(next)) {
      this.mob.getMoveControl().setWantedPosition(
          doorway.x, getGroundY(doorway), doorway.z, this.speedModifier);
      return;
    }
    boolean onLadder = this.mob.onClimbable();
    boolean descending = descendingTowards(next);
    BlockPos rungPos = this.path.getNextNodePos();
    if (descending && onLadder && next.y < this.mob.getY() - 0.5D
        && isClimbable(this.level.getBlockState(this.mob.blockPosition().below()))) {
      // Do not step sideways toward the ground exit while still several rungs up.
      rungPos = this.mob.blockPosition();
      next = new Vec3(rungPos.getX() + 0.5D, next.y, rungPos.getZ() + 0.5D);
    }
    BlockState rungState = this.level.getBlockState(rungPos);
    boolean ladder = rungState.getBlock() instanceof LadderBlock;
    boolean descendingLadder = descending && ladder;
    next = ladderApproach(next, rungState);
    double dx = next.x - this.mob.getX();
    double dz = next.z - this.mob.getZ();
    // Merely sharing the rung's block is not enough: the trailing half of the
    // body can still rest on the landing. Center fully before stopping the walk,
    // or a descending resident hangs at the top forever without ever falling.
    double clearance = Math.max(0.0D, (1.0D - this.mob.getBbWidth()) / 2.0D - 0.025D);
    double alignment = descending ? Math.min(MAX_LADDER_OFFSET, clearance) : 0.4D;
    if (Math.abs(dx) > alignment || Math.abs(dz) > alignment) {
      if (ladder) {
        this.mob.getMoveControl().setWantedPosition(next.x, descending ? this.mob.getY() : next.y,
            next.z, this.speedModifier);
      }
      return; // leaving the ladder sideways is the move control's ordinary walk
    }
    if (!onLadder && !isClimbable(this.level.getBlockState(this.path.getNextNodePos()))) {
      return;
    }
    // A rung straight above or below: hold the walk still and let the ladder
    // do it. Pointed at the feet, the move control asks for no stride at all,
    // which matters more than it looks. The first cut held only while on the
    // ladder, and a person hovering in the air cell just over the top rung
    // was not: the move control strode them into the shaft wall, vanilla read
    // the wall-press as "climb up" the moment they dipped into the rung, and
    // they bobbed at the ladder's top for good. Held from here, they simply
    // fall into the shaft and slide.
    this.mob.getMoveControl().setWantedPosition(this.mob.getX(), this.mob.getY(), this.mob.getZ(), 0.0D);
    if (descendingLadder) {
      Vec3 motion = this.mob.getDeltaMovement();
      this.mob.setDeltaMovement(0.0D, motion.y, 0.0D);
    }
    if (onLadder && next.y > this.mob.getY() + 0.05D) {
      Vec3 motion = this.mob.getDeltaMovement();
      this.mob.setDeltaMovement(motion.x, CLIMB_SPEED, motion.z);
    }
    // Downward needs nothing: a body on a ladder slides at the ladder's own rate.
  }

  private static boolean isClimbable(BlockState state) {
    return state.is(BlockTags.CLIMBABLE);
  }

  /** Keep a wide body inside the opening until its trailing edge clears an open door or trapdoor. */
  private Vec3 openPanelApproach(Vec3 target) {
    int nextIndex = this.path.getNextNodeIndex();
    // The path advances before the whole body leaves a doorway. Retain its clearance
    // for recent nodes too, or the next centered step steers back into the leaf.
    for (int i = Math.max(0, nextIndex - 2); i <= nextIndex; i++) {
      BlockPos pos = this.path.getNodePos(i);
      BlockState state = this.level.getBlockState(pos);
      boolean door = state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.OPEN);
      boolean trapdoor = state.getBlock() instanceof TrapDoorBlock && state.getValue(TrapDoorBlock.OPEN);
      if (!door && !trapdoor) continue;
      if (door && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) pos = pos.below();
      if (i != nextIndex && !this.mob.getBoundingBox().inflate(0.05D, 0.0D, 0.05D)
          .intersects(new AABB(pos).expandTowards(0.0D, door ? 1.0D : 0.0D, 0.0D))) continue;
      var shape = state.getCollisionShape(this.level, pos);
      if (shape.isEmpty()) continue;
      Vec3 offset = openPanelOffset(shape.bounds(), this.mob.getBbWidth());
      if (offset.equals(Vec3.ZERO)) continue;
      return offset.x != 0.0D ? new Vec3(pos.getX() + 0.5D + offset.x, target.y, target.z)
          : new Vec3(target.x, target.y, pos.getZ() + 0.5D + offset.z);
    }
    return target;
  }

  /** Planning and movement use the same body center inside an open panel's remaining aperture. */
  private static Vec3 openPanelOffset(AABB leaf, double width) {
    boolean thinX = leaf.getXsize() < leaf.getZsize();
    double leafMin = thinX ? leaf.minX : leaf.minZ;
    double leafMax = thinX ? leaf.maxX : leaf.maxZ;
    double openingMin = leafMin < 0.5D ? leafMax : 0.0D;
    double openingMax = leafMin < 0.5D ? 1.0D : leafMin;
    double radius = width / 2.0D;
    if (0.5D - radius >= openingMin + 0.01D && 0.5D + radius <= openingMax - 0.01D) return Vec3.ZERO;
    double offset = (openingMin + openingMax) / 2.0D - 0.5D;
    return thinX ? new Vec3(offset, 0, 0) : new Vec3(0, 0, offset);
  }

  private static boolean isOpenPanel(BlockState state) {
    return state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.OPEN)
        || state.getBlock() instanceof TrapDoorBlock && state.getValue(TrapDoorBlock.OPEN);
  }

  private Vec3 ladderApproach(Vec3 target, BlockState state) {
    if (!(state.getBlock() instanceof LadderBlock)) return target;
    // Clear the three-pixel ladder lip both when entering from the side and
    // when descending. Entry movement and arrival agree on the approach point;
    // descent still requires the body to pass over the rung before advancing.
    var facing = state.getValue(LadderBlock.FACING);
    double offset = Math.max(0.125D, this.mob.getBbWidth() / 2.0D - 0.2625D);
    return target.add(facing.getStepX() * offset, 0, facing.getStepZ() * offset);
  }

  private boolean descendingTowards(Vec3 next) {
    boolean upcomingAscent = this.path.getNextNodeIndex() + 1 < this.path.getNodeCount()
        && this.path.getNode(this.path.getNextNodeIndex() + 1).y > next.y;
    return !upcomingAscent && next.y < this.mob.getY() - 0.05D;
  }

  /** Debug-only measurements around the stock weighted A* search. */
  private static final class MeasuredPathFinder extends PathFinder {

    private final PersonNodeEvaluator evaluator;
    private final int baseMaxVisitedNodes;

    private MeasuredPathFinder(PersonNodeEvaluator evaluator, int maxVisitedNodes) {
      super(evaluator, maxVisitedNodes);
      this.evaluator = evaluator;
      this.baseMaxVisitedNodes = maxVisitedNodes;
    }

    @Override
    @Nullable
    public Path findPath(PathNavigationRegion region, Mob mob, Set<BlockPos> targets, float maxRange,
        int accuracy, float searchDepthMultiplier) {
      if (!Kithkyn.LOGGER.isDebugEnabled()) {
        return super.findPath(region, mob, targets, maxRange, accuracy, searchDepthMultiplier);
      }

      this.evaluator.resetSearchMetrics();
      long started = System.nanoTime();
      Path result = super.findPath(region, mob, targets, maxRange, accuracy, searchDepthMultiplier);
      long elapsedMicros = (System.nanoTime() - started) / 1_000L;
      int expandedNodes = this.evaluator.expandedNodeCount();
      int nodeLimit = (int)(this.baseMaxVisitedNodes * searchDepthMultiplier);
      boolean nodeLimitHit = expandedNodes >= nodeLimit - 1;
      Node end = result == null ? null : result.getEndNode();
      String outcome = result == null ? "none" : result.canReach() ? "reached" : "partial";

      Kithkyn.LOGGER.debug(
          "[path] {} at {} to {}: {}, end {}, remaining {}, path nodes {}, expanded {}/{}, discovered {}, "
              + "{} us, range {}, accuracy {}, node limit hit {}",
          mob.getName().getString(), mob.blockPosition(), targets, outcome,
          end == null ? null : end.asBlockPos(), result == null ? null : result.getDistToTarget(),
          result == null ? 0 : result.getNodeCount(), expandedNodes, nodeLimit,
          this.evaluator.discoveredNodeCount(), elapsedMicros, maxRange, accuracy, nodeLimitHit);
      return result;
    }
  }

  /** Vanilla's walking evaluator, with gates read as doors and rungs as ground. */
  private static final class PersonNodeEvaluator extends WalkNodeEvaluator {

    private int expandedNodeCount;

    @Override
    public void prepare(PathNavigationRegion level, Mob mob) {
      super.prepare(level, mob);
      // Vanilla floor(height + 1) asks for an extra block at integer heights.
      // A one- or two-block body fits exactly that many clear block cells.
      this.entityHeight = (int)Math.ceil(mob.getBbHeight());
    }

    private void resetSearchMetrics() {
      this.expandedNodeCount = 0;
    }

    private int discoveredNodeCount() {
      return this.nodes.size();
    }

    private int expandedNodeCount() {
      return this.expandedNodeCount;
    }

    @Override
    public Node getStart() {
      // Mid-climb the feet are off the ground, and vanilla would start the
      // route from the floor under the ladder: a person re-planning halfway up
      // would be walked back down to begin again. The cell just over the top
      // rung counts too: that is where a person stands for a tick between the
      // landing and the ladder.
      BlockPos feet = this.mob.blockPosition();
      if (this.mob.onClimbable()) {
        return this.getStartNode(feet);
      }
      if (!this.mob.onGround() && isClimbable(this.currentContext.getBlockState(feet.below()))) {
        return this.getStartNode(feet.below());
      }
      return super.getStart();
    }

    @Override
    public PathType getPathType(PathfindingContext context, int x, int y, int z) {
      PathType type = super.getPathType(context, x, y, z);
      BlockState state = context.getBlockState(new BlockPos(x, y, z));
      if (type == PathType.FENCE && state.getBlock() instanceof FenceGateBlock) {
        return PathType.DOOR_WOOD_CLOSED;
      }
      if (type == PathType.OPEN && isClimbable(state)) {
        return PathType.WALKABLE; // a rung is somewhere the feet can be
      }
      return type;
    }

    /** Whole-cell ceiling checks overestimate a body standing below its integer path node. */
    @Override
    public PathType getPathTypeOfMob(PathfindingContext context, int x, int y, int z, Mob mob) {
      PathType type = super.getPathTypeOfMob(context, x, y, z, mob);
      if (type != PathType.BLOCKED || getPathType(context, x, y, z) != PathType.WALKABLE) return type;
      Vec3 standing = WorkerFooting.standingPosition(mob, new BlockPos(x, y, z));
      if (standing == null || standing.y >= y) return type;
      // Only replace ordinary solid-cell rejection. Gates, rails and hazards keep
      // their existing rules even when their collision shape leaves physical room.
      for (PathType part : getPathTypeWithinMobBB(context, x, y, z)) {
        if (part != PathType.OPEN && part != PathType.WALKABLE && part != PathType.BLOCKED) return type;
      }
      return PathType.WALKABLE;
    }

    /** A half-step needs its actual rise, not the full extra block vanilla reserves for jumping. */
    @Override
    @Nullable
    protected Node findAcceptedNode(int x, int y, int z, int verticalDeltaLimit,
        double nodeFloorLevel, Direction direction, PathType previousType) {
      Node result = super.findAcceptedNode(x, y, z, verticalDeltaLimit, nodeFloorLevel, direction, previousType);
      if ((result != null && result.costMalus >= 0.0F) || verticalDeltaLimit != 0
          || getCachedPathType(x, y, z) != PathType.BLOCKED) return result;
      BlockPos step = new BlockPos(x, y + 1, z);
      Vec3 standing = WorkerFooting.standingPosition(this.mob, step);
      if (standing == null || standing.y <= nodeFloorLevel
          || standing.y - nodeFloorLevel > this.mob.maxUpStep()
          || getCachedPathType(x, y + 1, z) != PathType.WALKABLE) return result;
      // Retain vanilla's collision sweep above the starting column and its
      // destination checks. Only its whole-block jump admission is retried.
      return super.findAcceptedNode(x, y, z, 1, nodeFloorLevel, direction, previousType);
    }

    /** A rung's floor is the rung, not whatever is under the ladder. */
    @Override
    protected double getFloorLevel(BlockPos pos) {
      return isClimbable(this.currentContext.getBlockState(pos)) ? pos.getY() : super.getFloorLevel(pos);
    }

    @Override
    public int getNeighbors(Node[] outputArray, Node node) {
      this.expandedNodeCount++;
      int count = super.getNeighbors(outputArray, node);
      int clearCount = 0;
      for (int i = 0; i < count; i++) {
        Node neighbor = outputArray[i];
        if (!crossesOpenPanel(node, neighbor)) outputArray[clearCount++] = neighbor;
      }
      count = clearCount;
      if (!isClimbable(this.currentContext.getBlockState(new BlockPos(node.x, node.y, node.z)))) {
        return count;
      }
      // Vanilla may offer a fall to the ground beside a high rung. Taking that
      // edge leaves the ladder early and can strand the body on a nearby rail.
      // Descend the rungs first; ordinary same-height and one-step exits remain.
      int safeCount = 0;
      for (int i = 0; i < count; i++) {
        Node neighbor = outputArray[i];
        if (neighbor.y >= node.y - 1) outputArray[safeCount++] = neighbor;
      }
      count = safeCount;
      for (int step : new int[] {1, -1}) {
        Node rung = rung(node.x, node.y + step, node.z);
        if (this.isNeighborValid(rung, node)) {
          outputArray[count++] = rung;
        }
      }
      return count;
    }

    /** An open leaf is passable along its aperture, but still blocks transverse approaches. */
    private boolean crossesOpenPanel(Node from, Node to) {
      BlockPos start = from.asBlockPos();
      BlockPos end = to.asBlockPos();
      BlockState startState = this.currentContext.getBlockState(start);
      BlockState endState = this.currentContext.getBlockState(end);
      if (!isOpenPanel(startState) && !isOpenPanel(endState)) return false;
      Vec3 startCenter = panelNodeCenter(start, startState);
      Vec3 endCenter = panelNodeCenter(end, endState);
      return intersectsPanel(startCenter, endCenter, start, startState)
          || intersectsPanel(startCenter, endCenter, end, endState);
    }

    private Vec3 panelNodeCenter(BlockPos position, BlockState state) {
      Vec3 center = new Vec3(position.getX() + 0.5D, getFloorLevel(position), position.getZ() + 0.5D);
      if (!isOpenPanel(state)) return center;
      var shape = state.getCollisionShape(this.currentContext.level(), position);
      return shape.isEmpty() ? center : center.add(openPanelOffset(shape.bounds(), this.mob.getBbWidth()));
    }

    private boolean intersectsPanel(Vec3 start, Vec3 end, BlockPos position, BlockState state) {
      if (!isOpenPanel(state)) return false;
      var shape = state.getCollisionShape(this.currentContext.level(), position);
      if (shape.isEmpty()) return false;
      AABB leaf = shape.bounds().move(position);
      if (state.getBlock() instanceof DoorBlock) {
        leaf = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
            ? leaf.expandTowards(0, 1, 0) : leaf.expandTowards(0, -1, 0);
      }
      // Sweep the body's center against a body-expanded leaf, so turning into
      // the opening is permitted when every point along that movement clears it.
      double radius = this.mob.getBbWidth() / 2.0D;
      AABB obstruction = new AABB(leaf.minX - radius, leaf.minY - this.mob.getBbHeight(), leaf.minZ - radius,
          leaf.maxX + radius, leaf.maxY, leaf.maxZ + radius).deflate(0.000001D);
      return obstruction.contains(start) || obstruction.contains(end) || obstruction.clip(start, end).isPresent();
    }

    /** The rung at this cell as a node, or null where there is none or no room above it. */
    @Nullable
    private Node rung(int x, int y, int z) {
      BlockPos pos = new BlockPos(x, y, z);
      if (!isClimbable(this.currentContext.getBlockState(pos))) {
        return null;
      }
      // A closed trapdoor over the top rung is a lid, not a hatch; the mob
      // type check below reads any trapdoor as passable.
      BlockState above = this.currentContext.getBlockState(pos.above());
      if (above.getBlock() instanceof TrapDoorBlock && !above.getValue(TrapDoorBlock.OPEN)) {
        return null;
      }
      PathType type = this.getCachedPathType(x, y, z);
      float malus = this.mob.getPathfindingMalus(type);
      if (malus < 0.0F) {
        return null; // no head room, or something worse in the cell
      }
      Node rung = this.getNode(x, y, z);
      rung.type = type;
      rung.costMalus = Math.max(rung.costMalus, malus);
      return rung;
    }
  }
}
