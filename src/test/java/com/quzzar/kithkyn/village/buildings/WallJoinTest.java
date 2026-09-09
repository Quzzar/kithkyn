package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class WallJoinTest {
  @Test
  void lowerLinearToppersDoNotRemainUnderAnOverlappingRaisedCorner() {
    List<Long> ring = WallRoute.aroundBox(0, 64, 0, 64);
    for (int raised = 1; raised <= 4; raised++) {
      int raisedHeight = raised;
      List<Integer> ground = ring.stream().map(p -> BlockPos.getZ(p) < 8 ? 64 + raisedHeight : 64).toList();
      List<WallSection> sections = WallSegmentCatalog.forStyle(VillageStyle.BIRCH_FOREST)
          .compile(ring, Set.of(), ground, WallTerraces.deckProfile(ground, 3), WallTier.WOOD);
      Set<Long> rigid = new HashSet<>();
      for (WallSection section : sections) {
        if (section.kind() == WallSectionKind.CORNER_TOWER || section.kind() == WallSectionKind.GATEHOUSE)
          section.blocks().forEach(cell -> rigid.add(cell.position()));
      }
      for (WallSection section : sections) {
        if (section.kind() == WallSectionKind.CORNER_TOWER || section.kind() == WallSectionKind.GATEHOUSE) continue;
        for (WallBlockPlan cell : section.blocks()) {
          if (cell.piece() == WallBlockPlan.Piece.TORCH) {
            assertFalse(rigid.contains(cell.pos().above().asLong()), "A lower run torch is clipped by a raised corner: " + cell.pos());
          }
        }
      }
    }
  }
}
