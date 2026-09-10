package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.quzzar.kithkyn.village.Village;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class BuildingAlternativesTest {
  @AfterEach
  void clearDefinitions() {
    Buildings.reload(Map.of());
  }

  @Test
  void twelveLayoutsRemainIndependentChoicesWithinThreeRealTiers() {
    List<BuildingInfo> houses = twelveHouses();
    load(houses);
    assertEquals(12, Buildings.catalogue(VillageStyle.BIRCH_FOREST).size());
    for (int level = 1; level <= 3; level++) {
      List<BuildingInfo> choices = Buildings.alternatives("house", level, VillageStyle.BIRCH_FOREST);
      assertEquals(level == 1 ? 6 : level == 2 ? 4 : 2, choices.size());
      assertEquals("house_birch_forest_" + level, choices.getFirst().getName());
      assertSame(choices.getFirst(), Buildings.resolve("house", level, VillageStyle.BIRCH_FOREST));
      for (BuildingInfo info : choices) {
        assertNull(info.validate());
        assertNull(info.getUpgradesFrom());
        assertTrue(Buildings.isRegionalChoice(info, VillageStyle.BIRCH_FOREST));
      }
    }

    Village village = village();
    List<UrbanPlanner.Candidate> offered = UrbanPlanner.optionsFor(village).buildable();
    assertEquals(11, offered.size());
    assertTrue(offered.stream().allMatch(option -> option.mode() == ConstructionMode.FRESH));
    assertFalse(offered.stream().anyMatch(option -> option.info().getName().endsWith("__couple_room")));
    assertSame(houses.getFirst(), UrbanPlanner.singleHomeGoal(village));
    assertEquals("house_birch_forest_1__couple_room", UrbanPlanner.coupleHomeGoal(village).getName());
  }

  @Test
  void aSavedAlternativeBuildsThatExactLayoutInsteadOfTheCanonicalDesign() {
    load(twelveHouses());
    Village village = village();
    BuildingInfo chosen = Buildings.getByName("house_birch_forest_3__large_house_3");
    VillageGoal.set(village, chosen.getName(), "room for two couples and singles", "", village.getVillageTime());
    ConstructionChoice project = UrbanPlanner.chooseNextProject(village).join();
    assertSame(chosen, project.info());
    assertEquals(ConstructionMode.FRESH, project.mode());
  }

  @Test
  void regionalAlternativesReplaceTheFallbackFamilyAsAGroup() {
    BuildingInfo plains = house("house_plains_1", 1, 0);
    BuildingInfo plainsAlternative = house("house_plains_1__courtyard", 2, 0);
    load(List.of(plainsAlternative, plains));
    assertEquals(List.of(plains, plainsAlternative), Buildings.alternatives("house", 1, VillageStyle.TAIGA));
    assertSame(plains, Buildings.resolve("house", 1, VillageStyle.TAIGA));
    assertNull(Buildings.resolve("house", 1, VillageStyle.BIRCH_FOREST));

    BuildingInfo own = house("house_taiga_1__timber", 1, 0);
    load(List.of(plains, plainsAlternative, own));
    assertEquals(List.of(own), Buildings.alternatives("house", 1, VillageStyle.TAIGA));
    assertEquals(List.of(own), Buildings.catalogue(VillageStyle.TAIGA));
    assertSame(own, Buildings.resolve("house", 1, VillageStyle.TAIGA));
    assertFalse(Buildings.isRegionalChoice(plainsAlternative, VillageStyle.TAIGA));
  }

  @Test
  void singleGoalsSkipCoupleOnlyOrStalledLayoutsAndMarriageRetainsLegacyCottages() {
    BuildingInfo couple = house("house_birch_forest_1", 0, 1);
    BuildingInfo single = house("house_birch_forest_1__single", 1, 0);
    BuildingInfo otherSingle = house("house_birch_forest_1__terrace", 1, 0);
    BuildingInfo cottage = house("couple_cottage_birch_forest_1", 0, 1);
    load(List.of(couple, single, otherSingle, cottage));
    Village village = village();
    assertSame(single, UrbanPlanner.singleHomeGoal(village));
    VillageGoal.markStalled(village, single.getName(), village.getVillageTime());
    assertSame(otherSingle, UrbanPlanner.singleHomeGoal(village));
    assertSame(cottage, UrbanPlanner.coupleHomeGoal(village));
  }

  @Test
  void marriageCanSelectAPairInAHigherTierWhenNoDedicatedCottageExists() {
    BuildingInfo single = house("house_birch_forest_1", 1, 0);
    BuildingInfo mixed = house("house_birch_forest_3__shared", 2, 2);
    load(List.of(single, mixed));
    assertSame(mixed, UrbanPlanner.coupleHomeGoal(village()));
  }

  @Test
  void standaloneTierPaysItsOwnRecipeWithoutInventingAPredecessor() {
    BuildingInfo lower = house("house_birch_forest_1", 1, 0);
    lower.setMaterialCost(List.of(new ItemStack(Items.OAK_LOG, 20)));
    BuildingInfo larger = house("house_birch_forest_2", 2, 0);
    larger.setMaterialCost(List.of(new ItemStack(Items.COBBLESTONE, 12)));
    load(List.of(lower, larger));
    List<ItemStack> required = BuildingUpgrade.effectiveCost(larger, ConstructionMode.FRESH);
    assertEquals(1, required.size());
    assertEquals(Items.COBBLESTONE, required.getFirst().getItem());
    assertEquals(12, required.getFirst().getCount());
    assertNull(BuildingUpgrade.standingSource(village(), larger));
  }

  private static Village village() {
    Village village = new Village("Layout test");
    village.setStyle(VillageStyle.BIRCH_FOREST);
    return village;
  }

  private static void load(List<BuildingInfo> definitions) {
    Buildings.reload(definitions.stream().collect(Collectors.toMap(BuildingInfo::getName, Function.identity())));
  }

  private static List<BuildingInfo> twelveHouses() {
    List<BuildingInfo> houses = new ArrayList<>();
    houses.add(house("house_birch_forest_1", 1, 0));
    houses.add(house("house_birch_forest_1__couple_room", 0, 1));
    for (int index = 2; index <= 5; index++) houses.add(house("house_birch_forest_1__small_" + index, 1, 0));
    houses.add(house("house_birch_forest_2", 2, 0));
    for (int index = 2; index <= 4; index++) houses.add(house("house_birch_forest_2__medium_" + index, 2, 0));
    houses.add(house("house_birch_forest_3", 2, 1));
    houses.add(house("house_birch_forest_3__large_house_3", 2, 2));
    return houses;
  }

  private static BuildingInfo house(String id, int singles, int couples) {
    JsonObject json = new JsonObject();
    json.addProperty("structure", id);
    if (new BuildingInfo(id).getLevel() > 1) json.addProperty("standalone", true);
    JsonArray beds = new JsonArray();
    JsonArray pairs = new JsonArray();
    for (int index = 0; index < couples; index++) {
      JsonArray pair = new JsonArray();
      for (int side = 0; side < 2; side++) {
        var position = BlockPos.CODEC.encodeStart(JsonOps.INSTANCE, new BlockPos(index * 3 + side, 1, 0)).getOrThrow();
        beds.add(position);
        pair.add(position);
      }
      pairs.add(pair);
    }
    for (int index = 0; index < singles; index++) {
      beds.add(BlockPos.CODEC.encodeStart(JsonOps.INSTANCE, new BlockPos(index * 2, 1, 4)).getOrThrow());
    }
    json.add("beds", beds);
    json.add("couple_beds", pairs);
    return BuildingInfo.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
  }
}
