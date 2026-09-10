package com.quzzar.kithkyn.savedata;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

class RepairStoreTest {
  private static RepairStore.Repair repair(int x) {
    return new RepairStore.Repair("village", new UUID(0, 1), "house:0:NONE", new BlockPos(x, 64, 0),
        Blocks.DIRT.defaultBlockState(), true);
  }

  @Test void survivingAndPlayerEditedExplosionCandidatesNeverBecomeWork() {
    RepairStore store = new RepairStore();
    store.observeExplosion(repair(1));
    store.observeExplosion(repair(2));
    store.observeExplosion(repair(3));
    store.forget(repair(3).pos());
    store.confirmExplosions(pos -> pos.getX() != 2);
    assertEquals(java.util.List.of(repair(1)), store.candidates("village", 0, 100));
  }

  @Test void missingFirstItemYieldsToLaterWorkAndRetriesAfterItsOwnCooldown() {
    RepairStore store = new RepairStore();
    store.remember(repair(1));
    store.remember(repair(2));
    store.defer(repair(1).pos(), 1200);
    assertEquals(java.util.List.of(repair(2)), store.candidates("village", 0, 1));
    assertEquals(java.util.List.of(repair(1)), store.candidates("village", 1200, 1));
    store.forget(repair(1).pos());
    assertFalse(store.contains(repair(1)));
    assertTrue(store.contains(repair(2)));
  }

  @Test void boundedQueueKeepsOriginalStateAndExcludesOtherVillages() {
    RepairStore store = new RepairStore();
    for (int x = 0; x <= RepairStore.MAX_REPAIRS; x++) store.remember(repair(x));
    assertEquals(RepairStore.MAX_REPAIRS, store.candidates("village", 0, Integer.MAX_VALUE).size());
    assertFalse(store.contains(repair(RepairStore.MAX_REPAIRS)));
    assertTrue(store.candidates("another village", 0, 100).isEmpty());
    store.remember(new RepairStore.Repair("another village", new UUID(0, 2), "other", repair(0).pos(),
        Blocks.STONE.defaultBlockState(), false));
    assertTrue(store.contains(repair(0)));
  }
}
