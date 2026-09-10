package com.quzzar.kithkyn.entities.ai;

import java.util.UUID;

/** A guard's whole-night choice, reproducible from the saved identity and world day. */
public enum GuardNightRoutine {
  POST,
  PATROL,
  SLEEP;

  private static final long DAY_LENGTH = 24_000L;
  private static final long NIGHT_SALT = 0x9E3779B97F4A7C15L;
  private static final long PATROL_SALT = 0xD1B54A32D192ED03L;

  /**
   * All guards sleep on 20% of nights; awake captains always patrol. Among
   * awake posted guards, 30% patrol instead. Assignment and weapons do not
   * change, and dawn restores the assigned duty. Hashing the UUID and day
   * keeps each choice fixed through goal interruptions and entity reloads.
   */
  public static GuardNightRoutine choose(UUID person, long dayTime, boolean night,
      boolean captain, boolean hasPost) {
    return choose(person, dayTime, night, captain, hasPost, false, false);
  }

  /** Castle routes and the jailer's fixed watch preserve the very same sleeping nights. */
  public static GuardNightRoutine choose(UUID person, long dayTime, boolean night,
      boolean captain, boolean hasPost, boolean castlePatrol, boolean jailer) {
    if (!night) return jailer ? POST : castlePatrol || captain || !hasPost ? PATROL : POST;
    long seed = mix(person.getMostSignificantBits()) ^ person.getLeastSignificantBits()
        ^ Math.floorDiv(dayTime, DAY_LENGTH) * NIGHT_SALT;
    if (chance(seed) < 0.20D) return SLEEP;
    if (jailer) return POST;
    if (castlePatrol || captain || !hasPost || chance(seed ^ PATROL_SALT) < 0.30D) return PATROL;
    return POST;
  }

  /** A uniform 53-bit fraction; separate salts keep sleep and patrol independent. */
  private static double chance(long seed) {
    return (mix(seed) >>> 11) * 0x1.0p-53;
  }

  /** SplitMix64's finalizer diffuses nearby days and UUIDs across the complete bit range. */
  private static long mix(long value) {
    value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
    value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
    return value ^ (value >>> 31);
  }
}
