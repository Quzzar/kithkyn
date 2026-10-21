package com.quzzar.kithkyn.agency;

import com.quzzar.kithkyn.entities.KithkynAttachments;
import com.quzzar.kithkyn.entities.UndertakingService;
import com.quzzar.kithkyn.entities.UndertakingData.Lane;
import com.quzzar.kithkyn.entities.UndertakingData.Phase;
import java.util.Optional;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.agency.ResidentActionState.Action;
import com.quzzar.kithkyn.agency.ResidentActionState.Offer;
import com.quzzar.kithkyn.agency.ResidentActionState.Order;
import com.quzzar.kithkyn.agency.ResidentActionState.Outcome;
import com.quzzar.kithkyn.entities.UndertakingData.Position;
import com.quzzar.kithkyn.entities.AgeStage;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.entities.ai.goals.ResidentActionGoal;
import com.quzzar.kithkyn.configuration.KithkynConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.goal.Goal;

/** The server-owned bridge from an offered conversational action to navigation and truthful memory. */
public final class ResidentAgency {
  private final RealPerson person;
  private final ResidentActionState state;
  private long nextReconsider;
  private int reconsiderCursor;
  private static final java.util.Map<net.minecraft.server.MinecraftServer, Integer> RECONSIDERATION_TICKS = new java.util.WeakHashMap<>();

  public ResidentAgency(RealPerson person) {
    this.person = person;
    state = new ResidentActionState(person::getUUID, () -> person.getData(KithkynAttachments.UNDERTAKINGS.get()),
        result -> UndertakingService.commit(person, result), () -> person.level().getDayTime(), () -> ResidentObligations.snapshot(person));
  }

  public Optional<Order> current() {
    return state.current();
  }

  public void underway(Order order) { state.underway(order); }
  public boolean valid(Order order) {
    return current().filter(value -> value.revision() == order.revision()).isPresent() && interruption().isEmpty()
        && order.dimension().equals(person.level().dimension().location().toString())
        && order.duty().equals(ResidentObligations.snapshot(person));
  }

  public Offer offer(ServerPlayer requester) {
    return state.offer(requester.getUUID(), requester.getGameProfile().getName(),
        person.level().dimension().location().toString(), PointedLocations.get(requester).orElse(null),
        position(person.position()), person.level().getGameTime());
  }

  /** Capability text is appended only for a player's actual turn, never autonomous resident chatter. */
  public String prompt(Offer offer) {
    String point = offer.pointedAt() == null ? "No pointed location was supplied. Do not use go_to_point."
        : "The speaker pointed at " + coordinates(offer.pointedAt()) + ".";
    return "\nMovement requests: " + point + " " + describe(Optional.of(offer.requester()))
        + " If this speaker explicitly asks you to follow them, wait here, go to their pointed location, or resume"
        + " your ordinary activity, decide whether to agree, considering your personality and duties."
        + " You may refuse. For an agreed request add exactly one string field to the reply JSON:"
        + " \"action\":\"follow\", \"action\":\"wait\", \"action\":\"go_to_point\", or \"action\":\"resume\"."
        + " Most replies have no action. Hypothetical questions, requests about another person, greetings and"
        + " ordinary conversation have no action. Never invent coordinates. An agreement begins an attempt;"
        + " never claim you have arrived or finished before the game reports it."
        + " Example agreement: {\"say\":\"All right, I will come with you.\",\"action\":\"follow\"}."
        + " Example refusal: {\"say\":\"I have work to finish here.\"}.";
  }

  public void apply(Offer offer, ResidentActionSelection selection, ServerPlayer requester) {
    if (selection.error().isPresent()) {
      reportFailure(requester, selection.error().get());
      return;
    }
    if (selection.action().isEmpty()) return;
    Action action = selection.action().get();
    Optional<String> rejected = !offer.requester().equals(requester.getUUID())
        ? Optional.of("The request belongs to another speaker.") : state.rejection(offer, action, person.level().getGameTime());
    if (rejected.isEmpty() && !offer.dimension().equals(person.level().dimension().location().toString())) {
      rejected = Optional.of("The request came from another dimension.");
    }
    if (rejected.isEmpty()) rejected = unavailable(requester, action);
    if (rejected.isEmpty() && action == Action.GO_TO_POINT && !validGround(offer.pointedAt())) {
      rejected = Optional.of("The pointed location is no longer usable standing ground.");
    }
    if (rejected.isPresent()) {
      state.rejected(offer, action, rejected.get(), person.level().getGameTime());
      reportFailure(requester, rejected.get());
      return;
    }
    ResidentActionState.Acceptance accepted = state.accept(offer, action, person.level().getGameTime());
    if (!accepted.accepted()) {
      reportFailure(requester, accepted.detail());
      return;
    }
    accepted.replaced().ifPresent(this::record);
    if (action == Action.RESUME) state.latest().ifPresent(this::record);

    requester.displayClientMessage(Component.literal(person.getFullName() + ": " + accepted.detail()), true);
    Kithkyn.LOGGER.info("[resident action] {} accepted {}", person.getUUID(), action.wireName());
  }

