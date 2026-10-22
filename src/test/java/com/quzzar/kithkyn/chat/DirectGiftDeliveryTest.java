package com.quzzar.kithkyn.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

class DirectGiftDeliveryTest {

  @Test
  void insertsWhatFitsAndReturnsOnlyTheOverflow() {
    ItemStack gift = new ItemStack(Items.DIRT, 12);

    DirectGiftDelivery.Result result = DirectGiftDelivery.insert(gift, stack -> stack.shrink(8));

    assertEquals(8, result.inserted());
    assertEquals(4, result.overflow().getCount());
  }

  @Test
  void aGiftThatFitsLeavesNothingToDrop() {
    ItemStack gift = new ItemStack(Items.DIRT, 12);

    DirectGiftDelivery.Result result = DirectGiftDelivery.insert(gift, stack -> stack.setCount(0));

    assertEquals(12, result.inserted());
    assertTrue(result.overflow().isEmpty());
  }

  @Test
  void aResidentMayChooseAnUnrequestedValuableGift() {
    var reply = PersonChatDispatcher.parseReply(
        "{\"say\":\"Thank you for coming. These are for you.\",\"give\":\"minecraft:diamond\",\"give_count\":2}",
        "It is nice to see you again.");
    org.junit.jupiter.api.Assertions.assertNotNull(reply);
    assertEquals("minecraft:diamond", reply.give());
    assertEquals(2, reply.giveCount());
  }

  @Test
  void transfersPreserveComponentsAndDoNotMergeDifferentVariants() {
    ItemStack named = new ItemStack(Items.DIAMOND_SWORD);
    named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
        net.minecraft.network.chat.Component.literal("For the road"));
    ItemStack gift = PersonChatDispatcher.takeInto(ItemStack.EMPTY, named, 64, 1);
    assertTrue(named.isEmpty());
    assertEquals(1, gift.getCount());
    assertEquals("For the road", gift.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME).getString());
    ItemStack ordinary = new ItemStack(Items.DIAMOND_SWORD);
    assertEquals(1, PersonChatDispatcher.takeInto(gift, ordinary, 2, 1).getCount());
    assertEquals(1, ordinary.getCount());
  }

  @Test
  void offersBoundTheChosenVariantWithoutBlockingValuablesOrEquipment() {
    ItemStack named = new ItemStack(Items.DIAMOND_SWORD);
    named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
        net.minecraft.network.chat.Component.literal("For the road"));
    var offers = DirectGiftDelivery.offers(java.util.List.of(
        new ItemStack(Items.DIAMOND, 3), named, new ItemStack(Items.DIAMOND_SWORD)), ItemStack::getCount);
    assertEquals(java.util.Map.of("minecraft:diamond", 3, "minecraft:diamond_sword", 1), offers);
    assertEquals(1, named.getCount(), "A preview must not take the item");
  }

  @Test
  void aCancelledOverflowDropIsReturnedAndCannotBecomeACompletedGift() {
    ItemStack gift = new ItemStack(Items.DIAMOND, 3);
    var returned = new java.util.concurrent.atomic.AtomicInteger();
    var result = DirectGiftDelivery.deliver(gift, stack -> stack.shrink(1), overflow -> false,
        overflow -> { returned.set(overflow.getCount()); overflow.setCount(0); });
    assertEquals(1, result.handedOver()); assertEquals(0, result.placed());
    assertEquals(2, result.returned()); assertEquals(2, returned.get());
    assertTrue(DirectGiftDelivery.receipt("Rowan", "minecraft:diamond", 3, result).contains("I handed 1 minecraft:diamond"));
  }

  @Test
  void overflowAtTheRecipientsFeetIsDistinctFromInventoryDelivery() {
    ItemStack gift = new ItemStack(Items.DIAMOND, 3);
    var result = DirectGiftDelivery.deliver(gift, stack -> stack.shrink(1), overflow -> true,
        overflow -> org.junit.jupiter.api.Assertions.fail("A spawned gift must not be returned or duplicated"));
    assertEquals(1, result.inserted()); assertEquals(2, result.placed()); assertEquals(3, result.handedOver());
    assertEquals(0, result.returned());
  }

  @Test
  void anUnavailableVariantDoesNotHideAnotherCarriedVariant() {
    ItemStack kept = new ItemStack(Items.BREAD);
    kept.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Kept"));
    var offers = DirectGiftDelivery.offers(java.util.List.of(kept, new ItemStack(Items.BREAD, 2)),
        stack -> stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME) ? 0 : stack.getCount());
    assertEquals(java.util.Map.of("minecraft:bread", 2), offers);
  }
}
