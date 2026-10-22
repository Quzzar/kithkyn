package com.quzzar.kithkyn.decision;

import java.util.List;
import java.util.Optional;

/** Freshness and valid-choice boundary between a decision and an optional game reaction. */
public final class ReactionState {
  public enum Action {
    CONTINUE, WARN, RETREAT, SEEK_HELP, INVESTIGATE, DEFEND;
    public String key() { return name().toLowerCase(java.util.Locale.ROOT); }
    public static Optional<Action> parse(String key) {
      return java.util.Arrays.stream(values()).filter(action -> action.key().equals(key)).findFirst();
    }
  }
  public enum Status { DISABLED, IDLE, PENDING, DEFERRED, FAILED, STALE, STARTED, COMPLETED, INTERRUPTED }
  public record Offer(long revision, long expiresAt, List<Action> actions) {
    public Offer { actions = List.copyOf(actions); }
  }
  public sealed interface Verdict permits Accepted, Rejected { }
  public record Accepted(Action action) implements Verdict { }
  public record Rejected(Status status, String detail) implements Verdict { }
  private long revision;

  public Offer offer(long expiresAt, List<Action> actions) { return new Offer(++revision, expiresAt, actions); }
  public void invalidate() { revision++; }

  public Verdict resolve(Offer offer, DecisionProvider.Result result, long now) {
    if (offer.revision() != revision || now >= offer.expiresAt()) return new Rejected(Status.STALE, "The reaction offer changed or expired.");
    revision++;
    if (result instanceof DecisionProvider.Failure failure) return new Rejected(Status.FAILED, failure.error() + ": " + failure.detail());
    if (!(result instanceof DecisionProvider.Selected selected)) return new Rejected(Status.FAILED, "No decision result.");
    Optional<Action> action = Action.parse(selected.key());
    if (action.isEmpty() || !offer.actions().contains(action.get()) || !Double.isFinite(selected.confidence())
        || selected.confidence() < 0 || selected.confidence() > 1) return new Rejected(Status.FAILED, "The decision did not select an offered action with valid confidence.");
    return new Accepted(action.get());
  }
}
