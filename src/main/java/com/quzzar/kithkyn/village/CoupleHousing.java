package com.quzzar.kithkyn.village;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.quzzar.kithkyn.village.buildings.Building;
import com.quzzar.kithkyn.village.buildings.BuildingInfo;

/** Stateless pair allocation over the village's existing bed ledger. */
final class CoupleHousing {
  private CoupleHousing() {}

  /** Validates both destination beds before releasing or changing either spouse's assignment. */
  static boolean assign(UUID first, UUID second, Building building,
      Map<UUID, BedAssignment> assigned, List<BedAssignment> open,
      java.util.function.Predicate<BuildingInfo.CoupleBeds> allowed) {
    BuildingInfo.CoupleBeds pair = availablePair(first, second, building, assigned, open, allowed);
    if (pair == null) return false;
    BuildingInfo info = building.getInfo();
    int firstIndex = info.getBedLocations().indexOf(pair.first().asLong());
    int secondIndex = info.getBedLocations().indexOf(pair.second().asLong());
    UUID buildingId = building.getUUID();
    // Keep either spouse's existing side, including a partially occupied room after a return.
    if (holds(assigned.get(first), buildingId, secondIndex)
        || holds(assigned.get(second), buildingId, firstIndex)) {
      int swap = firstIndex;
      firstIndex = secondIndex;
      secondIndex = swap;
    }
    release(first, assigned, open);
    release(second, assigned, open);
    int chosenFirst = firstIndex;
    int chosenSecond = secondIndex;
    open.removeIf(bed -> holds(bed, buildingId, chosenFirst) || holds(bed, buildingId, chosenSecond));
    assigned.put(first, new BedAssignment(first, buildingId, firstIndex));
    assigned.put(second, new BedAssignment(second, buildingId, secondIndex));
    return true;
  }

  /** The read-only admission check and the final assignment use the same pair eligibility. */
  static BuildingInfo.CoupleBeds availablePair(UUID first, UUID second, Building building,
      Map<UUID, BedAssignment> assigned, List<BedAssignment> open,
      java.util.function.Predicate<BuildingInfo.CoupleBeds> allowed) {
    if (first.equals(second) || building.getInfo() == null) return null;
    BuildingInfo info = building.getInfo();
    for (BuildingInfo.CoupleBeds pair : info.getCoupleBeds()) {
      if (allowed.test(pair)
          && available(building.getUUID(), info.getBedLocations().indexOf(pair.first().asLong()),
              first, second, assigned, open)
          && available(building.getUUID(), info.getBedLocations().indexOf(pair.second().asLong()),
              first, second, assigned, open)) return pair;
    }
    return null;
  }

  /** A pair is free only when both actual slots exist and neither has an occupant. */
  static int freePairs(Building building, Map<UUID, BedAssignment> assigned, List<BedAssignment> open,
      java.util.function.Predicate<BuildingInfo.CoupleBeds> allowed) {
    BuildingInfo info = building.getInfo();
    if (info == null) return 0;
    int count = 0;
    for (BuildingInfo.CoupleBeds pair : info.getCoupleBeds()) {
      if (allowed.test(pair) && available(building.getUUID(), info.getBedLocations().indexOf(pair.first().asLong()),
          null, null, assigned, open)
          && available(building.getUUID(), info.getBedLocations().indexOf(pair.second().asLong()),
              null, null, assigned, open)) count++;
    }
    return count;
  }

  private static boolean available(UUID building, int index, UUID first, UUID second,
      Map<UUID, BedAssignment> assigned, List<BedAssignment> open) {
    if (index < 0) return false;
    for (Map.Entry<UUID, BedAssignment> entry : assigned.entrySet()) {
      if (holds(entry.getValue(), building, index)) {
        return entry.getKey().equals(first) || entry.getKey().equals(second);
      }
    }
    return open.stream().anyMatch(bed -> holds(bed, building, index));
  }

  private static boolean holds(BedAssignment bed, UUID building, int index) {
    return bed != null && bed.getBuildingUUID().equals(building) && bed.getBedIndex() == index;
  }

  private static void release(UUID resident, Map<UUID, BedAssignment> assigned, List<BedAssignment> open) {
    BedAssignment previous = assigned.remove(resident);
    if (previous != null) open.add(previous.setPersonUUID(null));
  }
}
