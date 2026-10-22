package com.quzzar.kithkyn.chat;

import java.util.List;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/** Local generation constraints complement the shared parser and physical inventory checks. */
final class PersonChatReplySchema {
  private PersonChatReplySchema() { }

  static String build(List<String> carriedItemIds, boolean planning, String speakerLine) {
    JsonObject properties = new JsonObject();
    JsonObject say = typed("string"); say.addProperty("minLength", 1); say.addProperty("maxLength", 2048);
    properties.add("say", say);
    properties.add("opinion", opinion(speakerLine)); properties.add("fight", typed("boolean")); properties.add("done", typed("boolean"));
    if (planning) {
      JsonObject requestProperties = new JsonObject();
      JsonObject text = typed("string"); text.addProperty("maxLength", 320);
      requestProperties.add("subject", text); requestProperties.add("reason", text.deepCopy());
      properties.add("request", object(requestProperties, "subject"));
    }
    JsonObject withoutGift = object(properties, "say");
    if (carriedItemIds.isEmpty()) return withoutGift.toString();
    JsonObject giftProperties = properties.deepCopy();
    JsonObject give = typed("string"); JsonArray ids = new JsonArray();
    carriedItemIds.stream().distinct().sorted().forEach(ids::add); give.add("enum", ids); giftProperties.add("give", give);
    JsonObject count = typed("integer"); count.addProperty("minimum", 1); count.addProperty("maximum", 64);
    giftProperties.add("give_count", count);
    JsonObject withGift = object(giftProperties, "say");
    withGift.getAsJsonArray("required").add("give");
    JsonArray alternatives = new JsonArray(); alternatives.add(withoutGift); alternatives.add(withGift);
    JsonObject schema = new JsonObject(); schema.add("oneOf", alternatives);
    return schema.toString();
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
