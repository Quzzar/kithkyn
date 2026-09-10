package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.quzzar.kithkyn.village.Occupation;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PhysicalWorksiteTest {
  @AfterEach
  void clearRegistry() {
    Buildings.reload(Map.of());
  }

  @Test
  void routedPostsAndNonVacancyWorksitesSurviveTheDefinitionCodec() {
    BuildingInfo center = definition("""
        {"structure":"village_center_jungle_1","work_stations":[
          {"pos":[4,1,4],"occupation":"MINER","worksite_category":"mine"}
        ]}
        """);
    BuildingInfo mine = definition("""
        {"structure":"mine_jungle_1","worksites":[
          {"pos":[2,0,4],"occupation":"MINER"}
        ],"mine_entrance":{"facing":"south","offset":[0,0,1],"width":3}}
        """);

    assertEquals("mine", center.getWorksiteCategory(center.getWorkLocations().keySet().iterator().next()));
    assertTrue(mine.getWorkLocations().isEmpty(), "A physical worksite must not create another job vacancy");
    assertEquals(Occupation.MINER, mine.getWorksiteLocations().values().iterator().next());

    BuildingInfo restored = BuildingInfo.CODEC.parse(JsonOps.INSTANCE,
        BuildingInfo.CODEC.encodeStart(JsonOps.INSTANCE, mine).getOrThrow()).getOrThrow();
    assertEquals(mine.getWorksiteLocations(), restored.getWorksiteLocations());
  }

  @Test
  void aPhysicalMineWorksiteOwnsTheSameShaftFrameAsAnOrdinaryMinePost() {
    BuildingInfo mine = definition("""
        {"structure":"mine_jungle_1","worksites":[
          {"pos":[2,0,4],"occupation":"MINER"}
        ],"mine_entrance":{"facing":"south","offset":[0,0,1],"width":3}}
        """);
    Buildings.reload(Map.of(mine.getName(), mine));
    Building building = new Building(new BlockPos(100, 64, 200), mine.getName(), Rotation.CLOCKWISE_90);

    MineShaft shaft = MineShaft.of(building).getFirst();
    assertEquals(new BlockPos(95, 64, 202), shaft.mouth());
    assertEquals(1, shaft.radius());
    assertEquals(BlockPos.asLong(2, 0, 4), shaft.rootStation());
  }

  @Test
  void aRoutedMinerVacancyDoesNotExcavateItsCivicBuilding() {
    BuildingInfo center = definition("""
        {"structure":"village_center_jungle_1","work_stations":[
          {"pos":[4,1,4],"occupation":"MINER","worksite_category":"mine"}
        ]}
        """);
    Buildings.reload(Map.of(center.getName(), center));
    Building building = new Building(new BlockPos(100, 64, 200), center.getName(), Rotation.NONE);

    assertTrue(MineShaft.of(building).isEmpty(), "The separate mine building must own the routed shaft");
  }

  @Test
  void malformedPhysicalWorksiteMetadataIsRejected() {
    BuildingInfo blankRoute = definition("""
        {"structure":"village_center_jungle_1","work_stations":[
          {"pos":[4,1,4],"occupation":"MINER","worksite_category":""}
        ]}
        """);
    assertEquals("worksite_category cannot be blank", blankRoute.validate());

    BuildingInfo duplicatePosition = definition("""
        {"structure":"mine_jungle_1",
         "work_stations":[{"pos":[2,0,4],"occupation":"MINER"}],
         "worksites":[{"pos":[2,0,4],"occupation":"MINER"}]}
        """);
    assertEquals("a position cannot be both a job vacancy and a physical worksite",
        duplicatePosition.validate());
  }

  private static BuildingInfo definition(String json) {
    return BuildingInfo.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
  }
}
