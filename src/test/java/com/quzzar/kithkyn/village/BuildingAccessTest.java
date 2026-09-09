package com.quzzar.kithkyn.village;

import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.junit.jupiter.api.Test;

class BuildingAccessTest {
  @Test void sharedLanesUseOnlyTheOverlapAndExcludeTheBuildingFootprints() {
    var a = new BoundingBox(0, 64, 0, 9, 80, 9);
    var b = new BoundingBox(11, 60, 3, 17, 76, 6);
    var gap = GradingSurvey.sharedGap(a, b);
    assertNotNull(gap);
    assertEquals(new BoundingBox(10, 0, 3, 10, 0, 6), gap);
    assertEquals(gap, GradingSurvey.sharedGap(b, a));
    assertNull(GradingSurvey.sharedGap(a, new BoundingBox(14, 0, 0, 20, 1, 9)));
    assertNull(GradingSurvey.sharedGap(a, new BoundingBox(11, 0, 11, 20, 1, 20)));
    assertNull(GradingSurvey.sharedGap(a, a));
  }

  @Test void northSouthAndWiderSmallGapsAreIncludedWithoutAnExtraApron() {
    var a = new BoundingBox(-10, 0, -10, -1, 1, -1);
    var b = new BoundingBox(-6, 0, 3, -3, 1, 9);
    assertEquals(new BoundingBox(-6, 0, 0, -3, 0, 2), GradingSurvey.sharedGap(a, b));
    assertEquals(GradingSurvey.sharedGap(a, b), GradingSurvey.sharedGap(b, a));
  }

  @Test void unwornLipBetweenEqualBuildingFloorsGetsTheGentlePathGrade() {
    int[] surface = {68, 69, 68};
    boolean[] fixed = {true, false, true};
    assertArrayEquals(new int[]{68, 68, 68}, GradingTargets.plan(3, 1, surface, surface,
        fixed, new boolean[]{false, true, false}, new boolean[]{false, true, false}));
    int[] unequal = {68, 69, 70};
    assertArrayEquals(unequal, GradingTargets.plan(3, 1, unequal, unequal,
        fixed, new boolean[]{false, true, false}, new boolean[]{false, true, false}));
  }

  @Test void openFrontIsOutsideTheActualWorldFootprintOnEverySide() {
    var bounds = new BoundingBox(-10, 64, -20, 0, 80, -10);
    assertEquals(new BlockPos(-11, 69, -15), LocationManager.openFront(bounds, Direction.WEST, 69));
    assertEquals(new BlockPos(1, 69, -15), LocationManager.openFront(bounds, Direction.EAST, 69));
    assertEquals(new BlockPos(-5, 69, -21), LocationManager.openFront(bounds, Direction.NORTH, 69));
    assertEquals(new BlockPos(-5, 69, -9), LocationManager.openFront(bounds, Direction.SOUTH, 69));
  }
}
