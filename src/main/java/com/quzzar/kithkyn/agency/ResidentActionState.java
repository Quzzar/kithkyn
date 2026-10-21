package com.quzzar.kithkyn.agency;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import com.quzzar.kithkyn.entities.UndertakingData;
import com.quzzar.kithkyn.entities.UndertakingData.Execution;
import com.quzzar.kithkyn.entities.UndertakingData.Duty;
import com.quzzar.kithkyn.entities.UndertakingData.Lane;
import com.quzzar.kithkyn.entities.UndertakingData.Phase;
import com.quzzar.kithkyn.entities.UndertakingData.Position;
import com.quzzar.kithkyn.entities.UndertakingData.Undertaking;
import com.quzzar.kithkyn.entities.UndertakingService;

/** Fresh offers and execution views backed exclusively by the resident's existing saved undertakings. */
public final class ResidentActionState {
  public static final long OFFER_LIFETIME = 600L;
  public static final long ORDER_LIFETIME = 6000L;
  public enum Action {
    FOLLOW("follow"), WAIT("wait"), GO_TO_POINT("go_to_point"), RESUME("resume");
    private final String wireName;
    Action(String wireName) { this.wireName = wireName; }
    public String wireName() { return wireName; }
    public static Optional<Action> parse(String value) {
      for (Action action : values()) if (action.wireName.equals(value)) return Optional.of(action);
      return Optional.empty();
    }
  }
  public record Offer(long revision, UUID requester, String requesterName, String dimension,
      Position pointedAt, Position standingAt, long expiresAt) { }
  public record Order(long revision, Action action, UUID requester, String requesterName,
      String dimension, Position destination, long expiresAt, Duty duty) { }
  public record Outcome(Order order, Phase end, String detail) { }
  public record Acceptance(boolean accepted, String detail, Optional<Outcome> replaced) { }

  private final Supplier<UUID> resident;
  private final Supplier<UndertakingData> read;
  private final Consumer<UndertakingService.Result> commit;
  private final LongSupplier day;
  private final Supplier<Duty> duty;
  private long revision;

