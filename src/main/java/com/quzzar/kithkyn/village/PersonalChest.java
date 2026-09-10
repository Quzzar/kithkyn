package com.quzzar.kithkyn.village;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.village.buildings.Building;
import com.quzzar.kithkyn.village.buildings.BuildingInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;

/**
 * A home's own chest: the container a building definition lists under
 * {@code personal_containers} instead of {@code containers}. It belongs to
 * the people whose beds share access to it, and to nobody else. Definitions
 * may bind beds to their room's containers; older homes use the nearest
 * personal container. It is never registered as village storage, so no worker fetches from
 * it, no deposit lands in it, the planner does not count it, and the
 * quartermaster's sweep never sees it. What a villager puts there is theirs;
 * a player taking from it is theft like any other village chest
 * (docs/building-spec.md, "Who may take from a chest").
 *
 * <p>Nothing here is stored. An adult's chest is derived from their bed
 * assignment; a dependent child's is derived from the resident parent's home.
 * The building definition is read every time, so an edit or rebuild is picked
 * up as it happens.
 */
public final class PersonalChest {

  private PersonalChest() {
  }

  /** The world positions of this building's own chests, in definition order. */
  public static List<BlockPos> chests(Building building) {
    List<BlockPos> out = new ArrayList<>();
    BuildingInfo info = building.getInfo();
    if (info == null) {
      return out;
    }
    BlockPos origin = BlockPos.of(building.getOriginLocation());
    for (Long local : info.getPersonalContainerLocations()) {
      out.add(origin.offset(BlockPos.of(local).rotate(building.getRotation())));
    }
    return out;
  }

  /** Every home chest in the village, for anyone asking whose a chest is. */
  public static Set<BlockPos> allChests(Village village) {
    Set<BlockPos> out = new HashSet<>();
    for (Building building : village.getBuildings()) {
      if (building != null) {
        out.addAll(chests(building));
      }
    }
    return out;
  }

  /**
   * The building this person lives in, or null when they have neither a bed nor
   * a valid dependent place in a parent's home.
   */
  @Nullable
  public static Building home(RealPerson person) {
    Village village = person.getVillage();
    if (village == null) {
      return null;
    }
    BedAssignment bed = village.getBedAssignment(person.getUUID());
    if (bed == null) {
      return village.dependentHome(person);
    }
    if (village.isBeingRebuilt(bed.getBuildingUUID())) {
      return null;
    }
    return village.getBuilding(bed.getBuildingUUID());
  }

  /**
   * This person's room chest, or the nearest home chest for a definition without
   * explicit room bindings. A dependent inherits the same room as their resident parent.
   */
  @Nullable
  public static BlockPos of(RealPerson person) {
    Building home = home(person);
    if (home == null) {
      return null;
    }
    Village village = person.getVillage();
    BedAssignment bed = village.getBedAssignment(person.getUUID());
    if (bed == null) {
      bed = village.dependentBedAssignment(person);
    }
    return bed == null ? null : forBed(home.getInfo(), BlockPos.of(home.getOriginLocation()),
        home.getRotation(), bed.getBedIndex());
  }

  /** Resolves only a bed's allowed containers, preserving the structure's rotation. */
  @Nullable
  static BlockPos forBed(@Nullable BuildingInfo info, BlockPos origin, Rotation rotation, int bedIndex) {
    if (info == null || bedIndex < 0 || bedIndex >= info.getBedLocations().size()) {
      return null;
    }
    BlockPos localBed = BlockPos.of(info.getBedLocations().get(bedIndex));
    List<BlockPos> candidates;
    if (info.getBedContainers() != null) {
      candidates = info.getBedContainers().stream()
          .filter(binding -> binding.bed().equals(localBed))
          .findFirst().map(BuildingInfo.BedContainers::containers).orElse(List.of());
    } else {
      candidates = info.getPersonalContainerLocations().stream().map(BlockPos::of).toList();
    }
    BlockPos nearest = null;
    double best = Double.MAX_VALUE;
    for (BlockPos chest : candidates) {
      double distance = chest.distSqr(localBed);
      if (nearest == null || distance < best) {
        nearest = chest;
        best = distance;
      }
    }
    return nearest == null ? null : origin.offset(nearest.rotate(rotation));
  }

  /**
   * The chest itself, when its chunk is resident; null otherwise. Never pages
   * a chunk in to answer: a villager asked about their things while far from
   * home says they cannot recall, rather than costing the tick a disk read
   * (docs/population-and-labor.md, the ledger rule).
   */
  @Nullable
  public static Container container(RealPerson person, BlockPos chest) {
    if (!(person.level() instanceof ServerLevel level) || !level.hasChunkAt(chest)) {
      return null;
    }
    return level.getBlockEntity(chest) instanceof Container container ? container : null;
  }

  /** Names only the residents who use this exact personal container. */
  public static List<String> housemateNames(RealPerson person) {
    Village village = person.getVillage();
    BlockPos chest = of(person);
    if (village == null || chest == null || !(person.level() instanceof ServerLevel level)) {
      return List.of();
    }
    List<String> names = new ArrayList<>();
    for (UUID otherId : village.getPopulation()) {
      if (otherId.equals(person.getUUID())) {
        continue;
      }
      RealPerson housemate = village.getPerson(level, otherId);
      if (housemate != null && chest.equals(of(housemate))) {
        names.add(housemate.getFullName());
      }
    }
    return names;
  }

  /** What a container holds, by kind, in the plain words a briefing uses: "12 wheat, 3 apple". */
  public static String summarize(Container container) {
    Map<String, Integer> counts = new LinkedHashMap<>();
    for (int slot = 0; slot < container.getContainerSize(); slot++) {
      ItemStack stack = container.getItem(slot);
      if (!stack.isEmpty()) {
        counts.merge(stack.getItem().getDescription().getString().toLowerCase(Locale.ROOT),
            stack.getCount(), Integer::sum);
      }
    }
    List<String> parts = new ArrayList<>();
    counts.forEach((name, count) -> parts.add(count + " " + name));
    return String.join(", ", parts);
  }
}
