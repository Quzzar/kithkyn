package com.quzzar.kithkyn.entities.ai;

import java.util.Set;

import javax.annotation.Nullable;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.village.buildings.MineShaft;
import com.quzzar.kithkyn.village.buildings.WorkerFooting;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
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
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.LanternBlock;
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
 * a ladder rung is a node the feet can stand in, its
 * floor is its own height rather than whatever lies under the ladder, and it
 * is joined to the rungs above and below it. Reaching the top is vanilla's
 * step-up onto the landing. The open cell immediately above a top rung is a
 * transition node, even when the body's edge still counts as grounded on the
 * landing, and it connects back down to that rung. What vanilla will not do
 * on its own is the climb itself with nobody pressing into the wall, so
 * {@link #tick()} supplies the vanilla
 * climbing speed while the next node is straight up, and holds the walk still
 * meanwhile: walking into the wall is what vanilla reads as "climb up", which
 * is the wrong answer on the way down.
 *
 * <b>Narrow decoration is not a floor.</b> Vanilla can join the collision tops
 * of a lantern and a fence into a path that a full-width body cannot actually
 * walk. People reject the tops of lanterns, candles, fences, gates and walls
 * as footing, so they use the real stair or ladder route through a building.
 *
 * <b>Route sight is not eyesight.</b> Vanilla uses the follow-range attribute
 * both for sensing and as the maximum distance a path search may expand. That
 * made ordinary routes end after about twenty blocks even though the search
 * still had nodes left. People search at least forty-eight blocks instead,
 * while their genetically varied follow range remains their perception range.
 * A fixed node ceiling still bounds the work, and its navigation region
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
  private static final float EXACT_SEARCH_RANGE = 128.0F;

  /** A one-step ramp waypoint must not accept the current cell as close enough. */
  private static final int MINE_WAYPOINT_ACCURACY = 0;

  /** Hand-access stances need a closer finish than vanilla's broad waypoint tolerance. */
  private static final double FINAL_POSITION_TOLERANCE = 0.04D;

  /** Squared distance of one diagonal stair step in the mine. */
  private static final double ADJACENT_MINE_STEP_SQR = 3.0D;

  /**
   * The server-thread time long retries may take, shared by every person in
   * every dimension: one thread runs all their path searches, and it is that
   * thread's ticks the budget keeps.
   */
  private static final LongRetryBudget LONG_RETRY_BUDGET = new LongRetryBudget();

  @Nullable
  private String lastMinePathFailure;

  /** The long retries that failed lately, so a stuck walker is not charged for them on every re-plan. */
  private final LongRetryMemo longRetry = new LongRetryMemo();

  public PersonPathNavigation(Mob mob, Level level) {
    super(mob, level);
  }

  /**
   * How far from where a person stands one ordinary path search can take them.
   * The search never expands a node further than this from its start, so a
   * target beyond it is only ever answered with a partial path, and asking costs
   * the whole node budget first. Callers choosing among many targets use this to
   * leave out the ones no single search can reach (#138).
   */
  public static float searchRange(Mob mob) {
    return Math.max(MINIMUM_SEARCH_RANGE, (float)mob.getAttributeValue(Attributes.FOLLOW_RANGE));
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
    float range = searchRange(this.mob);
    Path route = super.createPath(targets, regionOffset, offsetUpward, accuracy, range);
    // Reaching a watch platform can take more than 48 blocks of walking even
    // when it is nearby in a straight line. Retry exact work destinations with
    // a longer horizon and bounded extra search work, retaining loaded chunks.
    // A retry that just failed from here is not asked again (LongRetryMemo), and
    // one the shared budget cannot pay for now waits for a later re-plan
    // (LongRetryBudget). A mine shaft hop is an exact target, so it comes here too.
    long now = this.level.getGameTime();
    if (accuracy == 0 && range < EXACT_SEARCH_RANGE && (route == null || !route.canReach())
        && this.longRetry.worthRetrying(targets, this.mob.blockPosition(), now)) {
      if (!LONG_RETRY_BUDGET.admits(now, this.mob.getUUID())) {
        if (Kithkyn.LOGGER.isDebugEnabled()) {
          Kithkyn.LOGGER.debug("[path-budget] {} at {} to {}: long retry deferred",
              this.mob.getName().getString(), this.mob.blockPosition(), targets);
        }
        return route;
      }
      long started = System.nanoTime();
      Path longer = super.createPath(targets, regionOffset, offsetUpward, accuracy, EXACT_SEARCH_RANGE);
      LONG_RETRY_BUDGET.charge(now, System.nanoTime() - started);
      if (longer != null && longer.canReach()) {
        this.longRetry.reached(targets);
      } else {
        this.longRetry.failed(targets, this.mob.blockPosition(), now);
      }
      if (longer != null && (route == null || longer.canReach()
          || longer.getDistToTarget() < route.getDistToTarget())) route = longer;
    }
    return route;
  }

  private boolean isSolidWithoutCollision(BlockPos pos) {
    BlockState state = this.level.getBlockState(pos);
    return state.isSolid() && state.getCollisionShape(this.level, pos).isEmpty();
  }

  /**
   * The path to {@code pos}, or to the next ramp waypoint on the way when this
   * walk starts or ends down one of the village's mine shafts. The goal keeps
   * asking for its real target, and each answer is the next hop from wherever
   * the walker has got to, so the shaft is descended and climbed by the ramp.
   * A hop that lands in undug rock means the walker is not on the ramp at all
   * but in a cave or pocket the planned volume overlaps, and the ordinary path
   * is the only thing that can take them anywhere.
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
      if (!canUpdatePath()) {
        // Mid-air between two steps: vanilla answers nothing too, and a shaft hop
        // asked for now only logged a failure that was not one.
        return null;
      }
      BlockPos hop = MineShaft.waypoint(person.getVillage(), this.mob.blockPosition(), pos);
      if (hop != null && !openFooting(hop)) {
        // The plan says shaft, the world says rock. A guard who fell into a cave
        // pocket beside Zawiriko's ramp was held here for minutes (2026-09-11):
        // the hop through stone could never be reached, so every request got no
        // path at all. Off the ramp, the ordinary pathfinder decides.
        hop = null;
      }
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
    // Vanilla aims one block above any target flagged solid, and a sign or a banner is
    // flagged solid although it has no collision: a name plate beside a bed made the cell
    // in front of it unreachable, while a player walks straight into it. A cell open to
    // the body is the target as given.
    if (isSolidWithoutCollision(pos)) {
      return createPath(java.util.Set.of(pos), 8, false, accuracy);
    }
    return super.createPath(pos, accuracy);
  }

  /** Whether feet and head at {@code cell} are clear: a dug shaft cell rather than planned rock. */
  private boolean openFooting(BlockPos cell) {
    BlockPos head = cell.above();
    return this.level.getBlockState(cell).getCollisionShape(this.level, cell).isEmpty()
        && this.level.getBlockState(head).getCollisionShape(this.level, head).isEmpty();
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
        || isLadder(this.level.getBlockState(this.mob.blockPosition().below()));
  }

  /** A vertical rung must actually be reached, never skipped by ground corner-cutting. */
  @Override
  protected void followThePath() {
    if (this.path != null && !this.path.isDone()
        && isLadder(this.level.getBlockState(this.path.getNextNodePos()))) {
      Vec3 next = this.path.getNextEntityPos(this.mob);
      if (!descendingTowards(next)) next = ladderApproach(next,
          this.level.getBlockState(this.path.getNextNodePos()));
      if (Math.abs(this.mob.getX() - next.x) < 0.45D
          && Math.abs(this.mob.getZ() - next.z) < 0.45D
          // A climbing body's feet can oscillate anywhere inside the rung's
          // block. Occupying that exact rung is sufficient to advance toward
          // the next one; comparing its fractional height advances too early
          // near the top and can drop the body before it reaches the landing.
          && this.mob.blockPosition().equals(this.path.getNextNodePos())) this.path.advance();
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
    return isLadder(this.level.getBlockState(BlockPos.containing(vec))) ? vec.y : super.getGroundY(vec);
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
    boolean onLadder = isLadder(this.level.getBlockState(this.mob.blockPosition()));
    boolean descending = descendingTowards(next);
    BlockPos rungPos = this.path.getNextNodePos();
    if (descending && onLadder && next.y < this.mob.getY() - 0.5D
        && isLadder(this.level.getBlockState(this.mob.blockPosition().below()))) {
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
    if (!onLadder && !isLadder(this.level.getBlockState(this.path.getNextNodePos()))) {
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
      // Climbing straight up has no stride, so only the leftover velocity of the
      // approach moved the body sideways, and on a free-standing ladder that
      // carried climbers off the rung a few blocks up. Steer onto the rung's
      // approach point instead; shaft walls used to hide this on arid towers.
      this.mob.setDeltaMovement(Mth.clamp((next.x - this.mob.getX()) * 0.2D, -0.05D, 0.05D), CLIMB_SPEED,
          Mth.clamp((next.z - this.mob.getZ()) * 0.2D, -0.05D, 0.05D));
    }
    // Downward needs nothing: a body on a ladder slides at the ladder's own rate.
  }

  private static boolean isLadder(BlockState state) {
    return state.getBlock() instanceof LadderBlock;
  }

  /** Keep a wide body inside the opening until its trailing edge clears an open door or trapdoor. */
  private Vec3 openPanelApproach(Vec3 target) {
    int nextIndex = this.path.getNextNodeIndex();
    // The path advances before the whole body leaves a doorway. Retain its clearance
    // for recent nodes too, or the next centered step steers back into the leaf.
    for (int i = Math.max(0, nextIndex - 2); i <= nextIndex; i++) {
      BlockPos feet = this.path.getNodePos(i);
      for (int bodyY = 0; bodyY < Mth.ceil(this.mob.getBbHeight()); bodyY++) {
        BlockPos pos = feet.above(bodyY);
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
      // A nearby upper floor can require searching through several rooms before reaching
      // its staircase. Only the longer exact-target retry gets this bounded extra budget.
      float boundedMultiplier = accuracy == 0 && maxRange >= EXACT_SEARCH_RANGE
          ? searchDepthMultiplier * 2.0F : searchDepthMultiplier;
      if (!Kithkyn.LOGGER.isDebugEnabled()) {
        return super.findPath(region, mob, targets, maxRange, accuracy, boundedMultiplier);
      }

      this.evaluator.resetSearchMetrics();
      long started = System.nanoTime();
      Path result = super.findPath(region, mob, targets, maxRange, accuracy, boundedMultiplier);
      long elapsedMicros = (System.nanoTime() - started) / 1_000L;
      int expandedNodes = this.evaluator.expandedNodeCount();
      int nodeLimit = (int)(this.baseMaxVisitedNodes * boundedMultiplier);
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
      if (isLadder(this.currentContext.getBlockState(feet))) {
        return this.getStartNode(feet);
      }
      if (isLadderTransition(this.currentContext, feet)) {
        return this.getStartNode(feet);
      }
      return super.getStart();
    }

    @Override
    public PathType getPathType(PathfindingContext context, int x, int y, int z) {
      PathType type = super.getPathType(context, x, y, z);
      BlockState state = context.getBlockState(new BlockPos(x, y, z));
      // A closed horizontal panel is a floor or counter, not an aperture.
      // Vanilla admits TRAPDOOR nodes inside it, which routes workers through
      // market counters even when the roof prevents stepping onto them.
      if (type == PathType.TRAPDOOR && state.getBlock() instanceof TrapDoorBlock
          && !state.getValue(TrapDoorBlock.OPEN)) {
        return PathType.BLOCKED;
      }
      // A candle cluster is too low for vanilla to plan around and too tall to
      // step onto under an indoor ceiling, so a body walks into it and stays
      // there. It is an obstacle: people go round it.
      if (state.getBlock() instanceof CandleBlock) {
        return PathType.BLOCKED;
      }
      if (type == PathType.FENCE && state.getBlock() instanceof FenceGateBlock) {
        return PathType.DOOR_WOOD_CLOSED;
      }
      if (type == PathType.OPEN && isLadder(state)) {
        return PathType.WALKABLE; // a rung is somewhere the feet can be
      }
      if (type == PathType.OPEN && isLadderTransition(context, new BlockPos(x, y, z))) {
        return PathType.WALKABLE;
      }
      return type;
    }

    /** Whole-cell ceiling checks overestimate a body standing below its integer path node. */
    @Override
    public PathType getPathTypeOfMob(PathfindingContext context, int x, int y, int z, Mob mob) {
      PathType type = super.getPathTypeOfMob(context, x, y, z, mob);
      BlockPos position = new BlockPos(x, y, z);
      BlockState support = context.getBlockState(position.below());
      if (type == PathType.WALKABLE && (support.is(BlockTags.FENCES)
          || support.is(BlockTags.WALLS)
          || support.getBlock() instanceof FenceGateBlock
          || support.getBlock() instanceof LanternBlock
          || support.getBlock() instanceof CandleBlock)) {
        // Vanilla can chain narrow collision tops into a nominal shortcut,
        // such as stepping from a lantern onto a fence. A full villager body
        // cannot execute that route, so these decorative and barrier tops are
        // obstacles rather than floors. Stairs and slabs retain vanilla's
        // movement rules.
        return PathType.BLOCKED;
      }
      if (type != PathType.BLOCKED || getPathType(context, x, y, z) != PathType.WALKABLE) return type;
      Vec3 standing = WorkerFooting.standingPosition(mob, position);
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
      return isLadder(this.currentContext.getBlockState(pos))
          || isLadderTransition(this.currentContext, pos)
          ? pos.getY() : super.getFloorLevel(pos);
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
      BlockPos nodePos = new BlockPos(node.x, node.y, node.z);
      if (isLadderTransition(this.currentContext, nodePos)) {
        Node rung = rung(node.x, node.y - 1, node.z);
        if (this.isNeighborValid(rung, node)) outputArray[count++] = rung;
        return count;
      }
      if (isLadder(this.currentContext.getBlockState(nodePos))) {
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
      return count;
    }

    /** The open cell immediately above a ladder's top rung, where a climber crosses onto its landing. */
    private boolean isLadderTransition(PathfindingContext context, BlockPos pos) {
      return context.getBlockState(pos).getCollisionShape(context.level(), pos).isEmpty()
          && context.getBlockState(pos.below()).getBlock() instanceof LadderBlock;
    }

    /** An open leaf is passable along its aperture, but still blocks transverse approaches. */
    private boolean crossesOpenPanel(Node from, Node to) {
      BlockPos start = from.asBlockPos();
      BlockPos end = to.asBlockPos();
      Vec3 startCenter = panelNodeCenter(start);
      Vec3 endCenter = panelNodeCenter(end);
      return intersectsBodyPanel(startCenter, endCenter, start)
          || intersectsBodyPanel(startCenter, endCenter, end);
    }

    private Vec3 panelNodeCenter(BlockPos position) {
      Vec3 center = new Vec3(position.getX() + 0.5D, getFloorLevel(position), position.getZ() + 0.5D);
      for (int bodyY = 0; bodyY < this.entityHeight; bodyY++) {
        BlockPos body = position.above(bodyY);
        BlockState state = this.currentContext.getBlockState(body);
        if (!isOpenPanel(state)) continue;
        var shape = state.getCollisionShape(this.currentContext.level(), body);
        if (!shape.isEmpty()) return center.add(openPanelOffset(shape.bounds(), this.mob.getBbWidth()));
      }
      return center;
    }

    private boolean intersectsBodyPanel(Vec3 start, Vec3 end, BlockPos feet) {
      for (int bodyY = 0; bodyY < this.entityHeight; bodyY++) {
        BlockPos body = feet.above(bodyY);
        BlockState state = this.currentContext.getBlockState(body);
        if (intersectsPanel(start, end, body, state)) return true;
      }
      return false;
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
      if (!isLadder(this.currentContext.getBlockState(pos))) {
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
