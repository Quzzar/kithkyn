package com.quzzar.kithkyn.llm;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import com.quzzar.kithkyn.configuration.KithkynConfig;
import com.quzzar.kithkyn.llm.provider.LlmProvider;

class LlmServiceBackgroundQueueTest {
  @Test void queuedMemoryStartsAfterChatGraceWithoutAnotherRequestToWakeIt() throws Exception {
    var constructor = LlmService.class.getDeclaredConstructor(); constructor.setAccessible(true);
    LlmService service = constructor.newInstance();
    var calls = new AtomicInteger();
    var provider = new LlmProvider() {
      public void start() { }
      public void shutdown() { }
      public LlmService.Status getStatus() { return LlmService.Status.READY; }
      public String getStatusDetail() { return "fixture"; }
      public CompletableFuture<Optional<String>> complete(CompletionRequest request) {
        calls.incrementAndGet(); return CompletableFuture.completedFuture(Optional.of(request.user()));
      }
    };
    var field = LlmService.class.getDeclaredField("provider"); field.setAccessible(true); field.set(service, provider);
    boolean enabled = KithkynConfig.LlmEnabled; KithkynConfig.LlmEnabled = true;
    try {
      service.submitChat("fixture chat", "system", "hello", List.of(), 16, .4, 0).get(1, TimeUnit.SECONDS);
      var memory = service.submitPersona("fixture memory", "system", "remember", 16, .2);
      assertFalse(memory.isDone(), "Background work must respect the player's grace period");
      assertEquals(Optional.of("remember"), memory.get(9, TimeUnit.SECONDS));
      assertEquals(2, calls.get(), "No polling request or duplicate inference should be necessary");
    } finally { KithkynConfig.LlmEnabled = enabled; service.shutdown(); }
  }
}
