package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Explicit local-artifact audit; core catalog/schema tests do not depend on private captures. */
@EnabledIfEnvironmentVariable(named = "KITHKYN_BADLANDS_DATAPACK", matches = ".+")
class BadlandsAssetsTest {
  private static Path data() throws Exception {
    return Path.of(System.getenv("KITHKYN_BADLANDS_DATAPACK")).resolve("data/kithkyn");
  }

  private static JsonObject definition(String id) throws Exception {
    Path root = id.contains("_badlands_") ? data()
        : Path.of(Objects.requireNonNull(BadlandsAssetsTest.class.getResource("/data/kithkyn")).toURI());
    return JsonParser.parseString(Files.readString(root.resolve("kithkyn/buildings/" + id + ".json"))).getAsJsonObject();
  }

  private static CompoundTag template(String id) throws Exception {
    return NbtIo.readCompressed(data().resolve("structure/" + id + ".nbt"), NbtAccounter.unlimitedHeap());
  }

  private static Map<BlockPos, CompoundTag> states(CompoundTag template) {
    Map<BlockPos, CompoundTag> states = new HashMap<>();
    ListTag palette = template.getList("palette", Tag.TAG_COMPOUND);
    for (Tag value : template.getList("blocks", Tag.TAG_COMPOUND)) {
      CompoundTag block = (CompoundTag) value;
      ListTag pos = block.getList("pos", Tag.TAG_INT);
      BlockPos at = new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2));
      assertNull(states.put(at, palette.getCompound(block.getInt("state"))), "Duplicate template cell " + at);
      assertFalse(block.getCompound("nbt").contains("LootTable"));
      assertTrue(block.getCompound("nbt").getList("Items", Tag.TAG_COMPOUND).isEmpty());
    }
    return states;
  }

  @Test
  void allThirtySelectionsHaveRealHousingStorageAndNeutralIdentitySockets() throws Exception {
    int count = 0, beds = 0, primaryBeds = 0, secondaryBeds = 0, flags = 0;
    try (var files = Files.list(data().resolve("kithkyn/buildings"))) {
      for (Path file : files.filter(p -> p.getFileName().toString().contains("_badlands_")).toList()) {
        BuildingInfo info = BuildingInfo.CODEC.parse(JsonOps.INSTANCE,
            JsonParser.parseString(Files.readString(file))).getOrThrow();
        assertNull(info.validate(), file.toString());
        assertEquals("badlands", info.getVariant());
        Map<BlockPos, CompoundTag> states = states(template(info.getName()));
        Set<BlockPos> primary = Set.copyOf(info.getVillageIdentitySlots().primaryBlocks());
        Set<BlockPos> secondary = Set.copyOf(info.getVillageIdentitySlots().secondaryBlocks());
        assertTrue(java.util.Collections.disjoint(primary, secondary), info.getName());
        Set<BlockPos> physicalBeds = new HashSet<>();
        states.forEach((position, state) -> {
          if (state.getString("Name").endsWith("_bed")
              && state.getCompound("Properties").getString("part").equals("head")) physicalBeds.add(position);
        });
        assertEquals(physicalBeds, info.getBedLocations().stream().map(BlockPos::of).collect(java.util.stream.Collectors.toSet()),
            "Every physical bed must be available to the village: " + info.getName());
        for (BlockPos bed : physicalBeds) {
          CompoundTag state = states.get(bed);
          assertEquals("minecraft:white_bed", state.getString("Name"));
          assertTrue(primary.contains(bed) ^ secondary.contains(bed), info.getName() + bed);
          Direction facing = Direction.byName(state.getCompound("Properties").getString("facing"));
          CompoundTag foot = states.get(bed.relative(Objects.requireNonNull(facing).getOpposite()));
          assertEquals("minecraft:white_bed", foot.getString("Name"));
          assertEquals("foot", foot.getCompound("Properties").getString("part"));
          if (primary.contains(bed)) primaryBeds++; else secondaryBeds++;
        }
        Set<Long> containers = new HashSet<>(info.getContainerLocations());
        assertTrue(java.util.Collections.disjoint(containers, info.getPersonalContainerLocations()), info.getName());
        containers.addAll(info.getPersonalContainerLocations());
        for (long position : containers) {
          assertTrue(Set.of("minecraft:chest", "minecraft:trapped_chest", "minecraft:barrel")
              .contains(states.get(BlockPos.of(position)).getString("Name")), info.getName());
        }
        for (BlockPos position : info.getVillageIdentitySlots().banners()) {
          assertTrue(Set.of("minecraft:white_banner", "minecraft:white_wall_banner")
              .contains(states.get(position).getString("Name")), info.getName());
        }
        beds += physicalBeds.size();
        flags += info.getVillageIdentitySlots().banners().size();
        count++;
      }
    }
    assertEquals(30, count);
    assertEquals(46, beds);
    assertEquals(26, primaryBeds);
    assertEquals(20, secondaryBeds);
    assertEquals(9, flags);
  }

  @Test
  void marketClothChangesVillageColorWithoutTurningEveryAwningIntoAFlag() throws Exception {
    int primaryCloth = 0, secondaryCloth = 0, whiteCloth = 0;
    for (int tier = 1; tier <= 3; tier++) {
      String id = "market_badlands_" + tier;
      BuildingInfo info = BuildingInfo.CODEC.parse(JsonOps.INSTANCE, definition(id)).getOrThrow();
      assertEquals(tier, info.getWorkLocations().size());
      assertEquals(tier, info.getContainerLocations().size());
      assertTrue(info.getBedLocations().isEmpty());
      assertTrue(info.getVillageIdentitySlots().banners().isEmpty());
      for (var block : states(template(id)).entrySet()) {
        if (!block.getValue().getString("Name").endsWith("_wall_banner")) continue;
        assertEquals("minecraft:white_wall_banner", block.getValue().getString("Name"));
        if (info.getVillageIdentitySlots().primaryBlocks().contains(block.getKey())) primaryCloth++;
        else if (info.getVillageIdentitySlots().secondaryBlocks().contains(block.getKey())) secondaryCloth++;
        else whiteCloth++;
      }
    }
    assertEquals(32, primaryCloth);
    assertEquals(16, secondaryCloth);
    assertEquals(30, whiteCloth);
  }

  @Test
  void editedSheepFarmReplacesTheSupersededButcherComposite() throws Exception {
    CompoundTag sheepFarm = template("butchery_badlands_1");
    ListTag entities = sheepFarm.getList("entities", Tag.TAG_COMPOUND);
    int sheep = 0, paintings = 0;
    for (Tag value : entities) {
      CompoundTag entity = ((CompoundTag) value).getCompound("nbt");
      assertFalse(entity.contains("UUID"));
      switch (entity.getString("id")) {
        case "minecraft:sheep" -> {
          sheep++;
          assertTrue(entity.contains("Health", Tag.TAG_FLOAT));
        }
        case "minecraft:painting" -> paintings++;
        default -> fail("Unexpected livestock or decor: " + entity.getString("id"));
      }
    }
    assertEquals(3, sheep);
    assertEquals(1, paintings, "Keep the authored wall decoration alongside the three sheep");
    BuildingInfo info = BuildingInfo.CODEC.parse(JsonOps.INSTANCE, definition("butchery_badlands_1")).getOrThrow();
    assertEquals(2, info.getBedLocations().size(), "The butcher and spouse share the edited sheep-farm room");
    assertEquals(1, info.getWorkLocations().size());
  }

  @Test
  void pricesFollowExistingCapacityTiersAndOnlyAuthoredChainsUpgrade() throws Exception {
    Map<Integer, Integer> houseCounts = new HashMap<>();
    List<String> upgrades = new ArrayList<>();
    try (var files = Files.list(data().resolve("kithkyn/buildings"))) {
      for (Path file : files.filter(p -> p.getFileName().toString().contains("_badlands_")).toList()) {
        JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        BuildingInfo info = BuildingInfo.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        JsonObject reference = definition(info.getCategory() + "_plains_" + info.getLevel());
        assertEquals(reference.get("cost"), json.get("cost"), info.getName());
        if (info.getCategory().equals("house")) {
          houseCounts.merge(info.getLevel(), 1, Integer::sum);
          assertFalse(json.has("upgrades_from"), "House alternatives have no authored replacement chain");
          if (info.getLevel() > 1) assertTrue(json.get("standalone").getAsBoolean());
        }
        if (json.has("upgrades_from")) {
          assertEquals(info.getCategory() + "_badlands_" + (info.getLevel() - 1), json.get("upgrades_from").getAsString());
          upgrades.add(info.getName());
        }
      }
    }
    assertEquals(Map.of(1, 6, 2, 4, 3, 2), houseCounts);
    assertEquals(Set.of("storehouse_badlands_2", "market_badlands_2", "market_badlands_3"), Set.copyOf(upgrades));
    assertFalse(Files.exists(data().resolve("kithkyn/buildings/well_badlands_1.json")));
    assertFalse(Files.exists(data().resolve("kithkyn/buildings/watchtower_badlands_2.json")));
  }
}
