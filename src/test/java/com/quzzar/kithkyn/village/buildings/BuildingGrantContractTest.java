package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.minecraft.resources.ResourceLocation;

class BuildingGrantContractTest {

  private static BuildingInfo parse(String json) {
    return BuildingInfo.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
  }

  @Test
  void categoryRoomsAndSharedStorageDeclareTheirPlanningOutcomes() {
    BuildingInfo info = parse("""
        {"structure":"house_test_1","beds":[[1,1,1]],"containers":[[2,1,1]],
         "personal_containers":[[3,1,1]],"grants":[]}
        """);
    assertEquals(List.of(
        "category house requires grant HOUSING",
        "shared containers requires grant STORAGE"), BuildingGrantContract.missing(info));
  }

  @Test
  void aMixedUseBuildingDeclaresEveryLocallyPerformedJobOutcome() {
    BuildingInfo mixed = parse("""
        {"structure":"house_test_1","work_stations":[
          {"pos":[1,1,1],"occupation":"CLERIC"},
          {"pos":[2,1,1],"occupation":"BLACKSMITH"}],
         "grants":["HOUSING","HEALING","REPAIR","SMELTING","TOOLS_IRON","ARMOR_IRON"]}
        """);
    assertEquals(List.of("BLACKSMITH station requires grant SHIELDS"), BuildingGrantContract.missing(mixed));
  }

  @Test
  void aStationRoutedToASeparateWorksiteLeavesProductionToTheDestination() {
    BuildingInfo center = parse("""
        {"structure":"village_center_test_1","work_stations":[
          {"pos":[1,1,1],"occupation":"QUARTERMASTER","worksite_category":"storehouse"},
          {"pos":[2,1,1],"occupation":"MINER","worksite_category":"mine"}],
         "grants":["CIVIC_CENTER"]}
        """);
    assertTrue(BuildingGrantContract.missing(center).isEmpty());
  }

  @Test
  void localCivicJobsRemainVisibleEvenWithoutPhysicalAmenities() {
    BuildingInfo center = parse("""
        {"structure":"village_center_test_1","work_stations":[
          {"pos":[1,1,1],"occupation":"QUARTERMASTER"},
          {"pos":[2,1,1],"occupation":"BUILDER"}],
         "grants":["CIVIC_CENTER","LOGISTICS","CONSTRUCTION"]}
        """);
    assertTrue(BuildingGrantContract.missing(center).isEmpty());
  }

  @Test
  void aConditionalGrantCanSatisfyTheMinimumContract() {
    BuildingInfo tavern = parse("""
        {"structure":"tavern_test_1","work_stations":[{"pos":[1,1,1],"occupation":"INNKEEPER"}],
         "grants":["HOSPITALITY","FOOD"],
         "grants_if":[{"capability":"WANDERERS","requires_supply":["minecraft:bread"]}]}
        """);
    assertTrue(BuildingGrantContract.missing(tavern).isEmpty());
  }

  @Test
  void everyBundledDefinitionMeetsTheCompleteGrantContract() throws Exception {
    Map<ResourceLocation, JsonElement> definitions = resources("kithkyn/buildings");
    Map<String, BuildingInfo> loaded = BuildingDefinitionLoader.resolve(definitions);
    assertEquals(definitions.size(), loaded.size());
    assertFalse(loaded.isEmpty());
    Map<String, List<String>> problems = new HashMap<>();
    for (BuildingInfo info : loaded.values()) {
      List<String> missing = BuildingGrantContract.missing(info);
      if (!missing.isEmpty()) problems.put(info.getName(), missing);
    }
    assertTrue(problems.isEmpty(), problems.toString());
  }

  private static Map<ResourceLocation, JsonElement> resources(String directory) throws Exception {
    Path root = Path.of(Objects.requireNonNull(
        BuildingGrantContractTest.class.getResource("/data/kithkyn/" + directory)).toURI());
    Map<ResourceLocation, JsonElement> out = new HashMap<>();
    try (var files = Files.list(root)) {
      for (Path file : files.filter(path -> path.toString().endsWith(".json")).toList()) {
        out.put(ResourceLocation.fromNamespaceAndPath("kithkyn", file.getFileName().toString().replace(".json", "")),
            JsonParser.parseString(Files.readString(file)));
      }
    }
    return out;
  }
}
