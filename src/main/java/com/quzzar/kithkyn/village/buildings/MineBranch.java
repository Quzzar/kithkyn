package com.quzzar.kithkyn.village.buildings;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * The durable identity of one second-generation mine shaft.
 *
 * <p>The blocks already removed are deliberately not saved here. The world is
 * the excavation record, just as it is for the original shaft. A branch only
 * records which root work station and completed rib own the child, plus whether
 * that child has been fully worked out, so navigation and the top-down work
 * order survive a reload.
 */
public record MineBranch(long rootStation, int sourceDepth, int side, boolean complete) {

  public static final Codec<MineBranch> CODEC = RecordCodecBuilder.create(inst -> inst.group(
      Codec.LONG.fieldOf("root_station").forGetter(MineBranch::rootStation),
      Codec.INT.fieldOf("source_depth").forGetter(MineBranch::sourceDepth),
      Codec.INT.fieldOf("side").forGetter(MineBranch::side),
      Codec.BOOL.optionalFieldOf("complete", false).forGetter(MineBranch::complete)
  ).apply(inst, MineBranch::new));

  public MineBranch {
    if (sourceDepth < MineShaft.RIB_MIN_LINE || sourceDepth % MineShaft.RIB_PITCH != 0) {
      throw new IllegalArgumentException("A mine branch must start on a root rib line");
    }
    if (side != -1 && side != 1) {
      throw new IllegalArgumentException("A mine branch side must be -1 or 1");
    }
  }

  public MineBranch completed() {
    return complete ? this : new MineBranch(rootStation, sourceDepth, side, true);
  }

  /** Root ribs in the deterministic order the miner considers for child shafts. */
  public static List<MineBranch> candidates(long rootStation, int deepestRootColumn) {
    List<MineBranch> candidates = new ArrayList<>();
    for (int depth = MineShaft.RIB_MIN_LINE;
        depth <= deepestRootColumn; depth += MineShaft.RIB_PITCH) {
      candidates.add(new MineBranch(rootStation, depth, 1, false));
      candidates.add(new MineBranch(rootStation, depth, -1, false));
    }
    return List.copyOf(candidates);
  }

  /** Cheap rejection for consecutive same-side ribs of one root. */
  public boolean overlapsRootSibling(MineBranch other) {
    return rootStation == other.rootStation
        && side == other.side
        && Math.abs(sourceDepth - other.sourceDepth) <= MineShaft.RADIUS * 2;
  }

}
