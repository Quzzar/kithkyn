package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;

/** Structural authoring contracts checked against the actual shipped Birch assets. */
class BirchAssetsTest {
  private static Path data() throws Exception {
    return Path.of(Objects.requireNonNull(BirchAssetsTest.class.getResource("/data/kithkyn")).toURI());
  }

  @Test
  void noBirchTemplateWritesAirOutsideItsAuthoredHorizontalBounds() throws Exception {
    try (var files = Files.list(data().resolve("structure"))) {
      for (Path file : files.filter(p -> p.getFileName().toString().contains("_birch_forest_")).toList()) {
        CompoundTag root = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
        ListTag palette = root.getList("palette", Tag.TAG_COMPOUND);
        ListTag blocks = root.getList("blocks", Tag.TAG_COMPOUND);
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (Tag value : blocks) {
          CompoundTag block = (CompoundTag)value;
          if (palette.getCompound(block.getInt("state")).getString("Name").equals("minecraft:air")) continue;
          ListTag pos = block.getList("pos", Tag.TAG_INT);
          minX = Math.min(minX, pos.getInt(0)); maxX = Math.max(maxX, pos.getInt(0));
          minZ = Math.min(minZ, pos.getInt(2)); maxZ = Math.max(maxZ, pos.getInt(2));
        }
        for (Tag value : blocks) {
          CompoundTag block = (CompoundTag)value;
          ListTag pos = block.getList("pos", Tag.TAG_INT);
          assertTrue(pos.getInt(0) >= minX && pos.getInt(0) <= maxX
              && pos.getInt(2) >= minZ && pos.getInt(2) <= maxZ,
              file.getFileName()+" still writes capture padding at "+pos);
        }
      }
    }
  }

  @Test
  void storehouseEntranceStepIsAboveTheGroundCourse() throws Exception {
    var definition = JsonParser.parseString(Files.readString(
        data().resolve("kithkyn/buildings/storehouse_birch_forest_1.json"))).getAsJsonObject();
    int sink = definition.get("sink").getAsInt();
    CompoundTag root = NbtIo.readCompressed(data().resolve("structure/storehouse_birch_forest_1.nbt"), NbtAccounter.unlimitedHeap());
    for (Tag value : root.getList("blocks", Tag.TAG_COMPOUND)) {
      CompoundTag block = (CompoundTag)value;
      CompoundTag state = root.getList("palette", Tag.TAG_COMPOUND).getCompound(block.getInt("state"));
      if (state.getString("Name").equals("minecraft:cobblestone_stairs")) {
        assertEquals(1, block.getList("pos", Tag.TAG_INT).getInt(1)-sink,
            "The entrance's first step must not be buried in the ground");
      }
    }
  }

