package com.quzzar.kithkyn.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;

class KindTest {

  @Test
  void idsRoundTripAndUnknownReadsAsLiving() {
    for (Kind kind : Kind.values()) {
      assertEquals(kind, Kind.parse(kind.id()));
      assertEquals(kind, Kind.parse(kind.name()));
      assertEquals(kind, Kind.fromId(kind.id()));
    }
    assertNull(Kind.parse("lich"));
    assertEquals(Kind.LIVING, Kind.fromId(""));
    assertEquals(Kind.LIVING, Kind.fromId("lich"));
  }

  @Test
  void foundingRollIsFixedBySeedAndSite() {
    BlockPos site = new BlockPos(1204, 71, -388);
    Kind first = Kind.rollForFounding(73L, site, 0.5D);
    for (int repeat = 0; repeat < 8; repeat++) {
      assertEquals(first, Kind.rollForFounding(73L, site, 0.5D));
    }
  }

  @Test
  void foundingRollHonoursTheChanceEnds() {
    BlockPos site = new BlockPos(12, 64, 12);
    assertEquals(Kind.LIVING, Kind.rollForFounding(1L, site, 0.0D));
    assertEquals(Kind.UNDEAD, Kind.rollForFounding(1L, site, 1.0D));
  }

  @Test
  void foundingRollSpreadsAcrossSites() {
    Set<Kind> seen = EnumSet.noneOf(Kind.class);
    for (int x = 0; x < 64; x++) {
      seen.add(Kind.rollForFounding(9L, new BlockPos(x * 512, 64, 0), 0.5D));
    }
    assertTrue(seen.containsAll(EnumSet.allOf(Kind.class)), "half the sites should roll undead");
  }

  @Test
  void onlyTheUndeadDescribeThemselves() {
    assertTrue(Kind.LIVING.describe().isEmpty());
    assertTrue(Kind.UNDEAD.describe().contains("undead"));
  }
}
