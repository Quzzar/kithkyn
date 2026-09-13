package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Rotation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class BuildingRecipeTest {
  @AfterEach
  void clearRegistry() {
    Buildings.reload(Map.of());
  }

  private static ResourceLocation id(String name) {
    return ResourceLocation.fromNamespaceAndPath("kithkyn", name);
  }

  private static JsonElement json(String text) {
    return JsonParser.parseString(text);
  }

  private static JsonElement house(String name, int count) {
    return json("{\"structure\":\"" + name + "\",\"grants\":[\"HOUSING\"],"
        + "\"cost\":[{\"item\":\"minecraft:cobblestone\",\"count\":" + count + "}]}");
  }

  @Test
  void everyVariantOwnsItsRecipeAndCanBeTunedIndependently() {
    Map<ResourceLocation, JsonElement> definitions = Map.of(
        id("house_birch_forest_1"), house("house_birch_forest_1", 19),
        id("house_desert_1"), house("house_desert_1", 27),
        id("house_badlands_1__small_house_5"), house("house_badlands_1__small_house_5", 35));
    Map<String, BuildingInfo> loaded = BuildingDefinitionLoader.resolve(definitions);
    assertEquals(19, loaded.get("house_birch_forest_1").getMaterialCost().getFirst().getCount());
    assertEquals(27, loaded.get("house_desert_1").getMaterialCost().getFirst().getCount());
    assertEquals(35, loaded.get("house_badlands_1__small_house_5").getMaterialCost().getFirst().getCount());
    loaded.get("house_birch_forest_1").getMaterialCost().getFirst().shrink(1);
    assertEquals(27, loaded.get("house_desert_1").getMaterialCost().getFirst().getCount());
  }

  @Test
  void missingOrInvalidAuthoredCostsRejectBuildingsRatherThanMakingThemFree() {
    for (String cost : new String[] {"", ",\"cost\":null", ",\"cost\":[]", ",\"cost\":{}",
        ",\"cost\":[{\"item\":\"minecraft:cobblestone\",\"count\":0}]",
        ",\"cost\":[{\"item\":\"minecraft:cobblestone\",\"count\":-1}]"}) {
      JsonElement definition = json("{\"structure\":\"house_desert_1\",\"grants\":[\"HOUSING\"]" + cost + "}");
      assertTrue(BuildingDefinitionLoader.resolve(Map.of(id("house_desert_1"), definition)).isEmpty());
    }
  }

  @Test
  void aDefinitionWithoutItsStructureTemplateIsRejected() {
    Map<ResourceLocation, JsonElement> definitions = Map.of(
        id("house_desert_1"), house("house_desert_1", 20));

    assertTrue(BuildingDefinitionLoader.resolve(definitions, ignored -> false).isEmpty());
  }

  @Test
  void missingEmptyDuplicateAndRetiredGrantsRejectBuildings() {
    for (String grants : new String[] {"", ",\"grants\":[]", ",\"grants\":[\"HOUSING\",\"HOUSING\"]",
        ",\"grants\":[\"HOUSING\",\"PRIVATE_STORAGE\"]",
        ",\"grants\":[\"HOUSING\",\"GRAIN\"]",
        ",\"grants\":[\"HOUSING\"],\"grants_if\":[{\"capability\":\"HOUSING\",\"requires_capability\":[\"WATER\"]}]",
        ",\"grants\":[\"HOUSING\"],\"grants_if\":[{\"capability\":\"CROPS\",\"requires_capability\":[\"FUEL\"]}]"}) {
      JsonElement definition = json("{\"structure\":\"house_desert_1\"" + grants
          + ",\"cost\":[{\"item\":\"minecraft:cobblestone\",\"count\":20}]}");
      assertTrue(BuildingDefinitionLoader.resolve(Map.of(id("house_desert_1"), definition)).isEmpty());
    }
  }

  @Test
  void duplicateAirAndUnknownMaterialsAreRejected() {
    for (String cost : new String[] {
        "[{\"item\":\"minecraft:air\",\"count\":1}]",
        "[{\"item\":\"minecraft:missing_material\",\"count\":1}]",
        "[{\"item\":\"minecraft:cobblestone\",\"count\":1},{\"item\":\"minecraft:cobblestone\",\"count\":2}]"
    }) {
      assertTrue(BuildingRecipe.CODEC.parse(JsonOps.INSTANCE, json("{\"cost\":" + cost + "}")).error().isPresent());
    }
  }

  private static Map<ResourceLocation, JsonElement> resources(String directory) throws Exception {
    Path root = Path.of(Objects.requireNonNull(BuildingRecipeTest.class.getResource("/data/kithkyn/" + directory)).toURI());
    Map<ResourceLocation, JsonElement> out = new HashMap<>();
    try (var files = Files.list(root)) {
      for (Path file : files.filter(path -> path.toString().endsWith(".json")).toList()) {
        out.put(id(file.getFileName().toString().replace(".json", "")), json(Files.readString(file)));
      }
    }
    return out;
  }

  @Test
  void everyBundledDefinitionHasItsOwnValidPriceAndGrantContract() throws Exception {
    Map<ResourceLocation, JsonElement> definitions = resources("kithkyn/buildings");
    Map<String, BuildingInfo> loaded = BuildingDefinitionLoader.resolve(definitions);
    assertEquals(definitions.size(), loaded.size());
    for (BuildingInfo info : loaded.values()) {
      assertFalse(info.getMaterialCost().isEmpty(), info.getName());
      assertFalse(info.getGrants().isEmpty(), info.getName());
      assertTrue(info.getMaterialCost().stream().allMatch(stack -> stack.is(Items.OAK_LOG)
          || stack.is(Items.COBBLESTONE) || stack.is(Items.WHITE_WOOL) || stack.is(Items.IRON_INGOT)), info.getName());
      assertTrue(info.validateAuthoredContract() == null, info.getName() + ": " + info.validateAuthoredContract());
    }
    List<ItemStack> church = loaded.get("church_birch_forest_1").getMaterialCost();
    assertEquals(28, church.stream().filter(stack -> stack.is(Items.COBBLESTONE)).findFirst().orElseThrow().getCount());
    assertEquals(16, church.stream().filter(stack -> stack.is(Items.OAK_LOG)).findFirst().orElseThrow().getCount());
  }

  @Test
  void towerQuotesUseTheAuthoredVariantCostsForFreshAndUpgradePaths() {
    Map<ResourceLocation, JsonElement> definitions = Map.of(
        id("watchtower_birch_forest_1"), json("""
            {"structure":"watchtower_birch_forest_1","grants":["PROTECTION","RANGED_GUARD_POSTS"],
             "work_stations":[{"pos":[1,1,1],"occupation":"GUARD"}],
             "cost":[{"item":"minecraft:oak_log","count":16},{"item":"minecraft:cobblestone","count":24}]}
            """),
        id("watchtower_birch_forest_2"), json("""
            {"structure":"watchtower_birch_forest_2","upgrades_from":"watchtower_birch_forest_1",
             "grants":["PROTECTION","RANGED_GUARD_POSTS"],
             "work_stations":[{"pos":[1,1,1],"occupation":"GUARD"}],
             "cost":[{"item":"minecraft:oak_log","count":24},{"item":"minecraft:cobblestone","count":36}]}
            """));
    Buildings.reload(BuildingDefinitionLoader.resolve(definitions));
    BuildingInfo tower = Objects.requireNonNull(Buildings.getByName("watchtower_birch_forest_2"));
    var fresh = ConstructionQuote.requiredFor(tower, ConstructionMode.FRESH);
    assertEquals(24, fresh.stream().filter(stack -> stack.is(Items.OAK_LOG)).findFirst().orElseThrow().getCount());
    assertEquals(36, fresh.stream().filter(stack -> stack.is(Items.COBBLESTONE)).findFirst().orElseThrow().getCount());
    var upgrade = ConstructionQuote.requiredFor(tower, ConstructionMode.UPGRADE);
    assertEquals(8, upgrade.stream().filter(stack -> stack.is(Items.OAK_LOG)).findFirst().orElseThrow().getCount());
    assertEquals(12, upgrade.stream().filter(stack -> stack.is(Items.COBBLESTONE)).findFirst().orElseThrow().getCount());
  }

  @Test
  void savedBuildingsAndPendingProjectsRebindTheCurrentAuthoredRecipe() {
    Map<ResourceLocation, JsonElement> definitions = Map.of(id("house_desert_1"), house("house_desert_1", 19));
    Buildings.reload(BuildingDefinitionLoader.resolve(definitions));
    Building building = new Building("house_desert_1", Rotation.NONE);
    StructureInProgress project = new StructureInProgress(building, new Random(), ConstructionMode.FRESH);
    JsonElement saved = StructureInProgress.CODEC.encodeStart(JsonOps.INSTANCE, project).getOrThrow();
    Buildings.reload(BuildingDefinitionLoader.resolve(
        Map.of(id("house_desert_1"), house("house_desert_1", 27))));
    StructureInProgress restored = StructureInProgress.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();
    assertEquals(27, restored.requiredMaterials().getFirst().getCount());
    assertSame(Buildings.getByName("house_desert_1"), building.getInfo());
  }
}
