package com.quzzar.kithkyn.decision;

import com.quzzar.kithkyn.entities.KithkynAttachments;
import com.quzzar.kithkyn.entities.UndertakingService;
import com.quzzar.kithkyn.entities.UndertakingData.Lane;
import com.quzzar.kithkyn.entities.UndertakingData.Execution;
import com.quzzar.kithkyn.entities.UndertakingData.Phase;
import com.quzzar.kithkyn.entities.UndertakingData.Duty;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.entities.ai.ResidentWalk;
import com.quzzar.kithkyn.entities.UndertakingData.Position;
import com.quzzar.kithkyn.awareness.ObservationLog.Evidence;
import com.quzzar.kithkyn.awareness.ObservationLog.Kind;
import com.quzzar.kithkyn.awareness.ObservationLog.Observation;
import com.quzzar.kithkyn.awareness.ResidentAwareness;
import com.quzzar.kithkyn.chat.VillagerConversation;
import com.quzzar.kithkyn.configuration.KithkynConfig;
import com.quzzar.kithkyn.decision.ReactionState.Action;
import com.quzzar.kithkyn.decision.ReactionState.Status;
import com.quzzar.kithkyn.decision.ReactionState.Offer;
import com.quzzar.kithkyn.decision.ReactionState.Verdict;
import com.quzzar.kithkyn.decision.ReactionState.Accepted;
import com.quzzar.kithkyn.decision.ReactionState.Rejected;
import com.quzzar.kithkyn.entities.AgeStage;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.entities.Virtue;
import com.quzzar.kithkyn.village.LaborPlanner;
import com.quzzar.kithkyn.village.Occupation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;

/** Optional, session-local reactions to real evidence. Game code owns references, routes and all legal effects. */
public final class ResidentReactions {
  public record Movement(long revision, Action action, Vec3 destination, long expiresAt) { }
  private record Episode(Kind kind, UUID subject, long firstTick) { }
  private record Capture(Offer offer, Observation event, String dimension, Map<Action, Vec3> routes, UUID helper, Duty duty) { }
  private final RealPerson person;
  private final ReactionState state = new ReactionState();
  private final LinkedHashSet<Episode> handled = new LinkedHashSet<>();
  private Capture pending;
  private Capture active;
  private Movement movement;
  private Status status = Status.IDLE;
  private String detail = "No optional reaction has run.";
  private long nextDecision;
  private UUID undertaking;
  private LivingEntity defending;
  private long defenseExpires;
  private boolean loadedMattersChecked;

  public ResidentReactions(RealPerson person) { this.person = person; }
  public Optional<Movement> movement() { return Optional.ofNullable(movement); }
  public Status status() { return status; }
  public String report() { return "Enabled: " + KithkynConfig.ResidentReactions + ". " + status + ": " + detail; }

