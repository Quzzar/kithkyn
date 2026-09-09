import com.google.gson.*;
import net.minecraft.nbt.*;
import java.nio.file.*;
import java.util.*;

/** Converts approved captures into runtime assets without rewriting native NBT tag types. */
public final class VillageTemplateExport {
  static ListTag ints(int... values) {
    ListTag result = new ListTag();
    for (int value : values) result.add(IntTag.valueOf(value));
    return result;
  }

  static ListTag doubles(double... values) {
    ListTag result = new ListTag();
    for (double value : values) result.add(DoubleTag.valueOf(value));
    return result;
  }

  static int[] vector(JsonElement value) { return new Gson().fromJson(value, int[].class); }

  public static void main(String[] args) throws Exception {
    JsonArray plan = JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonArray();
    for (JsonElement entry : plan) {
      JsonObject spec = entry.getAsJsonObject();
      Path source = Path.of(spec.get("source").getAsString());
      Path output = Path.of(spec.get("output").getAsString());
      if (source.toRealPath().equals(output.toAbsolutePath())) throw new IllegalArgumentException("Source is immutable");
      CompoundTag root = NbtIo.readCompressed(source, NbtAccounter.unlimitedHeap());
      ListTag palette = root.getList("palette", Tag.TAG_COMPOUND);
      int[] shift = spec.has("crop") ? vector(spec.get("crop")) : new int[]{0, 0, 0};
      Set<String> white = new HashSet<>();
      for (JsonElement point : spec.getAsJsonArray("white")) white.add(Arrays.toString(vector(point)));
      for (Tag value : root.getList("blocks", Tag.TAG_COMPOUND)) {
        CompoundTag block = (CompoundTag)value;
        ListTag pos = block.getList("pos", Tag.TAG_INT);
        int[] local = {pos.getInt(0), pos.getInt(1), pos.getInt(2)};
        CompoundTag state = palette.getCompound(block.getInt("state")).copy();
        String name = state.getString("Name");
        if (white.contains(Arrays.toString(local))) {
          String suffix = name.endsWith("_bed") ? "bed" : name.endsWith("_wall_banner") ? "wall_banner" : "banner";
          state.putString("Name", "minecraft:white_" + suffix);
          if (suffix.contains("banner")) block.getCompound("nbt").remove("patterns");
        }
        CompoundTag properties = state.getCompound("Properties");
        if (properties.contains("occupied")) properties.putString("occupied", "false");
        if (name.endsWith("_sapling")) properties.putString("stage", "0");
        if (name.endsWith("_leaves")) properties.putString("persistent", "true");
        int index = palette.indexOf(state);
        if (index < 0) { palette.add(state); index = palette.size() - 1; }
        block.putInt("state", index);
        block.put("pos", ints(local[0]-shift[0],local[1]-shift[1],local[2]-shift[2]));
        CompoundTag data = block.getCompound("nbt");
        for (String key : List.of("Items", "LootTable", "LootTableSeed", "item", "RecipesUsed")) data.remove(key);
        if (data.contains("BurnTime")) data.putShort("BurnTime", (short)0);
        if (data.contains("CookTime")) data.putShort("CookTime", (short)0);
      }
      CompoundTag air = new CompoundTag(); air.putString("Name", "minecraft:air");
      int airIndex = palette.size(); palette.add(air);
      ListTag blocks = root.getList("blocks", Tag.TAG_COMPOUND);
      for (JsonElement point : spec.getAsJsonArray("air")) {
        CompoundTag block = new CompoundTag(); block.put("pos", ints(vector(point))); block.putInt("state", airIndex); blocks.add(block);
      }
      if (spec.has("overrides")) {
        for (JsonElement value : spec.getAsJsonArray("overrides")) {
          JsonObject edit = value.getAsJsonObject();
          ListTag position = ints(vector(edit.get("pos")));
          blocks.removeIf(tag -> ((CompoundTag)tag).get("pos").equals(position));
          CompoundTag state = new CompoundTag();
          state.putString("Name", edit.get("name").getAsString());
          CompoundTag properties = new CompoundTag();
          edit.getAsJsonObject("properties").entrySet().forEach(property ->
              properties.putString(property.getKey(), property.getValue().getAsString()));
          state.put("Properties", properties);
          int stateIndex = palette.indexOf(state);
          if (stateIndex < 0) { palette.add(state); stateIndex = palette.size() - 1; }
          CompoundTag block = new CompoundTag();
          block.put("pos", position); block.putInt("state", stateIndex); blocks.add(block);
        }
      }
      if (spec.has("horizontal_bounds")) {
        int[] bounds = vector(spec.get("horizontal_bounds"));
        // A deleted outer floor cell shrinks the building, not its terrain-clearing apron.
        blocks.removeIf(tag -> {
          CompoundTag block = (CompoundTag)tag;
          if (!palette.getCompound(block.getInt("state")).getString("Name").equals("minecraft:air")) return false;
          ListTag pos = block.getList("pos", Tag.TAG_INT);
          return pos.getInt(0) < bounds[0] || pos.getInt(2) < bounds[1]
              || pos.getInt(0) > bounds[2] || pos.getInt(2) > bounds[3];
        });
      }
      root.put("size", ints(vector(spec.get("size"))));
      for (Tag value : root.getList("entities", Tag.TAG_COMPOUND)) {
        CompoundTag entity = ((CompoundTag)value).getCompound("nbt");
        for (String key : List.of("UUID", "Leash", "AngryAt", "NeoForgeData")) entity.remove(key);
        entity.put("Motion", doubles(0,0,0));
        entity.putShort("Fire", (short)-1);
        entity.putShort("HurtTime", (short)0);
        entity.putShort("DeathTime", (short)0);
        entity.putInt("PortalCooldown", 0);
        if (entity.getString("id").equals("minecraft:iron_golem")) {
          entity.putInt("AngerTime", 0); entity.putFloat("Health", 100);
        } else if (Set.of("minecraft:cow", "minecraft:chicken").contains(entity.getString("id"))) {
          entity.putInt("Age", 0); entity.putInt("InLove", 0); entity.putBoolean("PersistenceRequired", true);
          entity.putFloat("Health", entity.getString("id").equals("minecraft:cow") ? 10 : 4);
        }
      }
      Files.createDirectories(output.getParent());
      NbtIo.writeCompressed(root, output);
      System.out.println(output.getFileName());
    }
  }
}
