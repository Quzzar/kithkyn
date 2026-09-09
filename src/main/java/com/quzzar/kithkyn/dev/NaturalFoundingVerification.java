package com.quzzar.kithkyn.dev;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.savedata.PlacedBlockStore;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.VillageGeneration;
import com.quzzar.kithkyn.village.VillageManager;
import com.quzzar.kithkyn.village.buildings.VillageStyle;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;

/** Real natural-search and delayed-plan checks, called only by the disposable Birch verifier. */
final class NaturalFoundingVerification {
    private static VillageGeneration generation;
    private static BlockPos anchor;
    private static int attempts;
    private static boolean passed;

    private NaturalFoundingVerification() { }

    static void start(ServerLevel level) {
        anchor = VillageGeneration.candidateSites(level.getSeed(), 20, 20).getFirst().atY(160);
        // A headless fixture has no real player keeping its explored neighborhood resident.
        for (int x = (anchor.getX() - 112) >> 4; x <= (anchor.getX() + 112) >> 4; x++) {
            for (int z = (anchor.getZ() - 112) >> 4; z <= (anchor.getZ() + 112) >> 4; z++) {
                level.setChunkForced(x, z, true);
            }
        }
        var ownership = PlacedBlockStore.get(level);
        for (BlockPos pos : BlockPos.betweenClosed(anchor.offset(-112, -7, -112), anchor.offset(112, 18, 112))) {
            ownership.clearPlaced(pos);
            level.setBlock(pos, pos.getY() < 160 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
        }
        Village probe = new Village("Unplaced probe");
        probe.attach(level);
        probe.setStyle(VillageStyle.BIRCH_FOREST);
        var plan = probe.planFounding(anchor, Rotation.CLOCKWISE_90, false).orElseThrow();
        check(probe.getBuildings().isEmpty() && !probe.hasClaimed(anchor), "Preflight published claims/buildings");

        // A player edits after the preflight: the final check must refuse without touching it.
        level.setBlock(anchor, Blocks.CHEST.defaultBlockState(), 2);
        ownership.markPlayerPlaced(anchor);
        check(!probe.found(plan), "Stale plan overwrote a newly protected chest");
        check(level.getBlockState(anchor).is(Blocks.CHEST) && probe.getBuildings().isEmpty()
            && !probe.hasClaimed(anchor), "Rejected commit changed the world or claims");

        BlockPos far = anchor.offset(10000, 0, 10000);
        check(!level.isLoaded(far), "Unloaded fixture is already loaded");
        check(probe.planFounding(far, Rotation.NONE, false).isEmpty(), "Unloaded preflight was accepted");
        check(!level.isLoaded(far), "Natural preflight loaded unexplored chunks");
        generation = new VillageGeneration();
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                check(level.isLoaded(anchor.offset(x * 16, 0, z * 16)), "Fixture exploration footprint is unloaded");
            }
        }
    }

    static void tick(ServerLevel level) {
        if (generation == null || passed) return;
        // Repeated observers exercise one shared per-world cursor, as multiple nearby players do.
        generation.searchAround(level, List.of(new ChunkPos(anchor), new ChunkPos(anchor)));
        attempts++;
        var manager = VillageManager.get(level);
        var found = manager.getVillages().values().stream()
            .filter(village -> village.getTownCenter() != null
                && VillageGeneration.tooClose(anchor, village.getCampfirePosition())).toList();
        check(found.size() <= 1, "Nearby observers created duplicate natural villages");
        if (!found.isEmpty()) {
            Village village = found.getFirst();
            check(village.getStyle() == VillageStyle.BIRCH_FOREST, "Natural fallback lost biome selection");
            check(village.getBuildings().size() == 3 && village.getTotalBeds() == 4, "Incomplete natural founding");
            check(village.getCampfirePosition().getX() != anchor.getX()
                || village.getCampfirePosition().getZ() != anchor.getZ(), "Rejected anchor was reused");
            check(level.getBlockState(anchor).is(Blocks.CHEST), "Fallback removed the protected anchor chest");
            check(!manager.naturalSiteAvailable(village.getCampfirePosition().above(300)), "Separation depends on Y");
            passed = true;
            Kithkyn.LOGGER.info("[natural-founding-verify] RESULT PASS: protected anchor fallback, delayed-plan safety, loaded-only search and biome-selected founding");
        }
        check(attempts < 250, "Natural fallback never completed");
    }

    static void requirePassed() {
        check(passed, "Natural founding verification did not finish");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
