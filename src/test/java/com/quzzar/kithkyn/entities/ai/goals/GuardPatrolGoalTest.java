package com.quzzar.kithkyn.entities.ai.goals;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class GuardPatrolGoalTest {
  @Test
  void patrolRejectsAPartialPathWhoseClosestNodeIsDownTheCave() {
    BlockPos target = new BlockPos(-1256, 73, 1109);

    assertFalse(GuardPatrolGoal.acceptsPatrolEndpoint(
        target, new BlockPos(-1248, 70, 1107), false));
    assertFalse(GuardPatrolGoal.acceptsPatrolEndpoint(
        target, new BlockPos(-1256, 68, 1109), false));
    assertTrue(GuardPatrolGoal.acceptsPatrolEndpoint(
        target, new BlockPos(-1255, 73, 1109), false));
  }

  @Test
  void guardRouteRejectsACompletePathThatDetoursDownTheCave() {
    BlockPos start = new BlockPos(-1223, 72, 1098);
    BlockPos station = new BlockPos(-1244, 78, 1115);

    assertFalse(GuardRouteSafety.staysNearSurface(start, station, List.of(
        start,
        new BlockPos(-1235, 71, 1102),
        new BlockPos(-1239, 69, 1108),
        new BlockPos(-1242, 54, 1113),
        station)));
    assertTrue(GuardRouteSafety.staysNearSurface(start, station, List.of(
        start,
        new BlockPos(-1235, 70, 1102),
        station)));
  }

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
