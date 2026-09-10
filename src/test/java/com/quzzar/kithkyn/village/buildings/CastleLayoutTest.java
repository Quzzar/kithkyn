package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
