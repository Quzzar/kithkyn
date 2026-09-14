package com.quzzar.kithkyn.village.buildings;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.quzzar.kithkyn.village.GuardRole;
import com.quzzar.kithkyn.village.Occupation;

/**
 * Relationships that can only be validated after the complete building catalog
 * has been decoded. A physical worksite is not a vacancy, so it is useful only
 * when a post in the same variant routes the same occupation to its category.
 */
public final class BuildingCatalogContract {

  /** The lead, path and grading posts unlocked by the population rules in {@code Village}. */
  private static final int CENTER_BUILDER_POSTS = 3;

  /** Minimum work that makes each productive category what its id claims it is. */
  private static final Map<String, List<Occupation>> REQUIRED_WORK = Map.ofEntries(
      Map.entry("bakery", List.of(Occupation.BAKER)),
      Map.entry("blacksmith", List.of(Occupation.BLACKSMITH)),
      Map.entry("butchery", List.of(Occupation.BUTCHER)),
      Map.entry("castle", List.of(Occupation.LEADER, Occupation.GUARD)),
      Map.entry("church", List.of(Occupation.CLERIC)),
      Map.entry("farm", List.of(Occupation.FARMER)),
      Map.entry("fishery", List.of(Occupation.FISHER)),
      Map.entry("hunting_lodge", List.of(Occupation.HUNTER)),
      Map.entry("lumberjack", List.of(Occupation.LUMBERJACK)),
      Map.entry("market", List.of(Occupation.MERCHANT)),
      Map.entry("mine", List.of(Occupation.MINER)),
      Map.entry("stoneworks", List.of(Occupation.MASON)),
      Map.entry("tavern", List.of(Occupation.INNKEEPER)),
      Map.entry("village_center", List.of(Occupation.BUILDER, Occupation.GUARD)),
      Map.entry("watchtower", List.of(Occupation.GUARD)));

  private record WorksiteKey(String variant, String category, Occupation occupation) {
  }

  private BuildingCatalogContract() {
  }

  /** Every catalog-level problem, grouped by the definition that authored it. */
  public static Map<String, List<String>> problems(Map<String, BuildingInfo> definitions) {
    Map<WorksiteKey, Set<String>> routes = new HashMap<>();
    Map<WorksiteKey, Set<String>> worksites = new HashMap<>();
    Map<String, LinkedHashSet<String>> problems = new LinkedHashMap<>();
    List<BuildingInfo> ordered = definitions.values().stream()
        .sorted(Comparator.comparing(BuildingInfo::getName)).toList();

    for (BuildingInfo info : ordered) {
      if (info.getUpgradesFrom() != null && !definitions.containsKey(info.getUpgradesFrom())) {
        add(problems, info.getName(), "upgrade predecessor " + info.getUpgradesFrom() + " is missing");
      }
      for (String starting : info.getStartingBuildings()) {
        if (!definitions.containsKey(starting)) {
          add(problems, info.getName(), "starting building " + starting + " is missing");
        }
      }
      Set<Occupation> authoredWork = new LinkedHashSet<>(info.getWorkLocations().values());
      authoredWork.addAll(info.getWorksiteLocations().values());
      for (Occupation required : REQUIRED_WORK.getOrDefault(info.getCategory(), List.of())) {
        if (!authoredWork.contains(required)) {
          add(problems, info.getName(), info.getCategory() + " requires a " + required
              + " work station or physical worksite");
        }
      }
      if (info.getCastleLayout() != null) {
        for (Occupation required : REQUIRED_WORK.get("castle")) {
          if (!authoredWork.contains(required)) {
            add(problems, info.getName(), "castle amenities require a " + required
                + " work station or physical worksite");
          }
        }
      }
      if ("village_center".equals(info.getCategory())) {
        validateCenterPosts(info, problems);
      }
      for (BuildingInfo.WorkStation station : info.workStations()) {
        station.worksiteCategory().ifPresent(category -> routes
            .computeIfAbsent(new WorksiteKey(info.getVariant(), category, station.occupation()), ignored -> new LinkedHashSet<>())
            .add(info.getName()));
      }
      for (Occupation occupation : new LinkedHashSet<>(info.getWorksiteLocations().values())) {
        worksites.computeIfAbsent(new WorksiteKey(info.getVariant(), info.getCategory(), occupation),
            ignored -> new LinkedHashSet<>()).add(info.getName());
      }
    }

    for (Map.Entry<WorksiteKey, Set<String>> entry : worksites.entrySet()) {
      if (routes.containsKey(entry.getKey())) {
        continue;
      }
      WorksiteKey key = entry.getKey();
      for (String owner : entry.getValue()) {
        add(problems, owner, key.occupation() + " worksite has no routed vacancy for "
            + key.variant() + "/" + key.category());
      }
    }
    for (Map.Entry<WorksiteKey, Set<String>> entry : routes.entrySet()) {
      if (worksites.containsKey(entry.getKey())) {
        continue;
      }
      WorksiteKey key = entry.getKey();
      for (String owner : entry.getValue()) {
        add(problems, owner, key.occupation() + " vacancy routes to missing "
            + key.variant() + "/" + key.category() + " worksite");
      }
    }

    Map<String, List<String>> immutable = new LinkedHashMap<>();
    problems.forEach((name, found) -> immutable.put(name, List.copyOf(found)));
    return Map.copyOf(immutable);
  }

  /** A center owns civic vacancies; production blocks remain in their physical buildings. */
  private static void validateCenterPosts(BuildingInfo info,
      Map<String, LinkedHashSet<String>> problems) {
    long builders = info.workStations().stream()
        .filter(station -> station.occupation() == Occupation.BUILDER)
        .count();
    if (builders != CENTER_BUILDER_POSTS) {
      add(problems, info.getName(), "village_center requires exactly " + CENTER_BUILDER_POSTS
          + " BUILDER posts for lead, path and grading duties; found " + builders);
    }

    long captains = info.workStations().stream()
        .filter(station -> station.occupation() == Occupation.GUARD)
        .filter(station -> station.guardDuty().orElse(null) == GuardRole.CAPTAIN)
        .count();
    if (captains != 1) {
      add(problems, info.getName(), "village_center requires exactly one explicit CAPTAIN guard post; found "
          + captains);
    }

    for (BuildingInfo.WorkStation station : info.workStations()) {
      if (station.occupation() == Occupation.MINER
          && !station.worksiteCategory().filter("mine"::equals).isPresent()) {
        add(problems, info.getName(), "a center-owned MINER post must route to the mine worksite");
      }
      if (station.occupation() == Occupation.QUARTERMASTER
          && station.worksiteCategory().isEmpty() && info.getContainerLocations().isEmpty()) {
        add(problems, info.getName(),
            "a center-owned QUARTERMASTER post needs center storage or a storehouse route");
      }
    }
  }

  private static void add(Map<String, LinkedHashSet<String>> problems, String name, String problem) {
    problems.computeIfAbsent(name, ignored -> new LinkedHashSet<>()).add(problem);
  }
}
