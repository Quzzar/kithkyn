package com.quzzar.kithkyn.village;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.configuration.KithkynConfig;
import com.quzzar.kithkyn.village.buildings.VillageStyle;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Founds living villages on explored land, independently of vanilla biome eligibility.
 * Each seeded region tries a bounded set of nearby sites through the ordinary founding planner.
 * State belongs to the level's village manager, never to a static cross-world cache.
 */
public final class VillageGeneration {
    private static final int SPACING = 34;
    private static final int SEPARATION = 8;
    private static final long SALT = 0x76_69_6C_6CL;
    public static final int MIN_SEPARATION_BLOCKS = SPACING * 16 / 2;
    private static final int FOOTPRINT_CHUNK_RADIUS = 3;
    private static final int MAX_PROBES = 2;
    private static final int MAX_CANDIDATE_CHECKS = 32;

    private final Map<Long, Search> searches = new HashMap<>();
    private int regionCursor;

    /** At most two exact terrain plans per second across all players, with no chunk generation. */
    public void tick(ServerLevel level) {
        if (!KithkynConfig.GenerateVillages || level.players().isEmpty()) return;
        searchAround(level, level.players().stream().map(player -> player.chunkPosition()).toList());
    }

    /** Shared exploration entry point, also exercised by the disposable-server verifier. */
    public void searchAround(ServerLevel level, List<ChunkPos> observers) {
        var manager = VillageManager.get(level);
        var regions = new LinkedHashSet<Long>();
        for (var observer : observers) {
            int regionX = Math.floorDiv(observer.x, SPACING);
            int regionZ = Math.floorDiv(observer.z, SPACING);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    regions.add(ChunkPos.asLong(regionX + dx, regionZ + dz));
                }
            }
        }
        List<Long> ordered = new ArrayList<>(regions);
        if (ordered.isEmpty()) return;
        int probes = 0;
        int start = Math.floorMod(regionCursor++, ordered.size());
        for (int checked = 0; checked < Math.min(MAX_CANDIDATE_CHECKS, ordered.size()); checked++) {
            long key = ordered.get((start + checked) % ordered.size());
            Search search = searches.computeIfAbsent(key, ignored ->
                new Search(candidateSites(level.getSeed(), ChunkPos.getX(key), ChunkPos.getZ(key))));
            BlockPos column = search.next();
            if (column == null || !footprintLoaded(level, column)) continue;
            // Unavailable land may become free after an in-flight founding fails, so defer it.
            if (!manager.naturalSiteAvailable(column)) continue;
            BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
            if (level.getFluidState(surface.below()).is(FluidTags.WATER)) {
                search.reject(column);
                continue;
            }
            Village probe = new Village("Unplaced founding probe");
            probe.attach(level);
            VillageStyle style = VillageStyle.fromBiome(level.getBiome(surface), level.getSeed(), surface);
            probe.setStyle(style);
            com.quzzar.kithkyn.entities.Kind kind = com.quzzar.kithkyn.entities.Kind.forNaturalFounding(level.getSeed(), surface);
            Rotation rotation = Rotation.values()[RandomSource.create(level.getSeed() ^ column.asLong())
                .nextInt(Rotation.values().length)];
            var plan = probe.planFounding(surface, rotation, false);
            probes++;
            if (plan.isEmpty()) {
                search.reject(column);
            } else {
                search.reserve();
                boolean reserved = manager.registerNaturalVillage(surface, style, kind, plan.get(), success -> {
                    search.complete(success);
                    if (success) Kithkyn.LOGGER.info("Natural {} village founded at {} after nearby-site search", kind.id(), surface);
                });
                if (!reserved) search.complete(false);
            }
            if (probes >= MAX_PROBES) return;
        }
    }

    /** Stable region anchor; retaining the old seed and grid keeps exploration density familiar. */
    static ChunkPos villageChunk(long seed, int regionX, int regionZ) {
        RandomSource random = RandomSource.create(seed + regionX * 341873128712L + regionZ * 132897987541L + SALT);
        return new ChunkPos(regionX * SPACING + random.nextInt(SPACING - SEPARATION),
            regionZ * SPACING + random.nextInt(SPACING - SEPARATION));
    }

    /** Read-only candidate catalog for a seeded region, useful for generation diagnostics. */
    public static List<BlockPos> candidateSites(long seed, int regionX, int regionZ) {
        return candidates(villageChunk(seed, regionX, regionZ));
    }

    /** Twenty-five sites within 64 blocks per axis, nearest first; no unbounded terrain sweep. */
    static List<BlockPos> candidates(ChunkPos anchor) {
        List<BlockPos> result = new ArrayList<>();
        for (int dx = -64; dx <= 64; dx += 32) {
            for (int dz = -64; dz <= 64; dz += 32) {
                result.add(new BlockPos(anchor.getMiddleBlockX() + dx, 0, anchor.getMiddleBlockZ() + dz));
            }
        }
        BlockPos center = anchor.getMiddleBlockPosition(0);
        result.sort(Comparator.comparingDouble((BlockPos pos) -> pos.distSqr(center))
            .thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ));
        return List.copyOf(result);
    }

    /** Horizontal separation ignores hills, basements and the Y used for preliminary probes. */
    public static boolean tooClose(BlockPos first, BlockPos second) {
        double dx = (double) first.getX() - second.getX();
        double dz = (double) first.getZ() - second.getZ();
        return dx * dx + dz * dz < (double) MIN_SEPARATION_BLOCKS * MIN_SEPARATION_BLOCKS;
    }

    static boolean footprintLoaded(ServerLevel level, BlockPos center) {
        for (int cx = -FOOTPRINT_CHUNK_RADIUS; cx <= FOOTPRINT_CHUNK_RADIUS; cx++) {
            for (int cz = -FOOTPRINT_CHUNK_RADIUS; cz <= FOOTPRINT_CHUNK_RADIUS; cz++) {
                if (!level.isLoaded(center.offset(cx * 16, 0, cz * 16))) return false;
            }
        }
        return true;
    }

    /** Deferred/unloaded sites remain eligible; only a tested refusal exhausts a candidate. */
    static final class Search {
        private final List<BlockPos> candidates;
        private final BitSet rejected = new BitSet();
        private int cursor;
        private boolean pending;
        private boolean settled;

        Search(List<BlockPos> candidates) {
            this.candidates = candidates;
        }

        BlockPos next() {
            if (pending || settled) return null;
            for (int checked = 0; checked < candidates.size(); checked++) {
                int index = cursor;
                cursor = (cursor + 1) % candidates.size();
                if (!rejected.get(index)) return candidates.get(index);
            }
            return null;
        }

        void reject(BlockPos candidate) {
            int index = candidates.indexOf(candidate);
            if (index >= 0) rejected.set(index);
        }

        void reserve() { pending = true; }

        void complete(boolean success) {
            pending = false;
            settled = success;
        }
    }
}
