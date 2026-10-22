package com.quzzar.kithkyn.chat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.google.gson.Gson;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.UUIDUtil;

/** Bounded per-counterpart conversation memory in the existing saved attachment. */
public record ChatSummaryData(Map<UUID, Memory> byPlayer) {
  public static final int MAX_PLAYERS = 8;
  public static final int MAX_QUOTES = 4;
  public static final ChatSummaryData EMPTY = new ChatSummaryData(Map.of());

  /** Old prose remains explicitly unverified; new memories contain only selected source lines. */
  public record Memory(List<String> quotes, String unverified) {
    private static final Codec<List<String>> QUOTES = Codec.STRING.listOf().validate(lines ->
        lines.size() <= MAX_QUOTES && lines.stream().allMatch(line -> !line.isBlank() && line.length() <= 2048)
            ? DataResult.success(lines) : DataResult.error(() -> "Invalid conversation memory bounds"));
    private static final Codec<Memory> CODEC = Codec.either(Codec.STRING, QUOTES).xmap(
        value -> value.map(old -> new Memory(List.of(), old), quotes -> new Memory(quotes, "")),
        memory -> memory.unverified().isBlank() ? Either.right(memory.quotes()) : Either.left(memory.unverified()));
    public Memory { quotes = List.copyOf(quotes); }
  }

  public static final Codec<ChatSummaryData> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Memory.CODEC)
      .xmap(ChatSummaryData::new, ChatSummaryData::byPlayer);

  public ChatSummaryData { byPlayer = Map.copyOf(byPlayer); }

  /** Attributed words remain questions or claims, never evidence that an action occurred. */
  public String with(UUID counterpart) {
    Memory memory = byPlayer.get(counterpart);
    if (memory == null) return "";
    if (!memory.unverified().isBlank()) {
      String old = ConversationMemoryPrompt.withoutVolatileWorldState(memory.unverified());
      return old.isBlank() ? "" : "Unverified earlier model-written recollection, not evidence: " + old;
    }
    return String.join("\n", memory.quotes().stream()
        .map(line -> "The counterpart said: " + new Gson().toJson(line)).toList());
  }

  public List<String> quotesWith(UUID counterpart) {
    Memory memory = byPlayer.get(counterpart);
    return memory == null ? List.of() : memory.quotes();
  }

  /** Successful source selection replaces old prose; failed inference leaves it untouched. */
  public ChatSummaryData withQuotes(UUID counterpart, List<String> quotes) {
    if (quotes.size() > MAX_QUOTES || quotes.stream().anyMatch(line -> line.isBlank() || line.length() > 2048))
      throw new IllegalArgumentException("Invalid conversation memory bounds");
    Map<UUID, Memory> updated = new HashMap<>(byPlayer);
    updated.put(counterpart, new Memory(quotes, ""));
    while (updated.size() > MAX_PLAYERS) {
      UUID other = updated.keySet().stream().filter(id -> !id.equals(counterpart)).findFirst().orElseThrow();
      updated.remove(other);
    }
    return new ChatSummaryData(updated);
  }
}
