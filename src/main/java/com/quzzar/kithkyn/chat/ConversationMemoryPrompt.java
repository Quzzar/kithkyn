package com.quzzar.kithkyn.chat;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.quzzar.kithkyn.llm.StrictJson;

/** The model selects source lines; it cannot write new autobiographical facts into memory. */
final class ConversationMemoryPrompt {
  private static final String[] VOLATILE_WORLD_STATE = {
      "build", "project", "saving", "save up", "short of", "material", "bed", "workplace",
      "job", "population", "stores", "finish"
  };

  record Prompt(String system, String user, List<String> candidates, String localJsonSchema) {
    /** Only exact, unique, in-range references can become saved memory. */
    Optional<List<String>> select(String raw) {
      var parsed = StrictJson.object(raw);
      if (parsed.isEmpty() || !parsed.get().keySet().equals(java.util.Set.of("keep"))) return Optional.empty();
      try {
        var ids = parsed.get().getAsJsonArray("keep");
        if (ids.size() > ChatSummaryData.MAX_QUOTES) return Optional.empty();
        var selected = new LinkedHashSet<Integer>();
        for (var value : ids) {
          if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) return Optional.empty();
          int id = value.getAsBigDecimal().intValueExact();
          if (id < 1 || id > candidates.size() || !selected.add(id)) return Optional.empty();
        }
        return Optional.of(selected.stream().sorted().map(id -> candidates.get(id - 1)).toList());
      } catch (RuntimeException invalid) { return Optional.empty(); }
    }
  }

  private ConversationMemoryPrompt() { }

  static Prompt build(String personName, String counterpartName, List<String> earlierQuotes,
      List<String> counterpartLines) {
    var unique = new LinkedHashSet<String>();
    unique.addAll(earlierQuotes);
    counterpartLines.stream().filter(line -> !line.isBlank() && line.length() <= 2048).forEach(unique::add);
    List<String> candidates = new ArrayList<>(unique);
    candidates = List.copyOf(candidates.subList(Math.max(0, candidates.size() - 16), candidates.size()));
    String system = "Select up to four important lines that " + personName + " should remember "
        + counterpartName + " saying. These are conversation quotes, not verified events. Keep significant "
        + "questions, personal disclosures, requests or promises. Preserve questions as questions. "
        + "Select only the numbered source lines; do not answer them, paraphrase them or add feelings. "
        + "Return only JSON: {\"keep\":[<line numbers>]}. An empty list is allowed for trivial small talk.";
    StringBuilder user = new StringBuilder("Source lines, oldest to newest:\n");
    for (int i = 0; i < candidates.size(); i++)
      user.append(i + 1).append(". ").append(new Gson().toJson(candidates.get(i))).append('\n');
    JsonObject schema = new JsonObject(); schema.addProperty("type", "object");
    schema.addProperty("additionalProperties", false);
    JsonObject keep = new JsonObject(); keep.addProperty("type", "array"); keep.addProperty("maxItems", ChatSummaryData.MAX_QUOTES);
    JsonObject item = new JsonObject(); item.addProperty("type", "integer"); item.addProperty("minimum", 1);
    item.addProperty("maximum", Math.max(1, candidates.size())); keep.add("items", item);
    JsonObject properties = new JsonObject(); properties.add("keep", keep); schema.add("properties", properties);
    JsonArray required = new JsonArray(); required.add("keep"); schema.add("required", required);
    return new Prompt(system, user.toString(), candidates, schema.toString());
  }

  /** Filters unverified historical prose and live answer timelines, not attributed source quotes. */
  static String withoutVolatileWorldState(String memory) {
    if (memory == null || memory.isBlank()) return "";
    StringBuilder kept = new StringBuilder();
    for (String sentence : memory.strip().split("(?<=[.!?])\\s+")) {
      String lower = sentence.toLowerCase(java.util.Locale.ROOT);
      boolean volatileFact = java.util.Arrays.stream(VOLATILE_WORLD_STATE).anyMatch(lower::contains);
      if (!volatileFact) {
        if (kept.length() > 0) kept.append(' ');
        kept.append(sentence.strip());
      }
    }
    return kept.toString();
  }
}
