package com.quzzar.kithkyn.chat;

import java.util.List;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/** Local generation constraints complement the shared parser and physical inventory checks. */
final class PersonChatReplySchema {
  private PersonChatReplySchema() { }

  static String build(List<String> carriedItemIds, boolean planning) {
    JsonObject properties = new JsonObject();
    JsonObject say = typed("string"); say.addProperty("minLength", 1); say.addProperty("maxLength", 2048);
    properties.add("say", say);
    if (!carriedItemIds.isEmpty()) {
      JsonObject give = typed("string"); JsonArray ids = new JsonArray();
      carriedItemIds.stream().distinct().sorted().forEach(ids::add); give.add("enum", ids); properties.add("give", give);
      JsonObject count = typed("integer"); count.addProperty("minimum", 1); count.addProperty("maximum", 64);
      properties.add("give_count", count);
    }
    JsonObject opinion = typed("integer"); opinion.addProperty("minimum", -10); opinion.addProperty("maximum", 10);
    properties.add("opinion", opinion); properties.add("fight", typed("boolean")); properties.add("done", typed("boolean"));
    if (planning) {
      JsonObject requestProperties = new JsonObject();
      JsonObject text = typed("string"); text.addProperty("maxLength", 320);
      requestProperties.add("subject", text); requestProperties.add("reason", text.deepCopy());
      properties.add("request", object(requestProperties, "subject"));
    }
    return object(properties, "say").toString();
  }

  private static JsonObject typed(String type) { JsonObject value = new JsonObject(); value.addProperty("type", type); return value; }
  private static JsonObject object(JsonObject properties, String requiredField) {
    JsonObject result = typed("object"); result.add("properties", properties); result.addProperty("additionalProperties", false);
    JsonArray required = new JsonArray(); required.add(requiredField); result.add("required", required); return result;
  }
}
