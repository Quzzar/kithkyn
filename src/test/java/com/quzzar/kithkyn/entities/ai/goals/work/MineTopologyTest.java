package com.quzzar.kithkyn.entities.ai.goals.work;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.quzzar.kithkyn.village.buildings.MineShaft;

import net.minecraft.core.BlockPos;

class MineTopologyTest {

  @Test
  void plannedRibEntranceIsInteriorRatherThanRampLining() {
    BlockPos rampEdge = new BlockPos(2, -10, 8);
    BlockPos ribEntrance = new BlockPos(3, -10, 8);

    assertTrue(MineTopology.isRamp(rampEdge));
    assertTrue(MineTopology.isRib(ribEntrance));
    assertTrue(MineTopology.isInterior(ribEntrance));
    assertFalse(MineTopology.crossesExteriorBoundary(rampEdge, ribEntrance));
  }

  @Test
  void descendingEntranceHeadroomIsInteriorRatherThanSealableLining() {
    BlockPos clearance = BlockPos.ZERO;
    BlockPos firstStairHead = new BlockPos(0, -1, 0);

    assertTrue(MineShaft.withinEntranceClearance(clearance));
    assertTrue(MineTopology.isInterior(clearance));
    assertFalse(MineTopology.crossesExteriorBoundary(firstStairHead, clearance));
  }

  @Test
  void childEntranceTorchPositionsMarkEitherWallAboveTheFirstStair() {
    assertEquals(java.util.List.of(
        new BlockPos(-MineShaft.RADIUS, -1, 0),
        new BlockPos(MineShaft.RADIUS, -1, 0)),
        MineTopology.entranceTorchCells());
    assertTrue(MineTopology.entranceTorchCells().stream()
        .allMatch(MineShaft::withinCorridor));
    assertTrue(MineTopology.entranceTorchCells().stream()
        .allMatch(cell -> MineShaft.withinCorridor(cell.below())));
  }

  @Test
  void branchWorkPositionBelongsToTheNavigableMine() {
    BlockPos fourBlocksAlongRib = new BlockPos(6, -10, 8);

    assertTrue(MineShaft.withinExcavation(fourBlocksAlongRib));
  }

  @Test
  void aFallFarBelowTheRampIsNotNavigableMineInterior() {
    BlockPos rampFloor = new BlockPos(0, -7, 5);
    BlockPos fallenIntoCave = new BlockPos(0, -29, 5);

    assertTrue(MineShaft.withinExcavation(rampFloor));
    assertFalse(MineShaft.withinExcavation(fallenIntoCave));
    assertTrue(MineShaft.belowExcavation(fallenIntoCave));
  }

  @Test
  void descendingBetweenRampStepsIsNotMistakenForACaveFall() {
    BlockPos oneBlockBelowCurrentColumn = new BlockPos(0, -8, 5);
    BlockPos genuinelyBelowRamp = new BlockPos(0, -9, 5);

    assertTrue(MineShaft.withinExcavation(oneBlockBelowCurrentColumn));
    assertFalse(MineShaft.belowExcavation(oneBlockBelowCurrentColumn),
        "a diagonal step may cross Y before entering the next ramp column");
    assertTrue(MineShaft.belowExcavation(genuinelyBelowRamp));
  }

  @Test
  void frontierRecoveryScansTheWholeRampIndependentOfWorkerPosition() {
    BlockPos deepRejectedFace = new BlockPos(1, -23, 24);

    var candidates = MineTopology.rampCellsThrough(deepRejectedFace.getZ());

    BlockPos firstMissingFloorCell = new BlockPos(-2, -24, 22);
    assertEquals(new BlockPos(-2, -1, -1), candidates.getFirst());
    assertTrue(candidates.contains(deepRejectedFace));
    assertTrue(candidates.stream().allMatch(MineTopology::isRamp));
    assertTrue(candidates.stream().noneMatch(cell -> cell.getZ() > deepRejectedFace.getZ()));
    assertTrue(candidates.indexOf(firstMissingFloorCell) < candidates.indexOf(deepRejectedFace),
        "floor gaps must be considered before an unreachable upper face farther down the ramp");
  }

  @Test
  void floorBridgeNeverUsesTheCaveBelowOrTheUnfinishedRampAhead() {
    BlockPos missingWalkCell = new BlockPos(0, -24, 22);

    var candidates = MineTopology.floorStandCandidates(missingWalkCell);

    assertTrue(candidates.contains(new BlockPos(0, -23, 21)),
        "the preceding stair is valid bridge footing");
    assertTrue(candidates.stream().allMatch(MineTopology::isRamp));
    assertTrue(candidates.stream().allMatch(candidate -> candidate.getZ() <= missingWalkCell.getZ()));
    assertTrue(candidates.stream().noneMatch(
        candidate -> candidate.getY() < MineTopology.floorY(candidate.getZ())),
        "a natural cave floor below the ramp must never become a bridge stand");
  }