  public ResidentActionState(Supplier<UUID> resident, Supplier<UndertakingData> read, Consumer<UndertakingService.Result> commit, LongSupplier day, Supplier<Duty> duty) {
    this.resident = resident; this.read = read; this.commit = commit; this.day = day; this.duty = duty;
  }
  public Offer offer(UUID requester, String name, String dimension, Position point, Position standing, long now) {
    return new Offer(++revision, requester, name, dimension, point, standing, now + OFFER_LIFETIME);
  }
  public Optional<String> rejection(Offer offer, Action action, long now) {
    if (offer.revision() != revision || now >= offer.expiresAt()) return Optional.of("The request is out of date.");
    if (current().filter(order -> !order.requester().equals(offer.requester())).isPresent()) return Optional.of("Already carrying out someone else's request.");
    if (action == Action.GO_TO_POINT && (offer.pointedAt() == null || !offer.pointedAt().finite())) return Optional.of("No valid pointed location was offered.");
    if (action == Action.WAIT && (offer.standingAt() == null || !offer.standingAt().finite())) return Optional.of("No valid waiting location was offered.");
    return Optional.empty();
  }
  public Acceptance accept(Offer offer, Action action, long now) {
    Optional<String> rejection = rejection(offer, action, now);
    if (rejection.isPresent()) return new Acceptance(false, rejection.get(), Optional.empty());
    Optional<Outcome> replaced = finish(Phase.CANCELLED, "The requester changed the instruction.");
    for (Undertaking old : matters().filter(matter -> matter.isOpen() && matter.withWhom().filter(offer.requester()::equals).isPresent()).toList()) {
      update(UndertakingService.transition(read.get(), resident.get(), old.id(), Phase.CANCELLED, "Replaced by a fresh instruction from its requester.", day.getAsLong()));
    }
    Optional<Undertaking> requested = request(offer, action, now);
    if (requested.isEmpty()) return new Acceptance(false, "The resident cannot track another open undertaking.", replaced);
    UUID id = requested.get().id();
    update(UndertakingService.transition(read.get(), resident.get(), id, Phase.ACCEPTED, "Agreed; execution has not begun.", day.getAsLong()));
    if (action == Action.RESUME) update(UndertakingService.transition(read.get(), resident.get(), id, Phase.COMPLETED, "Released from the request.", day.getAsLong()));
    revision++;
    return new Acceptance(true, switch (action) {
      case FOLLOW -> "Agreed to follow you."; case WAIT -> "Agreed to wait here.";
      case GO_TO_POINT -> "Agreed to try the pointed route."; case RESUME -> "Returning to ordinary activity.";
    }, replaced);
  }
  private Optional<Undertaking> request(Offer offer, Action action, long now) {
    Position destination = action == Action.GO_TO_POINT ? offer.pointedAt() : offer.standingAt();
    var execution = new Execution(resident.get(), Lane.PLAYER_REQUEST, action.wireName(), Phase.REQUESTED,
        offer.requesterName(), offer.dimension(), destination != null && destination.finite() ? Optional.of(destination) : Optional.empty(),
        action == Action.FOLLOW ? Optional.of(offer.requester()) : Optional.empty(), matters().mapToLong(matter -> matter.execution().orElseThrow().revision()).max().orElse(0) + 1, now, now + ORDER_LIFETIME, false, duty.get());
    var result = UndertakingService.requested(read.get(), execution, Optional.of(offer.requester()), day.getAsLong());
    update(result); return result.affected();
  }
  public void rejected(Offer offer, Action action, String reason, long now) {
    request(offer, action, now).ifPresent(matter -> update(UndertakingService.transition(read.get(), resident.get(),
        matter.id(), Phase.FAILED, "Not accepted: " + reason, day.getAsLong())));
  }
  public Optional<Order> current() {
    return currentMatter().map(ResidentActionState::order);
  }
  public Optional<Outcome> latest() {
    return matters().filter(matter -> !matter.execution().orElseThrow().phase().executing()
        && matter.execution().orElseThrow().phase() != Phase.REQUESTED)
        .reduce((first, second) -> second).map(matter -> new Outcome(order(matter), matter.execution().orElseThrow().phase(),
            matter.progressNote().orElse("No outcome detail.")));
  }
  public Optional<Outcome> finish(Phase end, String detail) {
    if (end == Phase.REQUESTED || end.executing()) return Optional.empty();
    Optional<Undertaking> current = currentMatter();
    if (current.isEmpty()) return Optional.empty();
    var result = UndertakingService.transition(read.get(), resident.get(), current.get().id(), end, detail, day.getAsLong());
    update(result); revision++;
    return result.affected().map(matter -> new Outcome(order(matter), end, detail));
  }
  public void underway(Order expected) {
    currentMatter().filter(matter -> matter.execution().orElseThrow().revision() == expected.revision()
        && matter.execution().orElseThrow().phase() == Phase.ACCEPTED)
        .ifPresent(matter -> update(UndertakingService.transition(read.get(), resident.get(), matter.id(), Phase.UNDERWAY,
            "Native navigation started; completion has not happened.", day.getAsLong())));
  }
  public Optional<Outcome> expire(long now) {
    return current().filter(order -> now >= order.expiresAt()).flatMap(order -> finish(Phase.EXPIRED, "The agreed time ran out."));
  }
  private Optional<Undertaking> currentMatter() { return matters().filter(matter -> matter.execution().orElseThrow().phase().executing()).findFirst(); }
  private java.util.stream.Stream<Undertaking> matters() {
    return read.get().undertakings().stream().filter(matter -> matter.execution().filter(execution -> execution.resident().equals(resident.get())
        && execution.lane() == Lane.PLAYER_REQUEST).isPresent());
  }
  private static Order order(Undertaking matter) {
    Execution execution = matter.execution().orElseThrow();
    return new Order(execution.revision(), Action.parse(execution.action()).orElseThrow(), matter.withWhom().orElseThrow(),
        execution.requesterName(), execution.dimension(), execution.destination().orElse(null), execution.expiresAt(), execution.duty());
  }
  private void update(UndertakingService.Result result) {
    if (result.changed()) commit.accept(result);
  }
}
