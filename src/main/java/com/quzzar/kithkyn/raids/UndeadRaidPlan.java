package com.quzzar.kithkyn.raids;

import java.util.Random;

import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;

/**
 * The shape of an undead raid, as pure arithmetic (docs/undead.md): how many
 * waves the difficulty sends, how many dead each wave brings against a village
 * of a given size, and what one raider carries. Kept free of the world so a
 * test can pin every number down.
 */
public final class UndeadRaidPlan {

  /** The weapons the dead scavenge; a third of every wave shoots. */
  public enum Weapon {
    STONE_SWORD,
    IRON_SWORD,
    BOW
  }

  public enum Armour {
    NONE,
    LEATHER,
    CHAIN
  }

  public record Kit(Weapon weapon, boolean shield, Armour helmet, Armour chest, Armour legs, Armour boots) {
  }

  private static final int MINIMUM_WAVE = 2;
  private static final int BASE_WAVE_CAP = 8;
  private static final int WAVE_CAP = 10;
  private static final int IRON_FROM_WAVE = 3;
  private static final float ARMOUR_CHANCE_PER_WAVE = 0.15F;
  private static final float ARMOUR_CHANCE_FLOOR = 0.10F;
  private static final float ARMOUR_CHANCE_CAP = 0.70F;

  private UndeadRaidPlan() {
  }

  /** Waves per raid: none in peace, and vanilla's two-three-four climb otherwise. */
  public static int waves(Difficulty difficulty) {
    return switch (difficulty) {
      case PEACEFUL -> 0;
      case EASY -> 2;
      case NORMAL -> 3;
      case HARD -> 4;
    };
  }

  /**
   * Raiders in one wave: a hamlet meets a handful and a town meets a band,
   * and every later wave brings one more than the last.
   */
  public static int waveSize(int population, int wave) {
    int base = Mth.clamp(2 + population / 3, MINIMUM_WAVE, BASE_WAVE_CAP);
    return Mth.clamp(base + wave - 1, MINIMUM_WAVE, WAVE_CAP);
  }

  /** One raider's kit, fixed by a seed so a saved raider re-arms the same way. */
  public static Kit kit(long seed, int wave) {
    Random random = new Random(seed);
    boolean ranged = random.nextInt(3) == 0;
    Weapon weapon = ranged ? Weapon.BOW : wave >= IRON_FROM_WAVE ? Weapon.IRON_SWORD : Weapon.STONE_SWORD;
    boolean shield = !ranged && random.nextInt(5) == 0;
    float chance = Math.min(ARMOUR_CHANCE_FLOOR + ARMOUR_CHANCE_PER_WAVE * wave, ARMOUR_CHANCE_CAP);
    Armour tier = wave >= IRON_FROM_WAVE ? Armour.CHAIN : Armour.LEATHER;
    return new Kit(weapon, shield,
        random.nextFloat() < chance ? tier : Armour.NONE,
        random.nextFloat() < chance ? tier : Armour.NONE,
        random.nextFloat() < chance ? tier : Armour.NONE,
        random.nextFloat() < chance ? tier : Armour.NONE);
  }
}
