package com.quzzar.kithkyn.decision;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.PersonEntityType;
import com.quzzar.kithkyn.awareness.ResidentAwareness;
import com.quzzar.kithkyn.configuration.KithkynConfig;
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
      if (level == null) setup(event.getServer().overworld());
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
          check(offered("seek_help"), "Visible allied guard did not offer help route");
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
          observer.agency().maintain(); observer.reactions().tick();
          check(observer.reactions().status() == Status.INTERRUPTED, "Defense claimed completion at its expiry boundary");
          check(observer.getData(KithkynAttachments.UNDERTAKINGS.get()).byId(matter.id()).orElseThrow().execution().orElseThrow().phase() == UndertakingData.Phase.EXPIRED,
              "Defense report disagreed with its authoritative expired undertaking");
          finished = true;
          Kithkyn.LOGGER.info("[resident-reaction-verify] RESULT PASS: physical investigate and retreat, episode deduplication, uncertain, invalid, stale subject, late child, disable interruption, moved help reference, last essential worker, warning, eligible age and nonessential post changes, defense deadline and authoritative expiry; controlled decisions, no model-quality claim");
          level.getServer().halt(false);
        }
        default -> throw new AssertionError("Unknown phase");
      }
    } catch (Exception | AssertionError failure) {
      finished = true; Kithkyn.LOGGER.error("[resident-reaction-verify] RESULT FAIL", failure); event.getServer().halt(false);
    }
  }

  private static void setup(ServerLevel world) {
    level = world;
    KithkynConfig.ResidentReactions = true; KithkynConfig.LlmResidentActions = false;
    KithkynConfig.ReactionDangerConfidence = KithkynConfig.ReactionInjuryConfidence = .8;
    world.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, world.getServer());
    world.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, world.getServer());
    world.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, world.getServer());
    world.setDayTime(6000); world.updateSkyBrightness(); world.getServer().tickRateManager().setTickRate(100);
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
    ReactionDecisions.verificationProvider(world.getServer(), new DecisionProvider() {
      @Override public CompletableFuture<Result> choose(Request next) {
        check(pending == null, "Concurrent reaction request"); calls++; request = next; pending = new CompletableFuture<>(); return pending;
      }
      @Override public void close() { }
    });
    ResidentAwareness.injured(injured, null);
  }
  private static RealPerson person(double x, double z) {
    RealPerson person = PersonEntityType.PERSON.get().create(level);
    person.setVillage(village.getID()); person.setOccupation(Occupation.WANDERER); person.setLifeStage(AgeStage.ADULT);
    person.setPersistenceRequired(); person.moveTo(x, 161, z); village.getPopulation().add(person.getUUID());
    check(level.addFreshEntity(person), "Resident spawn failed"); return person;
  }
  private static void freshObserver() {
    if (observer != null) observer.discard();
    observer = person(100.5, 100.5);
    // Isolate reaction navigation from ordinary work and panic, while keeping the registered reaction goal and native navigation.
    keepReactionGoal();
  }
  private static void keepReactionGoal() {
    observer.goalSelector.removeAllGoals(goal -> !(goal instanceof com.quzzar.kithkyn.entities.ai.goals.ResidentReactionGoal));
    observer.targetSelector.removeAllGoals(goal -> true);
  }
  private static boolean offered(String key) { return request.options().stream().anyMatch(option -> option.key().equals(key)); }
  private static void complete(String key, double confidence) {
    var completion = pending; pending = null; completion.complete(new DecisionProvider.Selected(key, confidence, Map.of()));
  }
  private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
