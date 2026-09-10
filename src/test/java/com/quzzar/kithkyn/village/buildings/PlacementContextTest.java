package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.junit.jupiter.api.Test;

class PlacementContextTest {

  private static final BoundingBox CENTER = new BoundingBox(-4, 60, -4, 4, 68, 4);
  private static final BoundingBox HOME = new BoundingBox(7, 58, -2, 11, 64, 2);

  @Test
  void reservationsBlockLaterSitesWithoutChangingTheOriginalPlanOrExternalClaims() {
    List<BoundingBox> anchors = new ArrayList<>(List.of(CENTER));
    var initial = new LocationValidator.PlacementContext(anchors, 1, 4,
        (x, z) -> x == 30 && z == 30, (ground, bounds) -> true);
    var expanded = initial.withPlannedBuilding(HOME);
    anchors.clear();

    assertTrue(initial.isClaimed(0, 0), "The center snapshot must survive changes to the source list");
    assertFalse(initial.isClaimed(9, 0), "A rejected expansion must not leak reservations");
    assertTrue(expanded.isClaimed(9, 0), "A later home must see the earlier home's footprint");
    assertTrue(expanded.isClaimed(30, 30), "Claims without standing buildings remain protected");
    assertFalse(expanded.isClaimed(6, 0), "A reservation must not turn its walking lane into a claim");
  }

  @Test
  void aPlannedHomeBecomesTheSameFrontageAnchorAsACompletedHome() {
    var initial = new LocationValidator.PlacementContext(List.of(CENTER), 1, 4,
        (x, z) -> false, (ground, bounds) -> true);
    var planned = initial.withPlannedBuilding(HOME);
    var completed = new LocationValidator.PlacementContext(List.of(CENTER, HOME), 2, 4,
        (x, z) -> false, (ground, bounds) -> true);

    assertEquals(candidates(completed), candidates(planned));
    assertTrue(candidates(planned).stream().anyMatch(candidate -> candidate.origin().x() == 14),
        "Growth must continue beyond a provisional home, not only around the town center");
    for (var candidate : candidates(planned)) {
      var footprint = new TownLayout.Footprint(candidate.origin().x(), candidate.origin().z(),
          candidate.origin().x() + 4, candidate.origin().z() + 4);
      assertTrue(TownLayout.hasClearance(footprint, TownLayout.MIN_GAP, planned::isClaimed),
          "A provisional building must exclude overlapping and wall-sharing placements");
    }
  }

  @Test
  void reservingAMutableBoundingBoxCopiesItsCoordinates() {
    BoundingBox editable = new BoundingBox(7, 58, -2, 11, 64, 2);
    var context = new LocationValidator.PlacementContext(List.of(CENTER), 1, 4,
        (x, z) -> false, (ground, bounds) -> true).withPlannedBuilding(editable);
    editable.move(100, 0, 100);

    assertTrue(context.isClaimed(9, 0));
    assertFalse(context.isClaimed(109, 100));
  }

  private static List<LocationValidator.PlannedCandidate> candidates(LocationValidator.PlacementContext context) {
    var rotations = new EnumMap<Rotation, BoundingBox>(Rotation.class);
    rotations.put(Rotation.NONE, new BoundingBox(0, 0, 0, 4, 6, 4));
    return LocationValidator.plannedCandidates(context, rotations, BlockPos.ZERO, Direction.SOUTH, 52);
  }
}
