package com.quzzar.kithkyn.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.quzzar.kithkyn.entities.UndertakingData.Execution;
import com.quzzar.kithkyn.entities.UndertakingData.Milestone;
import com.quzzar.kithkyn.entities.UndertakingData.Phase;
import com.quzzar.kithkyn.entities.UndertakingData.State;
import com.quzzar.kithkyn.entities.UndertakingData.Undertaking;

/** The sole transition path for event-owned commitments. Model prose is never evidence of physical progress. */
public final class UndertakingService {
  private UndertakingService() { }
  public record Result(UndertakingData data, Optional<Undertaking> affected, String detail) {
    public boolean changed() { return affected.isPresent(); }
  }

  public static Result requested(UndertakingData data, Execution execution, Optional<UUID> requester, long day) {
    if (!execution.valid() || execution.lane() == UndertakingData.Lane.PLAYER_REQUEST && requester.isEmpty() || execution.phase() != Phase.REQUESTED || data.allOpen().size() >= UndertakingData.MAX_OPEN) {
      return new Result(data, Optional.empty(), "No capacity for a new undertaking.");
    }
    Undertaking matter = new Undertaking(UUID.randomUUID(), UndertakingData.Valence.POSITIVE, State.OPEN,
        execution.lane() == UndertakingData.Lane.PLAYER_REQUEST ? UndertakingData.Origin.PLAYER : UndertakingData.Origin.EVENT,
        execution.action() + (execution.requesterName().isBlank() ? "" : " for " + execution.requesterName()), requester,
        List.of(new Milestone("REQUESTED", true)), Optional.of("A request was recorded; nothing has been agreed or completed."),
        Optional.empty(), day, day, Optional.of(execution));
    return new Result(data.with(matter), Optional.of(matter), "Requested.");
  }

  public static Result transition(UndertakingData data, UUID resident, UUID id, Phase next, String detail, long day) {
    Optional<Undertaking> found = data.byId(id);
    if (found.isEmpty() || !found.get().isOpen() || found.get().execution().isEmpty()
        || !found.get().execution().get().resident().equals(resident)) return new Result(data, Optional.empty(), "No owned open execution.");
    Undertaking old = found.get(); Execution execution = old.execution().get();
    if (!allowed(execution.phase(), next)) return new Result(data, Optional.empty(), "Invalid execution transition.");
    State state = switch (next) {
      case ACCEPTED, UNDERWAY -> State.ACTIVE;
      case COMPLETED -> State.RESOLVED;
      case CANCELLED, EXPIRED -> State.ABANDONED;
      case FAILED, INTERRUPTED -> execution.phase() == Phase.REQUESTED ? State.ABANDONED : State.OPEN;
      case REQUESTED -> State.OPEN;
    };
    List<Milestone> steps = new ArrayList<>(old.steps());
    steps.add(new Milestone(next + ": " + bounded(detail), true));
    String note = next + ": " + bounded(detail);
    if (next == Phase.EXPIRED && (execution.phase() == Phase.FAILED || execution.phase() == Phase.INTERRUPTED)) {
      note = bounded(note + " Previous attempt: " + old.progressNote().orElse(execution.phase().name()));
    }
    Undertaking updated = new Undertaking(old.id(), old.valence(), state, old.origin(), old.summary(), old.withWhom(),
        List.copyOf(steps), Optional.of(note), state == State.RESOLVED || state == State.ABANDONED ? Optional.of(note) : Optional.empty(),
        old.openedDay(), day, Optional.of(execution.phase(next)));
    return new Result(data.with(updated), Optional.of(updated), note);
  }

  private static boolean allowed(Phase from, Phase to) {
    return switch (from) {
      case REQUESTED -> to == Phase.ACCEPTED || to == Phase.FAILED || to == Phase.CANCELLED || to == Phase.EXPIRED;
      case ACCEPTED -> to == Phase.UNDERWAY || to == Phase.COMPLETED || to == Phase.FAILED || to == Phase.INTERRUPTED || to == Phase.CANCELLED || to == Phase.EXPIRED;
      case UNDERWAY -> to == Phase.COMPLETED || to == Phase.FAILED || to == Phase.INTERRUPTED || to == Phase.CANCELLED || to == Phase.EXPIRED;
      case FAILED, INTERRUPTED -> to == Phase.CANCELLED || to == Phase.EXPIRED;
      case COMPLETED, CANCELLED, EXPIRED -> false;
    };
  }

