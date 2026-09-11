package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Authored pit and entrance contracts for the private Jungle mine. */
@EnabledIfEnvironmentVariable(named = "KITHKYN_JUNGLE_DATAPACK", matches = ".+")
class JunglePlacementAssetsTest {
  private static Path data() {
    return Path.of(System.getenv("KITHKYN_JUNGLE_DATAPACK"), "data", "kithkyn");
  }

  @Test
  void mineKeepsTheApprovedSunkenWorkFloor() throws Exception {
    JsonObject definition = JsonParser.parseString(Files.readString(
        data().resolve("kithkyn/buildings/mine_jungle_1.json"))).getAsJsonObject();
    assertEquals(0, definition.get("sink").getAsInt());

    var worksite = definition.getAsJsonArray("worksites").get(0).getAsJsonObject().getAsJsonArray("pos");
    assertEquals(1, worksite.get(0).getAsInt());
    assertEquals(0, worksite.get(1).getAsInt());
    assertEquals(2, worksite.get(2).getAsInt());

    JsonObject entrance = definition.getAsJsonObject("mine_entrance");
    assertEquals("east", entrance.get("facing").getAsString());
    assertEquals(3, entrance.get("width").getAsInt());
    assertEquals(1, entrance.getAsJsonArray("offset").get(0).getAsInt());
    assertEquals(1, entrance.getAsJsonArray("offset").get(1).getAsInt());
    assertEquals(0, entrance.getAsJsonArray("offset").get(2).getAsInt());

    CompoundTag template = NbtIo.readCompressed(
        data().resolve("structure/mine_jungle_1.nbt"), NbtAccounter.unlimitedHeap());
    var size = template.getList("size", Tag.TAG_INT);
    assertEquals(6, size.getInt(0));
    assertEquals(6, size.getInt(1));
    assertEquals(5, size.getInt(2));
    var palette = template.getList("palette", Tag.TAG_COMPOUND);
    Map<Long, CompoundTag> cells = new HashMap<>();
    for (Tag value : template.getList("blocks", Tag.TAG_COMPOUND)) {
      CompoundTag block = (CompoundTag) value;
      var pos = block.getList("pos", Tag.TAG_INT);
      long key = new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2)).asLong();
      assertTrue(cells.put(key, block) == null, "Duplicate template cell " + pos);
    }

    assertBlock(cells, palette, 0, 0, 0, "minecraft:barrel");
    assertBlock(cells, palette, 0, 0, 4, "minecraft:barrel");
    for (int z = 1; z <= 3; z++) {
      assertBlock(cells, palette, 0, 0, z, "minecraft:jungle_stairs");
    }
    for (int x = 1; x <= 4; x++) {
      for (int z = 1; z <= 3; z++) {
        assertBlock(cells, palette, x, 0, z, "minecraft:air");
      }
    }
    assertBlock(cells, palette, 1, 0, 2, "minecraft:air");
    assertBlock(cells, palette, 2, 1, 2, "minecraft:air");
    assertBlock(cells, palette, 2, 2, 2, "minecraft:air");
  }

  private static void assertBlock(Map<Long, CompoundTag> cells, net.minecraft.nbt.ListTag palette,
      int x, int y, int z, String expected) {
    long key = new BlockPos(x, y, z).asLong();
    CompoundTag block = cells.get(key);
    assertTrue(block != null, "Missing template cell " + new BlockPos(x, y, z));
    assertEquals(expected, palette.getCompound(block.getInt("state")).getString("Name"),
        new BlockPos(x, y, z).toString());
  }
}
