package com.quzzar.kithkyn.wrongdoing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.quzzar.kithkyn.compat.AccessoryCompat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;

/** Transfers arrest evidence without death events, world drops, or an overflow inventory. */
public final class EvidenceInventory {
  private EvidenceInventory() {}

  /**
   * A distant temporary menu must be able to return its inputs after arrest. Survey cloned evidence and
   * backpack slots with the same transfer routine; never close a menu or change inventories during this check.
   * Unknown menus are treated as temporary, including their output slots, rather than risking item drops.
   */
  public static boolean canArrestWithoutDrops(ServerPlayer player, List<Container> evidence) {
    var menu = player.containerMenu;
    if (menu == player.inventoryMenu) return true;
    if (!menu.getCarried().isEmpty() && !player.inventoryMenu.getCarried().isEmpty()) return false;
    if (menu instanceof net.minecraft.world.inventory.ChestMenu
        || menu instanceof net.minecraft.world.inventory.HopperMenu
        || menu instanceof net.minecraft.world.inventory.DispenserMenu
        || menu instanceof net.minecraft.world.inventory.ShulkerBoxMenu
        || menu instanceof net.minecraft.world.inventory.AbstractFurnaceMenu
        || menu instanceof net.minecraft.world.inventory.BrewingStandMenu
        || menu instanceof net.minecraft.world.inventory.HorseInventoryMenu
        || menu instanceof net.minecraft.world.inventory.LecternMenu
        || menu instanceof net.minecraft.world.inventory.CrafterMenu) return true;
    List<Container> copiedEvidence = new ArrayList<>();
    Set<Container> visited = Collections.newSetFromMap(new IdentityHashMap<>());
    for (Container container : evidence) {
      if (visited.add(container)) copiedEvidence.add(copy(container));
    }
    SimpleContainer backpack = new SimpleContainer(36);
    boolean keepInventory = player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
    for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
      ItemStack[] stack = {player.getInventory().getItem(slot).copy()};
      if (canTakeVanilla(player, slot, keepInventory)) transfer(stack[0], copiedEvidence, remainder -> stack[0] = remainder);
      if (slot < 36) backpack.setItem(slot, stack[0]);
    }
    Map<Container, Set<Integer>> seenSlots = new IdentityHashMap<>();
    for (var slot : menu.slots) {
      if (slot.container == player.getInventory() || slot.container instanceof net.minecraft.world.inventory.ResultContainer
          || !seenSlots.computeIfAbsent(slot.container, ignored -> new java.util.HashSet<>()).add(slot.getContainerSlot())) continue;
      ItemStack[] remainder = {slot.getItem().copy()};
      transfer(remainder[0], List.of(backpack), item -> remainder[0] = item);
      if (!remainder[0].isEmpty()) return false;
    }
    return true;
  }

  private static SimpleContainer copy(Container source) {
    SimpleContainer copy = new SimpleContainer(source.getContainerSize()) {
      @Override public int getMaxStackSize() { return source.getMaxStackSize(); }
    };
    for (int slot = 0; slot < source.getContainerSize(); slot++) copy.setItem(slot, source.getItem(slot).copy());
    return copy;
  }

  /** Confiscates only droppable carried equipment, leaving retained items and capacity overflow on the player. */
  public static int confiscate(ServerPlayer player, DamageSource source, List<Container> evidence) {
    if (player.isSpectator()) return 0;
    boolean keepInventory = player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
    int moved = confiscateVanilla(player, evidence, keepInventory);
    if (!keepInventory) {
      Container crafting = player.inventoryMenu.getCraftSlots();
      for (int slot = 0; slot < crafting.getContainerSize(); slot++) {
        if (!wouldDrop(crafting.getItem(slot))) continue;
        int sourceSlot = slot;
        moved += transfer(crafting.getItem(slot), evidence, remainder -> crafting.setItem(sourceSlot, remainder));
      }
    }
    moved += AccessoryCompat.confiscateDroppable(player, source, evidence);
    var menu = player.containerMenu;
    if (!keepInventory && wouldDrop(menu.getCarried())) moved += transfer(menu.getCarried(), evidence, menu::setCarried);
    if (menu != player.inventoryMenu && !menu.getCarried().isEmpty() && player.inventoryMenu.getCarried().isEmpty()) {
      // Closing a distant chest would otherwise drop this stack when every inventory slot is full.
      // transferState does not replace the inventory menu's cursor.
      player.inventoryMenu.setCarried(menu.getCarried());
      menu.setCarried(ItemStack.EMPTY);
    }
    if (menu != player.inventoryMenu) {
      // Arrest preflight proved that temporary inputs fit in the remaining backpack.
      // Close before syncing the retained cursor so an old empty-cursor update cannot hide it.
      player.closeContainer();
      moved += confiscateVanilla(player, evidence, keepInventory);
      player.inventoryMenu.sendAllDataToRemote();
    }
    if (moved > 0) {
      player.getInventory().setChanged();
      player.inventoryMenu.broadcastChanges();
      player.containerMenu.broadcastChanges();
    }
    return moved;
  }

  /** Shared pass also handles temporary menu inputs after ordinary menu closure returns them to the pack. */
  private static int confiscateVanilla(ServerPlayer player, List<Container> evidence, boolean keepInventory) {
    int moved = 0;
    for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
      if (!canTakeVanilla(player, slot, keepInventory)) continue;
      int sourceSlot = slot;
      moved += transfer(player.getInventory().getItem(slot), evidence,
          remainder -> player.getInventory().setItem(sourceSlot, remainder));
    }
    return moved;
  }

  private static boolean canTakeVanilla(ServerPlayer player, int slot, boolean keepInventory) {
    ItemStack stack = player.getInventory().getItem(slot);
    if (keepInventory || !wouldDrop(stack)) return false;
    EquipmentSlot equipment = equipmentSlot(slot, player.getInventory().selected);
    return equipment == null || !AccessoryCompat.grantsAccessorySlots(stack, equipment);
  }

  /** Only equipped vanilla slots can currently affect the accessory inventory's capacity. */
  static EquipmentSlot equipmentSlot(int inventorySlot, int selectedHotbar) {
    if (inventorySlot == selectedHotbar) return EquipmentSlot.MAINHAND;
    return switch (inventorySlot) {
      case 36 -> EquipmentSlot.FEET;
      case 37 -> EquipmentSlot.LEGS;
      case 38 -> EquipmentSlot.CHEST;
      case 39 -> EquipmentSlot.HEAD;
      case 40 -> EquipmentSlot.OFFHAND;
      default -> null;
    };
  }

  /** Vanishing equipment would be destroyed on death, so it is not evidence from a surviving prisoner. */
  public static boolean wouldDrop(ItemStack stack) {
    return !stack.isEmpty() && !EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP);
  }

  private record Placement(Container container, int slot, ItemStack value) {}

  /**
   * Plans actual free capacity before removing anything. The source writer must update the same stable slot;
   * Curios uses its physical handler and lets its own tick process equipment callbacks exactly once.
   * Called on the server thread with the castle's ordinary barrel containers.
   */
  public static int transfer(ItemStack source, List<Container> evidence, Consumer<ItemStack> writeSource) {
    if (source.isEmpty()) return 0;
    ItemStack remainder = source.copy();
    List<Placement> placements = new ArrayList<>();
    Set<Container> visited = Collections.newSetFromMap(new IdentityHashMap<>());
    for (Container container : evidence) {
      if (!visited.add(container)) continue;
      for (int slot = 0; slot < container.getContainerSize() && !remainder.isEmpty(); slot++) {
        ItemStack stored = container.getItem(slot);
        if (!container.canPlaceItem(slot, remainder)
            || !stored.isEmpty() && !ItemStack.isSameItemSameComponents(stored, remainder)) continue;
        int room = Math.min(container.getMaxStackSize(), remainder.getMaxStackSize()) - stored.getCount();
        int accepted = Math.min(remainder.getCount(), Math.max(0, room));
        if (accepted == 0) continue;
        placements.add(new Placement(container, slot, remainder.copyWithCount(stored.getCount() + accepted)));
        remainder.shrink(accepted);
      }
    }
    int moved = source.getCount() - remainder.getCount();
    if (moved == 0) return 0;
    writeSource.accept(remainder);
    for (Placement placement : placements) {
      placement.container().setItem(placement.slot(), placement.value());
      placement.container().setChanged();
    }
    return moved;
  }
}
