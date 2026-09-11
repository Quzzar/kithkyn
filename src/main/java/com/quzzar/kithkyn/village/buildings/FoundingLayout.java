package com.quzzar.kithkyn.village.buildings;

import com.quzzar.kithkyn.savedata.PlacedBlockStore;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.VillageManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Surveys a complete starting settlement with the same search used by later construction. */
public final class FoundingLayout {
  private FoundingLayout() { }

  /** Every accepted building becomes a provisional frontage anchor, without publishing live claims. */
  public static Optional<List<InstantBuildStructure>> plan(Village village, InstantBuildStructure center,
      Random random, boolean loadChunks) {
    if (!suitable(village, center, loadChunks)) return Optional.empty();
    var definitions = Buildings.foundingCompanions(center.getBuilding().getInfo(), village.getStyle());
    if (definitions.isEmpty()) return Optional.empty();
    var context = new LocationValidator.PlacementContext(List.of(worldBounds(center)), 1,
        center.getBuilding().getRadius(),
        (x, z) -> village.hasClaimed(new BlockPos(x, 0, z)),
        (ground, bounds) -> unowned(village,
            bounds.moved(ground.getX(), ground.getY(), ground.getZ()), ground.getY() + 1));
    BlockPos anchor = BlockPos.of(center.getBuilding().getCenterLocation()).below();
    List<InstantBuildStructure> structures = new ArrayList<>();
    for (BuildingInfo info : definitions.get()) {
      var template = village.getLevel().getStructureManager().getOrCreate(
          net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.quzzar.kithkyn.Kithkyn.MODID, info.getPath()));
      var search = LocationValidator.findValidLocation(village.getLevel(), anchor, template,
          info.getEntranceFacing(), info.getSink(), List.of(Rotation.values()), village, context, random);
      if (!search.found()) return Optional.empty();
      var structure = new InstantBuildStructure(new Building(info.getName(), search.rotation()), random, village.getLevel())
          .withIdentity(village.getIdentity()).seatAtOrigin(search.site().below(info.getSink()), new HashSet<>());
      if (!suitable(village, structure, false)) return Optional.empty();
      structures.add(structure);
      context = context.withPlannedBuilding(worldBounds(structure));
    }
    return Optional.of(List.copyOf(structures));
  }

  /** Refuse protected columns before founding's instant clearing can touch them. */
  public static boolean unowned(Village village, BoundingBox bounds, int planeY) {
    if (!readable(village, bounds, false)) return false;
    var level = village.getLevel();
    var ownership = PlacedBlockStore.get(level);
    var neighbours = VillageManager.get(level).getVillages().values();
    for (int x = bounds.minX() - LocationValidator.MIN_GAP; x <= bounds.maxX() + LocationValidator.MIN_GAP; x++) {
      for (int z = bounds.minZ() - LocationValidator.MIN_GAP; z <= bounds.maxZ() + LocationValidator.MIN_GAP; z++) {
        BlockPos column = new BlockPos(x, 0, z);
        for (Village other : neighbours) {
          if (other.hasClaimed(column)) return false;
        }
        for (int y = Math.min(bounds.minY(), planeY - 6); y <= Math.max(bounds.maxY(), planeY + 24); y++) {
          BlockPos pos = new BlockPos(x, y, z);
          if (ownership.isPlayerPlaced(pos) || ownership.isVillagePlaced(pos) || level.getBlockEntity(pos) != null) {
            return false;
          }
        }
      }
    }
    return true;
  }

  public static BoundingBox worldBounds(InstantBuildStructure structure) {
    BlockPos at = BlockPos.of(structure.getBuilding().getOriginLocation());
    return structure.getBounds().moved(at.getX(), at.getY(), at.getZ());
  }

  /** The same terrain and ownership rules apply to the center, companions and delayed commits. */
  public static boolean suitable(Village village, InstantBuildStructure structure, boolean loadChunks) {
    var template = village.getLevel().getStructureManager().getOrCreate(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.quzzar.kithkyn.Kithkyn.MODID,
            structure.getBuilding().getInfo().getPath()));
    if (template.getSize().getX() <= 0 || template.getSize().getY() <= 0 || template.getSize().getZ() <= 0) return false;
    BoundingBox bounds = worldBounds(structure);
    if (!readable(village, bounds, loadChunks)) return false;
    BlockPos origin = BlockPos.of(structure.getBuilding().getOriginLocation());
    return !SitePreparation.score(village.getLevel(), village,
        origin.above(structure.getBuilding().getInfo().getSink()), structure.getBounds()).impossible()
        && unowned(village, bounds, origin.getY() + structure.getBuilding().getInfo().getSink() + 1);
  }

  /** Natural probes and delayed commits never synchronously load a missing chunk. */
  private static boolean readable(Village village, BoundingBox bounds, boolean loadChunks) {
    for (int x = (bounds.minX() - 1) >> 4; x <= (bounds.maxX() + 1) >> 4; x++) {
      for (int z = (bounds.minZ() - 1) >> 4; z <= (bounds.maxZ() + 1) >> 4; z++) {
        if (loadChunks) village.getLevel().getChunk(x, z);
        else if (!village.getLevel().isLoaded(new BlockPos(x << 4, 0, z << 4))) return false;
      }
    }
    return true;
  }
}
