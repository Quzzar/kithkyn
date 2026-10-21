package com.quzzar.kithkyn.llm;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

/** Single-object boundary for executable model output, including duplicate-key and string validation. */
public final class StrictJson {
  private StrictJson() { }

  public static Optional<JsonObject> object(String raw) {
    if (raw == null || !validStrings(raw)) return Optional.empty();
    try (JsonReader reader = new JsonReader(new StringReader(raw))) {
      reader.setLenient(false);
      JsonElement value = read(reader, 0);
      return value.isJsonObject() && reader.peek() == JsonToken.END_DOCUMENT
          ? Optional.of(value.getAsJsonObject()) : Optional.empty();
    } catch (IOException | RuntimeException invalid) { return Optional.empty(); }
  }

  private static JsonElement read(JsonReader reader, int depth) throws IOException {
    if (depth > 16) throw new IOException("JSON nesting limit exceeded");
    return switch (reader.peek()) {
      case BEGIN_OBJECT -> {
        reader.beginObject();
        JsonObject object = new JsonObject();
        while (reader.hasNext()) {
          String key = reader.nextName();
          if (object.has(key)) throw new IOException("Duplicate JSON field");
          object.add(key, read(reader, depth + 1));
        }
        reader.endObject();
        yield object;
      }
      case BEGIN_ARRAY -> {
        reader.beginArray();
        JsonArray array = new JsonArray();
        while (reader.hasNext()) array.add(read(reader, depth + 1));
        reader.endArray();
        yield array;
      }
      case STRING -> new JsonPrimitive(reader.nextString());
      case NUMBER -> new JsonPrimitive(new BigDecimal(reader.nextString()));
      case BOOLEAN -> new JsonPrimitive(reader.nextBoolean());
      case NULL -> { reader.nextNull(); yield JsonNull.INSTANCE; }
      default -> throw new IOException("Unexpected JSON token");
    };
  }

  /** Gson 2.10's legacy strict reader still tolerates raw controls and escaped apostrophes. */
  private static boolean validStrings(String raw) {
    boolean quoted = false;
    for (int index = 0; index < raw.length(); index++) {
      char character = raw.charAt(index);
      if (!quoted) quoted = character == '"';
      else if (character == '"') quoted = false;
      else if (character < 0x20) return false;
      else if (character == '\\') {
        if (++index >= raw.length()) return false;
        char escape = raw.charAt(index);
        if (escape == 'u') {
          for (int digit = 0; digit < 4; digit++) {
            if (++index >= raw.length() || Character.digit(raw.charAt(index), 16) < 0) return false;
          }
        } else if ("\"\\/bfnrt".indexOf(escape) < 0) return false;
      }
    }
    return !quoted;
  }
}
