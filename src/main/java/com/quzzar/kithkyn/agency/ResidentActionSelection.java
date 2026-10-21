package com.quzzar.kithkyn.agency;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

/** Strict action boundary: malformed or unsupported tool output cannot become a game action. */
public record ResidentActionSelection(Optional<ResidentActionState.Action> action, Optional<String> error) {
  public static ResidentActionSelection none() {
    return new ResidentActionSelection(Optional.empty(), Optional.empty());
  }

  public static ResidentActionSelection invalid(String detail) {
    return new ResidentActionSelection(Optional.empty(), Optional.of(detail));
  }

  public static ResidentActionSelection parse(String raw) {
    String selected = null;
    Set<String> fields = new HashSet<>();
    try (JsonReader reader = new JsonReader(new StringReader(raw))) {
      reader.setLenient(false);
      reader.beginObject();
      while (reader.hasNext()) {
        String field = reader.nextName();
        if (!fields.add(field)) return invalid("The movement response contained duplicate fields.");
        if (field.equals("action")) {
          if (reader.peek() != JsonToken.STRING) return invalid("The action response was not a supported action name.");
          selected = reader.nextString();
        } else reader.skipValue();
      }
      reader.endObject();
      if (reader.peek() != JsonToken.END_DOCUMENT) return invalid("The movement response was not a single JSON object.");
    } catch (IOException | IllegalStateException failure) {
      return invalid("The movement response was not valid JSON.");
    }
    if (selected == null) return none();
    Optional<ResidentActionState.Action> action = ResidentActionState.Action.parse(selected);
    return action.isPresent() ? new ResidentActionSelection(action, Optional.empty())
        : invalid("The action response named an unsupported action.");
  }
}
