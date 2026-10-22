package com.quzzar.kithkyn.entities.ai.goals;

import java.util.function.Predicate;

import com.quzzar.kithkyn.entities.RealPerson;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;

/** Civilian flight yields when the resident has explicitly chosen a quarrel, as panic does. */
public final class PersonAvoidGoal<T extends LivingEntity> extends AvoidEntityGoal<T> {
  private final RealPerson person;

  public PersonAvoidGoal(RealPerson person, Class<T> type, Predicate<LivingEntity> threat) {
    super(person, type, 12.0F, 0.5D, 0.5D, threat);
    this.person = person;
  }

  @Override
  public boolean canUse() {
    return !person.isQuarrelling() && super.canUse();
  }

  @Override
  public boolean canContinueToUse() {
    return !person.isQuarrelling() && super.canContinueToUse();
  }
}
