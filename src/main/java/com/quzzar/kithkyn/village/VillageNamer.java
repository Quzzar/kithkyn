package com.quzzar.kithkyn.village;

import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.llm.LlmService;
import com.quzzar.kithkyn.village.buildings.VillageStyle;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

/**
 * Names a village at founding (campfire map #60, decided by Aaron: LLM-named
 * only, no rename mechanism). The name is requested BEFORE the camp is placed
 * and the founding waits the moment it takes to land, so a village only ever
 * has one name: no provisional name in logs, on residents, or anywhere else.
 * One retry on unparseable output, then a word-list name stands. The LLM
 * service always completes its future (empty on unavailability, error, or
 * timeout), so the founding can never hang on this request.
 */
public final class VillageNamer {

  private static final List<String> COMMON_ENDINGS = List.of(
      "field", "bury", "haven", "stead", "wick", "gate", "hollow", "march", "ford", "crest");

  private static final int MAX_NAME_LENGTH = 32;

  private VillageNamer() {
  }

  /**
   * Requests the founding name and hands the final choice to {@code onName} on
   * the server thread: the LLM name when generation succeeds, a word-list name
   * when it fails twice. Exactly one name is ever delivered.
   */
  public static void requestFoundingName(ServerLevel level, VillageStyle style, Consumer<String> onName) {
    String system = "You name new settlements in a medieval fantasy world."
        + " Reply with ONLY the settlement name: one to three words, letters and spaces only,"
        + " no quotes and no explanation.";
    String user = foundingPrompt(style);
    request(level, style, system, user, true, onName);
  }

  /** The selected architectural catalog supplies the identity, never incidental nearby blocks. */
  static String foundingPrompt(VillageStyle style) {
    NamingProfile profile = profile(style);
    return "Name a new settlement with this architectural and community identity: "
        + profile.description() + "\nVaried examples of tone, NOT names to choose from: "
        + String.join(", ", profile.examples())
        + ". Invent a fresh name with its own sound; do not copy, respell, or recombine the examples."
        + " It need not mention trees, building materials, or the biome. Avoid a literal landscape label.";
  }

  private static void request(ServerLevel level, VillageStyle style, String system, String user, boolean mayRetry,
      Consumer<String> onName) {
    LlmService.get().submitPersona("naming a new settlement", system, user, 16, 0.7)
        .thenAccept(reply -> level.getServer().execute(() -> {
          Set<String> existing = VillageManager.get(level).getVillages().values().stream()
              .map(Village::getName).collect(Collectors.toSet());
          Optional<String> name = reply.flatMap(raw -> acceptedName(raw, style, existing));
          if (name.isPresent()) {
            onName.accept(name.get());
          } else if (mayRetry) {
            Kithkyn.LOGGER.debug("Village name generation produced unusable output; retrying once");
            request(level, style, system, user
                + "\nThe previous answer was invalid, already used, or an example. Choose a different original name.",
                false, onName);
          } else {
            String fallback = fallback(style, existing, level.getRandom());
            Kithkyn.LOGGER.info("Village name generation failed twice; the word-list name '{}' stands", fallback);
            onName.accept(fallback);
          }
        }));
  }

  /** Rejects example copies and existing names again when the asynchronous answer reaches the server. */
  static Optional<String> acceptedName(String raw, VillageStyle style, Set<String> existing) {
    return parse(raw).filter(name -> !blockedNames(style, existing).contains(nameKey(name)));
  }

  /** A style-aware, bounded retry over real alternatives, followed by a unique letter suffix if crowded. */
  static String fallback(VillageStyle style, Set<String> existing, RandomSource random) {
    NamingProfile profile = profile(style);
    Set<String> blocked = blockedNames(style, existing);
    int endings = profile.endings().size();
    int count = profile.starts().size() * endings;
    int start = random.nextInt(count);
    String base = profile.starts().get(start / endings) + profile.endings().get(start % endings);
    for (int index = 0; index < count; index++) {
      int pick = (start + index) % count;
      String name = profile.starts().get(pick / endings) + profile.endings().get(pick % endings);
      if (!blocked.contains(nameKey(name))) return name;
    }
    for (int index = 0; ; index++) {
      String suffix = "";
      for (int value = index; value >= 0; value = value / 26 - 1) {
        suffix = (char) ('A' + value % 26) + suffix;
      }
      String name = base + " " + suffix;
      if (!blocked.contains(nameKey(name))) return name;
    }
  }

  private static Set<String> blockedNames(VillageStyle style, Set<String> existing) {
    return java.util.stream.Stream.concat(existing.stream(), profile(style).examples().stream())
        .map(VillageNamer::nameKey).collect(Collectors.toSet());
  }

