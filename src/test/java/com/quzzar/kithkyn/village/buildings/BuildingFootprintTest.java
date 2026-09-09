package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.junit.jupiter.api.Test;

class BuildingFootprintTest {
  @Test
  void actualBirchBoundsExcludeTheCaptureBorderWithoutMovingTheCoordinateFrame() throws Exception {
    var center = template("village_center_birch_forest_1");
    assertEquals(new BoundingBox(4, 0, 4, 24, 10, 24), BuildingFootprint.bounds(center, Rotation.NONE));
    assertEquals(new BoundingBox(-24, 0, 4, -4, 10, 24), BuildingFootprint.bounds(center, Rotation.CLOCKWISE_90));
    assertEquals(new BoundingBox(-24, 0, -24, -4, 10, -4), BuildingFootprint.bounds(center, Rotation.CLOCKWISE_180));
    assertEquals(new BoundingBox(4, 0, -24, 24, 10, -4), BuildingFootprint.bounds(center, Rotation.COUNTERCLOCKWISE_90));
    var store = template("storehouse_birch_forest_1");
    assertEquals(new BoundingBox(5, 0, 4, 11, 7, 14), BuildingFootprint.bounds(store, Rotation.NONE));
    assertEquals(new BoundingBox(3, 0, 4, 12, 7, 16),
        BuildingFootprint.bounds(template("fishery_birch_forest_1"), Rotation.NONE),
        "The new fishery entrance, but not the capture border, belongs to its footprint");
    assertEquals(new BoundingBox(3, 0, 4, 12, 9, 12),
        BuildingFootprint.bounds(template("mine_birch_forest_1"), Rotation.NONE));
  }

  @Test
  void callersCannotMutateCachedBoundsAndReloadClearsTheCache() throws Exception {
    var template = template("storehouse_birch_forest_1");
    var first = BuildingFootprint.bounds(template, Rotation.NONE);
    first.encapsulate(new BlockPos(-100, -100, -100));
    assertEquals(5, BuildingFootprint.bounds(template, Rotation.NONE).minX());
    BuildingFootprint.clearCache();
    assertEquals(5, BuildingFootprint.bounds(template, Rotation.NONE).minX());
  }

  @Test
  void narrowerBirchTowerCanLeaveOnlyItsAuthoredLandscapeWithoutPaddingTheUpgrade() throws Exception {
    var old = template("watchtower_birch_forest_1");
    var target = BuildingFootprint.bounds(template("watchtower_birch_forest_2"), Rotation.NONE);
    var footprint = new TownLayout.Footprint(target.minX(), target.minZ(), target.maxX(), target.maxZ());
    assertTrue(BuildingUpgrade.leavesOnlyLandscape(old, BlockPos.ZERO, Rotation.NONE, footprint));
    assertFalse(BuildingUpgrade.leavesOnlyLandscape(old, BlockPos.ZERO, Rotation.NONE,
        new TownLayout.Footprint(6, 6, 8, 8)), "A smaller replacement cannot leave pieces of the old tower");
    assertEquals(9, target.getXSpan(), "Do not pad the second tower to imitate the old capture");
  }

  private static StructureTemplate template(String name) throws Exception {
    var result = new StructureTemplate();
    try (var input = Objects.requireNonNull(BuildingFootprintTest.class.getResourceAsStream(
        "/data/kithkyn/structure/"+name+".nbt"))) {
      result.load(BuiltInRegistries.BLOCK.asLookup(), NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap()));
    }
    return result;
  }
}
