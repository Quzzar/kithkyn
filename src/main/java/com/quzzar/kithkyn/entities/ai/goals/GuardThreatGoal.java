package com.quzzar.kithkyn.entities.ai.goals;

import java.util.function.Predicate;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.village.Occupation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.phys.AABB;

/** The normal hostile search, with a three-dimensional watch for ranged guards. */
public final class GuardThreatGoal extends NearestAttackableTargetGoal<Mob> {
  public GuardThreatGoal(RealPerson person, Predicate<LivingEntity> threat) {
    super(person, Mob.class, 5, true, !hasRangedWatch(person), threat);
  }

  /** A nominal 48-block radius, preserving the existing eyesight and wisdom variation. */
  public static double range(RealPerson person) {
    return person.getAttributeValue(Attributes.FOLLOW_RANGE) * 2.4D;
  }

  private static boolean hasRangedWatch(RealPerson person) {
    return person.getOccupation() == Occupation.GUARD && (person.isFixedRangedGuard()
        || person.getMainHandItem().getItem() instanceof BowItem
        || person.getMainHandItem().getItem() instanceof CrossbowItem);
  }

  @Override
  protected double getFollowDistance() {
    return mob instanceof RealPerson person && hasRangedWatch(person)
        ? range(person) : super.getFollowDistance();
  }

  @Override
  protected AABB getTargetSearchArea(double distance) {
    return mob instanceof RealPerson person && hasRangedWatch(person)
        ? mob.getBoundingBox().inflate(distance) : super.getTargetSearchArea(distance);
  }
}