  /** Spaces, case and punctuation must not turn a copied name into an apparently new one. */
  private static String nameKey(String name) {
    return name.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}]", "");
  }

  /** Small authored naming cues, not a runtime nationality or a new village identity axis. */
  private record NamingProfile(String description, List<String> examples, List<String> starts,
      List<String> endings) {
    private NamingProfile(String description, List<String> examples, List<String> starts) {
      this(description, examples, starts, COMMON_ENDINGS);
    }
  }

  private static NamingProfile profile(VillageStyle style) {
    return switch (style) {
      case BADLANDS -> new NamingProfile(
          "A close community of orange-clay and red-sandstone courtyards, stepped roof terraces,"
              + " shared households, shaded markets, acacia workshops and carefully protected water."
              + " Compact, warm invented fantasy names with a distinct sound.",
          List.of("Kestara", "Oravel", "Tavren", "Sorela"),
          List.of("Kes", "Ora", "Tav", "Sor", "Vel", "An", "Cas", "Mer"),
          List.of("ara", "avel", "ren", "ela", "ora", "aven", "erin", "ali"));
      case BIRCH_FOREST -> new NamingProfile(
          "Grounded, welcoming woodland folk; pale birch timber, rough and mossy cobblestone,"
              + " grass roofs, sheltered rooms, torches, candles and shared hearths."
              + " An intimate, gently whimsical old-world sound, earthy rather than grandiose.",
          List.of("Brindle", "Tallowick", "Dunmere", "Bellamere", "Heartham"),
          List.of("Brin", "Tallow", "Dun", "Bellen", "Wick", "Hearth", "Mallow", "Fen"));
      case PLAINS -> new NamingProfile(
          "An open, sociable farming community of timber homes, shared markets and busy workshops."
              + " Familiar, warm, practical place-names with a little age to them.",
          List.of("Wetherby", "Alderton", "Merrivale"),
          List.of("Alder", "Merri", "Wether", "Fair", "Amber", "Elm", "Bell", "Barley"));
      case TAIGA -> new NamingProfile(
          "A sheltered conifer settlement of dark timber, sturdy workshops and close hearths."
              + " Steady, compact, weather-worn names with a strong sound.",
          List.of("Valdren", "Harrowfell", "Ostwick"),
          List.of("Val", "Harrow", "Ost", "Raven", "Alder", "Grey", "Wold", "Tor"));
      case SNOWY -> new NamingProfile(
          "A close-knit cold-country settlement built around shelter, stores and enduring warm interiors."
              + " Spare, resonant, resilient names rather than a list of snow synonyms.",
          List.of("Eldvik", "Halden", "Varnholt"),
          List.of("Eld", "Hal", "Varn", "Keld", "Haven", "Auren", "Bryn", "Nord"));
      case DESERT -> new NamingProfile(
          "An arid-country settlement of sheltered courtyards, pale masonry, shade and hospitable traders."
              + " Warm, flowing, memorable invented place-names.",
          List.of("Darava", "Serevan", "Amarin"),
          List.of("Dara", "Sere", "Amar", "Tala", "Aven", "Sola", "Nara", "Kes"));
      case SAVANNA -> new NamingProfile(
          "A sunlit, open-country community of warm timber, gathering spaces and lively trade."
              + " Bright, rhythmic, grounded invented place-names.",
          List.of("Talora", "Orendi", "Marava"),
          List.of("Talo", "Oren", "Mara", "Kera", "Noma", "Ari", "Kasa", "Vela"));
    };
  }

  /** First line, unquoted, at most three words of letters; empty when unusable. */
  private static Optional<String> parse(String raw) {
    String candidate = raw.strip().split("\\R", 2)[0].strip();
    if (candidate.length() >= 2
        && (candidate.charAt(0) == '"' || candidate.charAt(0) == '\'')
        && candidate.charAt(candidate.length() - 1) == candidate.charAt(0)) {
      candidate = candidate.substring(1, candidate.length() - 1).strip();
    }
    if (candidate.endsWith(".")) {
      candidate = candidate.substring(0, candidate.length() - 1).strip();
    }
    if (candidate.isEmpty() || candidate.length() > MAX_NAME_LENGTH) {
      return Optional.empty();
    }
    String[] words = candidate.split(" +");
    if (words.length > 3) {
      return Optional.empty();
    }
    if (!candidate.matches("[\\p{L}][\\p{L}' -]*")) {
      return Optional.empty();
    }
    // Settlement names read as proper nouns; a lowercase reply gets title case.
    StringBuilder titled = new StringBuilder(candidate.length());
    for (int i = 0; i < words.length; i++) {
      if (i > 0) {
        titled.append(' ');
      }
      titled.append(Character.toUpperCase(words[i].charAt(0)))
          .append(words[i].substring(1));
    }
    String result = titled.toString();
    if (result.toLowerCase(Locale.ROOT).startsWith("the ") && words.length == 1) {
      return Optional.empty();
    }
    return Optional.of(result);
  }

}
