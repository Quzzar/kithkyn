package com.quzzar.kithkyn.entities.ai.goals.work;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.village.Village;

import net.minecraft.core.BlockPos;

/** Shared selection and safe hand access for cooking, recovery, and fireside gathering. */
public final class CampfireAccess {
  public static final double REACH_SQR = 9.0D;

  /** The fire being used and the supported, reachable ground beside it. */
  public record Target(BlockPos fire, BlockPos approach) { }

  private CampfireAccess() { }

  /** Choose the nearest usable authored fire; full cooking fires and blocked routes are skipped. */
  @Nullable
  public static Target select(RealPerson person, Village village, boolean cooking, double range) {
    return nearest(person.blockPosition(), village.getCampfirePositions(),
        fire -> fire.closerToCenterThan(person.position(), range) && usable(person, fire, cooking),
        fire -> ContainerAccess.approachTo(person, fire, REACH_SQR));
  }

  /** Spread social visits across the authored fires, with a stable personal preference for each visit. */
  @Nullable
  public static Target gathering(RealPerson person, Village village) {
    List<BlockPos> fires = village.getCampfirePositions();
    if (fires.isEmpty()) return null;
    int first = Math.floorMod(person.getUUID().hashCode() + person.tickCount / 1200, fires.size());
    for (int offset = 0; offset < fires.size(); offset++) {
      BlockPos fire = fires.get((first + offset) % fires.size());
      if (!usable(person, fire, false)) continue;
      BlockPos approach = ContainerAccess.approachTo(person, fire, REACH_SQR);
      if (approach != null) return new Target(fire, approach);
    }
    return null;
  }

  /** Keep filtering order independent from world access so the fallback policy can be checked directly. */
  @Nullable
  static Target nearest(BlockPos from, List<BlockPos> fires, Predicate<BlockPos> usable,
      Function<BlockPos, BlockPos> approach) {
    for (BlockPos fire : fires.stream().distinct()
        .sorted(Comparator.comparingDouble(from::distSqr)).toList()) {
      if (!usable.test(fire)) continue;
      BlockPos standing = approach.apply(fire);
      if (standing != null) return new Target(fire, standing);
    }
    return null;
  }

  /** Loaded lit fires can be visited; starting a roast also requires an unoccupied cooking slot. */
  public static boolean usable(RealPerson person, BlockPos fire, boolean cooking) {
    if (!person.level().hasChunkAt(fire)) return false;
    var campfire = CampfireRoast.litFireAt(person.level(), fire);
    return campfire != null && (!cooking || CampfireRoast.hasFreeSlot(campfire));
  }

  /** Actual hand access must stay clear after selection; a floor or wall is not arm's reach. */
  public static boolean inReach(RealPerson person, BlockPos fire) {
    return ContainerAccess.canReach(person, person.getEyePosition(), fire, REACH_SQR);
  }
}