  /** Mounted under /kkdev for explicit failures and outcomes without asking the dialogue model to explain them. */
  public static com.mojang.brigadier.builder.LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> branch() {
    return net.minecraft.commands.Commands.literal("reaction").then(net.minecraft.commands.Commands.argument("target",
        net.minecraft.commands.arguments.EntityArgument.entity()).executes(context -> {
          var entity = net.minecraft.commands.arguments.EntityArgument.getEntity(context, "target");
          if (!(entity instanceof RealPerson resident)) return 0;
          context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(resident.reactions().report()), false);
          return 1;
        }));
  }

  /** Staggered admission, one decision per episode, ten-second resident cooldown. No model call from an active goal. */
  public void tick() {
    if (!(person.level() instanceof ServerLevel level)) return;
    long now = level.getGameTime();
    if (!loadedMattersChecked) {
      loadedMattersChecked = true;
      for (var matter : person.getData(KithkynAttachments.UNDERTAKINGS.get()).allOpen()) {
        if (matter.execution().filter(execution -> execution.resident().equals(person.getUUID())
            && execution.lane() == Lane.REACTION && execution.phase().executing()).isPresent()) {
          persist(matter.id(), Phase.INTERRUPTED, "The reaction session ended before a verified outcome; fresh perception and a new decision are required.");
        }
      }
    }
    if (defending != null) {
      if (!KithkynConfig.ResidentReactions || !person.isAlive() || now >= defenseExpires) {
        update(Status.INTERRUPTED, "Native defense ended or expired without a verified completion.");
      } else if (!defending.isAlive() && ResidentAwareness.visible(person, defending)) {
        update(Status.COMPLETED, "The witnessed threat died; no claim about who killed it.");
      } else if (person.getTarget() != defending) {
        update(Status.INTERRUPTED, "Native defense lost its target without a verified completion.");
      }
      return;
    }
    if (!KithkynConfig.ResidentReactions) {
      if (pending != null || movement != null) interrupt("Optional reactions were disabled.");
      return;
    }
    if (pending != null || movement != null) {
      if (!eligible() || active != null && now >= movement.expiresAt()) interrupt("A duty, immediate need, or expiry interrupted the reaction.");
      return;
    }
    if (now < nextDecision || Math.floorMod(now + person.getId(), 20) != 0 || !eligible()) return;
    List<Observation> observations = person.awareness().snapshot();
    Observation event = observations.stream().filter(this::trigger)
        .filter(observation -> !handled.contains(episode(observation)))
        .max(java.util.Comparator.comparingLong(Observation::lastTick)).orElse(null);
    if (event == null) return;
    var decisions = ReactionDecisions.of(level.getServer());
    if (!decisions.available(level.getServer())) { status = Status.DEFERRED; detail = "Waiting for the shared decision budget."; return; }
    Capture capture = capture(event, observations, now);
    if (capture == null) { remember(event); return; }
    pending = capture;
    update(Status.PENDING, "A bounded reaction decision is pending.");
    nextDecision = now + 200;
    remember(event);
    var character = new ReactionRequests.Character(person.getOccupation().name(), persona(),
        person.getVirtue(Virtue.AGGRESSION), person.getVirtue(Virtue.CURIOSITY),
        person.getVirtue(Virtue.PROTECT_SELF), person.getVirtue(Virtue.PROTECT_OTHERS),
        person.getVirtue(Virtue.DRIVE), person.getHealth() / person.getMaxHealth(), LaborPlanner.mustKeepOnPost(person),
        person.distanceTo(subject(event)), com.quzzar.kithkyn.relationships.OpinionService.opinionOf(person, event.subject().id()));
    decisions.choose(level.getServer(), ReactionRequests.build(character, event, capture.offer().actions(),
        UndertakingService.briefing(person.getData(KithkynAttachments.UNDERTAKINGS.get()),
            person.getUUID(), Optional.empty(), 3)), result -> apply(capture, result));
  }

  private String persona() {
    String blurb = person.getData(KithkynAttachments.PERSONA.get()).blurb();
    return person.getPersonality().displayName() + ": " + blurb.substring(0, Math.min(1200, blurb.length()));
  }

  private boolean trigger(Observation event) {
    return (event.kind() == Kind.VISIBLE_THREAT && event.evidence() == Evidence.SEEN
        || event.kind() == Kind.INJURY && (event.evidence() == Evidence.SEEN || event.evidence() == Evidence.EXPERIENCED));
  }

  private boolean eligible() {
    return person.isAlive() && !person.isRemoved() && !person.isNoAi() && !person.isRaider() && !person.isWanderingMerchant()
        && person.getLifeStage() != AgeStage.TODDLER && person.getLifeStage() != AgeStage.KID
        && !person.isSleeping() && !person.isPassenger() && !person.isQuarrelling()
        && !person.isOnFire() && !person.isInLava() && person.getAirSupply() > 0
        && person.getHealth() >= person.getMaxHealth() / 3F
        && !(person.shouldSleepAtNight() && person.level().isNight())
        && (person.getVillage() == null || !person.getVillage().isTraveler(person.getUUID()))
        && person.goalSelector.getAvailableGoals().stream().noneMatch(goal -> goal.isRunning() && goal.getPriority() < 3
            && (goal.getGoal().getFlags().contains(Goal.Flag.MOVE) || goal.getGoal().getFlags().contains(Goal.Flag.LOOK)));
  }

  private Capture capture(Observation event, List<Observation> observations, long now) {
    if (!eventValid(event)) return null;
    var actions = new ArrayList<>(List.of(Action.CONTINUE, Action.WARN));
    Map<Action, Vec3> routes = new EnumMap<>(Action.class);
    boolean mobile = ResidentObligations.mayLeavePost(person);
    UUID helper = null;
    LivingEntity subject = subject(event);
    if (event.kind() == Kind.VISIBLE_THREAT) {
      if (mobile) {
        Vec3 away = person.position().subtract(subject.position()).multiply(1, 0, 1).normalize();
        for (int distance : new int[] {8, 6, 4}) {
          Vec3 destination = standing(person.position().add(away.scale(distance)));
          if (destination.distanceToSqr(subject.position()) > person.distanceToSqr(subject) + 9 && reachable(destination)) {
            routes.put(Action.RETREAT, destination); actions.add(Action.RETREAT); break;
          }
        }
      }
      if (canDefend(subject)) actions.add(Action.DEFEND);
    } else if (mobile && subject instanceof RealPerson resident && allied(resident) && subject != person
        && observations.stream().noneMatch(observation -> observation.kind() == Kind.VISIBLE_THREAT)) {
      Vec3 destination = near(subject);
      if (destination != null) { routes.put(Action.INVESTIGATE, destination); actions.add(Action.INVESTIGATE); }
    }
    if (mobile) {
      List<RealPerson> guards = new ArrayList<>();
      ServerLevel level = (ServerLevel) person.level();
      level.getEntities(EntityTypeTest.forClass(RealPerson.class), person.getBoundingBox().inflate(16),
          guard -> guard != person && guard.isAlive() && guard.getOccupation() == Occupation.GUARD && allied(guard), guards, 8);
      for (RealPerson guard : guards) {
        if (!ResidentAwareness.visible(person, guard)) continue;
        Vec3 destination = near(guard);
        if (destination != null && (event.kind() != Kind.VISIBLE_THREAT
            || destination.distanceToSqr(subject.position()) > person.distanceToSqr(subject))) {
          helper = guard.getUUID(); routes.put(Action.SEEK_HELP, destination); actions.add(Action.SEEK_HELP); break;
        }
      }
    }
    return new Capture(state.offer(Math.min(now + 100, event.expiresTick()), actions), event,
        person.level().dimension().location().toString(), Map.copyOf(routes), helper, duty());
  }

  private void apply(Capture capture, DecisionProvider.Result result) {
    if (pending != capture) return;
    pending = null;
    double threshold = capture.event().kind() == Kind.VISIBLE_THREAT ? KithkynConfig.ReactionDangerConfidence : KithkynConfig.ReactionInjuryConfidence;
    Verdict verdict = state.resolve(capture.offer(), result, threshold, person.level().getGameTime());
    if (verdict instanceof Rejected rejected) { update(rejected.status(), rejected.detail()); return; }
    if (!KithkynConfig.ResidentReactions || !eligible() || !capture.duty().equals(duty()) || !capture.dimension().equals(person.level().dimension().location().toString())
        || !eventValid(capture.event())) { update(Status.STALE, "The resident, evidence, or duties changed while deciding."); return; }
    Action action = ((Accepted) verdict).action();
    if (action == Action.CONTINUE) { if (!begin(capture, action, null)) return; update(Status.COMPLETED, "Selected continue; no optional reaction started."); return; }
    if (action == Action.WARN) { if (begin(capture, action, null)) callOut(capture); return; }
    if (action == Action.DEFEND) {
      LivingEntity threat = subject(capture.event());
      if (!canDefend(threat)) {
        update(Status.STALE, "Native combat no longer permits this target."); return;
      }
      if (!begin(capture, action, null)) return;
      person.setTarget(threat);
      defending = threat; defenseExpires = person.level().getGameTime() + 200;
      underway();
      update(Status.STARTED, "Set the witnessed hostile as the native combat target; no victory is claimed.");
      return;
    }
    Vec3 destination = capture.routes().get(action);
    if (!ResidentObligations.mayLeavePost(person) || !reachable(destination) || !destinationValid(capture, action, destination)) {
      update(Status.STALE, "The captured route, post, or destination changed."); return;
    }
    if (!begin(capture, action, destination)) return;
    active = capture;
    movement = new Movement(capture.offer().revision(), action, destination, person.level().getGameTime() + 200);
    update(Status.STARTED, "Started " + action.key() + "; arrival has not happened yet.");
  }

  /** Called by the registered native goal; never infers completion from a model response. */
  public boolean validMovement(Movement expected) {
    return movement == expected && KithkynConfig.ResidentReactions && eligible() && active.duty().equals(duty())
        && active.dimension().equals(person.level().dimension().location().toString())
        && person.level().getGameTime() < expected.expiresAt()
        && ResidentObligations.mayLeavePost(person)
        && ResidentWalk.usableGround(person, expected.destination())
        && (expected.action() != Action.INVESTIGATE || person.awareness().snapshot().stream().noneMatch(event -> event.kind() == Kind.VISIBLE_THREAT))
        && (person.tickCount % 10 != 0 || destinationValid(active, expected.action(), expected.destination()));
  }

  public void arrived(Movement expected) {
    if (movement != expected) return;
    Capture capture = active;
    if (!validMovement(expected) || !destinationValid(capture, expected.action(), expected.destination())) {
      finish(Status.STALE, "The destination or observed resident changed before arrival."); return;
    }
    if (expected.action() == Action.SEEK_HELP) callOut(capture);
    else update(Status.COMPLETED, expected.action() == Action.RETREAT ? "Reached the captured retreat point; safety is not guaranteed."
        : "Reached the observed injured resident; no treatment was performed.");
    movement = null; active = null;
  }

  private boolean destinationValid(Capture capture, Action action, Vec3 destination) {
    if (action == Action.SEEK_HELP) {
      var entity = ((ServerLevel) person.level()).getEntity(capture.helper());
      return entity instanceof RealPerson guard && guard.isAlive() && guard.getOccupation() == Occupation.GUARD
          && allied(guard) && guard.position().distanceToSqr(destination) <= 16 && ResidentAwareness.visible(person, guard);
    }
    if (action == Action.INVESTIGATE) return person.awareness().snapshot().stream().noneMatch(observation -> observation.kind() == Kind.VISIBLE_THREAT) && eventValid(capture.event()) && subject(capture.event()).position().distanceToSqr(destination) <= 16;
    LivingEntity threat = subject(capture.event());
    return threat != null && threat.isAlive() && threat.position().distanceToSqr(destination) > 16;
  }

  public void finish(Status outcome, String message) { movement = null; active = null; update(outcome, message); }
  public void interrupt(String message) {
    if (pending == null && movement == null && defending == null) return;
    state.invalidate(); pending = null; finish(Status.INTERRUPTED, message);
  }

  private void callOut(Capture capture) {
    underway();
    VillagerConversation.speak(person, capture.event().kind() == Kind.INJURY ? "Someone is hurt!" : "Watch out, there's danger nearby!");
    update(Status.COMPLETED, "Called out a warning. No listener response is claimed.");
  }

  private Duty duty() {
    return ResidentObligations.snapshot(person);
  }

  private boolean canDefend(LivingEntity threat) {
    return threat != null && person.getTarget() == null && person.willInitiateCombat()
        && person.canAttack(threat) && !person.isAlliedTo(threat) && person.isCampfireThreat(threat)
        && (!(threat instanceof net.minecraft.world.entity.NeutralMob neutral) || neutral.isAngryAt(person));
  }

  private boolean eventValid(Observation event) {
    LivingEntity subject = subject(event);
    return subject != null && subject.isAlive() && !subject.isRemoved()
        && event.place().dimension().equals(person.level().dimension().location().toString())
        && subject.position().distanceToSqr(new Vec3(event.place().x(), event.place().y(), event.place().z())) <= 16
        && (subject == person && event.evidence() == Evidence.EXPERIENCED || ResidentAwareness.visible(person, subject));
  }
  private LivingEntity subject(Observation event) {
    return person.level() instanceof ServerLevel level && level.getEntity(event.subject().id()) instanceof LivingEntity living ? living : null;
  }
  private boolean allied(RealPerson other) { return !other.isRaider() && person.getVillage() != null && other.getVillage() == person.getVillage(); }
  private boolean reachable(Vec3 destination) {
    if (!ResidentWalk.usableGround(person, destination)) return false;
    var path = ResidentWalk.path(person, destination);
    return path != null && path.canReach();
  }
  private Vec3 near(LivingEntity entity) {
    for (Vec3 offset : List.of(new Vec3(2, 0, 0), new Vec3(-2, 0, 0), new Vec3(0, 0, 2), new Vec3(0, 0, -2))) {
      Vec3 point = standing(entity.position().add(offset));
      if (reachable(point)) return point;
    }
    return null;
  }
  private static Vec3 standing(Vec3 point) { return Vec3.atBottomCenterOf(BlockPos.containing(point)); }
  private static Episode episode(Observation event) { return new Episode(event.kind(), event.subject().id(), event.firstTick()); }
  private void remember(Observation event) { handled.add(episode(event)); while (handled.size() > 32) handled.remove(handled.iterator().next()); }
  private boolean begin(Capture capture, Action action, Vec3 destination) {
    var key = KithkynAttachments.UNDERTAKINGS.get();
    var execution = new Execution(person.getUUID(),
        Lane.REACTION, action.key(), Phase.REQUESTED, capture.dimension(),
        Optional.ofNullable(destination).map(value -> new Position(value.x, value.y, value.z)),
        Optional.of(action == Action.SEEK_HELP ? capture.helper() : capture.event().subject().id()), capture.offer().revision(), person.level().getGameTime(), person.level().getGameTime() + 200, capture.duty());
    var result = UndertakingService.requested(person.getData(key), execution, Optional.empty(), person.level().getDayTime());
    if (!UndertakingService.commit(person, result)) { update(Status.FAILED, "No capacity to record this reaction undertaking."); return false; }
    undertaking = result.affected().orElseThrow().id();
    if (!execution.instant() && !persist(undertaking, Phase.ACCEPTED, "Validated reaction accepted; no outcome is claimed.")) {
      update(Status.STALE, "The reaction could not be accepted in its saved undertaking."); return false;
    }
    return true;
  }

  public void underway() {
    if (undertaking == null) return;
    var matter = person.getData(KithkynAttachments.UNDERTAKINGS.get()).byId(undertaking);
    if (matter.flatMap(value -> value.execution()).filter(value -> value.phase() == Phase.ACCEPTED).isPresent()) {
      persist(undertaking, Phase.UNDERWAY, "The physical reaction began; it has not completed.");
    }
  }

  private boolean persist(UUID id, Phase phase, String message) {
    var key = KithkynAttachments.UNDERTAKINGS.get();
    return UndertakingService.commit(person,
        UndertakingService.transition(person.getData(key), person.getUUID(), id, phase, message, person.level().getDayTime()));
  }

  private void update(Status next, String message) {
    if (undertaking != null && (next == Status.COMPLETED || next == Status.FAILED || next == Status.INTERRUPTED || next == Status.STALE)) {
      if (!persist(undertaking, next == Status.COMPLETED ? Phase.COMPLETED
          : next == Status.FAILED ? Phase.FAILED : Phase.INTERRUPTED, message)) {
        var saved = person.getData(KithkynAttachments.UNDERTAKINGS.get()).byId(undertaking);
        Phase phase = saved.flatMap(value -> value.execution()).map(value -> value.phase()).orElse(Phase.INTERRUPTED);
        next = switch (phase) {
          case COMPLETED -> Status.COMPLETED;
          case FAILED -> Status.FAILED;
          case INTERRUPTED, CANCELLED, EXPIRED -> Status.INTERRUPTED;
          default -> Status.STALE;
        };
        message = "No new outcome was recorded. Saved status: " + phase + ". "
            + saved.flatMap(value -> value.progressNote()).orElse("The saved undertaking is unavailable.");
      }
      undertaking = null; defending = null;
    }
    status = next; detail = message;
    Kithkyn.LOGGER.info("[resident reaction] {} {}: {}", person.getUUID(), next, message);
  }
}
