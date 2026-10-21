package com.quzzar.kithkyn.agency;

import java.util.Optional;
import com.quzzar.kithkyn.llm.StrictJson;

/** Strict action boundary: malformed or unsupported tool output cannot become a game action. */
public record ResidentActionSelection(Optional<ResidentActionState.Action> action, Optional<String> error) {
  public static ResidentActionSelection none() {
    return new ResidentActionSelection(Optional.empty(), Optional.empty());
  }

  public static ResidentActionSelection invalid(String detail) {
    return new ResidentActionSelection(Optional.empty(), Optional.of(detail));
  }

  public static ResidentActionSelection parse(String raw) {
    var parsed = StrictJson.object(raw);
    if (parsed.isEmpty()) return invalid("The movement response was not one strict JSON object.");
    var object = parsed.get();
    if (!object.has("action")) return none();
    var selected = object.get("action");
    if (!selected.isJsonPrimitive() || !selected.getAsJsonPrimitive().isString()) {
      return invalid("The action response was not a supported action name.");
    }
    Optional<ResidentActionState.Action> action = ResidentActionState.Action.parse(selected.getAsString());
    return action.isPresent() ? new ResidentActionSelection(action, Optional.empty())
        : invalid("The action response named an unsupported action.");
  }
}
