package com.quzzar.kithkyn.entities.ai;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.FlyNodeEvaluator;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Flight planning for a village's allay keepers. Vanilla flight treats an open
 * trapdoor's whole cell as passable even though the upright panel still blocks
 * one side. Keeper routes retain the usable space around a panel but reject an
 * edge when the allay's body would actually cross its collision shape.
 */
public final class AllayPathNavigation extends FlyingPathNavigation {

  public AllayPathNavigation(Mob mob, Level level) {
    super(mob, level);
  }

  @Override
  protected PathFinder createPathFinder(int maxVisitedNodes) {
    this.nodeEvaluator = new KeeperNodeEvaluator();
    this.nodeEvaluator.setCanPassDoors(true);
    return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
  }

  /** The flying evaluator with trapdoor transitions checked against real collision. */
  private static final class KeeperNodeEvaluator extends FlyNodeEvaluator {
    @Override
    public int getNeighbors(Node[] output, Node node) {
      int count = super.getNeighbors(output, node);
      int accepted = 0;
      for (int index = 0; index < count; index++) {
        Node candidate = output[index];
        if (canTraverse(node, candidate)) {
          output[accepted++] = candidate;
        }
      }
      return accepted;
    }

    private boolean canTraverse(Node from, Node to) {
      AABB start = bodyAt(from);
      Vec3 movement = new Vec3(to.x - from.x, to.y - from.y, to.z - from.z);
      AABB sweptBounds = start.minmax(start.move(movement));
      List<VoxelShape> panels = null;
      for (BlockPos pos : BlockPos.betweenClosed(
          Mth.floor(sweptBounds.minX), Mth.floor(sweptBounds.minY), Mth.floor(sweptBounds.minZ),
          Mth.floor(sweptBounds.maxX), Mth.floor(sweptBounds.maxY), Mth.floor(sweptBounds.maxZ))) {
        BlockState state = this.currentContext.getBlockState(pos);
        if (!(state.getBlock() instanceof TrapDoorBlock)) {
          continue;
        }
        VoxelShape panel = state.getCollisionShape(this.currentContext.level(), pos)
            .move(pos.getX(), pos.getY(), pos.getZ());
        if (!panel.isEmpty()) {
          if (panels == null) {
            panels = new ArrayList<>(1);
          }
          panels.add(panel);
        }
      }
      return panels == null || hasCollisionFreeSweep(start, movement, panels);
    }

    private AABB bodyAt(Node node) {
      double radius = this.mob.getBbWidth() / 2.0D;
      double x = node.x + 0.5D;
      double z = node.z + 0.5D;
      return new AABB(x - radius, node.y, z - radius,
          x + radius, node.y + this.mob.getBbHeight(), z + radius);
    }
  }

  /** Sample a body closely enough that a three-sixteenths trapdoor panel cannot be skipped. */
  static boolean hasCollisionFreeSweep(AABB start, Vec3 movement, List<VoxelShape> collisions) {
    double shortestSide = Math.min(start.getXsize(), Math.min(start.getYsize(), start.getZsize()));
    int steps = Math.max(1, Mth.ceil(movement.length() / (shortestSide / 2.0D)));
    Vec3 step = movement.scale(1.0D / steps);
    AABB body = start;
    for (int index = 0; index < steps; index++) {
      body = body.move(step);
      AABB sampledBody = body;
      if (collisions.stream().anyMatch(shape ->
          Shapes.joinIsNotEmpty(shape, Shapes.create(sampledBody), BooleanOp.AND))) {
        return false;
      }
    }
    return true;
  }
}
