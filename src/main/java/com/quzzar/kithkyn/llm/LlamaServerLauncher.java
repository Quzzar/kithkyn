package com.quzzar.kithkyn.llm;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipInputStream;

import com.quzzar.kithkyn.Kithkyn;

import net.neoforged.fml.loading.FMLPaths;

/**
 * Provisions and launches llama.cpp's server, so the fast path needs nothing
 * installed by hand.
 *
 * Provisioned the same way as the model itself: the GGUF weights are a couple
 * of gigabytes downloaded on first run, and a llama.cpp build is 11 to 18 MB
 * beside them. Fetching it is cheaper than the model it will load, and
 * llama.cpp is MIT licensed, so shipping or fetching it is allowed.
 *
 * Deliberately a SUBPROCESS rather than a JNI binding. A binding would mean
 * native libraries for four platforms inside the mod jar, loaded through a
 * modded classloader, with all the split-package and multi-release-jar hazards
 * that native inference-in-JVM runs into under FML. A process boundary cannot
 * have a classloader conflict, so this avoids the whole class of them.
 */
public final class LlamaServerLauncher {

  /**
   * Pinned. llama.cpp publishes many builds a day and renames assets between
   * them, so tracking "latest" would break without anyone touching this repo.
   */
  private static final String BUILD = "b10653";
  private static final String RELEASE =
      "https://github.com/ggml-org/llama.cpp/releases/download/" + BUILD + "/";

  /** Where a model lives on HuggingFace, and what it costs to fetch. */
  public record Model(String repo, String file, String label, String revision, long size, String sha256) {
    String url() {
      return "https://huggingface.co/" + repo + "/resolve/" + revision + "/" + file + "?download=true";
    }
  }

  /**
   * Benchmarked on the four jobs the mod actually gives a model, using the real
   * system prompts, few-shot turns and temperatures from each call site. Ten
   * runs per task, scored on whether the CONTENT was usable rather than on
   * whether it parsed: every model parses nearly everything, so parse rate
   * cannot separate them.
   *
   *                  decide  chat  relation | persona
   *   Qwen2.5-3B     10/10  9/10    10/10   |  6/10
   *   Gemma-2-2B     10/10 10/10    10/10   |  6/10
   *   Llama-3.2-3B    8/10 10/10    10/10   |  1/10
   *   Qwen2.5-1.5B    8/10  9/10    10/10   |  3/10
   *
   * Median latency, measured alone on an M-series Mac with nothing else running:
   *
   *                  decide  chat  persona  relation   size
   *   Qwen2.5-1.5B    454ms  299ms   597ms    561ms    1.1G
   *   Gemma-2-2B      642ms  742ms  1215ms   1442ms    1.7G
   *   Qwen2.5-3B      743ms  611ms  1142ms   1022ms    2.1G
   *   Llama-3.2-3B    766ms  580ms  1261ms   1020ms    2.0G
   *
   * The table above scores whether a single reply is USABLE, and by that
   * measure the models are close. It misses the thing a player feels most,
   * because it looks at one turn at a time: whether a villager can hold a
   * CONVERSATION. A follow-up ran a twelve-turn chat per model with history
   * accumulating as the mod builds it, and scored repetition, echoing the
   * player back, and how many distinct openings the model managed:
   *
   *                  distinct openings / 12   reused own opening   echoed player
   *   Llama-3.2-3B          12                     0                   0
   *   Gemma-2-2B            10                     2                   0
   *   Qwen2.5-3B             2                     0                   4
   *   Qwen2.5-1.5B           8                     4                   0
   *
   * Qwen2.5-3B opened "Ah, Quzzar!" on all twelve turns - the "talks in
   * circles" failure a player hits within a minute. That is why the Qwen
   * options were removed and Llama-3.2-3B is the default: it was the clear best
   * at conversation, at 580ms a reply, and a near-tie on the build decision it
   * is a hair behind on. Gemma is the one kept alternative.
   *
   * The persona column is NOT trustworthy and is kept only to show that the
   * task is the weak one for every model. It asks whether the blurb used each
   * listed trait, scored by keyword, and cannot recognise "a mountain of a man"
   * as "a true giant"; hand reading shows real defects underneath but the
   * number overstates them. Scoring persona properly needs a judge model.
   *
   * Three call sites are still unmeasured: relationship SELECTION, reflection,
   * and village naming.
   */
  public static final Model GEMMA_2B = new Model(
      "bartowski/gemma-2-2b-it-GGUF", "gemma-2-2b-it-Q4_K_M.gguf", "Gemma-2-2B-it",
      "855f67caed130e1befc571b52bd181be2e858883", 1708582752L,
      "e0aee85060f168f0f2d8473d7ea41ce2f3230c1bc1374847505ea599288a7787");
  public static final Model LLAMA_3B = new Model(
      "bartowski/Llama-3.2-3B-Instruct-GGUF", "Llama-3.2-3B-Instruct-Q4_K_M.gguf", "Llama-3.2-3B-Instruct",
      "5ab33fa94d1d04e903623ae72c95d1696f09f9e8", 2019377696L,
      "6c1a2b41161032677be168d354123594c0e6e67d2b9227c84f296ad037c728ff");

