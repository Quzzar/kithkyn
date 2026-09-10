package com.quzzar.kithkyn.entities.ai.goals;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class GuardPatrolGoalTest {
  @Test
  void castleArrivalRequiresTheActualWaypointOnItsFloor() {
    BlockPos target = new BlockPos(20, 11, 16);
    assertTrue(GuardPatrolGoal.reachedCastlePoint(target, new Vec3(20.5, 11, 16.5)));
    assertTrue(GuardPatrolGoal.reachedCastlePoint(target, new Vec3(20.6, 11.5, 16.6)));
    assertFalse(GuardPatrolGoal.reachedCastlePoint(target, new Vec3(22.5, 11, 16.5)));
    assertFalse(GuardPatrolGoal.reachedCastlePoint(target, new Vec3(20.5, 10, 16.5)));
    assertFalse(GuardPatrolGoal.reachedCastlePoint(target, new Vec3(20.5, 12, 16.5)));
  }
}
