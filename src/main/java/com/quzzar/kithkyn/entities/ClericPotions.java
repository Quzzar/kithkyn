package com.quzzar.kithkyn.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.quzzar.kithkyn.entities.ai.HealthRecoveryPolicy;
import com.quzzar.kithkyn.village.Occupation;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.phys.Vec3;

/**
 * A cleric's potions are stock, not a spell (Aaron, 2026-09-12).
 *
 * <p>Every potion a cleric uses is a real bottle from their hands or pack, and
 * what they carry decides what they can do. How a bottle is used follows from
 * what it is (Aaron, 2026-09-12): an ordinary potion is drunk by the cleric
 * themselves ({@code DrinkPotionGoal}), and a splash or lingering potion is
 * thrown. Whether a throw goes to a friend or a foe follows from what the brew
 * would do to the body it lands on ({@link #outcomeOn}): a brew that helps goes
 * to an ally, and only while no enemy stands in the splash; a brew that hurts
 * goes to an enemy, and only while no ally does. What a brew does depends on the
 * body, so a splash of healing burns the undead, a splash of harming heals them,
 * and poison and regeneration do not take on them at all.
 *
 * <p>Nothing is conjured. Stock is grown the one way a cleric has, at their
 * station ({@code BrewStep}): one carried potion of a brew stands for the
 * recipe, and a brewing session yields {@link #BREW_YIELD} more of it. That seed
 * is why the last potion of a brew is never thrown, drunk or given away
 * ({@link #spare}, {@link #giveable}): a cleric who parts with it has lost the
 * brew for good, and would have to be handed another before making more.
 *
 * <p>A brew is the item plus its {@link PotionContents}: a potion of
 * regeneration and a splash of regeneration are different brews, as are a
 * splash of healing and a splash of harming. The static helpers take the three
 * places a villager keeps things so the rules can be tested on plain
 * containers; the {@link RealPerson} overloads read the villager's own.
 */
public final class ClericPotions {

  /** Carried potions of one brew a cleric brews back up to. */
  public static final int STOCK_TARGET = 4;

  /** Potions one brewing session yields, the three bottles of a brewing stand. */
  public static final int BREW_YIELD = 3;

  /** A brewing session takes as long as the stand does: twenty seconds. */
  public static final int BREW_TICKS = 400;

  /** The items a cleric's stock is kept in overnight rather than shelved with the rest of the pack. */
  public static final Set<Item> STOCK_ITEMS = Set.of(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION);

  /** Below this the cleric reaches for instant healing over regeneration. */
  private static final float NEARLY_DEAD = 4.0F;

  /** The effects that mend a hurt body: what a healer throws, and what a hurt villager drinks. */
  private static final List<Holder<MobEffect>> MENDING = List.of(MobEffects.HEAL, MobEffects.REGENERATION,
      MobEffects.ABSORPTION, MobEffects.HEALTH_BOOST);

  private ClericPotions() {
  }

  /** One brew the cleric carries, with a sample stack and the count across every slot. */
  public record Stock(ItemStack sample, int count) {
  }

  /** What a brew would do to one body it reaches. */
  public enum Outcome {
    HELPS, HURTS, MIXED, NOTHING
  }

  /** What is wrong with a body right now that a potion could answer, most urgent first. */
  public enum Need {
    DROWNING, BURNING, HURT
  }

  /** A potion with an effect in it, of any kind: stock a cleric keeps, brews and uses. Water is not. */
  public static boolean isStock(ItemStack stack) {
    if (stack.isEmpty() || !STOCK_ITEMS.contains(stack.getItem())) {
      return false;
    }
    PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
    return contents != null && contents.getAllEffects().iterator().hasNext();
  }

  /** A splash or lingering potion that does something: thrown, never drunk. */
  public static boolean isThrowable(ItemStack stack) {
    return isStock(stack) && stack.getItem() != Items.POTION;
  }

  /** An ordinary potion that does something: drunk, never thrown. */
  public static boolean isDrinkable(ItemStack stack) {
    return isStock(stack) && stack.getItem() == Items.POTION;
  }

