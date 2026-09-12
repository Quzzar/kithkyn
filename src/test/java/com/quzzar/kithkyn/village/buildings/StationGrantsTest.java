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

class StationGrantsTest {

  private static BuildingInfo parse(String json) {
    return BuildingInfo.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
  }

  @Test
  void aClericStationWithoutHealingIsReported() {
    BuildingInfo info = parse("{\"structure\":\"shrine\",\"work_stations\":[{\"pos\":[1,1,1],\"occupation\":\"CLERIC\"}]}");
    assertEquals(List.of("CLERIC station needs the HEALING grant"), StationGrants.missing(info));
  }

  @Test
  void everyStationInAMixedBuildingNeedsItsOwnGrant() {
    BuildingInfo both = parse("{\"structure\":\"chapel_forge\",\"work_stations\":["
        + "{\"pos\":[1,1,1],\"occupation\":\"CLERIC\"},{\"pos\":[2,1,1],\"occupation\":\"BLACKSMITH\"}],"
        + "\"grants\":[\"HEALING\"]}");
    assertEquals(List.of("BLACKSMITH station needs the REPAIR grant"), StationGrants.missing(both));
    BuildingInfo complete = parse("{\"structure\":\"chapel_forge\",\"work_stations\":["
        + "{\"pos\":[1,1,1],\"occupation\":\"CLERIC\"},{\"pos\":[2,1,1],\"occupation\":\"BLACKSMITH\"}],"
        + "\"grants\":[\"HEALING\",\"REPAIR\"]}");
    assertTrue(StationGrants.missing(complete).isEmpty());
  }

  @Test
  void aConditionalGrantCountsAndTradesWithoutACanonicalGrantAreLeftAlone() {
    BuildingInfo inn = parse("{\"structure\":\"inn\",\"work_stations\":[{\"pos\":[1,1,1],\"occupation\":\"INNKEEPER\"}],"
        + "\"grants_if\":[{\"capability\":\"WANDERERS\",\"requires_supply\":[\"minecraft:bread\"]}]}");
    assertTrue(StationGrants.missing(inn).isEmpty());
    BuildingInfo center = parse("{\"structure\":\"camp\",\"work_stations\":["
        + "{\"pos\":[1,1,1],\"occupation\":\"BUILDER\"},{\"pos\":[2,1,1],\"occupation\":\"GUARD\"}]}");
    assertTrue(StationGrants.missing(center).isEmpty());
  }

  @Test
  void everyBundledDefinitionGrantsWhatItsStationsStandFor() throws Exception {
    Map<String, BuildingInfo> loaded = BuildingDefinitionLoader.resolve(
        resources("kithkyn/buildings"), resources(BuildingRecipe.DIRECTORY));
    assertFalse(loaded.isEmpty());
    Map<String, List<String>> problems = new HashMap<>();
    for (BuildingInfo info : loaded.values()) {
      List<String> missing = StationGrants.missing(info);
      if (!missing.isEmpty()) {
        problems.put(info.getName(), missing);
      }
    }
    assertTrue(problems.isEmpty(), problems.toString());
  }

  private static Map<ResourceLocation, JsonElement> resources(String directory) throws Exception {
    Path root = Path.of(Objects.requireNonNull(StationGrantsTest.class.getResource("/data/kithkyn/" + directory)).toURI());
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
