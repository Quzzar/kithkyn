package com.quzzar.kithkyn.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.quzzar.kithkyn.village.Occupation;

import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
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
 * <p>Every throw consumes one real splash potion from the cleric's hands or
 * pack, and what the cleric carries decides what they can do: a splash of
 * regeneration makes a healer, a splash of harming makes a hexer, and a cleric
 * carrying both chooses by target. Nothing is conjured. Stock is grown the one
 * way a cleric has, at their station ({@code BrewStep}): one carried potion of
 * a brew stands for the recipe, and a brewing session yields {@link #BREW_YIELD}
 * more of it. That seed is why the last potion of a brew is never thrown and
 * never given away ({@link #giveable}): a cleric who parts with it has lost the
 * brew for good, and would have to be handed another before making more.
 *
 * <p>A brew is the item plus its {@link PotionContents}: a splash of healing
 * and a splash of harming are both splash potions and are never confused. The
 * static helpers take the three places a villager keeps things so the rules
 * can be tested on plain containers; the {@link RealPerson} overloads read the
 * villager's own.
 */
public final class ClericPotions {

  /** Carried potions of one brew a cleric brews back up to. */
  public static final int STOCK_TARGET = 4;

  /** Potions one brewing session yields, the three bottles of a brewing stand. */
  public static final int BREW_YIELD = 3;

  /** A brewing session takes as long as the stand does: twenty seconds. */
  public static final int BREW_TICKS = 400;

  /** The items a cleric's stock is kept in overnight rather than shelved with the rest of the pack. */
  public static final Set<Item> STOCK_ITEMS = Set.of(Items.SPLASH_POTION, Items.LINGERING_POTION);

  /** Below this the cleric reaches for instant healing over regeneration. */
  private static final float NEARLY_DEAD = 4.0F;

  private ClericPotions() {
  }

  /** One brew the cleric carries, with a sample stack and the count across every slot. */
  public record Stock(ItemStack sample, int count) {
  }

  /** A potion that can be thrown at all: a splash or lingering potion that does something. */
  public static boolean isThrowable(ItemStack stack) {
    if (stack.isEmpty() || !STOCK_ITEMS.contains(stack.getItem())) {
      return false;
    }
    PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
    return contents != null && contents.getAllEffects().iterator().hasNext();
  }

  /** A throwable brew whose every effect hurts: what a cleric throws at an enemy and never at a friend. */
  public static boolean isHarmful(ItemStack stack) {
    if (!isThrowable(stack)) {
      return false;
    }
    for (MobEffectInstance effect : stack.get(DataComponents.POTION_CONTENTS).getAllEffects()) {
      if (effect.getEffect().value().getCategory() != MobEffectCategory.HARMFUL) {
        return false;
      }
    }
    return true;
  }

  /** A throwable brew with nothing harmful in it: what a cleric throws over the hurt. */
  public static boolean isBeneficial(ItemStack stack) {
    if (!isThrowable(stack)) {
      return false;
    }
    for (MobEffectInstance effect : stack.get(DataComponents.POTION_CONTENTS).getAllEffects()) {
      if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
        return false;
      }
    }
    return true;
  }

  /** Whether the brew carries this effect, whatever else it does. */
  public static boolean has(ItemStack stack, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
    if (!isThrowable(stack)) {
      return false;
    }
    for (MobEffectInstance instance : stack.get(DataComponents.POTION_CONTENTS).getAllEffects()) {
      if (instance.getEffect().equals(effect)) {
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

  /** Every throwable stack across the hands and the pack, live, in that order. */
  static List<ItemStack> stacks(ItemStack mainHand, ItemStack offHand, Container pack) {
    List<ItemStack> out = new ArrayList<>();
    if (isThrowable(mainHand)) {
      out.add(mainHand);
    }
    if (isThrowable(offHand)) {
      out.add(offHand);
    }
    for (int slot = 0; slot < pack.getContainerSize(); slot++) {
      ItemStack stack = pack.getItem(slot);
      if (isThrowable(stack)) {
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
   * The stack to throw one of, for the first brew that passes the test and has
   * a potion to spare, or null when none does. The seed is protected here: a
   * brew with a single potion left is never offered, so throwing can never end
   * a brew. Hands are preferred over the pack, which is where the mark of the
   * trade already sits.
   */
  @Nullable
  public static ItemStack throwable(ItemStack mainHand, ItemStack offHand, Container pack,
      Predicate<ItemStack> kind) {
    for (ItemStack stack : stacks(mainHand, offHand, pack)) {
      if (kind.test(stack) && carried(mainHand, offHand, pack, stack) >= 2) {
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
    if (!isThrowable(stack)) {
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
   * The order a healer reaches for brews: instant healing when the patient is
   * nearly dead, regeneration otherwise, then anything else beneficial they
   * happen to carry.
   */
  public static List<Predicate<ItemStack>> healingPreference(LivingEntity patient) {
    Predicate<ItemStack> healing = stack -> isBeneficial(stack) && has(stack, MobEffects.HEAL);
    Predicate<ItemStack> regeneration = stack -> isBeneficial(stack) && has(stack, MobEffects.REGENERATION);
    return patient.getHealth() <= NEARLY_DEAD
        ? List.of(healing, regeneration, ClericPotions::isBeneficial)
        : List.of(regeneration, healing, ClericPotions::isBeneficial);
  }

  // The villager's own three places, read live.

  public static boolean isCleric(RealPerson person) {
    return person.getOccupation() == Occupation.CLERIC;
  }

  public static List<Stock> stock(RealPerson person) {
    return stock(person.getMainHandItem(), person.getOffhandItem(), person.personMainInv);
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

  /** Whether the target is one the cleric must never splash with harm. */
  public static boolean isAlly(LivingEntity thrower, LivingEntity other) {
    return other instanceof Person || other instanceof net.minecraft.world.entity.npc.AbstractVillager
        || com.quzzar.kithkyn.village.VillageGolems.supports(other)
        || (other instanceof Player player && !player.getAbilities().instabuild)
        || thrower.isAlliedTo(other);
  }

  /**
   * Whether a harmful splash burst at the target would catch an ally. A thrown
   * potion reaches everyone within four blocks across and two up or down of
   * where it breaks, so the check is that box around the target, less the
   * target itself.
   */
  public static boolean harmfulSplashWouldCatchAlly(LivingEntity thrower, LivingEntity target) {
    for (LivingEntity nearby : thrower.level().getEntitiesOfClass(LivingEntity.class,
        target.getBoundingBox().inflate(4.0D, 2.0D, 4.0D))) {
      if (nearby != target && nearby.isAlive() && isAlly(thrower, nearby)) {
        return true;
      }
    }
    return false;
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