  /**
   * What the brew does to a body, given whether healing and harm trade places
   * for it (the undead) and which of the brew's effects take on it at all.
   * Neutral effects count for nothing either way.
   */
  public static Outcome outcome(ItemStack stack, boolean invertedHealAndHarm, Predicate<MobEffectInstance> takes) {
    if (!isStock(stack)) {
      return Outcome.NOTHING;
    }
    boolean helps = false;
    boolean hurts = false;
    for (MobEffectInstance effect : stack.get(DataComponents.POTION_CONTENTS).getAllEffects()) {
      if (!takes.test(effect)) {
        continue;
      }
      MobEffectCategory category = effect.getEffect().value().getCategory();
      if (invertedHealAndHarm && effect.getEffect().equals(MobEffects.HEAL)) {
        category = MobEffectCategory.HARMFUL;
      } else if (invertedHealAndHarm && effect.getEffect().equals(MobEffects.HARM)) {
        category = MobEffectCategory.BENEFICIAL;
      }
      if (category == MobEffectCategory.BENEFICIAL) {
        helps = true;
      } else if (category == MobEffectCategory.HARMFUL) {
        hurts = true;
      }
    }
    if (helps && hurts) {
      return Outcome.MIXED;
    }
    return helps ? Outcome.HELPS : hurts ? Outcome.HURTS : Outcome.NOTHING;
  }

  /** What the brew does to this body, read from the body itself. */
  public static Outcome outcomeOn(ItemStack stack, LivingEntity body) {
    return outcome(stack, body.isInvertedHealAndHarm(), body::canBeAffected);
  }

  /** A brew whose every effect helps an ordinary living body. */
  public static boolean isBeneficial(ItemStack stack) {
    return outcome(stack, false, effect -> true) == Outcome.HELPS;
  }

  /** A brew whose every effect hurts an ordinary living body: what a cleric may fight with. */
  public static boolean isHarmful(ItemStack stack) {
    return outcome(stack, false, effect -> true) == Outcome.HURTS;
  }

  /** Whether the brew carries this effect, whatever else it does. */
  public static boolean has(ItemStack stack, Holder<MobEffect> effect) {
    if (!isStock(stack)) {
      return false;
    }
    for (MobEffectInstance instance : stack.get(DataComponents.POTION_CONTENTS).getAllEffects()) {
      if (instance.getEffect().equals(effect)) {
        return true;
      }
    }
    return false;
  }

  /** Whether the brew carries an effect that mends a hurt body. */
  public static boolean mends(ItemStack stack) {
    for (Holder<MobEffect> effect : MENDING) {
      if (has(stack, effect)) {
        return true;
      }
    }
    return false;
  }

  /** The same item with the same potion inside: the identity of a brew. */
  public static boolean sameBrew(ItemStack a, ItemStack b) {
    if (a.isEmpty() || b.isEmpty() || a.getItem() != b.getItem()) {
      return false;
    }
    PotionContents ca = a.get(DataComponents.POTION_CONTENTS);
    PotionContents cb = b.get(DataComponents.POTION_CONTENTS);
    return ca != null && ca.equals(cb);
  }

  /** Every stock stack across the hands and the pack, live, in that order. */
  static List<ItemStack> stacks(ItemStack mainHand, ItemStack offHand, Container pack) {
    List<ItemStack> out = new ArrayList<>();
    if (isStock(mainHand)) {
      out.add(mainHand);
    }
    if (isStock(offHand)) {
      out.add(offHand);
    }
    for (int slot = 0; slot < pack.getContainerSize(); slot++) {
      ItemStack stack = pack.getItem(slot);
      if (isStock(stack)) {
        out.add(stack);
      }
    }
    return out;
  }

  /** What the cleric carries, one entry per brew. */
  public static List<Stock> stock(ItemStack mainHand, ItemStack offHand, Container pack) {
    List<Stock> tally = new ArrayList<>();
    for (ItemStack stack : stacks(mainHand, offHand, pack)) {
      boolean counted = false;
      for (int index = 0; index < tally.size(); index++) {
        Stock entry = tally.get(index);
        if (sameBrew(entry.sample(), stack)) {
          tally.set(index, new Stock(entry.sample(), entry.count() + stack.getCount()));
          counted = true;
          break;
        }
      }
      if (!counted) {
        tally.add(new Stock(stack.copyWithCount(1), stack.getCount()));
      }
    }
    return tally;
  }

