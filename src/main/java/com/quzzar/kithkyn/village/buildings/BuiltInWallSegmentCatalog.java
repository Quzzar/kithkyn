package com.quzzar.kithkyn.village.buildings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;

/** The procedural starter catalog behind the authored wall-segment seam. */
final class BuiltInWallSegmentCatalog implements WallSegmentCatalog {

  static final BuiltInWallSegmentCatalog INSTANCE = new BuiltInWallSegmentCatalog(AuthoredWoodWallSegments.INSTANCE);
  static final BuiltInWallSegmentCatalog BIRCH_FOREST = new BuiltInWallSegmentCatalog(AuthoredWoodWallSegments.BIRCH_FOREST);
  static final BuiltInWallSegmentCatalog ARID = new BuiltInWallSegmentCatalog(AuthoredWoodWallSegments.ARID);
  static final BuiltInWallSegmentCatalog SWAMP = new BuiltInWallSegmentCatalog(AuthoredWoodWallSegments.SWAMP);
  private final AuthoredWoodWallSegments authored;

  /** Long enough to read as a structure, short enough for several builders to share the ring. */
  private static final int MAX_SECTION_LENGTH = 7;
  private static final int WOOD_GATEHOUSE_RADIUS = 8;

  private BuiltInWallSegmentCatalog(AuthoredWoodWallSegments authored) {
    this.authored = authored;
  }

  @Override
  public List<WallSection> compile(List<Long> ring, Set<Long> gates, List<Integer> ground,
      List<Integer> deck, WallTier tier) {
    return compile(ring, gates, ground, deck, tier, Set.of());
  }

  @Override
  public List<WallSection> compile(List<Long> ring, Set<Long> gates, List<Integer> ground,
      List<Integer> deck, WallTier tier, Set<Long> towerExclusions) {
    if (ring.isEmpty() || ground.size() != ring.size() || deck.size() != ring.size()) {
      return List.of();
    }
    List<WallSectionKind> kinds = classify(ring, gates, deck, tier, towerExclusions);
    List<WallSection> sections = new ArrayList<>();
    int from = 0;
    while (from < ring.size()) {
      WallSectionKind kind = kinds.get(from);
      int to = from + 1;
      while (to < ring.size() && to - from < MAX_SECTION_LENGTH && kinds.get(to) == kind) {
        to++;
      }
      sections.add(new WallSection(kind,
          blocksFor(ring, gates, ground, deck, tier, from, to, kind)));
      from = to;
    }
    List<WallSection> normalized = withoutUnsupportedAttachments(
        solidifyCoveredLinearToppers(withoutOverlaps(sections)));
    return thinLinearLanterns(normalized);
  }

  /** A lower run joins the underside of a rigid feature as masonry, not buried battlements. */
  private static List<WallSection> solidifyCoveredLinearToppers(List<WallSection> sections) {
    Set<Long> rigidCells = new HashSet<>();
    for (WallSection section : sections) {
      if (isLinear(section.kind())) continue;
      for (WallBlockPlan cell : section.blocks()) {
        rigidCells.add(cell.position());
      }
    }
    List<WallSection> result = new ArrayList<>();
    for (WallSection section : sections) {
      if (!isLinear(section.kind())) {
        result.add(section);
        continue;
      }
      Map<Long, Integer> tops = new java.util.HashMap<>();
      for (WallBlockPlan cell : section.blocks()) {
        BlockPos pos = cell.pos();
        tops.merge(BlockPos.asLong(pos.getX(), 0, pos.getZ()), pos.getY(), Math::max);
      }
      Set<Long> covered = new HashSet<>();
      tops.forEach((column, y) -> {
        if (rigidCells.contains(BlockPos.of(column).atY(y + 1).asLong())) covered.add(column);
      });
      List<WallBlockPlan> cells = section.blocks().stream().map(cell -> {
        BlockPos pos = cell.pos();
        if (!isTopper(cell.piece()) || !covered.contains(BlockPos.asLong(pos.getX(), 0, pos.getZ()))) return cell;
        WallBlockPlan.Piece piece = cell.piece() == WallBlockPlan.Piece.MOSSY_WALL
            ? WallBlockPlan.Piece.MOSSY_POST : WallBlockPlan.Piece.BODY;
        return new WallBlockPlan(cell.position(), piece, cell.role());
      }).toList();
      result.add(new WallSection(section.kind(), cells));
    }
    return List.copyOf(result);
  }

