package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.block.Rotation;

class MineBranchTest {

  @Test
  void candidatesRunFromTheTopDownAndConsiderBothSides() {
    List<MineBranch> candidates = MineBranch.candidates(42L, 12);

    assertEquals(List.of(
        new MineBranch(42L, 4, 1, false),
        new MineBranch(42L, 4, -1, false),
        new MineBranch(42L, 8, 1, false),
        new MineBranch(42L, 8, -1, false),
        new MineBranch(42L, 12, 1, false),
        new MineBranch(42L, 12, -1, false)), candidates);
  }

  @Test
  void consecutiveSameSideChildrenAreRejectedWithoutAGeometryScan() {
    MineBranch first = new MineBranch(7L, 4, 1, false);

    assertTrue(first.overlapsRootSibling(new MineBranch(7L, 8, 1, false)));
    assertFalse(first.overlapsRootSibling(new MineBranch(7L, 12, 1, false)));
    assertFalse(first.overlapsRootSibling(new MineBranch(7L, 4, -1, false)));
    assertFalse(first.overlapsRootSibling(new MineBranch(8L, 4, 1, false)));
  }

  @Test
  void buildingCodecDefaultsOldSavesToNoBranchesAndRoundTripsNewOnes() {
    Building building = new Building(new BlockPos(3, 70, 9), "mine_birch_forest_1", Rotation.CLOCKWISE_90);
    MineBranch branch = new MineBranch(BlockPos.ZERO.asLong(), 4, 1, false);
    building.addMineBranch(branch);

    CompoundTag encoded = (CompoundTag) Building.CODEC.encodeStart(NbtOps.INSTANCE, building).getOrThrow();
    Building restored = Building.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
    assertEquals(List.of(branch), restored.getMineBranches());

    encoded.remove("mine_branches");
    Building legacy = Building.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
    assertTrue(legacy.getMineBranches().isEmpty());
  }

  @Test
  void upgradesRetainTheMineNetwork() {
    Building mine = new Building(new BlockPos(3, 70, 9), "mine_birch_forest_1", Rotation.NONE);
    MineBranch branch = new MineBranch(BlockPos.ZERO.asLong(), 4, -1, true);
    mine.addMineBranch(branch);

    Building upgraded = Building.upgradeOf(
        mine, "mine_birch_forest_2", new BlockPos(3, 70, 9), Rotation.NONE);

    assertEquals(List.of(branch), upgraded.getMineBranches());
  }
}
