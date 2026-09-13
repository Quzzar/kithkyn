package com.quzzar.kithkyn.entities.ai.goals.work;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.village.buildings.WorkerFooting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Shared standing positions and unobstructed hand access for containers, beds, and other activity blocks. */
public final class ContainerAccess {
  private static final double DOORWAY_BODY_WIDTH = 13.0D / 16.0D - 2.0D * 0.01D;
  private record Approach(BlockPos node, Vec3 feet) { }

  /** Places on a block a player can actually point at, including the exposed lower face of stacked storage. */
  private static final double[][] TARGET_POINTS = {
      {0.5D, 0.5D, 0.5D},
      {0.001D, 0.25D, 0.5D}, {0.999D, 0.25D, 0.5D},
      {0.5D, 0.25D, 0.001D}, {0.5D, 0.25D, 0.999D},
      {0.5D, 0.999D, 0.5D}, {0.5D, 0.001D, 0.5D}
  };

  private ContainerAccess() { }

  /** An exact route to hand access, possibly staged at a wooden doorway before walking opens it. */
  @Nullable
  public static BlockPos approachTo(RealPerson person, BlockPos target, double reachSqr) {
    return approachTo(person, target, reachSqr, position -> true);
  }

  /** The same physical approach search, restricted to standing positions allowed by the activity. */
  @Nullable
  public static BlockPos approachTo(RealPerson person, BlockPos target, double reachSqr,
      Predicate<BlockPos> allowedStanding) {
    if (!person.level().hasChunkAt(target)) return null;
    double reach = Math.sqrt(reachSqr);
    int radius = Mth.ceil(reach);
    double eyeHeight = person.getEyeHeight();
    double centerY = target.getY() + 0.5D;
    int bottom = Mth.ceil(centerY - eyeHeight - reach);
    int top = Mth.ceil(centerY - eyeHeight + reach);
    Map<BlockPos, Approach> candidates = new LinkedHashMap<>();
    for (BlockPos pos : BlockPos.betweenClosed(
        new BlockPos(target.getX() - radius, bottom, target.getZ() - radius),
        new BlockPos(target.getX() + radius, top, target.getZ() + radius))) {
      Vec3 feet = WorkerFooting.standingPosition(person, pos);
      if (feet != null && allowedStanding.test(BlockPos.containing(feet))
          && canReach(person, feet.add(0.0D, eyeHeight, 0.0D), target, reachSqr)) {
        BlockPos node = pos.immutable();
        candidates.put(node, new Approach(node, feet));
      }
    }
    // Ask one multi-target search. Planning each candidate separately consumed
    // the server-wide long-route budget inside one tick and could reject a
    // reachable stacked barrel merely because another shelf was checked first.
    Path path = candidates.isEmpty() ? null : person.getNavigation().createPath(candidates.keySet(), 0);
    if (path != null && path.canReach() && path.getEndNode() != null) {
      BlockPos end = path.getEndNode().asBlockPos();
      if (candidates.containsKey(end)) return end;
    }
    return closedClosetDoor(person, target, reachSqr, allowedStanding);
  }

  /** A one-cell closet has no inside stance until the ordinary door goal opens its wooden door. */
  @Nullable
  private static BlockPos closedClosetDoor(RealPerson person, BlockPos target, double reachSqr,
      Predicate<BlockPos> allowedStanding) {
    if (!(person.level().getBlockEntity(target) instanceof Container)
        || !(person.getNavigation() instanceof GroundPathNavigation ground) || !ground.canOpenDoors()
        || person.getBbHeight() > 2.0D || person.getBbWidth() > DOORWAY_BODY_WIDTH) return null;
    for (Direction outward : Direction.Plane.HORIZONTAL) {
      BlockPos position = target.relative(outward);
      if (!person.level().hasChunkAt(position)) continue;
      var state = person.level().getBlockState(position);
      if (!(state.getBlock() instanceof DoorBlock door) || !door.type().canOpenByHand()
          || state.getValue(DoorBlock.OPEN) || state.getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER
          || state.getValue(DoorBlock.FACING).getAxis() != outward.getAxis()
          || !allowedStanding.test(position)
          || !person.level().getBlockState(position.below()).isFaceSturdy(person.level(), position.below(), Direction.UP)) continue;
      var upper = person.level().getBlockState(position.above());
      if (upper.getBlock() != door || upper.getValue(DoorBlock.HALF) != DoubleBlockHalf.UPPER) continue;
      Vec3 eye = Vec3.atBottomCenterOf(position).add(0, person.getEyeHeight(), 0);
      if (eye.distanceToSqr(Vec3.atCenterOf(target)) > reachSqr) continue;
      // Cross only this adjacent door's cell for the planning ray. Actual interaction
      // still uses canReach from the villager's real eyes after OpenDoorGoal opens it.
      Vec3 beyond = eye.add(-outward.getStepX() * 0.500001D, 0, -outward.getStepZ() * 0.500001D);
      if (!canReach(person, beyond, target, reachSqr)) continue;
      var path = ground.createPath(position, 0);
      if (path != null && path.canReach() && path.getEndNode() != null
          && path.getEndNode().asBlockPos().equals(position)) return position;
    }
    return null;
  }

  /** Once walking opens a closet, continue to a real hand stance instead of its swung door leaf. */
  @Nullable
  public static BlockPos resolveOpenedDoor(RealPerson person, BlockPos target,
      @Nullable BlockPos currentApproach, double reachSqr) {
    if (currentApproach == null) return null;
    var state = person.level().getBlockState(currentApproach);
    if (!(state.getBlock() instanceof DoorBlock) || !state.getValue(DoorBlock.OPEN)) return currentApproach;
    BlockPos replacement = approachTo(person, target, reachSqr);
    return replacement == null ? currentApproach : replacement;
  }

  /** A visible point on the target must be within reach of the eyes without another block in the way. */
  public static boolean canReach(RealPerson person, Vec3 eye, BlockPos target, double reachSqr) {
    if (!person.level().hasChunkAt(target)) return false;
    for (double[] offset : TARGET_POINTS) {
      Vec3 point = new Vec3(target.getX() + offset[0], target.getY() + offset[1], target.getZ() + offset[2]);
      if (eye.distanceToSqr(point) > reachSqr) continue;
      var hit = person.level().clip(new ClipContext(eye, point,
          ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, person));
      if (hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(target)) return true;
    }
    return false;
  }
}
