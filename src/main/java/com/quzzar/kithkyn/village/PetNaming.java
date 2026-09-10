package com.quzzar.kithkyn.village;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonObject;
import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.chat.Dialogue;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.llm.LlmService;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.DyeColor;

/**
 * The name, collar, and coat a villager gives a new companion pet, settled by the
 * villager on the shared {@link Dialogue} engine, the same "just ask the villager"
 * pattern as {@link com.quzzar.kithkyn.relationships.MarriageNaming}. Rather
 * than a rule rolling the details, the owner speaks as themselves about the animal
 * they have just been given and names it; the way to end the talk is a valid
 * choice.
 *
 * <p>The choice is constrained to what actually exists in game: the sixteen dye
 * colours for the collar, and the coat variants the registry holds for the
 * species. The model is shown those exact options and its answer is validated
 * against them, so nothing invalid can slip through. A talk that never lands a
 * valid choice resolves to nothing, and the caller keeps the random defaults the
 * pet already wears: the owner's voice is honoured when they use it, never a pet
 * left nameless when they do not.
 *
 * <p>One voice, at most two turns, on the shared {@link SingleVoiceNaming}
 * plumbing: naming a pet is a small, private decision, not a debate.
 */
public final class PetNaming {

  private PetNaming() {
  }

  /** The owner's chosen look for their pet: what to call it, its collar, and its coat. */
  public record PetDecision(String name, DyeColor collar, ResourceLocation variant) {
  }

  /**
   * Runs the owner's naming talk and completes with the name, collar, and coat
   * they settle on, or empty when the LLM is down or their talk lands no valid
   * choice (the caller then keeps the pet's random defaults). Call on the server
   * thread: the talk's fixed facts are read from the owner and the registry as it
   * starts.
   */
  public static CompletableFuture<Optional<PetDecision>> decide(RealPerson owner, CompanionPets.Species species,
      TamableAnimal pet) {
    if (!LlmService.get().isReady()) {
      return CompletableFuture.completedFuture(Optional.empty());
    }
    return Dialogue.run(new Naming(owner, species));
  }

  /**
   * The owner as a single voice: they speak as themselves and the first valid
   * look resolves the talk. The prompt strings and the allowed options are built
   * once, when the talk is convened, so a turn reads no live entity state.
   */
  private static final class Naming extends SingleVoiceNaming<PetDecision> {

    private final CompanionPets.Species species;
    private final String system;

    /** Coat options by lowercase path, so the model's answer can be matched back to the real key. */
    private final Map<String, ResourceLocation> allowedVariants;

    private Naming(RealPerson owner, CompanionPets.Species species) {
      super(owner, owner.getFirstName() + " names their " + species.word());
      this.species = species;
      this.allowedVariants = variantsFor(owner, species);
      this.system = buildSystem(owner, species, allowedVariants.keySet());
    }

    @Override
    protected String system() {
      return system;
    }

    /** The owner's briefing: who they are, the animal before them, and the exact collar and coat options. */
    private static String buildSystem(RealPerson self, CompanionPets.Species species, Iterable<String> variantPaths) {
      return speakerBriefing(self)
          .append("You have just been given a ").append(species.word())
          .append(" of your own, and it is yours to name and to make ready. Choose a name for it, ")
          .append("a collar colour, and its coat, in keeping with who you are. Speak as yourself, in the ")
          .append("first person, one short sentence a turn. When you have decided, state your choices in a ")
          .append("decision. Reply with ONLY a JSON object: {\"say\":\"<what you say>\",")
          .append("\"decision\":{\"name\":\"<the ").append(species.word()).append("'s name>\",")
          .append("\"collar\":\"<a collar colour>\",\"variant\":\"<a coat>\"}}. ")
          .append("The collar must be exactly one of: ").append(quoted(collarNames())).append(". ")
          .append("The coat must be exactly one of: ").append(quoted(variantPaths)).append('.')
          .toString();
    }

    @Override
    protected String user(Dialogue.Transcript transcript, boolean lastChance) {
      StringBuilder user = new StringBuilder();
      user.append("A ").append(species.word()).append(" of your own stands before you; name it and ready it.\n\n");
      user.append("Your words so far:\n");
      if (transcript.isEmpty()) {
        user.append("(nothing said yet)\n");
      } else {
        for (String line : transcript.lines()) {
          user.append(line).append('\n');
        }
      }
      user.append("\nIt is your turn, ").append(ownerFirst).append('.');
      if (lastChance) {
        user.append(" Include your \"decision\" now with the name, collar, and coat you settle on.");
      }
      user.append(" Reply with ONLY the JSON object.");
      return user.toString();
    }

    /** A decision from its object, present only when the name, collar, and coat all validate. */
    @Override
    protected Optional<PetDecision> readDecision(JsonObject decision) {
      Optional<String> name = validateName(field(decision, "name"));
      Optional<DyeColor> collar = validateCollar(field(decision, "collar"));
      Optional<ResourceLocation> variant = validateVariant(field(decision, "variant"));
      if (name.isEmpty() || collar.isEmpty() || variant.isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(new PetDecision(name.get(), collar.get(), variant.get()));
    }

    @Override
    protected void logDecision(PetDecision decision) {
      Kithkyn.LOGGER.info("[pet naming] {} names their {} '{}'", owner.getFullName(), species.word(), decision.name());
    }

    /** The dye colour whose name the model gave, case-insensitively, or empty. */
    private Optional<DyeColor> validateCollar(String candidate) {
      if (candidate == null) {
        return Optional.empty();
      }
      String trimmed = candidate.strip();
      for (DyeColor colour : DyeColor.values()) {
        if (colour.getName().equalsIgnoreCase(trimmed)) {
          return Optional.of(colour);
        }
      }
      return Optional.empty();
    }

    /** The coat key the model gave, matched by path case-insensitively against the allowed set, or empty. */
    private Optional<ResourceLocation> validateVariant(String candidate) {
      if (candidate == null) {
        return Optional.empty();
      }
      String trimmed = candidate.strip().toLowerCase(Locale.ROOT);
      // Accept a bare path ("ashen") or a full key ("minecraft:ashen").
      int colon = trimmed.indexOf(':');
      String path = colon >= 0 ? trimmed.substring(colon + 1) : trimmed;
      return Optional.ofNullable(allowedVariants.get(path));
    }
  }

  /** The species' coat variants keyed by lowercase path, in registry order, for prompt and validation. */
  private static Map<String, ResourceLocation> variantsFor(RealPerson owner, CompanionPets.Species species) {
    Map<String, ResourceLocation> variants = new LinkedHashMap<>();
    Set<ResourceLocation> keys = species == CompanionPets.Species.DOG
        ? owner.level().registryAccess().registryOrThrow(Registries.WOLF_VARIANT).keySet()
        : owner.level().registryAccess().registryOrThrow(Registries.CAT_VARIANT).keySet();
    for (ResourceLocation key : keys) {
      variants.put(key.getPath().toLowerCase(Locale.ROOT), key);
    }
    return variants;
  }

  /** The sixteen dye-colour names, for the collar option list. */
  private static List<String> collarNames() {
    List<String> names = new ArrayList<>();
    for (DyeColor colour : DyeColor.values()) {
      names.add(colour.getName());
    }
    return names;
  }
}
