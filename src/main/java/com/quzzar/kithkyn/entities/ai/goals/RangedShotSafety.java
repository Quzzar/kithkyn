package com.quzzar.kithkyn.entities.ai.goals;

import com.quzzar.kithkyn.entities.RealPerson;

import net.minecraft.world.entity.LivingEntity;
import com.quzzar.kithkyn.village.VillageGolems;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** The narrow shot corridor, not every friendly body sharing the shooter's platform. */
public final class RangedShotSafety {

  private static final double CLEARANCE = 0.3D;

  private RangedShotSafety() {
  }

  public static boolean blockedByFriendly(LivingEntity shooter, LivingEntity target) {
    Vec3 from = shooter.getEyePosition();
    Vec3 to = target.getBoundingBox().getCenter();
    for (LivingEntity nearby : shooter.level().getEntitiesOfClass(LivingEntity.class,
        new AABB(from, to).inflate(CLEARANCE))) {
      if (nearby == shooter || nearby == target || !nearby.isAlive()) {
        continue;
      }
      if ((nearby instanceof RealPerson || nearby instanceof AbstractVillager || VillageGolems.supports(nearby)
          || shooter.isAlliedTo(nearby)) && intersects(from, to, nearby.getBoundingBox())) {
        return true;
      }
    }
    return false;
  }

  /** A finite segment also excludes allies behind the shooter or beyond the target. */
  static boolean intersects(Vec3 from, Vec3 to, AABB body) {
    AABB clearance = body.inflate(CLEARANCE);
    return clearance.contains(from) || clearance.clip(from, to).isPresent();
  }
}
