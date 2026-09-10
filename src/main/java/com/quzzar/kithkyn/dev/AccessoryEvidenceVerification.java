package com.quzzar.kithkyn.dev;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.compat.AccessoryCompat;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Opt-in Curios integration fixture, only in a disposable world with Curios 9.5.1 installed. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class AccessoryEvidenceVerification {
  private static int ticks;
  private AccessoryEvidenceVerification() {}

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (!Boolean.getBoolean("kithkyn.accessoryEvidence.verify") || ++ticks != 40) return;
    try {
      check(AccessoryCompat.isPresent(), "Curios is required for this opt-in fixture");
      Probe.verify(event.getServer().overworld());
      Kithkyn.LOGGER.info("[accessory-evidence-verify] RESULT PASS: physical and cosmetic slots, retention, overflow, components and equipment lifecycle");
    } catch (Exception | AssertionError failure) {
      Kithkyn.LOGGER.error("[accessory-evidence-verify] RESULT FAIL", failure);
    } finally {
      event.getServer().halt(false);
    }
  }

  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  /** Optional provider types stay out of the subscriber class loaded in ordinary non-Curios games. */
  private static final class Probe {
    private static int unequipped;

    private static void verify(ServerLevel level) {
      var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
      player.getInventory().clearContent();
      level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY).set(false, level.getServer());
      var inventory = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).orElseThrow();
      var ring = inventory.getStacksHandler("ring").orElseThrow();
      check(ring.getStacks().getSlots() >= 4 && ring.getCosmeticStacks().getSlots() >= 1, "fixture ring slots missing");
      var item = net.minecraft.world.item.Items.GOLD_NUGGET;
      top.theillusivec4.curios.api.CuriosApi.registerCurio(item, new top.theillusivec4.curios.api.type.capability.ICurioItem() {
        @Override
        public com.google.common.collect.Multimap<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>,
            net.minecraft.world.entity.ai.attributes.AttributeModifier> getAttributeModifiers(
            top.theillusivec4.curios.api.SlotContext context, net.minecraft.resources.ResourceLocation id,
            net.minecraft.world.item.ItemStack stack) {
          var modifiers = com.google.common.collect.HashMultimap
              .<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>, net.minecraft.world.entity.ai.attributes.AttributeModifier>create();
          modifiers.put(net.minecraft.world.entity.ai.attributes.Attributes.LUCK,
              new net.minecraft.world.entity.ai.attributes.AttributeModifier(id, 1,
                  net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
          return modifiers;
        }

        @Override
        public void onUnequip(top.theillusivec4.curios.api.SlotContext context,
            net.minecraft.world.item.ItemStack replacement, net.minecraft.world.item.ItemStack stack) {
          unequipped++;
        }
      });
      registerRule(net.minecraft.world.item.Items.APPLE, top.theillusivec4.curios.api.type.capability.ICurio.DropRule.ALWAYS_KEEP);
      registerRule(net.minecraft.world.item.Items.BREAD, top.theillusivec4.curios.api.type.capability.ICurio.DropRule.DESTROY);
      var ordinary = new net.minecraft.world.item.ItemStack(item, 3);
      ordinary.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Remembered ring"));
      ring.getStacks().setStackInSlot(0, ordinary.copy());
      ring.getStacks().setStackInSlot(1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.APPLE));
      ring.getStacks().setStackInSlot(2, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD));
      var slotGrant = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD);
      top.theillusivec4.curios.api.CuriosApi.addSlotModifier(slotGrant, "ring",
          net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("kithkyn", "evidence_fixture_extra_ring"), 1,
          net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE, "ring");
      ring.getStacks().setStackInSlot(3, slotGrant);
      var cosmetic = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND);
      cosmetic.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Cosmetic keepsake"));
      ring.getCosmeticStacks().setStackInSlot(0, cosmetic.copy());
      double baseLuck = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.LUCK);
      nativeTick(player);
      check(player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.LUCK) == baseLuck + 1,
          "Curios did not apply equipped accessory modifier");
      check(ring.getStacks().getSlots() == 5, "test accessory did not grant an extra ring slot");

      var first = new net.minecraft.world.SimpleContainer(1);
      first.setItem(0, ordinary.copyWithCount(63));
      int moved = com.quzzar.kithkyn.wrongdoing.EvidenceInventory.confiscate(player, player.damageSources().generic(), java.util.List.of(first));
      check(moved == 1 && first.getItem(0).getCount() == 64 && ring.getStacks().getStackInSlot(0).getCount() == 2,
          "partial capacity did not retain exact accessory overflow");
      check(net.minecraft.world.item.ItemStack.isSameItemSameComponents(ordinary, first.getItem(0)), "accessory components changed");
      check(!ring.getCosmeticStacks().getStackInSlot(0).isEmpty(), "full evidence removed cosmetic overflow");
      nativeTick(player);
      int beforeRemoval = unequipped;
      var second = new net.minecraft.world.SimpleContainer(4);
      moved = com.quzzar.kithkyn.wrongdoing.EvidenceInventory.confiscate(player, player.damageSources().generic(), java.util.List.of(second));
      Kithkyn.LOGGER.info("[accessory-evidence-verify] second transfer moved={} worn={} cosmetic={} evidence={}", moved,
          java.util.stream.IntStream.range(0, ring.getStacks().getSlots()).mapToObj(slot -> ring.getStacks().getStackInSlot(slot).toString()).toList(),
          ring.getCosmeticStacks().getStackInSlot(0),
          java.util.stream.IntStream.range(0, second.getContainerSize()).mapToObj(slot -> second.getItem(slot).toString()).toList());
      check(moved == 3 && ring.getStacks().getStackInSlot(0).isEmpty() && ring.getCosmeticStacks().getStackInSlot(0).isEmpty(),
          "ordinary and cosmetic accessories did not move to evidence");
      check(!ring.getStacks().getStackInSlot(1).isEmpty() && !ring.getStacks().getStackInSlot(2).isEmpty(),
          "death-retained or destroyed accessories were confiscated");
      check(!ring.getStacks().getStackInSlot(3).isEmpty(), "slot-granting accessory was removed");
      nativeTick(player);
      nativeTick(player);
      check(unequipped == beforeRemoval + 1, "onUnequip did not run exactly once for actual removal");
      check(player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.LUCK) == baseLuck,
          "Curios left the removed accessory modifier active");
      player.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
      level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY).set(true, level.getServer());
      check(com.quzzar.kithkyn.wrongdoing.EvidenceInventory.confiscate(player, player.damageSources().generic(), java.util.List.of(second)) == 0,
          "keepInventory did not protect inventory");
      check(!player.getInventory().getItem(0).isEmpty(), "keepInventory lost vanilla equipment");
      registerRule(net.minecraft.world.item.Items.COAL, top.theillusivec4.curios.api.type.capability.ICurio.DropRule.ALWAYS_DROP);
      ring.getStacks().setStackInSlot(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COAL));
      check(com.quzzar.kithkyn.wrongdoing.EvidenceInventory.confiscate(player, player.damageSources().generic(), java.util.List.of(second)) == 1,
          "Curios ALWAYS_DROP failed to override keepInventory");
      ring.getStacks().setStackInSlot(0, ordinary.copyWithCount(1));
      check(com.quzzar.kithkyn.wrongdoing.EvidenceInventory.confiscate(player, player.damageSources().generic(), java.util.List.of(second)) == 0,
          "Curios DEFAULT did not inherit keepInventory");
      setKeepCurios("OFF");
      check(com.quzzar.kithkyn.wrongdoing.EvidenceInventory.confiscate(player, player.damageSources().generic(), java.util.List.of(second)) == 1,
          "Curios OFF config did not override keepInventory");
      setKeepCurios("ON");
      level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY).set(false, level.getServer());
      ring.getStacks().setStackInSlot(0, ordinary.copyWithCount(1));
      player.getInventory().clearContent();
      check(com.quzzar.kithkyn.wrongdoing.EvidenceInventory.confiscate(player, player.damageSources().generic(), java.util.List.of(second)) == 0,
          "Curios ON config did not retain default accessories");
      setKeepCurios("DEFAULT");
      var temporary = new net.minecraft.world.inventory.AnvilMenu(99, player.getInventory(),
          net.minecraft.world.inventory.ContainerLevelAccess.create(level, net.minecraft.core.BlockPos.ZERO));
      player.containerMenu = temporary;
      temporary.getSlot(0).set(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
      temporary.setCarried(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND));
      for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COBBLESTONE, 64));
      var full = new net.minecraft.world.SimpleContainer(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIRT, 64));
      check(!com.quzzar.kithkyn.wrongdoing.EvidenceInventory.canArrestWithoutDrops(player, java.util.List.of(full)),
          "overflowing temporary input menu was accepted");
      check(temporary.getSlot(0).hasItem() && temporary.getCarried().getCount() == 1 && player.getInventory().getItem(0).getCount() == 64,
          "menu preflight mutated real items");
      var space = new net.minecraft.world.SimpleContainer(1);
      check(com.quzzar.kithkyn.wrongdoing.EvidenceInventory.canArrestWithoutDrops(player, java.util.List.of(space)),
          "temporary inputs could not reuse a backpack slot freed by evidence");
      com.quzzar.kithkyn.wrongdoing.EvidenceInventory.confiscate(player, player.damageSources().generic(), java.util.List.of(space));
      check(temporary.getCarried().isEmpty() && player.inventoryMenu.getCarried().is(net.minecraft.world.item.Items.DIAMOND),
          "overflowing cursor was not parked on the player's inventory menu");
      check(player.containerMenu == player.inventoryMenu, "old temporary menu did not close during arrest");
      check(player.getInventory().countItem(net.minecraft.world.item.Items.IRON_SWORD) == 1,
          "temporary input failed to return to the newly freed inventory slot");
      check(player.inventoryMenu.getCarried().is(net.minecraft.world.item.Items.DIAMOND),
          "closing old menu dropped the retained cursor");
      player.inventoryMenu.setCarried(net.minecraft.world.item.ItemStack.EMPTY);
      player.getInventory().clearContent();
      var another = new net.minecraft.world.inventory.AnvilMenu(100, player.getInventory(),
          net.minecraft.world.inventory.ContainerLevelAccess.create(level, net.minecraft.core.BlockPos.ZERO));
      player.containerMenu = another;
      var keepsake = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
      keepsake.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Anvil keepsake"));
      another.getSlot(0).set(keepsake.copy());
      var ample = new net.minecraft.world.SimpleContainer(10);
      check(com.quzzar.kithkyn.wrongdoing.EvidenceInventory.canArrestWithoutDrops(player, java.util.List.of(ample)),
          "empty backpack rejected ordinary temporary inputs");
      com.quzzar.kithkyn.wrongdoing.EvidenceInventory.confiscate(player, player.damageSources().generic(), java.util.List.of(ample));
      check(ample.countItem(net.minecraft.world.item.Items.IRON_SWORD) == 1
          && java.util.stream.IntStream.range(0, ample.getContainerSize()).anyMatch(slot -> net.minecraft.world.item.ItemStack.matches(keepsake, ample.getItem(slot))),
          "returned temporary input did not reach available evidence with its components intact");
      var crafting = player.inventoryMenu.getCraftSlots();
      for (int slot = 0; slot < crafting.getContainerSize(); slot++) {
        var input = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHERITE_SCRAP, 2);
        input.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Craft input " + slot));
        crafting.setItem(slot, input);
      }
      var craftEvidence = new net.minecraft.world.SimpleContainer(1);
      check(com.quzzar.kithkyn.wrongdoing.EvidenceInventory.confiscate(player, player.damageSources().generic(), java.util.List.of(craftEvidence)) == 2,
          "inventory crafting input did not use available evidence capacity");
      check(crafting.getItem(0).isEmpty() && crafting.getItem(1).getCount() == 2 && crafting.getItem(2).getCount() == 2
          && crafting.getItem(3).getCount() == 2 && craftEvidence.countItem(net.minecraft.world.item.Items.NETHERITE_SCRAP) == 2,
          "inventory crafting overflow was not retained in its exact original slots");
    }

    private static void registerRule(net.minecraft.world.item.Item item,
        top.theillusivec4.curios.api.type.capability.ICurio.DropRule rule) {
      top.theillusivec4.curios.api.CuriosApi.registerCurio(item, new top.theillusivec4.curios.api.type.capability.ICurioItem() {
        @Override public top.theillusivec4.curios.api.type.capability.ICurio.DropRule getDropRule(
            top.theillusivec4.curios.api.SlotContext context, net.minecraft.world.damagesource.DamageSource source,
            boolean recentlyHit, net.minecraft.world.item.ItemStack stack) { return rule; }

        /** The actual 9.5.1 ItemizedCurioCapability still invokes this overload internally. */
        @Override public top.theillusivec4.curios.api.type.capability.ICurio.DropRule getDropRule(
            top.theillusivec4.curios.api.SlotContext context, net.minecraft.world.damagesource.DamageSource source,
            int looting, boolean recentlyHit, net.minecraft.world.item.ItemStack stack) { return rule; }
      });
    }

    private static void nativeTick(net.minecraft.server.level.ServerPlayer player) {
      net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(player));
    }

    /** Sets the optional provider's server config only inside this disposable integration fixture. */
    private static void setKeepCurios(String setting) {
      try {
        Object config = Class.forName("top.theillusivec4.curios.common.CuriosConfig").getField("SERVER").get(null);
        Object value = config.getClass().getField("keepCurios").get(config);
        Class<?> enumType = Class.forName("top.theillusivec4.curios.common.CuriosConfig$KeepCurios");
        Object selected = java.util.Arrays.stream(enumType.getEnumConstants()).filter(item -> ((Enum<?>) item).name().equals(setting)).findFirst().orElseThrow();
        value.getClass().getMethod("set", Object.class).invoke(value, selected);
      } catch (ReflectiveOperationException failure) {
        throw new AssertionError("could not configure native Curios retention test", failure);
      }
    }
  }
}
