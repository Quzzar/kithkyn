package com.quzzar.kithkyn.entities.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.FlyNodeEvaluator;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;

/**
 * Flight planning for a village's allay keepers. Vanilla flight admits every
 * trapdoor cell whether the panel is open or closed, so a keeper whose route
 * crossed a closed hatch flew into it and pressed there for good; nobody opens
 * trapdoors for anyone in this mod. A closed trapdoor is a wall to a keeper and
 * its route goes round, through an open panel or a door it can open itself
 * (entities/ai/behavior/AllayQuartermasterBehavior).
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

  /** The flying evaluator with closed trapdoors read as the walls they are. */
  private static final class KeeperNodeEvaluator extends FlyNodeEvaluator {
    @Override
    public PathType getPathType(PathfindingContext context, int x, int y, int z) {
      BlockState state = context.getBlockState(new BlockPos(x, y, z));
      if (state.getBlock() instanceof TrapDoorBlock && !state.getValue(TrapDoorBlock.OPEN)) {
        return PathType.BLOCKED;
      }
      return super.getPathType(context, x, y, z);
    }
  }
}
