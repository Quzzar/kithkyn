package com.quzzar.kithkyn.decision;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import com.quzzar.kithkyn.configuration.KithkynConfig;
import net.minecraft.server.MinecraftServer;

/** Server-thread admission and lifecycle. One request at a time, at most one start per second, no queue or retries. */
public final class ReactionDecisions {
  private static final Map<MinecraftServer, ReactionDecisions> SERVERS = new WeakHashMap<>();
  private DecisionProvider provider;
  private DecisionProvider.Failure configurationFailure;
  private boolean inFlight;
  private long nextStart;

  private ReactionDecisions(DecisionProvider provider) { this.provider = provider; }

  static void verificationProvider(MinecraftServer server, DecisionProvider provider) {
    if (!Boolean.getBoolean("kithkyn.residentReactions.verify")) throw new IllegalStateException("Verification is disabled");
    stop(server);
    SERVERS.put(server, new ReactionDecisions(provider));
  }

  private ReactionDecisions() {
    try { provider = new SystemOneProvider(KithkynConfig.ReactionEndpoint, KithkynConfig.ReactionModel, KithkynConfig.ReactionApiKey); }
    catch (RuntimeException invalid) {
      configurationFailure = new DecisionProvider.Failure(DecisionProvider.FailureKind.CONFIGURATION, "Invalid decision endpoint configuration. Restart after correcting it.");
    }
  }

  public static ReactionDecisions of(MinecraftServer server) { return SERVERS.computeIfAbsent(server, ignored -> new ReactionDecisions()); }
  public boolean available(MinecraftServer server) { return !inFlight && server.getTickCount() >= nextStart; }

  public boolean choose(MinecraftServer server, DecisionProvider.Request request, Consumer<DecisionProvider.Result> completion) {
    if (!available(server)) return false;
    nextStart = server.getTickCount() + 20L;
    if (configurationFailure != null) { completion.accept(configurationFailure); return true; }
    inFlight = true;
    provider.choose(request).whenComplete((result, failure) -> server.execute(() -> {
      inFlight = false;
      completion.accept(failure == null ? result : new DecisionProvider.Failure(DecisionProvider.FailureKind.TRANSPORT,
          "Decision request failed unexpectedly."));
    }));
    return true;
  }

  public static void stop(MinecraftServer server) {
    ReactionDecisions service = SERVERS.remove(server);
    if (service != null && service.provider != null) service.provider.close();
  }
}
