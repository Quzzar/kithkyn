package com.quzzar.kithkyn.entities.ai.goals;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.village.Occupation;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.VillageGolems;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.ai.goal.Goal;

/** Helps local guards and intervenes when a nearby village member is under attack. */
public final class GolemDefendVillageGoal extends Goal {
  private final AbstractGolem golem;
  private LivingEntity threat;
  private long nextScan;

  public GolemDefendVillageGoal(AbstractGolem golem) {
    this.golem = golem;
    setFlags(EnumSet.of(Flag.TARGET));
  }

  @Override
  public boolean canUse() {
    Village village = VillageGolems.village(golem);
    if (village == null || golem.level().getGameTime() < nextScan) {
      return false;
    }
    nextScan = golem.level().getGameTime() + 20;
    Set<LivingEntity> threats = new HashSet<>();
    for (LivingEntity nearby : golem.level().getEntitiesOfClass(LivingEntity.class,
        golem.getBoundingBox().inflate(24), LivingEntity::isAlive)) {
      if (nearby instanceof Mob mob && VillageGolems.isMember(village, mob.getTarget())) {
        threats.add(nearby);
      }
      if (VillageGolems.isMember(village, nearby)) {
        if (nearby instanceof RealPerson guard && guard.getOccupation() == Occupation.GUARD
            && guard.getTarget() != null) {
          threats.add(guard.getTarget());
        }
        if (nearby.getLastHurtByMob() != null
            && nearby.tickCount - nearby.getLastHurtByMobTimestamp() <= 100) {
          threats.add(nearby.getLastHurtByMob());
        }
      }
    }
    threat = threats.stream().filter(target -> canDefendAgainst(village, target))
        .filter(golem::hasLineOfSight)
        .min(Comparator.comparingDouble(golem::distanceToSqr)).orElse(null);
    return threat != null;
  }

  private boolean canDefendAgainst(Village village, LivingEntity target) {
    return target.isAlive() && target.level() == golem.level()
        && !VillageGolems.isMember(village, target) && golem.canAttack(target)
        && golem.distanceToSqr(target) <= 32 * 32;
  }

  @Override
  public boolean canContinueToUse() {
    return threat != null && VillageGolems.village(golem) != null && golem.getTarget() == threat
        && canDefendAgainst(VillageGolems.village(golem), threat);
  }

  @Override
  public void start() {
    golem.setTarget(threat);
  }

  @Override
  public void stop() {
    if (golem.getTarget() == threat) {
      golem.setTarget(null);
    }
    threat = null;
  }
}
