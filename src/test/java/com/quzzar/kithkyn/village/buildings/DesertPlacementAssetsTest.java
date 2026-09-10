package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Authored ground and entrance contracts for the private Desert catalog. */
@EnabledIfEnvironmentVariable(named = "KITHKYN_DESERT_DATAPACK", matches = ".+")
class DesertPlacementAssetsTest {
  private static Path data() {
    return Path.of(System.getenv("KITHKYN_DESERT_DATAPACK"), "data", "kithkyn");
  }

  private static JsonObject definition(String name) throws Exception {
    return JsonParser.parseString(Files.readString(data().resolve("kithkyn/buildings/" + name + ".json")))
        .getAsJsonObject();
  }

  @Test
  void lowestStepsAndFenceBasesRemainAboveTerrain() throws Exception {
    for (String name : List.of("mine_desert_1", "storehouse_desert_1", "house_desert_2",
        "house_desert_2__medium_house_2")) {
      assertEquals(-1, definition(name).get("sink").getAsInt(), name);
    }
  }

  @Test
  void desertShaftFitsCoveredLaneRatherThanCuttingAcrossItsFence() throws Exception {
    var definition = definition("mine_desert_1");
    var entrance = definition.getAsJsonObject("mine_entrance");
    var station = definition.getAsJsonArray("work_stations").get(0).getAsJsonObject().getAsJsonArray("pos");
    var offset = entrance.getAsJsonArray("offset");
    assertEquals("south", entrance.get("facing").getAsString());
    assertEquals(3, entrance.get("width").getAsInt());
    assertEquals(4, station.get(0).getAsInt() + offset.get(0).getAsInt());
    assertEquals(1, station.get(1).getAsInt() + offset.get(1).getAsInt());
    assertEquals(5, station.get(2).getAsInt() + offset.get(2).getAsInt());
    assertEquals(0, station.get(1).getAsInt(), "Worker stands on exposed terrain beside the mouth");
  }

  @Test
  void marketHasOneEntranceCarpetAndNoDuplicateTemplateCells() throws Exception {
    var template = NbtIo.readCompressed(data().resolve("structure/market_desert_1.nbt"), NbtAccounter.unlimitedHeap());
    var palette = template.getList("palette", Tag.TAG_COMPOUND);
    var positions = new java.util.HashSet<String>();
    int entranceCarpets = 0;
    for (Tag value : template.getList("blocks", Tag.TAG_COMPOUND)) {
      var block = (net.minecraft.nbt.CompoundTag) value;
      var pos = block.getList("pos", Tag.TAG_INT);
      assertTrue(positions.add(pos.toString()), "Duplicate cell " + pos);
      if (pos.getInt(1) == 1 && palette.getCompound(block.getInt("state")).getString("Name").endsWith("_carpet")) {
        entranceCarpets++;
      }
    }
    assertEquals(1, entranceCarpets);
  }
}
