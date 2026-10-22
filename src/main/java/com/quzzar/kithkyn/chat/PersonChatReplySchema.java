package com.quzzar.kithkyn.chat;

import java.util.List;
import java.util.Map;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/** Local generation constraints complement the shared parser and physical inventory checks. */
final class PersonChatReplySchema {
  private PersonChatReplySchema() { }

  static String build(Map<String, Integer> carriedGifts, boolean planning, String speakerLine) {
    JsonObject properties = new JsonObject();
    properties.add("opinion", opinion(speakerLine));
    JsonObject say = typed("string"); say.addProperty("minLength", 1); say.addProperty("maxLength", 2048);
    properties.add("say", say);
    properties.add("fight", typed("boolean")); properties.add("done", typed("boolean"));
    if (planning) {
      JsonObject requestProperties = new JsonObject();
      JsonObject text = typed("string"); text.addProperty("maxLength", 320);
      requestProperties.add("subject", text); requestProperties.add("reason", text.deepCopy());
      properties.add("request", object(requestProperties, "subject"));
    }
    JsonArray alternatives = new JsonArray();
    addOpinionAlternatives(alternatives, properties, false);
    carriedGifts.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
      JsonObject giftProperties = new JsonObject();
      giftProperties.add("opinion", properties.get("opinion").deepCopy());
      JsonObject give = typed("string"); give.addProperty("const", entry.getKey()); giftProperties.add("give", give);
      JsonObject count = typed("integer"); count.addProperty("minimum", 1); count.addProperty("maximum", Math.min(64, entry.getValue()));
      giftProperties.add("give_count", count);
      properties.entrySet().stream().filter(property -> !property.getKey().equals("opinion"))
          .forEach(property -> giftProperties.add(property.getKey(), property.getValue().deepCopy()));
      addOpinionAlternatives(alternatives, giftProperties, true);
    });
    JsonObject schema = new JsonObject(); schema.add("oneOf", alternatives);
    return schema.toString();
  }

  /** llama.cpp emits required fields first; an opinion must be required in its own branch. */
  private static void addOpinionAlternatives(JsonArray alternatives, JsonObject properties, boolean gift) {
    JsonObject neutral = properties.deepCopy(); neutral.remove("opinion");
    JsonObject withoutOpinion = object(neutral, gift ? "give" : "say");
    if (gift) { withoutOpinion.getAsJsonArray("required").add("give_count"); withoutOpinion.getAsJsonArray("required").add("say"); }
    alternatives.add(withoutOpinion);
    JsonObject withOpinion = object(properties.deepCopy(), "opinion");
    if (gift) { withOpinion.getAsJsonArray("required").add("give"); withOpinion.getAsJsonArray("required").add("give_count"); }
    withOpinion.getAsJsonArray("required").add("say");
    alternatives.add(withOpinion);
  }

  private static JsonObject opinion(String speakerLine) {
    JsonArray alternatives = new JsonArray();
    for (boolean positive : List.of(false, true)) {
      JsonObject properties = new JsonObject();
      JsonObject kind = typed("string"); JsonArray kinds = new JsonArray();
      kinds.add(positive ? "kindness" : "insult"); kinds.add(positive ? "apology" : "threat"); kind.add("enum", kinds);
      properties.add("kind", kind);
      JsonObject quote = typed("string"); quote.addProperty("const", speakerLine); properties.add("quote", quote);
      JsonObject delta = typed("integer"); delta.addProperty("minimum", positive ? 1 : -10);
      delta.addProperty("maximum", positive ? 10 : -1); properties.add("delta", delta);
      JsonObject change = object(properties, "kind");
      change.getAsJsonArray("required").add("quote"); change.getAsJsonArray("required").add("delta");
      alternatives.add(change);
    }
    JsonObject result = new JsonObject(); result.add("oneOf", alternatives); return result;
  }

  private static JsonObject typed(String type) { JsonObject value = new JsonObject(); value.addProperty("type", type); return value; }
  private static JsonObject object(JsonObject properties, String requiredField) {
    JsonObject result = typed("object"); result.add("properties", properties); result.addProperty("additionalProperties", false);
    JsonArray required = new JsonArray(); required.add(requiredField); result.add("required", required); return result;
  }
}
