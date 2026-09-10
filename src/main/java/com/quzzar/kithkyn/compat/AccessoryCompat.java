package com.quzzar.kithkyn.compat;

import java.util.List;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.fml.ModList;

/**
 * Optional integration with accessory-slot mods, so a villager knows about
 * (and can wear) everything on their person, not just vanilla hands and armor
 * (conversation map #46).
 *
 * Curios is the target: in every configuration where accessory slots exist on
 * NeoForge 1.21.1, mod id {@code curios} is loaded. Plain Curios provides it;
 * the Accessories compatibility layer installs real Curios and redirects its
 * calls into Accessories-backed storage. An Accessories-only install with no
 * compatibility layer is the one gap, and it reads here as "no accessories".
 *
 * Every Curios type reference lives in {@link CuriosAccessories}, which is
 * touched only inside a presence check, so the class never loads and this
 * facade is inert when the mod is absent.
 */
public final class AccessoryCompat {

  private static final String CURIOS_MOD_ID = "curios";

  private static Boolean present;

  private AccessoryCompat() {
  }

  /** True when an accessory-slot provider is installed. Resolved once. */
  public static boolean isPresent() {
    if (present == null) {
      present = ModList.get().isLoaded(CURIOS_MOD_ID);
    }
    return present;
  }

  /**
   * Human-readable names of what the entity is wearing in accessory slots,
   * for the conversation briefing. Empty when nothing is worn or no provider
   * is installed.
   */
  public static List<String> wornAccessories(LivingEntity entity) {
    if (!isPresent()) {
      return List.of();
    }
    return CuriosAccessories.worn(entity);
  }

  /**
   * Tries to wear the stack in a free accessory slot, returning what is left
   * over. Returns the stack untouched when no provider is installed, when the
   * item belongs in no slot, or when every slot it fits is occupied: the
   * caller then handles it as ordinary inventory.
   */
  public static ItemStack equipIfPossible(LivingEntity entity, ItemStack stack) {
    if (!isPresent() || stack.isEmpty()) {
      return stack;
    }
    return CuriosAccessories.equip(entity, stack);
  }

  /** Moves death-droppable accessories into evidence while preserving retained and unsafe dynamic slots. */
  public static int confiscateDroppable(ServerPlayer player, DamageSource source, List<Container> evidence) {
    return isPresent() ? CuriosAccessories.confiscate(player, source, evidence) : 0;
  }

  /** Vanilla equipment can also grant Curios slots; keep it equipped when removal could eject overflow. */
  public static boolean grantsAccessorySlots(ItemStack stack, EquipmentSlot slot) {
    return isPresent() && CuriosAccessories.grantsSlots(stack, slot);
  }

}