  /** How many of this brew are carried altogether. */
  public static int carried(ItemStack mainHand, ItemStack offHand, Container pack, ItemStack brew) {
    int count = 0;
    for (ItemStack stack : stacks(mainHand, offHand, pack)) {
      if (sameBrew(stack, brew)) {
        count += stack.getCount();
      }
    }
    return count;
  }

  /**
   * The stack to use one of, for the first brew that passes the test and has a
   * potion to spare, or null when none does. The seed is protected here: a brew
   * with a single potion left is never offered, so no throw or drink can end a
   * brew. Hands come before the pack: the off hand is where the cleric works.
   */
  @Nullable
  public static ItemStack spare(ItemStack mainHand, ItemStack offHand, Container pack, Predicate<ItemStack> kind) {
    for (ItemStack stack : stacks(mainHand, offHand, pack)) {
      if (kind.test(stack) && carried(mainHand, offHand, pack, stack) >= 2) {
        return stack;
      }
    }
    return null;
  }

  /** A splash or lingering potion to spare that passes the test, or null. */
  @Nullable
  public static ItemStack throwable(ItemStack mainHand, ItemStack offHand, Container pack,
      Predicate<ItemStack> kind) {
    return spare(mainHand, offHand, pack, stack -> isThrowable(stack) && kind.test(stack));
  }

  /** The first carried stack that passes the test, seed or not, or null: for villagers who do not brew. */
  @Nullable
  static ItemStack first(ItemStack mainHand, ItemStack offHand, Container pack, Predicate<ItemStack> kind) {
    for (ItemStack stack : stacks(mainHand, offHand, pack)) {
      if (kind.test(stack)) {
        return stack;
      }
    }
    return null;
  }

  /**
   * How many of this stack may leave the cleric: everything but the last
   * potion of its brew. A stack that is not potion stock is entirely giveable.
   */
  public static int giveable(ItemStack mainHand, ItemStack offHand, Container pack, ItemStack stack) {
    if (!isStock(stack)) {
      return stack.getCount();
    }
    int total = carried(mainHand, offHand, pack, stack);
    return Math.max(0, Math.min(stack.getCount(), total - 1));
  }

  /**
   * How many bottles of this brew in the pack are spare: the pack's count,
   * capped at everything but the seed. What the chat briefing tells a cleric
   * they have in their pockets, so a model that only ever offers what it was
   * told about never offers the seed (Aaron, 2026-09-12: "you have this many
   * minus one").
   */
  public static int sparePackCount(ItemStack mainHand, ItemStack offHand, Container pack, ItemStack brew) {
    int inPack = 0;
    for (int slot = 0; slot < pack.getContainerSize(); slot++) {
      ItemStack stack = pack.getItem(slot);
      if (sameBrew(stack, brew)) {
        inPack += stack.getCount();
      }
    }
    return Math.max(0, Math.min(inPack, carried(mainHand, offHand, pack, brew) - 1));
  }

  /** The carried brew furthest below {@link #STOCK_TARGET}, or null when every brew is stocked. */
  @Nullable
  public static Stock lowestBelowTarget(ItemStack mainHand, ItemStack offHand, Container pack) {
    Stock lowest = null;
    for (Stock entry : stock(mainHand, offHand, pack)) {
      if (entry.count() < STOCK_TARGET && (lowest == null || entry.count() < lowest.count())) {
        lowest = entry;
      }
    }
    return lowest;
  }

  /**
   * The order a healer reaches for brews for a hurt patient: instant healing
   * when the patient is nearly dead, regeneration otherwise, then anything else
   * that mends. Every choice must help this patient as a whole, so a splash of
   * healing is never the answer for a body it would burn.
   */
  public static List<Predicate<ItemStack>> healingPreference(LivingEntity patient) {
    Predicate<ItemStack> helps = stack -> outcomeOn(stack, patient) == Outcome.HELPS;
    Predicate<ItemStack> healing = stack -> helps.test(stack) && has(stack, MobEffects.HEAL);
    Predicate<ItemStack> regeneration = stack -> helps.test(stack) && has(stack, MobEffects.REGENERATION);
    Predicate<ItemStack> mending = stack -> helps.test(stack) && mends(stack);
    return patient.getHealth() <= NEARLY_DEAD
        ? List.of(healing, regeneration, mending)
        : List.of(regeneration, healing, mending);
  }

