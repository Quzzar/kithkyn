package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.quzzar.kithkyn.village.Occupation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class BuildingVariantAmenitiesTest {

  @AfterEach
  void clearBuildingRegistry() {
    Buildings.reload(Map.of());
  }

  @Test
  void sameLevelVariantsKeepTheirOwnDeclaredAmenities() {
    BuildingInfo plains = new BuildingInfo("house_plains_3")
        .addBedLocation(1, 1, 1)
        .addBedLocation(2, 1, 1)
        .addBedLocation(3, 1, 1)
        .addBedLocation(4, 1, 1)
        .addContainerLocation(2, 1, 2);
    BuildingInfo taiga = new BuildingInfo("house_taiga_3")
        .addBedLocation(1, 1, 1)
        .addBedLocation(2, 1, 1)
        .addBedLocation(3, 1, 1)
        .addWorkLocation(2, 1, 2, Occupation.BUILDER);
    Buildings.reload(Map.of(plains.getName(), plains, taiga.getName(), taiga));

    BuildingInfo resolvedPlains = Buildings.resolve("house", 3, VillageStyle.PLAINS);
    BuildingInfo resolvedTaiga = Buildings.resolve("house", 3, VillageStyle.TAIGA);

    assertEquals(4, resolvedPlains.getBedLocations().size());
    assertEquals(1, resolvedPlains.getContainerLocations().size());
    assertEquals(3, resolvedTaiga.getBedLocations().size());
    assertEquals(1, resolvedTaiga.getWorkLocations().size());
  }

  @Test
  void buildingDefinitionReadsSemanticVillageIdentitySlots() {
    BuildingInfo info = BuildingInfo.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
        {
          "structure": "watchtower_plains_1",
          "village_identity": {
            "primary_blocks": [[2, 4, 0]],
            "secondary_blocks": [[3, 4, 0]],
            "banners": [[5, 6, 1]]
          }
        }
        """)).getOrThrow();

    assertEquals(1, info.getVillageIdentitySlots().primaryBlocks().size());
    assertEquals(1, info.getVillageIdentitySlots().secondaryBlocks().size());
    assertEquals(1, info.getVillageIdentitySlots().banners().size());
  }
}
