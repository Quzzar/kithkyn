package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.Rotation;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.quzzar.kithkyn.village.VillageIdentity;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class VillageIdentityApplierTest {

  @Test
  void placementRotatesSlotsAndColorsEitherBedHalfBeforeTheSecondHalfExists() {
    BuildingInfo info = BuildingInfo.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
        {"structure":"identity_fixture_1","village_identity":{
          "primary_blocks":[[1,1,1]],"secondary_blocks":[[2,1,1]],"banners":[[3,1,1]]}}
        """)).getOrThrow();
    Buildings.reload(Map.of(info.getName(), info));
    try {
      VillageIdentity identity = new VillageIdentity("Fixture", DyeColor.PURPLE, DyeColor.LIME, List.of());
      BlockPos origin = new BlockPos(100, 70, -200);
      for (Rotation rotation : Rotation.values()) {
        Building building = new Building(origin, info.getName(), rotation);
        var placement = VillageIdentityApplier.placement(building, identity);
        BlockPos head = origin.offset(new BlockPos(1, 1, 1).rotate(rotation));
        var state = Blocks.BLUE_BED.defaultBlockState().setValue(BedBlock.PART, BedPart.HEAD)
            .setValue(BedBlock.FACING, Direction.SOUTH).rotate(rotation);
        assertEquals(Blocks.PURPLE_BED, placement.state(head, state).getBlock());
        BlockPos foot = VillageIdentityApplier.bedPartner(head, state);
        assertEquals(Blocks.PURPLE_BED,
            placement.state(foot, state.setValue(BedBlock.PART, BedPart.FOOT)).getBlock());
        BlockPos accent = origin.offset(new BlockPos(2, 1, 1).rotate(rotation));
        assertEquals(Blocks.LIME_WOOL, placement.state(accent, Blocks.YELLOW_WOOL.defaultBlockState()).getBlock());
        BlockPos banner = origin.offset(new BlockPos(3, 1, 1).rotate(rotation));
        var flag = Blocks.BLUE_WALL_BANNER.defaultBlockState().rotate(rotation);
        var recolored = placement.state(banner, flag);
        assertEquals(Blocks.PURPLE_WALL_BANNER, recolored.getBlock());
        assertEquals(flag.getValue(HorizontalDirectionalBlock.FACING), recolored.getValue(HorizontalDirectionalBlock.FACING));
        assertSame(flag, placement.state(banner.above(), flag));
      }
    } finally {
      Buildings.reload(Map.of());
    }
  }

  @Test
  void recolorsAnAuthoredAccentWithinItsMaterialFamily() {
    assertEquals(Blocks.BLUE_WOOL.defaultBlockState(),
        VillageIdentityApplier.recolor(Blocks.RED_WOOL.defaultBlockState(), DyeColor.BLUE));
    assertEquals(Blocks.YELLOW_CARPET.defaultBlockState(),
        VillageIdentityApplier.recolor(Blocks.WHITE_CARPET.defaultBlockState(), DyeColor.YELLOW));
  }

  @Test
  void keepsSharedPropertiesWhileChangingColor() {
    var source = Blocks.RED_GLAZED_TERRACOTTA.defaultBlockState()
        .setValue(HorizontalDirectionalBlock.FACING, net.minecraft.core.Direction.WEST);
    var recolored = VillageIdentityApplier.recolor(source, DyeColor.CYAN);

    assertEquals(Blocks.CYAN_GLAZED_TERRACOTTA, recolored.getBlock());
    assertEquals(net.minecraft.core.Direction.WEST, recolored.getValue(HorizontalDirectionalBlock.FACING));
  }

  @Test
  void leavesOrdinaryArchitectureUnchanged() {
    var stone = Blocks.COBBLED_DEEPSLATE.defaultBlockState();
    assertSame(stone, VillageIdentityApplier.recolor(stone, DyeColor.PURPLE));
  }

  @Test
  void bedColorsPreserveFacingPartAndOccupancyForEveryOrientation() {
    for (Direction facing : Direction.Plane.HORIZONTAL) {
      for (BedPart part : BedPart.values()) {
        var source = Blocks.WHITE_BED.defaultBlockState().setValue(BedBlock.FACING, facing)
            .setValue(BedBlock.PART, part).setValue(BedBlock.OCCUPIED, true);
        var result = VillageIdentityApplier.recolor(source, DyeColor.BLUE);
        assertEquals(Blocks.BLUE_BED, result.getBlock());
        assertEquals(facing, result.getValue(BedBlock.FACING));
        assertEquals(part, result.getValue(BedBlock.PART));
        assertTrue(result.getValue(BedBlock.OCCUPIED));
        var partner = source.setValue(BedBlock.PART, part == BedPart.HEAD ? BedPart.FOOT : BedPart.HEAD);
        BlockPos other = VillageIdentityApplier.bedPartner(BlockPos.ZERO, source);
        assertEquals(BlockPos.ZERO, VillageIdentityApplier.bedPartner(other, partner));
        assertTrue(VillageIdentityApplier.isMatchingBedPartner(source, partner));
        assertFalse(VillageIdentityApplier.isMatchingBedPartner(source, source));
        assertFalse(VillageIdentityApplier.isMatchingBedPartner(source, Blocks.AIR.defaultBlockState()));
        assertFalse(VillageIdentityApplier.isMatchingBedPartner(source,
            partner.setValue(BedBlock.FACING, facing.getOpposite())));
      }
    }
  }
}
