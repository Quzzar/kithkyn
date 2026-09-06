package com.quzzar.kithkyn.village.buildings;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.quzzar.kithkyn.village.Village;

import net.minecraft.world.item.Item;

/** Groups placements for one project while retaining real service, capacity, and material tradeoffs. */
public final class RedevelopmentAlternatives {
  private RedevelopmentAlternatives() {
  }

  public record Evaluated(ConstructionChoice choice, BuildingImpact.Redevelopment impact, boolean affordable) {
    private RedevelopmentPlan plan() {
      return choice.redevelopment();
    }

    private int work() {
      return plan().blocks().size() + plan().prepBreak().size() + plan().prepFill().size();
    }
  }

  public record Option(Evaluated evaluated, boolean preferred, boolean hasAlternatives) {
    public String preference() {
      if (!hasAlternatives) {
        return "";
      }
      return preferred ? "Preferred placement for this target. " : "Alternative placement for the same target. ";
    }
  }

  private record Project(String target, ConstructionMode mode, Optional<UUID> source) {
  }

  public static List<Option> select(Village village, List<ConstructionChoice> choices, Map<Item, Integer> stock) {
    return select(choices.stream().map(choice -> new Evaluated(choice,
        BuildingImpact.redevelopment(village, choice.redevelopment()),
        ConstructionQuote.capture(choice, stock).affordable())).toList());
  }

  /**
   * Among equivalent impacts, only an alternative no dearer in every material and no more work can replace
   * another. Different salvage totals remain visible because unused salvage is returned later.
   * Across different impacts, affordability comes first, then service preservation, displacement,
   * affected workplaces and removal work. This is an explicit placement preference, never an eligibility gate.
   */
  public static List<Option> select(List<Evaluated> evaluated) {
    Map<Project, List<Evaluated>> groups = new LinkedHashMap<>();
    for (Evaluated alternative : evaluated) {
      RedevelopmentPlan plan = alternative.plan();
      Project group = new Project(plan.target(), plan.mode(), plan.source());
      List<Evaluated> retained = groups.computeIfAbsent(group, ignored -> new ArrayList<>());
      if (retained.stream().anyMatch(prior -> replaces(prior, alternative))) {
        continue;
      }
      retained.removeIf(prior -> replaces(alternative, prior));
      retained.add(alternative);
    }
    Comparator<Evaluated> order = Comparator.comparing((Evaluated value) -> !value.affordable())
        .thenComparingInt(value -> value.impact().services().lostAfter().size())
        .thenComparingInt(value -> value.impact().services().lostDuring().size())
        .thenComparingInt(value -> value.impact().displacedResidents())
        .thenComparingInt(value -> value.impact().affectedWorkplaces())
        .thenComparingInt(value -> value.plan().removed().size())
        .thenComparingInt(Evaluated::work)
        .thenComparingLong(value -> value.plan().ground())
        .thenComparing(value -> value.plan().rotation());
    List<Option> result = new ArrayList<>();
    for (List<Evaluated> group : groups.values()) {
      group.sort(order);
      for (int index = 0; index < group.size(); index++) {
        result.add(new Option(group.get(index), index == 0, group.size() > 1));
      }
    }
    return List.copyOf(result);
  }

  private static boolean replaces(Evaluated better, Evaluated other) {
    if (!better.impact().equals(other.impact())
        || !MaterialAmount.tally(better.plan().salvage()).equals(MaterialAmount.tally(other.plan().salvage()))
        || better.work() > other.work()
        || better.plan().removed().size() > other.plan().removed().size()) {
      return false;
    }
    Map<Item, Integer> cost = MaterialAmount.tally(better.plan().netRequired());
    Map<Item, Integer> otherCost = MaterialAmount.tally(other.plan().netRequired());
    return cost.entrySet().stream().allMatch(entry -> entry.getValue() <= otherCost.getOrDefault(entry.getKey(), 0));
  }
}
