package com.quzzar.kithkyn.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class FishingCastTest {
  private final Vec3 rod = new Vec3(1, 65, 1);
  private final BlockPos water = new BlockPos(5, 63, 1);

  @Test
  void castsFromTheRodAndReturnsToItAtCatchTime() {
    assertEquals(rod, FishingCast.bobber(rod, water, 0));
    assertEquals(rod, FishingCast.bobber(rod, water, FishingCast.CATCH_TICKS));
    Vec3 halfway = FishingCast.bobber(rod, water, FishingCast.CAST_TICKS / 2F);
    assertTrue(halfway.x > rod.x && halfway.x < water.getX());
    assertTrue(halfway.y > rod.lerp(new Vec3(5.5, 63.9, 1.5), 0.5).y);
  }

  @Test
  void waitsAtTheSelectedWaterThroughoutTheCatchInterval() {
    for (int age = 12; age < 390; age++) {
      Vec3 floating = FishingCast.bobber(rod, water, age);
      assertEquals(5.5, floating.x);
      assertEquals(1.5, floating.z);
      assertEquals(63.9, floating.y, 0.026);
    }
  }
}
