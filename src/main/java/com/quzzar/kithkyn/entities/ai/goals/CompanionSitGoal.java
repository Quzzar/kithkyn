package com.quzzar.kithkyn.entities.ai.goals;

import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;

/** A villager-owned pet sits only on command, even though vanilla cannot resolve its owner. */
public final class CompanionSitGoal extends SitWhenOrderedToGoal {
  private final TamableAnimal pet;

  public CompanionSitGoal(TamableAnimal pet) {
    super(pet);
    this.pet = pet;
  }

  @Override
  public boolean canUse() {
    return pet.isOrderedToSit() && super.canUse();
  }
}
