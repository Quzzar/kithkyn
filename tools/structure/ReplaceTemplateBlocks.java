import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.Bootstrap;

/**
 * Swaps the block at named cells of structure templates with Minecraft's own typed NBT reader and
 * writer, so nothing else in the file changes. Plan: a JSON array of
 * {"path": ..., "replace": [{"pos": [x,y,z], "name": "minecraft:acacia_stairs"}, ...]}. A replacement
 * keeps the cell's block properties (facing, half, shape) unless it carries its own "properties"
 * object. Palette entries nothing uses any more are dropped.
 * Run like the other tools here: java -cp <moddev classpath> tools/structure/ReplaceTemplateBlocks.java plan.json
 */
public final class ReplaceTemplateBlocks {
  public static void main(String[] args) throws Exception {
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
    JsonArray plan = JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonArray();
    for (JsonElement element : plan) {
      JsonObject spec = element.getAsJsonObject();
      Path path = Path.of(spec.get("path").getAsString());
      Map<String, JsonObject> replacements = new HashMap<>();
      for (JsonElement item : spec.getAsJsonArray("replace")) {
        JsonObject replacement = item.getAsJsonObject();
        JsonArray c = replacement.getAsJsonArray("pos");
        replacements.put(c.get(0).getAsInt() + "," + c.get(1).getAsInt() + "," + c.get(2).getAsInt(), replacement);
      }
      CompoundTag root = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
      if (root.contains("palettes")) throw new IllegalStateException(path + ": multi-palette templates are not supported");
      ListTag palette = root.getList("palette", Tag.TAG_COMPOUND);
      ListTag blocks = root.getList("blocks", Tag.TAG_COMPOUND);
      int replaced = 0;
      for (Tag item : blocks) {
        CompoundTag block = (CompoundTag) item;
        ListTag pos = block.getList("pos", Tag.TAG_INT);
        JsonObject replacement = replacements.get(pos.getInt(0) + "," + pos.getInt(1) + "," + pos.getInt(2));
        if (replacement == null) continue;
        CompoundTag current = palette.getCompound(block.getInt("state"));
        CompoundTag desired = new CompoundTag();
        desired.putString("Name", replacement.get("name").getAsString());
        if (replacement.has("properties")) {
          CompoundTag properties = new CompoundTag();
          for (Map.Entry<String, JsonElement> e : replacement.getAsJsonObject("properties").entrySet()) properties.putString(e.getKey(), e.getValue().getAsString());
          if (!properties.isEmpty()) desired.put("Properties", properties);
        } else if (current.contains("Properties")) {
          desired.put("Properties", current.getCompound("Properties").copy());
        }
        int state = -1;
        for (int i = 0; i < palette.size() && state < 0; i++) if (palette.getCompound(i).equals(desired)) state = i;
        if (state < 0) { palette.add(desired); state = palette.size() - 1; }
        block.putInt("state", state);
        replaced++;
      }
      if (replaced != replacements.size()) throw new IllegalStateException(path + ": expected " + replacements.size() + " replacements, made " + replaced);
      // Drop palette entries nothing references any more and renumber the rest in place.
      int[] remap = new int[palette.size()];
      java.util.Arrays.fill(remap, -1);
      ListTag compact = new ListTag();
      for (Tag item : blocks) {
        CompoundTag block = (CompoundTag) item;
        int state = block.getInt("state");
        if (remap[state] < 0) { remap[state] = compact.size(); compact.add(palette.getCompound(state).copy()); }
        block.putInt("state", remap[state]);
      }
      root.put("palette", compact);
      NbtIo.writeCompressed(root, path);
      if (!NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap()).equals(root)) throw new IllegalStateException("round-trip mismatch for " + path);
      System.out.println("replaced " + replaced + " in " + path + " (palette " + remap.length + " -> " + compact.size() + ")");
    }
  }
}
