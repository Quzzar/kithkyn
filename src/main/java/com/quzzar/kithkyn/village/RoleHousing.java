package com.quzzar.kithkyn.village;

import javax.annotation.Nullable;

import com.quzzar.kithkyn.village.buildings.Building;
import com.quzzar.kithkyn.village.buildings.BuildingInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;

/** Role room entitlement comes from the ordinary job ledger, including the center's existing captain. */
final class RoleHousing {
  private RoleHousing() {}

  static boolean matches(Village village, Building home, BuildingInfo.RoomReservation room,
      @Nullable JobAssignment job) {
    if (job == null) return false;
    if (room.occupation().isPresent()) {
      var stations = java.util.List.copyOf(home.getInfo().getWorkLocations().values());
      return job.getOccupation() == room.occupation().get() && job.getBuildingUUID().equals(home.getUUID())
          && job.getStationIndex() >= 0 && job.getStationIndex() < stations.size()
          && stations.get(job.getStationIndex()) == job.getOccupation();
    }
    Building center = village.getTownCenter();
    return room.guardRole().orElse(null) == GuardRole.CAPTAIN && center != null
        && GuardDuty.isCaptain(job, center.getUUID(), center.getInfo());
  }

  /** A transfer never gives up a usable old home for a missing, obstructed or rebuilding room. */
  static boolean usable(Village village, Building home, BuildingInfo.RoomReservation room) {
    if (village.isBeingRebuilt(home.getUUID())) return false;
    var level = village.getLevel();
    if (level == null) return true;
    for (BlockPos authored : room.beds()) {
      BlockPos position = BlockPos.of(home.getOriginLocation()).offset(authored.rotate(home.getRotation()));
      if (!level.hasChunkAt(position)) return false;
      var state = level.getBlockState(position);
      if (!(state.getBlock() instanceof BedBlock)) return false;
      BlockPos other = position.relative(state.getValue(BedBlock.PART) == BedPart.HEAD
          ? state.getValue(BedBlock.FACING).getOpposite() : state.getValue(BedBlock.FACING));
      var otherState = level.getBlockState(other);
      if (!(otherState.getBlock() instanceof BedBlock)
          || otherState.getValue(BedBlock.PART) == state.getValue(BedBlock.PART)
          || otherState.getValue(BedBlock.FACING) != state.getValue(BedBlock.FACING)
          || !level.getBlockState(position.above()).getCollisionShape(level, position.above()).isEmpty()
          || !level.getBlockState(other.above()).getCollisionShape(level, other.above()).isEmpty()) return false;
    }
    return true;
  }
}
