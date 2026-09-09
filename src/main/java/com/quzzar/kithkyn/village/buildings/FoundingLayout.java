package com.quzzar.kithkyn.village.buildings;

import com.quzzar.kithkyn.savedata.PlacedBlockStore;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.VillageManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Chooses centered founding companions, preferring two-block lanes and inward fronts. */
public final class FoundingLayout {
  private FoundingLayout() { }

  public record Plan(InstantBuildStructure mine, InstantBuildStructure storehouse, int blocksMoved) { }

  record Candidate(InstantBuildStructure structure, Direction side, BoundingBox worldBounds, int cost,
      TownLayout.Preference preference) { }
  record Pair(Candidate mine, Candidate storehouse) {
    int cost() { return mine.cost() + storehouse.cost(); }
    boolean opposite() { return mine.side().getOpposite() == storehouse.side(); }
    TownLayout.Preference preference() {
      int gap = Math.min(TownLayout.clearGap(footprint(mine.worldBounds()), footprint(storehouse.worldBounds())),
          Math.min(mine.preference().gap(), storehouse.preference().gap()));
      return new TownLayout.Preference(gap,
          mine.preference().inwardFronts() + storehouse.preference().inwardFronts());
    }
  }

  /** Tries preferred geometry first; fallback scans are spent only when it cannot form a safe pair. */
  public static Optional<Plan> plan(Village village, InstantBuildStructure center, int planeY, Random random,
      boolean loadChunks) {
    BoundingBox anchor = worldBounds(center);
    if (!suitable(village, center, planeY, loadChunks)) return Optional.empty();
    List<Candidate> mines = new ArrayList<>();
    List<Candidate> stores = new ArrayList<>();
    for (TownLayout.Preference preference : TownLayout.PREFERENCES) {
      mines.addAll(candidates(village, anchor, Buildings.FOUNDING_MINE_CATEGORY,
          planeY, preference, random, loadChunks));
      stores.addAll(candidates(village, anchor, Buildings.FOUNDING_STOREHOUSE_CATEGORY,
          planeY, preference, random, loadChunks));
      Optional<Pair> pair = choose(mines, stores);
      if (pair.isPresent() && pair.get().preference().gap() >= preference.gap()
          && pair.get().preference().inwardFronts() >= preference.inwardFronts() * 2) {
        return pair.map(chosen -> new Plan(chosen.mine().structure(), chosen.storehouse().structure(), chosen.cost()));
      }
    }
    return Optional.empty();
  }

  static Optional<Pair> choose(List<Candidate> mines, List<Candidate> stores) {
    List<Pair> pairs = new ArrayList<>();
    for (Candidate mine : mines) {
      for (Candidate store : stores) {
        if (mine.side() != store.side()
            && TownLayout.clearGap(footprint(mine.worldBounds()), footprint(store.worldBounds())) >= TownLayout.MIN_GAP) {
          pairs.add(new Pair(mine, store));
        }
      }
    }
    return pairs.stream().min(Comparator.comparing(Pair::preference, TownLayout.PREFERRED_FIRST)
        .thenComparingInt(Pair::cost).thenComparing(Pair::opposite));
  }

  private static List<Candidate> candidates(Village village, BoundingBox anchor, String category,
      int planeY, TownLayout.Preference preference, Random random, boolean loadChunks) {
    BuildingInfo info = Buildings.resolve(category, 1, village.getStyle());
    if (info == null) return List.of();
    var level = village.getLevel();
    List<Candidate> result = new ArrayList<>();
    for (Direction side : Direction.Plane.HORIZONTAL) {
      Rotation inward = info.rotationFacing(side.getOpposite());
      for (Rotation rotation : Rotation.values()) {
        if ((rotation == inward) != (preference.inwardFronts() > 0)) continue;
        var probe = new InstantBuildStructure(new Building(info.getName(), rotation), random, level);
        BoundingBox bounds = probe.getBounds();
        for (var origin : frontageOrigins(anchor, bounds, side, preference.gap())) {
          BlockPos at = new BlockPos(origin.x(), planeY - 1 - info.getSink(), origin.z());
          var structure = new InstantBuildStructure(new Building(info.getName(), rotation), random, level)
              .withIdentity(village.getIdentity()).seatAtOrigin(at, new HashSet<>());
          BoundingBox world = worldBounds(structure);
          if (!readable(village, world, loadChunks)) continue;
          // Score local bounds once at the shared ground plane, not the sunk basement origin.
          var cost = SitePreparation.score(level, village, new BlockPos(at.getX(), planeY - 1, at.getZ()), bounds);
          if (!cost.impossible() && unowned(village, world, planeY)) {
            result.add(new Candidate(structure, side, world, cost.blocksMoved(), preference));
          }
        }
      }
    }
    return result;
  }

  /** Founding centers each companion on its chosen side instead of offering edge-aligned alternatives. */
  static List<TownLayout.Origin> frontageOrigins(BoundingBox anchor, BoundingBox candidate, Direction side, int gap) {
    return List.of(TownLayout.centeredFrontageOrigin(
        footprint(anchor), footprint(candidate), gap, side));
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

  private static TownLayout.Footprint footprint(BoundingBox bounds) {
    return new TownLayout.Footprint(bounds.minX(), bounds.minZ(), bounds.maxX(), bounds.maxZ());
  }

  /** The same terrain and ownership rules apply to the center, companions and delayed commits. */
  public static boolean suitable(Village village, InstantBuildStructure structure, int planeY, boolean loadChunks) {
    BoundingBox bounds = worldBounds(structure);
    if (!readable(village, bounds, loadChunks)) return false;
    BlockPos origin = BlockPos.of(structure.getBuilding().getOriginLocation());
    return !SitePreparation.score(village.getLevel(), village,
        new BlockPos(origin.getX(), planeY - 1, origin.getZ()), structure.getBounds()).impossible()
        && unowned(village, bounds, planeY);
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
