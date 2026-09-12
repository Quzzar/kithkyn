package com.quzzar.kithkyn.entities.ai;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;

class LongRetryMemoTest {

  private static final Set<BlockPos> POST = Set.of(new BlockPos(10, 70, 10));
  private static final BlockPos STUCK = new BlockPos(0, 64, 0);

  @Test
  void aFreshMemoAlwaysRetries() {
    assertTrue(new LongRetryMemo().worthRetrying(POST, STUCK, 0L));
  }

  @Test
  void aFailureFromTheSameSpotIsNotRepeatedWithinTheCooldown() {
    LongRetryMemo memo = new LongRetryMemo();
    memo.failed(POST, STUCK, 1_000L);
    assertFalse(memo.worthRetrying(POST, STUCK, 1_010L));
    assertFalse(memo.worthRetrying(POST, STUCK.offset(1, 0, 1), 1_099L));
    assertTrue(memo.worthRetrying(POST, STUCK, 1_000L + LongRetryMemo.COOLDOWN_TICKS));
  }

  @Test
  void movingOrWantingSomewhereElseEarnsAFreshRetry() {
    LongRetryMemo memo = new LongRetryMemo();
    memo.failed(POST, STUCK, 1_000L);
    assertTrue(memo.worthRetrying(POST, STUCK.offset(LongRetryMemo.MOVED_BLOCKS, 0, 0), 1_010L));
    assertTrue(memo.worthRetrying(Set.of(new BlockPos(40, 70, 40)), STUCK, 1_010L));
  }

  @Test
  void reachingTheTargetClearsTheMemory() {
    LongRetryMemo memo = new LongRetryMemo();
    memo.failed(POST, STUCK, 1_000L);
    memo.reached();
    assertTrue(memo.worthRetrying(POST, STUCK, 1_001L));
  }
}
