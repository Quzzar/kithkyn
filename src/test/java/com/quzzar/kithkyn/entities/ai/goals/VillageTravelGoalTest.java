package com.quzzar.kithkyn.entities.ai.goals;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class VillageTravelGoalTest {
  @Test
  void travelRejectsACaveEndpointFarFromTheAssignedWorkplace() {
    BlockPos workplace = new BlockPos(-1264, 78, 1132);

    assertFalse(VillageTravelGoal.acceptsTravelEndpoint(
        workplace, new BlockPos(-1242, 71, 1115)));
    assertTrue(VillageTravelGoal.acceptsTravelEndpoint(
        workplace, new BlockPos(-1263, 78, 1131)));
  }
}
