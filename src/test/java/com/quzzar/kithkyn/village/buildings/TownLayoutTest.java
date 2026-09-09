package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;

import org.junit.jupiter.api.Test;

class TownLayoutTest {

  @Test
  void frontageSlotsLeaveOneLaneAndAlignAlongTheNeighbour() {
    TownLayout.Footprint anchor = new TownLayout.Footprint(0, 0, 4, 6);
    TownLayout.Footprint candidate = new TownLayout.Footprint(0, 0, 2, 2);

    var origins = TownLayout.frontageOrigins(anchor, candidate, 1);

    assertTrue(origins.contains(new TownLayout.Origin(6, 0)));
    assertTrue(origins.contains(new TownLayout.Origin(6, 2)));
    assertTrue(origins.contains(new TownLayout.Origin(6, 4)));
  }

  @Test
  void relationshipRewardsAContinuousEdgeAndAnInfillCorner() {
    Set<String> claims = new HashSet<>();
    for (int z = 0; z <= 4; z++) {
      claims.add(cell(-2, z));
    }
    for (int x = 0; x <= 4; x++) {
      claims.add(cell(x, -2));
    }

    TownLayout.Relationship relationship = TownLayout.relationship(
        new TownLayout.Footprint(0, 0, 4, 4),
        (x, z) -> claims.contains(cell(x, z)));

    assertEquals(2, relationship.adjacentSides());
    assertEquals(10, relationship.frontage());
  }

  @Test
  void twoBlockLanesContinueFrontageWithoutCountingAThickWallTwice() {
    TownLayout.Relationship relationship = TownLayout.relationship(new TownLayout.Footprint(0, 0, 4, 4),
        (x, z) -> x >= 0 && x <= 4 && (z == -2 || z == -3) || z >= 0 && z <= 4 && x == 7);

    assertEquals(2, relationship.adjacentSides());
    assertEquals(10, relationship.frontage());
  }

  @Test
  void exactlyOneWalkingBlockRemainsLegalButTwoRanksFirst() {
    TownLayout.ClaimedGround claimed = (x, z) -> x >= 0 && x <= 4 && z >= 0 && z <= 4;
    TownLayout.Footprint tight = new TownLayout.Footprint(6, 0, 10, 4);
    TownLayout.Footprint roomy = new TownLayout.Footprint(7, 0, 11, 4);
    var center = new TownLayout.Origin(2, 2);

    assertTrue(TownLayout.hasClearance(tight, TownLayout.MIN_GAP, claimed));
    assertFalse(TownLayout.hasClearance(tight, TownLayout.PREFERRED_GAP, claimed));
    assertTrue(TownLayout.hasClearance(roomy, TownLayout.PREFERRED_GAP, claimed));
    assertFalse(TownLayout.hasClearance(new TownLayout.Footprint(5, 0, 9, 4), TownLayout.MIN_GAP, claimed));
    assertTrue(TownLayout.PREFERRED_FIRST.compare(TownLayout.preference(roomy, Direction.WEST, center, claimed),
        TownLayout.preference(tight, Direction.WEST, center, claimed)) < 0);
  }

  @Test
  void authoredFrontFacesCenterFromEverySideIncludingNegativeRotatedBounds() {
    var center = new TownLayout.Origin(-100, -200);
    var local = new TownLayout.Footprint(-8, 3, -2, 7);
    var anchor = new TownLayout.Footprint(-110, -210, -90, -190);
    var info = new BuildingInfo("storehouse_plains_1");
    for (Direction side : Direction.Plane.HORIZONTAL) {
      var placed = local.moved(TownLayout.centeredFrontageOrigin(anchor, local, 2, side));
      Rotation inward = info.rotationFacing(side.getOpposite());
      for (Rotation rotation : Rotation.values()) {
        assertEquals(rotation == inward,
            TownLayout.facesCenter(placed, rotation.rotate(info.getEntranceFacing()), center));
      }
    }
  }

  @Test
  void aDiagonalCenterAllowsBothClosestFrontsButNeverAnOutwardFront() {
    var footprint = new TownLayout.Footprint(10, 10, 14, 14);
    var center = new TownLayout.Origin(0, 0);
    assertTrue(TownLayout.facesCenter(footprint, Direction.NORTH, center));
    assertTrue(TownLayout.facesCenter(footprint, Direction.WEST, center));
    assertFalse(TownLayout.facesCenter(footprint, Direction.SOUTH, center));
    assertFalse(TownLayout.facesCenter(footprint, Direction.EAST, center));
  }

  @Test
  void aLongFootprintCanTurnWhenOnlyTheShortSideFitsBetweenExistingWalls() {
    var center = new TownLayout.Origin(0, 0);
    var longEastWest = new TownLayout.Footprint(10, -1, 18, 1);
    var longNorthSouth = new TownLayout.Footprint(13, -4, 15, 4);
    TownLayout.ClaimedGround claims = (x, z) -> x == 11 || x == 17;

    assertFalse(TownLayout.hasClearance(longEastWest, TownLayout.MIN_GAP, claims));
    assertTrue(TownLayout.hasClearance(longNorthSouth, TownLayout.MIN_GAP, claims));
    assertTrue(TownLayout.facesCenter(longEastWest, Direction.WEST, center));
    assertFalse(TownLayout.facesCenter(longNorthSouth, Direction.NORTH, center));
    var available = List.of(longEastWest, longNorthSouth).stream()
        .filter(footprint -> TownLayout.hasClearance(footprint, TownLayout.MIN_GAP, claims)).toList();
    assertEquals(List.of(longNorthSouth), available);
  }

  @Test
  void upgradeOriginsCanExtendInEveryDirectionWhileContainingTheOldFootprint() {
    TownLayout.Footprint standing = new TownLayout.Footprint(10, 10, 12, 12);
    TownLayout.Footprint upgrade = new TownLayout.Footprint(0, 0, 4, 4);

    var origins = TownLayout.containingOrigins(standing, upgrade);

    assertEquals(new TownLayout.Origin(9, 9), origins.getFirst());
    assertTrue(origins.contains(new TownLayout.Origin(8, 8)));
    assertTrue(origins.contains(new TownLayout.Origin(10, 10)));
    assertEquals(9, origins.size());
  }

  private static String cell(int x, int z) {
    return x + "," + z;
  }
}
