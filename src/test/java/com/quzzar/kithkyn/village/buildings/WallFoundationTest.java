package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class WallFoundationTest {
  @Test
  void gatePassagesStayOpenInEveryOrientationOnFlatAndRaisedDecks() {
    for (VillageStyle style : List.of(VillageStyle.BIRCH_FOREST, VillageStyle.DESERT)) {
      for (int deckY : List.of(66, 70)) {
        List<Long> ring = WallRoute.aroundBox(0, 64, 0, 64);
        Set<Long> gates = WallPreview.cardinalGates(ring, 0, 64, 0, 64);
        Map<Long, WallBlockPlan> cells = WallSegmentCatalog.forStyle(style)
            .compile(ring, gates, Collections.nCopies(ring.size(), 64),
                Collections.nCopies(ring.size(), deckY), WallTier.WOOD).stream()
            .flatMap(section -> section.blocks().stream())
            .collect(Collectors.toMap(WallBlockPlan::position, Function.identity()));
        for (long gate : gates) {
          BlockPos anchor = BlockPos.of(gate);
          boolean eastWestWall = anchor.getZ() == 0 || anchor.getZ() == 64;
          for (int along = -1; along <= 1; along++) {
            for (int across = -2; across <= 2; across++) {
              for (int y = 65; y < deckY + 3; y++) {
                BlockPos opening = offset(anchor, eastWestWall, along, across, y);
                assertFalse(cells.containsKey(opening.asLong()),
                    () -> style + " blocked passage at " + opening + ": " + cells.get(opening.asLong()));
              }
            }
          }
          // The legs beside the passage still extend from their authored base to terrain.
          for (int along : List.of(-2, 2)) {
            for (int across : List.of(-1, 1)) {
              for (int y = 64; y <= deckY - 2; y++) {
                BlockPos leg = offset(anchor, eastWestWall, along, across, y);
                assertNotNull(cells.get(leg.asLong()), "Unsupported gate leg at " + leg);
              }
            }
          }
        }
      }
    }
  }

  @Test
  void completedWallsStayCompleteWhenAnUpdatedCatalogChangesSectionSignatures() {
    List<Long> ring = WallRoute.aroundBox(0, 64, 0, 64);
    WallProject wall = WallProject.completed(ring, Set.of(),
        Collections.nCopies(ring.size(), 64), WallTier.WOOD, VillageStyle.BIRCH_FOREST);
    wall.markSiteCleared();
    var saved = WallProject.CODEC.encodeStart(JsonOps.INSTANCE, wall).getOrThrow().getAsJsonObject();
    saved.getAsJsonArray("sections").get(0).getAsJsonObject().addProperty("signature", -1L);
    WallProject restored = WallProject.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();
    assertTrue(restored.isComplete(), "An art update must not restart an already finished wall");
    assertTrue(restored.isSiteCleared());
    assertEquals(0, restored.remainingBlocks());

    // The same stale section data must not mark an unfinished or deferred wall complete.
    saved.addProperty("cursor", 0);
    assertFalse(WallProject.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow().isComplete());
    saved.addProperty("cursor", ring.size());
    var deferred = new com.google.gson.JsonArray();
    deferred.add(1);
    saved.add("deferred", deferred);
    assertFalse(WallProject.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow().isComplete());
  }

  private static BlockPos offset(BlockPos anchor, boolean eastWestWall, int along, int across, int y) {
    return new BlockPos(anchor.getX() + (eastWestWall ? along : across), y,
        anchor.getZ() + (eastWestWall ? across : along));
  }

  @Test
  void anArtRepairOnlyRechecksTheChangedSectionOfAnUnfinishedWall() {
    List<Long> ring = WallRoute.aroundBox(0, 64, 0, 64);
    WallProject wall = new WallProject(ring, Set.of(), Collections.nCopies(ring.size(), 64),
        WallTier.WOOD, VillageStyle.BIRCH_FOREST);
    var saved = WallProject.CODEC.encodeStart(JsonOps.INSTANCE, wall).getOrThrow().getAsJsonObject();
    saved.getAsJsonArray("sections").get(0).getAsJsonObject().addProperty("cursor", 1);
    saved.getAsJsonArray("sections").get(1).getAsJsonObject().addProperty("cursor", 1);
    saved.getAsJsonArray("sections").get(1).getAsJsonObject().addProperty("signature", -1L);
    WallProject restored = WallProject.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();
    assertEquals(1, restored.section(0).cursor());
    assertEquals(0, restored.section(1).cursor());
    assertFalse(restored.isComplete());
  }
}
