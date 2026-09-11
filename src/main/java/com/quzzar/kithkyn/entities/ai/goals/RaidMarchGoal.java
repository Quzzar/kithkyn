package com.quzzar.kithkyn.entities.ai.goals;

import java.util.EnumSet;

import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.VillageManager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * A raider with nobody to fight walks on the village they were sent against
 * (docs/undead.md), so a war party that spawned at the edge ends up at the
 * fire rather than milling where it stood. Fighting always wins: the goal
 * yields the moment a target is picked. A raider whose raid is over, or whose
 * village is gone, has no reason to exist and leaves the world.
 */
public final class RaidMarchGoal extends Goal {

  private static final double CLOSE_ENOUGH = 6.0D;
  private static final int REPATH_TICKS = 40;

  private final RealPerson raider;
  private int repath;

  public RaidMarchGoal(RealPerson raider) {
    this.raider = raider;
    setFlags(EnumSet.of(Flag.MOVE));
  }

  @Override
  public boolean canUse() {
    if (raider.getTarget() != null || !(raider.level() instanceof ServerLevel level)) {
      return false;
    }
    Village village = VillageManager.get(level).getVillage(raider.getRaidTargetVillageId());
    if (village == null || !village.isUnderRaid()) {
      raider.discard();
      return false;
    }
    BlockPos center = village.getGatheringPoint();
    return center != null && !center.equals(BlockPos.ZERO)
        && !center.closerToCenterThan(raider.position(), CLOSE_ENOUGH);
  }

  @Override
  public boolean canContinueToUse() {
    return raider.getTarget() == null && !raider.getNavigation().isDone();
  }

  @Override
  public void start() {
    repath = 0;
    march();
  }

  @Override
  public void tick() {
    if (++repath >= REPATH_TICKS) {
      repath = 0;
      march();
    }
  }

  private void march() {
    if (raider.level() instanceof ServerLevel level) {
      Village village = VillageManager.get(level).getVillage(raider.getRaidTargetVillageId());
      BlockPos center = village == null ? null : village.getGatheringPoint();
      if (center != null) {
        raider.getNavigation().moveTo(center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D, 1.0D);
      }
    }
  }
}
