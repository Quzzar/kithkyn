package com.quzzar.kithkyn.entities.ai.goals;

import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.village.LocationManager;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Bedtime for whoever is not lying down tonight. Two kinds of resident never
 * run SleepAtNightGoal: a guard assigned to stand watch, and an unhoused
 * resident, who stays up by the village fire until the village builds them a
 * bed (docs/population-and-labor.md). Bedtime is still when the day's pack
 * goes back to the village stores and the village hands out gear, rations
 * and upgrades, so both run the same stow-and-restock where they stand
 * instead of at a bed. Before the unhoused turned in here, a homeless
 * camper's litter pickups piled up in the pack for as long as the village
 * left them without a bed, and only a rung bell ever emptied it.
 *
 * <p>No movement flag and no navigation: the routine is instantaneous, and
 * the whole point is that the guard stays on patrol and the camper stays at
 * the fire. bedtimeWithoutBed shares goToBed's 100-tick cooldown, so this
 * refires through the night, and whatever an awake camper tidies up after
 * dark is in the stores moments later. Registered for everyone: a bed is
 * assigned or lost, and a guard's night routine rerolled, under a goal set
 * built long before, so who turns in standing is decided on the night.
 */
public class BedtimeWithoutBedGoal extends Goal {

  private final RealPerson person;

  public BedtimeWithoutBedGoal(RealPerson person) {
    this.person = person;
  }

  @Override
  public boolean canUse() {
    return person.level().isNight() && person.callToBedCoolDown <= 0 && person.getVillage() != null
        && (!person.shouldSleepAtNight() || isUnhoused());
  }

  /** A would-be sleeper with nowhere to sleep: no bed of their own and no family home to rest in. */
  private boolean isUnhoused() {
    return LocationManager.getNightRestLocation(person).equals(BlockPos.ZERO);
  }

  @Override
  public boolean canContinueToUse() {
    // One-shot: the routine runs in start; the cooldown gates the refire.
    return false;
  }

  @Override
  public void start() {
    if (!person.shouldSleepAtNight()) {
      // The watch is this job's rest. Zeroed so the daysSinceSleep last-resort
      // recovery (UnstuckPersonGoal) cannot read a count carried over from
      // before the job - an unhoused sleepless stretch, say - as a villager
      // wedged for days, and teleport a working guard home every night. An
      // unhoused resident's night is genuinely unslept and stays counted.
      person.noteSlept();
    }
    person.bedtimeWithoutBed();
  }
}
