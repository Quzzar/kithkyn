package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;

class MineShaftTest {

  @Test
  void narrowRootAndChildrenKeepTheirWidthAndAdjacentEntryInEveryRotation() {
    for (Rotation rotation : Rotation.values()) {
      BlockPos mouth = new BlockPos(30, 100, 40);
      MineShaft root = MineShaft.root(mouth, rotation, 42L, 3);
      assertEquals(1, root.radius());
      assertEquals(mouth.offset(new BlockPos(0, -1, -1).rotate(rotation)), root.entry());
      for (int side : new int[]{-1, 1}) {
        MineShaft child = MineShaft.child(root, new MineBranch(42L, 8, side, false));
        assertEquals(1, child.radius());
        assertEquals(mouth.offset(new BlockPos(side * 9, -10, 8).rotate(rotation)), child.entry());
      }
      BlockPos inside = mouth.offset(new BlockPos(0, -7, 5).rotate(rotation));
      BlockPos outsideWall = mouth.offset(new BlockPos(2, -7, 5).rotate(rotation));
      assertNull(MineShaft.waypoint(List.of(root), outsideWall, mouth.above(5)));
      assertTrue(MineShaft.waypoint(List.of(root), inside, mouth.above(5)) != null);
    }
  }

  @Test
  void approachingARootWorkStationUsesTheOrdinarySurfaceRoute() {
    BlockPos mouth = new BlockPos(0, 100, 0);
    BlockPos outside = new BlockPos(-12, 100, 0);
    for (Rotation rotation : Rotation.values()) {
      MineShaft root = MineShaft.root(mouth, rotation, 0L);

      assertNull(MineShaft.waypoint(List.of(root), outside, mouth),
          "an untouched mine must not require its underground entry to be dug first");
    }
  }

  @Test
  void birchTownCenterBasementCanApproachTheRootWorkStationNormally() {
    BlockPos mouth = new BlockPos(9895, 62, 9809);
    BlockPos basement = new BlockPos(9872, 59, 9807);
    for (Rotation rotation : Rotation.values()) {
      MineShaft root = MineShaft.root(mouth, rotation, 0L);

      assertFalse(MineShaft.withinExcavation(basement.subtract(mouth).rotate(inverse(rotation))));
      assertNull(MineShaft.waypoint(List.of(root), basement, mouth),
          "being below the station in another building must not force an underground mine entry");
    }
  }

  @Test
  void leavingADeepRootShaftForItsWorkStationStillClimbsTheRamp() {
    BlockPos mouth = new BlockPos(100, 80, -30);
    for (Rotation rotation : Rotation.values()) {
      MineShaft root = MineShaft.root(mouth, rotation, 0L);
      BlockPos deep = mouth.offset(new BlockPos(0, -12, 10).rotate(rotation));
      BlockPos previousStep = mouth.offset(new BlockPos(0, -11, 9).rotate(rotation));

      assertEquals(previousStep, MineShaft.waypoint(List.of(root), deep, mouth));
    }
  }

  @Test
  void approachingADeepRootTargetStillUsesTheShaftEntry() {
    BlockPos mouth = new BlockPos(100, 80, -30);
    BlockPos outside = mouth.offset(-12, 0, 0);
    for (Rotation rotation : Rotation.values()) {
      MineShaft root = MineShaft.root(mouth, rotation, 0L);
      BlockPos deep = mouth.offset(new BlockPos(0, -12, 10).rotate(rotation));

      assertEquals(root.entry(), MineShaft.waypoint(List.of(root), outside, deep));
    }
  }