  /**
   * What is wrong with a body right now that a potion could answer, or null:
   * out of air under water, then burning, then hurt below the drinking line
   * ({@link HealthRecoveryPolicy#shouldDrinkForHealth}) and not already
   * regenerating, unless nearly dead, when instant healing still helps.
   */
  @Nullable
  public static Need need(LivingEntity body) {
    if (body.isUnderWater() && body.getAirSupply() < body.getMaxAirSupply() / 3
        && !body.hasEffect(MobEffects.WATER_BREATHING)) {
      return Need.DROWNING;
    }
    if (body.isOnFire() && !body.fireImmune() && !body.hasEffect(MobEffects.FIRE_RESISTANCE)) {
      return Need.BURNING;
    }
    boolean nearlyDead = body.getHealth() <= NEARLY_DEAD;
    if (HealthRecoveryPolicy.shouldDrinkForHealth(body.getHealth(), body.getMaxHealth())
        && (nearlyDead || !body.hasEffect(MobEffects.REGENERATION))) {
      return Need.HURT;
    }
    return null;
  }

  /** The order a body reaches for brews that answer its need, each helping that body as a whole. */
  public static List<Predicate<ItemStack>> preferenceFor(Need need, LivingEntity body) {
    Predicate<ItemStack> helps = stack -> outcomeOn(stack, body) == Outcome.HELPS;
    return switch (need) {
      case DROWNING -> List.of(stack -> helps.test(stack) && has(stack, MobEffects.WATER_BREATHING));
      case BURNING -> List.of(stack -> helps.test(stack) && has(stack, MobEffects.FIRE_RESISTANCE));
      case HURT -> healingPreference(body);
    };
  }

  // The villager's own three places, read live.

  public static boolean isCleric(RealPerson person) {
    return person.getOccupation() == Occupation.CLERIC;
  }

  public static List<Stock> stock(RealPerson person) {
    return stock(person.getMainHandItem(), person.getOffhandItem(), person.personMainInv);
  }

  @Nullable
  public static ItemStack spare(RealPerson person, Predicate<ItemStack> kind) {
    return spare(person.getMainHandItem(), person.getOffhandItem(), person.personMainInv, kind);
  }

  @Nullable
  public static ItemStack throwable(RealPerson person, Predicate<ItemStack> kind) {
    return throwable(person.getMainHandItem(), person.getOffhandItem(), person.personMainInv, kind);
  }

  /** The first brew in the preference order the cleric can spare one of, or null. */
  @Nullable
  public static ItemStack throwable(RealPerson person, List<Predicate<ItemStack>> preference) {
    for (Predicate<ItemStack> kind : preference) {
      ItemStack stack = throwable(person, kind);
      if (stack != null) {
        return stack;
      }
    }
    return null;
  }

  public static boolean hasThrowable(RealPerson person, Predicate<ItemStack> kind) {
    return throwable(person, kind) != null;
  }

  /**
   * The potion this villager would drink now, or null: an ordinary potion that
   * answers what is wrong with them and helps them as a whole. A cleric keeps
   * the seed of every brew; a villager who does not brew drinks what they were
   * given.
   */
  @Nullable
  public static ItemStack drinkFor(RealPerson person) {
    Need need = need(person);
    if (need == null) {
      return null;
    }
    boolean cleric = isCleric(person);
    for (Predicate<ItemStack> kind : preferenceFor(need, person)) {
      Predicate<ItemStack> drink = stack -> isDrinkable(stack) && kind.test(stack);
      ItemStack stack = cleric
          ? spare(person, drink)
          : first(person.getMainHandItem(), person.getOffhandItem(), person.personMainInv, drink);
      if (stack != null) {
        return stack;
      }
    }
    return null;
  }

