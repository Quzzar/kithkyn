package com.quzzar.kithkyn.llm;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;
class StrictJsonTest {
  @Test void acceptsStrictNestedJsonAndRejectsAmbiguousExecutableOutput() {
    assertTrue(StrictJson.object("{\"x\":[true,null,{\"n\":2.5}],\"line\":\"a\\nb\"}").isPresent());
    for (String raw : List.of("{x:1}", "{'x':1}", "{\"x\":1,\"x\":2}", "{\"x\":{\"y\":1,\"y\":2}}", "{\"x\":1} {}", "[]", "{\"x\":NaN}", "{\"x\":\"a\nb\"}", "{\"x\":\"a\\'b\"}", "{\"x\":01}", "{\"x\":1,}", "{\"x\":1} // yes")) {
      assertTrue(StrictJson.object(raw).isEmpty(), raw);
    }
  }
}
