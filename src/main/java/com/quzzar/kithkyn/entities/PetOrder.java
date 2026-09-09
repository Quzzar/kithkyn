package com.quzzar.kithkyn.entities;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.llm.LlmDecision;
import com.quzzar.kithkyn.llm.LlmService;
import com.quzzar.kithkyn.village.CompanionPets;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Wolf;

/**
 * Puts a small choice to a pet owner's own brain: sit their companion down and
 * have it stay, or call it back to their side, or leave it exactly as it is.
 * Mirrors {@link CraftOffer}'s take-or-skip shape, with one deliberate inversion:
 * silence here is a NO-OP, not an action. A mute or absent model must never move
 * a pet, so only an explicit first-option choice acts; an empty answer, or the
 * "leave it" second option, leaves the pet where it stands. The caller
 * ({@code RealPerson.maybeOrderPet}) decides WHEN to ask, once a day when a pet
 * is loaded; this only carries the ask and the hands, per the decide() pattern of
 * docs/llm-brain.md.
 */
public final class PetOrder {

  private PetOrder() {
  }

  /**
   * Offers the owner the choice over their pet and lands the answer back on the
   * main thread. The pet's posture is read now and carried through, so the answer
   * acts on the state the question was actually asked about, however long the
   * model took.
   */
  public static void offer(RealPerson owner, TamableAnimal pet) {
    boolean sitting = pet.isOrderedToSit();
    LlmService llm = LlmService.get();
    MinecraftServer server = owner.getServer();
    if (!llm.isReady() || server == null) {
      return; // no brain to ask, so the pet stays exactly as it is
    }
    String name = pet.getName().getString();
    List<String> options = sitting
        ? List.of("Call " + name + " back to your side", "Let " + name + " keep resting where it is")
        : List.of("Have " + name + " sit and stay here", "Let " + name + " keep following you");
    String purpose = owner.getFirstName() + " sees to their " + petWord(pet);
    llm.decide(purpose, situation(owner, pet), options).whenComplete((decision, error) -> {
      if (error != null) {
        Kithkyn.LOGGER.error("'{}' could not weigh what to do with {}", owner.getFullName(), name, error);
      }
      Optional<LlmDecision> settled = decision == null ? Optional.empty() : decision;
      server.execute(() -> finish(owner, pet, sitting, settled));
    });
  }

  /**
   * Acts on the owner's choice, or does nothing. The default is no-op: an empty
   * answer (model down, timed out, unparsed) or the "leave it" second option
   * leaves the pet untouched. Only the first option moves it, sitting it when it
   * was following, and calling it back when it was sat.
   */
  private static void finish(RealPerson owner, TamableAnimal pet, boolean sitting, Optional<LlmDecision> decision) {
    if (decision.isEmpty() || decision.get().choiceIndex() == 1) {
      return;
    }
    if (pet.isRemoved() || !pet.isAlive() || owner.isRemoved() || !owner.isAlive()
        || pet.level() != owner.level() || pet.isOrderedToSit() != sitting
        || CompanionPets.ownerUuid(pet).filter(owner.getUUID()::equals).isEmpty()) {
      return;
    }
    CompanionPets.commandSit(pet, !sitting);
    Kithkyn.LOGGER.info("'{}' {} {}: {}", owner.getFullName(),
        sitting ? "calls back" : "sits down", pet.getName().getString(), decision.get().reason());
  }

  /** Facts distinguish a deliberate rest from the animal's rendered pose or the owner's job title. */
  static String situation(RealPerson owner, TamableAnimal pet) {
    String activity = owner.isSleeping() ? "You are sleeping."
        : owner.getNavigation().isDone() ? "You are awake and currently not walking."
        : "You are awake and moving around.";
    String recent = owner.recentActivity().map(value -> " Your recent activity was " + value + ".").orElse("");
    double distance = owner.distanceTo(pet);
    long restTicks = CompanionPets.observedRestTicks(pet);
    String posture = pet.isOrderedToSit()
        ? "Your " + pet.getName().getString() + " is sitting where you left it. "
            + (restTicks == 0 ? "Its earlier resting duration is not known."
                : String.format(Locale.ROOT, "It has been waiting for at least %.1f village days.", restTicks / 24000.0))
        : "Your " + pet.getName().getString() + " is allowed to follow you.";
    boolean visibleHostile = !pet.level().getEntitiesOfClass(Mob.class, pet.getBoundingBox().inflate(12),
        mob -> mob instanceof Enemy && mob.isAlive() && pet.hasLineOfSight(mob)).isEmpty();
    String surroundings = visibleHostile ? "A hostile creature is visible within 12 blocks of your pet."
        : "No hostile creature is visible within 12 blocks of your pet.";
    if (pet.isOnFire() || pet.isInLava()) {
      surroundings += " Your pet is in immediate danger from fire or lava.";
    }
    return CraftOffer.identityLead(owner) + activity + recent + " " + posture
        + String.format(Locale.ROOT, " Your pet is %.0f blocks from you. ", distance) + surroundings
        + " Normally keep your companion with you. Sitting is a temporary rest or a response to a concrete danger; "
        + "your occupation or personality alone does not mean following is dangerous. "
        + "If a rest has become long and there is no current reason to stay there, call your pet back. "
        + "Decide whether to change its order and give your reason in a few words.";
  }

  /** The everyday word for the animal, for the call-log purpose line. */
  private static String petWord(TamableAnimal pet) {
    if (pet instanceof Wolf) {
      return "dog";
    }
    if (pet instanceof Cat) {
      return "cat";
    }
    return "pet";
  }
}
