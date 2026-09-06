package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

class ConstructionAccessTest {
  @Test
  void approachesStayOutsideTheReplacementAndEveryVictim() {
    var boxes = List.of(new BoundingBox(0, 0, 0, 8, 5, 8), new BoundingBox(8, 0, 3, 12, 4, 6));
    var positions = ConstructionAccess.perimeter(boxes);
    assertFalse(positions.isEmpty());
    assertFalse(positions.contains(new BlockPos(9, 0, 4)), "Outside the new house is still inside a victim");
    assertTrue(positions.contains(new BlockPos(-1, 0, 4)));
    assertTrue(positions.stream().allMatch(at -> ConstructionAccess.outside(boxes,
        at.getX() + 0.5, at.getZ() + 0.5, 0.8)));
  }

  @Test
  void wideWorkersNeedBodyClearanceRatherThanJustAnOutsideFootCoordinate() {
    var boxes = List.of(new BoundingBox(0, 0, 0, 8, 5, 8));
    assertTrue(ConstructionAccess.outside(boxes, -0.5, 4.5, 0.8));
    assertFalse(ConstructionAccess.outside(boxes, -0.5, 4.5, 1.2));
    assertTrue(ConstructionAccess.outside(boxes, -1.5, 4.5, 1.2));
    assertFalse(ConstructionAccess.outside(boxes, 4.5, 4.5, 0.6));
  }

  @Test
  void aSafeGapBetweenBuildingsRemainsUsable() {
    var boxes = List.of(new BoundingBox(0, 0, 0, 4, 5, 4), new BoundingBox(8, 0, 0, 12, 4, 4));
    assertTrue(ConstructionAccess.outside(boxes, 6.5, 2.5, 0.8));
    assertTrue(ConstructionAccess.perimeter(boxes).contains(new BlockPos(6, 0, 2)));
  }
}
