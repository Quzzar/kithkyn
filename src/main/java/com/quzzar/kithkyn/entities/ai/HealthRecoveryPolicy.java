package com.quzzar.kithkyn.entities.ai;

/** Shared health thresholds and timing for a person's recovery behaviors. */
public final class HealthRecoveryPolicy {

  /** Ten seconds of Regeneration I after using the village campfire. */
  public static final int CAMPFIRE_REGENERATION_TICKS = 20 * 10;

  /** One campfire recovery per person per minute. */
  public static final long CAMPFIRE_COOLDOWN_TICKS = 20L * 60L;

  /** Only residents already this close consider the campfire a recovery option. */
  public static final double CAMPFIRE_SEARCH_RANGE = 30.0D;

  /**
   * How far a hurt villager will walk to be tended by the village cleric. Wider
   * than the campfire's because a cleric is the better cure: a splash of
   * regeneration outlasts the fire's ten seconds several times over, and the
   * cleric brings it to whoever stands close.
   */
  public static final double CLERIC_SEARCH_RANGE = 48.0D;

  /**
   * How close a patient stands to the cleric to be tended: inside the ten
   * blocks the cleric scans and the seven they can throw, with room for the
   * cleric to step in and steady the throw.
   */
  public static final double CLERIC_TENDING_REACH = 5.0D;

  /** How long a patient waits beside a cleric who has not tended them before going about their day. */
  public static final int CLERIC_WAIT_TICKS = 20 * 60;

  private HealthRecoveryPolicy() {
  }

  /** Eating, retreating, and campfire recovery begin below one-third health. */
  public static boolean isBadlyHurt(float health, float maximumHealth) {
    return maximumHealth > 0.0F && health < maximumHealth / 3.0F;
  }

  /**
   * Where a recovery stops: a little above the line it started at, so a
   * villager hovering on the threshold does not start and stop every second.
   * The eating goal's rule, shared by seeking the cleric.
   */
  public static boolean isBackOverTheLine(float health, float maximumHealth) {
    return health >= maximumHealth / 3.0F + 2.0F;
  }

  /** Whether the persisted campfire cooldown has elapsed. */
  public static boolean isCampfireReady(long gameTime, long availableAt) {
    return gameTime >= availableAt;
  }

  /** The person needs help, carries no food, and is outside their cooldown. */
  public static boolean shouldSeekCampfire(float health, float maximumHealth, boolean hasMeal,
      long gameTime, long availableAt) {
    return isBadlyHurt(health, maximumHealth)
        && !hasMeal
        && isCampfireReady(gameTime, availableAt);
  }

  /**
   * The person needs help and a cleric could give it: badly hurt, not already
   * regenerating, and by day, since the cleric's round stops at dark like every
   * other job. Food is no bar: a hurt villager eats on the way and while they
   * wait (Aaron, 2026-09-12: "run to cleric, eat some food").
   */
  public static boolean shouldSeekCleric(float health, float maximumHealth, boolean regenerating,
      boolean night) {
    return isBadlyHurt(health, maximumHealth) && !regenerating && !night;
  }

  /**
   * A potion that mends is drunk below two thirds of health: sooner than
   * eating, since a potion is carried for exactly this and regeneration heals
   * over time rather than at once (ClericPotions.need).
   */
  public static boolean shouldDrinkForHealth(float health, float maximumHealth) {
    return maximumHealth > 0.0F && health < maximumHealth * 2.0F / 3.0F;
  }

  /** The first game tick at which another campfire recovery may begin. */
  public static long nextCampfireUse(long gameTime) {
    return gameTime + CAMPFIRE_COOLDOWN_TICKS;
  }
}
