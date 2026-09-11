package com.quzzar.kithkyn.raids;

import com.quzzar.kithkyn.entities.RealPerson;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Puts a {@link UndeadRaidPlan.Kit} on a raider: the one place the plan meets real items. */
final class RaiderKit {

  private RaiderKit() {
  }

  static void arm(RealPerson raider, UndeadRaidPlan.Kit kit) {
    raider.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(switch (kit.weapon()) {
      case STONE_SWORD -> Items.STONE_SWORD;
      case IRON_SWORD -> Items.IRON_SWORD;
      case BOW -> Items.BOW;
    }));
    if (kit.shield()) {
      raider.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
    }
    wear(raider, EquipmentSlot.HEAD, kit.helmet(), Items.LEATHER_HELMET, Items.CHAINMAIL_HELMET);
    wear(raider, EquipmentSlot.CHEST, kit.chest(), Items.LEATHER_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE);
    wear(raider, EquipmentSlot.LEGS, kit.legs(), Items.LEATHER_LEGGINGS, Items.CHAINMAIL_LEGGINGS);
    wear(raider, EquipmentSlot.FEET, kit.boots(), Items.LEATHER_BOOTS, Items.CHAINMAIL_BOOTS);
  }

  private static void wear(RealPerson raider, EquipmentSlot slot, UndeadRaidPlan.Armour armour, Item leather, Item chain) {
    switch (armour) {
      case NONE -> {
      }
      case LEATHER -> raider.setItemSlot(slot, new ItemStack(leather));
      case CHAIN -> raider.setItemSlot(slot, new ItemStack(chain));
    }
  }
}
