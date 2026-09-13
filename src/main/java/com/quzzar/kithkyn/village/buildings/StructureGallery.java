package com.quzzar.kithkyn.village.buildings;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import com.quzzar.kithkyn.Kithkyn;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Places every loaded building definition side by side on one grass datum, so a
 * whole catalogue can be walked end to end and compared at production seating
 * height. Built for reviewing candidate structures (docs/structure-sourcing.md)
 * and for checking a content pass, not for anything the simulation uses.
 *
 * <p>Buildings are grouped by category and then by level, so a category's
 * progression reads left to right and unrelated categories never interleave.
 */
public class StructureGallery {

  /** Empty blocks left between plinths, so neighbours never read as one build. */
  private static final int AISLE = 6;

  /** Plinths per row before the gallery wraps to a new one. */
  private static final int PER_ROW = 6;

  /** Dirt below the visible grass, deep enough for the deepest authored sink. */
  private static final int GROUND_DEPTH = 7;

  /** One entry in the layout: a definition and the footprint it needs. */
  private record Plot(BuildingInfo info, StructureTemplate template, BoundingBox bounds) {
    int sizeX() {
      return bounds.getXSpan();
    }

    int sizeZ() {
      return bounds.getZSpan();
    }
  }

  /** One plot's north-west surface corner within the complete gallery. */
  private record Placement(Plot plot, int x, int z) {}

  /**
   * Builds the gallery with its north-west corner at {@code origin}.
   *
   * @return the number of definitions placed, or -1 if none are loaded
   */
  public static int build(ServerLevel level, BlockPos origin, Random random) {
    List<Plot> plots = collectPlots(level);
    if (plots.isEmpty()) {
      return -1;
    }

    List<Placement> placements = layout(plots);
    int width = placements.stream().mapToInt(plot -> plot.x() + plot.plot().sizeX()).max().orElse(0);
    int depth = placements.stream().mapToInt(plot -> plot.z() + plot.plot().sizeZ()).max().orElse(0);
    prepareGround(level, origin, width, depth);

    int placed = 0;
    for (Placement placement : placements) {
      Plot plot = placement.plot();
      BlockPos footprintCorner = origin.offset(placement.x(), 0, placement.z());
      BlockPos templateOrigin = footprintCorner.offset(-plot.bounds().minX(), 0, -plot.bounds().minZ());
      placeLabel(level, footprintCorner.offset(0, 1, -2), plot.info());
      if (place(level, plot.info(), templateOrigin, random)) {
        placed++;
      }
    }

    return placed;
  }

  /** Keeps layout independent of world writes so the complete grass extent is known first. */
  private static List<Placement> layout(List<Plot> plots) {
    List<Placement> placements = new ArrayList<>();
    int cursorX = 0;
    int cursorZ = 0;
    int rowDepth = 0;
    int inRow = 0;

    for (Plot plot : plots) {
      if (inRow == PER_ROW) {
        cursorX = 0;
        cursorZ += rowDepth + AISLE;
        rowDepth = 0;
        inRow = 0;
      }

      placements.add(new Placement(plot, cursorX, cursorZ));
      cursorX += plot.sizeX() + AISLE;
      rowDepth = Math.max(rowDepth, plot.sizeZ());
      inRow++;
    }
    return placements;
  }

  /** Loads every definition's template, dropping any whose structure file is missing. */
  private static List<Plot> collectPlots(ServerLevel level) {
    List<Plot> plots = new ArrayList<>();
    for (BuildingInfo info : Buildings.allBuildings().values()) {
      ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Kithkyn.MODID, info.getPath());
      StructureTemplate template = level.getStructureManager().get(id).orElse(null);
      if (template == null) {
        Kithkyn.LOGGER.warn("Gallery: no structure file for {}, skipping", info.getName());
        continue;
      }
      plots.add(new Plot(info, template, BuildingFootprint.bounds(template, Rotation.NONE)));
    }

    plots.sort(Comparator.comparing((Plot p) -> p.info().getCategory())
        .thenComparing(p -> p.info().getVariant())
        .thenComparingInt(p -> p.info().getLevel())
        .thenComparing(p -> p.info().getName()));
    return plots;
  }

  /** A continuous grass surface whose top block is the same ground plane production placement scores. */
  private static void prepareGround(ServerLevel level, BlockPos origin, int width, int depth) {
    for (int x = -3; x <= width + 2; x++) {
      for (int z = -3; z <= depth + 2; z++) {
        BlockPos surface = origin.offset(x, 0, z);
        level.setBlock(surface, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        for (int below = 1; below < GROUND_DEPTH; below++) {
          level.setBlock(surface.below(below), Blocks.DIRT.defaultBlockState(), 2);
        }
      }
    }
  }

  /** A sign in front of each plinth carrying the definition's id and shape. */
  private static void placeLabel(ServerLevel level, BlockPos pos, BuildingInfo info) {
    level.setBlock(pos.below(), Blocks.SMOOTH_STONE.defaultBlockState(), 2);
    level.setBlock(pos, Blocks.OAK_SIGN.defaultBlockState(), 3);
    if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
      String design = info.getName().contains("__")
          ? info.getName().substring(info.getName().indexOf("__") + 2) : "canonical";
      SignText text = sign.getFrontText()
          .setMessage(0, Component.literal(info.getCategory()))
          .setMessage(1, Component.literal(info.getVariant()))
          .setMessage(2, Component.literal("level " + info.getLevel() + " sink " + info.getSink()))
          .setMessage(3, Component.literal(design));
      sign.setText(text, true);
      sign.setChanged();
    }
  }

  /** Places one definition through the normal instant-build path. */
  private static boolean place(ServerLevel level, BuildingInfo info, BlockPos templateOrigin, Random random) {
    Building building = new Building(templateOrigin, info.getName(), Rotation.NONE);
    InstantBuildStructure structure = new InstantBuildStructure(building, random, level)
        .seatAtOrigin(seatedOrigin(templateOrigin, info), new HashSet<>());
    return structure.buildInstantly();
  }

  /** Applies the same authored sink as ordinary founding, growth, and exact dev placement. */
  static BlockPos seatedOrigin(BlockPos surfaceOrigin, BuildingInfo info) {
    return surfaceOrigin.below(info.getSink());
  }

}
