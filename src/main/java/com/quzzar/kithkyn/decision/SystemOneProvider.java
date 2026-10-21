package com.quzzar.kithkyn.decision;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.JsonObject;
import com.quzzar.kithkyn.llm.StrictJson;

/** Kev/TypeSafe System One HTTP contract, with one in-flight request and no retries or provider substitution. */
public final class SystemOneProvider implements DecisionProvider {
  private static final Duration TIMEOUT = Duration.ofSeconds(5);
  private final HttpClient http;
  private final URI endpoint;
  private final String model;
  private final String apiKey;
  private final AtomicBoolean busy = new AtomicBoolean();
  private volatile boolean stopped;
  private volatile CompletableFuture<?> pending;

  public SystemOneProvider(String baseUrl, String model, String apiKey) {
    URI base = URI.create(baseUrl);
    if (!Set.of("http", "https").contains(base.getScheme()) || base.getHost() == null || base.getRawQuery() != null
        || base.getRawFragment() != null || base.getRawUserInfo() != null || model == null || model.isBlank()
        || apiKey == null) throw new IllegalArgumentException("Invalid decision endpoint configuration");
    this.endpoint = URI.create(baseUrl.replaceAll("/+$", "") + "/v1/systemone");
    this.model = model;
    this.apiKey = apiKey;
    this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
  }

  /** Public for replay capture; the endpoint never receives executable arguments or a generative chat prompt. */
  public static JsonObject payload(Request request, String model) {
    JsonObject root = new JsonObject();
    root.addProperty("model", model);
    root.addProperty("state", request.state());
    JsonObject question = new JsonObject();
    question.addProperty("type", "choice");
    question.addProperty("instructions", request.instructions());
    JsonObject criteria = new JsonObject();
    request.options().forEach(option -> criteria.addProperty(option.key(), option.description()));
    question.add("criteria", criteria);
    JsonObject questions = new JsonObject();
    questions.add("reaction", question);
    root.add("questions", questions);
    return root;
  }

  @Override public CompletableFuture<Result> choose(Request request) {
    if (stopped) return CompletableFuture.completedFuture(new Failure(FailureKind.STOPPED, "Decision provider stopped."));
    if (!busy.compareAndSet(false, true)) return CompletableFuture.completedFuture(new Failure(FailureKind.BUSY, "Decision provider already has a request."));
    try {
      var builder = HttpRequest.newBuilder(endpoint).timeout(TIMEOUT).header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(payload(request, model).toString()));
      if (!apiKey.isBlank()) builder.header("Authorization", "Bearer " + apiKey);
      var sent = http.sendAsync(builder.build(), ignored -> new BoundedBody());
      pending = sent;
      var timedOut = new AtomicBoolean();
      CompletableFuture.delayedExecutor(TIMEOUT.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS).execute(() -> {
        if (!sent.isDone()) { timedOut.set(true); sent.cancel(true); }
      });
      return sent.<Result>handle((response, failure) -> {
        if (stopped) return new Failure(FailureKind.STOPPED, "Decision provider stopped.");
        if (failure != null) {
          Throwable cause = failure;
          while (cause.getCause() != null) cause = cause.getCause();
          return new Failure(timedOut.get() || cause instanceof java.net.http.HttpTimeoutException ? FailureKind.TIMEOUT : FailureKind.TRANSPORT,
              "Decision request failed: " + cause.getClass().getSimpleName());
        }
        if (response.statusCode() != 200) return new Failure(FailureKind.TRANSPORT, "Decision endpoint returned HTTP " + response.statusCode());
        return parse(new String(response.body(), StandardCharsets.UTF_8), request);
      }).whenComplete((ignored, failure) -> { pending = null; busy.set(false); });
    } catch (RuntimeException invalid) {
      busy.set(false);
      return CompletableFuture.completedFuture(new Failure(FailureKind.CONFIGURATION, "Decision request configuration is invalid."));
    }
  }

  public static Result parse(String raw, Request request) {
    try {
      JsonObject root = StrictJson.object(raw).orElseThrow();
      if (root.has("truncated") && (!root.get("truncated").isJsonPrimitive()
          || !root.getAsJsonPrimitive("truncated").isBoolean() || root.get("truncated").getAsBoolean())) throw new IllegalArgumentException();
      JsonObject answers = root.getAsJsonObject("answers");
      if (!answers.keySet().equals(Set.of("reaction"))) throw new IllegalArgumentException();
      JsonObject result = answers.getAsJsonObject("reaction");
      if (!result.keySet().equals(Set.of("type", "choice", "confidence", "probabilities"))
          || !string(result, "type").equals("choice")) throw new IllegalArgumentException();
      String choice = string(result, "choice");
      double confidence = probability(result, "confidence");
      JsonObject distribution = result.getAsJsonObject("probabilities");
      Set<String> offered = request.options().stream().map(Option::key).collect(java.util.stream.Collectors.toSet());
      if (!distribution.keySet().equals(offered) || !offered.contains(choice)) throw new IllegalArgumentException();
      var probabilities = new LinkedHashMap<String, Double>();
      for (String key : offered) probabilities.put(key, probability(distribution, key));
      double sum = probabilities.values().stream().mapToDouble(Double::doubleValue).sum();
      double maximum = probabilities.values().stream().mapToDouble(Double::doubleValue).max().orElseThrow();
      double expectedConfidence = (maximum / sum - 1.0 / offered.size()) / (1.0 - 1.0 / offered.size());
      if (Math.abs(sum - 1.0) > 0.002 || probabilities.get(choice) < maximum - 0.0001
          || Math.abs(confidence - expectedConfidence) > 0.002) throw new IllegalArgumentException();
      return new Selected(choice, confidence, probabilities);
    } catch (RuntimeException invalid) {
      return new Failure(FailureKind.INVALID_RESPONSE, "Decision response did not match the offered choices and probability contract.");
    }
  }

  private static String string(JsonObject object, String field) {
    var value = object.getAsJsonPrimitive(field);
    if (!value.isString()) throw new IllegalArgumentException();
    return value.getAsString();
  }

  private static double probability(JsonObject object, String field) {
    var value = object.getAsJsonPrimitive(field);
    if (!value.isNumber()) throw new IllegalArgumentException();
    double probability = value.getAsDouble();
    if (!Double.isFinite(probability) || probability < 0 || probability > 1) throw new IllegalArgumentException();
    return probability;
  }

  @Override public void close() {
    stopped = true;
    var active = pending;
    if (active != null) active.cancel(true);
    http.shutdownNow();
  }

  private static final class BoundedBody implements HttpResponse.BodySubscriber<byte[]> {
    private static final int LIMIT = 65536;
    private final CompletableFuture<byte[]> body = new CompletableFuture<>();
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private Flow.Subscription subscription;
    @Override public CompletionStage<byte[]> getBody() { return body; }
    @Override public void onSubscribe(Flow.Subscription subscription) { this.subscription = subscription; subscription.request(1); }
    @Override public void onNext(List<ByteBuffer> buffers) {
      for (ByteBuffer buffer : buffers) {
        if (bytes.size() + buffer.remaining() > LIMIT) {
          subscription.cancel();
          body.completeExceptionally(new IOException("Decision response exceeded 64 KiB"));
          return;
        }
        byte[] chunk = new byte[buffer.remaining()];
        buffer.get(chunk);
        bytes.writeBytes(chunk);
      }
      subscription.request(1);
    }
    @Override public void onError(Throwable failure) { body.completeExceptionally(failure); }
    @Override public void onComplete() { body.complete(bytes.toByteArray()); }
  }
}
