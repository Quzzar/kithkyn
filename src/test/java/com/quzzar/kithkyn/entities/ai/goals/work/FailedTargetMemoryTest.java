package com.quzzar.kithkyn.entities.ai.goals.work;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;

class FailedTargetMemoryTest {

  @Test
  void anUnreachableTargetIsSkippedUntilItsRetryWindowExpires() {
    FailedTargetMemory memory = new FailedTargetMemory(2_400);
    BlockPos target = new BlockPos(4, 70, -3);

    memory.reject(target, 100);

    assertTrue(memory.contains(target, 2_499));
    assertFalse(memory.contains(target, 2_500));
  }
}
