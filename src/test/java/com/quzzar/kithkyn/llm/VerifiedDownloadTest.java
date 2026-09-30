package com.quzzar.kithkyn.llm;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises actual HTTP range transfers and cache corruption without downloading model weights. */
class VerifiedDownloadTest {
  @TempDir Path directory;
  private HttpServer server;
  private final byte[] content = "verified model fixture".getBytes(java.nio.charset.StandardCharsets.UTF_8);

  @AfterEach void stop() { if (server != null) server.stop(0); }

  private String hash() throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
  }

  private URI start(com.sun.net.httpserver.HttpHandler handler) throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/model", handler);
    server.start();
    return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/model");
  }

  @Test void resumesAnInterruptedTransferAndPublishesOnlyTheCompleteFile() throws Exception {
    Path target = directory.resolve("model.gguf");
    Files.write(directory.resolve("model.gguf.partial"), java.util.Arrays.copyOf(content, 5));
    AtomicReference<String> range = new AtomicReference<>();
    URI uri = start(exchange -> {
      range.set(exchange.getRequestHeaders().getFirst("Range"));
      exchange.getResponseHeaders().set("Content-Range", "bytes 5-" + (content.length - 1) + "/" + content.length);
      exchange.sendResponseHeaders(206, content.length - 5);
      try (var body = exchange.getResponseBody()) { body.write(content, 5, content.length - 5); }
    });
    try (HttpClient client = HttpClient.newHttpClient()) { VerifiedDownload.ensure(client, uri, target, content.length, hash()); }
    assertEquals("bytes=5-", range.get());
    assertArrayEquals(content, Files.readAllBytes(target));
    assertFalse(Files.exists(directory.resolve("model.gguf.partial")));
  }

  @Test void restartsWhenTheServerIgnoresTheRangeAndRepairsCorruptCache() throws Exception {
    Path target = directory.resolve("model.gguf");
    Files.writeString(target, "corrupt cache");
    Files.writeString(directory.resolve("model.gguf.partial"), "partial");
    URI uri = start(exchange -> {
      exchange.sendResponseHeaders(200, content.length);
      try (var body = exchange.getResponseBody()) { body.write(content); }
    });
    try (HttpClient client = HttpClient.newHttpClient()) { VerifiedDownload.ensure(client, uri, target, content.length, hash()); }
    assertArrayEquals(content, Files.readAllBytes(target));
  }

  @Test void neverPublishesWrongBytesEvenWhenTheLengthMatches() throws Exception {
    Path target = directory.resolve("model.gguf");
    AtomicInteger calls = new AtomicInteger();
    URI uri = start(exchange -> {
      calls.incrementAndGet();
      exchange.sendResponseHeaders(200, content.length);
      try (var body = exchange.getResponseBody()) { body.write(new byte[content.length]); }
    });
    try (HttpClient client = HttpClient.newHttpClient()) {
      assertThrows(java.io.IOException.class, () -> VerifiedDownload.ensure(client, uri, target, content.length, hash()));
    }
    assertEquals(2, calls.get());
    assertFalse(Files.exists(target));
    assertFalse(Files.exists(directory.resolve("model.gguf.partial")));
  }

  @Test void rejectsAResumeAtTheWrongOffsetWithoutDamagingThePartialFile() throws Exception {
    Path target = directory.resolve("model.gguf");
    Path partial = directory.resolve("model.gguf.partial");
    byte[] first = java.util.Arrays.copyOf(content, 5);
    Files.write(partial, first);
    URI uri = start(exchange -> {
      exchange.getResponseHeaders().set("Content-Range", "bytes 0-" + (content.length - 1) + "/" + content.length);
      exchange.sendResponseHeaders(206, content.length);
      try (var body = exchange.getResponseBody()) { body.write(content); }
    });
    try (HttpClient client = HttpClient.newHttpClient()) {
      assertThrows(java.io.IOException.class, () -> VerifiedDownload.ensure(client, uri, target, content.length, hash()));
    }
    assertArrayEquals(first, Files.readAllBytes(partial));
    assertFalse(Files.exists(target));
  }

  @Test void usesVerifiedCacheAndCompletesAReadyPartialWithoutNetworkAccess() throws Exception {
    Path target = directory.resolve("model.gguf");
    Files.write(directory.resolve("model.gguf.partial"), content);
    try (HttpClient client = HttpClient.newHttpClient()) {
      URI unavailable = URI.create("http://127.0.0.1:1/model");
      VerifiedDownload.ensure(client, unavailable, target, content.length, hash());
      VerifiedDownload.ensure(client, unavailable, target, content.length, hash());
    }
    assertArrayEquals(content, Files.readAllBytes(target));
  }
}