  private static boolean isTopper(WallBlockPlan.Piece piece) {
    return switch (piece) {
      case PARAPET, SLAB, COBBLE_WALL, MOSSY_WALL, COBBLE_SLAB_BOTTOM, COBBLE_SLAB_TOP,
          TORCH, TORCH_NORTH, TORCH_EAST, TORCH_SOUTH, TORCH_WEST,
          LANTERN, LANTERN_HANGING -> true;
      default -> false;
    };
  }

  /**
   * Feature clearance may win an overlap containing an attachment's support
   * without touching the detail itself. Drop the orphan rather than placing it.
   */
  private static List<WallSection> withoutUnsupportedAttachments(
      List<WallSection> sections) {
    Set<Long> positions = sections.stream()
        .flatMap(section -> section.blocks().stream())
        .map(WallBlockPlan::position)
        .collect(java.util.stream.Collectors.toSet());
    List<WallSection> supported = new ArrayList<>(sections.size());
    for (WallSection section : sections) {
      List<WallBlockPlan> blocks = section.blocks().stream()
          .filter(block -> {
            BlockPos support = switch (block.piece()) {
              case LANTERN -> block.pos().below();
              case LANTERN_HANGING -> block.pos().above();
              case BANNER_NORTH -> block.pos().south();
              case BANNER_EAST -> block.pos().west();
              case BANNER_SOUTH -> block.pos().north();
              case BANNER_WEST -> block.pos().east();
              default -> null;
            };
            return support == null || positions.contains(support.asLong());
          })
          .toList();
      if (!blocks.isEmpty()) {
        supported.add(new WallSection(section.kind(), blocks));
      }
    }
    return List.copyOf(supported);
  }

  /** Keeps towers and gates lit while halving the repeated lantern rhythm on runs. */
  private static List<WallSection> thinLinearLanterns(List<WallSection> sections) {
    List<WallSection> thinned = new ArrayList<>(sections.size());
    int linearOrdinal = 0;
    for (WallSection section : sections) {
      if (!isLinear(section.kind())) {
        thinned.add(section);
        continue;
      }
      boolean keepLantern = Math.floorMod(linearOrdinal++, 2) == 0;
      boolean emittedLantern = false;
      List<WallBlockPlan> blocks = new ArrayList<>(section.blocks().size());
      for (WallBlockPlan block : section.blocks()) {
        if (!isLantern(block.piece())) {
          blocks.add(block);
        } else if (keepLantern && !emittedLantern) {
          blocks.add(block);
          emittedLantern = true;
        }
      }
      thinned.add(new WallSection(section.kind(), blocks));
    }
    return List.copyOf(thinned);
  }

  private static boolean isLinear(WallSectionKind kind) {
    return kind == WallSectionKind.STRAIGHT
        || kind == WallSectionKind.DIAGONAL
        || kind == WallSectionKind.TERRACE;
  }

  private static boolean isLantern(WallBlockPlan.Piece piece) {
    return piece == WallBlockPlan.Piece.LANTERN
        || piece == WallBlockPlan.Piece.LANTERN_HANGING;
  }

  /**
   * Neighboring width bands and towers intentionally overlap. Compile every
   * world position once. Authored empty volume wins over neighboring generated
   * cells, then exact detail wins over a generic barrier, so
   * cost and progress cannot charge twice for the same block.
   */
  private static List<WallSection> withoutOverlaps(List<WallSection> sections) {
    Map<Long, WallBlockPlan> winners = new LinkedHashMap<>();
    for (WallSection section : sections) {
      for (WallBlockPlan block : section.blocks()) {
        WallBlockPlan existing = winners.get(block.position());
        if (existing == null || priority(block.role()) > priority(existing.role())) {
          winners.put(block.position(), block);
        }
      }
    }

    Set<Long> emitted = new HashSet<>();
    List<WallSection> normalized = new ArrayList<>();
    for (WallSection section : sections) {
      List<WallBlockPlan> blocks = section.blocks().stream()
          .filter(block -> block.role() != WallCellRole.CLEARANCE)
          .filter(block -> winners.get(block.position()).equals(block))
          .filter(block -> emitted.add(block.position()))
          .toList();
      if (!blocks.isEmpty()) {
        normalized.add(new WallSection(section.kind(), blocks));
      }
    }
    return List.copyOf(normalized);
  }

  private static int priority(WallCellRole role) {
    return switch (role) {
      case BARRIER -> 0;
      case EXACT, FOUNDATION -> 1;
      case CLEARANCE -> 2;
    };
  }