  private Optional<String> unavailable(ServerPlayer requester, Action action) {
    if (!person.isAlive() || !requester.isAlive() || requester.isRemoved()
        || requester.level() != person.level() || requester.distanceToSqr(person) > 144) {
      return Optional.of("The conversation participants are no longer nearby.");
    }
    if (action == Action.RESUME) return Optional.empty();
    if (person.isRaider() || person.isWanderingMerchant() || !ResidentObligations.mayLeavePost(person)
        || person.getLifeStage() == AgeStage.TODDLER || person.getLifeStage() == AgeStage.KID) {
      return Optional.of("This resident cannot take that movement request.");
    }
    return interruption();
  }

  private Optional<String> interruption() {
    if (!KithkynConfig.LlmResidentActions) return Optional.of("Resident actions were disabled.");
    if (!person.isAlive() || person.isRemoved() || person.isNoAi()) return Optional.of("The resident is no longer available.");
    if (!ResidentObligations.mayLeavePost(person)) return Optional.of("An essential or fixed post needs this resident.");
    if (person.getTarget() != null || person.isQuarrelling() || person.isOnFire()
        || person.isInLava() || person.getAirSupply() <= 0) return Optional.of("Immediate danger interrupted the request.");
    if (person.isPassenger() || person.isSleeping() || person.shouldSleepAtNight() && person.level().isNight()
        || person.getHealth() < person.getMaxHealth() / 3.0F) {
      return Optional.of("Rest or recovery interrupted the request.");
    }
    if (person.goalSelector.getAvailableGoals().stream().anyMatch(running -> running.isRunning()
        && running.getPriority() < ResidentActionGoal.PRIORITY
        && (running.getGoal().getFlags().contains(Goal.Flag.MOVE)
            || running.getGoal().getFlags().contains(Goal.Flag.LOOK)))) {
      return Optional.of("An urgent activity needs the resident's attention.");
    }
    if (person.getVillage() != null && person.getVillage().isTraveler(person.getUUID())) {
      return Optional.of("Village travel interrupted the request.");
    }
    return Optional.empty();
  }

  /** Tick only the existing intention. No model call, world scan or path search happens here. */
  public void maintain() {
    state.expire(person.level().getGameTime()).ifPresent(this::record);
    maintainUndertakings();
    if (state.current().isEmpty()) return;
    Order order = state.current().get();
    if (!order.duty().equals(ResidentObligations.snapshot(person))) {
      finish(Phase.INTERRUPTED, "The captured age or assignment changed."); return;
    }
    if (!order.dimension().equals(person.level().dimension().location().toString())) {
      finish(Phase.INTERRUPTED, "The request belongs to another dimension."); return;
    }
    Optional<String> interrupted = interruption();
    if (interrupted.isPresent()) {
      finish(Phase.INTERRUPTED, interrupted.get());
      return;
    }
    if (!(person.level() instanceof ServerLevel level)) return;
    ServerPlayer requester = requester(order).orElse(null);
    if (requester == null || !requester.isAlive() || requester.level() != level) {
      finish(Phase.FAILED, "The requester left or became unavailable.");
    } else if (requester.distanceToSqr(person) > 48 * 48) {
      finish(Phase.FAILED, "The requester moved out of range.");
    }
  }

