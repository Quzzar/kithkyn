package com.quzzar.kithkyn.entities.ai.goals;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

class RangedShotSafetyTest {

  private static final Vec3 FROM = new Vec3(0, 10, 0);
  private static final Vec3 TO = new Vec3(10, 10, 0);

  @Test
  void anAdjacentGuardDoesNotBlockTheShotButOneInTheLineDoes() {
    assertFalse(RangedShotSafety.intersects(FROM, TO, new AABB(-0.3, 9, 1.7, 0.3, 11, 2.3)));
    assertTrue(RangedShotSafety.intersects(FROM, TO, new AABB(4.7, 9, -0.3, 5.3, 11, 0.3)));
  }

  @Test
  void alliesBehindEitherEndAndBelowTheShotAreNotObstructions() {
    assertFalse(RangedShotSafety.intersects(FROM, TO, new AABB(-3, 9, -0.3, -2, 11, 0.3)));
    assertFalse(RangedShotSafety.intersects(FROM, TO, new AABB(12, 9, -0.3, 13, 11, 0.3)));
    assertFalse(RangedShotSafety.intersects(FROM, TO, new AABB(4, 5, -0.3, 5, 7, 0.3)));
  }

  @Test
  void sharedSpaceAtTheMuzzleIsUnsafe() {
    assertTrue(RangedShotSafety.intersects(FROM, TO, new AABB(-0.3, 9, -0.3, 0.3, 11, 0.3)));
  }
}