  @Test
  void childEntryAndDirectionFollowEitherSideUnderEveryRootRotation() {
    BlockPos rootMouth = new BlockPos(100, 80, -30);
    long station = new BlockPos(2, 0, 3).asLong();
    for (Rotation rootRotation : Rotation.values()) {
      MineShaft root = MineShaft.root(rootMouth, rootRotation, station);
      for (int side : List.of(1, -1)) {
        MineShaft child = MineShaft.child(root, new MineBranch(station, 4, side, false));
        BlockPos expectedEntry = rootMouth.offset(new BlockPos(side * 10, -6, 4).rotate(rootRotation));
        BlockPos outward = new BlockPos(side, 0, 0).rotate(rootRotation);
        BlockPos childForward = new BlockPos(0, 0, 1).rotate(child.rotation());

        assertEquals(expectedEntry, child.entry());
        assertEquals(child.mouth(), child.entranceClearance());
        assertTrue(MineShaft.withinExcavation(
            child.entranceClearance().subtract(child.mouth()).rotate(inverse(child.rotation()))));
        assertEquals(outward, childForward);
        assertEquals(1, child.generation());
      }
    }
  }

  private static Rotation inverse(Rotation rotation) {
    return switch (rotation) {
      case CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90;
      case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
      default -> rotation;
    };
  }

  @Test
  void surfaceRouteEntersTheRootRibBeforeDescendingTheChild() {
    MineShaft root = MineShaft.root(new BlockPos(0, 100, 0), Rotation.NONE, 0L);
    MineShaft child = MineShaft.child(root, new MineBranch(0L, 4, 1, false));
    BlockPos surface = new BlockPos(-12, 100, 0);
    BlockPos childTarget = child.mouth().offset(new BlockPos(0, -12, 10).rotate(child.rotation()));

    assertEquals(root.entry(), MineShaft.waypoint(List.of(root, child), surface, childTarget));

    BlockPos rootAtSourceLine = root.mouth().offset(new BlockPos(0, -6, 4));
    BlockPos lastRampStep = root.mouth().offset(new BlockPos(1, -5, 3));
    assertEquals(rootAtSourceLine,
        MineShaft.waypoint(List.of(root, child), lastRampStep, childTarget));

    BlockPos firstRibStep = root.mouth().offset(new BlockPos(1, -6, 4));
    assertEquals(firstRibStep,
        MineShaft.waypoint(List.of(root, child), rootAtSourceLine, childTarget));

    BlockPos besideChildEntry = root.mouth().offset(new BlockPos(9, -6, 4));
    assertEquals(child.entry(),
        MineShaft.waypoint(List.of(root, child), besideChildEntry, childTarget));

    BlockPos firstChildStep = child.mouth().offset(new BlockPos(0, -2, 0).rotate(child.rotation()));
    assertEquals(firstChildStep,
        MineShaft.waypoint(List.of(root, child), child.entry(), childTarget));
  }

  @Test
  void childRouteClimbsToItsParentRibBeforeLeavingTheRoot() {
    MineShaft root = MineShaft.root(new BlockPos(0, 100, 0), Rotation.NONE, 0L);
    MineShaft child = MineShaft.child(root, new MineBranch(0L, 4, 1, false));
    BlockPos surface = new BlockPos(-12, 100, 0);
    BlockPos deepChild = child.mouth().offset(new BlockPos(0, -12, 10).rotate(child.rotation()));
    BlockPos childAtNine = child.mouth().offset(new BlockPos(0, -11, 9).rotate(child.rotation()));

    assertEquals(childAtNine,
        MineShaft.waypoint(List.of(root, child), deepChild, surface));

    BlockPos childAtZero = child.mouth().offset(new BlockPos(0, -2, 0).rotate(child.rotation()));
    assertEquals(child.entry(),
        MineShaft.waypoint(List.of(root, child), childAtZero, surface));

    BlockPos firstRibStep = root.mouth().offset(new BlockPos(9, -6, 4));
    assertEquals(firstRibStep,
        MineShaft.waypoint(List.of(root, child), child.entry(), surface));

    BlockPos besideRootCorridor = root.mouth().offset(new BlockPos(3, -6, 4));
    BlockPos rootAtSourceLine = root.mouth().offset(new BlockPos(0, -6, 4));
    BlockPos corridorEdge = root.mouth().offset(new BlockPos(2, -6, 4));
    assertEquals(corridorEdge,
        MineShaft.waypoint(List.of(root, child), besideRootCorridor, surface));
    assertEquals(root.mouth().offset(0, -5, 3),
        MineShaft.waypoint(List.of(root, child), rootAtSourceLine, surface));
  }

