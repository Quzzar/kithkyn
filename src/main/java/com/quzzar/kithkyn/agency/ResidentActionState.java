package com.quzzar.kithkyn.agency;

import java.util.Optional;
import java.util.UUID;

/** Session-scoped resident intentions, with freshness and ownership checked before execution. */
public final class ResidentActionState {
  public static final long OFFER_LIFETIME = 600L;
  public static final long ORDER_LIFETIME = 6000L;

  /** The model selects an implemented verb; it never supplies coordinates or executable code. */
  public enum Action {
    FOLLOW("follow"), WAIT("wait"), GO_TO_POINT("go_to_point"), RESUME("resume");

    private final String wireName;

    Action(String wireName) {
      this.wireName = wireName;
    }

    public String wireName() {
      return wireName;
    }

    public static Optional<Action> parse(String value) {
      for (Action action : values()) {
        if (action.wireName.equals(value)) return Optional.of(action);
      }
      return Optional.empty();
    }
  }

  public enum End { COMPLETED, FAILED, INTERRUPTED, CANCELLED, EXPIRED }

  public record Position(double x, double y, double z) {
    public boolean finite() {
      return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z);
    }
  }

  /** A copy of the reference when the question was asked, not when inference eventually replies. */
  public record Offer(long revision, UUID requester, String requesterName, String dimension,
      Position pointedAt, Position standingAt, long expiresAt) { }

  public record Order(long revision, Action action, UUID requester, String requesterName,
      String dimension, Position destination, long expiresAt) { }

  public record Outcome(Order order, End end, String detail) { }

  public record Acceptance(boolean accepted, String detail, Optional<Outcome> replaced) { }

  private long revision;
  private Order current;
  private Outcome latest;

  public Offer offer(UUID requester, String requesterName, String dimension, Position pointedAt,
      Position standingAt, long now) {
    return new Offer(++revision, requester, requesterName, dimension, pointedAt, standingAt,
        now + OFFER_LIFETIME);
  }

  /** Every new conversation offer or terminal result makes an older model reply unusable. */
  public Optional<String> rejection(Offer offer, Action action, long now) {
    if (offer.revision() != revision || now >= offer.expiresAt()) return Optional.of("The request is out of date.");
    if (current != null && !current.requester().equals(offer.requester())) {
      return Optional.of("Already carrying out someone else's request.");
    }
    if (action == Action.GO_TO_POINT && (offer.pointedAt() == null || !offer.pointedAt().finite())) {
      return Optional.of("No valid pointed location was offered.");
    }
    if (action == Action.WAIT && (offer.standingAt() == null || !offer.standingAt().finite())) {
      return Optional.of("No valid waiting location was offered.");
    }
    return Optional.empty();
  }

  public Acceptance accept(Offer offer, Action action, long now) {
    Optional<String> rejected = rejection(offer, action, now);
    if (rejected.isPresent()) return new Acceptance(false, rejected.get(), Optional.empty());
    Optional<Outcome> replaced = current == null ? Optional.empty()
        : finish(End.CANCELLED, "The requester changed the instruction.");
    if (action == Action.RESUME) {
      latest = new Outcome(new Order(++revision, action, offer.requester(), offer.requesterName(),
          offer.dimension(), null, now), End.COMPLETED, "Released from the request.");
      return new Acceptance(true, "Returning to ordinary activity.", replaced);
    }
    Position destination = action == Action.GO_TO_POINT ? offer.pointedAt() : offer.standingAt();
    current = new Order(++revision, action, offer.requester(), offer.requesterName(), offer.dimension(),
        destination, now + ORDER_LIFETIME);
    return new Acceptance(true, switch (action) {
      case FOLLOW -> "Following you.";
      case WAIT -> "Waiting here.";
      case GO_TO_POINT -> "Walking to the pointed location.";
      case RESUME -> throw new IllegalStateException("Resume is completed above");
    }, replaced);
  }

  public Optional<Order> current() {
    return Optional.ofNullable(current);
  }

  public Optional<Outcome> latest() {
    return Optional.ofNullable(latest);
  }

  public Optional<Outcome> finish(End end, String detail) {
    if (current == null) return Optional.empty();
    latest = new Outcome(current, end, detail);
    current = null;
    revision++;
    return Optional.of(latest);
  }

  public Optional<Outcome> expire(long now) {
    return current != null && now >= current.expiresAt()
        ? finish(End.EXPIRED, "The agreed time ran out.") : Optional.empty();
  }
}
