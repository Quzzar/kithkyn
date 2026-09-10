package com.quzzar.kithkyn.village;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.quzzar.kithkyn.village.buildings.BuildingInfo;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;

class PersonalChestTest {

  @Test
  void roomBindingsOverrideDistanceAndBedsWithoutStorageCannotBorrowAnotherRoomsChest() {
    BuildingInfo info = definition("""
        ,"bed_containers":[
          {"bed":[1,1,1],"containers":[[8,1,1]]},
          {"bed":[2,1,1],"containers":[]},
          {"bed":[3,1,1],"containers":[[8,1,1]]},
          {"bed":[4,1,1],"containers":[[1,1,2]]}
        ]
        """);
    assertNull(info.validate());
    BlockPos origin = new BlockPos(50, 70, -20);
    for (Rotation rotation : Rotation.values()) {
      BlockPos shared = origin.offset(new BlockPos(8, 1, 1).rotate(rotation));
      assertEquals(shared, PersonalChest.forBed(info, origin, rotation, 0));
      assertNull(PersonalChest.forBed(info, origin, rotation, 1));
      assertEquals(shared, PersonalChest.forBed(info, origin, rotation, 2));
      assertEquals(origin.offset(new BlockPos(1, 1, 2).rotate(rotation)),
          PersonalChest.forBed(info, origin, rotation, 3));
      assertNull(PersonalChest.forBed(info, origin, rotation, 4));
      assertNull(PersonalChest.forBed(info, origin, rotation, -1));
      assertNull(PersonalChest.forBed(info, origin, rotation, 5));
    }
    BuildingInfo restored = BuildingInfo.CODEC.parse(JsonOps.INSTANCE,
        BuildingInfo.CODEC.encodeStart(JsonOps.INSTANCE, info).getOrThrow()).getOrThrow();
    assertEquals(info.getBedContainers(), restored.getBedContainers());
    assertNull(PersonalChest.forBed(restored, origin, Rotation.NONE, 1));
  }

  @Test
  void definitionsWithoutRoomBindingsRetainNearestHomeStorage() {
    BuildingInfo info = definition("");
    assertNull(info.getBedContainers());
    assertEquals(new BlockPos(1, 1, 2), PersonalChest.forBed(info, BlockPos.ZERO, Rotation.NONE, 0));
    assertEquals(new BlockPos(8, 1, 1), PersonalChest.forBed(info, BlockPos.ZERO, Rotation.NONE, 4));
  }

  private static BuildingInfo definition(String bindings) {
    return BuildingInfo.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
        {
          "structure":"village_center_badlands_1",
          "beds":[[1,1,1],[2,1,1],[3,1,1],[4,1,1],[8,1,2]],
          "personal_containers":[[1,1,2],[8,1,1]],
          "containers":[[0,1,1]]
          %s
        }
        """.formatted(bindings))).getOrThrow();
  }
}