  private static List<WallSectionKind> classify(List<Long> ring, Set<Long> gates,
      List<Integer> deck, WallTier tier, Set<Long> towerExclusions) {
    List<WallSectionKind> kinds = new ArrayList<>(ring.size());
    int gatehouseRadius = WOOD_GATEHOUSE_RADIUS;
    for (int i = 0; i < ring.size(); i++) {
      Delta incoming = delta(ring, previous(i, ring.size()), i);
      Delta outgoing = delta(ring, i, next(i, ring.size()));
      if (distanceToGate(ring, gates, i) <= gatehouseRadius) {
        kinds.add(WallSectionKind.GATEHOUSE);
      } else if (isTowerAnchor(incoming, outgoing)
          && !towerExclusions.contains(ring.get(i))) {
        kinds.add(WallSectionKind.CORNER_TOWER);
      } else if (deck.get(i).intValue() != deck.get(previous(i, ring.size())).intValue()
          || deck.get(i).intValue() != deck.get(next(i, ring.size())).intValue()) {
        kinds.add(WallSectionKind.TERRACE);
      } else if (outgoing.x() != 0 && outgoing.z() != 0) {
        kinds.add(WallSectionKind.DIAGONAL);
      } else {
        kinds.add(WallSectionKind.STRAIGHT);
      }
    }
    return kinds;
  }

  private List<WallBlockPlan> blocksFor(List<Long> ring, Set<Long> gates,
      List<Integer> ground, List<Integer> deck, WallTier tier, int from, int to,
      WallSectionKind sectionKind) {
    Map<Long, WallBlockPlan> blocks = new LinkedHashMap<>();
    for (int i = from; i < to; i++) {
      long column = ring.get(i);
      int x = BlockPos.getX(column);
      int z = BlockPos.getZ(column);
      int floor = WallRaiser.seamFloor(ground, i);
      int top = deck.get(i);
      addPalisadeColumn(blocks, x, z, floor, top, distanceToGate(ring, gates, i) <= 1);
    }
    for (WallBlockPlan block : this.authored.cellsFor(
        ring, gates, ground, deck, from, to, sectionKind)) {
      put(blocks, block.pos().getX(), block.pos().getY(), block.pos().getZ(),
          block.piece(), block.role());
    }
    return List.copyOf(blocks.values());
  }

  private static void addPalisadeColumn(Map<Long, WallBlockPlan> blocks, int x, int z,
      int floor, int top, boolean gateOpening) {
    for (int y = floor; y <= top; y++) {
      if (!gateOpening) {
        put(blocks, x, y, z, WallBlockPlan.Piece.BODY, WallCellRole.BARRIER);
      }
    }
  }

  private static void put(Map<Long, WallBlockPlan> blocks, int x, int y, int z,
      WallBlockPlan.Piece piece, WallCellRole role) {
    long position = BlockPos.asLong(x, y, z);
    WallBlockPlan existing = blocks.get(position);
    if (existing == null
        || replacementPriority(role) > replacementPriority(existing.role())) {
      blocks.put(position, new WallBlockPlan(position, piece, role));
    }
  }

  private static int replacementPriority(WallCellRole role) {
    return switch (role) {
      case CLEARANCE -> 0;
      case BARRIER -> 1;
      case EXACT -> 2;
      case FOUNDATION -> 3;
    };
  }

  private static Delta delta(List<Long> ring, int from, int to) {
    return new Delta(
        Integer.signum(BlockPos.getX(ring.get(to)) - BlockPos.getX(ring.get(from))),
        Integer.signum(BlockPos.getZ(ring.get(to)) - BlockPos.getZ(ring.get(from))));
  }


  private static boolean isTowerAnchor(Delta incoming, Delta outgoing) {
    return !incoming.equals(outgoing)
        && !isDiagonal(incoming)
        && isDiagonal(outgoing);
  }

  private static boolean isDiagonal(Delta delta) {
    return delta.x() != 0 && delta.z() != 0;
  }

  private static int distanceToGate(List<Long> ring, Set<Long> gates, int index) {
    int best = Integer.MAX_VALUE;
    for (int i = 0; i < ring.size(); i++) {
      if (gates.contains(ring.get(i))) {
        int direct = Math.abs(i - index);
        best = Math.min(best, Math.min(direct, ring.size() - direct));
      }
    }
    return best;
  }

  private static int previous(int index, int size) {
    return Math.floorMod(index - 1, size);
  }

  private static int next(int index, int size) {
    return (index + 1) % size;
  }

  private record Delta(int x, int z) {
  }
}