  @Test
  void everyDeclaredAmenityExistsAndAllDynamicBedsHaveOneRole() throws Exception {
    int count = 0;
    int tallGrass = 0;
    Path dataRoot = data();
    try (var files = Files.list(dataRoot.resolve("kithkyn/buildings"))) {
      for (Path file : files.filter(p -> p.toString().endsWith("_birch_forest_1.json")
          || p.toString().endsWith("_birch_forest_2.json") || p.toString().endsWith("_birch_forest_3.json")).toList()) {
        BuildingInfo info = BuildingInfo.CODEC.parse(JsonOps.INSTANCE,
            JsonParser.parseString(Files.readString(file))).getOrThrow();
        assertEquals("birch_forest", info.getVariant());
        CompoundTag template = NbtIo.readCompressed(dataRoot.resolve("structure/"+info.getName()+".nbt"), NbtAccounter.unlimitedHeap());
        ListTag palette = template.getList("palette", Tag.TAG_COMPOUND);
        Map<BlockPos, CompoundTag> states = new HashMap<>();
        for (Tag value : template.getList("blocks", Tag.TAG_COMPOUND)) {
          CompoundTag block = (CompoundTag)value;
          ListTag p = block.getList("pos", Tag.TAG_INT);
          BlockPos pos = new BlockPos(p.getInt(0),p.getInt(1),p.getInt(2));
          assertNull(states.put(pos, palette.getCompound(block.getInt("state"))), "Duplicate cell "+file+pos);
          CompoundTag data = block.getCompound("nbt");
          assertFalse(data.contains("LootTable"), file.toString());
          assertTrue(data.getList("Items", Tag.TAG_COMPOUND).isEmpty(), file.toString());
          if (states.get(pos).getString("Name").equals("minecraft:tall_grass")) tallGrass++;
        }
        if (info.getName().equals("village_center_birch_forest_1")) {
          assertEquals(11, template.getList("size", Tag.TAG_INT).getInt(1), "Capture the new treetop tips");
          for (int x : new int[]{6, 22}) {
            for (int z : new int[]{6, 22}) {
              assertEquals("minecraft:birch_leaves", states.get(new BlockPos(x, 10, z)).getString("Name"));
            }
          }
          assertEquals("minecraft:chiseled_stone_bricks", states.get(new BlockPos(14, 8, 14)).getString("Name"));
          Set<BlockPos> bannerSlots = Set.of(new BlockPos(14,4,9), new BlockPos(9,4,14),
              new BlockPos(19,4,14), new BlockPos(14,4,19));
          assertEquals(bannerSlots, Set.copyOf(info.getVillageIdentitySlots().banners()));
          for (BlockPos pos : bannerSlots) {
            assertEquals("minecraft:white_wall_banner", states.get(pos).getString("Name"),
                "The restored outward flags remain dynamic village identity slots");
            assertEquals("minecraft:cobblestone_slab", states.get(pos.above()).getString("Name"));
            assertEquals("bottom", states.get(pos.above()).getCompound("Properties").getString("type"));
          }
          for (BlockPos pos : List.of(new BlockPos(14,4,10), new BlockPos(10,4,14),
              new BlockPos(18,4,14), new BlockPos(14,4,18))) {
            assertEquals("minecraft:chiseled_stone_bricks", states.get(pos).getString("Name"),
                "Preserve the approved restored exit lintels");
          }
          for (int x : new int[]{10, 18}) {
            for (int z : new int[]{10, 18}) {
              assertEquals("minecraft:candle", states.get(new BlockPos(x, 6, z)).getString("Name"));
            }
          }
        }
        if (info.getName().equals("storehouse_birch_forest_1")) {
          assertEquals(-1, info.getSink(), "The storehouse remains raised one block");
          for (int x = 4; x <= 5; x++) {
            for (int z = 5; z <= 9; z++) {
              CompoundTag floor = states.get(new BlockPos(x, 0, z));
              if (x == 4) {
                assertNull(floor, "The removed outer edge must not become a terrain-carving apron");
              } else {
                assertEquals("minecraft:air", floor.getString("Name"),
                    "Preserve the removed forecourt instead of restoring its old cobblestone");
              }
            }
          }
        }
        if (info.getName().equals("fishery_birch_forest_1")) {
          assertEquals(0, info.getSink(), "The fishery uses normal seating, not another one-block drop");
          for (int z = 12; z <= 14; z++) {
            assertEquals("minecraft:cobblestone", states.get(new BlockPos(3, 0, z)).getString("Name"));
            CompoundTag step = states.get(new BlockPos(4, 1, z));
            assertEquals("minecraft:cobblestone_stairs", step.getString("Name"));
            assertEquals("east", step.getCompound("Properties").getString("facing"));
            assertEquals("bottom", step.getCompound("Properties").getString("half"));
          }
          assertEquals("minecraft:air", states.get(new BlockPos(8, 2, 11)).getString("Name"));
          BlockPos barrel = new BlockPos(10, 2, 10);
          assertEquals("minecraft:barrel", states.get(barrel).getString("Name"));
          assertEquals(Set.of(barrel.asLong()), new HashSet<>(info.getContainerLocations()));
          assertEquals(Set.of(barrel.asLong()), info.getWorkLocations().keySet(),
              "The fisher must work at the relocated barrel, not the removed one");
        }
        if (info.getName().equals("mine_birch_forest_1")) {
          for (int z = 7; z <= 9; z++) {
            assertEquals("minecraft:air", states.get(new BlockPos(10,0,z)).getString("Name"));
            CompoundTag rear = states.get(new BlockPos(11,0,z));
            assertEquals(z == 9 ? "minecraft:mossy_cobblestone_stairs" : "minecraft:cobblestone_stairs", rear.getString("Name"));
            assertEquals("top", rear.getCompound("Properties").getString("half"));
            assertEquals("east", rear.getCompound("Properties").getString("facing"));
          }
        }
        for (long packed : info.getBedLocations()) {
          CompoundTag state = states.get(BlockPos.of(packed));
          assertNotNull(state, file+" bed "+BlockPos.of(packed));
          assertEquals("minecraft:white_bed", state.getString("Name"), file.toString());
          assertEquals("head", state.getCompound("Properties").getString("part"));
        }
        List<BlockPos> roles = new ArrayList<>(info.getVillageIdentitySlots().primaryBlocks());
        roles.addAll(info.getVillageIdentitySlots().secondaryBlocks());
        assertEquals(info.getBedLocations().size(), roles.size(), file.toString());
        assertEquals(roles.size(), new HashSet<>(roles).size());
        Set<Long> shared = new HashSet<>(info.getContainerLocations());
        for (long packed : info.getPersonalContainerLocations()) assertFalse(shared.contains(packed), file.toString());
        shared.addAll(info.getPersonalContainerLocations());
        for (long packed : shared) assertTrue(Set.of("minecraft:chest","minecraft:trapped_chest","minecraft:barrel")
            .contains(states.get(BlockPos.of(packed)).getString("Name")), file.toString());
        count++;
      }
    }
    assertEquals(22, count);
    assertEquals(32, tallGrass, "All 16 approved tall-grass plants, including the revised center, must survive the export");
  }

