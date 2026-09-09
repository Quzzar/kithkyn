package com.quzzar.kithkyn.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;

class GuardWeaponsTest {

  @Test
  void weaponExchangeConservesBothStacksAndComponentsEvenWithAFullPack() {
    ItemStack crossbow = new ItemStack(Items.CROSSBOW);
    crossbow.set(DataComponents.CUSTOM_NAME, Component.literal("The village watch"));
    crossbow.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(new ItemStack(Items.SPECTRAL_ARROW)));
    crossbow.setDamageValue(17);
    ItemStack sword = new ItemStack(Items.IRON_SWORD);
    sword.setDamageValue(29);
    SimpleContainer pack = new SimpleContainer(sword, new ItemStack(Items.DIAMOND, 64));

    ItemStack hand = GuardWeapons.exchange(pack, 0, crossbow);
    assertSame(sword, hand);
    assertSame(crossbow, pack.getItem(0));
    hand = GuardWeapons.exchange(pack, 0, hand);

    assertSame(crossbow, hand);
    assertSame(sword, pack.getItem(0));
    assertEquals(17, hand.getDamageValue());
    assertEquals(29, sword.getDamageValue());
    assertFalse(hand.get(DataComponents.CHARGED_PROJECTILES).isEmpty());
    assertEquals("The village watch", hand.getHoverName().getString());
    assertEquals(64, pack.getItem(1).getCount());
  }

  @Test
  void stowKeepsTheBackupWeaponAndDoesNotPocketExtraWeaponsOrOrdinaryGoods() {
    ItemStack backup = new ItemStack(Items.STONE_SWORD);
    SimpleContainer pack = new SimpleContainer(backup, new ItemStack(Items.WHEAT, 12),
        new ItemStack(Items.STONE_SWORD));
    List<ItemStack> stored = new ArrayList<>();

    assertTrue(GuardWeapons.stowWithWeapons(pack, new ItemStack(Items.CROSSBOW),
        List.of(JobTool.CROSSBOW, JobTool.SWORD), Set.of(), stack -> {
      stored.add(stack);
      return ItemStack.EMPTY;
    }));

    assertSame(backup, pack.getItem(0));
    assertTrue(pack.getItem(1).isEmpty());
    assertTrue(pack.getItem(2).isEmpty());
    assertEquals(2, stored.size());
    assertEquals(12, stored.get(0).getCount());
  }

  @Test
  void fullStorageDoesNotLoseBackupOrRejectedGoods() {
    ItemStack crossbow = new ItemStack(Items.CROSSBOW);
    ItemStack goods = new ItemStack(Items.DIAMOND, 64);
    SimpleContainer pack = new SimpleContainer(crossbow, goods);
    assertFalse(GuardWeapons.stowWithWeapons(pack, new ItemStack(Items.STONE_SWORD),
        List.of(JobTool.CROSSBOW, JobTool.SWORD), Set.of(), stack -> stack));
    assertSame(crossbow, pack.getItem(0));
    assertEquals(64, pack.getItem(1).getCount());
  }

  @Test
  void startingSidearmSpillsOneExistingStackIfNecessaryAndNeverDuplicatesASword() {
    ItemStack diamonds = new ItemStack(Items.DIAMOND, 64);
    SimpleContainer pack = new SimpleContainer(diamonds);
    List<ItemStack> displaced = new ArrayList<>();
    GuardWeapons.installSidearm(pack, new ItemStack(Items.CROSSBOW), displaced::add);
    assertTrue(pack.getItem(0).is(Items.STONE_SWORD));
    assertEquals(List.of(diamonds), displaced);
    GuardWeapons.installSidearm(pack, new ItemStack(Items.CROSSBOW), displaced::add);
    assertEquals(1, displaced.size());
    assertEquals(1, pack.getItem(0).getCount());
  }

  @Test
  void selectionHasHysteresisAndDoesNotDrawASwordForHiddenOrDeadTargets() {
    assertTrue(GuardWeapons.wantsSword(false, true, 3, true));
    assertFalse(GuardWeapons.wantsSword(false, true, 3.1, true));
    assertTrue(GuardWeapons.wantsSword(true, true, 3.1, true));
    assertFalse(GuardWeapons.wantsSword(true, true, 4.1, true));
    assertFalse(GuardWeapons.wantsSword(true, true, 2, false));
    assertFalse(GuardWeapons.wantsSword(true, false, 2, true));
  }

  @Test
  void patrolKeepsAxeThroughRestockAndExchangesWithoutLosingDamageOrNames() {
    ItemStack axe = new ItemStack(Items.IRON_AXE);
    axe.setDamageValue(42);
    axe.set(DataComponents.CUSTOM_NAME, Component.literal("Captain's axe"));
    ItemStack sword = new ItemStack(Items.IRON_SWORD);
    SimpleContainer pack = new SimpleContainer(axe, new ItemStack(Items.BIRCH_LOG, 12));
    assertTrue(GuardWeapons.stowWithWeapons(pack, sword, List.of(JobTool.AXE, JobTool.SWORD),
        Set.of(), stack -> ItemStack.EMPTY));
    assertSame(axe, pack.getItem(0));
    assertTrue(pack.getItem(1).isEmpty());
    ItemStack hand = GuardWeapons.exchange(pack, 0, sword);
    assertSame(axe, hand);
    assertSame(sword, pack.getItem(0));
    assertSame(sword, GuardWeapons.exchange(pack, 0, hand));
    assertEquals(42, pack.getItem(0).getDamageValue());
    assertEquals("Captain's axe", pack.getItem(0).getHoverName().getString());
  }

  @Test
  void patrolPrefersSwordExceptDuringChoppingAndCombatInterruptsTheChop() {
    assertEquals(JobTool.SWORD, GuardWeapons.patrolWeapon(false, false, true));
    assertEquals(JobTool.AXE, GuardWeapons.patrolWeapon(true, false, true));
    assertEquals(JobTool.SWORD, GuardWeapons.patrolWeapon(true, true, true));
    assertEquals(JobTool.SWORD, GuardWeapons.patrolWeapon(false, true, true));
    assertEquals(JobTool.AXE, GuardWeapons.patrolWeapon(false, true, false));
    assertEquals(JobTool.AXE, GuardWeapons.patrolWeapon(false, false, false));
  }
}
