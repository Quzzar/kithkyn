package com.quzzar.kithkyn.entities;

import java.util.Locale;

import javax.annotation.Nullable;

import com.quzzar.kithkyn.configuration.KithkynConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

/**
 * Whether a person is one of the living or one of the undead (docs/undead.md).
 *
 * The undead are people in every respect the simulation cares about: they
 * hold jobs, marry, raise children, keep pets and gossip. What sets them
 * apart is one number, {@link #strangerBaseline()}: where a person of this
 * kind starts anyone they have never met. The living start a stranger at
 * indifference. The undead start a stranger well past the grudge line, so
 * their fighters attack on sight and everyone else keeps their distance,
 * until that one person has been given a reason to think better of you.
 *
 * A kind is fixed at the village: a village is founded living or undead,
 * its arrivals take its kind, and children take their parents'. The look
 * (a skeleton today; a zombie is a second undead look waiting to be
 * authored) is chosen by the appearance compositor from the same value.
 */
public enum Kind {
  LIVING,
  UNDEAD;

  /** Salt for the founding roll, so it never lines up with the style or rotation rolls. */
  private static final long FOUNDING_SALT = 0x5D3A2B7C91E4F608L;

  /** The token this kind takes in data: {@code living}, {@code undead}. */
  public String id() {
    return name().toLowerCase(Locale.ROOT);
  }

  /** The kind for an id token, in either case, or null when no such kind exists. */
  @Nullable
  public static Kind parse(String id) {
    for (Kind kind : values()) {
      if (kind.id().equalsIgnoreCase(id)) {
        return kind;
      }
    }
    return null;
  }

  /** The kind for an id token, living for anything unknown or blank, which is what every older save is. */
  public static Kind fromId(String id) {
    Kind kind = parse(id);
    return kind != null ? kind : LIVING;
  }

  /**
   * Where a person of this kind starts a stranger, from -100 to 100. Read
   * wherever an opinion of someone outside the village is looked up with no
   * entry yet, and where such opinions fade back to over time. Never stored,
   * so the switch is the one config number for the undead.
   */
  public int strangerBaseline() {
    return this == UNDEAD ? KithkynConfig.UndeadStrangerBaseline : 0;
  }

  /**
   * How the kind is stated to the person's own brain, after their gender:
   * a plain fact about themselves. The living need no such line.
   */
  public String describe() {
    return this == UNDEAD ? "undead, a walking skeleton of old bone" : "";
  }

  /** The kind a naturally founded village takes, rolled once from the world seed and the site. */
  public static Kind forNaturalFounding(long worldSeed, BlockPos site) {
    return rollForFounding(worldSeed, site, KithkynConfig.UndeadVillageChance);
  }

  /**
   * The pure roll behind {@link #forNaturalFounding}: the same seed and site
   * always answer the same way, so a founding probe and the founding it
   * leads to agree, and a site search cannot reroll its way to a preference.
   */
  public static Kind rollForFounding(long worldSeed, BlockPos site, double undeadChance) {
    if (undeadChance <= 0.0D) {
      return LIVING;
    }
    if (undeadChance >= 1.0D) {
      return UNDEAD;
    }
    RandomSource random = RandomSource.create(worldSeed ^ site.asLong() ^ FOUNDING_SALT);
    return random.nextDouble() < undeadChance ? UNDEAD : LIVING;
  }

}
