package com.quzzar.kithkyn.village.buildings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.quzzar.kithkyn.village.Occupation;

/**
 * Minimum planning outcomes implied by a building's authored purpose and
 * concrete amenities. Definitions may add more grants for their particular
 * mixed-use design, but may not hide what their category, rooms, storage, or
 * locally performed jobs provide from the village brain.
 */
public final class BuildingGrantContract {

  private static final Set<String> RETIRED = Set.of("PRIVATE_STORAGE", "GRAIN", "BREAD", "FUEL");

  private static final Map<String, List<String>> CATEGORY_GRANTS = Map.ofEntries(
      Map.entry("bakery", List.of("FOOD", "BAKED_GOODS")),
      Map.entry("blacksmith", List.of("REPAIR", "SMELTING", "TOOLS_IRON", "ARMOR_IRON", "SHIELDS")),
      Map.entry("butchery", List.of("FOOD", "MEAT", "LEATHER")),
      Map.entry("castle", List.of("GOVERNANCE", "PROTECTION", "CUSTODY")),
      Map.entry("church", List.of("HEALING")),
      Map.entry("couple_cottage", List.of("HOUSING", "FAMILY_HOUSING")),
      Map.entry("farm", List.of("FOOD", "CROPS")),
      Map.entry("fishery", List.of("FOOD", "FISH")),
      Map.entry("house", List.of("HOUSING")),
      Map.entry("hunting_lodge", List.of("FOOD", "MEAT", "LEATHER")),
      Map.entry("lumberjack", List.of("LOGS", "PLANKS")),
      Map.entry("market", List.of("TRADE")),
      Map.entry("mine", List.of("STONE", "ORES", "MINERALS")),
      Map.entry("stoneworks", List.of("CUT_STONE")),
      Map.entry("storehouse", List.of("STORAGE")),
      Map.entry("tavern", List.of("HOSPITALITY", "WANDERERS")),
      Map.entry("village_center", List.of("CIVIC_CENTER")),
      Map.entry("watchtower", List.of("PROTECTION", "RANGED_GUARD_POSTS")),
      Map.entry("well", List.of("WATER")));

  private static final Map<Occupation, List<String>> OCCUPATION_GRANTS = Map.ofEntries(
      Map.entry(Occupation.BAKER, List.of("FOOD", "BAKED_GOODS")),
      Map.entry(Occupation.BLACKSMITH, List.of("REPAIR", "SMELTING", "TOOLS_IRON", "ARMOR_IRON", "SHIELDS")),
      Map.entry(Occupation.BUILDER, List.of("CONSTRUCTION")),
      Map.entry(Occupation.BUTCHER, List.of("FOOD", "MEAT", "LEATHER")),
      Map.entry(Occupation.CLERIC, List.of("HEALING")),
      Map.entry(Occupation.FARMER, List.of("FOOD", "CROPS")),
      Map.entry(Occupation.FISHER, List.of("FOOD", "FISH")),
      Map.entry(Occupation.GUARD, List.of("PROTECTION")),
      Map.entry(Occupation.HERDER, List.of("LIVESTOCK", "WOOL")),
      Map.entry(Occupation.HUNTER, List.of("FOOD", "MEAT", "LEATHER")),
      Map.entry(Occupation.INNKEEPER, List.of("HOSPITALITY", "WANDERERS")),
      Map.entry(Occupation.LEADER, List.of("GOVERNANCE")),
      Map.entry(Occupation.LUMBERJACK, List.of("LOGS", "PLANKS")),
      Map.entry(Occupation.MASON, List.of("CUT_STONE")),
      Map.entry(Occupation.MERCHANT, List.of("TRADE")),
      Map.entry(Occupation.MINER, List.of("STONE", "ORES", "MINERALS")),
      Map.entry(Occupation.QUARTERMASTER, List.of("LOGISTICS")));

  private BuildingGrantContract() {
  }

  /** Retired planning labels rejected at the datapack boundary. */
  public static boolean isRetired(String grant) {
    return RETIRED.contains(grant);
  }

  /** Every minimum grant absent from this definition, with its source. */
  public static List<String> missing(BuildingInfo info) {
    List<String> problems = new ArrayList<>();
    Set<String> reported = new HashSet<>();
    require(problems, reported, info, "category " + info.getCategory(), CATEGORY_GRANTS.get(info.getCategory()));
    if (info.getCastleLayout() != null) {
      require(problems, reported, info, "castle amenities", CATEGORY_GRANTS.get("castle"));
    }
    if (!info.getBedLocations().isEmpty()) require(problems, reported, info, "beds", List.of("HOUSING"));
    if (!info.getCoupleBeds().isEmpty()) {
      require(problems, reported, info, "couple rooms", List.of("FAMILY_HOUSING"));
    }
    if (!info.getContainerLocations().isEmpty()) {
      require(problems, reported, info, "shared containers", List.of("STORAGE"));
    }

    Set<Occupation> seen = new HashSet<>();
    for (BuildingInfo.WorkStation station : info.workStations()) {
      if (station.worksiteCategory().isPresent() || !seen.add(station.occupation())) continue;
      require(problems, reported, info, station.occupation() + " station",
          OCCUPATION_GRANTS.get(station.occupation()));
    }
    return problems;
  }

  private static void require(List<String> problems, Set<String> reported, BuildingInfo info,
      String source, List<String> required) {
    if (required == null) return;
    for (String grant : required) {
      boolean declared = info.getGrants().contains(grant)
          || info.getConditionalGrants().stream().anyMatch(candidate -> candidate.capability().equals(grant));
      if (!declared && reported.add(grant)) problems.add(source + " requires grant " + grant);
    }
  }
}
