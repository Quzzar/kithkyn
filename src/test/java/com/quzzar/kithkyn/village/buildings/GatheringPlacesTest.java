package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.quzzar.kithkyn.village.Village;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import org.junit.jupiter.api.Test;

class GatheringPlacesTest {
  @Test
  void oldDefinitionsKeepTheirSingleFireAndCivicAnchor() {
    BuildingInfo info = definition("\"gathering_point\": [2,1,3]");
    assertFalse(info.hasExplicitMeetingPoint());
    assertEquals(new BlockPos(2,1,3), info.getMeetingPoint());
    assertEquals(List.of(new BlockPos(2,1,3)), info.getCampfireLocations());
  }

  @Test
  void meetingPointAndBothFireCoordinatesSurviveRoundTripAndEveryRotation() {
    BuildingInfo info = definition("\"meeting_point\": [12,1,17], \"campfires\": [[7,9,19],[14,11,24]]");
    info = BuildingInfo.CODEC.parse(JsonOps.INSTANCE,
        BuildingInfo.CODEC.encodeStart(JsonOps.INSTANCE, info).getOrThrow()).getOrThrow();
    assertTrue(info.hasExplicitMeetingPoint());
    for (Rotation rotation : Rotation.values()) {
      BuildingInfo definition = info;
      BlockPos origin = new BlockPos(100, 80, -300);
      Building center = new Building(origin, info.getName(), rotation) {
        @Override public BuildingInfo getInfo() { return definition; }
      };
      Village village = new Village("Two fires") {
        @Override public Building getTownCenter() { return center; }
      };
      assertEquals(origin.offset(new BlockPos(12,1,17).rotate(rotation)), village.getCenterPosition());
      assertEquals(info.getCampfireLocations().stream().map(pos -> origin.offset(pos.rotate(rotation))).toList(),
          village.getCampfirePositions());
    }
  }

  @Test
  void explicitlyEmptyCampfiresDoNotInventOneFromTheLegacyAnchor() {
    BuildingInfo info = definition("\"gathering_point\": [2,1,3], \"meeting_point\": [5,1,5], \"campfires\": []");
    assertEquals(new BlockPos(5,1,5), info.getMeetingPoint());
    assertTrue(info.getCampfireLocations().isEmpty());
  }

  private static BuildingInfo definition(String fields) {
    return BuildingInfo.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
        "{\"structure\":\"village_center_birch_forest_1\"," + fields + "}")).getOrThrow();
  }
}
