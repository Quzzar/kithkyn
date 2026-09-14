package com.quzzar.kithkyn.village;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.quzzar.kithkyn.village.buildings.Building;
import com.quzzar.kithkyn.village.buildings.BuildingInfo;
import com.quzzar.kithkyn.village.buildings.Buildings;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;

class StorehouseTest {

  @AfterEach
  void clearDefinitions() {
    Buildings.reload(Map.of());
  }

  @Test
  void centralShelvesIncludeEveryStorehouseButNotWorkplaceStorage() {
    BuildingInfo mine = definition("mine_birch_forest_1", "mine", 4, 1, 4);
    BuildingInfo first = definition("storehouse_birch_forest_1", "storehouse", 1, 1, 1);
    BuildingInfo second = definition("storehouse_birch_forest_1__annex", "storehouse", 2, 1, 2);
    Buildings.reload(Map.of(mine.getName(), mine, first.getName(), first, second.getName(), second));

    TestVillage village = new TestVillage();
    village.addCompleted(new BlockPos(100, 64, 100), mine);
    village.addCompleted(new BlockPos(110, 64, 100), first);
    village.addCompleted(new BlockPos(120, 64, 100), second);

    assertEquals(List.of(first.getName(), second.getName()),
        Storehouse.buildings(village).stream().map(Building::getName).toList());
    assertEquals(List.of(new BlockPos(111, 65, 101), new BlockPos(122, 65, 102)),
        Storehouse.chests(village));
  }

  @Test
  void anExistingVillagePicksUpAContainerAddedByADefinitionRepair() {
    BuildingInfo original = definition("storehouse_birch_forest_1", "storehouse", 1, 1, 1);
    Buildings.reload(Map.of(original.getName(), original));
    TestVillage village = new TestVillage();
    village.addCompleted(new BlockPos(100, 64, 100), original);

    BuildingInfo repaired = BuildingInfo.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
        {"structure":"storehouse_birch_forest_1","category":"storehouse","variant":"birch_forest",
         "containers":[[1,1,1],[2,1,2]],"grants":["STORAGE"]}
        """)).getOrThrow();
    Buildings.reload(Map.of(repaired.getName(), repaired));

    assertTrue(village.getBrain().reconcileContainers(village.getBuildings()));
    assertEquals(List.of(new BlockPos(101, 65, 101), new BlockPos(102, 65, 102)),
        village.getSharedContainerPositions().stream().sorted().toList());
  }

  private static BuildingInfo definition(String structure, String category, int x, int y, int z) {
    return BuildingInfo.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
        {"structure":"%s","category":"%s","variant":"birch_forest",
         "containers":[[%d,%d,%d]],"grants":["STORAGE"]}
        """.formatted(structure, category, x, y, z))).getOrThrow();
  }

  private static final class TestVillage extends Village {
    private TestVillage() {
      super("Shelf test");
    }

    private void addCompleted(BlockPos origin, BuildingInfo info) {
      addBuilding(new Building(origin, info.getName(), Rotation.NONE));
    }
  }
}