  @Test
  void stoneworksHasTheGroundLevelLeadInToItsRaisedWalkway() throws Exception {
    CompoundTag root = NbtIo.readCompressed(data().resolve("structure/stoneworks_birch_forest_1.nbt"), NbtAccounter.unlimitedHeap());
    for (Tag value : root.getList("blocks", Tag.TAG_COMPOUND)) {
      CompoundTag block = (CompoundTag)value;
      ListTag pos = block.getList("pos", Tag.TAG_INT);
      if (pos.getInt(0) == 3 && pos.getInt(1) == 1 && pos.getInt(2) == 5) {
        CompoundTag state = root.getList("palette", Tag.TAG_COMPOUND).getCompound(block.getInt("state"));
        assertEquals("minecraft:birch_stairs", state.getString("Name"));
        assertEquals("east", state.getCompound("Properties").getString("facing"));
        assertEquals("bottom", state.getCompound("Properties").getString("half"));
        return;
      }
    }
    fail("Missing ground-level access stair");
  }

  @Test
  void basementInteriorIsCarvedButSurroundingUndergroundTerrainIsNot() throws Exception {
    CompoundTag root = NbtIo.readCompressed(data().resolve("structure/village_center_birch_forest_1.nbt"), NbtAccounter.unlimitedHeap());
    Map<BlockPos,String> states = new HashMap<>();
    for (Tag value : root.getList("blocks",10)) {
      CompoundTag block=(CompoundTag)value; ListTag p=block.getList("pos",3);
      states.put(new BlockPos(p.getInt(0),p.getInt(1),p.getInt(2)),root.getList("palette",10).getCompound(block.getInt("state")).getString("Name"));
    }
    assertEquals("minecraft:air", states.get(new BlockPos(14,1,11)));
    assertEquals("minecraft:mossy_cobblestone", states.get(new BlockPos(14,0,11)));
    assertNull(states.get(new BlockPos(0,1,0)));
  }
}
