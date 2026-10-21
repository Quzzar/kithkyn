package com.quzzar.kithkyn.chat;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.llm.provider.LlmProvider.CompletionRequest;
import com.quzzar.kithkyn.chat.PersonChatContext.AssembledChat;
import com.quzzar.kithkyn.chat.PersonChatContext.Turn;
import com.quzzar.kithkyn.configuration.KithkynConfig;
import com.quzzar.kithkyn.entities.ClericPotions;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.VillageRequests;
import com.quzzar.kithkyn.entities.KithkynAttachments;
import com.quzzar.kithkyn.llm.LlmService;
import com.quzzar.kithkyn.networking.PersonChatReplyPacket;
import com.quzzar.kithkyn.relationships.OpinionService;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * The conversation pipeline (conversation map #28): assembles the villager's
 * briefing, asks the LLM through the chat-priority slot, parses the
 * structured reply, applies validated conversation effects, and records the real transcript.
 * Game-owned undertaking outcomes are read as context; dialogue cannot write them.
 *
 * <p>The same {@link #converse} core also serves talk between two villagers
 * ({@link VillagerConversation}): the speaker is a name and a UUID either way,
 * so history, summaries, undertakings, and requests all work per-interlocutor
 * unchanged. What differs is the LLM lane (villager turns ride the background
 * queue so a player's reply is never delayed) and who executes a give.
 */
public final class PersonChatDispatcher {

  private static final int MAX_HISTORY_TURNS = 6;
  // Chat's token cap and base temperature are config-driven (the "LLM chat max
  // new tokens" / "LLM chat temperature" keys). The retry temperature and both
  // repetition penalties below stay fixed: they are anti-repeat mechanics for a
  // small model, not tuning knobs a player is meant to turn.

  /**
   * How hard to push the model off words it has just used.
   *
   * A villager's own last line is in the transcript it is asked to continue,
   * and on a 3B at temperature 0.4 that line is the highest-probability thing
   * to say next - so it says it again, verbatim, to any input. Aaron got the
   * same sentence three times in a row to "What?", "I'm good" and "What?".
   *
   * Deliberately mild. The reply is JSON, and a large penalty would be
   * penalising the braces and quotes the parser needs as much as the words.
   */
  private static final double CHAT_REPETITION_PENALTY = 0.3D;

  /** Hotter and pushed harder, for the one retry after a verbatim repeat. */
  private static final double RETRY_TEMPERATURE = 0.85D;
  private static final double RETRY_REPETITION_PENALTY = 0.7D;

  /**
   * Words compared at the head of a reply to decide it is the same answer again.
   *
   * First tried as "the first sentence", which misses the commonest shape: a
   * model whose every reply opens "Ah, Quzzar! ..." has a TWO word first
   * sentence, so a sentence-based rule waves all twelve of them through. A
   * fixed number of leading words does not care where the punctuation falls.
   */
  private static final int OPENER_WORDS = 5;

  private static final long HISTORY_EXPIRY_MS = 10 * 60 * 1000;

  private static final Set<UUID> IN_FLIGHT = ConcurrentHashMap.newKeySet();

  // ---- Conversation lifecycle (consumed by entity goals, e.g. pause+face) ----

  /**
   * Someone this villager is in conversation with: a player with the chat
   * screen open, or a fellow villager mid-exchange. Refreshed by messages.
   */
  private record ChatSession(UUID partner, long lastActivityMs) {
  }

  private static final Map<Integer, ChatSession> SESSIONS = new ConcurrentHashMap<>();

  /**
   * Backstop for lost close packets (disconnects, crashes), and the per-turn
   * budget of a villager-to-villager exchange: a turn the background LLM lane
   * cannot serve inside this window lets the pair drift back to their lives
   * instead of standing frozen.
   */
  static final long SESSION_TIMEOUT_MS = 30_000;

  /** Called when the chat screen opens (right-click path). */
  public static void markOpen(RealPerson person, ServerPlayer player) {
    markTalking(person, player.getUUID());
    com.quzzar.kithkyn.awareness.ResidentAwareness.approached(person, player);
  }

  /**
   * Marks (or refreshes) this villager as in conversation with the given
   * partner, player or villager alike. The villager-to-villager driver calls
   * this for both ends of an exchange.
   */
  public static void markTalking(RealPerson person, UUID partner) {
    SESSIONS.put(person.getId(), new ChatSession(partner, System.currentTimeMillis()));
  }

  /** Called when the conversation ends (screen closed, or the exchange ran out). */
  public static void markClosed(int entityId, UUID partner) {
    SESSIONS.computeIfPresent(entityId, (id, session) -> session.partner().equals(partner) ? null : session);
  }

  /**
   * True while someone is in conversation with this villager (activity within
   * the timeout). The authoritative "in conversation" signal: goals gate on
   * this rather than inventing their own notion.
   */
  public static boolean isConversing(RealPerson person) {
    ChatSession session = SESSIONS.get(person.getId());
    if (session == null) {
      return false;
    }
    if (System.currentTimeMillis() - session.lastActivityMs() > SESSION_TIMEOUT_MS) {
      SESSIONS.remove(person.getId(), session);
      return false;
    }
    return true;
  }

  /** Whoever is currently conversing with this villager (player or villager UUID), if anyone. */
  public static Optional<UUID> conversingWith(RealPerson person) {
    return isConversing(person) ? Optional.of(SESSIONS.get(person.getId()).partner()) : Optional.empty();
  }

  private PersonChatDispatcher() {
  }

  public static void handle(ServerPlayer player, RealPerson person, String message) {
    if (!IN_FLIGHT.add(player.getUUID())) {
      return; // one chat request in flight per player
    }
    com.quzzar.kithkyn.awareness.ResidentAwareness.addressed(person, player, message);
    dispatch(person, player, message, message);
  }

  /**
   * The villager speaks first: sent when a chat opens with nothing to show, so
   * a conversation never starts on an empty screen. Stored in history with an
   * empty player line, which the screen renders as a villager-only line.
   */
  private static final String GREETING_CUE = "*walks up to you*";

  public static void greet(RealPerson person, ServerPlayer player) {
    if (!IN_FLIGHT.add(player.getUUID())) {
      return;
    }
    dispatch(person, player, GREETING_CUE, "");
  }

  /**
   * Shared tail: converse, execute any give, answer the player's screen.
   *
   * The in-flight guard is cleared on EVERY outcome, not just success. It used
   * to be released inside thenAccept, so one failed request left the player's
   * entry behind for good, and from then on both greetings and sends were
   * dropped in silence: the villager never spoke and the send button looked
   * broken. A guard that can leak is worse than no guard at all.
   */
  private static void dispatch(RealPerson person, ServerPlayer player, String message, String historyLine) {
    CompletableFuture<Reply> pending;
    try {
      pending = converse(person, player.getGameProfile().getName(), player.getUUID(), message, historyLine, false);
    } catch (RuntimeException e) {
      IN_FLIGHT.remove(player.getUUID());
      throw e;
    }
    pending.whenComplete((reply, error) -> player.getServer().execute(() -> {
      IN_FLIGHT.remove(player.getUUID());
      if (error != null || reply == null) {
        Kithkyn.LOGGER.warn("Chat with {} failed for {}", person.getFullName(),
            player.getGameProfile().getName(), error);
        PacketDistributor.sendToPlayer(player,
            new PersonChatReplyPacket(person.getId(), "", false));
        return;
      }
      if (reply.give() != null && person.isAlive()) {
        executeGive(person, player, reply.give(), reply.giveCount());
      }
      // Words answered with blows: the villager's own decision. The villager
      // path does the same in VillagerConversation.
      if (reply.fight() && person.isAlive()) {
        person.pickFightWith(player);
      }
      // The villager's own leave-taking: the screen shows the farewell and
      // closes itself a little later, which closes the session the usual way.
      if (reply.done()) {
        Kithkyn.LOGGER.info("[chat] {} takes their leave of {}", person.getFullName(),
            player.getGameProfile().getName());
      }
      PacketDistributor.sendToPlayer(player, new PersonChatReplyPacket(person.getId(), reply.say(), reply.done()));
    }));
  }

  /**
   * The pipeline core, shared by the right-click packet path and the
   * {@code /kithkyn chat} test command: assemble briefing, ask the LLM, parse,
   * record history. A validated give is returned but NOT executed here — the
   * caller decides (the packet path tosses the item; the command path, which
   * may have no player to toss to, just reports it).
   */
  public static CompletableFuture<Reply> converse(RealPerson person, String speakerName, UUID speakerUUID,
      String message) {
    return converse(person, speakerName, speakerUUID, message, message);
  }

  /**
   * As above, but {@code historyLine} is what gets recorded as the speaker's
   * side of the exchange: the greeting path passes an empty string, since the
   * speaker has not said anything yet.
   */
  public static CompletableFuture<Reply> converse(RealPerson person, String speakerName, UUID speakerUUID,
      String message, String historyLine) {
    return converse(person, speakerName, speakerUUID, message, historyLine, false);
  }

  /**
   * As above, with the LLM lane made explicit: {@code background} turns ride
   * the low-priority queue (villager-to-villager talk, where nobody is at a
   * screen waiting), foreground turns take the chat-priority slot that holds
   * village work off while a player is mid-conversation.
   */
  public static CompletableFuture<Reply> converse(RealPerson person, String speakerName, UUID speakerUUID,
      String message, String historyLine, boolean background) {
    long askedAtMs = System.currentTimeMillis();
    // Refresh the open session (never create one: console/RCON chats via
    // /kithkyn chat should not make the villager stand at attention).
    SESSIONS.computeIfPresent(person.getId(), (id, session) -> session.partner().equals(speakerUUID)
        ? new ChatSession(speakerUUID, System.currentTimeMillis())
        : session);

    // History comes from the persisted attachment (survives restarts, #45);
    // the briefing sees the freshest tail of it.
    List<ChatHistoryData.Exchange> exchanges = person.getData(KithkynAttachments.CHAT_HISTORY.get())
        .with(speakerUUID);
    List<Turn> history = exchanges.stream()
        .skip(Math.max(0, exchanges.size() - MAX_HISTORY_TURNS))
        .map(e -> new Turn(e.playerLine(), e.reply(), e.dayTime()))
        .toList();

    AssembledChat chat = PersonChatContext.assemble(person, speakerName, speakerUUID, history, message);

    // What the villager said last time, so an identical answer can be caught
    // rather than shipped. The penalty above makes the lock less likely; this
    // catches it when it happens anyway, which is the half that does not
    // depend on a model honouring a sampling parameter.
    String lastAnswer = history.isEmpty() ? null : history.get(history.size() - 1).villagerLine();

    String purpose = speakerName + " -> " + person.getFullName();
    return ask(purpose, initialRequest(chat), background)
        .thenCompose(first -> {
          if (!echoesLastAnswer(first, lastAnswer)) {
            return CompletableFuture.completedFuture(first);
          }
          Kithkyn.LOGGER.debug("{} opened exactly as they did last time; sampling once more",
              person.getFullName());
          return ask(purpose + " (resampled)", new CompletionRequest(chat.system(), chat.user(), chat.examples(),
              KithkynConfig.LlmChatMaxNewTokens, RETRY_TEMPERATURE, RETRY_REPETITION_PENALTY), background)
              .thenApply(second -> echoesLastAnswer(second, lastAnswer) ? first : second);
        })
        .thenApply(result -> {
          String say = null;
          String give = null;
          int giveCount = 1;
          int opinion = 0;
          JsonObject request = null;
          boolean fight = false;
          boolean done = false;
          if (result.isPresent()) {
            Reply parsed = parseReply(result.get());
            if (parsed != null) {
              say = parsed.say();
              give = parsed.give();
              giveCount = parsed.giveCount();
              opinion = parsed.opinionDelta();
              request = parsed.request();
              fight = parsed.fight();
              done = parsed.done();
            }
          }
          if (say == null || say.isBlank()) {
            Kithkyn.LOGGER.warn("[chat] No valid model reply for {}", person.getUUID());
            return null;
          }
          say = VillagerText.clean(say);
          finalizeExchange(person, speakerName, speakerUUID, historyLine, say, give, giveCount, opinion,
              request, fight, System.currentTimeMillis() - askedAtMs);
          return new Reply(say, give, giveCount, opinion, request, fight, done);
        });
  }

  /** Exposes the exact initial chat request for paired replay without copying its sampling policy. */
  public static CompletionRequest initialRequest(AssembledChat chat) {
    return new CompletionRequest(chat.system(), chat.user(), chat.examples(), KithkynConfig.LlmChatMaxNewTokens,
        KithkynConfig.LlmChatTemperature, CHAT_REPETITION_PENALTY);
  }

  private static CompletableFuture<Optional<String>> ask(String purpose, CompletionRequest request, boolean background) {
    return background
        ? LlmService.get().submitBackgroundChat(purpose, request.system(), request.user(), request.examples(),
            request.maxNewTokens(), request.temperature(), request.frequencyPenalty())
        : LlmService.get().submitChat(purpose, request.system(), request.user(), request.examples(),
            request.maxNewTokens(), request.temperature(), request.frequencyPenalty());
  }

  /**
   * Whether a raw reply is the villager saying their last answer over again.
   *
   * Compared on the parsed "say" rather than the raw JSON, so a differing
   * "give" or "opinion" does not disguise the same sentence, and loosely
   * enough that trailing punctuation or capitalisation is not a difference.
   *
   * <b>The opening sentence counts on its own.</b> The first version of this
   * compared whole lines, which is what the failure looked like in the sample
   * I had. In real play it takes another shape: the model locks onto an
   * OPENING and varies the tail, so "Just a bit on my mind, that's all. How are
   * you?" is followed by "Just a bit on my mind, that's all. What's in your
   * inventory?" - four such replies in a row, every one of them a distinct
   * line, and a whole-line check waves all of them through.
   */
  private static boolean echoesLastAnswer(Optional<String> raw, String lastAnswer) {
    if (raw == null || raw.isEmpty() || lastAnswer == null || lastAnswer.isBlank()) {
      return false;
    }
    Reply parsed = parseReply(raw.get());
    if (parsed == null || parsed.say() == null || parsed.say().isBlank()) {
      return false; // An invalid response is handled explicitly by the caller.
    }
    return sameAnswer(parsed.say(), lastAnswer);
  }

  /** Same whole line, or the same handful of opening words twice running. */
  static boolean sameAnswer(String a, String b) {
    if (flatten(a).equals(flatten(b))) {
      return true;
    }
    String openA = opener(a);
    String openB = opener(b);
    // A reply too short to have an opener is not evidence of anything.
    return !openA.isEmpty() && openA.equals(openB);
  }

  /** The first few words, flattened; empty when the reply is shorter than that. */
  private static String opener(String line) {
    String[] words = flatten(line).split(" ");
    if (words.length < OPENER_WORDS) {
      return "";
    }
    return String.join(" ", java.util.Arrays.copyOfRange(words, 0, OPENER_WORDS));
  }

  private static String flatten(String line) {
    return line.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9 ]", "").trim()
        .replaceAll(" +", " ");
  }

  /**
   * Server-thread tail of every exchange: persist it to the history
   * attachment, apply the (capped) opinion adjustment, and log — async replies
   * are invisible to RCON sources, so headless capture reads the log.
   */
  private static void finalizeExchange(RealPerson person, String speakerName, UUID speakerUUID,
      String playerLine, String reply, String give, int giveCount, int requestedOpinion,
      JsonObject request, boolean fight, long elapsedMs) {
    var server = person.getServer();
    if (server == null) {
      return;
    }
    server.execute(() -> {
      int applied = applyOpinion(person, speakerUUID, requestedOpinion);
      person.setData(KithkynAttachments.CHAT_HISTORY.get(),
          person.getData(KithkynAttachments.CHAT_HISTORY.get())
              .withExchange(speakerUUID,
                  new ChatHistoryData.Exchange(playerLine, reply, person.level().getDayTime())));
      String asked = applyRequest(person, request);
      Kithkyn.LOGGER.info("[chat] ({}ms) {} -> {}: \"{}\"{}{}{}{}", elapsedMs, speakerName, person.getFullName(), reply,
          give != null ? " [give: " + giveCount + "x " + give + "]" : "",
          applied != 0 ? " [opinion: " + (applied > 0 ? "+" : "") + applied + "]" : "",
          asked != null ? " [" + asked + "]" : "",
          fight ? " [fight]" : "");
    });
  }

  /**
   * The write path for the request tool: the villager's ask is queued on the
   * village for the brain to weigh at its next decision (VillageRequests,
   * UrbanPlanner). A request decides nothing here; it only records what this
   * villager put forward and why. Returns a short line for the log, or null when
   * there was nothing to record (no village, or an empty subject).
   */
  private static String applyRequest(RealPerson person, JsonObject request) {
    if (request == null) {
      return null;
    }
    Village village = person.getVillage();
    if (village == null) {
      return null;
    }
    String subject = request.has("subject") && request.get("subject").isJsonPrimitive()
        ? request.get("subject").getAsString().strip()
        : "";
    if (subject.isEmpty()) {
      return null;
    }
    String reason = request.has("reason") && request.get("reason").isJsonPrimitive()
        ? request.get("reason").getAsString().strip()
        : "";
    VillageRequests.add(village, person.getUUID(), person.getFullName(), subject, reason,
        village.getVillageTime());
    return "request: " + subject;
  }

  /** How the consolidation summary is generated: room for a few sentences, low heat. */
  private static final int SUMMARY_MAX_TOKENS = 150;
  private static final double SUMMARY_TEMPERATURE = 0.3D;

  /**
   * Fold a villager's conversation with someone (a player, or a fellow
   * villager) into their per-counterpart summary ({@link ChatSummaryData}),
   * run in the background when the chat CLOSES so the memory is ready before
   * the next conversation opens. {@code RealPerson.openChat} then greets from
   * it on a new Minecraft day with no wait, rather than summarizing on demand
   * and stalling the open on a model generation. A villager conversation
   * closes through the same path (VillagerConversation), one summary each.
   *
   * <p>The transcript is kept, not cleared: re-opening the same day continues it,
   * and the new day's open is what clears it. Persona priority, so it never delays
   * chat or decisions. Skipped when the player has not actually spoken this
   * session (a greeting alone is not worth a summary), and a no-op if the LLM is
   * off or the call fails, in which case the villager keeps the summary it had.
   */
  public static void summarizeSession(RealPerson person, UUID playerId, String playerName) {
    MinecraftServer server = person.getServer();
    if (server == null) {
      return;
    }
    List<ChatHistoryData.Exchange> session = person.getData(KithkynAttachments.CHAT_HISTORY.get()).with(playerId);
    boolean playerSpoke = session.stream().anyMatch(e -> !e.playerLine().isBlank());
    if (!playerSpoke) {
      return;
    }
    String earlier = person.getData(KithkynAttachments.CHAT_SUMMARY.get()).with(playerId);

    ConversationMemoryPrompt.Prompt memoryPrompt = ConversationMemoryPrompt.build(
        person.getFullName(), playerName, earlier,
        session.stream().map(ChatHistoryData.Exchange::playerLine).filter(line -> !line.isBlank()).toList(),
        com.quzzar.kithkyn.entities.UndertakingService.briefing(person.getData(KithkynAttachments.UNDERTAKINGS.get()), person.getUUID(), Optional.of(playerId), 8));

    LlmService.get().submitPersona(person.getFullName() + " keeps a memory of " + playerName,
        memoryPrompt.system(), memoryPrompt.user(), SUMMARY_MAX_TOKENS, SUMMARY_TEMPERATURE)
        .whenComplete((result, error) -> {
          if (error != null || result == null || result.isEmpty()) {
            return;
          }
          String summary = VillagerText.clean(result.get().trim());
          if (summary.isEmpty()) {
            return;
          }
          // An echo of the old memory is not a new memory. Keep the stored one
          // rather than overwrite it with a clone, so a copied summary cannot
          // accrete embellishments ("over the years") talk after talk.
          if (isEcho(summary, earlier)) {
            Kithkyn.LOGGER.info("[chat summary] {} keeps their memory of {} (new summary echoed the old)",
                person.getFullName(), playerName);
            return;
          }
          server.execute(() -> {
            person.setData(KithkynAttachments.CHAT_SUMMARY.get(),
                person.getData(KithkynAttachments.CHAT_SUMMARY.get()).withSummary(playerId, summary));
            // The one INFO the summary path emits: the write is otherwise silent,
            // so headless verification and "why didn't they remember me" debugging
            // both read this line, as the rest of the chat pipeline is read.
            Kithkyn.LOGGER.info("[chat summary] {} now remembers {}: \"{}\"",
                person.getFullName(), playerName, summary);
          });
        });
  }


  /** Share of the earlier memory's words the new one must reuse to count as an echo. */
  private static final double ECHO_THRESHOLD = 0.85D;

  /**
   * Whether a fresh summary is just the earlier one back again. Compared on the
   * bag of words rather than exact text, so a copy with one flourish swapped
   * ("a mix of kindness and frustration" for "a test of my patience") still
   * reads as the echo it is.
   */
  private static boolean isEcho(String fresh, String earlier) {
    if (earlier == null || earlier.isBlank()) {
      return false;
    }
    Set<String> before = words(earlier);
    Set<String> now = words(fresh);
    if (before.isEmpty() || now.isEmpty()) {
      return false;
    }
    long shared = now.stream().filter(before::contains).count();
    return shared >= ECHO_THRESHOLD * Math.max(before.size(), now.size());
  }

  private static Set<String> words(String text) {
    Set<String> out = new HashSet<>();
    for (String word : text.toLowerCase(Locale.ROOT).split("[^a-z0-9']+")) {
      if (word.length() > 2) {
        out.add(word);
      }
    }
    return out;
  }

  /** A validated conversational reply. Physical action outcomes are recorded separately by the game. */
  public record Reply(String say, String give, int giveCount, int opinionDelta,
      JsonObject request, boolean fight, boolean done) {
  }

  /** Largest single-call opinion step the model may take. */
  private static final int OPINION_STEP_LIMIT = 10;
  /** Largest total swing one conversation window may produce (#44's cap). */
  private static final int OPINION_CONVERSATION_CAP = 15;
  private static final long OPINION_WINDOW_MS = 10 * 60 * 1000;

  private record OpinionBudget(long windowStartMs, int spent) {
  }

  private static final Map<Long, OpinionBudget> OPINION_BUDGETS = new ConcurrentHashMap<>();

  /** Strict contract shared by player and resident dialogue; malformed output has no side effects or substitute narrative. */
  public static Reply parseReply(String raw) {
    var parsed = com.quzzar.kithkyn.llm.StrictJson.object(raw);
    if (parsed.isEmpty()) return null;
    JsonObject node = parsed.get();
    if (!Set.of("say", "give", "give_count", "opinion", "request", "fight", "done").containsAll(node.keySet())) return null;
    try {
      if (!node.has("say") || !node.getAsJsonPrimitive("say").isString()) return null;
      String say = node.get("say").getAsString();
      if (say.isBlank() || say.length() > 2048) return null;
      String give = null; int count = 1; int opinion = 0;
      if (node.has("give")) {
        if (!node.getAsJsonPrimitive("give").isString()) return null;
        give = node.get("give").getAsString();
        if (!give.matches("[a-z0-9_.:/-]+")) return null;
      }
      if (node.has("give_count")) {
        if (!node.getAsJsonPrimitive("give_count").isNumber()) return null;
        count = node.get("give_count").getAsBigDecimal().intValueExact();
        if (give == null || count < 1 || count > 64) return null;
      }
      if (node.has("opinion")) {
        if (!node.getAsJsonPrimitive("opinion").isNumber()) return null;
        opinion = clampStep(node.get("opinion").getAsBigDecimal().intValueExact());
      }
      JsonObject request = null;
      if (node.has("request")) {
        request = node.getAsJsonObject("request");
        if (!Set.of("subject", "reason").containsAll(request.keySet()) || !request.has("subject")) return null;
        for (var entry : request.entrySet()) if (!entry.getValue().isJsonPrimitive()
            || !entry.getValue().getAsJsonPrimitive().isString() || entry.getValue().getAsString().length() > 320) return null;
      }
      boolean fight = bool(node, "fight"), done = bool(node, "done");
      return new Reply(say, give, count, opinion, request, fight, done);
    } catch (RuntimeException invalid) { return null; }
  }
  private static boolean bool(JsonObject node, String key) {
    if (!node.has(key)) return false;
    var value = node.getAsJsonPrimitive(key);
    if (!value.isBoolean()) throw new IllegalArgumentException("Expected boolean");
    return value.getAsBoolean();
  }

  private static int clampStep(int delta) {
    return Math.max(-OPINION_STEP_LIMIT, Math.min(OPINION_STEP_LIMIT, delta));
  }

  /**
   * Applies the model's opinion adjustment, spending from a bounded
   * per-conversation budget so one chat cannot swing a relationship end to
   * end. Storage is {@link OpinionService}'s call, the one tool for "how I
   * feel about someone" (docs/relationships.md): a fellow resident's judgement
   * lands on the relationship pair's lean, a player's on the villager's own
   * social attachment. Returns the delta actually applied.
   */
  private static int applyOpinion(RealPerson person, UUID speakerUUID, int requestedDelta) {
    if (requestedDelta == 0) {
      return 0;
    }
    long pairKey = ((long) person.getId() << 32) ^ speakerUUID.hashCode();
    long now = System.currentTimeMillis();
    OpinionBudget budget = OPINION_BUDGETS.compute(pairKey, (key, existing) ->
        existing == null || now - existing.windowStartMs() > OPINION_WINDOW_MS
            ? new OpinionBudget(now, 0)
            : existing);
    int remaining = OPINION_CONVERSATION_CAP - Math.abs(budget.spent());
    int clamped = Math.max(-remaining, Math.min(remaining, requestedDelta));
    if (clamped == 0) {
      return 0;
    }
    int applied = OpinionService.apply(person, speakerUUID, clamped, "moved in conversation");
    if (applied == 0) {
      return 0;
    }
    OPINION_BUDGETS.put(pairKey, new OpinionBudget(budget.windowStartMs(), budget.spent() + applied));
    return applied;
  }

  /**
   * A give is honoured for any item actually in the villager's pockets, as
   * often as they like: a villager's things are theirs to part with (Aaron,
   * 2026-09-12: no cooldown, and no guard on the job's own gear). The model
   * once offered items on ordinary turns, a torch when asked how business is,
   * a diamond for asking a question; the prompt now tells them to give only
   * when asked, and that is the only brake on how often.
   */
  private static void executeGive(RealPerson person, ServerPlayer player, String itemId, int requestedCount) {
    ResourceLocation id = ResourceLocation.tryParse(itemId.contains(":") ? itemId : "minecraft:" + itemId);
    if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
      Kithkyn.LOGGER.info("[chat give] REJECTED '{}': not a valid item id", itemId);
      return;
    }
    var item = BuiltInRegistries.ITEM.get(id);
    int want = Math.max(1, requestedCount);
    int before = countHeld(person, item);

    // Snapshot every slot at INFO so a give is diagnosable end to end: what the
    // villager held before, how much of it left them, and where the drop landed.
    Kithkyn.LOGGER.info("[chat give] {} wants to give {}x '{}' to {} | holds {} of it | all slots: {}",
        person.getFullName(), want, itemId, player.getGameProfile().getName(), before, slotSnapshot(person));

    if (before == 0) {
      Kithkyn.LOGGER.info("[chat give] REJECTED '{}': {} has none in any slot "
          + "(hand, off-hand, armour, or carry-inventory)", itemId, person.getFullName());
      return;
    }

    int collected = takeFromSlots(person, item, want);

    // A hand-off is an inventory move, not loose loot. The old path spawned the
    // whole stack at the player's feet with no pickup delay, where the giver's
    // ordinary litter-pickup goal could immediately take it back. Insert first;
    // only a full player's overflow becomes an item at the player's feet.
    DirectGiftDelivery.Result delivery = DirectGiftDelivery.insert(
        new ItemStack(item, collected), player.getInventory()::add);
    if (delivery.inserted() > 0) {
      player.getInventory().setChanged();
    }
    ItemEntity overflowDrop = null;
    boolean overflowSpawned = false;
    if (!delivery.overflow().isEmpty()) {
      overflowDrop = new ItemEntity(person.level(), player.getX(), player.getY() + 0.2,
          player.getZ(), delivery.overflow());
      overflowDrop.setDeltaMovement(Vec3.ZERO);
      overflowDrop.setDefaultPickUpDelay();
      overflowDrop.setTarget(player.getUUID());
      overflowDrop.setThrower(player);
      overflowSpawned = person.level().addFreshEntity(overflowDrop);
    }

    Kithkyn.LOGGER.info(
        "[chat give] {} handed {}x {} to {} (asked {}); villager now holds {}; inserted={}, overflow={}{}",
        person.getFullName(), collected, itemId, player.getGameProfile().getName(), want,
        countHeld(person, item), delivery.inserted(), delivery.overflow().getCount(),
        overflowDrop == null ? "" : " in drop #" + overflowDrop.getId() + " spawned=" + overflowSpawned);
  }

  /**
   * A give from one VILLAGER to another: the same validation and slot-taking
   * as the player path, but the gift lands straight in the listener's pockets
   * (with any overflow dropped at their feet) rather than being tossed for a
   * player to pick up. The move is a move, never a copy: what enters the
   * receiver is exactly what {@link #takeFromSlots} removed from the giver.
   * The receiver also logs the gift as a pickup from the giver, so their next
   * reflection can decide what the gesture meant to them.
   */
  static void executeGive(RealPerson from, RealPerson to, String itemId, int requestedCount) {
    ResourceLocation id = ResourceLocation.tryParse(itemId.contains(":") ? itemId : "minecraft:" + itemId);
    if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
      Kithkyn.LOGGER.info("[chat give] REJECTED '{}': not a valid item id", itemId);
      return;
    }
    var item = BuiltInRegistries.ITEM.get(id);
    int want = Math.max(1, requestedCount);
    int before = countHeld(from, item);
    Kithkyn.LOGGER.info("[chat give] {} wants to give {}x '{}' to {} | holds {} of it | all slots: {}",
        from.getFullName(), want, itemId, to.getFullName(), before, slotSnapshot(from));
    if (before == 0) {
      Kithkyn.LOGGER.info("[chat give] REJECTED '{}': {} has none in any slot "
          + "(hand, off-hand, armour, or carry-inventory)", itemId, from.getFullName());
      return;
    }

    int collected = takeFromSlots(from, item, want);
    ItemStack leftover = to.personMainInv.addItem(new ItemStack(item, collected));
    if (!leftover.isEmpty()) {
      // Full pockets: what did not fit lands at the receiver's feet.
      ItemEntity drop = new ItemEntity(to.level(), to.getX(), to.getY() + 0.2, to.getZ(), leftover);
      drop.setDeltaMovement(Vec3.ZERO);
      to.level().addFreshEntity(drop);
    }

    to.setData(KithkynAttachments.PERSONAL_LOG.get(),
        to.getData(KithkynAttachments.PERSONAL_LOG.get())
            .withEntry(com.quzzar.kithkyn.entities.PersonalLogData.pickup(
                BuiltInRegistries.ITEM.getKey(item).toString(), collected,
                to.level().getDayTime(), to.level().getGameTime(), Optional.of(from.getUUID()))));

    Kithkyn.LOGGER.info("[chat give] {} handed {}x {} to {} (asked {}); giver now holds {}",
        from.getFullName(), collected, itemId, to.getFullName(), want, countHeld(from, item));
  }

  /**
   * Takes up to {@code want} of the item from the villager's slots and returns
   * how many actually came out. Equipment first (hand, off-hand, armour), then
   * carry-inventory, across as many stacks as it takes. Anything goes, the job's
   * own tool or token included: a villager parting with what they work with is
   * their business, and by day the tool-tending pass draws it back
   * ({@link RealPerson#tendJobTool}, {@link RealPerson#tendSignatureGear}).
   *
   * <p>The one exception is the cleric's last potion of a brew. Potions are
   * stock the cleric can only grow from a bottle they already hold
   * ({@link ClericPotions}), so giving the last one away loses the brew for
   * good; everything above that seed is theirs to give like anything else.
   */
  private static int takeFromSlots(RealPerson person, net.minecraft.world.item.Item item, int want) {
    int collected = 0;
    for (EquipmentSlot eq : EquipmentSlot.values()) {
      if (collected >= want) {
        break;
      }
      ItemStack worn = person.getItemBySlot(eq);
      if (!worn.isEmpty() && worn.getItem() == item) {
        int take = Math.min(want - collected, giveable(person, worn));
        worn.shrink(take);
        person.setItemSlot(eq, worn.isEmpty() ? ItemStack.EMPTY : worn);
        collected += take;
      }
    }
    for (int i = 0; i < person.personMainInv.getContainerSize() && collected < want; i++) {
      ItemStack stack = person.personMainInv.getItem(i);
      if (!stack.isEmpty() && stack.getItem() == item) {
        int take = Math.min(want - collected, giveable(person, stack));
        person.personMainInv.removeItem(i, take);
        collected += take;
      }
    }
    return collected;
  }

  /** How much of a stack may leave: all of it, except a cleric's seed potion. */
  private static int giveable(RealPerson person, ItemStack stack) {
    return ClericPotions.isCleric(person) ? ClericPotions.giveable(person, stack) : stack.getCount();
  }

  /** How many of {@code item} the villager holds across every slot. */
  private static int countHeld(RealPerson person, net.minecraft.world.item.Item item) {
    int held = 0;
    for (EquipmentSlot eq : EquipmentSlot.values()) {
      ItemStack worn = person.getItemBySlot(eq);
      if (worn.getItem() == item) {
        held += worn.getCount();
      }
    }
    for (int i = 0; i < person.personMainInv.getContainerSize(); i++) {
      ItemStack stack = person.personMainInv.getItem(i);
      if (stack.getItem() == item) {
        held += stack.getCount();
      }
    }
    return held;
  }

  /** A compact "slot=Nx item, ..." snapshot of everything the villager carries. */
  private static String slotSnapshot(RealPerson person) {
    StringBuilder snapshot = new StringBuilder();
    for (EquipmentSlot eq : EquipmentSlot.values()) {
      ItemStack worn = person.getItemBySlot(eq);
      if (!worn.isEmpty()) {
        snapshot.append(eq.getName()).append('=').append(worn.getCount()).append("x ")
            .append(BuiltInRegistries.ITEM.getKey(worn.getItem())).append(", ");
      }
    }
    for (int i = 0; i < person.personMainInv.getContainerSize(); i++) {
      ItemStack stack = person.personMainInv.getItem(i);
      if (!stack.isEmpty()) {
        snapshot.append("carry=").append(stack.getCount()).append("x ")
            .append(BuiltInRegistries.ITEM.getKey(stack.getItem())).append(", ");
      }
    }
    return snapshot.length() == 0 ? "nothing" : snapshot.substring(0, snapshot.length() - 2);
  }

}
