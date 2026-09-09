package com.quzzar.kithkyn.village;

import static org.junit.jupiter.api.Assertions.*;
import java.util.HashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class VillageGenerationTest {
    @Test
    void candidatesAreStableUniqueBoundedAndNearestFirstInNegativeRegions() {
        for (int x : List.of(-30, -1, 0, 20)) {
            ChunkPos anchor = VillageGeneration.villageChunk(73L, x, -2);
            List<BlockPos> sites = VillageGeneration.candidateSites(73L, x, -2);
            assertEquals(sites, VillageGeneration.candidateSites(73L, x, -2));
            assertEquals(25, sites.size());
            assertEquals(25, new HashSet<>(sites).size());
            assertEquals(anchor.getMiddleBlockPosition(0), sites.getFirst());
            double previous = -1;
            for (BlockPos site : sites) {
                assertTrue(Math.abs(site.getX() - sites.getFirst().getX()) <= 64);
                assertTrue(Math.abs(site.getZ() - sites.getFirst().getZ()) <= 64);
                double distance = site.distSqr(sites.getFirst());
                assertTrue(distance >= previous);
                previous = distance;
            }
            assertEquals(x, Math.floorDiv(anchor.x, 34));
            assertEquals(-2, Math.floorDiv(anchor.z, 34));
        }
    }

    @Test
    void rejectedAnchorFallsBackAndDeferredSitesAreNotExhausted() {
        List<BlockPos> sites = VillageGeneration.candidateSites(1L, 0, 0);
        var search = new VillageGeneration.Search(sites);
        assertEquals(sites.getFirst(), search.next());
        search.reject(sites.getFirst());
        for (int i = 1; i < sites.size(); i++) assertEquals(sites.get(i), search.next());
        assertEquals(sites.get(1), search.next(), "Unloaded/deferred sites must remain eligible");
        sites.forEach(search::reject);
        assertNull(search.next(), "Finite rejection set must stop the search");
    }

    @Test
    void reservationBlocksAnotherProbeAndFailedCallbackResumesWithoutRerolling() {
        List<BlockPos> sites = VillageGeneration.candidateSites(1L, 0, 0);
        var search = new VillageGeneration.Search(sites);
        search.next();
        search.reserve();
        assertNull(search.next());
        search.complete(false);
        assertEquals(sites.get(1), search.next());
        search.reserve();
        search.complete(true);
        assertNull(search.next());
    }

    @Test
    void separationIsHorizontalAndBoundaryIsInclusive() {
        assertTrue(VillageGeneration.tooClose(new BlockPos(-100, -64, 10), new BlockPos(-100, 319, 10)));
        assertTrue(VillageGeneration.tooClose(BlockPos.ZERO, new BlockPos(271, 300, 0)));
        assertFalse(VillageGeneration.tooClose(BlockPos.ZERO, new BlockPos(272, 0, 0)));
        assertFalse(VillageGeneration.tooClose(BlockPos.ZERO, new BlockPos(200, 0, 200)));
    }
}
