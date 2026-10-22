package com.quzzar.kithkyn.decision;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/** Independent bounded-choice capability. It cannot generate dialogue, arguments, coordinates or reasons. */
public interface DecisionProvider extends AutoCloseable {
  record Option(String key, String description) {
    public Option {
      if (key == null || !key.matches("[a-z][a-z0-9_]{0,31}") || description == null || description.isBlank()) {
        throw new IllegalArgumentException("Invalid decision option");
      }
    }
  }

  record Request(String state, String instructions, List<Option> options) {
    public Request {
      Objects.requireNonNull(state);
      Objects.requireNonNull(instructions);
      options = List.copyOf(options);
      if (state.isBlank() || state.length() > 16000 || instructions.isBlank() || options.size() < 2 || options.size() > 6
          || options.stream().map(Option::key).distinct().count() != options.size()) {
        throw new IllegalArgumentException("Invalid bounded decision request");
      }
    }
  }

  sealed interface Result permits Selected, Failure { }
  record Selected(String key, double confidence, Map<String, Double> probabilities) implements Result {
    public Selected { probabilities = Map.copyOf(probabilities); }
  }
  enum FailureKind { CONFIGURATION, TRANSPORT, INVALID_RESPONSE, TIMEOUT, BUSY, STOPPED }
  record Failure(FailureKind error, String detail) implements Result { }

  CompletableFuture<Result> choose(Request request);
  @Override void close();
}
