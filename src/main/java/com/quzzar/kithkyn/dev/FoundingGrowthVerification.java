package com.quzzar.kithkyn.dev;

import com.google.gson.JsonArray;
import com.mojang.serialization.JsonOps;
import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.savedata.PlacedBlockStore;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.buildings.BuildingInfo;
import com.quzzar.kithkyn.village.buildings.Buildings;
import com.quzzar.kithkyn.village.buildings.FoundingLayout;
import com.quzzar.kithkyn.village.buildings.InstantBuildStructure;
import com.quzzar.kithkyn.village.buildings.VillageStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Destructive fixtures run only in a disposable server with kithkyn.foundingGrowth.verify enabled. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class FoundingGrowthVerification {
  private static final String PREFIX = "[founding-growth-verify]";
  private static final String CENTER = "village_center_birch_forest_1";
  private static final int RADIUS = 100;
  private static int ticks;
  private static int stage;
  private static boolean finished;

  private FoundingGrowthVerification() { }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (!Boolean.getBoolean("kithkyn.foundingGrowth.verify") || finished || ++ticks < 40) return;
    ServerLevel level = event.getServer().overworld();
    try {
      level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, event.getServer());
      level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, event.getServer());
      level.setDayTime(6000);
      if (stage == 0) verifyTerrain(level, new BlockPos(3000, 160, 3000), false);
      else if (stage == 1) verifyTerrain(level, new BlockPos(3400, 160, 3000), true);
      else if (stage == 2) verifyFinalStarterRefusal(level, new BlockPos(3800, 160, 3000));
      else {
        finished = true;
        Kithkyn.LOGGER.info("{} RESULT PASS: flat and independently seated slope founding, four authored homes, "
            + "physical beds and sink, last-home stale protection, and all-or-nothing blocked-final-starter refusal", PREFIX);
        event.getServer().halt(false);
      }
      stage++;
    } catch (Exception | AssertionError failure) {
      finished = true;
      Kithkyn.LOGGER.error(PREFIX + " RESULT FAIL", failure);
      event.getServer().halt(false);
    }
  }

  private static void verifyTerrain(ServerLevel level, BlockPos site, boolean slope) throws ReflectiveOperationException {
    prepare(level, site, slope);
    Village village = probe(level);
    var plan = village.planFounding(site, Rotation.NONE, false).orElseThrow(() -> new AssertionError("No complete " + (slope ? "sloped" : "flat") + " plan"));
    check(plan.companions().size() == 6 && plan.structures().size() == 7, "starting_buildings did not supply four homes");
    check(plan.companions().stream().filter(structure -> structure.getBuilding().getInfo().getCategory().equals("house")).count() == 4,
        "Wrong starter house count");
    verifyUnpublished(village, site);
    List<Integer> grounds = plan.structures().stream().map(structure -> ground(structure).getY()).distinct().toList();
    check(slope ? grounds.size() > 1 : grounds.equals(List.of(site.getY() - 1)), "Incorrect terrain seating " + grounds);

    // A player protects the last home while the full plan waits. Earlier structures must remain unbuilt.
    InstantBuildStructure last = plan.companions().getLast();
    BoundingBox lastBounds = FoundingLayout.worldBounds(last);
    BlockPos chest = new BlockPos(lastBounds.minX() + 1, ground(last).getY() + 1, lastBounds.minZ() + 1);
    level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 2);
    PlacedBlockStore.get(level).markPlayerPlaced(chest);
    long protectedChecksum = checksum(level, site);
    check(!village.found(plan), "A stale last-home plan overwrote player storage");
    check(checksum(level, site) == protectedChecksum, "A rejected delayed commit changed terrain");
    verifyUnpublished(village, site);
    level.setBlock(chest, Blocks.AIR.defaultBlockState(), 2);
    PlacedBlockStore.get(level).clearPlaced(chest);

    check(village.found(plan), "Complete supported plan could not commit");
    check(village.getBuildings().size() == 7 && village.getTotalBeds() == 4, "Starter homes did not provide the bedless center's housing");
    for (var structure : plan.structures()) {
      var building = structure.getBuilding();
      for (long bed : building.getInfo().getBedLocations()) {
        BlockPos at = BlockPos.of(building.getOriginLocation()).offset(BlockPos.of(bed).rotate(building.getRotation()));
        check(level.getBlockState(at).getBlock() instanceof BedBlock, "Registered starting bed is physically absent " + at);
      }
    }
    List<BoundingBox> bounds = plan.structures().stream().map(FoundingLayout::worldBounds).toList();
    for (int first = 0; first < bounds.size(); first++) {
      for (int second = first + 1; second < bounds.size(); second++) {
        BoundingBox other = bounds.get(second);
        check(!bounds.get(first).intersects(other.minX() - 1, other.minZ() - 1, other.maxX() + 1, other.maxZ() + 1),
            "Starter footprints overlap or have no walking gap");
      }
    }
    Kithkyn.LOGGER.info("{} {} PASS: seven buildings, four physical house beds, ground courses {}, stale last-home protection", PREFIX,
        slope ? "SLOPE" : "FLAT", grounds);
  }

  private static void verifyFinalStarterRefusal(ServerLevel level, BlockPos site) throws ReflectiveOperationException {
    prepare(level, site, false);
    BuildingInfo original = Buildings.getByName(CENTER);
    var encoded = BuildingInfo.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow().getAsJsonObject();
    JsonArray prefix = encoded.getAsJsonArray("starting_buildings").deepCopy();
    prefix.remove(prefix.size() - 1);
    encoded.add("starting_buildings", prefix);
    BuildingInfo shortened = BuildingInfo.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
    replaceCenter(shortened);
    var firstFive = probe(level).planFounding(site, Rotation.NONE, false).orElseThrow();
    List<BoundingBox> allowed = firstFive.structures().stream().map(FoundingLayout::worldBounds).toList();
    var ownership = PlacedBlockStore.get(level);
    // Leave only the complete prefix's exact parcels and walking margins usable.
    for (int x = site.getX() - RADIUS; x <= site.getX() + RADIUS; x++) {
      for (int z = site.getZ() - RADIUS; z <= site.getZ() + RADIUS; z++) {
        int columnX = x;
        int columnZ = z;
        boolean reservedParcel = allowed.stream().anyMatch(box -> columnX >= box.minX() - 1 && columnX <= box.maxX() + 1
            && columnZ >= box.minZ() - 1 && columnZ <= box.maxZ() + 1);
        if (!reservedParcel) ownership.markPlayerPlaced(new BlockPos(x, site.getY(), z));
      }
    }
    check(probe(level).planFounding(site, Rotation.NONE, false).isPresent(), "Obstruction also prevents the earlier starter prefix");
    replaceCenter(original);
    Village rejected = probe(level);
    long before = checksum(level, site);
    check(rejected.planFounding(site, Rotation.NONE, false).isEmpty(), "A sixth companion fit where only five parcels exist");
    check(checksum(level, site) == before, "An incomplete founding preflight changed terrain");
    verifyUnpublished(rejected, site);
    Kithkyn.LOGGER.info("{} FINAL STARTER PASS: five companions fit, sixth refuses, terrain and live claims unchanged", PREFIX);
  }

  private static void replaceCenter(BuildingInfo info) {
    var registry = new HashMap<>(Buildings.allBuildings());
    registry.put(CENTER, info);
    Buildings.reload(registry);
  }

  private static Village probe(ServerLevel level) throws ReflectiveOperationException {
    Village village = new Village("Founding growth fixture");
    village.attach(level);
    village.setStyle(VillageStyle.BIRCH_FOREST);
    var random = Village.class.getDeclaredField("random");
    random.setAccessible(true);
    random.set(village, new Random(1847));
    return village;
  }

  private static BlockPos ground(InstantBuildStructure structure) {
    return BlockPos.of(structure.getBuilding().getOriginLocation()).above(structure.getBuilding().getInfo().getSink());
  }

  private static void prepare(ServerLevel level, BlockPos site, boolean slope) {
    for (int x = (site.getX() - RADIUS) >> 4; x <= (site.getX() + RADIUS) >> 4; x++) {
      for (int z = (site.getZ() - RADIUS) >> 4; z <= (site.getZ() + RADIUS) >> 4; z++) level.setChunkForced(x, z, true);
    }
    for (int x = site.getX() - RADIUS; x <= site.getX() + RADIUS; x++) {
      for (int z = site.getZ() - RADIUS; z <= site.getZ() + RADIUS; z++) {
        // Broad one-block terraces keep each footprint within normal earthwork limits.
        int surface = site.getY() - 1 + (slope ? Math.floorDiv(x - site.getX() + 12, 24) : 0);
        for (int y = site.getY() - 12; y <= surface; y++) level.setBlock(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState(), 2);
      }
    }
  }

  private static void verifyUnpublished(Village village, BlockPos site) {
    check(village.getBuildings().isEmpty() && village.getTownCenter() == null, "A trial published village buildings");
    for (int x = site.getX() - RADIUS; x <= site.getX() + RADIUS; x++) {
      for (int z = site.getZ() - RADIUS; z <= site.getZ() + RADIUS; z++) {
        check(!village.hasClaimed(new BlockPos(x, 0, z)), "A trial leaked claimed ground");
      }
    }
  }

  private static long checksum(ServerLevel level, BlockPos site) {
    long hash = 0xcbf29ce484222325L;
    for (BlockPos pos : BlockPos.betweenClosed(site.offset(-RADIUS, -12, -RADIUS), site.offset(RADIUS, 28, RADIUS))) {
      hash = (hash ^ Block.getId(level.getBlockState(pos))) * 0x100000001b3L;
    }
    return hash;
  }

  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
}
