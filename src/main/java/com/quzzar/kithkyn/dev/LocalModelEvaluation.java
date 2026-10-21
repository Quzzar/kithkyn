package com.quzzar.kithkyn.dev;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.PersonEntityType;
import com.quzzar.kithkyn.agency.ResidentActionSelection;
import com.quzzar.kithkyn.agency.ResidentActionState;
import com.quzzar.kithkyn.agency.ResidentActionState.Action;
import com.quzzar.kithkyn.chat.PersonChatContext;
import com.quzzar.kithkyn.chat.PersonChatContext.Turn;
import com.quzzar.kithkyn.chat.PersonChatDispatcher;
import com.quzzar.kithkyn.configuration.KithkynConfig;
import com.quzzar.kithkyn.entities.AgeStage;
import com.quzzar.kithkyn.entities.Gender;
import com.quzzar.kithkyn.entities.KithkynAttachments;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.llm.LlamaServerLauncher;
import com.quzzar.kithkyn.llm.LlamaServerLauncher.Model;
import com.quzzar.kithkyn.llm.LlmService;
import com.quzzar.kithkyn.llm.provider.LlmProvider.CompletionRequest;
import com.quzzar.kithkyn.llm.provider.OpenAiCompatibleProvider;
import com.quzzar.kithkyn.persona.PersonaData;
import com.quzzar.kithkyn.village.Occupation;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/** Opt-in paired replay through real prompts/providers. Does not mutate a player's world or recommendations. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class LocalModelEvaluation {
  private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
  private static final UUID SPEAKER = UUID.fromString("aeb29150-216c-49a2-95e9-d7987b664783");
  private static final List<Candidate> CANDIDATES = List.of(
      new Candidate("llama", LlamaServerLauncher.LLAMA_3B),
      new Candidate("heretic", new Model("saidutta69/Llama-3.2-3B-Instruct-heretic",
          "Llama-3.2-3B-Instruct-heretic-Q4_K_M.gguf", "Llama 3.2 3B Heretic",
          "226645d6f970ac002e72fab956c41401e1a0768a", 2019373408L,
          "e780f98b35c991bc1d56331d7fac3b07d8a83356241962d13e5fdae8ff81e9a0")),
      new Candidate("gemma", LlamaServerLauncher.GEMMA_2B),
      new Candidate("abliterated", new Model("bartowski/gemma-2-2b-it-abliterated-GGUF",
          "gemma-2-2b-it-abliterated-Q4_K_M.gguf", "Gemma 2 2B abliterated",
          "11124c8787de96de70c3d5cb8d5d49c420ebcdf8", 1708582784L,
          "5f0264dd8197b8f96b6f363461cc9d5b7ad49284d2849c68942f43109a1252c4")));
  private static volatile Thread worker;

  private record Candidate(String id, Model model) { }
  private record Scenario(String id, String category, String message, String action, boolean point,
      boolean following, String memory, String situation, List<String> options, int choice) { }
  private record Corpus(List<Scenario> scenarios, List<String> conversation) { }

  private LocalModelEvaluation() { }

  @SubscribeEvent
  public static void started(ServerStartedEvent event) {
    if (!Boolean.getBoolean("kithkyn.models.evaluate")) return;
    MinecraftServer server = event.getServer();
    ServerLevel level = server.overworld();
    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
    level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
    level.setDayTime(6000);
    worker = new Thread(() -> {
      try {
        if (KithkynConfig.LlmEnabled || !KithkynConfig.LlmResidentActions || KithkynConfig.GenerateVillages) {
          throw new IllegalStateException("Evaluation requires Enable LLM?=false, Experimental resident actions=true, and Generate villages=false");
        }
        Corpus corpus = JSON.fromJson(Files.readString(Path.of(System.getProperty("kithkyn.models.corpus"))), Corpus.class);
        validate(corpus);
        int repeats = Integer.getInteger("kithkyn.models.repeats", 3);
        if (repeats < 1 || repeats > 20) throw new IllegalArgumentException("Evaluation repeats must be 1-20");
        for (String id : System.getProperty("kithkyn.models.candidates").split(",")) {
          Candidate candidate = CANDIDATES.stream().filter(item -> item.id().equals(id)).findFirst()
              .orElseThrow(() -> new IllegalArgumentException("Unknown evaluation candidate: " + id));
          evaluate(server, candidate, corpus, repeats);
        }
        Kithkyn.LOGGER.info("[model-evaluation] RESULT COMPLETE: inspect per-candidate results; completion is not a quality pass");
      } catch (Exception failure) {
        Kithkyn.LOGGER.error("[model-evaluation] RESULT ERROR", failure);
      } finally {
        server.execute(() -> server.halt(false));
      }
    }, "kithkyn-local-model-evaluation");
    worker.setDaemon(true);
    worker.start();
  }

  @SubscribeEvent
  public static void stopping(ServerStoppingEvent event) {
    Thread running = worker;
    if (running != null && running.isAlive() && running != Thread.currentThread()) running.interrupt();
  }

  private static void validate(Corpus corpus) {
    if (corpus == null || corpus.scenarios() == null || corpus.scenarios().isEmpty()
        || corpus.conversation() == null || corpus.conversation().isEmpty()) {
      throw new IllegalArgumentException("Missing evaluation corpus");
    }
    var ids = new java.util.HashSet<String>();
    for (Scenario scenario : corpus.scenarios()) {
      if (scenario.id() == null || !ids.add(scenario.id())) throw new IllegalArgumentException("Duplicate/missing case id");
      if (scenario.category().equals("decision")) {
        if (scenario.situation() == null || scenario.options() == null || scenario.options().isEmpty()
            || scenario.choice() < 0 || scenario.choice() >= scenario.options().size()) {
          throw new IllegalArgumentException("Invalid decision case " + scenario.id());
        }
      } else if (!List.of("action", "item", "memory", "roleplay").contains(scenario.category())
          || scenario.message() == null || scenario.message().isBlank() || scenario.action() == null
          || !scenario.action().equals("none") && Action.parse(scenario.action()).isEmpty()) {
        throw new IllegalArgumentException("Invalid conversation case " + scenario.id());
      }
    }
  }

  private static void evaluate(MinecraftServer server, Candidate candidate, Corpus corpus, int repeats) throws Exception {
    Path output = FMLPaths.GAMEDIR.get().resolve("results");
    Files.createDirectories(output);
    Path reportFile = output.resolve(candidate.id() + ".json");
    JsonObject report = new JsonObject();
    report.addProperty("started", Instant.now().toString());
    report.addProperty("candidate", candidate.id());
    report.add("model", JSON.toJsonTree(candidate.model()));
    report.addProperty("os", System.getProperty("os.name") + " " + System.getProperty("os.version"));
    report.addProperty("architecture", System.getProperty("os.arch"));
    report.addProperty("processors", Runtime.getRuntime().availableProcessors());
    report.addProperty("repeats", repeats);
    report.addProperty("scope", "Serial raw completions, actual game context/provider, no dispatcher retry or physical execution");
    report.addProperty("rss_note", "Process RSS only, not total unified/GPU memory; null means the metric was unavailable");
    report.addProperty("seed", "Runtime default, uncontrolled; repeated samples are not guaranteed independent");
    JsonArray samples = new JsonArray();
    report.add("samples", samples);
    Process process = null;
    OpenAiCompatibleProvider provider = null;
    try (HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(30)).build()) {
      Path binary = LlamaServerLauncher.ensureServer(http);
      if (binary == null) throw new IOException("No runtime for this platform");
      Path weights = LlamaServerLauncher.ensureModel(http, candidate.model());
      int port;
      try (ServerSocket socket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) { port = socket.getLocalPort(); }
      ProcessBuilder command = LlamaServerLauncher.buildCommand(binary, weights, port, 8192);
      // Normalize each pair's format explicitly. The Heretic GGUF has no chat template.
      // Without this, llama.cpp guesses ChatML for a Llama-trained checkpoint.
      command.command().addAll(List.of("--no-jinja", "--chat-template", candidate.id().equals("gemma")
          || candidate.id().equals("abliterated") ? "gemma" : "llama3"));
      report.addProperty("chat_format", candidate.id().equals("gemma") || candidate.id().equals("abliterated") ? "gemma" : "llama3");
      report.add("command", JSON.toJsonTree(command.command()));
      report.addProperty("runtime_sha256", digest(Files.readAllBytes(binary)));
      process = command.redirectErrorStream(true).redirectOutput(output.resolve(candidate.id() + "-runtime.log").toFile()).start();
      long startup = System.nanoTime();
      awaitHealthy(http, process, port);
      report.addProperty("rendered_template_probe", verifyTemplate(http, port));
      report.addProperty("startup_ms", elapsed(startup));
      provider = new OpenAiCompatibleProvider(OpenAiCompatibleProvider.local("http://127.0.0.1:" + port, weights.getFileName().toString()),
          () -> "", () -> weights.getFileName().toString());
      provider.start();
      // Warm-up is recorded separately and excluded from reported request latency.
      if (provider.complete(new CompletionRequest("Reply briefly.", "Say ready.", List.of(), 16, 0.0D))
          .get(160, TimeUnit.SECONDS).isEmpty()) throw new IOException("Warm-up produced no response");
      for (int repeat = 0; repeat < repeats; repeat++) {
        for (Scenario scenario : corpus.scenarios()) {
          CompletionRequest request = server.submit(() -> request(server.overworld(), scenario, List.of())).get(30, TimeUnit.SECONDS);
          samples.add(sample(provider, process, scenario, request, repeat));
          write(reportFile, report);
          Kithkyn.LOGGER.info("[model-evaluation] {} sample {}: {}", candidate.id(), samples.size(), scenario.id());
        }
      }
      List<Turn> history = new ArrayList<>();
      for (int index = 0; index < corpus.conversation().size(); index++) {
        String line = corpus.conversation().get(index);
        Scenario scenario = new Scenario("conversation-" + index, "conversation", line, "none", false,
            false, null, null, List.of(), 0);
        List<Turn> captured = List.copyOf(history);
        CompletionRequest request = server.submit(() -> request(server.overworld(), scenario, captured)).get(30, TimeUnit.SECONDS);
        JsonObject sample = sample(provider, process, scenario, request, 0);
        samples.add(sample);
        if (sample.has("say")) history.add(new Turn(line, sample.get("say").getAsString(), 6000L));
        write(reportFile, report);
      }
      report.addProperty("status", "complete");
    } catch (Exception failure) {
      report.addProperty("status", "error");
      report.addProperty("error", failure.toString());
      Kithkyn.LOGGER.error("[model-evaluation] {} failed", candidate.id(), failure);
      if (failure instanceof InterruptedException) throw failure;
    } finally {
      if (provider != null) provider.shutdown();
      if (process != null) {
        process.destroy();
        if (!process.waitFor(10, TimeUnit.SECONDS)) process.destroyForcibly();
      }
      report.addProperty("finished", Instant.now().toString());
      write(reportFile, report);
    }
  }

  private static CompletionRequest request(ServerLevel level, Scenario scenario, List<Turn> history) {
    if (scenario.category().equals("decision")) return LlmService.decisionRequest(scenario.situation(), scenario.options(), false);
    EvaluationPerson person = new EvaluationPerson(level);
    ServerPlayer speaker = FakePlayerFactory.get(level, new GameProfile(SPEAKER, "Rowan"));
    speaker.moveTo(0.5D, 80, 3.5D);
    if (scenario.memory() != null) person.logMemory(scenario.memory(), Optional.of(SPEAKER));
    if (scenario.following()) {
      person.agency().apply(person.agency().offer(speaker),
          new ResidentActionSelection(Optional.of(Action.FOLLOW), Optional.empty()), speaker);
      if (person.agency().current().filter(intent -> intent.action() == Action.FOLLOW).isEmpty()) {
        throw new IllegalStateException("The active-follow evaluation fixture was rejected");
      }
    }
    var assembled = PersonChatContext.assemble(person, "Rowan", SPEAKER, history, scenario.message());
    var offer = new ResidentActionState.Offer(1, SPEAKER, "Rowan", level.dimension().location().toString(),
        scenario.point() ? new ResidentActionState.Position(6.5D, 80, 2.5D) : null,
        new ResidentActionState.Position(0.5D, 80, 0.5D), level.getGameTime() + 600);
    var grounded = new PersonChatContext.AssembledChat(assembled.system() + person.agency().prompt(offer), assembled.user(), assembled.examples());
    return PersonChatDispatcher.initialRequest(grounded);
  }

  private static JsonObject sample(OpenAiCompatibleProvider provider, Process process, Scenario scenario,
      CompletionRequest request, int repeat) throws Exception {
    JsonObject row = new JsonObject();
    row.addProperty("id", scenario.id());
    row.addProperty("category", scenario.category());
    row.addProperty("repeat", repeat);
    if (scenario.category().equals("decision")) row.addProperty("expected_choice", scenario.choice());
    else row.addProperty("expected_action", scenario.action());
    row.add("request", JSON.toJsonTree(request));
    row.addProperty("prompt_sha256", digest(JSON.toJson(request).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    long started = System.nanoTime();
    Optional<String> response = LlmService.callProvider(provider, "evaluation", scenario.id(), request).get(160, TimeUnit.SECONDS);
    row.addProperty("latency_ms", elapsed(started));
    row.addProperty("response_present", response.isPresent());
    row.add("rss_kib", rss(process));
    if (response.isEmpty()) return row;
    String raw = response.get();
    row.addProperty("raw", raw);
    if (scenario.category().equals("decision")) {
      var decision = LlmService.parseStrictDecision(raw, scenario.options());
      row.addProperty("decision_valid", decision.isPresent());
      row.addProperty("decision_correct", decision.isPresent() && decision.get().choiceIndex() == scenario.choice());
      return row;
    }
    ResidentActionSelection selected = ResidentActionSelection.parse(raw);
    row.addProperty("action_valid", selected.error().isEmpty());
    String action = selected.action().map(Action::wireName).orElse("none");
    row.addProperty("selected_action", action);
    row.addProperty("action_matches", selected.error().isEmpty() && action.equals(scenario.action()));
    selected.error().ifPresent(error -> row.addProperty("action_error", error));
    try {
      JsonObject reply = JsonParser.parseString(raw).getAsJsonObject();
      if (reply.has("say") && reply.get("say").isJsonPrimitive() && reply.getAsJsonPrimitive("say").isString()) {
        String say = reply.get("say").getAsString();
        row.addProperty("say", say);
        row.addProperty("dialogue_present", !say.isBlank());
      }
      row.addProperty("inventory_legal", inventoryLegal(reply));
      row.addProperty("fight", reply.has("fight") && reply.get("fight").getAsBoolean());
    } catch (RuntimeException malformed) {
      row.addProperty("dialogue_present", false);
    }
    return row;
  }

  private static boolean inventoryLegal(JsonObject reply) {
    if (!reply.has("give")) return true;
    try {
      String itemId = reply.get("give").getAsString();
      if (!itemId.contains(":")) itemId = "minecraft:" + itemId;
      int available = switch (itemId) {
        case "minecraft:bread" -> 3;
        case "minecraft:oak_log" -> 4;
        case "minecraft:stone_axe" -> 1;
        default -> 0;
      };
      int count = reply.has("give_count") ? reply.get("give_count").getAsBigDecimal().intValueExact() : 1;
      return count > 0 && count <= available;
    } catch (RuntimeException malformed) { return false; }
  }

  private static void awaitHealthy(HttpClient client, Process process, int port) throws Exception {
    long deadline = System.nanoTime() + Duration.ofMinutes(2).toNanos();
    while (System.nanoTime() < deadline) {
      if (!process.isAlive()) throw new IOException("Runtime exited with " + process.exitValue());
      try {
        var response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/health"))
            .timeout(Duration.ofSeconds(2)).GET().build(), HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() == 200) return;
      } catch (IOException notListeningYet) { }
      Thread.sleep(500);
    }
    throw new IOException("Runtime did not become healthy within two minutes");
  }

  private static String verifyTemplate(HttpClient client, int port) throws Exception {
    JsonObject body = JsonParser.parseString("""
        {"messages":[{"role":"system","content":"template-probe-system"},
        {"role":"user","content":"template-probe-user"}],"add_generation_prompt":true}
        """).getAsJsonObject();
    var response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/apply-template"))
        .timeout(Duration.ofSeconds(5)).header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(), HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 200) throw new IOException("Template probe HTTP " + response.statusCode());
    String rendered = JsonParser.parseString(response.body()).getAsJsonObject().get("prompt").getAsString();
    if (!rendered.contains("template-probe-user") || !rendered.contains("template-probe-system")) {
      throw new IOException("The configured chat template omitted the test messages");
    }
    return rendered;
  }

  private static com.google.gson.JsonElement rss(Process process) {
    try {
      Process probe = new ProcessBuilder("ps", "-o", "rss=", "-p", Long.toString(process.pid())).start();
      if (!probe.waitFor(2, TimeUnit.SECONDS)) probe.destroyForcibly();
      else if (probe.exitValue() == 0) {
        String result = new String(probe.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).trim();
        return JSON.toJsonTree(Long.parseLong(result));
      }
    } catch (InterruptedException stopped) { Thread.currentThread().interrupt(); }
    catch (IOException | NumberFormatException unavailable) {
      Kithkyn.LOGGER.warn("[model-evaluation] RSS metric unavailable: {}", unavailable.toString());
    }
    return JsonNull.INSTANCE; // An unavailable metric is never reported as zero memory.
  }

  private static String digest(byte[] bytes) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
  }

  private static long elapsed(long started) { return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started); }

  private static void write(Path path, JsonObject report) throws IOException {
    Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
    Files.writeString(temporary, JSON.toJson(report));
    Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
  }

  private static final class EvaluationPerson extends RealPerson {
    private EvaluationPerson(ServerLevel level) {
      super(PersonEntityType.PERSON.get(), level);
      setUUID(UUID.fromString("7024b858-707c-4f59-b0f4-6e219f275845"));
      setFirstName("Mira");
      setLastName("Flint");
      setGender(Gender.FEMALE);
      setLifeStage(AgeStage.ADULT);
      setOccupation(Occupation.WANDERER);
      setData(KithkynAttachments.PERSONA.get(), new PersonaData(
          "A friendly, blunt traveler who enjoys helping with short walks. She is independent, dislikes threats,"
              + " and admits when she does not know something. She has no current urgent duties or commitments.",
          "Taps her axe handle while thinking.", "evaluation-fixture", 0, 1));
      setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_AXE));
      personMainInv.addItem(new ItemStack(Items.BREAD, 3));
      personMainInv.addItem(new ItemStack(Items.OAK_LOG, 4));
      moveTo(0.5D, 80, 0.5D, 0, 0);
    }
  }
}
