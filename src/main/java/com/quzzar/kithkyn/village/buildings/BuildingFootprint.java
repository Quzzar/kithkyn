package com.quzzar.kithkyn.village.buildings;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * The authored building envelope, not the empty rectangle used when capturing it.
 * Coordinates stay in the original template frame, so saved amenities do not move.
 */
public final class BuildingFootprint {
  private static final java.util.Map<StructureTemplate, BoundingBox> CACHE =
      java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

  private BuildingFootprint() { }

  /** Templates are immutable between datapack reloads; do not rescan them during site searches. */
  public static void clearCache() {
    CACHE.clear();
  }

  /** Includes every palette's blocks and decorations, but never exterior air padding. */
  public static BoundingBox bounds(StructureTemplate template, Rotation rotation) {
    BoundingBox local = CACHE.computeIfAbsent(template, BuildingFootprint::measure);
    return BoundingBox.fromCorners(new BlockPos(local.minX(), local.minY(), local.minZ()).rotate(rotation),
        new BlockPos(local.maxX(), local.maxY(), local.maxZ()).rotate(rotation));
  }

  private static BoundingBox measure(StructureTemplate template) {
    BoundingBox result = null;
    for (StructureTemplate.Palette palette : template.palettes) {
      for (StructureTemplate.StructureBlockInfo block : palette.blocks()) {
        if (block.state().isAir() || block.state().is(Blocks.STRUCTURE_VOID)) continue;
        BlockPos pos = block.pos();
        if (result == null) result = new BoundingBox(pos);
        else result.encapsulate(pos);
      }
    }
    return result != null ? result
        : template.getBoundingBox(new StructurePlaceSettings(), BlockPos.ZERO);
  }
}
