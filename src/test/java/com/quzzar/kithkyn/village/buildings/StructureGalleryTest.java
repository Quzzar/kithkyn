package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.BlockPos;

class StructureGalleryTest {

  @Test
  void galleryUsesTheProductionGroundPlaneAndAuthoredSink() {
    BlockPos surface = new BlockPos(12000, 220, 12000);
    assertEquals(surface.above(), StructureGallery.seatedOrigin(surface, definition(-1)));
    assertEquals(surface, StructureGallery.seatedOrigin(surface, definition(0)));
    assertEquals(surface.below(5), StructureGallery.seatedOrigin(surface, definition(5)));
  }

  private static BuildingInfo definition(int sink) {
    return BuildingInfo.CODEC.parse(JsonOps.INSTANCE,
        JsonParser.parseString("{\"structure\":\"house_test_1\",\"sink\":" + sink + "}"))
        .getOrThrow();
  }
}
