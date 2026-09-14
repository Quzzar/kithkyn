package com.quzzar.kithkyn.entities.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class VillageRouteDepthTest {
  @Test
  void surfaceTripsStayAboveTheVillageCaveFloor() {
    BlockPos center = new BlockPos(-1222, 73, 1097);
    BlockPos start = new BlockPos(-1229, 72, 1103);
    BlockPos bedApproach = new BlockPos(-1263, 77, 1131);

    assertEquals(71, VillageRouteDepth.minimumY(center, start, List.of(bedApproach)));
  }

  @Test
  void anUndergroundDestinationStillPermitsAnIntentionalDescent() {
    BlockPos center = new BlockPos(0, 73, 0);
    BlockPos start = new BlockPos(0, 72, 0);
    BlockPos mineFace = new BlockPos(20, 40, 20);

    assertEquals(38, VillageRouteDepth.minimumY(center, start, List.of(mineFace)));
  }

  @Test
  void aTrappedResidentMayRouteUpWithoutDescendingFurther() {
    BlockPos center = new BlockPos(0, 73, 0);
    BlockPos trapped = new BlockPos(10, 54, 10);
    BlockPos surface = new BlockPos(20, 76, 20);

    assertEquals(54, VillageRouteDepth.minimumY(center, trapped, List.of(surface)));
  }
}