  public static int giveable(RealPerson person, ItemStack stack) {
    return giveable(person.getMainHandItem(), person.getOffhandItem(), person.personMainInv, stack);
  }

  public static int sparePackCount(RealPerson person, ItemStack brew) {
    return sparePackCount(person.getMainHandItem(), person.getOffhandItem(), person.personMainInv, brew);
  }

  @Nullable
  public static Stock lowestBelowTarget(RealPerson person) {
    return lowestBelowTarget(person.getMainHandItem(), person.getOffhandItem(), person.personMainInv);
  }

  /** Whether the body is one the cleric must never splash with harm. */
  public static boolean isAlly(LivingEntity thrower, LivingEntity other) {
    return other instanceof Person || other instanceof net.minecraft.world.entity.npc.AbstractVillager
        || com.quzzar.kithkyn.village.VillageGolems.supports(other)
        || (other instanceof Player player && !player.getAbilities().instabuild)
        || thrower.isAlliedTo(other);
  }

  /**
   * Whether the body is on the other side: a hostile mob, the cleric's own
   * target, or a mob with its sights on the cleric or one of their allies. An
   * ally is never an enemy.
   */
  public static boolean isEnemy(LivingEntity thrower, LivingEntity other) {
    if (isAlly(thrower, other)) {
      return false;
    }
    if (other instanceof Enemy || (thrower instanceof Mob mob && mob.getTarget() == other)) {
      return true;
    }
    return other instanceof Mob mob && mob.getTarget() != null
        && (mob.getTarget() == thrower || isAlly(thrower, mob.getTarget()));
  }

  /**
   * Whether a splash burst at the target would catch anyone the test picks out,
   * the target aside. A thrown potion reaches everyone within four blocks across
   * and two up or down of where it breaks, and a lingering cloud spreads less
   * than that, so the check is that box around the target.
   */
  public static boolean splashWouldCatch(LivingEntity thrower, LivingEntity target, Predicate<LivingEntity> who) {
    for (LivingEntity nearby : thrower.level().getEntitiesOfClass(LivingEntity.class,
        target.getBoundingBox().inflate(4.0D, 2.0D, 4.0D))) {
      if (nearby != target && nearby.isAlive() && who.test(nearby)) {
        return true;
      }
    }
    return false;
  }

  /** Whether a harmful burst at the target would catch an ally, the cleric included. */
  public static boolean harmfulSplashWouldCatchAlly(LivingEntity thrower, LivingEntity target) {
    return splashWouldCatch(thrower, target, other -> isAlly(thrower, other));
  }

  /** Whether a helpful burst at the target would catch an enemy (Aaron, 2026-09-12). */
  public static boolean helpfulSplashWouldCatchEnemy(LivingEntity thrower, LivingEntity target) {
    return splashWouldCatch(thrower, target, other -> isEnemy(thrower, other));
  }

  /**
   * Lob one potion of the stack at the target, the way a witch does: aimed at
   * where the target is heading, lofted with distance. The stack loses the
   * potion thrown; the thrown one is a copy of a single bottle, so a stack's
   * other potions are never in the air.
   */
  public static void throwOne(LivingEntity thrower, LivingEntity target, ItemStack stack) {
    Vec3 drift = target.getDeltaMovement();
    double dx = target.getX() + drift.x - thrower.getX();
    double dy = target.getEyeY() - 1.1F - thrower.getY();
    double dz = target.getZ() + drift.z - thrower.getZ();
    float flat = Mth.sqrt((float) (dx * dx + dz * dz));

    ThrownPotion thrown = new ThrownPotion(thrower.level(), thrower);
    thrown.setItem(stack.copyWithCount(1));
    thrown.setXRot(-20.0F);
    thrown.shoot(dx, dy + flat * 0.2F, dz, 0.75F, 8.0F);
    thrower.level().playSound((Player) null, thrower.getX(), thrower.getY(), thrower.getZ(),
        SoundEvents.SPLASH_POTION_THROW, thrower.getSoundSource(), 1.0F,
        0.8F + thrower.getRandom().nextFloat() * 0.4F);
    thrower.level().addFreshEntity(thrown);
    stack.shrink(1);
  }

}
