package com.quzzar.kithkyn.village;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonObject;
import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.chat.Dialogue;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.llm.LlmService;

import net.minecraft.world.entity.animal.allay.Allay;

/**
 * The name a quartermaster gives an allay keeper they have just taken on,
 * settled the way an owner names a pet ({@link PetNaming}): the villager speaks
 * as themselves about the small spirit before them and names it. The keeper
 * already wears a fallback name from the adoption, so a talk that never lands
 * a valid name leaves it as it is; the same ten fallback names are shown to the
 * model as the kind of name a keeper carries.
 */
public final class KeeperNaming {

  private KeeperNaming() {
  }

  /**
   * Runs the quartermaster's naming talk and completes with the name they
   * settle on, or empty when the LLM is down or their talk lands no valid
   * name. Call on the server thread: the talk's fixed facts are read as it starts.
   */
  public static CompletableFuture<Optional<String>> decide(RealPerson quartermaster, Allay keeper,
      List<String> examples) {
    if (!LlmService.get().isReady()) {
      return CompletableFuture.completedFuture(Optional.empty());
    }
    return Dialogue.run(new Naming(quartermaster, keeper, examples));
  }

  /** The quartermaster as a single voice: the first valid name resolves the talk. */
  private static final class Naming extends SingleVoiceNaming<String> {

    private final String system;
    private final String wearing;

    private Naming(RealPerson quartermaster, Allay keeper, List<String> examples) {
      super(quartermaster, quartermaster.getFirstName() + " names the storehouse keeper");
      this.wearing = keeper.getName().getString();
      this.system = speakerBriefing(quartermaster)
          .append("You keep the village stores, and the village has just taken on an allay: a small winged ")
          .append("spirit that will keep the storehouse shelves beside you, fetching what the village leaves ")
          .append("about and shelving it. It is yours to name. Keepers carry short, light names such as ")
          .append(quoted(examples)).append("; choose one of your own in that spirit, in keeping with who you ")
          .append("are and with your village, rather than repeating those. Speak as yourself, in the first ")
          .append("person, one short sentence a turn. When you have decided, state it in a decision. Reply ")
          .append("with ONLY a JSON object: {\"say\":\"<what you say>\",\"decision\":{\"name\":\"<the keeper's name>\"}}.")
          .toString();
    }

    @Override
    protected String system() {
      return system;
    }

    @Override
    protected String user(Dialogue.Transcript transcript, boolean lastChance) {
      StringBuilder user = new StringBuilder();
      user.append("The allay hovers before you at the storehouse counter, answering to \"").append(wearing)
          .append("\" for now; name it.\n\n");
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
        user.append(" Include your \"decision\" now with the name you settle on.");
      }
      user.append(" Reply with ONLY the JSON object.");
      return user.toString();
    }

    @Override
    protected Optional<String> readDecision(JsonObject decision) {
      return validateName(field(decision, "name"));
    }

    @Override
    protected void logDecision(String name) {
      Kithkyn.LOGGER.info("[keeper naming] {} names the storehouse keeper '{}'", owner.getFullName(), name);
    }
  }
}
