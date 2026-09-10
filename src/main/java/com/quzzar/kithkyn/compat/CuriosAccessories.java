package com.quzzar.kithkyn.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.wrongdoing.EvidenceInventory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotAttribute;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.api.type.capability.ICurio.DropRule;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

/**
 * The Curios-facing half of {@link AccessoryCompat}. Never referenced unless
 * the mod is loaded: touching this class loads Curios types, which do not
 * exist otherwise.
 */
final class CuriosAccessories {

  private CuriosAccessories() {
  }

  /** Display names of every non-empty accessory slot, in slot order. */
  static List<String> worn(LivingEntity entity) {
    List<String> worn = new ArrayList<>();
    CuriosApi.getCuriosInventory(entity).ifPresent(inventory -> {
      IItemHandlerModifiable equipped = inventory.getEquippedCurios();
      for (int slot = 0; slot < equipped.getSlots(); slot++) {
        ItemStack stack = equipped.getStackInSlot(slot);
        if (!stack.isEmpty()) {
          worn.add(stack.getHoverName().getString());
        }
      }
    });
    return worn;
  }

  /**
   * Inserts into the first slot that accepts the stack. Curios validates the
   * item against the slot itself, so an item that belongs nowhere simply comes
   * back untouched.
   */
  static ItemStack equip(LivingEntity entity, ItemStack stack) {
    return CuriosApi.getCuriosInventory(entity).map(inventory -> {
      IItemHandlerModifiable equipped = inventory.getEquippedCurios();
      ItemStack remaining = stack;
      for (int slot = 0; slot < equipped.getSlots() && !remaining.isEmpty(); slot++) {
        if (equipped.getStackInSlot(slot).isEmpty()) {
          remaining = equipped.insertItem(slot, remaining, false);
        }
      }
      return remaining;
    }).orElse(stack);
  }

  private record WornSlot(String identifier, int index, boolean cosmetic, boolean visible,
      IDynamicStackHandler handler, ItemStack stack) {}

  /** Same vanilla equipment modifier view that Curios uses when handling an equipment change. */
  static boolean grantsSlots(ItemStack stack, EquipmentSlot slot) {
    boolean[] grants = {false};
    stack.forEachModifier(slot, (attribute, modifier) -> {
      if (attribute.value() instanceof SlotAttribute) grants[0] = true;
    });
    return grants[0];
  }

  /**
   * Reads actual named functional/cosmetic slots rather than a combined handler whose indices can move.
   * Setting a physical slot leaves previous-stack tracking intact: Curios itself removes modifiers and
   * invokes onUnequip at its next normal tick. No fake death or duplicate equip callbacks are emitted.
   */
  static int confiscate(ServerPlayer player, DamageSource source, List<Container> evidence) {
    Optional<ICuriosItemHandler> found = CuriosApi.getCuriosInventory(player);
    Optional<Boolean> keepsCurios = keepsCurios(player);
    if (found.isEmpty() || keepsCurios.isEmpty()) return 0;
    ICuriosItemHandler inventory = found.get();
    List<WornSlot> slots = new ArrayList<>();
    for (Map.Entry<String, ICurioStacksHandler> entry : inventory.getCurios().entrySet()) {
      snapshot(slots, entry.getKey(), entry.getValue(), false);
      snapshot(slots, entry.getKey(), entry.getValue(), true);
    }
    int moved = 0;
    for (WornSlot slot : slots) {
      ICurioStacksHandler group = inventory.getCurios().get(slot.identifier());
      IDynamicStackHandler current = group == null ? null
          : slot.cosmetic() ? group.getCosmeticStacks() : group.getStacks();
      if (current != slot.handler() || slot.index() >= current.getSlots()
          || !ItemStack.matches(current.getStackInSlot(slot.index()), slot.stack())) continue;
      if (!current.getClass().getName().equals("top.theillusivec4.curios.common.inventory.DynamicStackHandler")) {
        Kithkyn.LOGGER.debug("[custody] Retained accessory in unsupported physical slot handler {}", current.getClass().getName());
        continue;
      }
      SlotContext context = new SlotContext(slot.identifier(), player, slot.index(), slot.cosmetic(), slot.visible());
      try {
        DropRule rule = CuriosApi.getCurio(slot.stack())
            .map(curio -> curio.getDropRule(context, source, player.lastHurtByPlayerTime > 0)).orElse(DropRule.DEFAULT);
        if (rule == DropRule.DEFAULT) {
          rule = CuriosApi.getSlot(slot.identifier(), player.level()).map(ISlotType::getDropRule).orElse(DropRule.DEFAULT);
        }
        if (rule == DropRule.ALWAYS_KEEP || rule == DropRule.DESTROY
            || rule == DropRule.DEFAULT && keepsCurios.get() || !EvidenceInventory.wouldDrop(slot.stack())) continue;
        // Removing a slot-granting item can eject retained/overflow accessories on the following tick.
        if (!slot.cosmetic() && CuriosApi.getAttributeModifiers(context, CuriosApi.getSlotId(context), slot.stack())
            .keySet().stream().anyMatch(attribute -> attribute.value() instanceof SlotAttribute)) continue;
        moved += EvidenceInventory.transfer(slot.stack(), evidence, remainder -> current.setStackInSlot(slot.index(), remainder));
      } catch (RuntimeException failure) {
        Kithkyn.LOGGER.warn("[custody] Retained unsupported Curios slot {}:{} during evidence transfer",
            slot.identifier(), slot.index(), failure);
      }
    }
    return moved;
  }

  private static void snapshot(List<WornSlot> slots, String id, ICurioStacksHandler group, boolean cosmetic) {
    IDynamicStackHandler handler = cosmetic ? group.getCosmeticStacks() : group.getStacks();
    for (int index = 0; index < handler.getSlots(); index++) {
      ItemStack stack = handler.getStackInSlot(index);
      if (!stack.isEmpty()) slots.add(new WornSlot(id, index, cosmetic,
          group.getRenders().size() > index && group.getRenders().get(index), handler, stack.copy()));
    }
  }

  /**
   * Curios' API jar omits its server config type. Read the public field only inside this optional adapter;
   * unknown compatibility implementations retain their items rather than guessing their retention policy.
   * DEFAULT follows vanilla keepInventory; ON and OFF override it for default-rule Curios items.
   */
  private static Optional<Boolean> keepsCurios(ServerPlayer player) {
    try {
      Object server = Class.forName("top.theillusivec4.curios.common.CuriosConfig").getField("SERVER").get(null);
      Object value = server.getClass().getField("keepCurios").get(server);
      if (value instanceof ModConfigSpec.ConfigValue<?> config && config.get() instanceof Enum<?> setting) {
        return switch (setting.name()) {
          case "ON" -> Optional.of(true);
          case "OFF" -> Optional.of(false);
          case "DEFAULT" -> Optional.of(player.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY));
          default -> Optional.empty();
        };
      }
    } catch (ReflectiveOperationException | RuntimeException failure) {
      Kithkyn.LOGGER.warn("[custody] Retained accessories because their provider's death-retention config is unavailable", failure);
    }
    return Optional.empty();
  }

}