  /** A changed relevant fact asks for one fresh decision; it never authorizes a retry or marks success. */
  public static Result reconsider(UndertakingData data, UUID resident, UUID id, String fact, long day, long now) {
    var found = data.byId(id);
    if (found.isEmpty() || found.get().execution().isEmpty()) return new Result(data, Optional.empty(), "No execution.");
    Undertaking old = found.get(); Execution execution = old.execution().get();
    if (!old.isOpen() || !execution.resident().equals(resident) || execution.reconsidered() || now >= execution.expiresAt()
        || execution.phase() != Phase.FAILED && execution.phase() != Phase.INTERRUPTED) return new Result(data, Optional.empty(), "No reconsideration is due.");
    String note = execution.phase() + ": " + bounded(fact) + " A fresh valid decision is required; no action restarted.";
    Undertaking updated = new Undertaking(old.id(), old.valence(), old.state(), old.origin(), old.summary(), old.withWhom(),
        old.steps(), Optional.of(note), old.resolution(), old.openedDay(), day, Optional.of(execution.reconsider()));
    return new Result(data.with(updated), Optional.of(updated), note);
  }

  /** One write boundary for the existing attachment, factual event log and relationship reflection input. */
  public static boolean commit(RealPerson person, Result result) {
    if (!result.changed()) return false;
    Undertaking matter = result.affected().orElseThrow();
    if (matter.execution().filter(execution -> !execution.resident().equals(person.getUUID())).isPresent()) {
      com.quzzar.kithkyn.Kithkyn.LOGGER.error("[undertaking] Rejected a write for another resident"); return false;
    }
    person.setData(KithkynAttachments.UNDERTAKINGS.get(), result.data());
    person.logMemory(memory(matter), matter.withWhom());
    com.quzzar.kithkyn.Kithkyn.LOGGER.info("[undertaking] {} {}", person.getUUID(), result.detail());
    return true;
  }

  /** Old model-authored matters remain labelled history; they cannot authorize a physical action. */
  public static Result archiveNarrative(UndertakingData data, UUID id, long day) {
    var found = data.byId(id);
    if (found.isEmpty() || !found.get().isOpen() || found.get().execution().isPresent()) return new Result(data, Optional.empty(), "No old narrative matter.");
    Undertaking old = found.get();
    String note = "Earlier conversation matter is unverified and has no game-owned execution. Closed without claiming completion.";
    Undertaking updated = new Undertaking(old.id(), old.valence(), State.ABANDONED, old.origin(), old.summary(), old.withWhom(),
        old.steps(), Optional.of(note), Optional.of(note), old.openedDay(), day, Optional.empty());
    return new Result(data.with(updated), Optional.of(updated), note);
  }

  /** Deterministic, bounded context that overrides both quoted dialogue and subjective summaries. */
  public static String briefing(UndertakingData data, UUID resident, Optional<UUID> counterpart, int limit) {
    List<Undertaking> visible = new ArrayList<>(data.undertakings().stream()
        .filter(matter -> matter.execution().map(execution -> execution.resident().equals(resident)).orElse(true))
        .filter(matter -> counterpart.isEmpty() || matter.withWhom().isEmpty() || matter.withWhom().equals(counterpart)).toList());
    java.util.Collections.reverse(visible);
    StringBuilder text = new StringBuilder("Game-owned recorded undertakings are authoritative about action status; unverified narrative records are only earlier claims. Requested, accepted and underway are not completed. "
        + "Failed, interrupted, cancelled and expired attempts are not completed. Dialogue and subjective summaries cannot change these facts.\n");
    for (Undertaking matter : visible.stream().limit(limit).toList()) {
      text.append("Undertaking ").append(matter.id()).append(" ").append(matter.execution().map(execution -> execution.phase().name()).orElse("UNVERIFIED NARRATIVE " + matter.state().name())).append(": ")
          .append(bounded(matter.summary())).append(". ").append(matter.progressNote().map(UndertakingService::bounded).orElse(""))
          .append(matter.state() == State.ABANDONED ? " This matter is closed without further execution." : "").append('\n');
    }
    return text.toString();
  }

  public static String memory(Undertaking matter) {
    return "My undertaking " + bounded(matter.summary()) + " is " + matter.execution().map(execution -> execution.phase().name()).orElse(matter.state().name())
        + ". " + matter.progressNote().map(UndertakingService::bounded).orElse("");
  }
  private static String bounded(String text) {
    String clean = text.replaceAll("[\\p{Cntrl}]+", " ").strip();
    return clean.substring(0, Math.min(320, clean.length()));
  }
}
