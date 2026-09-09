package com.quzzar.kithkyn.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.entities.ai.goals.work.PackLogistics;
import com.quzzar.kithkyn.village.GuardDuty;
import com.quzzar.kithkyn.village.Occupation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

/**
 * A guard's two physical weapons. Both use ordinary hand/pack persistence;
 * switching exchanges stacks, never copies them or invents a replacement.
 */
public final class GuardWeapons {

  private static final double DRAW_SWORD_DISTANCE = 3.0D;
  private static final double RETURN_TO_CROSSBOW_DISTANCE = 4.0D;

  private GuardWeapons() {
  }

  public static boolean hasSidearmDuty(RealPerson person) {
    GuardDuty duty = GuardDuty.of(person);
    return duty != null && duty.backupSword();
  }

  /** Fixed wall posts keep their original single weapon; patrols retain an axe beside their sword. */
  public static boolean isPatrol(RealPerson person) {
    return person.getOccupation() == Occupation.GUARD && GuardDuty.of(person) == null;
  }

  public static boolean usesLoadout(RealPerson person) {
    return isPatrol(person) || hasSidearmDuty(person);
  }

  private static List<JobTool> loadout(RealPerson person) {
    return isPatrol(person) ? List.of(JobTool.AXE, JobTool.SWORD) : List.of(JobTool.CROSSBOW, JobTool.SWORD);
  }

  /** Availability checks do not draw a tool merely because an idle work scan ran. */
  public static boolean carries(RealPerson person, JobTool tool) {
    return tool.inHand(person) || find(person.personMainInv, tool) >= 0;
  }

  /** The shared work lifecycle clears this even when danger or unreachable terrain cancels the chop. */
  public static void chopping(RealPerson person, boolean active) {
    person.setGuardChopping(active && isPatrol(person));
    tick(person);
  }

  /** Swap only carried weapons during combat; provisioning has its own cadence. */
  public static void tick(RealPerson person) {
    if (!usesLoadout(person) || person.isEating()) {
      return;
    }
    LivingEntity target = person.getTarget();
    if (isPatrol(person)) {
      JobTool preferred = patrolWeapon(person.isGuardChopping(), target != null && target.isAlive(),
          carries(person, JobTool.SWORD));
      draw(person, preferred);
      return;
    }
    boolean wantsSword = wantsSword(person.getMainHandItem().getItem() instanceof SwordItem,
        target != null && target.isAlive(), target == null ? Double.POSITIVE_INFINITY : person.distanceTo(target),
        target != null && person.hasLineOfSight(target));
    draw(person, wantsSword ? JobTool.SWORD : JobTool.CROSSBOW);
  }

  static JobTool patrolWeapon(boolean chopping, boolean liveTarget, boolean hasSword) {
    return hasSword && (!chopping || liveTarget) ? JobTool.SWORD : JobTool.AXE;
  }

  /** A small hysteresis keeps a target at arm's reach from toggling the hand every tick. */
  static boolean wantsSword(boolean holdingSword, boolean liveTarget, double distance, boolean visible) {
    return liveTarget && visible
        && distance <= (holdingSword ? RETURN_TO_CROSSBOW_DISTANCE : DRAW_SWORD_DISTANCE);
  }

  /** One-time backup kit. A full pack displaces one real stack onto the ground, never deletes it. */
  public static void issueSidearm(RealPerson person) {
    if (hasSidearmDuty(person)) {
      installSidearm(person.personMainInv, person.getMainHandItem(), stack -> {
        person.spawnAtLocation(stack);
        Kithkyn.LOGGER.info("[guard-kit] {} set down {} to carry their backup sword",
            person.getFullName(), stack.getHoverName().getString());
      });
    }
  }

  static void installSidearm(Container pack, ItemStack hand, Consumer<ItemStack> displaced) {
    if (hand.getItem() instanceof SwordItem || find(pack, JobTool.SWORD) >= 0) {
      return;
    }
    int slot = emptySlot(pack);
    if (slot < 0) {
      slot = pack.getContainerSize() - 1;
      if (slot < 0) {
        return;
      }
      ItemStack previous = pack.removeItemNoUpdate(slot);
      displaced.accept(previous);
    }
    pack.setItem(slot, JobTool.SWORD.basicStack());
  }

  /** Keep exactly one carried backup of each missing hand kind while the rest of the pack is shelved. */
  public static boolean stowUnkept(RealPerson person, Set<Item> keeping,
      UnaryOperator<ItemStack> store) {
    if (!usesLoadout(person)) {
      return PackLogistics.stowUnkept(person.personMainInv, keeping, store);
    }
    return stowWithWeapons(person.personMainInv, person.getMainHandItem(), loadout(person), keeping, store);
  }

