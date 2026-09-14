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
  void aRoutedVacancyAndItsPhysicalWorksiteAgreeAcrossBuildings() {
    BuildingInfo center = definition("""
        {"structure":"village_center_swamp_1","work_stations":[
          {"pos":[4,1,4],"occupation":"LUMBERJACK","worksite_category":"lumberjack"},
          {"pos":[5,1,4],"occupation":"BUILDER"},
          {"pos":[6,1,4],"occupation":"GUARD","guard_duty":"CAPTAIN"},
          {"pos":[7,1,4],"occupation":"BUILDER"},
          {"pos":[8,1,4],"occupation":"BUILDER"},
          {"pos":[9,1,4],"occupation":"BUILDER"},
          {"pos":[10,1,4],"occupation":"BUILDER"}
        ]}
        """);
    BuildingInfo mine = definition("""
        {"structure":"mine_swamp_1","work_stations":[
          {"pos":[2,0,4],"occupation":"MINER"}
        ]}
        """);
    BuildingInfo lumberjack = definition("""
        {"structure":"lumberjack_swamp_1",
         "work_stations":[{"pos":[3,1,6],"occupation":"LUMBERJACK"}],
         "worksites":[
          {"pos":[2,1,6],"occupation":"LUMBERJACK"}
        ]}
        """);

    Map<String, java.util.List<String>> problems = BuildingCatalogContract.problems(
        Map.of(center.getName(), center, mine.getName(), mine, lumberjack.getName(), lumberjack));

    assertTrue(problems.isEmpty(), problems.toString());
  }

  @Test
  void everyRoutedVacancyRequiresAMatchingPhysicalWorksite() {
    BuildingInfo house = definition("""
        {"structure":"house_jungle_1","work_stations":[
          {"pos":[4,1,4],"occupation":"LUMBERJACK","worksite_category":"lumberjack"}
        ]}
        """);

    assertEquals(Map.of(house.getName(), java.util.List.of(
        "LUMBERJACK vacancy routes to missing jungle/lumberjack worksite")),
        BuildingCatalogContract.problems(Map.of(house.getName(), house)));
  }

  @Test
  void authoredBuildingReferencesMustResolveInsideTheCatalog() {
    BuildingInfo center = definition("""
        {"structure":"village_center_desert_1","starting_buildings":["mine_desert_1"],
         "work_stations":[
          {"pos":[1,1,1],"occupation":"BUILDER"},
          {"pos":[2,1,1],"occupation":"GUARD","guard_duty":"CAPTAIN"},
          {"pos":[3,1,1],"occupation":"BUILDER"},
          {"pos":[4,1,1],"occupation":"BUILDER"},
          {"pos":[5,1,1],"occupation":"BUILDER"},
          {"pos":[6,1,1],"occupation":"BUILDER"}
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
            "bakery requires a local BAKER vacancy in work_stations"),
        center.getName(), java.util.List.of(
            "village_center requires a local GUARD vacancy in work_stations",
            "village_center requires exactly 5 BUILDER duty anchors; found 1",
            "village_center requires exactly one explicit CAPTAIN guard post; found 0")),
        BuildingCatalogContract.problems(Map.of(bakery.getName(), bakery, center.getName(), center)));
  }

  @Test
  void everyCenterHasFiveBuilderDutiesAndOneExplicitCaptain() {
    BuildingInfo center = definition("""
        {"structure":"village_center_desert_1","work_stations":[
          {"pos":[1,1,1],"occupation":"BUILDER"},
          {"pos":[2,1,1],"occupation":"BUILDER"},
          {"pos":[3,1,1],"occupation":"BUILDER"},
          {"pos":[4,1,1],"occupation":"BUILDER"},
          {"pos":[5,1,1],"occupation":"BUILDER"},
          {"pos":[6,1,1],"occupation":"GUARD","guard_duty":"CAPTAIN"}
        ]}
        """);

    assertTrue(BuildingCatalogContract.problems(Map.of(center.getName(), center)).isEmpty());
  }

  @Test
  void centersCannotOwnMineOrStorehouseVacancies() {
    BuildingInfo center = definition("""
        {"structure":"village_center_desert_1","work_stations":[
          {"pos":[1,1,1],"occupation":"BUILDER"},
          {"pos":[2,1,1],"occupation":"BUILDER"},
          {"pos":[3,1,1],"occupation":"BUILDER"},
          {"pos":[4,1,1],"occupation":"BUILDER"},
          {"pos":[5,1,1],"occupation":"BUILDER"},
          {"pos":[6,1,1],"occupation":"GUARD","guard_duty":"CAPTAIN"},
          {"pos":[7,1,1],"occupation":"MINER","worksite_category":"mine"},
          {"pos":[8,1,1],"occupation":"QUARTERMASTER","worksite_category":"storehouse"}
        ]}
        """);

    assertEquals(java.util.List.of(
        "village_center cannot own a MINER vacancy; each physical mine must contribute its own worker",
        "village_center cannot own a QUARTERMASTER vacancy; each physical storehouse must contribute its own worker"),
        BuildingCatalogContract.problems(Map.of(center.getName(), center)).get(center.getName()));
  }

  @Test
  void everyMineAndStorehouseOwnsItsWorkerVacancy() {
    BuildingInfo mine = definition("""
        {"structure":"mine_jungle_1","worksites":[
          {"pos":[2,0,4],"occupation":"MINER"}
        ]}
        """);
    BuildingInfo storehouse = definition("""
        {"structure":"storehouse_jungle_1","worksites":[
          {"pos":[3,1,3],"occupation":"QUARTERMASTER"}
        ]}
        """);

    assertEquals(java.util.List.of(
        "mine must own exactly one MINER vacancy in work_stations; found 0"),
        BuildingCatalogContract.problems(Map.of(mine.getName(), mine)).get(mine.getName()));
    assertEquals(java.util.List.of(
        "storehouse must own exactly one QUARTERMASTER vacancy in work_stations; found 0"),
        BuildingCatalogContract.problems(Map.of(storehouse.getName(), storehouse)).get(storehouse.getName()));

    BuildingInfo duplicateMine = definition("""
        {"structure":"mine_swamp_1","work_stations":[
          {"pos":[2,0,4],"occupation":"MINER"},
          {"pos":[3,0,4],"occupation":"MINER"}
        ]}
        """);
    assertEquals(java.util.List.of(
        "mine must own exactly one MINER vacancy in work_stations; found 2"),
        BuildingCatalogContract.problems(Map.of(duplicateMine.getName(), duplicateMine))
            .get(duplicateMine.getName()));
  }

  private static BuildingInfo definition(String json) {
    return BuildingInfo.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
  }
}
