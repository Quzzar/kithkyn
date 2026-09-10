package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.*;

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

  private static JsonElement definition(String name) {
    return json("{\"structure\":\"" + name + "\"}");
  }

  private static JsonElement recipe(int count) {
    return json("{\"cost\":[{\"item\":\"minecraft:cobblestone\",\"count\":" + count + "}]}");
  }

  @Test
  void oneRecipePricesEveryStyleAndLayoutAndUpdatesTogetherOnReload() {
    Map<ResourceLocation, JsonElement> definitions = Map.of(
        id("house_birch_forest_1"), definition("house_birch_forest_1"),
        id("house_desert_1"), definition("house_desert_1"),
        id("house_badlands_1__small_house_5"), definition("house_badlands_1__small_house_5"));
    for (int amount : new int[] {19, 27}) {
      Map<String, BuildingInfo> loaded = BuildingDefinitionLoader.resolve(definitions, Map.of(id("house_1"), recipe(amount)));
      assertEquals(3, loaded.size());
      loaded.values().forEach(info -> assertEquals(amount, info.getMaterialCost().getFirst().getCount()));
      loaded.get("house_birch_forest_1").getMaterialCost().getFirst().shrink(1);
      assertEquals(amount, loaded.get("house_desert_1").getMaterialCost().getFirst().getCount());
    }
  }

  @Test
  void missingOrInvalidRecipesRejectBuildingsRatherThanMakingThemFree() {
    Map<ResourceLocation, JsonElement> definitions = Map.of(id("house_desert_1"), definition("house_desert_1"));
    assertTrue(BuildingDefinitionLoader.resolve(definitions, Map.of()).isEmpty());
    assertTrue(BuildingDefinitionLoader.resolve(definitions, Map.of(id("house_1"), recipe(0))).isEmpty());
    assertTrue(BuildingDefinitionLoader.resolve(definitions, Map.of(id("house_1"), recipe(-1))).isEmpty());
    assertTrue(BuildingDefinitionLoader.resolve(definitions, Map.of(id("house_1"), json("{\"cost\":[]}"))).isEmpty());
  }

  @Test
  void anExplicitBuildingCostReplacesTheDefaultWithoutAffectingOtherVariants() {
    JsonElement override = json("""
        {"structure":"house_desert_1","cost":[{"item":"minecraft:oak_log","count":7}]}
        """);
    var definitions = Map.of(id("house_desert_1"), override,
        id("house_birch_forest_1"), definition("house_birch_forest_1"));
    for (int defaultPrice : new int[] {19, 27}) {
      var loaded = BuildingDefinitionLoader.resolve(definitions, Map.of(id("house_1"), recipe(defaultPrice)));
      var price = loaded.get("house_desert_1").getMaterialCost();
      assertEquals(1, price.size());
      assertTrue(price.getFirst().is(Items.OAK_LOG));
      assertEquals(7, price.getFirst().getCount());
      assertEquals(defaultPrice, loaded.get("house_birch_forest_1").getMaterialCost().getFirst().getCount());
    }
    assertEquals(1, BuildingDefinitionLoader.resolve(definitions, Map.of()).size(),
        "An explicit full recipe also prices categories with no default");
  }

  @Test
  void invalidOverridesRejectTheBuildingInsteadOfFallingBackOrAcceptingPartialRecipes() {
    for (String cost : new String[] {"null", "[]", "{}",
        "[{\"item\":\"minecraft:cobblestone\",\"count\":0}]",
        "[{\"item\":\"minecraft:cobblestone\",\"count\":7},{\"item\":\"minecraft:missing_material\",\"count\":1}]"}) {
      JsonElement invalid = json("{\"structure\":\"house_desert_1\",\"cost\":" + cost + "}");
      assertTrue(BuildingDefinitionLoader.resolve(Map.of(id("house_desert_1"), invalid),
          Map.of(id("house_1"), recipe(19))).isEmpty());
      JsonElement invalidDefault = json("{\"cost\":" + cost + "}");
      assertTrue(BuildingDefinitionLoader.resolve(Map.of(id("house_desert_1"), definition("house_desert_1")),
          Map.of(id("house_1"), invalidDefault)).isEmpty());
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
  void everyBundledDefinitionHasExactlyOneSharedPriceAndUsesAvailableMaterials() throws Exception {
    Map<ResourceLocation, JsonElement> definitions = resources("kithkyn/buildings");
    Map<ResourceLocation, JsonElement> recipes = resources(BuildingRecipe.DIRECTORY);
    Map<String, BuildingInfo> loaded = BuildingDefinitionLoader.resolve(definitions, recipes);
    assertEquals(definitions.size(), loaded.size());
    for (JsonElement recipe : recipes.values()) {
      assertTrue(BuildingRecipe.CODEC.parse(JsonOps.INSTANCE, recipe).result().isPresent());
    }
    for (BuildingInfo info : loaded.values()) {
      assertFalse(info.getMaterialCost().isEmpty(), info.getName());
      assertTrue(info.getMaterialCost().stream().allMatch(stack -> stack.is(Items.OAK_LOG)
          || stack.is(Items.OAK_PLANKS) || stack.is(Items.COBBLESTONE)
          || stack.is(Items.WHITE_WOOL) || stack.is(Items.IRON_INGOT)), info.getName());
    }
    // The bundled Birch church carries no cost override, so its price is the shared
    // church_1 recipe: 380 cobblestone and 133 oak logs (docs/building-spec.md, church).
    List<ItemStack> church = loaded.get("church_birch_forest_1").getMaterialCost();
    assertEquals(380, church.stream().filter(stack -> stack.is(Items.COBBLESTONE)).findFirst().orElseThrow().getCount());
    assertEquals(133, church.stream().filter(stack -> stack.is(Items.OAK_LOG)).findFirst().orElseThrow().getCount());
  }

  /**
   * The bundled recipes price watchtower_1 at 24 logs and 40 cobblestone and
   * watchtower_2 at 20 logs and 60 cobblestone, so a fresh level 2 pays the
   * level-1 recipe plus the positive delta (24 logs, 60 cobblestone) and an
   * upgrade pays only the 20 extra cobblestone.
   */
  @Test
  void towerQuotesPreservePositiveUpgradeDeltasWithoutRefundingFewerLogs() throws Exception {
    Map<ResourceLocation, JsonElement> definitions = Map.of(
        id("watchtower_birch_forest_1"), definition("watchtower_birch_forest_1"),
        id("watchtower_birch_forest_2"), json("""
            {"structure":"watchtower_birch_forest_2","upgrades_from":"watchtower_birch_forest_1"}
            """));
    Buildings.reload(BuildingDefinitionLoader.resolve(definitions, resources(BuildingRecipe.DIRECTORY)));
    BuildingInfo tower = Objects.requireNonNull(Buildings.getByName("watchtower_birch_forest_2"));
    var fresh = ConstructionQuote.requiredFor(tower, ConstructionMode.FRESH);
    assertEquals(24, fresh.stream().filter(stack -> stack.is(Items.OAK_LOG)).findFirst().orElseThrow().getCount());
    assertEquals(60, fresh.stream().filter(stack -> stack.is(Items.COBBLESTONE)).findFirst().orElseThrow().getCount());
    var upgrade = ConstructionQuote.requiredFor(tower, ConstructionMode.UPGRADE);
    assertEquals(1, upgrade.size());
    assertTrue(upgrade.getFirst().is(Items.COBBLESTONE));
    assertEquals(20, upgrade.getFirst().getCount());
  }

  @Test
  void savedBuildingsAndPendingProjectsRebindTheLoadedRecipeInsteadOfPersistingAnEmptyDefinition() {
    Map<ResourceLocation, JsonElement> definitions = Map.of(id("house_desert_1"), definition("house_desert_1"));
    Buildings.reload(BuildingDefinitionLoader.resolve(definitions, Map.of(id("house_1"), recipe(19))));
    Building building = new Building("house_desert_1", Rotation.NONE);
    StructureInProgress project = new StructureInProgress(building, new Random(), ConstructionMode.FRESH);
    JsonElement saved = StructureInProgress.CODEC.encodeStart(JsonOps.INSTANCE, project).getOrThrow();
    Buildings.reload(BuildingDefinitionLoader.resolve(definitions, Map.of(id("house_1"), recipe(27))));
    StructureInProgress restored = StructureInProgress.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();
    assertEquals(27, restored.requiredMaterials().getFirst().getCount());
    assertSame(Buildings.getByName("house_desert_1"), building.getInfo());
  }
}
