package com.quzzar.kithkyn.llm;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.quzzar.kithkyn.configuration.KithkynConfig;
import com.quzzar.kithkyn.llm.provider.LlmProvider;

class LlmServiceBackgroundQueueTest {
  private LlmService service;
  private boolean enabled;

  @BeforeEach void setup() throws Exception {
    var constructor = LlmService.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    service = constructor.newInstance();
    enabled = KithkynConfig.LlmEnabled;
    KithkynConfig.LlmEnabled = true;
  }

  @AfterEach void cleanup() {
    service.shutdown();
    KithkynConfig.LlmEnabled = enabled;
  }

  @Test void queuedMemoryStartsAfterChatGraceWithoutAnotherRequestToWakeIt() throws Exception {
    var provider = new FixtureProvider(request -> CompletableFuture.completedFuture(Optional.of(request.user())));
    install(provider);
    chat().get(1, TimeUnit.SECONDS);
    var memory = memory("remember");
    assertFalse(memory.isDone(), "Background work must respect the player's grace period");
    assertEquals(Optional.of("remember"), memory.get(9, TimeUnit.SECONDS));
    assertEquals(List.of("hello", "remember"), provider.calls);
  }

  @Test void shutdownClosesQueuedWorkAndOldWakeCannotDispatchItThroughAnotherWorld() throws Exception {
    install(new FixtureProvider(request -> CompletableFuture.completedFuture(Optional.of("old chat"))));
    chat().get(1, TimeUnit.SECONDS);
    var oldMemory = memory("old memory");
    service.shutdown();
    assertEquals(Optional.empty(), oldMemory.get(1, TimeUnit.SECONDS));
    assertEquals(Optional.empty(), memory("submitted while stopped").get(1, TimeUnit.SECONDS));
    var held = new CompletableFuture<Optional<String>>();
    var replacement = new FixtureProvider(request -> held);
    install(replacement);
    service.startLoading();
    var current = memory("new memory");
    CompletableFuture.runAsync(() -> { }, CompletableFuture.delayedExecutor(6500, TimeUnit.MILLISECONDS))
        .get(8, TimeUnit.SECONDS);
    assertEquals(List.of("new memory"), replacement.calls);
    assertFalse(current.isDone());
    held.complete(Optional.of("current result"));
    assertEquals(Optional.of("current result"), current.get(1, TimeUnit.SECONDS));
  }

  @Test void stoppedBackgroundCompletionCannotReleaseAnotherWorldsInFlightSlot() throws Exception {
    var oldReply = new CompletableFuture<Optional<String>>();
    install(new FixtureProvider(request -> oldReply));
    var oldMemory = memory("old memory");
    service.shutdown();
    assertEquals(Optional.empty(), oldMemory.get(1, TimeUnit.SECONDS));
    var newReply = new CompletableFuture<Optional<String>>();
    var replacement = new FixtureProvider(request -> newReply);
    install(replacement);
    service.startLoading();
    var first = memory("first");
    var second = memory("second");
    oldReply.complete(Optional.of("stale result"));
    assertEquals(List.of("first"), replacement.calls);
    assertFalse(first.isDone());
    assertFalse(second.isDone());
    newReply.complete(Optional.of("current result"));
    assertEquals(Optional.of("current result"), first.get(1, TimeUnit.SECONDS));
    assertEquals(Optional.of("current result"), second.get(1, TimeUnit.SECONDS));
    assertEquals(List.of("first", "second"), replacement.calls);
  }

  @Test void stoppedChatCompletionCannotChangeTheNewWorldsPriorityCounters() throws Exception {
    var oldReply = new CompletableFuture<Optional<String>>();
    install(new FixtureProvider(request -> oldReply));
    var oldChat = chat();
    service.shutdown();
    var newReply = new CompletableFuture<Optional<String>>();
    var replacement = new FixtureProvider(request -> request.user().equals("hello") ? newReply
        : CompletableFuture.completedFuture(Optional.of(request.user())));
    install(replacement);
    service.startLoading();
    var currentChat = chat();
    var currentMemory = memory("new memory");
    oldReply.complete(Optional.of("stale chat"));
    assertEquals(Optional.empty(), oldChat.get(1, TimeUnit.SECONDS));
    assertFalse(currentMemory.isDone());
    newReply.complete(Optional.of("new chat"));
    assertEquals(Optional.of("new chat"), currentChat.get(1, TimeUnit.SECONDS));
    assertEquals(Optional.of("new memory"), currentMemory.get(9, TimeUnit.SECONDS));
    assertEquals(List.of("hello", "new memory"), replacement.calls);
  }

  @Test void synchronousProviderFailureCompletesWorkAndReleasesTheQueue() throws Exception {
    var provider = new FixtureProvider(request -> {
      if (request.user().equals("broken")) throw new IllegalStateException("fixture failure");
      return CompletableFuture.completedFuture(Optional.of(request.user()));
    });
    install(provider);
    assertEquals(Optional.empty(), memory("broken").get(1, TimeUnit.SECONDS));
    assertEquals(Optional.of("next"), memory("next").get(1, TimeUnit.SECONDS));
  }

  private CompletableFuture<Optional<String>> chat() {
    return service.submitChat("fixture chat", "system", "hello", List.of(), 16, .4, 0);
  }

  private CompletableFuture<Optional<String>> memory(String text) {
    return service.submitPersona("fixture memory", "system", text, 16, .2);
  }

  private void install(LlmProvider provider) throws Exception {
    var field = LlmService.class.getDeclaredField("provider");
    field.setAccessible(true);
    field.set(service, provider);
  }

  private static final class FixtureProvider implements LlmProvider {
    private final List<String> calls = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final Function<CompletionRequest, CompletableFuture<Optional<String>>> reply;
    private FixtureProvider(Function<CompletionRequest, CompletableFuture<Optional<String>>> reply) { this.reply = reply; }
    public void start() { }
    public void shutdown() { }
    public LlmService.Status getStatus() { return LlmService.Status.READY; }
    public String getStatusDetail() { return "fixture"; }
    public CompletableFuture<Optional<String>> complete(CompletionRequest request) {
      calls.add(request.user());
      return reply.apply(request);
    }
  }
}
