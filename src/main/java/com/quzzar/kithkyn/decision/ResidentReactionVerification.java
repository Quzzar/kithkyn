package com.quzzar.kithkyn.decision;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.PersonEntityType;
import com.quzzar.kithkyn.awareness.ResidentAwareness;
import com.quzzar.kithkyn.configuration.KithkynConfig;
import com.quzzar.kithkyn.chat.PersonChatDispatcher;
import com.quzzar.kithkyn.chat.PersonChatContext;
import com.quzzar.kithkyn.llm.LlmService;
import com.quzzar.kithkyn.decision.ReactionState.Status;
import com.quzzar.kithkyn.entities.*;
import com.quzzar.kithkyn.village.*;
import com.quzzar.kithkyn.village.buildings.Building;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Isolated real-world routes and lifecycle with a controlled typed endpoint. Separate from model quality replay. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class ResidentReactionVerification {
  private static final boolean LIVE_MODELS = Boolean.getBoolean("kithkyn.residentReactions.models");
  private static LiveRun live;
  private static int ticks, phase, calls;
  private static boolean finished;
  private static ServerLevel level;
  private static Village village;
  private static RealPerson observer, injured, guard;
  private static Zombie threat;
  private static CompletableFuture<DecisionProvider.Result> pending;
  private static DecisionProvider.Request request;
  private static Vec3 destination;
  private ResidentReactionVerification() { }

  @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
    if (!Boolean.getBoolean("kithkyn.residentReactions.verify") || finished || ++ticks < 40) return;
    try {
      if (LIVE_MODELS && !LlmService.get().isReady()) {
        check(LlmService.get().getStatus() != LlmService.Status.FAILED, "Local dialogue provider failed: " + LlmService.get().getStatusDetail());
        check(ticks < 1800, "Timed out waiting for the real dialogue model");
        return;
      }
      if (level == null) setup(event.getServer().overworld());
      if (LIVE_MODELS) { live.tick(); check(ticks < 7200, "Timed out in live-model acceptance"); return; }
      check(ticks < 1800, "Timed out in phase " + phase + ": " + observer.reactions().report());
      switch (phase) {
        case 0 -> {
          if (pending == null) return;
          check(offered("investigate"), "Injured ally was not offered an actual reachable approach");
          check(!offered("defend") && !offered("retreat"), "Injury offered combat or an invented escape");
          complete("investigate", 1);
          phase++;
        }
        case 1 -> {
          if (observer.reactions().movement().isEmpty()) return;
          destination = observer.reactions().movement().orElseThrow().destination();
          check(observer.distanceToSqr(destination) > 4, "Approach fixture did not require movement");
          phase++;
        }
        case 2 -> {
          if (observer.reactions().movement().isPresent()) return;
          check(observer.reactions().status() == Status.COMPLETED && observer.distanceToSqr(destination) <= 1,
              "Approach did not physically complete: " + observer.reactions().report());
          check(calls == 1, "Repeated observation flooded decisions");
          freshObserver(); ResidentAwareness.injured(injured, null); phase++;
        }
        case 3 -> {
          if (pending == null) return;
          complete("warn", .1); phase++;
        }
        case 4 -> {
          if (observer.reactions().status() == Status.PENDING) return;
          check(observer.reactions().status() == Status.UNCERTAIN && observer.reactions().movement().isEmpty(), "Low confidence executed");
          freshObserver(); ResidentAwareness.injured(injured, null); phase++;
        }
        case 5 -> {
          if (pending == null) return;
          complete("teleport", 1); phase++;
        }
        case 6 -> {
          if (observer.reactions().status() == Status.PENDING) return;
          check(observer.reactions().status() == Status.FAILED && observer.reactions().movement().isEmpty(), "Invented reaction executed");
          freshObserver(); ResidentAwareness.injured(injured, null); phase++;
        }
        case 7 -> {
          if (pending == null) return;
          injured.moveTo(injured.position().add(8, 0, 0)); complete("investigate", 1); phase++;
        }
        case 8 -> {
          if (observer.reactions().status() == Status.PENDING) return;
          check(observer.reactions().status() == Status.STALE && observer.reactions().movement().isEmpty(), "Moved subject did not stale the offer");
          injured.moveTo(108.5, 161, 100.5);
          freshObserver(); ResidentAwareness.injured(injured, null); phase++;
        }
        case 9 -> {
          if (pending == null) return;
          observer.setLifeStage(AgeStage.KID); complete("investigate", 1); phase++;
        }
        case 10 -> {
          check(observer.reactions().movement().isEmpty(), "Child moved for a late adult offer");
          if (pending != null) return;
          freshObserver(); ResidentAwareness.injured(injured, null); phase++;
        }
        case 11 -> {
          if (pending == null) return;
          complete("investigate", 1); phase++;
        }
        case 12 -> {
          if (observer.reactions().movement().isEmpty()) return;
          KithkynConfig.ResidentReactions = false; phase++;
        }
        case 13 -> {
          if (observer.reactions().movement().isPresent()) return;
          check(observer.reactions().status() == Status.INTERRUPTED, "Disabling reactions retained a walk");
          KithkynConfig.ResidentReactions = true;
          freshObserver();
          threat = EntityType.ZOMBIE.create(level); threat.setNoAi(true); threat.moveTo(observer.position().add(6, 0, 0));
          check(level.addFreshEntity(threat), "Threat spawn failed"); phase++;
        }
        case 14 -> {
          if (pending == null) return;
          check(offered("retreat"), "Reachable retreat was not offered");
          complete("retreat", 1); phase++;
        }
        case 15 -> {
          if (observer.reactions().movement().isEmpty()) return;
          destination = observer.reactions().movement().orElseThrow().destination(); phase++;
        }
        case 16 -> {
          if (observer.reactions().movement().isPresent()) return;
          check(observer.reactions().status() == Status.COMPLETED && observer.distanceToSqr(destination) <= 1,
              "Retreat did not physically complete: " + observer.reactions().report());
          threat.discard();
          freshObserver(); guard = person(112.5, 102.5); guard.setOccupation(Occupation.GUARD); guard.setNoAi(true);
          ResidentAwareness.injured(injured, null); phase++;
        }
        case 17 -> {
          if (pending == null) return;
          check(offered("seek_help"), "Visible allied guard did not offer help route: " + request
              + "; helper=" + guard.position() + ", occupation=" + guard.getOccupation()
              + ", same village=" + (guard.getVillage() == observer.getVillage())
              + ", visible=" + com.quzzar.kithkyn.wrongdoing.Witnesses.canIdentify(level, observer, guard));
          complete("seek_help", 1); phase++;
        }
        case 18 -> {
          if (observer.reactions().movement().isEmpty()) return;
          guard.moveTo(guard.position().add(8, 0, 0)); phase++;
        }
        case 19 -> {
          if (observer.reactions().movement().isPresent()) return;
          check(observer.reactions().status() == Status.INTERRUPTED, "Moved helper did not interrupt walk");
          freshObserver(); observer.setOccupation(Occupation.BUILDER); keepReactionGoal();
          ResidentAwareness.injured(injured, null); phase++;
        }
        case 20 -> {
          if (pending == null) return;
          check(!offered("investigate") && !offered("seek_help") && !offered("retreat"), "Essential last worker was offered an excursion");
          complete("warn", 1); phase++;
        }
        case 21 -> {
          if (observer.reactions().status() == Status.PENDING) return;
          check(observer.reactions().status() == Status.COMPLETED && observer.reactions().movement().isEmpty(), "Warning did not finish without movement");
          freshObserver(); ResidentAwareness.injured(injured, null); phase++;
        }
        case 22 -> {
          if (pending == null) return;
          observer.setLifeStage(AgeStage.TEENAGER); keepReactionGoal(); complete("investigate", 1); phase++;
        }
        case 23 -> {
          if (observer.reactions().status() == Status.PENDING) return;
          check(observer.reactions().status() == Status.STALE && observer.reactions().movement().isEmpty(), "Eligible age change did not stale offer");
          freshObserver(); ResidentAwareness.injured(injured, null); phase++;
        }
        case 24 -> {
          if (pending == null) return;
          observer.setOccupation(Occupation.GUARD); keepReactionGoal(); complete("investigate", 1); phase++;
        }
        case 25 -> {
          if (observer.reactions().status() == Status.PENDING) return;
          check(observer.reactions().status() == Status.STALE && observer.reactions().movement().isEmpty(), "Changed nonessential post did not stale offer");
          injured.setHealth(injured.getMaxHealth());
          freshObserver(); observer.setOccupation(Occupation.GUARD); keepReactionGoal();
          threat = EntityType.ZOMBIE.create(level); threat.setNoAi(true); threat.moveTo(observer.position().add(6, 0, 0));
          check(level.addFreshEntity(threat), "Defense threat spawn failed"); phase++;
        }
        case 26 -> {
          if (pending == null) return;
          check(offered("defend"), "Native guard defense was not offered");
          complete("defend", 1); phase++;
        }
        case 27 -> {
          if (observer.reactions().status() == Status.PENDING) return;
          check(observer.reactions().status() == Status.STARTED && observer.getTarget() == threat, "Defense did not set the actual combat target");
          var matter = observer.getData(KithkynAttachments.UNDERTAKINGS.get()).allOpen().stream()
              .filter(value -> value.execution().filter(execution -> execution.action().equals("defend")).isPresent()).findFirst().orElseThrow();
          level.getServer().getWorldData().overworldData().setGameTime(matter.execution().orElseThrow().expiresAt());
          threat.setHealth(0);
          UndertakingService.maintain(observer); observer.reactions().tick();
          check(observer.reactions().status() == Status.INTERRUPTED, "Defense claimed completion at its expiry boundary");
          check(observer.getData(KithkynAttachments.UNDERTAKINGS.get()).byId(matter.id()).orElseThrow().execution().orElseThrow().phase() == UndertakingData.Phase.EXPIRED,
              "Defense report disagreed with its authoritative expired undertaking");
          verifySavedHistory();
          verifyGifts();
          finished = true;
          Kithkyn.LOGGER.info("[resident-reaction-verify] RESULT PASS: physical investigate and retreat, episode deduplication, uncertain, invalid, stale subject, late child, disable interruption, moved help reference, last essential worker, warning, eligible age and nonessential post changes, defense deadline and authoritative expiry, actual reload without replay, retired command closure and inventory preservation; controlled decisions, no model-quality claim");
          level.getServer().halt(false);
        }
        default -> throw new AssertionError("Unknown phase");
      }
    } catch (Exception | AssertionError failure) {
      if (live != null) live.failure(failure);
      finished = true; Kithkyn.LOGGER.error("[resident-reaction-verify] RESULT FAIL", failure); event.getServer().halt(false);
    }
  }

  private static void setup(ServerLevel world) {
    level = world;
    KithkynConfig.ResidentReactions = true;
    KithkynConfig.ReactionDangerConfidence = KithkynConfig.ReactionInjuryConfidence = .8;
    world.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, world.getServer());
    world.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, world.getServer());
    world.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, world.getServer());
    world.setDayTime(6000); world.updateSkyBrightness(); world.getServer().tickRateManager().setTickRate(LIVE_MODELS ? 20 : 100);
    for (int x = 4; x <= 9; x++) for (int z = 4; z <= 9; z++) world.setChunkForced(x, z, true);
    for (var entity : world.getAllEntities()) if (entity instanceof RealPerson || entity instanceof Zombie) entity.discard();
    VillageManager.get(world).getVillages().clear();
    for (BlockPos pos : BlockPos.betweenClosed(70, 160, 70, 150, 166, 140)) world.setBlock(pos,
        (pos.getY() == 160 ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 2);
    village = new Village("Reaction verification") {
      private final Building center = new Building(new BlockPos(100, 160, 100), "village_center_birch_forest_1", Rotation.NONE);
      @Override public Building getTownCenter() { return center; }
      @Override public void update(ServerLevel ignored) { }
    };
    village.attach(world); VillageManager.get(world).getVillages().put(village.getID(), village);
    injured = person(108.5, 100.5); injured.setNoAi(true);
    freshObserver();
    if (LIVE_MODELS) { live = new LiveRun(); return; }
    ReactionDecisions.verificationProvider(world.getServer(), new DecisionProvider() {
      @Override public CompletableFuture<Result> choose(Request next) {
        check(pending == null, "Concurrent reaction request"); calls++; request = next; pending = new CompletableFuture<>(); return pending;
      }
      @Override public void close() { }
    });
    ResidentAwareness.injured(injured, null);
  }
  private static RealPerson person(double x, double z) {
    RealPerson person = LIVE_MODELS ? new LivePerson(level) : PersonEntityType.PERSON.get().create(level);
    person.setVillage(village.getID()); person.setOccupation(Occupation.WANDERER); person.setLifeStage(AgeStage.ADULT);
    person.setPersistenceRequired(); person.moveTo(x, 161, z); village.getPopulation().add(person.getUUID());
    check(level.addFreshEntity(person), "Resident spawn failed"); return person;
  }
  private static void freshObserver() {
    if (observer != null) observer.discard();
    observer = person(100.5, 100.5);
    // Isolate reaction navigation from ordinary work and panic, while keeping the registered reaction goal and native navigation.
    if (!LIVE_MODELS) keepReactionGoal();
  }
  private static void verifySavedHistory() {
    long now = level.getGameTime();
    var key = KithkynAttachments.UNDERTAKINGS.get();
    var execution = new UndertakingData.Execution(observer.getUUID(), UndertakingData.Lane.REACTION, "investigate", UndertakingData.Phase.REQUESTED,
        level.dimension().location().toString(), Optional.of(new UndertakingData.Position(108.5, 161, 100.5)), Optional.of(injured.getUUID()), 99,
        now, now + 200, ResidentObligations.snapshot(observer));
    var request = UndertakingService.requested(observer.getData(key), execution, Optional.empty(), level.getDayTime());
    check(UndertakingService.commit(observer, request), "Could not record the unfinished reaction before reload");
    UUID reaction = request.affected().orElseThrow().id();
    UndertakingService.commit(observer, UndertakingService.transition(observer.getData(key), observer.getUUID(), reaction, UndertakingData.Phase.ACCEPTED, "Accepted", level.getDayTime()));
    UndertakingService.commit(observer, UndertakingService.transition(observer.getData(key), observer.getUUID(), reaction, UndertakingData.Phase.UNDERWAY, "Began before reload", level.getDayTime()));
    // Seed the former experimental save shape, never the removed input/execution implementation.
    UUID retired = UUID.randomUUID(); UUID requester = new UUID(0, 1234);
    var oldExecution = new UndertakingData.Execution(observer.getUUID(), UndertakingData.Lane.PLAYER_REQUEST, "wait", UndertakingData.Phase.UNDERWAY,
        level.dimension().location().toString(), execution.destination(), Optional.empty(), 1, now, now + 6000, execution.duty());
    observer.setData(key, observer.getData(key).with(new UndertakingData.Undertaking(retired, UndertakingData.Valence.POSITIVE, UndertakingData.State.ACTIVE,
        UndertakingData.Origin.PLAYER, "An earlier private wait request", Optional.of(requester), List.of(), Optional.empty(), Optional.empty(), level.getDayTime(), level.getDayTime(), Optional.of(oldExecution))));
    observer.personMainInv.addItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND, 3));
    UUID residentId = observer.getUUID(); var saved = new net.minecraft.nbt.CompoundTag();
    check(observer.save(saved), "Could not serialize reaction history and inventory");
    observer.remove(net.minecraft.world.entity.Entity.RemovalReason.UNLOADED_TO_CHUNK);
    observer = (RealPerson) EntityType.create(saved, level).orElseThrow();
    check(level.addFreshEntity(observer), "Could not restore serialized resident");
    UndertakingService.maintain(observer); observer.reactions().tick();
    check(observer.getUUID().equals(residentId) && observer.personMainInv.countItem(net.minecraft.world.item.Items.DIAMOND) == 3, "Reload changed identity or inventory");
    check(observer.getData(key).byId(reaction).orElseThrow().execution().orElseThrow().phase() == UndertakingData.Phase.INTERRUPTED
        && observer.reactions().movement().isEmpty(), "Reload silently resumed the saved reaction");
    check(observer.getData(key).byId(retired).orElseThrow().execution().orElseThrow().phase() == UndertakingData.Phase.CANCELLED, "Retired player command was not closed as history");
    var other = com.quzzar.kithkyn.chat.PersonChatContext.assemble(observer, "Other", new UUID(0, 9876), List.of(), "What happened?");
    check(!other.system().contains("earlier private wait request") && !other.system().contains("Movement requests:"), "Removed/private command context leaked into normal chat");
  }

  private static void verifyGifts() {
    var player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,
        new com.mojang.authlib.GameProfile(new UUID(0, 1702), "GiftRecipient"));
    level.addNewPlayer(player); player.moveTo(observer.position().add(0, 0, 2));
    int before = player.getInventory().countItem(net.minecraft.world.item.Items.DIAMOND);
    var reply = PersonChatDispatcher.parseReply("{\"say\":\"This is for you.\",\"give\":\"minecraft:diamond\",\"give_count\":1}", "Hello again.");
    check(reply != null, "Unrequested valuable gift was rejected");
    PersonChatDispatcher.executeGive(observer, player, reply.give(), reply.giveCount());
    PersonChatDispatcher.executeGive(observer, player, reply.give(), reply.giveCount());
    check(observer.personMainInv.countItem(net.minecraft.world.item.Items.DIAMOND) == 1
        && player.getInventory().countItem(net.minecraft.world.item.Items.DIAMOND) == before + 2, "Valuable player gift did not conserve inventory");
    var sword = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);
    sword.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("For a friend"));
    observer.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, sword);
    injured.moveTo(observer.position().add(3, 0, 0));
    PersonChatDispatcher.executeGive(observer, injured, "minecraft:diamond_sword", 1);
    check(observer.getMainHandItem().isEmpty() && injured.personMainInv.getItems().stream().anyMatch(stack ->
        stack.is(net.minecraft.world.item.Items.DIAMOND_SWORD)
            && "For a friend".equals(stack.getHoverName().getString())), "Equipped gift lost its ownership or components");
    var saved = new net.minecraft.nbt.CompoundTag(); check(observer.save(saved), "Could not save gift facts");
    observer.remove(net.minecraft.world.entity.Entity.RemovalReason.UNLOADED_TO_CHUNK);
    observer = (RealPerson) EntityType.create(saved, level).orElseThrow(); check(level.addFreshEntity(observer), "Could not reload gift facts");
    String context = PersonChatContext.assemble(observer, "GiftRecipient", player.getUUID(), List.of(), "What did you give me?").system();
    check(observer.getData(KithkynAttachments.PERSONAL_LOG.get()).memoriesNewestFirst().stream()
        .filter(entry -> entry.text().contains("I handed 1 minecraft:diamond to GiftRecipient")).count() == 2,
        "Identical physical handoffs collapsed into one memory after reload");
    check(context.contains("I handed 1 minecraft:diamond to GiftRecipient") && context.contains("I handed 1 minecraft:diamond_sword"),
        "Actual gift outcomes were absent after entity reload");
  }
  private static void keepReactionGoal() {
    observer.goalSelector.removeAllGoals(goal -> !(goal instanceof com.quzzar.kithkyn.entities.ai.goals.ResidentReactionGoal));
    observer.targetSelector.removeAllGoals(goal -> true);
  }
  private static boolean offered(String key) { return request.options().stream().anyMatch(option -> option.key().equals(key)); }
  private static void complete(String key, double confidence) {
    var completion = pending; pending = null; completion.complete(new DecisionProvider.Selected(key, confidence, Map.of()));
  }

  /** Same registered entities, game event capture, HTTP decision provider and player chat dispatcher as live play. */
  private static final class LiveRun {
    private final com.google.gson.Gson json = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
    private final com.google.gson.JsonObject report = new com.google.gson.JsonObject();
    private final com.google.gson.JsonArray cases = new com.google.gson.JsonArray();
    private final com.google.gson.JsonArray decisions = new com.google.gson.JsonArray();
    private final java.nio.file.Path output = net.neoforged.fml.loading.FMLPaths.GAMEDIR.get().resolve("results/gameplay-" + System.currentTimeMillis() + ".json");
    private final net.minecraft.server.level.ServerPlayer speaker;
    private final UUID speakerId = new UUID(0, 1701);
    private com.google.gson.JsonObject current;
    private CompletableFuture<PersonChatDispatcher.Reply> chat;
    private int scenario = -1, stage, since, question, chatDoneAt = -1;
    private long chatStarted;
    private final long started = System.nanoTime();

    private LiveRun() {
      check(KithkynConfig.LlmEnabled && "local".equals(KithkynConfig.LlmProviderName)
          && "llama-3b".equals(KithkynConfig.LlmLocalModel), "Live acceptance requires the configured stock local Llama 3B");
      speaker = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,
          new com.mojang.authlib.GameProfile(speakerId, "Rowan"));
      level.addNewPlayer(speaker);
      ((LivePerson) injured).name("Tessa", "Reed");
      report.addProperty("status", "running"); report.addProperty("controlled_replies", false);
      report.addProperty("native_goals_removed", false); report.addProperty("tick_rate", 20);
      report.addProperty("native_policy_validated", true);
      report.addProperty("dialogue_model", KithkynConfig.LlmLocalModel);
      report.addProperty("decision_model", KithkynConfig.ReactionModel);
      report.addProperty("danger_confidence", KithkynConfig.ReactionDangerConfidence);
      report.addProperty("injury_confidence", KithkynConfig.ReactionInjuryConfidence);
      report.addProperty("chat_entry", "Existing player chat dispatcher with simulated player; no rendered client");
      report.add("cases", cases); report.add("decisions", decisions);
      var provider = new SystemOneProvider(KithkynConfig.ReactionEndpoint, KithkynConfig.ReactionModel, KithkynConfig.ReactionApiKey);
      ReactionDecisions.verificationProvider(level.getServer(), new DecisionProvider() {
        @Override public CompletableFuture<Result> choose(Request next) {
          long begin = System.nanoTime();
          var row = new com.google.gson.JsonObject(); row.addProperty("case", scenario);
          row.addProperty("game_tick", level.getGameTime()); row.add("request", SystemOneProvider.payload(next, KithkynConfig.ReactionModel));
          decisions.add(row); calls++;
          return provider.choose(next).thenApply(result -> {
            level.getServer().execute(() -> { row.addProperty("latency_ms", (System.nanoTime() - begin) / 1_000_000.0);
              row.add("parsed_provider_result", json.toJsonTree(result)); save(); });
            return result;
          });
        }
        @Override public void close() { provider.close(); }
      });
      save(); beginCase();
    }

    private void beginCase() {
      scenario++;
      if (scenario == 4) {
        report.addProperty("status", "recorded");
        report.addProperty("acceptance", "Requires review against actual outcomes; harness completion is not a quality pass");
        report.addProperty("elapsed_seconds", (System.nanoTime() - started) / 1_000_000_000.0);
        report.addProperty("decision_calls", calls);
        report.addProperty("generative_calls", com.quzzar.kithkyn.llm.LlmCallLog.requestCount()); save();
        finished = true;
        Kithkyn.LOGGER.info("[resident-model-verify] RESULT RECORDED: {}. Real providers; review outcomes and dialogue before claiming acceptance.", output);
        level.getServer().halt(false); return;
      }
      if (threat != null) { threat.discard(); threat = null; }
      freshObserver(); ((LivePerson) observer).name(scenario == 0 ? "Mira" : scenario == 1 ? "Bram" : scenario == 2 ? "Alden" : "Nora", "Flint");
      ((LivePerson) observer).character(scenario);
      injured.setHealth(injured.getMaxHealth()); injured.moveTo(scenario == 3 ? 103.5 : 108.5, 161, 100.5);
      observer.personMainInv.addItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND, 3));
      if (scenario == 0) observer.personMainInv.addItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD, 3));
      current = new com.google.gson.JsonObject();
      current.addProperty("scenario", new String[] {"helpful-injury", "reserved-injury", "guard-threat", "nearby-curious-injury"}[scenario]);
      current.addProperty("resident", observer.getFullName()); current.addProperty("start_position", observer.position().toString());
      current.addProperty("speaker_diamonds_before", speaker.getInventory().countItem(net.minecraft.world.item.Items.DIAMOND));
      current.addProperty("speaker_bread_before", speaker.getInventory().countItem(net.minecraft.world.item.Items.BREAD));
      current.add("dialogue", new com.google.gson.JsonArray()); cases.add(current);
      if (scenario == 2) {
        threat = EntityType.ZOMBIE.create(level); threat.setNoAi(true); threat.moveTo(observer.position().add(6, 0, 0));
        check(level.addFreshEntity(threat), "Live threat did not spawn");
      } else check(injured.hurt(level.damageSources().fall(), 4), "Actual injury event was not delivered");
      stage = question = 0; since = ticks; save();
    }

    private void tick() {
      if (stage == 0) {
        Status status = observer.reactions().status();
        boolean ended = status != Status.IDLE && status != Status.PENDING && status != Status.DEFERRED && status != Status.STARTED;
        if (!ended && ticks - since < 300) return;
        check(status != Status.PENDING, "Reaction did not finish its bounded provider request");
        current.addProperty("reaction_status", status.name()); current.addProperty("reaction_report", observer.reactions().report());
        current.addProperty("end_position", observer.position().toString());
        current.add("recorded_undertakings", UndertakingData.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,
            observer.getData(KithkynAttachments.UNDERTAKINGS.get())).getOrThrow());
        current.add("recent_observations", json.toJsonTree(observer.awareness().snapshot()));
        current.addProperty("victim_health", injured.getHealth());
        current.addProperty("native_target", observer.getTarget() == null ? "none" : observer.getTarget().getType().toShortString());
        if (threat != null) {
          current.addProperty("threat_alive", threat.isAlive());
          var damage = threat.getLastDamageSource();
          current.addProperty("last_damager", damage == null || damage.getEntity() == null ? "unknown" : damage.getEntity().getUUID().toString());
          threat.discard(); observer.setTarget(null);
        }
        ask("What did you notice nearby just now, and what did you actually do about it?"); stage = 1;
      } else if (stage == 1 || stage == 3) {
        if (!chat.isDone()) return;
        if (chatDoneAt < 0) { chatDoneAt = ticks; return; }
        if (ticks == chatDoneAt) return;
        PersonChatDispatcher.Reply reply = chat.join();
        var row = new com.google.gson.JsonObject(); row.addProperty("question", question);
        row.addProperty("after_reload", stage == 3); row.addProperty("usable", reply != null);
        row.addProperty("latency_ms", (System.nanoTime() - chatStarted) / 1_000_000.0);
        row.add("reply", json.toJsonTree(reply)); current.getAsJsonArray("dialogue").add(row); save();
        if (stage == 3) {
          if (scenario == 0 && question == 2) { question++; ask("Could you spare one bread from your pockets?"); return; }
          if (scenario == 0 && question == 3) { question++; ask("Was Tessa still hurt when you last saw her?"); return; }
          if (scenario == 0 && question == 4) { question++; ask("That was a useless answer. You're an idiot."); return; }
          if (scenario == 0 && question == 5) { question++; ask("I'm sorry for insulting you. You didn't deserve that."); return; }
          if (scenario == 0 && question == 6) { question++; ask("Could you give me one diamond from your pockets?"); return; }
          if (scenario == 0 && question == 7) {
            current.addProperty("bread_before_description", observer.personMainInv.countItem(net.minecraft.world.item.Items.BREAD));
            question++; ask("Could you give me a description of the bread you carry?"); return;
          }
          if (scenario == 0 && question == 8) { question++; ask("Which items have you actually handed me during this conversation? Distinguish gifts from offers."); return; }
          current.addProperty("diamonds_remaining", observer.personMainInv.countItem(net.minecraft.world.item.Items.DIAMOND));
          current.addProperty("speaker_diamonds_after", speaker.getInventory().countItem(net.minecraft.world.item.Items.DIAMOND));
          check(observer.personMainInv.countItem(net.minecraft.world.item.Items.DIAMOND)
              + speaker.getInventory().countItem(net.minecraft.world.item.Items.DIAMOND)
              - current.get("speaker_diamonds_before").getAsInt() == 3, "Gift did not conserve diamond ownership");
          current.add("personal_log", PersonalLogData.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,
              observer.getData(KithkynAttachments.PERSONAL_LOG.get())).getOrThrow());
          current.addProperty("speaker_bread_after", speaker.getInventory().countItem(net.minecraft.world.item.Items.BREAD));
          current.addProperty("bread_remaining", observer.personMainInv.countItem(net.minecraft.world.item.Items.BREAD));
          PersonChatDispatcher.markClosed(observer.getId(), speakerId); beginCase(); return;
        }
        if (++question == 1) {
          ask(scenario == 2 ? "Did you kill that zombie yourself? Tell me only what actually happened."
              : "Did you heal Tessa or make her injury better?"); return;
        }
        PersonChatDispatcher.markClosed(observer.getId(), speakerId);
        PersonChatDispatcher.summarizeSession(observer, speakerId, "Rowan");
        stage = 2; since = ticks;
      } else if (stage == 2) {
        String summary = observer.getData(KithkynAttachments.CHAT_SUMMARY.get()).with(speakerId);
        if (summary.isBlank() && ticks - since < 500) return;
        current.addProperty("summary", summary); current.addProperty("summary_available", !summary.isBlank());
        UUID id = observer.getUUID(); int diamonds = observer.personMainInv.countItem(net.minecraft.world.item.Items.DIAMOND);
        current.addProperty("diamonds_before_reload", diamonds);
        check(diamonds + speaker.getInventory().countItem(net.minecraft.world.item.Items.DIAMOND)
            - current.get("speaker_diamonds_before").getAsInt() == 3, "Gift conservation failed before reload");
        var tag = new net.minecraft.nbt.CompoundTag();
        check(observer.save(tag), "Live resident could not serialize");
        observer.remove(net.minecraft.world.entity.Entity.RemovalReason.UNLOADED_TO_CHUNK);
        observer = (RealPerson) EntityType.create(tag, level).orElseThrow();
        check(level.addFreshEntity(observer), "Live resident could not reload");
        check(observer.getUUID().equals(id) && observer.personMainInv.countItem(net.minecraft.world.item.Items.DIAMOND) == diamonds, "Live reload changed identity or inventory");
        current.addProperty("reload_identity_and_inventory_preserved", true);
        ask(scenario == 2 ? "Thinking back, what do you remember actually doing about the zombie?"
            : "Thinking back, what do you remember actually doing when Tessa was hurt?"); stage = 3;
      }
    }

    private void ask(String message) {
      speaker.moveTo(observer.position().add(0, 0, 2));
      PersonChatDispatcher.markOpen(observer, speaker);
      current.addProperty("last_question", message); chatStarted = System.nanoTime(); chatDoneAt = -1;
      chat = PersonChatDispatcher.handle(speaker, observer, message); save();
    }
    private void save() {
      try { java.nio.file.Files.createDirectories(output.getParent()); java.nio.file.Files.writeString(output, json.toJson(report)); }
      catch (java.io.IOException failed) { throw new IllegalStateException("Cannot preserve real-model evidence", failed); }
    }
    private void failure(Throwable failure) { report.addProperty("status", "error"); report.addProperty("error", failure.toString()); save(); }
  }

  private static final class LivePerson extends RealPerson {
    private LivePerson(ServerLevel level) { super(PersonEntityType.PERSON.get(), level); name("Resident", "Test"); setGender(Gender.FEMALE); }
    private void name(String first, String last) { setFirstName(first); setLastName(last); }
    private void character(int scenario) {
      setOccupation(scenario == 2 ? Occupation.GUARD : Occupation.WANDERER);
      setVirtue(Virtue.AGGRESSION, scenario == 2 ? .4F : -.35F);
      setVirtue(Virtue.CURIOSITY, scenario == 1 ? -.45F : .45F);
      setVirtue(Virtue.PROTECT_OTHERS, scenario == 1 ? -.45F : .45F);
      setVirtue(Virtue.PROTECT_SELF, .3F); setVirtue(Virtue.DRIVE, .2F);
      setPersonality(Personality.generateFromVirtues(getVirtue(Virtue.AGGRESSION), getVirtue(Virtue.CURIOSITY), getVirtue(Virtue.DRIVE), getVirtue(Virtue.PROTECT_OTHERS), getVirtue(Virtue.PROTECT_SELF)));
      setData(KithkynAttachments.PERSONA.get(), new com.quzzar.kithkyn.persona.PersonaData(scenario == 1
          ? "A private, incurious resident who avoids getting involved in other people's trouble. Admits what she does not know."
          : scenario == 3 ? "An inquisitive resident who checks on visibly injured neighbors by approaching to observe. Does not pretend to heal them. Admits what she does not know."
          : "A concerned, observant resident who notices injured neighbors and cares about others. Admits what she does not know.",
          "Speaks plainly.", "live-model-fixture", 0, 1));
      reloadState();
      check(goalSelector.getAvailableGoals().stream().filter(goal -> goal.getGoal() instanceof
          com.quzzar.kithkyn.entities.ai.goals.ResidentReactionGoal).count() == 1,
          "Live resident must have exactly one reaction goal after rebuilding native AI");
      if (scenario == 2) check(targetSelector.getAvailableGoals().stream().filter(goal -> goal.getGoal() instanceof
          com.quzzar.kithkyn.entities.ai.goals.GuardThreatGoal).count() == 1,
          "Live guard must have exactly one native threat-targeting goal");
    }
  }
  private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
