package com.quzzar.kithkyn.entities.ai.goals.work;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class CampfireAccessTest {
  @Test
  void skipsUnavailableAndUnreachableFiresBeforeTryingTheNextNearest() {
    BlockPos unlit = new BlockPos(1, 0, 0);
    BlockPos blocked = new BlockPos(2, 0, 0);
    BlockPos usable = new BlockPos(3, 0, 0);
    List<BlockPos> pathChecks = new ArrayList<>();
    CampfireAccess.Target target = CampfireAccess.nearest(BlockPos.ZERO,
        List.of(usable, blocked, unlit), fire -> !fire.equals(unlit), fire -> {
          pathChecks.add(fire);
          return fire.equals(blocked) ? null : fire.north();
        });
    assertEquals(new CampfireAccess.Target(usable, usable.north()), target);
    assertEquals(List.of(blocked, usable), pathChecks);
  }

  @Test
  void noUsableFireProducesNoVisitRatherThanDefaultingToTheMeetingPoint() {
    assertNull(CampfireAccess.nearest(BlockPos.ZERO, List.of(new BlockPos(1, 0, 0)),
        fire -> false, fire -> fire.north()));
    assertNull(CampfireAccess.nearest(BlockPos.ZERO, List.of(new BlockPos(1, 0, 0)),
        fire -> true, fire -> null));
  }
}
