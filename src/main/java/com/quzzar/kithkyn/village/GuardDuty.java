package com.quzzar.kithkyn.village;

import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.village.buildings.Building;
import com.quzzar.kithkyn.village.buildings.BuildingInfo;
import com.quzzar.kithkyn.village.buildings.WallPost;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;

/**
 * The fixed watch shared by authored workplaces and the procedural wall.
 * Assignment ownership and staffing stay in the existing job system; this is
 * only its live equipment and post behavior, never separately saved state.
 */
public record GuardDuty(BlockPos position, BlockPos lookAt, boolean ranged, boolean backupSword) {

  public static final String RANGED_GUARD_POSTS = "RANGED_GUARD_POSTS";

  /** The founding center's guard station is the captain; it remains an ordinary GUARD job. */
  public static boolean isCaptain(RealPerson person) {
    Village village = person.getVillage();
    return person.getOccupation() == Occupation.GUARD && village != null && village.getTownCenter() != null
        && isCaptain(village.getJobAssignment(person.getUUID()), village.getTownCenter().getUUID());
  }

  static boolean isCaptain(@Nullable JobAssignment assignment, @Nullable UUID townCenter) {
    return assignment != null && townCenter != null && assignment.getOccupation() == Occupation.GUARD
        && !assignment.isWallPost() && townCenter.equals(assignment.getBuildingUUID());
  }

  /** No fixed duty means the ordinary guard keeps its patrol and sword/axe loadout. */
  @Nullable
  public static GuardDuty of(RealPerson person) {
    return resolve(person, false);
  }

  /** An unavailable workplace suspends the post, not the sentry's equipment or role. */
  @Nullable
  public static GuardDuty available(RealPerson person) {
    return resolve(person, true);
  }

  @Nullable
  private static GuardDuty resolve(RealPerson person, boolean requireAvailable) {
    if (person.getOccupation() != Occupation.GUARD) {
      return null;
    }
    Village village = person.getVillage();
    if (village == null) {
      return null;
    }
    JobAssignment job = village.getJobAssignment(person.getUUID());
    if (job == null || job.getOccupation() != Occupation.GUARD) {
      return null;
    }
    WallPost wall = village.getWallPost(job);
    if (wall != null) {
      return fromWall(wall);
    }
    Building building = village.getBuilding(job.getBuildingUUID());
    if (building == null || (requireAvailable && village.isBeingRebuilt(building.getUUID()))) {
      return null;
    }
    return fromBuilding(building.getInfo(), BlockPos.of(building.getOriginLocation()),
        building.getRotation(), job.getStationIndex());
  }

  /** Existing wall weapons and staffing priorities remain unchanged. */
  public static GuardDuty fromWall(WallPost post) {
    return new GuardDuty(post.position(), post.lookAt(), post.duty().usesCrossbow(), false);
  }

  /**
   * An opted-in definition uses its normal, globally indexed GUARD stations.
   * Idle watch faces authored north, rotated with the structure; combat can
   * look and fire in any direction without leaving the post.
   */
  @Nullable
  public static GuardDuty fromBuilding(@Nullable BuildingInfo info, BlockPos origin,
      Rotation rotation, int stationIndex) {
    if (info == null || !info.getGrants().contains(RANGED_GUARD_POSTS) || stationIndex < 0) {
      return null;
    }
    int index = 0;
    for (Map.Entry<Long, Occupation> station : info.getWorkLocations().entrySet()) {
      if (index++ == stationIndex) {
        if (station.getValue() != Occupation.GUARD) {
          return null;
        }
        BlockPos position = origin.offset(BlockPos.of(station.getKey()).rotate(rotation));
        return new GuardDuty(position, position.offset(new BlockPos(0, 0, -8).rotate(rotation)), true, true);
      }
    }
    return null;
  }
}
