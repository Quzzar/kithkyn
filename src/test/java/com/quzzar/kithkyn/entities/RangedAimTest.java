package com.quzzar.kithkyn.entities;

import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class RangedAimTest {
  @Test
  void arrowsIntersectBodiesAcrossRangesElevationsAndDirections() {
    for (double speed : new double[] {3, 6}) {
      for (int range : new int[] {4, 12, 24, 40, 48, 64}) {
        for (int height : new int[] {-20, 0, 12}) {
          for (int direction = 0; direction < 8; direction++) {
            double angle = direction * Math.PI / 4;
            assertHit(new Vec3(Math.cos(angle) * range, height, Math.sin(angle) * range),
                Vec3.ZERO, speed, 0.05, RangedAim.ARROW_DRAG);
          }
        }
      }
    }
  }

  @Test
  void leadsTargetsWalkingAcrossAndAlongTheShot() {
    for (Vec3 motion : new Vec3[] {new Vec3(0, 0, 0.2), new Vec3(0.2, 0, 0), new Vec3(-0.2, 0, 0)}) {
      for (double speed : new double[] {1.6, 3, 6}) {
        assertHit(new Vec3(24, -4, 0), motion, speed, 0.05, RangedAim.ARROW_DRAG);
      }
    }
  }

  @Test
  void retainsSlowHuntingShotsAndStraightFireworkFlight() {
    assertHit(new Vec3(14, -1, 0), Vec3.ZERO, 1.6, 0.05, RangedAim.ARROW_DRAG);
    assertHit(new Vec3(48, -12, 3), new Vec3(0, 0, 0.15), 6, 0, 1);
  }

  @Test
  void rejectsShotsBeyondTheProjectilesReach() {
    assertTrue(RangedAim.velocity(Vec3.ZERO, new Vec3(48, 0, 0), Vec3.ZERO, 1.6, 0.05, RangedAim.ARROW_DRAG).isEmpty());
    assertTrue(RangedAim.velocity(Vec3.ZERO, new Vec3(12, 0, 0), new Vec3(7, 0, 0), 6, 0.05, RangedAim.ARROW_DRAG).isEmpty());
  }

  /** Independent tick-by-tick flight and relative collision against a moving target box. */
  private static void assertHit(Vec3 target, Vec3 motion, double speed, double gravity, double drag) {
    Vec3 velocity = RangedAim.velocity(Vec3.ZERO, target, motion, speed, gravity, drag).orElseThrow();
    Vec3 position = Vec3.ZERO;
    AABB body = new AABB(-0.25, -0.8, -0.25, 0.25, 0.8, 0.25);
    for (int tick = 0; tick < 100; tick++) {
      Vec3 next = position.add(velocity);
      Vec3 relativeStart = position.subtract(target.add(motion.scale(tick)));
      Vec3 relativeEnd = next.subtract(target.add(motion.scale(tick + 1)));
      if (body.contains(relativeStart) || body.clip(relativeStart, relativeEnd).isPresent()) return;
      position = next;
      velocity = velocity.scale(drag).add(0, -gravity, 0);
    }
    throw new AssertionError("Missed target=" + target + ", motion=" + motion + ", speed=" + speed);
  }
}
