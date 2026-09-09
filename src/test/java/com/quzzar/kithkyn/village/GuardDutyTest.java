package com.quzzar.kithkyn.village;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.quzzar.kithkyn.village.buildings.BuildingInfo;
import com.quzzar.kithkyn.village.buildings.WallPost;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;

class GuardDutyTest {

  @Test
  void captainIsOnlyTheCurrentTownCenterGuardAssignment() {
    var center = java.util.UUID.randomUUID();
    var person = java.util.UUID.randomUUID();
    assertTrue(GuardDuty.isCaptain(new JobAssignment(person, Occupation.GUARD, center, 3), center));
    assertFalse(GuardDuty.isCaptain(new JobAssignment(person, Occupation.MINER, center, 3), center));
    assertFalse(GuardDuty.isCaptain(new JobAssignment(person, Occupation.GUARD,
        java.util.UUID.randomUUID(), 0), center));
    assertFalse(GuardDuty.isCaptain(JobAssignment.wallPost(0), center));
    assertFalse(GuardDuty.isCaptain(null, center));
    assertFalse(GuardDuty.isCaptain(new JobAssignment(person, Occupation.GUARD, center, 3), null));
  }

  @Test
  void authoredGuardPostsRotateAndKeepGlobalWorkstationIndexes() {
    BuildingInfo info = definition(true);
    BlockPos origin = new BlockPos(80, 64, -20);
    for (Rotation rotation : Rotation.values()) {
      assertNull(GuardDuty.fromBuilding(info, origin, rotation, 0));
      for (int index = 1; index <= 2; index++) {
        GuardDuty duty = GuardDuty.fromBuilding(info, origin, rotation, index);
        assertNotNull(duty);
        assertEquals(origin.offset(new BlockPos(index + 2, 8, 4).rotate(rotation)), duty.position());
        assertEquals(duty.position().offset(new BlockPos(0, 0, -8).rotate(rotation)), duty.lookAt());
        assertTrue(duty.ranged());
        assertTrue(duty.backupSword());
      }
      assertNull(GuardDuty.fromBuilding(info, origin, rotation, -1));
      assertNull(GuardDuty.fromBuilding(info, origin, rotation, 3));
    }
  }

  @Test
  void existingWatchtowersDoNotOptInByNameOrByVillageCapability() {
    assertNull(GuardDuty.fromBuilding(definition(false), BlockPos.ZERO, Rotation.NONE, 1));
  }

  @Test
  void wallPostsKeepTheirOwnWeaponTypeAndExactGeometry() {
    for (WallPost.Duty kind : WallPost.Duty.values()) {
      WallPost post = new WallPost(new BlockPos(5, 8, 4), new BlockPos(6, 8, 9), 42, kind);
      GuardDuty duty = GuardDuty.fromWall(post);
      assertEquals(post.position(), duty.position());
      assertEquals(post.lookAt(), duty.lookAt());
      assertEquals(kind.usesCrossbow(), duty.ranged());
      assertFalse(duty.backupSword());
    }
  }

  private static BuildingInfo definition(boolean ranged) {
    return BuildingInfo.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
        {
          "structure": "watchtower_plains_2",
          "upgrades_from": "watchtower_plains_1",
          "work_stations": [
            {"pos": [1,1,1], "occupation": "BAKER"},
            {"pos": [3,8,4], "occupation": "GUARD"},
            {"pos": [4,8,4], "occupation": "GUARD"}
          ],
          "grants": %s
        }
        """.formatted(ranged ? "[\"RANGED_GUARD_POSTS\"]" : "[\"PROTECTION\"]"))).getOrThrow();
  }
}
