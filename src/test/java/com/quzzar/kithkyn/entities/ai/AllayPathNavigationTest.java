package com.quzzar.kithkyn.entities.ai;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

class AllayPathNavigationTest {
  private static final double ALLAY_WIDTH = 0.35D;
  private static final double ALLAY_HEIGHT = 0.6D;

  @Test
  void anOpenTrapdoorRejectsOnlyMovesThatCrossItsVerticalPanel() {
    BlockState trapdoor = Blocks.JUNGLE_TRAPDOOR.defaultBlockState()
        .setValue(TrapDoorBlock.OPEN, true)
        .setValue(TrapDoorBlock.FACING, Direction.SOUTH);
    List<VoxelShape> collisions = collisionsOf(trapdoor);

    assertFalse(AllayPathNavigation.hasCollisionFreeSweep(
        bodyAt(0.5D, 0.0D, -0.5D), new Vec3(0.0D, 0.0D, 1.0D), collisions));
    assertTrue(AllayPathNavigation.hasCollisionFreeSweep(
        bodyAt(0.5D, 0.0D, 0.5D), new Vec3(0.0D, 0.0D, 1.0D), collisions));
    assertTrue(AllayPathNavigation.hasCollisionFreeSweep(
        bodyAt(-0.5D, 0.0D, 0.5D), new Vec3(1.0D, 0.0D, 0.0D), collisions));
  }

  @Test
  void aClosedTopTrapdoorAllowsFlightUnderneathButNotThroughIt() {
    BlockState trapdoor = Blocks.JUNGLE_TRAPDOOR.defaultBlockState()
        .setValue(TrapDoorBlock.OPEN, false)
        .setValue(TrapDoorBlock.HALF, Half.TOP);
    List<VoxelShape> collisions = collisionsOf(trapdoor);

    assertTrue(AllayPathNavigation.hasCollisionFreeSweep(
        bodyAt(0.5D, 0.0D, -0.5D), new Vec3(0.0D, 0.0D, 1.0D), collisions));
    assertFalse(AllayPathNavigation.hasCollisionFreeSweep(
        bodyAt(0.5D, 0.0D, 0.5D), new Vec3(0.0D, 1.0D, 0.0D), collisions));
  }

  private static List<VoxelShape> collisionsOf(BlockState state) {
    return List.of(state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
  }

  private static AABB bodyAt(double x, double y, double z) {
    double radius = ALLAY_WIDTH / 2.0D;
    return new AABB(x - radius, y, z - radius, x + radius, y + ALLAY_HEIGHT, z + radius);
  }
}
