package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.quzzar.kithkyn.village.GuardRole;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class CastleLayoutTest {
  private static final String JAIL = "\"castle\":{\"custody_cell\":[7,2,5],\"release_point\":[9,1,2],"
      + "\"evidence_containers\":[[6,0,2],[8,0,2]]}";

  @Test
  void routesRoundTripAsIndependentImmutableLists() {
    var points = new ArrayList<>(List.of(new BlockPos(8, 11, 13), new BlockPos(20, 11, 13)));
    var routes = new HashMap<GuardRole, List<BlockPos>>();
    routes.put(GuardRole.CROSSBOW_POST, points);
    CastleLayout layout = new CastleLayout(BlockPos.ZERO, BlockPos.ZERO, routes,
        List.of(new BlockPos(9, 11, 19), new BlockPos(9, 12, 19)));
    points.clear();
    routes.clear();
    CastleLayout restored = CastleLayout.CODEC.parse(JsonOps.INSTANCE,
        CastleLayout.CODEC.encodeStart(JsonOps.INSTANCE, layout).getOrThrow()).getOrThrow();
    assertEquals(layout, restored);
    assertEquals(2, restored.patrolRoute(GuardRole.CROSSBOW_POST).size());
    assertTrue(restored.patrolRoute(GuardRole.SWORD_POST).isEmpty());
  }

  @Test
  void jailerRoutesAndRoutesWithNoActualMovementAreRejected() {
    for (String routes : List.of(
        "{\"JAILER\":[[8,11,13],[20,11,13]]}",
        "{\"SWORD_POST\":[[8,1,13],[8,1,13]]}",
        "{\"CROSSBOW_POST\":[]}")) {
      var result = CastleLayout.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
          {"custody_cell":[6,11,18],"release_point":[8,11,17],
           "evidence_containers":[[9,11,19],[9,12,19]],"patrol_routes":%s}
          """.formatted(routes)));
      assertTrue(result.error().isPresent(), routes);
    }
  }

  /** The Polynesian Coast centre is the king's hall and keeps the jail itself; no other civic building may. */
  @Test
  void aCentreMayKeepTheJailWhileOtherBuildingsStillMayNot() {
    assertTrue(CastleLayout.allowedIn("castle"));
    assertTrue(CastleLayout.allowedIn(Buildings.VILLAGE_CENTER_CATEGORY));
    assertFalse(CastleLayout.allowedIn("house"));
    BuildingInfo hall = parse("{\"structure\":\"village_center_polynesian_coast_1\",\"work_stations\":["
        + "{\"pos\":[7,4,7],\"occupation\":\"LEADER\"},"
        + "{\"pos\":[7,1,2],\"occupation\":\"GUARD\",\"guard_duty\":\"JAILER\"}]," + JAIL + "}");
    assertNull(hall.validate());
    assertEquals(new BlockPos(7, 2, 5), hall.getCastleLayout().custodyCell());
    assertEquals("castle amenities require the castle or village_center category",
        parse("{\"structure\":\"house_polynesian_coast_1\"," + JAIL + "}").validate());
  }

  private static BuildingInfo parse(String json) {
    return BuildingInfo.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
  }
}
