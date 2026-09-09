package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.junit.jupiter.api.Test;

class FoundingLayoutTest {
  private static FoundingLayout.Candidate candidate(Direction side, int x, int z, int cost) {
    return new FoundingLayout.Candidate(null, side, new BoundingBox(x, 60, z, x + 4, 70, z + 4), cost,
        new TownLayout.Preference(2, 1));
  }

  @Test
  void flatGroundPrefersAdjacentSidesButCheaperOppositeSidesStillWin() {
    var mine = candidate(Direction.NORTH, -102, -110, 0);
    var opposite = candidate(Direction.SOUTH, -102, -90, 0);
    var adjacent = candidate(Direction.EAST, -90, -102, 0);
    assertEquals(adjacent, FoundingLayout.choose(List.of(mine), List.of(opposite, adjacent)).orElseThrow().storehouse());
    var roughAdjacent = candidate(Direction.EAST, -90, -102, 1);
    assertEquals(opposite, FoundingLayout.choose(List.of(mine), List.of(roughAdjacent, opposite)).orElseThrow().storehouse());
  }

  @Test
  void rejectsSameSideAndOverlappingCornersAndNeverInventsAnUnavailableCandidate() {
    var mine = candidate(Direction.NORTH, -10, -10, 0);
    assertTrue(FoundingLayout.choose(List.of(mine), List.of()).isEmpty());
    assertTrue(FoundingLayout.choose(List.of(mine), List.of(candidate(Direction.NORTH, 10, -10, 0))).isEmpty());
    assertTrue(FoundingLayout.choose(List.of(mine), List.of(candidate(Direction.WEST, -9, -9, 0))).isEmpty());
    assertTrue(FoundingLayout.choose(List.of(mine), List.of(candidate(Direction.WEST, -5, -10, 0))).isEmpty());
    assertTrue(FoundingLayout.choose(List.of(mine), List.of(candidate(Direction.WEST, -4, -10, 0))).isPresent());
  }

  @Test
  void foundingOnlyOffersCenteredSlotsOnEverySide() {
    for (int width : List.of(20, 21)) {
      var anchor = new BoundingBox(-30, 60, -23, -30 + width - 1, 75, -3);
      for (var local : List.of(new BoundingBox(-18, 0, 4, -4, 8, 12),
          new BoundingBox(4, 0, -18, 12, 8, -4))) {
        for (Direction side : Direction.Plane.HORIZONTAL) {
          for (int lane : List.of(2, 1)) {
            var origins = FoundingLayout.frontageOrigins(anchor, local, side, lane);
            assertEquals(1, origins.size(), "Founding must not offer off-center edge alignments");
            var origin = origins.getFirst();
            var placed = local.moved(origin.x(), 0, origin.z());
            int doubledCenterError = side.getAxis() == Direction.Axis.X
                ? placed.minZ() + placed.maxZ() - anchor.minZ() - anchor.maxZ()
                : placed.minX() + placed.maxX() - anchor.minX() - anchor.maxX();
            assertTrue(Math.abs(doubledCenterError) <= 1, "Center must match to the nearest half-block");
            int gap = switch (side) {
              case NORTH -> anchor.minZ() - placed.maxZ() - 1;
              case SOUTH -> placed.minZ() - anchor.maxZ() - 1;
              case EAST -> placed.minX() - anchor.maxX() - 1;
              case WEST -> anchor.minX() - placed.maxX() - 1;
              default -> throw new AssertionError(side);
            };
            assertEquals(lane, gap);
          }
        }
      }
    }
  }

  @Test
  void widerFoundingLanesWinAndATightPairRemainsAvailable() {
    var mine = candidate(Direction.NORTH, -2, -10, 0);
    var tight = new FoundingLayout.Candidate(null, Direction.EAST,
        new BoundingBox(10, 60, -2, 14, 70, 2), 0, new TownLayout.Preference(1, 1));
    var roomy = new FoundingLayout.Candidate(null, Direction.EAST,
        new BoundingBox(11, 60, -2, 15, 70, 2), 10, new TownLayout.Preference(2, 1));

    assertEquals(roomy, FoundingLayout.choose(List.of(mine), List.of(tight, roomy)).orElseThrow().storehouse());
    assertEquals(tight, FoundingLayout.choose(List.of(mine), List.of(tight)).orElseThrow().storehouse());
  }

  @Test
  void inwardFoundingFrontsWinAndOtherRotationsRemainFallbacks() {
    var mine = candidate(Direction.NORTH, -2, -10, 0);
    var inward = candidate(Direction.EAST, 10, -2, 10);
    var turned = new FoundingLayout.Candidate(null, Direction.EAST, inward.worldBounds(), 0,
        new TownLayout.Preference(2, 0));

    assertEquals(inward, FoundingLayout.choose(List.of(mine), List.of(turned, inward)).orElseThrow().storehouse());
    assertEquals(turned, FoundingLayout.choose(List.of(mine), List.of(turned)).orElseThrow().storehouse());
  }

  @Test
  void aNarrowGapBetweenCompanionsCannotPretendToHaveTwoBlocksOfClearance() {
    var mine = candidate(Direction.NORTH, 0, 0, 0);
    var tightCorner = candidate(Direction.WEST, 6, 0, 0);
    var roomyCorner = candidate(Direction.WEST, 7, 0, 10);

    assertEquals(1, FoundingLayout.choose(List.of(mine), List.of(tightCorner)).orElseThrow().preference().gap());
    assertEquals(roomyCorner,
        FoundingLayout.choose(List.of(mine), List.of(tightCorner, roomyCorner)).orElseThrow().storehouse());
  }

  @Test
  void everySideUsesExactInclusiveEdgesWithNegativeRotatedAndOddEvenFootprints() {
    for (var anchor : List.of(new TownLayout.Footprint(-30, -23, -10, -3), new TownLayout.Footprint(-30, -23, -11, -4))) {
      var candidate = new TownLayout.Footprint(-18, 4, -4, 12);
      for (Direction side : Direction.Plane.HORIZONTAL) {
        for (var origin : TownLayout.frontageOrigins(anchor, candidate, 1, side)) {
          var placed = candidate.moved(origin);
          int gap = switch (side) {
            case NORTH -> anchor.minZ() - placed.maxZ() - 1;
            case SOUTH -> placed.minZ() - anchor.maxZ() - 1;
            case EAST -> placed.minX() - anchor.maxX() - 1;
            case WEST -> anchor.minX() - placed.maxX() - 1;
            default -> throw new AssertionError(side);
          };
          assertEquals(1, gap);
        }
      }
    }
  }
}
