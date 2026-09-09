package com.quzzar.kithkyn.village;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import org.junit.jupiter.api.Test;

class VillageFoundingFootprintTest {

  @Test
  void oddCenterUsesItsExactTwentyNineBlockFootprint() {
    BoundingBox footprint = Village.buildingFootprint(new BlockPos(100, 59, 200),
        new BoundingBox(0, 0, 0, 28, 9, 28), 64);

    assertEquals(new BoundingBox(100, 64, 200, 128, 64, 228), footprint);
    assertEquals(29, footprint.getXSpan());
    assertEquals(29, footprint.getZSpan());
    assertEquals(1, footprint.getYSpan());
  }

  @Test
  void evenStorehouseDoesNotGainAnExtraColumnFromCenterRounding() {
    BoundingBox footprint = Village.buildingFootprint(new BlockPos(-100, 63, -200),
        new BoundingBox(0, 0, 0, 15, 7, 18), 64);

    assertEquals(new BoundingBox(-100, 64, -200, -85, 64, -182), footprint);
    assertEquals(16, footprint.getXSpan());
    assertEquals(19, footprint.getZSpan());
  }

  @Test
  void rotatedBoundsKeepNegativeOffsetsAndTheirExactSwappedDimensions() {
    BlockPos origin = new BlockPos(100, 59, 200);
    BoundingBox[] rotatedBounds = {
        new BoundingBox(-18, 0, 0, 0, 7, 15),
        new BoundingBox(-15, 0, -18, 0, 7, 0),
        new BoundingBox(0, 0, -15, 18, 7, 0)
    };
    BoundingBox[] expectedFootprints = {
        new BoundingBox(82, 64, 200, 100, 64, 215),
        new BoundingBox(85, 64, 182, 100, 64, 200),
        new BoundingBox(100, 64, 185, 118, 64, 200)
    };

    for (int i = 0; i < rotatedBounds.length; i++) {
      BoundingBox footprint = Village.buildingFootprint(origin, rotatedBounds[i], 64);

      assertEquals(expectedFootprints[i], footprint);
      assertEquals(rotatedBounds[i].getXSpan(), footprint.getXSpan());
      assertEquals(rotatedBounds[i].getZSpan(), footprint.getZSpan());
    }
  }
}
