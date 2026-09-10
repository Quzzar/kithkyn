import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/** Makes a stopped world's installed village pack override the mod's default resources. */
public final class SetVillageDatapackPriority {
  public static void main(String[] args) throws Exception {
    if (args.length != 3 || !args[2].matches("file/[a-z0-9][a-z0-9_-]*")) {
      throw new IllegalArgumentException("Supply input level.dat, a new output file, and file/<pack-name>");
    }
    Path input = Path.of(args[0]).toRealPath();
    Path output = Path.of(args[1]).toAbsolutePath();
    if (input.equals(output) || Files.exists(output)) {
      throw new IllegalArgumentException("Output must be a new copy");
    }
    CompoundTag root = NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
    CompoundTag before = root.copy();
    CompoundTag packs = root.getCompound("Data").getCompound("DataPacks");
    if (!packs.contains("Enabled", Tag.TAG_LIST) || !packs.contains("Disabled", Tag.TAG_LIST)) {
      throw new IllegalArgumentException("World has no saved datapack selection");
    }
    ListTag enabled = without((ListTag) packs.get("Enabled"), args[2]);
    ListTag disabled = without((ListTag) packs.get("Disabled"), args[2]);
    enabled.add(StringTag.valueOf(args[2]));
    packs.put("Enabled", enabled);
    packs.put("Disabled", disabled);

    CompoundTag unchanged = root.copy();
    unchanged.getCompound("Data").put("DataPacks", before.getCompound("Data").getCompound("DataPacks").copy());
    if (!unchanged.equals(before)) throw new IllegalStateException("Changed unrelated world state");
    NbtIo.writeCompressed(root, output);
    if (!NbtIo.readCompressed(output, NbtAccounter.unlimitedHeap()).equals(root)) {
      throw new IllegalStateException("Native world metadata readback changed tag types or values");
    }
    System.out.println("Preserved world metadata; enabled " + args[2] + " last: " + enabled);
  }

  private static ListTag without(ListTag source, String pack) {
    ListTag result = new ListTag();
    for (Tag value : source) {
      if (!(value instanceof StringTag)) throw new IllegalArgumentException("Datapack IDs must be strings");
      if (!value.getAsString().equals(pack)) result.add(value.copy());
    }
    return result;
  }
}
