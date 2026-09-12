package com.quzzar.kithkyn.savedata;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.Graveyard;
import com.quzzar.kithkyn.village.WandererPool;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * The server-wide village registry, persisted as codec-backed NBT in the
 * overworld's data storage. This object IS the village manager: access it
 * through {@link com.quzzar.kithkyn.village.VillageManager#get}.
 */
public class VillageManagerSaveData extends SavedData {

    /** Bump when the layout changes; older formats (including the pre-codec Java-serialized blob) start fresh. */
    public static final int FORMAT_VERSION = 2;

    private static final Codec<Map<String, Village>> VILLAGES_CODEC = Codec.unboundedMap(Codec.STRING, Village.CODEC);

    public static final SavedData.Factory<VillageManagerSaveData> FACTORY = new SavedData.Factory<>(
            VillageManagerSaveData::new, VillageManagerSaveData::load, null);

    private final Map<String, Village> villages = new HashMap<>();

    /** Everyone on the road beyond the horizon: one list for the whole server (docs/population-and-labor.md). */
    private final WandererPool wanderers = new WandererPool(this::setDirty);
    private final Graveyard graveyard = new Graveyard(this::setDirty);

    // Runtime-only: the level this registry belongs to, re-attached on access.
    private ServerLevel level;

    // Runtime-only: sites whose founding name is still being generated, so the
    // generation tick cannot found the same site twice while the name is in
    // flight. Touched on the server thread only.
    private final Set<Long> pendingFoundings = new HashSet<>();
    private final com.quzzar.kithkyn.village.VillageGeneration generation =
            new com.quzzar.kithkyn.village.VillageGeneration();

    /** Natural search state is scoped to this world and shared across its players. */
    public void generateVillages(ServerLevel level) {
        generation.tick(level);
    }

    public boolean naturalSiteAvailable(BlockPos location) {
        return naturalSiteAvailable(location, null);
    }

    /** Pending names reserve horizontal space just like already founded villages. */
    private boolean naturalSiteAvailable(BlockPos location, Long ownReservation) {
        for (long pending : pendingFoundings) {
            if (ownReservation != null && pending == ownReservation.longValue()) continue;
            if (com.quzzar.kithkyn.village.VillageGeneration.tooClose(location, BlockPos.of(pending))) return false;
        }
        for (Village village : villages.values()) {
            if (village.getTownCenter() != null && com.quzzar.kithkyn.village.VillageGeneration.tooClose(location,
                    village.getCenterPosition())) return false;
        }
        return true;
    }

    /** Names only viable sites, then rechecks the exact prepared geometry before committing. */
    public boolean registerNaturalVillage(BlockPos location,
            com.quzzar.kithkyn.village.buildings.VillageStyle style, com.quzzar.kithkyn.entities.Kind kind,
            Village.FoundingPlan plan, java.util.function.Consumer<Boolean> onComplete) {
        if (level == null || !naturalSiteAvailable(location)) return false;
        long reservation = location.asLong();
        if (!pendingFoundings.add(reservation)) return false;
        ServerLevel serverLevel = level;
        com.quzzar.kithkyn.village.VillageNamer.requestFoundingName(serverLevel, style, name -> {
            boolean founded = false;
            try {
                if (naturalSiteAvailable(location, reservation)) {
                    var identity = com.quzzar.kithkyn.village.VillageIdentity.generate(name, serverLevel.getRandom());
                    Village village = new Village(identity);
                    village.setStyle(style);
                    village.setKind(kind);
                    village.attach(serverLevel);
                    founded = village.found(plan);
                    if (founded) {
                        villages.put(village.getID(), village);
                        setDirty();
                    }
                }
                if (!founded) Kithkyn.LOGGER.debug("Natural founding at {} changed while naming; resuming site search", location);
            } finally {
                pendingFoundings.remove(reservation);
                onComplete.accept(founded);
            }
        });
        return true;
    }

    public VillageManagerSaveData() {
    }

    public static VillageManagerSaveData load(CompoundTag tag, HolderLookup.Provider registries) {
        VillageManagerSaveData data = new VillageManagerSaveData();

        int format = tag.getInt("Format");
        if (format < FORMAT_VERSION) {
            if (tag.contains("villages")) {
                Kithkyn.LOGGER.error("=======================================================================");
                Kithkyn.LOGGER.error("Found village data in the old Java-serialized format. That format is");
                Kithkyn.LOGGER.error("no longer supported and cannot be migrated; starting with no villages.");
                Kithkyn.LOGGER.error("(Existing villagers and buildings in the world are unaffected, but they");
                Kithkyn.LOGGER.error("no longer belong to a registered village.)");
                Kithkyn.LOGGER.error("=======================================================================");
            }
            return data;
        }

        VILLAGES_CODEC.parse(NbtOps.INSTANCE, tag.get("Villages"))
                .resultOrPartial(error -> Kithkyn.LOGGER.error("Failed to load village data: {}", error))
                .ifPresent(data.villages::putAll);
        if (tag.contains("Wanderers")) {
            WandererPool.CODEC.parse(NbtOps.INSTANCE, tag.get("Wanderers"))
                    .resultOrPartial(error -> Kithkyn.LOGGER.error("Failed to load the wanderers beyond the horizon: {}", error))
                    .ifPresent(data.wanderers::load);
        }
        if (tag.contains("TheDead")) {
            Graveyard.CODEC.parse(NbtOps.INSTANCE, tag.get("TheDead"))
                    .resultOrPartial(error -> Kithkyn.LOGGER.error("Failed to load the register of the dead: {}", error))
                    .ifPresent(data.graveyard::load);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("Format", FORMAT_VERSION);
        Tag encoded = VILLAGES_CODEC.encodeStart(NbtOps.INSTANCE, Map.copyOf(villages))
                .resultOrPartial(error -> Kithkyn.LOGGER.error("Failed to save village data: {}", error))
                .orElse(null);
        if (encoded != null) {
            tag.put("Villages", encoded);
        }
        Tag road = WandererPool.CODEC.encodeStart(NbtOps.INSTANCE, List.copyOf(wanderers.entries()))
                .resultOrPartial(error -> Kithkyn.LOGGER.error("Failed to save the wanderers beyond the horizon: {}", error))
                .orElse(null);
        if (road != null) {
            tag.put("Wanderers", road);
        }
        Tag dead = Graveyard.CODEC.encodeStart(NbtOps.INSTANCE, List.copyOf(graveyard.entries()))
                .resultOrPartial(error -> Kithkyn.LOGGER.error("Failed to save the register of the dead: {}", error))
                .orElse(null);
        if (dead != null) {
            tag.put("TheDead", dead);
        }
        return tag;
    }

    /** Re-binds this registry (and every village in it) to its level. Called by VillageManager on access. */
    public void attach(ServerLevel level) {
        if (this.level != level) {
            this.level = level;
            villages.values().forEach(village -> village.attach(level));
        }
    }

    public boolean registerVillage(ServerLevelAccessor levelAccess, BlockPos location) {
        return registerVillage(levelAccess, location, null);
    }

    /**
     * Founds a village at the site in the given style, or, with none given, in
     * the style the biome there calls for ({@link com.quzzar.kithkyn.village.buildings.VillageStyle#fromBiome}).
     */
    public boolean registerVillage(ServerLevelAccessor levelAccess, BlockPos location,
            @javax.annotation.Nullable com.quzzar.kithkyn.village.buildings.VillageStyle style) {
        return registerVillage(levelAccess, location, style, com.quzzar.kithkyn.entities.Kind.LIVING);
    }

    /**
     * Founds a village of the given kind: a manual founding is living unless
     * the command says undead, because a placed village is a deliberate act
     * and a surprise kind would be a bug report (docs/undead.md).
     *
     * <p>A manual founding keeps the same separation from other villages that a
     * natural one does. On 2026-09-12 three foundings landed within a few blocks
     * of each other near Sorevia: the second began while the first was still
     * being cleared (#138), and the third logged a POI conflict on the first's
     * blocks. Returns false, founding nothing, when the site is within
     * {@code VillageGeneration.MIN_SEPARATION_BLOCKS} of a standing village
     * centre or a founding still being named.
     */
    public boolean registerVillage(ServerLevelAccessor levelAccess, BlockPos location,
            @javax.annotation.Nullable com.quzzar.kithkyn.village.buildings.VillageStyle style,
            com.quzzar.kithkyn.entities.Kind kind) {
        if (!naturalSiteAvailable(location)) {
            Kithkyn.LOGGER.info("Refused to found a village at {}: within {} blocks of another village or a pending founding",
                    location.toShortString(), com.quzzar.kithkyn.village.VillageGeneration.MIN_SEPARATION_BLOCKS);
            return false;
        }
        // One name for life (#60): the LLM name is requested BEFORE the camp is
        // placed, and founding runs when it lands moments later, so the village
        // never carries a provisional name. The wait opens a short window in
        // which VillageGeneration re-offers the same site every tick (the map
        // holds no village there yet), so in-flight sites are remembered and
        // skipped.
        long site = location.asLong();
        if (!pendingFoundings.add(site)) {
            return false;
        }
        ServerLevel serverLevel = level != null ? level : levelAccess.getLevel();
        var selectedStyle = style != null ? style
                : com.quzzar.kithkyn.village.buildings.VillageStyle.fromBiome(
                        serverLevel.getBiome(location), serverLevel.getSeed(), location);
        com.quzzar.kithkyn.village.VillageNamer.requestFoundingName(serverLevel, selectedStyle, name -> {
            try {
                var identity = com.quzzar.kithkyn.village.VillageIdentity.generate(name, serverLevel.getRandom());
                Village village = new Village(identity);
                village.setStyle(selectedStyle);
                village.setKind(kind);
                village.attach(serverLevel);
                village.initNew(location);
                if (village.getTownCenter() == null) return;
                villages.put(village.getID(), village);
                setDirty();
            } finally {
                pendingFoundings.remove(site);
            }
        });
        return true;
    }

    /** Runs every second, driven by the overworld tick handler. */
    public void tick(ServerLevel level) {
        attach(level);
        villages.values().forEach(village -> village.update(level));
        com.quzzar.kithkyn.raids.UndeadRaids.scanGrudges(level, villages.values());
        if (!villages.isEmpty()) {
            setDirty();
        }
    }

    public Village getVillage(String uuid) {
        return villages.get(uuid);
    }

    /**
     * Unmakes a village: its loaded people, the blocks it placed, and its record
     * ({@link Village#demolish}). Dev tooling for test villages.
     */
    public Village.Removal removeVillage(ServerLevel level, Village village) {
        Village.Removal removal = village.demolish(level);
        com.quzzar.kithkyn.village.VillageChunkLoader.release(level, village.getID());
        villages.remove(village.getID());
        setDirty();
        return removal;
    }

    /** The village whose town center is closest to the given position, or null if none exist. */
    public Village getNearestVillage(BlockPos pos) {
        Village nearest = null;
        double nearestDistSqr = Double.MAX_VALUE;
        for (Village village : villages.values()) {
            if (village.getTownCenter() == null) {
                continue;
            }
            double distSqr = pos.distSqr(BlockPos.of(village.getTownCenter().getCenterLocation()));
            if (distSqr < nearestDistSqr) {
                nearestDistSqr = distSqr;
                nearest = village;
            }
        }
        return nearest;
    }

    public Map<String, Village> getVillages() {
        return villages;
    }

    public WandererPool getWanderers() {
        return wanderers;
    }

    public Graveyard getGraveyard() {
        return graveyard;
    }

}