  static boolean stowWithWeapons(Container pack, ItemStack hand, List<JobTool> loadout, Set<Item> keeping,
      UnaryOperator<ItemStack> store) {
    List<Integer> slots = new ArrayList<>();
    List<ItemStack> weapons = new ArrayList<>();
    for (JobTool tool : loadout) {
      int slot = tool.kind().isInstance(hand.getItem()) ? -1 : find(pack, tool);
      if (slot >= 0) {
        slots.add(slot);
        weapons.add(pack.removeItemNoUpdate(slot));
      }
    }
    try {
      return PackLogistics.stowUnkept(pack, keeping, store);
    } finally {
      for (int index = 0; index < slots.size(); index++) {
        pack.setItem(slots.get(index), weapons.get(index));
      }
    }
  }

  /**
   * Reuse the ordinary stock, upgrade and paid replacement path for each hand
   * in turn. The temporarily parked weapon reserves its own pack slot, so a
   * full pack cannot consume a replacement that there is no room to carry.
   */
  public static void restock(RealPerson person, BlockPos near) {
    provision(person, near, true);
  }

  /** Daytime maintenance restores missing weapons without repeatedly upgrading intact gear. */
  public static void tend(RealPerson person, BlockPos near) {
    if (loadout(person).stream().allMatch(tool -> carries(person, tool))) {
      return;
    }
    provision(person, near, false);
  }

  private static void provision(RealPerson person, BlockPos near, boolean upgrade) {
    if (!usesLoadout(person) || person.getVillage() == null || person.getTarget() != null
        || person.isEating() || person.isGuardChopping()) {
      return;
    }
    try {
      List<JobTool> weapons = loadout(person);
      for (int index = 0; index < weapons.size(); index++) {
        JobTool tool = weapons.get(index);
        if (!readyHand(person, tool)) return;
        provisionHand(person, tool, upgrade, upgrade && index == 0,
            !(isPatrol(person) && tool == JobTool.SWORD), near);
      }
    } finally {
      // Even a full pack or an unavailable sword must leave the carried fallback drawn.
      tick(person);
    }
  }

  private static void provisionHand(RealPerson person, JobTool tool, boolean upgrade, boolean armor,
      boolean replaceMissing, BlockPos near) {
    if (!upgrade && tool.inHand(person)) {
      return;
    }
    person.equipBestPossibleGear(tool.kind(), null, armor, near);
    if (!tool.inHand(person) && replaceMissing) {
      JobTool.replace(person, tool, near);
    }
  }

  /** Put the requested kind in hand, or safely park the old hand before fetching it. */
  private static boolean readyHand(RealPerson person, JobTool tool) {
    if (tool.inHand(person) || draw(person, tool)) {
      return true;
    }
    ItemStack held = person.getMainHandItem();
    if (held.isEmpty()) {
      return true;
    }
    int slot = emptySlot(person.personMainInv);
    if (slot < 0) {
      person.logBlocker("My pack has no room to provision my two guard weapons");
      return false;
    }
    person.personMainInv.setItem(slot, held);
    person.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
    person.clearBlocker("My pack has no room to provision my two guard weapons");
    return true;
  }

  private static boolean draw(RealPerson person, JobTool tool) {
    if (tool.inHand(person)) {
      return true;
    }
    int slot = find(person.personMainInv, tool);
    if (slot < 0) {
      return false;
    }
    person.stopUsingItem();
    person.setChargingCrossbow(false);
    person.setItemSlot(EquipmentSlot.MAINHAND,
        exchange(person.personMainInv, slot, person.getMainHandItem()));
    return true;
  }

  /** The freed slot receives the old hand, even when every pack slot was occupied. */
  static ItemStack exchange(Container pack, int slot, ItemStack hand) {
    ItemStack next = pack.getItem(slot);
    pack.setItem(slot, hand);
    return next;
  }

  private static int find(Container pack, JobTool tool) {
    for (int slot = 0; slot < pack.getContainerSize(); slot++) {
      if (tool.kind().isInstance(pack.getItem(slot).getItem())) {
        return slot;
      }
    }
    return -1;
  }

  private static int emptySlot(Container pack) {
    for (int slot = 0; slot < pack.getContainerSize(); slot++) {
      if (pack.getItem(slot).isEmpty()) {
        return slot;
      }
    }
    return -1;
  }
}
