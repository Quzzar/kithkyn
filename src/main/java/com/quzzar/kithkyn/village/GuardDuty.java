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

  /** The captain is one authored station, or the first center guard in an older definition. */
  public static boolean isCaptain(RealPerson person) {
    Village village = person.getVillage();
    return person.getOccupation() == Occupation.GUARD && village != null && village.getTownCenter() != null
        && isCaptain(village.getJobAssignment(person.getUUID()), village.getTownCenter().getUUID(),
            village.getTownCenter().getInfo());
  }

  static boolean isCaptain(@Nullable JobAssignment assignment, @Nullable UUID townCenter,
      @Nullable BuildingInfo info) {
    if (assignment == null || townCenter == null || info == null
        || assignment.getOccupation() != Occupation.GUARD || assignment.isWallPost()
        || !townCenter.equals(assignment.getBuildingUUID())) {
      return false;
    }
    int index = 0;
    int firstUnspecifiedGuard = -1;
    int explicitCaptain = -1;
    for (Occupation occupation : info.getWorkLocations().values()) {
      if (occupation == Occupation.GUARD) {
        if (firstUnspecifiedGuard < 0 && info.getGuardRole(index) == null) firstUnspecifiedGuard = index;
        if (info.getGuardRole(index) == GuardRole.CAPTAIN) explicitCaptain = index;
      }
      index++;
    }
    if (explicitCaptain >= 0) return assignment.getStationIndex() == explicitCaptain;
    return firstUnspecifiedGuard >= 0 && assignment.getStationIndex() == firstUnspecifiedGuard;
  }

  /** Explicit sword patrols use the ordinary patrol route without woodcutting or an axe loadout. */
  public static boolean isSwordPatrol(RealPerson person) {
    return authoredRole(person) == GuardRole.PATROL;
  }

  @Nullable
  private static GuardRole authoredRole(RealPerson person) {
    Village village = person.getVillage();
    if (person.getOccupation() != Occupation.GUARD || village == null) return null;
    JobAssignment job = village.getJobAssignment(person.getUUID());
    if (job == null || job.isWallPost()) return null;
    Building building = village.getBuilding(job.getBuildingUUID());
    return building == null || building.getInfo() == null
        ? null : building.getInfo().getGuardRole(job.getStationIndex());
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
    if (info == null || stationIndex < 0) {
      return null;
    }
    int index = 0;
    for (Map.Entry<Long, Occupation> station : info.getWorkLocations().entrySet()) {
      if (index++ == stationIndex) {
        if (station.getValue() != Occupation.GUARD) {
          return null;
        }
        GuardRole role = info.getGuardRole(stationIndex);
        if (role != null && !role.hasPost()) return null;
        if (role == null && !info.getGrants().contains(RANGED_GUARD_POSTS)) return null;
        BlockPos position = origin.offset(BlockPos.of(station.getKey()).rotate(rotation));
        boolean ranged = role != GuardRole.SWORD_POST;
        return new GuardDuty(position, position.offset(new BlockPos(0, 0, -8).rotate(rotation)), ranged, ranged);
      }
    }
    return null;
  }
}
