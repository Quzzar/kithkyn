package com.quzzar.kithkyn.village.bookkeeping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import net.minecraft.world.item.Items;

class InternalBookkeeperTest {

  @Test
  void boundsRepeatedMemoriesOfTheSameShortage() {
    InternalBookkeeper bookkeeper = new InternalBookkeeper();
    for (int count = 0; count < 20; count++) {
      bookkeeper.addEvent(new NoResourceBookkeepingEvent(Items.DIRT, 1));
    }
    bookkeeper.addEvent(new NoResourceBookkeepingEvent(Items.OAK_LOG, 1));

    assertEquals(4F, bookkeeper.totalImpact(NoResourceBookkeepingEvent.class));
  }

  @Test
  void doesNotBoundIndependentEvents() {
    InternalBookkeeper bookkeeper = new InternalBookkeeper();
    for (int count = 0; count < 4; count++) {
      bookkeeper.addEvent(new BookkeepingEvent());
    }

    assertEquals(4F, bookkeeper.totalImpact(BookkeepingEvent.class));
  }
}
