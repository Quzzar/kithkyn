package com.quzzar.kithkyn.village;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.quzzar.kithkyn.chat.Dialogue;
import com.quzzar.kithkyn.chat.VillagerConversation;
import com.quzzar.kithkyn.chat.VillagerText;
import com.quzzar.kithkyn.entities.KithkynAttachments;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.llm.LlmService;
import com.quzzar.kithkyn.persona.PersonaData;

import net.minecraft.server.MinecraftServer;

/**
 * The shared shape of a villager naming something of their own: one voice, at
 * most two turns on the {@link Dialogue} engine, a JSON reply of what they say
 * and what they decide, spoken aloud to any player in earshot. {@link PetNaming}
 * settles a pet's name and look, {@link KeeperNaming} an allay keeper's name.
 * A subclass supplies the briefing, the cue and how its decision object is read;
 * the background LLM turn, the parse and the speaking live here. Prompts read
 * only fixed strings snapshotted when the talk is convened; speaking a line is
 * the one thing that touches the world, and that hops to the server thread.
 */
abstract class SingleVoiceNaming<D> implements Dialogue.Protocol<D> {

  /** One voice, two turns: a first say and, if it did not decide, a second that must. */
  private static final int MAX_TURNS = 2;

  /** Room for a short line plus the small decision object; a naming reply is never long. */
  private static final int NAMING_MAX_TOKENS = 96;

  /** Warm enough for character, low enough to keep the JSON intact on a small model. */
  private static final double NAMING_TEMPERATURE = 0.5D;

  /** A mild push off words just used, the same anti-repeat nudge conversation uses. */
  private static final double NAMING_PENALTY = 0.3D;

  /** A given name is short; anything past this is the model running on, and is trimmed. */
  private static final int MAX_NAME_LENGTH = 24;

  protected final RealPerson owner;
  protected final String ownerFirst;
  private final String purpose;

  SingleVoiceNaming(RealPerson owner, String purpose) {
    this.owner = owner;
    this.ownerFirst = owner.getFirstName();
    this.purpose = purpose;
  }

  /** The owner's briefing, built once when the talk is convened. */
  protected abstract String system();

  /** The turn's cue: the scene, the talk so far, and a last turn that insists on a decision. */
  protected abstract String user(Dialogue.Transcript transcript, boolean lastChance);

  /** A decision from its object, present only when every field validates. */
  protected abstract Optional<D> readDecision(JsonObject decision);

  /** The log line for a settled decision. */
  protected abstract void logDecision(D decision);

  @Override
  public int voices() {
    return 1;
  }

  @Override
  public int maxTurns() {
    return MAX_TURNS;
  }

  @Override
  public CompletableFuture<Dialogue.Turn<D>> takeTurn(int speaker, Dialogue.Transcript transcript,
      boolean lastChance) {
    String user = user(transcript, lastChance);
    CompletableFuture<Dialogue.Turn<D>> turn = new CompletableFuture<>();
    LlmService.get()
        .submitBackgroundChat(purpose, system(), user, List.of(), NAMING_MAX_TOKENS, NAMING_TEMPERATURE, NAMING_PENALTY)
        .whenComplete((reply, error) -> {
          MinecraftServer server = owner.getServer();
          if (server == null) {
            turn.complete(Dialogue.Turn.abort());
            return;
          }
          server.execute(() -> {
            ParsedTurn<D> parsed = (error != null || reply == null || reply.isEmpty())
                ? null
                : parse(reply.get());
            if (parsed == null) {
              turn.complete(Dialogue.Turn.abort());
              return;
            }
            String say = VillagerText.clean(parsed.say());
            if (!say.isBlank()) {
              VillagerConversation.speak(owner, say);
            }
            if (parsed.decision().isPresent()) {
              D decision = parsed.decision().get();
              logDecision(decision);
              turn.complete(Dialogue.Turn.resolved(decision));
              return;
            }
            if (lastChance) {
              turn.complete(Dialogue.Turn.abort());
              return;
            }
            turn.complete(Dialogue.Turn.spoke(say.isBlank() ? "" : ownerFirst + ": " + say));
          });
        });
    return turn;
  }

  /** A parsed reply: what the owner said, and what they settled, if they settled something valid. */
  private record ParsedTurn<D>(String say, Optional<D> decision) {
  }

  /**
   * Reads a reply: the strict-then-lenient discipline used across llm-land, the
   * spoken line from {@code say} and the choice from {@code decision}. Null when
   * nothing parses, which ends the talk.
   */
  private ParsedTurn<D> parse(String raw) {
    int start = raw.indexOf('{');
    int end = raw.lastIndexOf('}');
    if (start < 0 || end <= start) {
      return null;
    }
    try {
      JsonObject node = JsonParser.parseString(raw.substring(start, end + 1)).getAsJsonObject();
      String say = node.has("say") && node.get("say").isJsonPrimitive() ? node.get("say").getAsString() : "";
      Optional<D> decision = Optional.empty();
      if (node.has("decision") && node.get("decision").isJsonObject()) {
        decision = readDecision(node.getAsJsonObject("decision"));
      }
      return new ParsedTurn<>(say, decision);
    } catch (RuntimeException e) {
      return null;
    }
  }

  /** The opening of every briefing: who is speaking and what they are like. */
  static StringBuilder speakerBriefing(RealPerson self) {
    StringBuilder system = new StringBuilder();
    system.append("You are ").append(self.getFullName()).append(", ").append(self.getGender().describe())
        .append(" of ").append(self.getVillageName()).append(". ");
    PersonaData persona = self.getData(KithkynAttachments.PERSONA.get());
    if (persona != null && !persona.isEmpty()) {
      system.append("About you: ").append(persona.blurb()).append(' ');
    }
    return system;
  }

  /** A cleaned, trimmed, capped name, or empty when the model gave nothing usable. */
  static Optional<String> validateName(String candidate) {
    if (candidate == null) {
      return Optional.empty();
    }
    String cleaned = VillagerText.clean(candidate).strip();
    if (cleaned.length() > MAX_NAME_LENGTH) {
      cleaned = cleaned.substring(0, MAX_NAME_LENGTH).strip();
    }
    return cleaned.isEmpty() ? Optional.empty() : Optional.of(cleaned);
  }

  /** The exact-field getter: a string primitive, or null when absent or not a string. */
  static String field(JsonObject object, String key) {
    return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : null;
  }

  /** The options as a quoted, comma-separated list for the prompt. */
  static String quoted(Iterable<String> values) {
    List<String> shown = new ArrayList<>();
    for (String value : values) {
      shown.add('"' + value + '"');
    }
    return String.join(", ", shown);
  }
}
