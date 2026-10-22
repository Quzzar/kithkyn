package com.quzzar.kithkyn.decision;

import static org.junit.jupiter.api.Assertions.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import com.sun.net.httpserver.HttpServer;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class SystemOneProviderTest {
  private static final DecisionProvider.Request REQUEST = new DecisionProvider.Request("witnessed event", "choose",
      List.of(new DecisionProvider.Option("continue", "native activity"), new DecisionProvider.Option("warn", "call out")));
  private static final String GOOD = "{\"answers\":{\"reaction\":{\"type\":\"choice\",\"choice\":\"warn\",\"confidence\":0.8,\"probabilities\":{\"continue\":0.1,\"warn\":0.9}}},\"usage\":{\"input_tokens\":20},\"latency_ms\":1}";

  @Test void validatesTheExactDistributionAndChanceCorrectedConfidence() {
    var selected = assertInstanceOf(DecisionProvider.Selected.class, SystemOneProvider.parse(GOOD, REQUEST));
    assertEquals("warn", selected.key());
    for (String bad : List.of(GOOD.replace("0.8", "0.9"), GOOD.replace("0.9", "0.6"),
        GOOD.replace("\"choice\":\"warn\"", "\"choice\":\"continue\""),
        GOOD.replace("\"warn\":0.9", "\"warn\":0.9,\"teleport\":0"), GOOD.replace("\"confidence\":0.8", "\"confidence\":\"0.8\""),
        GOOD.replace("\"type\":\"choice\"", "\"type\":\"choice\",\"reason\":\"yes\""),
        GOOD.replace("\"warn\":0.9", "\"warn\":0.9,\"warn\":0.9"),
        GOOD.replace("\"answers\":", "\"truncated\":true,\"answers\":"), GOOD + "\n{}", "{}")) {
      assertInstanceOf(DecisionProvider.Failure.class, SystemOneProvider.parse(bad, REQUEST), bad);
    }
  }

  @Test void postsTheIndependentContractWithOptionalBearerAuthAndBoundsTheBody() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    var authorization = new AtomicReference<String>(); var body = new AtomicReference<String>();
    server.createContext("/v1/systemone", exchange -> {
      authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
      body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
      byte[] response = (body.get().contains("oversized") ? "x".repeat(70000) : GOOD).getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(200, response.length); exchange.getResponseBody().write(response); exchange.close();
    }); server.start();
    try (var provider = new SystemOneProvider("http://127.0.0.1:" + server.getAddress().getPort(), "kev-latest", "test-only")) {
      assertInstanceOf(DecisionProvider.Selected.class, provider.choose(REQUEST).get(8, TimeUnit.SECONDS));
      assertEquals("Bearer test-only", authorization.get());
      var payload = JsonParser.parseString(body.get()).getAsJsonObject();
      assertFalse(payload.has("messages")); assertEquals("witnessed event", payload.get("state").getAsString());
      assertEquals(List.of("continue", "warn"), payload.getAsJsonObject("questions").getAsJsonObject("reaction").getAsJsonObject("criteria").keySet().stream().toList());
      assertInstanceOf(DecisionProvider.Failure.class, provider.choose(new DecisionProvider.Request("oversized", "choose", REQUEST.options())).get(8, TimeUnit.SECONDS));
    } finally { server.stop(0); }
  }

  @Test void cancelsAStalledBodyAndRejectsConcurrentOrStoppedRequests() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    var headers = new CompletableFuture<Void>(); var release = new CompletableFuture<Void>();
    server.createContext("/v1/systemone", exchange -> {
      exchange.sendResponseHeaders(200, 10000); exchange.getResponseBody().write('{'); exchange.getResponseBody().flush();
      headers.complete(null);
      try { release.get(10, TimeUnit.SECONDS); } catch (Exception ignored) { }
      exchange.close();
    }); server.start();
    var provider = new SystemOneProvider("http://127.0.0.1:" + server.getAddress().getPort(), "kev-latest", "");
    try {
      var pending = provider.choose(REQUEST); headers.get(3, TimeUnit.SECONDS);
      assertEquals(DecisionProvider.FailureKind.BUSY, ((DecisionProvider.Failure) provider.choose(REQUEST).join()).error());
      assertEquals(DecisionProvider.FailureKind.TIMEOUT, ((DecisionProvider.Failure) pending.get(7, TimeUnit.SECONDS)).error());
      provider.close();
      assertEquals(DecisionProvider.FailureKind.STOPPED, ((DecisionProvider.Failure) provider.choose(REQUEST).join()).error());
    } finally { release.complete(null); provider.close(); server.stop(0); }
  }

  @Test void parsesAllActualPinnedKevReplayResponses() throws Exception {
    var lines = java.nio.file.Files.readAllLines(java.nio.file.Path.of(System.getProperty("kithkyn.reactionReplay")));
    assertEquals(48, lines.size());
    for (String line : lines) {
      var row = JsonParser.parseString(line).getAsJsonObject();
      var payload = JsonParser.parseString(row.get("request_body").getAsString()).getAsJsonObject();
      var question = payload.getAsJsonObject("questions").getAsJsonObject("reaction");
      var options = question.getAsJsonObject("criteria").entrySet().stream()
          .map(entry -> new DecisionProvider.Option(entry.getKey(), entry.getValue().getAsString())).toList();
      var request = new DecisionProvider.Request(payload.get("state").getAsString(), question.get("instructions").getAsString(), options);
      assertEquals(200, row.get("http_status").getAsInt());
      assertInstanceOf(DecisionProvider.Selected.class, SystemOneProvider.parse(row.get("response_body").getAsString(), request), row.get("id").getAsString());
    }
  }

  @Test void rejectsInvalidEndpointAndDuplicateOptions() {
    for (String url : List.of("file:///tmp/model", "https://user:secret@example.test", "http://localhost?key=x")) {
      assertThrows(IllegalArgumentException.class, () -> new SystemOneProvider(url, "kev", ""));
    }
    assertThrows(IllegalArgumentException.class, () -> new DecisionProvider.Request("state", "choose", List.of(REQUEST.options().getFirst(), REQUEST.options().getFirst())));
  }
}
