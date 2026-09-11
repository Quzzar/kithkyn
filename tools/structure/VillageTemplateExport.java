import com.google.gson.*;
import net.minecraft.nbt.*;
import java.nio.file.*;
import java.util.*;

/**
 * Converts approved captures into runtime assets without rewriting native NBT tag types.
 *
 * <p>Plan keys per entry: {@code source}, {@code output}, {@code size}, {@code white}, {@code air},
 * and optionally {@code crop}, {@code overrides}, {@code horizontal_bounds}, {@code ground_layer}.
 * An optional {@code entities} list bakes authored initial entities (livestock, an allay) into a
 * capture that has none: each entry is {@code {"pos": [x, y, z], "nbt": ...}} with {@code pos} in
 * cropped template coordinates (doubles allowed) and {@code nbt} either an SNBT string, which keeps
 * exact tag types such as {@code Health:10.0f}, or a JSON object parsed as SNBT text (integers become
 * ints, decimals doubles, booleans bytes). Entries are appended to the template's entity list as
 * {@code pos}, {@code blockPos} (the floor of {@code pos}) and {@code nbt}, then pass through the same
 * hygiene as captured entities below. Every block must lie inside {@code size}: one outside it
 * fails the export rather than stretching the building's footprint in the world.
 */
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

  /** Remove palette states no surviving block uses and remap every state index. */
  static void compactPalette(ListTag palette, ListTag blocks) {
    boolean[] used = new boolean[palette.size()];
    for (Tag value : blocks) {
      int state = ((CompoundTag)value).getInt("state");
      if (state < 0 || state >= palette.size()) {
        throw new IllegalArgumentException("Block refers to missing palette state " + state);
      }
      used[state] = true;
    }
    int[] remap = new int[palette.size()];
    ListTag compact = new ListTag();
    for (int state = 0; state < palette.size(); state++) {
      if (!used[state]) {
        remap[state] = -1;
        continue;
      }
      remap[state] = compact.size();
      compact.add(palette.get(state).copy());
    }
    for (Tag value : blocks) {
      CompoundTag block = (CompoundTag)value;
      block.putInt("state", remap[block.getInt("state")]);
    }
    palette.clear();
    palette.addAll(compact);
  }

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
      int[] size = vector(spec.get("size"));
      ListTag blocks = root.getList("blocks", Tag.TAG_COMPOUND);
      if (spec.has("crop")) {
        // A crop is an explicit selection, not merely a coordinate translation. Gallery
        // captures commonly surround a building with labels, fluid-containment barriers,
        // or a review plinth. Discard every source cell outside the selected cuboid before
        // rebasing it so those review fixtures can never survive at negative coordinates.
        blocks.removeIf(tag -> {
          ListTag position = ((CompoundTag)tag).getList("pos", Tag.TAG_INT);
          for (int axis = 0; axis < 3; axis++) {
            int local = position.getInt(axis) - shift[axis];
            if (local < 0 || local >= size[axis]) return true;
          }
          return false;
        });
      }
      Set<String> white = new HashSet<>();
      for (JsonElement point : spec.getAsJsonArray("white")) white.add(Arrays.toString(vector(point)));
      for (Tag value : blocks) {
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
      for (JsonElement point : spec.getAsJsonArray("air")) {
        CompoundTag block = new CompoundTag(); block.put("pos", ints(vector(point))); block.putInt("state", airIndex); blocks.add(block);
      }
      if (spec.has("overrides")) {
        for (JsonElement value : spec.getAsJsonArray("overrides")) {
          JsonObject edit = value.getAsJsonObject();
          ListTag position = ints(vector(edit.get("pos")));
          blocks.removeIf(tag -> ((CompoundTag)tag).get("pos").equals(position));
          String blockName = edit.get("name").getAsString();
          CompoundTag state = new CompoundTag();
          state.putString("Name", blockName);
          CompoundTag properties = new CompoundTag();
          edit.getAsJsonObject("properties").entrySet().forEach(property ->
              properties.putString(property.getKey(), property.getValue().getAsString()));
          state.put("Properties", properties);
          int stateIndex = palette.indexOf(state);
          if (stateIndex < 0) { palette.add(state); stateIndex = palette.size() - 1; }
          CompoundTag block = new CompoundTag();
          block.put("pos", position);
          block.putInt("state", stateIndex);
          if (Set.of("minecraft:barrel", "minecraft:chest", "minecraft:trapped_chest").contains(blockName)) {
            CompoundTag blockEntity = new CompoundTag();
            blockEntity.putString("id", blockName);
            block.put("nbt", blockEntity);
          }
          blocks.add(block);
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
      if (spec.has("ground_layer")) {
        int ground = spec.get("ground_layer").getAsInt();
        // Dense world captures include exterior ground air, which must not excavate terrain.
        blocks.removeIf(tag -> {
          CompoundTag block = (CompoundTag)tag;
          return block.getList("pos", Tag.TAG_INT).getInt(1) <= ground
              && palette.getCompound(block.getInt("state")).getString("Name").equals("minecraft:air");
        });
      }
      compactPalette(palette, blocks);
      // A block outside the declared size is never intended: it stretches the building's
      // footprint in the world (a gallery sign captured four cells in front of a mine
      // pushed the whole mine back), so the export fails instead of shipping it.
      for (Tag tag : blocks) {
        CompoundTag block = (CompoundTag)tag;
        ListTag position = block.getList("pos", Tag.TAG_INT);
        for (int axis = 0; axis < 3; axis++) {
          if (position.getInt(axis) < 0 || position.getInt(axis) >= size[axis]) {
            throw new IllegalArgumentException("Block outside the declared size at " + position + " in "
                + spec.get("output").getAsString() + ": crop it out, override it to air inside horizontal_bounds, or widen size");
          }
        }
        if (palette.getCompound(block.getInt("state")).getString("Name").equals("minecraft:barrier")) {
          throw new IllegalArgumentException("Barrier block in production structure at " + position + " in "
              + spec.get("output").getAsString() + ": remove gallery containment before export");
        }
      }
      for (Tag state : palette) {
        if (((CompoundTag)state).getString("Name").equals("minecraft:barrier")) {
          throw new IllegalArgumentException("Barrier state in production structure palette in "
              + spec.get("output").getAsString());
        }
      }
      root.put("size", ints(size));
      if (spec.has("entities")) {
        ListTag entities = root.getList("entities", Tag.TAG_COMPOUND);
        for (JsonElement value : spec.getAsJsonArray("entities")) {
          JsonObject addition = value.getAsJsonObject();
          double[] position = new Gson().fromJson(addition.get("pos"), double[].class);
          JsonElement data = addition.get("nbt");
          CompoundTag nbt = TagParser.parseTag(data.isJsonPrimitive() ? data.getAsString() : data.toString());
          CompoundTag entity = new CompoundTag();
          entity.put("pos", doubles(position));
          entity.put("blockPos", ints((int)Math.floor(position[0]), (int)Math.floor(position[1]), (int)Math.floor(position[2])));
          entity.put("nbt", nbt);
          entities.add(entity);
        }
        root.put("entities", entities);
      }
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