  @Test
  void siblingRouteLeavesOneChildBeforeEnteringTheOther() {
    MineShaft root = MineShaft.root(new BlockPos(0, 100, 0), Rotation.NONE, 0L);
    MineShaft first = MineShaft.child(root, new MineBranch(0L, 4, 1, true));
    MineShaft second = MineShaft.child(root, new MineBranch(0L, 4, -1, false));
    BlockPos firstDeep = first.mouth().offset(new BlockPos(0, -12, 10).rotate(first.rotation()));
    BlockPos secondDeep = second.mouth().offset(new BlockPos(0, -12, 10).rotate(second.rotation()));
    BlockPos firstAtNine = first.mouth().offset(new BlockPos(0, -11, 9).rotate(first.rotation()));

    assertEquals(firstAtNine,
        MineShaft.waypoint(List.of(root, first, second), firstDeep, secondDeep));
    BlockPos firstStepTowardSecond = root.mouth().offset(new BlockPos(-1, -6, 4));
    assertEquals(firstStepTowardSecond,
        MineShaft.waypoint(List.of(root, first, second),
            root.mouth().offset(new BlockPos(0, -6, 4)), secondDeep));
  }

  @Test
  void overlapPreflightUsesTheWholeThreeDimensionalChildPlan() {
    MineShaft root = MineShaft.root(new BlockPos(0, 100, 0), Rotation.NONE, 0L);
    MineShaft atFour = MineShaft.child(root, new MineBranch(0L, 4, 1, false));
    MineShaft atEight = MineShaft.child(root, new MineBranch(0L, 8, 1, false));
    MineShaft atTwelve = MineShaft.child(root, new MineBranch(0L, 12, 1, false));
    MineShaft opposite = MineShaft.child(root, new MineBranch(0L, 4, -1, false));

    assertTrue(MineShaft.overlapsPlannedExcavation(atFour, atEight, -64));
    assertFalse(MineShaft.overlapsPlannedExcavation(atFour, atTwelve, -64));
    assertFalse(MineShaft.overlapsPlannedExcavation(atFour, opposite, -64));
    assertFalse(MineShaft.overlapsPlannedExcavation(atFour, root, -64),
        "the child's single parent-rib entry is an intentional intersection");
  }

  @Test
  void overlapPreflightAlsoCatchesChildrenFromDifferentRootStations() {
    MineShaft firstRoot = MineShaft.root(new BlockPos(0, 100, 0), Rotation.NONE, 0L);
    MineShaft secondRoot = MineShaft.root(new BlockPos(4, 100, 0), Rotation.NONE, 1L);
    MineShaft firstChild = MineShaft.child(firstRoot, new MineBranch(0L, 4, 1, false));
    MineShaft secondChild = MineShaft.child(secondRoot, new MineBranch(1L, 4, 1, false));

    assertTrue(MineShaft.overlapsPlannedExcavation(firstChild, secondChild, -64));
  }

  @Test
  void childCannotCutThroughAnotherRootShaft() {
    MineShaft source = MineShaft.root(new BlockPos(0, 100, 0), Rotation.NONE, 0L);
    MineShaft neighbour = MineShaft.root(new BlockPos(12, 100, 0), Rotation.NONE, 1L);
    MineShaft child = MineShaft.child(source, new MineBranch(0L, 4, 1, false));

    assertTrue(MineShaft.overlapsPlannedExcavation(child, neighbour, -64));
  }

  @Test
  void beingInsideOneShaftIsNeverMistakenForFallingBelowAnother() {
    MineShaft upper = MineShaft.root(new BlockPos(0, 100, 0), Rotation.NONE, 0L);
    MineShaft lower = MineShaft.root(new BlockPos(0, 90, 0), Rotation.NONE, 1L);
    BlockPos lowerWalkCell = new BlockPos(0, 83, 5);

    assertTrue(MineShaft.belowExcavation(lowerWalkCell.subtract(upper.mouth())));
    assertFalse(MineShaft.belowExcavation(List.of(upper, lower), lowerWalkCell));
  }
}
