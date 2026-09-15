package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.block.Rotation;
import org.junit.jupiter.api.AfterEach;

class ConstructionAccessTest {
  @AfterEach
  void clearRegistry() {
    Buildings.reload(Map.of());
  }

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

  @Test
  void freshConstructionApproachesTheAuthoredGroundPlane() {
    BuildingInfo info = BuildingInfo.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
        {"structure":"well_test_1","sink":1}
        """)).getOrThrow();
    Buildings.reload(Map.of(info.getName(), info));
    Building building = new Building(new BlockPos(100, 63, 200), info.getName(), Rotation.NONE);
    StructureInProgress project = new StructureInProgress(building, new Random(1));

    assertEquals(64, ConstructionAccess.groundLevel(project));
  }
}