  /** Existing saved matters, not a retry queue. Expiry is deterministic; one relevant availability change is recorded once. */
  private void maintainUndertakings() {
    if (!(person.level() instanceof ServerLevel level) || !person.isAlive() || person.isRemoved()) return;
    long now = level.getGameTime();
    var key = KithkynAttachments.UNDERTAKINGS.get();
    for (var matter : person.getData(key).allOpen()) {
      var execution = matter.execution();
      if (execution.isEmpty()) {
        UndertakingService.commit(person,
            UndertakingService.archiveNarrative(person.getData(key), matter.id(), level.getDayTime()));
      } else if (execution.get().resident().equals(person.getUUID())
          && (now >= execution.get().expiresAt() || now < execution.get().startedAt())) {
        UndertakingService.commit(person,
            UndertakingService.transition(person.getData(key), person.getUUID(), matter.id(), Phase.EXPIRED,
                "The undertaking's bounded lifetime ended; no success is claimed.", level.getDayTime()));
      }
    }
    if (!KithkynConfig.LlmResidentActions || now < nextReconsider || Math.floorMod(now + person.getId(), 20) != 0
        || current().isPresent() || interruption().isPresent() || person.reactions().movement().isPresent()) return;
    nextReconsider = now + 100;
    var candidates = person.getData(key).allOpen().stream().filter(matter -> matter.execution().filter(execution ->
        execution.resident().equals(person.getUUID()) && execution.lane() == Lane.PLAYER_REQUEST
        && !execution.reconsidered() && (execution.phase() == Phase.FAILED || execution.phase() == Phase.INTERRUPTED)).isPresent()).toList();
    if (candidates.isEmpty()) return;
    var matter = candidates.get(Math.floorMod(reconsiderCursor++, candidates.size()));
    var execution = matter.execution().orElseThrow();
    if (!execution.dimension().equals(level.dimension().location().toString())) return;
    var requester = matter.withWhom().map(level::getEntity).filter(ServerPlayer.class::isInstance).map(ServerPlayer.class::cast).orElse(null);
    if (requester == null || !requester.isAlive() || !com.quzzar.kithkyn.awareness.ResidentAwareness.visible(person, requester)) return;
    int serverTick = level.getServer().getTickCount();
    if (RECONSIDERATION_TICKS.getOrDefault(level.getServer(), Integer.MIN_VALUE) == serverTick) return;
    RECONSIDERATION_TICKS.put(level.getServer(), serverTick);
    boolean available = true;
    if (!execution.action().equals("follow")) {
      Position destination = execution.destination().orElse(null);
      if (!validGround(destination)) return;
      var path = com.quzzar.kithkyn.entities.ai.ResidentWalk.path(person, vector(destination), false);
      available = path != null && path.canReach();
    }
    if (available) {
      var result = UndertakingService.reconsider(person.getData(key), person.getUUID(), matter.id(),
          "The requester and captured destination are available again.", level.getDayTime(), now);
      if (UndertakingService.commit(person, result)) requester.displayClientMessage(Component.literal(
          person.getFullName() + ": That earlier " + execution.action() + " request is available to reconsider. Ask again to start a new attempt."), false);
    }
  }

  public void finish(Phase end, String detail) {
    state.finish(end, detail).ifPresent(this::record);
  }

  public Optional<ServerPlayer> requester(Order order) {
    return person.level() instanceof ServerLevel level
        && level.getEntity(order.requester()) instanceof ServerPlayer player
        ? Optional.of(player) : Optional.empty();
  }

  private void record(Outcome outcome) {
    Kithkyn.LOGGER.info("[resident action] {} {} {}", person.getUUID(), outcome.order().action().wireName(),
        outcome.end());
    requester(outcome.order()).ifPresent(player -> player.displayClientMessage(
        Component.literal(person.getFullName() + ": " + outcome.detail()), true));
  }

  private void reportFailure(ServerPlayer requester, String detail) {
    requester.displayClientMessage(Component.literal(person.getFullName() + ": " + detail), true);
    Kithkyn.LOGGER.info("[resident action] {} rejected: {}", person.getUUID(), detail);
  }

  public boolean validGround(Position destination) {
    if (destination == null || !destination.finite()) return false;
    Vec3 target = vector(destination);
    BlockPos feet = BlockPos.containing(target);
    return person.distanceToSqr(target) <= 48 * 48
        && person.level().hasChunkAt(feet) && person.level().getWorldBorder().isWithinBounds(feet)
        && person.level().getFluidState(feet).isEmpty() && person.level().getFluidState(feet.below()).isEmpty()
        && person.level().getBlockState(feet.below()).isFaceSturdy(person.level(), feet.below(), Direction.UP)
        && person.level().noCollision(person, person.getBoundingBox().move(target.subtract(person.position())));
  }

  public String describe() {
    return describe(Optional.empty());
  }

  private String describe(Optional<java.util.UUID> counterpart) {
    String active = state.current().map(order -> counterpart.isPresent() && !counterpart.get().equals(order.requester())
        ? "Busy with another request; you cannot accept a new movement request from this speaker."
        : "Current movement request: " + order.action().wireName() + " for " + order.requesterName()
            + ", still active and not completed.").orElse("No movement request is active.");
    return active + state.latest().filter(outcome -> counterpart.isEmpty() || counterpart.get().equals(outcome.order().requester()))
        .map(outcome -> " Last movement outcome: " + outcome.end() + ", "
        + outcome.detail()).orElse("");
  }

  public static Position position(Vec3 value) {
    return new Position(value.x, value.y, value.z);
  }

  public static Vec3 vector(Position value) {
    return new Vec3(value.x(), value.y(), value.z());
  }

  private static String coordinates(Position value) {
    return BlockPos.containing(value.x(), value.y(), value.z()).toShortString();
  }
}
