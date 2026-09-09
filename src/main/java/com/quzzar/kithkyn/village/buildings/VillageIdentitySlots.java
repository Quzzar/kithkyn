package com.quzzar.kithkyn.village.buildings;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;

/**
 * Semantic positions authored into a concrete building definition. They let a
 * structure opt individual accents into a village's colors without turning the
 * entire regional architecture into a global palette swap.
 */
public record VillageIdentitySlots(
    List<BlockPos> primaryBlocks,
    List<BlockPos> secondaryBlocks,
    List<BlockPos> banners) {

  public static final VillageIdentitySlots EMPTY = new VillageIdentitySlots(List.of(), List.of(), List.of());

  public static final Codec<VillageIdentitySlots> CODEC = RecordCodecBuilder.create(inst -> inst.group(
      BlockPos.CODEC.listOf().optionalFieldOf("primary_blocks", List.of())
          .forGetter(VillageIdentitySlots::primaryBlocks),
      BlockPos.CODEC.listOf().optionalFieldOf("secondary_blocks", List.of())
          .forGetter(VillageIdentitySlots::secondaryBlocks),
      BlockPos.CODEC.listOf().optionalFieldOf("banners", List.of())
          .forGetter(VillageIdentitySlots::banners)
  ).apply(inst, VillageIdentitySlots::new));

  public VillageIdentitySlots {
    primaryBlocks = List.copyOf(primaryBlocks);
    secondaryBlocks = List.copyOf(secondaryBlocks);
    banners = List.copyOf(banners);
  }
}
