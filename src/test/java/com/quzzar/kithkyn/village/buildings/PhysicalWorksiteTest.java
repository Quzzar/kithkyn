package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
  void aRoutedWorksiteMayReserveItsOwnLiveInBedWithoutCreatingAnotherVacancy() {
    BuildingInfo mine = definition("""
        {"structure":"mine_romanian_1","beds":[[2,1,2]],"worker_beds":[[2,1,2]],
         "worksites":[{"pos":[7,1,3],"occupation":"MINER"}]}
        """);

    assertEquals(null, mine.validate());
    assertTrue(mine.isWorkerBed(0));
    assertTrue(mine.getWorkLocations().isEmpty(), "The routed worksite must not add a second miner vacancy");
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

  @Test
  void everyPhysicalWorksiteRequiresAMatchingRoutedVacancy() {
    BuildingInfo center = definition("""
        {"structure":"village_center_swamp_1","work_stations":[
          {"pos":[4,1,4],"occupation":"MINER","worksite_category":"mine"},
          {"pos":[5,1,4],"occupation":"BUILDER"},
          {"pos":[6,1,4],"occupation":"GUARD","guard_duty":"CAPTAIN"},
          {"pos":[7,1,4],"occupation":"BUILDER"},
          {"pos":[8,1,4],"occupation":"BUILDER"}
        ]}
        """);
    BuildingInfo mine = definition("""
        {"structure":"mine_swamp_1","worksites":[
          {"pos":[2,0,4],"occupation":"MINER"}
        ]}
        """);
    BuildingInfo lumberjack = definition("""
        {"structure":"lumberjack_swamp_1","worksites":[
          {"pos":[2,1,6],"occupation":"LUMBERJACK"}
        ]}
        """);

    Map<String, java.util.List<String>> problems = BuildingCatalogContract.problems(
        Map.of(center.getName(), center, mine.getName(), mine, lumberjack.getName(), lumberjack));

    assertFalse(problems.containsKey(center.getName()), problems.toString());
    assertFalse(problems.containsKey(mine.getName()), problems.toString());
    assertEquals(java.util.List.of(
        "LUMBERJACK worksite has no routed vacancy for swamp/lumberjack"),
        problems.get(lumberjack.getName()));
  }

  @Test
  void everyRoutedVacancyRequiresAMatchingPhysicalWorksite() {
    BuildingInfo center = definition("""
        {"structure":"village_center_jungle_1","work_stations":[
          {"pos":[4,1,4],"occupation":"MINER","worksite_category":"mine"},
          {"pos":[5,1,4],"occupation":"BUILDER"},
          {"pos":[6,1,4],"occupation":"GUARD","guard_duty":"CAPTAIN"},
          {"pos":[7,1,4],"occupation":"BUILDER"},
          {"pos":[8,1,4],"occupation":"BUILDER"}
        ]}
        """);

    assertEquals(Map.of(center.getName(), java.util.List.of(
        "MINER vacancy routes to missing jungle/mine worksite")),
        BuildingCatalogContract.problems(Map.of(center.getName(), center)));
  }

  @Test
  void authoredBuildingReferencesMustResolveInsideTheCatalog() {
    BuildingInfo center = definition("""
        {"structure":"village_center_desert_1","starting_buildings":["mine_desert_1"],
         "work_stations":[
          {"pos":[1,1,1],"occupation":"BUILDER"},
          {"pos":[2,1,1],"occupation":"GUARD","guard_duty":"CAPTAIN"},
          {"pos":[3,1,1],"occupation":"BUILDER"},
          {"pos":[4,1,1],"occupation":"BUILDER"}
         ]}
        """);
    BuildingInfo tower = definition("""
        {"structure":"watchtower_desert_2","upgrades_from":"watchtower_desert_1",
         "work_stations":[{"pos":[1,1,1],"occupation":"GUARD"}]}
        """);

    assertEquals(Map.of(
        center.getName(), java.util.List.of("starting building mine_desert_1 is missing"),
        tower.getName(), java.util.List.of("upgrade predecessor watchtower_desert_1 is missing")),
        BuildingCatalogContract.problems(Map.of(center.getName(), center, tower.getName(), tower)));
  }

  @Test
  void productiveBuildingCategoriesCannotSilentlyOmitTheirCoreWork() {
    BuildingInfo bakery = definition("""
        {"structure":"bakery_desert_1"}
        """);
    BuildingInfo center = definition("""
        {"structure":"village_center_desert_1","work_stations":[
          {"pos":[1,1,1],"occupation":"BUILDER"}
        ]}
        """);

    assertEquals(Map.of(
        bakery.getName(), java.util.List.of(
            "bakery requires a BAKER work station or physical worksite"),
        center.getName(), java.util.List.of(
            "village_center requires a GUARD work station or physical worksite",
            "village_center requires exactly 3 BUILDER posts for lead, path and grading duties; found 1",
            "village_center requires exactly one explicit CAPTAIN guard post; found 0")),
        BuildingCatalogContract.problems(Map.of(bakery.getName(), bakery, center.getName(), center)));
  }

  @Test
  void everyCenterHasThreeBuilderDutiesAndOneExplicitCaptain() {
    BuildingInfo center = definition("""
        {"structure":"village_center_desert_1","work_stations":[
          {"pos":[1,1,1],"occupation":"BUILDER"},
          {"pos":[2,1,1],"occupation":"BUILDER"},
          {"pos":[3,1,1],"occupation":"BUILDER"},
          {"pos":[4,1,1],"occupation":"GUARD","guard_duty":"CAPTAIN"}
        ]}
        """);

    assertTrue(BuildingCatalogContract.problems(Map.of(center.getName(), center)).isEmpty());
  }

  @Test
  void centerOwnedProductionPostsPointAtTheirPhysicalFacilities() {
    BuildingInfo center = definition("""
        {"structure":"village_center_desert_1","work_stations":[
          {"pos":[1,1,1],"occupation":"BUILDER"},
          {"pos":[2,1,1],"occupation":"BUILDER"},
          {"pos":[3,1,1],"occupation":"BUILDER"},
          {"pos":[4,1,1],"occupation":"GUARD","guard_duty":"CAPTAIN"},
          {"pos":[5,1,1],"occupation":"MINER"},
          {"pos":[6,1,1],"occupation":"QUARTERMASTER"}
        ]}
        """);

    assertEquals(java.util.List.of(
        "a center-owned MINER post must route to the mine worksite",
        "a center-owned QUARTERMASTER post needs center storage or a storehouse route"),
        BuildingCatalogContract.problems(Map.of(center.getName(), center)).get(center.getName()));
  }

  private static BuildingInfo definition(String json) {
    return BuildingInfo.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
  }
}
