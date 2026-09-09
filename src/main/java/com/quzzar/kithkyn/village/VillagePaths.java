package com.quzzar.kithkyn.village;

import com.quzzar.kithkyn.village.buildings.Building;
import com.quzzar.kithkyn.village.buildings.WallProject;
import com.quzzar.kithkyn.village.buildings.WorkerFooting;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Destinations for the existing center-to-entrance walking and grading system. */
public final class VillagePaths {
  private VillagePaths() { }

  /** A null target is temporarily unavailable, not a deleted destination. */
  public record Destination(String id, String revision, @Nullable BlockPos target,
      double reachSqr, boolean initialEligible) { }

  public record Gate(String id, String revision, BlockPos anchor) { }

  public static List<Destination> destinations(ServerLevel level, Village village) {
    List<Destination> destinations = new ArrayList<>();
    for (Building building : village.getBuildings()) {
      if (building == village.getTownCenter()) continue;
      LocationManager.Entrance entrance = LocationManager.getEntrance(level, building);
      destinations.add(new Destination(building.getUUID().toString(), revision(building),
          entrance == null ? BlockPos.of(building.getCenterLocation()) : entrance.doorstep(),
          entrance == null ? building.getRadius() * building.getRadius() * 0.4D : 6.25D,
          entrance != null));
    }
    for (Gate gate : gates(village.getWallProject())) {
      BlockPos target = approach(gate.anchor(), pos -> WorkerFooting.canStand(level, pos));
      destinations.add(new Destination(gate.id(), gate.revision(), target, 1.0D, target != null));
    }
    return List.copyOf(destinations);
  }

  /** All identities survive temporarily blocked or unloaded entrances. */
  public static Set<String> registeredIds(Village village) {
    Set<String> ids = new HashSet<>();
    village.getBuildings().forEach(building -> ids.add(building.getUUID().toString()));
    gates(village.getWallProject()).forEach(gate -> ids.add(gate.id()));
    return Set.copyOf(ids);
  }

  /** Gate identity follows the saved passage, never construction progress or current terrain. */
  static List<Gate> gates(@Nullable WallProject wall) {
    if (wall == null) return List.of();
    List<Gate> gates = new ArrayList<>();
    for (int index = 0; index < wall.getRing().size(); index++) {
      long packed = wall.getRing().get(index);
      if (!wall.getGates().contains(packed)) continue;
      BlockPos anchor = BlockPos.of(packed).atY(wall.getGround().get(index));
      String id = "gate:" + BlockPos.asLong(anchor.getX(), 0, anchor.getZ());
      gates.add(new Gate(id, wall.getStyle().id() + ":" + wall.getTier() + ":"
          + anchor.getY() + ":" + wall.getDeck().get(index), anchor));
    }
    return List.copyOf(gates);
  }

  /** Search around saved ground, not the heightmap that sees the gatehouse roof. */
  @Nullable
  static BlockPos approach(BlockPos anchor, Predicate<BlockPos> safe) {
    for (int distance = 0; distance <= 3; distance++) {
      BlockPos above = anchor.above(distance);
      if (safe.test(above)) return above;
      if (distance > 0 && safe.test(anchor.below(distance))) return anchor.below(distance);
    }
    return null;
  }

  private static String revision(Building building) {
    return building.getName() + ":" + building.getOriginLocation() + ":" + building.getRotation();
  }
}