  /**
   * What the config's model name may say, and what it gets. Llama-3.2-3B is the
   * default and the only unqualified answer: it was the clear best at holding a
   * conversation when the four were run head to head, and a villager that talks
   * in circles is the thing players notice first. Gemma stays as the one
   * alternative. The Qwen options were removed - both looped their own opening
   * line back at the player turn after turn (Aaron, in play).
   */
  public static Model byName(String name) {
    String key = name == null ? "" : name.toLowerCase(Locale.ROOT).replace(" ", "");
    if (key.contains("gemma")) {
      return GEMMA_2B;
    }
    return LLAMA_3B;
  }

  private LlamaServerLauncher() {
  }

  /** The release asset for this machine, or null where llama.cpp ships none. */
  static String assetFor(String osName, String arch) {
    String os = osName.toLowerCase(Locale.ROOT);
    String architecture = arch.toLowerCase(Locale.ROOT);
    boolean arm = architecture.equals("aarch64") || architecture.equals("arm64");
    if (!arm && !architecture.equals("amd64") && !architecture.equals("x86_64")) return null;
    if (os.contains("mac") || os.contains("darwin")) {
      return "llama-" + BUILD + "-bin-macos-" + (arm ? "arm64" : "x64") + ".tar.gz";
    }
    if (os.contains("win")) {
      return "llama-" + BUILD + "-bin-win-cpu-" + (arm ? "arm64" : "x64") + ".zip";
    }
    if (os.contains("linux")) {
      return "llama-" + BUILD + "-bin-ubuntu-" + (arm ? "arm64" : "x64") + ".tar.gz";
    }
    return null;
  }


  /** Published GitHub release asset sizes and digests for the pinned CPU runtimes. */
  private record Artifact(long size, String sha256) { }
  private static final Map<String, Artifact> ARTIFACTS = Map.of(
      "macos-arm64.tar.gz", new Artifact(10996458L, "8a1d3050c83af509dcce0ff2f747cf20a7a2b6f7150722cda851d7f2fbb6cf5e"),
      "macos-x64.tar.gz", new Artifact(11059514L, "e3be95ea7de89ce66c8c75cb772c7cd4d7406d48d7ea5be2608d71ebe0077d00"),
      "ubuntu-arm64.tar.gz", new Artifact(13079596L, "e41c0cda2879d83b300a999754b48f3b710ddd52efeeb8f8cdacae2e0ab3af84"),
      "ubuntu-x64.tar.gz", new Artifact(16329417L, "56f0af30ba047d30c685a32cf89a7ac4b4fccc27c8eade902d1157102b3bcbbf"),
      "win-cpu-arm64.zip", new Artifact(11864380L, "d21bec9e40516c3e5513600796937e2286088f228ee9025b92bdc33c1d98af12"),
      "win-cpu-x64.zip", new Artifact(18090863L, "63cae6a35f6b2f9574bfd6653b305b549d77995994c88ab5940a945f9b16da11"));

  private static Path runtimeDir() {
    return FMLPaths.GAMEDIR.get().resolve("kithkyn").resolve("runtime");
  }

  private static Path modelDir() {
    return FMLPaths.GAMEDIR.get().resolve("kithkyn").resolve("models");
  }

