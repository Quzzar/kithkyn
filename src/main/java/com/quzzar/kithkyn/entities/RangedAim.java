package com.quzzar.kithkyn.entities;

import java.util.Optional;
import net.minecraft.world.phys.Vec3;

/** A low, direct intercept using Minecraft's move, drag, then gravity tick order. */
public final class RangedAim {
  public static final double ARROW_DRAG = 0.99F;
  private static final int MAX_FLIGHT_TICKS = 100;

  private RangedAim() { }

  /** Returns the earliest reachable launch velocity; an impossible shot stays unfired. */
  public static Optional<Vec3> velocity(Vec3 origin, Vec3 target, Vec3 targetMotion,
      double speed, double gravity, double drag) {
    Vec3 offset = target.subtract(origin);
    double previousTime = 0;
    for (int tick = 1; tick <= MAX_FLIGHT_TICKS; tick++) {
      if (requiredVelocity(offset, targetMotion, tick, gravity, drag).lengthSqr() <= speed * speed) {
        double lower = previousTime;
        double upper = tick;
        for (int iteration = 0; iteration < 24; iteration++) {
          double middle = (lower + upper) * 0.5D;
          if (requiredVelocity(offset, targetMotion, middle, gravity, drag).lengthSqr() > speed * speed) {
            lower = middle;
          } else {
            upper = middle;
          }
        }
        return Optional.of(requiredVelocity(offset, targetMotion, upper, gravity, drag));
      }
      previousTime = tick;
    }
    return Optional.empty();
  }

  /** Fractional ticks interpolate the same straight segment used by projectile collision. */
  private static Vec3 requiredVelocity(Vec3 offset, Vec3 targetMotion, double time, double gravity, double drag) {
    int ticks = (int) time;
    double fraction = time - ticks;
    double decay = Math.pow(drag, ticks);
    double travelled = drag == 1 ? ticks : (1 - decay) / (1 - drag);
    double fall = drag == 1 ? gravity * ticks * (ticks - 1) / 2D
        : gravity * (ticks - travelled) / (1 - drag);
    fall += fraction * gravity * travelled;
    travelled += fraction * decay;
    return offset.add(targetMotion.scale(time)).add(0, fall, 0).scale(1 / travelled);
  }
}
