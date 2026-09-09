package com.quzzar.kithkyn.village;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;

/** Auxiliary defenders, deliberately outside human population, employment and housing. */
public final class GolemRoster {
  public record Member(String name, long chunk) {
    public static final Codec<Member> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("name").forGetter(Member::name),
        Codec.LONG.fieldOf("chunk").forGetter(Member::chunk)
    ).apply(instance, Member::new));
  }

  public static final Codec<GolemRoster> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Member.CODEC)
      .xmap(GolemRoster::new, GolemRoster::members);

  private final Map<UUID, Member> members;

  public GolemRoster() {
    this(Map.of());
  }

  private GolemRoster(Map<UUID, Member> members) {
    this.members = new HashMap<>(members);
  }

  /** Last known members remain recorded when their chunks unload. */
  public Map<UUID, Member> members() {
    return Map.copyOf(members);
  }

  public void remember(UUID id, String name, long chunk) {
    members.put(id, new Member(name, chunk));
  }

  public void remove(UUID id) {
    members.remove(id);
  }
}
