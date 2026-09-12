import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.Bootstrap;

/**
 * Derives one structure template from another with Minecraft's own typed NBT reader and writer.
 * Plan: a JSON array of {"source": ..., "output": ..., "size": [x,y,z], "shift": [dx,dy,dz],
 * "remove": [[x,y,z], ...], "set": [{"pos": [x,y,z], "name": ..., "properties": {...}}, ...]}.
 * The shift applies to every source cell first; removals and sets are in output coordinates, and a
 * set replaces whatever is at its cell or adds a new cell. Every cell must land inside the size.
 * Run like the other tools here: java -cp <moddev classpath> tools/structure/EditTemplateBlocks.java plan.json
 */
public final class EditTemplateBlocks {
  public static void main(String[] args) throws Exception {
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
    JsonArray plan = JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonArray();
    for (JsonElement element : plan) {
      JsonObject spec = element.getAsJsonObject();
      Path source = Path.of(spec.get("source").getAsString());
      Path output = Path.of(spec.get("output").getAsString());
      if (source.equals(output)) throw new IllegalArgumentException("Source and output must differ: " + source);
      int[] size = ints(spec.getAsJsonArray("size"));
      int[] shift = spec.has("shift") ? ints(spec.getAsJsonArray("shift")) : new int[3];
      CompoundTag root = NbtIo.readCompressed(source, NbtAccounter.unlimitedHeap());
      if (root.contains("palettes")) throw new IllegalStateException(source + ": multi-palette templates are not supported");
      ListTag palette = root.getList("palette", Tag.TAG_COMPOUND);
      Map<String, CompoundTag> cells = new HashMap<>();
      for (Tag item : root.getList("blocks", Tag.TAG_COMPOUND)) {
        CompoundTag block = ((CompoundTag) item).copy();
        ListTag pos = block.getList("pos", Tag.TAG_INT);
        int[] moved = {pos.getInt(0) + shift[0], pos.getInt(1) + shift[1], pos.getInt(2) + shift[2]};
        block.put("pos", posTag(moved));
        cells.put(key(moved), block);
      }
      int removed = 0;
      if (spec.has("remove")) {
        for (JsonElement cell : spec.getAsJsonArray("remove")) {
          if (cells.remove(key(ints(cell.getAsJsonArray()))) == null) throw new IllegalStateException(source + ": nothing to remove at " + cell);
          removed++;
        }
      }
      int set = 0;
      if (spec.has("set")) {
        for (JsonElement item : spec.getAsJsonArray("set")) {
          JsonObject edit = item.getAsJsonObject();
          int[] pos = ints(edit.getAsJsonArray("pos"));
          CompoundTag desired = new CompoundTag();
          desired.putString("Name", edit.get("name").getAsString());
          if (edit.has("properties") && !edit.getAsJsonObject("properties").isEmpty()) {
            CompoundTag properties = new CompoundTag();
            for (Map.Entry<String, JsonElement> e : edit.getAsJsonObject("properties").entrySet()) properties.putString(e.getKey(), e.getValue().getAsString());
            desired.put("Properties", properties);
          }
          int state = -1;
          for (int i = 0; i < palette.size() && state < 0; i++) if (palette.getCompound(i).equals(desired)) state = i;
          if (state < 0) { palette.add(desired); state = palette.size() - 1; }
          CompoundTag block = new CompoundTag();
          block.put("pos", posTag(pos));
          block.putInt("state", state);
          cells.put(key(pos), block);
          set++;
        }
      }
      List<CompoundTag> ordered = new ArrayList<>(cells.values());
      ordered.sort((a, b) -> {
        ListTag pa = a.getList("pos", Tag.TAG_INT); ListTag pb = b.getList("pos", Tag.TAG_INT);
        for (int i : new int[] {1, 2, 0}) if (pa.getInt(i) != pb.getInt(i)) return Integer.compare(pa.getInt(i), pb.getInt(i));
        return 0;
      });
      int[] remap = new int[palette.size()];
      java.util.Arrays.fill(remap, -1);
      ListTag compact = new ListTag();
      ListTag blocks = new ListTag();
      for (CompoundTag block : ordered) {
        ListTag pos = block.getList("pos", Tag.TAG_INT);
        for (int i = 0; i < 3; i++) {
          if (pos.getInt(i) < 0 || pos.getInt(i) >= size[i]) throw new IllegalStateException(output + ": cell " + pos + " outside size " + java.util.Arrays.toString(size));
        }
        int state = block.getInt("state");
        if (remap[state] < 0) { remap[state] = compact.size(); compact.add(palette.getCompound(state).copy()); }
        block.putInt("state", remap[state]);
        blocks.add(block);
      }
      root.put("palette", compact);
      root.put("blocks", blocks);
      root.put("size", posTag(size));
      // Captured entities ride the same shift and drop out with the cells around them.
      if (root.contains("entities")) {
        ListTag kept = new ListTag();
        for (Tag item : root.getList("entities", Tag.TAG_COMPOUND)) {
          CompoundTag entity = ((CompoundTag) item).copy();
          ListTag pos = entity.getList("pos", Tag.TAG_DOUBLE);
          double[] moved = {pos.getDouble(0) + shift[0], pos.getDouble(1) + shift[1], pos.getDouble(2) + shift[2]};
          if (moved[0] < 0 || moved[0] >= size[0] || moved[1] < 0 || moved[1] >= size[1] || moved[2] < 0 || moved[2] >= size[2]) continue;
          ListTag doubles = new ListTag();
          for (double v : moved) doubles.add(net.minecraft.nbt.DoubleTag.valueOf(v));
          entity.put("pos", doubles);
          entity.put("blockPos", posTag(new int[] {(int) Math.floor(moved[0]), (int) Math.floor(moved[1]), (int) Math.floor(moved[2])}));
          CompoundTag nbt = entity.getCompound("nbt");
          if (nbt.contains("Pos")) nbt.put("Pos", doubles.copy());
          // An empty frame is a placement fixture, not decoration; a plan may drop them.
          boolean emptyFrame = nbt.getString("id").endsWith("item_frame") && !nbt.contains("Item");
          if (emptyFrame && spec.has("drop_empty_frames") && spec.get("drop_empty_frames").getAsBoolean()) continue;
          kept.add(entity);
        }
        root.put("entities", kept);
      }
      Files.createDirectories(output.getParent());
      NbtIo.writeCompressed(root, output);
      if (!NbtIo.readCompressed(output, NbtAccounter.unlimitedHeap()).equals(root)) throw new IllegalStateException("round-trip mismatch for " + output);
      System.out.println(output + ": " + blocks.size() + " cells (" + removed + " removed, " + set + " set), size " + java.util.Arrays.toString(size) + ", palette " + compact.size());
    }
  }

  private static int[] ints(JsonArray array) {
    int[] result = new int[array.size()];
    for (int i = 0; i < result.length; i++) result[i] = array.get(i).getAsInt();
    return result;
  }

  private static ListTag posTag(int[] pos) {
    ListTag tag = new ListTag();
    for (int v : pos) tag.add(IntTag.valueOf(v));
    return tag;
  }

  private static String key(int[] pos) {
    return pos[0] + "," + pos[1] + "," + pos[2];
  }
}