  @Test
  void routeAuditCanAnchorPastADecorativeEntranceStep() {
    var candidates = MineTopology.entranceStandCandidates();

    assertEquals(new BlockPos(0, -1, -1), candidates.getFirst());
    assertTrue(candidates.contains(new BlockPos(0, -2, 0)),
        "the first ordinary ramp floor can anchor a route when the threshold is a stair");
    assertTrue(candidates.stream().allMatch(MineTopology::isRamp));
  }

  @Test
  void miningStandCannotUseANaturalCaveLedge() {
    BlockPos belowRamp = new BlockPos(0, -25, 22);
    BlockPos sideLedge = new BlockPos(3, -24, 22);

    assertFalse(MineTopology.isNavigableStand(belowRamp));
    assertFalse(MineTopology.isNavigableStand(sideLedge));
    assertFalse(MineTopology.isNavigableStand(new BlockPos(3, -25, 22)),
        "even an opened vein cell below the ramp is unsafe footing");
  }

  @Test
  void descendingFrontCanBeWorkedFromHigherFootingTwoColumnsBack() {
    BlockPos rejectedFace = new BlockPos(-2, -52, 50);
    BlockPos lastDryFooting = new BlockPos(0, -50, 48);

    var candidates = MineTopology.workStandCandidates(rejectedFace, 12.0D);

    assertTrue(candidates.contains(lastDryFooting),
        "the last dry ramp step is exactly within the miner's configured reach");
    assertTrue(candidates.stream().allMatch(MineTopology::isNavigableStand));
    assertTrue(candidates.stream().allMatch(candidate -> candidate.distSqr(rejectedFace) <= 12.0D));
    assertFalse(candidates.contains(rejectedFace),
        "an air work target cannot also be chosen as the place to stand");
  }

  @Test
  void aSuccessfulFirstRampHopDoesNotHideASolidBarrierFartherDown() {
    BlockPos start = new BlockPos(0, MineTopology.floorY(0), 0);
    BlockPos target = new BlockPos(0, MineTopology.floorY(6), 6);
    Set<BlockPos> openRamp = new java.util.HashSet<>();
    for (int z = 0; z <= 6; z++) {
      openRamp.add(new BlockPos(0, MineTopology.floorY(z), z));
    }

    assertTrue(MineTopology.standPathExists(start, target, openRamp::contains, 128));

    openRamp.remove(new BlockPos(0, MineTopology.floorY(3), 3));
    assertFalse(MineTopology.standPathExists(start, target, openRamp::contains, 128),
        "a reachable first waypoint cannot make work beyond a sealed ramp selectable");
  }

  @Test
  void anOpenedVeinCannotSelectAStandOutsideMineNavigation() {
    // Mosswood's failed target: mouth (157, 92, 798), stand (152, -6, 895).
    BlockPos offRampVeinStand = new BlockPos(-5, -98, 97);

    assertFalse(MineShaft.withinExcavation(offRampVeinStand));
    assertFalse(MineTopology.isNavigableStand(offRampVeinStand),
        "a work stand outside navigation geometry sends the miner back up the ramp");
    BlockPos plannedRibStand = new BlockPos(-5, -98, 96);
    assertTrue(MineTopology.isNavigableStand(plannedRibStand));
    assertTrue(MineShaft.withinExcavation(plannedRibStand));
  }

  @Test
  void offsetStepBesideFloodedRampCellIsExteriorLining() {
    BlockPos floodedRampCell = new BlockPos(-2, -3, 5);
    BlockPos offsetLeak = new BlockPos(-2, -3, 6);

    assertTrue(MineTopology.isRamp(floodedRampCell));
    assertFalse(MineTopology.isInterior(offsetLeak));
    assertTrue(MineTopology.crossesExteriorBoundary(floodedRampCell, offsetLeak));
  }

  @Test
  void ribOuterEdgeStillCrossesIntoExteriorLining() {
    BlockPos ribEnd = new BlockPos(10, -10, 8);
    BlockPos beyondRib = new BlockPos(11, -10, 8);

    assertTrue(MineTopology.isRib(ribEnd));
    assertTrue(MineTopology.crossesExteriorBoundary(ribEnd, beyondRib));
  }

  @Test
  void floodedRibCanBeSacrificedAtItsDoorwayWithoutBlockingTheRamp() {
    BlockPos floodedRibEnd = new BlockPos(-10, -10, 8);

    assertEquals(new BlockPos(-3, -10, 8), MineTopology.ribDoorway(floodedRibEnd));
    assertFalse(MineTopology.isRamp(MineTopology.ribDoorway(floodedRibEnd)));
    assertNull(MineTopology.ribDoorway(new BlockPos(-2, -10, 8)));
  }

}
