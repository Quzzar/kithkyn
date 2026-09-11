package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class BuildingEntranceTest {
  @AfterEach
  void clearRegistry() { Buildings.reload(Map.of()); }

  @Test
  void bothBirchCompanionsFaceTheCenterOnEverySide() throws Exception {
    for (String name : new String[]{"mine_birch_forest_1", "storehouse_birch_forest_1"}) {
      BuildingInfo info = definition(name);
      assertEquals(Direction.WEST, info.getEntranceFacing());
      for (Direction inward : Direction.Plane.HORIZONTAL) {
        assertEquals(inward, info.rotationFacing(inward).rotate(info.getEntranceFacing()));
      }
    }
  }

  @Test
  void buildingFrontsUseThePublicApproachInsteadOfTheOldNorthDefaultOrInwardDoorState() throws Exception {
    for (String category : new String[]{"house", "couple_cottage", "bakery", "blacksmith", "butchery",
        "church", "fishery", "hunting_lodge", "stoneworks", "watchtower", "market"}) {
      assertEquals(Direction.WEST, definition(category + "_birch_forest_1").getEntranceFacing(), category);
    }
    assertEquals(Direction.WEST, definition("house_birch_forest_2").getEntranceFacing());
    assertEquals(Direction.NORTH, definition("lumberjack_birch_forest_1").getEntranceFacing());
    assertEquals(Direction.NORTH, definition("market_birch_forest_2").getEntranceFacing());
    assertEquals(Direction.SOUTH, definition("market_birch_forest_3").getEntranceFacing());
    assertEquals(Direction.SOUTH, definition("watchtower_birch_forest_2").getEntranceFacing());
  }

  @Test
  void minerAndNavigationShareTheCenteredAuthoredShaftInEveryRotation() throws Exception {
    BuildingInfo info = definition("mine_birch_forest_1");
    Buildings.reload(Map.of(info.getName(), info));
    BlockPos origin = new BlockPos(100, 64, -200);
    BlockPos station = BlockPos.of(info.getWorkLocations().keySet().iterator().next());
    assertEquals(new BlockPos(8, 0, 7), station, "Existing work-station coordinates must not drift");
    for (Rotation rotation : Rotation.values()) {
      Building building = new Building(origin, info.getName(), rotation);
      MineShaft worker = MineShaft.root(building, origin.offset(station.rotate(rotation)));
      MineShaft navigation = MineShaft.of(building).getFirst();
      assertEquals(origin.offset(new BlockPos(9, 0, 8).rotate(rotation)), worker.mouth());
      assertEquals(rotation.rotate(Direction.EAST), worker.rotation().rotate(Direction.SOUTH));
      assertEquals(navigation.mouth(), worker.mouth());
      assertEquals(navigation.rotation(), worker.rotation());
      assertEquals(station.asLong(), worker.rootStation());
    }
  }

  @Test
  void savedMinesKeepTheirExcavatedFrameWhileNewMinesUseTheAuthoredEntrance() throws Exception {
    BuildingInfo info = definition("mine_birch_forest_1");
    Buildings.reload(Map.of(info.getName(), info));
    for (Rotation rotation : Rotation.values()) {
      Building fresh = new Building(new BlockPos(-100, 64, 200), info.getName(), rotation);
      var saved = Building.CODEC.encodeStart(JsonOps.INSTANCE, fresh).getOrThrow().getAsJsonObject();
      Building restored = Building.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();
      assertEquals(MineShaft.of(fresh).getFirst().mouth(), MineShaft.of(restored).getFirst().mouth());
      assertEquals(MineShaft.of(fresh).getFirst().rotation(), MineShaft.of(restored).getFirst().rotation());
      // Already-dug Birch shafts retain the pre-landing-fix frame on reload.
      saved.add("mine_entrance", JsonParser.parseString("{\"facing\":\"east\",\"offset\":[0,0,1]}"));
      Building excavated = Building.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();
      assertEquals(BlockPos.of(excavated.getOriginLocation()).offset(new BlockPos(8, 0, 8).rotate(rotation)),
          MineShaft.of(excavated).getFirst().mouth());
      saved.remove("mine_entrance");
      Building legacy = Building.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();
      BlockPos station = BlockPos.of(info.getWorkLocations().keySet().iterator().next());
      assertEquals(BlockPos.of(legacy.getOriginLocation()).offset(station.rotate(rotation)), MineShaft.of(legacy).getFirst().mouth());
      assertEquals(rotation, MineShaft.of(legacy).getFirst().rotation());
    }
  }

  @Test
  void aTwoBlockAuthoredMineOpeningIsAccepted() {
    var json = JsonParser.parseString("{\"facing\":\"south\",\"offset\":[0,1,1],\"width\":2}");
    BuildingInfo.MineEntrance entrance = BuildingInfo.MineEntrance.CODEC
        .parse(JsonOps.INSTANCE, json).getOrThrow();

    assertEquals(2, entrance.width());
    assertEquals(new BlockPos(0, 1, 1), entrance.offset());
  }

  @Test
  void legacyStorehousesKeepTheirActualGroundWhileFreshOnesGetTheRaisedStep() throws Exception {
    BuildingInfo info = definition("storehouse_birch_forest_1");
    Buildings.reload(Map.of(info.getName(), info));
    Building fresh = new Building(info.getName(), Rotation.NONE);
    assertEquals(-1, fresh.getPlacedSink());
    var saved = Building.CODEC.encodeStart(JsonOps.INSTANCE, fresh).getOrThrow().getAsJsonObject();
    assertEquals(-1, Building.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow().getPlacedSink());
    saved.remove("placed_sink");
    assertEquals(0, Building.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow().getPlacedSink());
  }

  private static BuildingInfo definition(String name) throws Exception {
    try (var reader = new InputStreamReader(Objects.requireNonNull(BuildingEntranceTest.class.getResourceAsStream(
        "/data/kithkyn/kithkyn/buildings/"+name+".json")), StandardCharsets.UTF_8)) {
      return BuildingInfo.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader)).getOrThrow();
    }
  }
}
