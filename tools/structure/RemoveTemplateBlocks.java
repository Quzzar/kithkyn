import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.Bootstrap;

/**
 * Drops named cells from structure templates with Minecraft's own typed NBT reader and writer, so
 * nothing else in the file changes. Plan: a JSON array of {"path": ..., "remove": [[x,y,z], ...]}.
 * Run like the other tools here: java -cp <moddev classpath> tools/structure/RemoveTemplateBlocks.java plan.json
 */
public final class RemoveTemplateBlocks {
  public static void main(String[] args) throws Exception {
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
    JsonArray plan = JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonArray();
    for (JsonElement element : plan) {
      JsonObject spec = element.getAsJsonObject();
      Path path = Path.of(spec.get("path").getAsString());
      Set<String> remove = new HashSet<>();
      for (JsonElement cell : spec.getAsJsonArray("remove")) {
        JsonArray c = cell.getAsJsonArray();
        remove.add(c.get(0).getAsInt() + "," + c.get(1).getAsInt() + "," + c.get(2).getAsInt());
      }
      CompoundTag root = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
      ListTag blocks = root.getList("blocks", Tag.TAG_COMPOUND);
      ListTag kept = new ListTag();
      int dropped = 0;
      for (Tag item : blocks) {
        CompoundTag block = (CompoundTag) item;
        ListTag pos = block.getList("pos", Tag.TAG_INT);
        String key = pos.getInt(0) + "," + pos.getInt(1) + "," + pos.getInt(2);
        if (remove.contains(key)) { dropped++; continue; }
        kept.add(block);
      }
      if (dropped != remove.size()) throw new IllegalStateException(path + ": expected to drop " + remove.size() + " cells, found " + dropped);
      root.put("blocks", kept);
      NbtIo.writeCompressed(root, path);
      if (!NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap()).equals(root)) throw new IllegalStateException("round-trip mismatch for " + path);
      System.out.println("dropped " + dropped + " from " + path);
    }
  }
}