  /**
   * The llama-server executable, fetched and unpacked if this is the first run.
   * Returns null when no build exists for this platform, which is a reason to
   * fall back to rule-based behavior.
   */
  public static Path ensureServer(HttpClient client) throws IOException, InterruptedException {
    String asset = assetFor(System.getProperty("os.name", ""), System.getProperty("os.arch", ""));
    if (asset == null) {
      Kithkyn.LOGGER.warn("llama.cpp publishes no build for {} {}; using rule-based behavior",
          System.getProperty("os.name"), System.getProperty("os.arch"));
      return null;
    }

    Path dir = runtimeDir().resolve(BUILD);
    Files.createDirectories(dir);
    Path archive = dir.resolve(asset);
    Artifact artifact = ARTIFACTS.get(asset.substring(("llama-" + BUILD + "-bin-").length()));
    if (artifact == null) throw new IOException("Unverified runtime asset: " + asset);
    VerifiedDownload.ensure(client, URI.create(RELEASE + asset), archive, artifact.size(), artifact.sha256());
    Path extracted = dir.resolve("verified");
    Path marker = dir.resolve("verified-server.sha256");
    Path server = findServer(extracted);
    if (server == null || !Files.isRegularFile(marker)
        || !Files.readString(marker).equals(artifact.sha256() + "\n" + VerifiedDownload.sha256(server))) {
      Files.createDirectories(extracted);
      unpack(archive, extracted);
      server = findServer(extracted);
      if (server == null) throw new IOException("llama-server not found inside " + asset);
      Files.writeString(marker, artifact.sha256() + "\n" + VerifiedDownload.sha256(server));
    }
    makeRunnable(server);
    Kithkyn.LOGGER.info("Verified local runtime ready at {}", server);
    return server;
  }

  /** Fetches pinned weights, resuming interruptions and replacing corrupt cached files. */
  public static Path ensureModel(HttpClient client, Model model) throws IOException, InterruptedException {
    Path target = modelDir().resolve(model.file());
    VerifiedDownload.ensure(client, URI.create(model.url()), target, model.size(), model.sha256());
    Kithkyn.LOGGER.info("Verified {} ready ({} MB)", model.label(), model.size() / 1_000_000);
    return target;
  }

  public static ProcessBuilder buildCommand(Path server, Path model, int port, int contextSize) {
    List<String> command = new ArrayList<>();
    command.add(server.toAbsolutePath().toString());
    command.add("-m");
    command.add(model.toAbsolutePath().toString());
    command.add("--host");
    // Loopback only. This server answers without authentication, so it must not
    // be reachable from anywhere but this machine.
    command.add("127.0.0.1");
    command.add("--port");
    command.add(Integer.toString(port));
    command.add("-c");
    command.add(Integer.toString(contextSize));
    // Villager prompts are short and many; a small batch keeps latency down.
    command.add("-np");
    command.add("2");

    ProcessBuilder builder = new ProcessBuilder(command);
    builder.directory(server.getParent().toFile());
    return builder;
  }

  /** ZIP extraction uses Java so a Windows install does not depend on an external unzip program. */
  static void unpack(Path archive, Path into) throws IOException, InterruptedException {
    if (archive.getFileName().toString().endsWith(".zip")) {
      Path root = into.toAbsolutePath().normalize();
      try (ZipInputStream input = new ZipInputStream(Files.newInputStream(archive))) {
        java.util.zip.ZipEntry entry;
        while ((entry = input.getNextEntry()) != null) {
          Path destination = root.resolve(entry.getName()).normalize();
          if (!destination.startsWith(root)) throw new IOException("Archive entry escapes runtime directory");
          if (entry.isDirectory()) Files.createDirectories(destination);
          else {
            Files.createDirectories(destination.getParent());
            Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
          }
          input.closeEntry();
        }
      }
      return;
    }
    Process process = new ProcessBuilder("tar", "-xzf", archive.toString(), "-C", into.toString())
        .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
    if (!process.waitFor(5, java.util.concurrent.TimeUnit.MINUTES)) {
      process.destroyForcibly();
      throw new IOException("Timed out unpacking " + archive.getFileName());
    }
    if (process.exitValue() != 0) throw new IOException("Could not unpack " + archive.getFileName());
  }

  private static Path findServer(Path dir) throws IOException {
    if (!Files.isDirectory(dir)) {
      return null;
    }
    try (var walk = Files.walk(dir)) {
      return walk.filter(Files::isRegularFile)
          .filter(p -> {
            String n = p.getFileName().toString();
            return n.equals("llama-server") || n.equals("llama-server.exe");
          })
          .findFirst().orElse(null);
    }
  }

  /**
   * Unix needs the execute bit, and macOS quarantines anything downloaded:
   * Gatekeeper refuses to run it until the attribute is cleared, which is the
   * single most likely reason a first launch fails.
   */
  private static void makeRunnable(Path server) {
    server.toFile().setExecutable(true, false);
    if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac")) {
      return;
    }
    try {
      new ProcessBuilder("xattr", "-dr", "com.apple.quarantine",
          server.getParent().toAbsolutePath().toString())
          .redirectErrorStream(true).start().waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
    } catch (Exception e) {
      Kithkyn.LOGGER.warn("Could not clear the quarantine flag; macOS may refuse to start the "
          + "local runtime. Villagers can still use rule-based behavior.", e);
    }
  }
}
