package com.quzzar.kithkyn.chat;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ChatGiftPolicyTest {
  @Test void onlyExplicitRequestsForTheNamedItemAuthorizeGifts() {
    assertTrue(ChatGiftPolicy.requested("minecraft:bread", "Could you spare one bread from your pockets?"));
    assertTrue(ChatGiftPolicy.requested("bread", "Can I have some bread?"));
    assertTrue(ChatGiftPolicy.requested("minecraft:oak_log", "Please give me two oak logs."));
    assertFalse(ChatGiftPolicy.requested("minecraft:bread", "Did you give me bread earlier?"));
    assertFalse(ChatGiftPolicy.requested("minecraft:bread", "What did you notice nearby?"));
    assertFalse(ChatGiftPolicy.requested("minecraft:bread", "Could you give me dirt?"));
    assertFalse(ChatGiftPolicy.requested("minecraft:bread", "Tell me about breadfruit."));
    assertFalse(ChatGiftPolicy.requested("minecraft:bread", "Can I have your opinion on bread?"));
    assertFalse(ChatGiftPolicy.requested("minecraft:bread", "Could you give me a description of bread?"));
    assertFalse(ChatGiftPolicy.requested("minecraft:bread", "Could you give me dirt, not bread?"));
    assertFalse(ChatGiftPolicy.requested("minecraft:bread", "Could you give me bread if I ask later?"));
    assertFalse(ChatGiftPolicy.requested("minecraft:bread", "I need no bread."));
  }
  @Test void preciousGiftsCannotBeAuthorizedEvenWhenRequested() {
    assertFalse(ChatGiftPolicy.requested("minecraft:diamond", "Could you give me one diamond?"));
    assertNull(PersonChatDispatcher.parseReply("{\"say\":\"I noticed you talking.\",\"give\":\"minecraft:diamond\",\"give_count\":3}", "What did you notice nearby?"));
    assertNull(PersonChatDispatcher.parseReply("{\"say\":\"Here.\",\"give\":\"minecraft:bread\",\"give_count\":1}", "Did you heal her?"));
    assertNotNull(PersonChatDispatcher.parseReply("{\"say\":\"Here.\",\"give\":\"minecraft:bread\",\"give_count\":1}", "Could you spare one bread?"));
  }
  @Test void transfersPreserveComponentsAndDoNotMergeDifferentVariants() {
    var named = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD, 3);
    named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("For the road"));
    var gift = PersonChatDispatcher.takeInto(net.minecraft.world.item.ItemStack.EMPTY, named, 2, 3);
    assertEquals(1, named.getCount()); assertEquals(2, gift.getCount());
    assertEquals("For the road", gift.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME).getString());
    var ordinary = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD, 3);
    assertEquals(2, PersonChatDispatcher.takeInto(gift, ordinary, 3, 3).getCount());
    assertEquals(3, ordinary.getCount());
  }

}
