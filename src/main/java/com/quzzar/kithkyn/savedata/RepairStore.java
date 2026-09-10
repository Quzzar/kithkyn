package com.quzzar.kithkyn.savedata;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.quzzar.kithkyn.Kithkyn;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

/** Bounded, confirmed damage for one dimension. Only block states survive, never inventories or entities. */
public final class RepairStore extends SavedData {
  public static final int MAX_REPAIRS = 8192;
  private static final String DATA_NAME = Kithkyn.MODID + "~repairs";
  private static final Factory<RepairStore> FACTORY = new Factory<>(RepairStore::new, RepairStore::load, null);
  private final LinkedHashMap<Long, Repair> repairs = new LinkedHashMap<>();
  private final LinkedHashMap<Long, Repair> pendingExplosions = new LinkedHashMap<>();
  private final Map<Long, Long> deferredUntil = new HashMap<>();

  /** The building revision prevents an old repair from reappearing in a replacement or upgraded building. */
  public record Repair(String village, UUID building, String revision, BlockPos pos, BlockState state,
      boolean ground) { }

  public static RepairStore get(ServerLevel level) {
    return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
  }

  /** Explosion lists describe candidates, not proof that a block was actually destroyed. */
  public void observeExplosion(Repair repair) {
    if (pendingExplosions.size() < MAX_REPAIRS) pendingExplosions.putIfAbsent(repair.pos().asLong(), repair);
  }

  /** Runs after the level tick, once explosions and other listeners have finished changing the world. */
  public void confirmExplosions(java.util.function.Predicate<BlockPos> isAir) {
    for (Repair repair : pendingExplosions.values()) {
      if (isAir.test(repair.pos())) remember(repair);
    }
    pendingExplosions.clear();
  }

  /** New observations never replace the state already owed at a position. */
  public void remember(Repair repair) {
    if (repairs.containsKey(repair.pos().asLong()) || repairs.size() >= MAX_REPAIRS) return;
    repairs.put(repair.pos().asLong(), repair);
    setDirty();
  }

  /** Player edits cancel pending as well as confirmed work, including an edit during an explosion tick. */
  public void forget(BlockPos pos) {
    pendingExplosions.remove(pos.asLong());
    deferredUntil.remove(pos.asLong());
    if (repairs.remove(pos.asLong()) != null) setDirty();
  }

  public boolean contains(Repair repair) {
    return repair.equals(repairs.get(repair.pos().asLong()));
  }

  /** Retry delays are transient: a restart may retry early, but never loses unpaid damage. */
  public void defer(BlockPos pos, long until) {
    deferredUntil.put(pos.asLong(), until);
  }

  /** A bounded scan skips deferred entries, so one unavailable item cannot hide affordable later repairs. */
  public List<Repair> candidates(String village, long now, int limit) {
    List<Repair> result = new ArrayList<>();
    for (Repair repair : repairs.values()) {
      if (repair.village().equals(village) && deferredUntil.getOrDefault(repair.pos().asLong(), 0L) <= now) {
        result.add(repair);
        if (result.size() >= limit) break;
      }
    }
    return result;
  }

  public static RepairStore load(CompoundTag tag, HolderLookup.Provider registries) {
    RepairStore store = new RepairStore();
    for (Tag raw : tag.getList("Repairs", Tag.TAG_COMPOUND)) {
      if (store.repairs.size() >= MAX_REPAIRS) break;
      CompoundTag entry = (CompoundTag) raw;
      if (!entry.hasUUID("Building")) continue;
      BlockState state = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK),
          entry.getCompound("State"));
      if (state.isAir()) continue;
      Repair repair = new Repair(entry.getString("Village"), entry.getUUID("Building"),
          entry.getString("Revision"), BlockPos.of(entry.getLong("Pos")), state, entry.getBoolean("Ground"));
      store.repairs.put(repair.pos().asLong(), repair);
    }
    return store;
  }

  @Override
  public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
    ListTag list = new ListTag();
    for (Repair repair : repairs.values()) {
      CompoundTag entry = new CompoundTag();
      entry.putString("Village", repair.village());
      entry.putUUID("Building", repair.building());
      entry.putString("Revision", repair.revision());
      entry.putLong("Pos", repair.pos().asLong());
      entry.put("State", NbtUtils.writeBlockState(repair.state()));
      entry.putBoolean("Ground", repair.ground());
      list.add(entry);
    }
    tag.put("Repairs", list);
    return tag;
  }
}
