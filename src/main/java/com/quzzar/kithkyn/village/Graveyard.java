package com.quzzar.kithkyn.village;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.configuration.KithkynConfig;
import com.quzzar.kithkyn.entities.Kind;
import com.quzzar.kithkyn.entities.RealPerson;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * The register of the dead (docs/undead.md): every living villager who died,
 * saved whole the way the road saves a wanderer, with when, where and how it
 * ended. An undead village growing with nobody of its kind to recruit raises
 * someone from here before it conjures anyone: its own dead first, then the
 * nearest, then the most recent. The risen keep who they were and what they
 * remember; {@link Raising} says what of the old life stays in the grave.
 *
 * <p>Raiders are never buried, they were never alive, and the undead dying
 * again are gone for good. Lives in the village registry's save data, so it
 * is one register for the whole server, bounded by config.
 */
public final class Graveyard {

  /** One of the dead: who, when (game time), where, whose village, how, and everything they were. */
  public record Entry(String name, long diedAt, long where, String village, String cause, CompoundTag person) {
    public static final Codec<Entry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
        Codec.STRING.fieldOf("name").forGetter(Entry::name),
        Codec.LONG.fieldOf("died_at").forGetter(Entry::diedAt),
        Codec.LONG.fieldOf("where").forGetter(Entry::where),
        Codec.STRING.fieldOf("village").forGetter(Entry::village),
        Codec.STRING.fieldOf("cause").forGetter(Entry::cause),
        CompoundTag.CODEC.fieldOf("person").forGetter(Entry::person)
    ).apply(inst, Entry::new));
  }

  public static final Codec<List<Entry>> CODEC = Entry.CODEC.listOf();

  private final ArrayList<Entry> entries = new ArrayList<>();

  /** Marks the owning save data dirty; every change to the list runs it. */
  private final Runnable onChange;

  public Graveyard(Runnable onChange) {
    this.onChange = onChange;
  }

  /** Replaces the list with what was saved. Load only. */
  public void load(List<Entry> saved) {
    entries.clear();
    entries.addAll(saved);
  }

  /**
   * Writes a dying person into the register. Only the living are buried: a
   * raider was never alive, and the undead dying again are simply gone. What
   * they carried is left where they fell, not kept. Returns false when the
   * register keeps nobody (a cap of zero) or the person would not serialize.
   */
  public boolean bury(RealPerson person, String cause, long now) {
    int cap = KithkynConfig.GraveyardCap;
    if (cap <= 0 || person.getKind() != Kind.LIVING || person.isRaider()) {
      return false;
    }
    CompoundTag tag = new CompoundTag();
    if (!person.save(tag)) {
      Kithkyn.LOGGER.warn("'{}' could not be written into the register of the dead", person.getFullName());
      return false;
    }
    Village village = person.getVillage();
    entries.add(new Entry(person.getFullName(), now, person.blockPosition().asLong(),
        village == null ? "" : village.getName(), cause, Raising.withoutBelongings(tag)));
    while (entries.size() > cap) {
      Entry forgotten = entries.remove(0);
      Kithkyn.LOGGER.info("'{}' will not rise: the register of the dead remembers only so many", forgotten.name());
    }
    onChange.run();
    return true;
  }

  /**
   * Raises the dead best suited to the village, standing at {@code pos} but not
   * yet added to the level, or null when nobody can rise. One whose id is
   * still walking the world is left in the register; an entry that no longer
   * restores is dropped rather than tried again forever, like the road's.
   */
  @Nullable
  public RealPerson raise(ServerLevel level, BlockPos pos, Village village) {
    BlockPos center = village.centerPosition();
    for (Entry entry : ranked(entries, village.getName(), center == null ? pos : center)) {
      if (entry.person().hasUUID("UUID") && level.getEntity(entry.person().getUUID("UUID")) != null) {
        continue;
      }
      entries.remove(entry);
      onChange.run();
      RealPerson risen = restore(level, pos, entry);
      if (risen != null) {
        risen.logMemory("I remember how it ended: " + entry.cause() + ". I rose again at " + village.getName() + ".",
            Optional.empty());
        return risen;
      }
    }
    return null;
  }

  /** Whose turn it is to rise: the village's own dead, then the nearest, then the most recent. */
  static List<Entry> ranked(List<Entry> entries, String villageName, BlockPos center) {
    return entries.stream()
        .sorted(Comparator.<Entry, Boolean>comparing(entry -> !entry.village().equals(villageName))
            .thenComparingDouble(entry -> BlockPos.of(entry.where()).distSqr(center))
            .thenComparing(Comparator.comparingLong(Entry::diedAt).reversed()))
        .toList();
  }

  @Nullable
  private static RealPerson restore(ServerLevel level, BlockPos pos, Entry entry) {
    try {
      Entity entity = EntityType.create(Raising.prepare(entry.person()), level).orElse(null);
      if (entity instanceof RealPerson person) {
        person.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360F, 0F);
        person.setHealth(person.getMaxHealth());
        return person;
      }
      Kithkyn.LOGGER.error("'{}' could not be raised from the register of the dead and is lost", entry.name());
    } catch (RuntimeException error) {
      Kithkyn.LOGGER.error("'{}' could not be raised from the register of the dead and is lost", entry.name(), error);
    }
    return null;
  }

  /** Everyone in the register, oldest death first. */
  public List<Entry> entries() {
    return Collections.unmodifiableList(entries);
  }

  public int size() {
    return entries.size();
  }
}
