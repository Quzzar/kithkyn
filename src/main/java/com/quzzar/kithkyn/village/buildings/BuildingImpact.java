package com.quzzar.kithkyn.village.buildings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.village.Occupation;
import com.quzzar.kithkyn.village.Village;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Calculated construction facts shared by demand checks and every planner option. */
public final class BuildingImpact {
  private static final Set<String> FOOD_GRANTS = Set.of("GRAIN", "MEAT", "BREAD");
  private static final Map<String, String> SERVICE_NAMES = Map.ofEntries(
      Map.entry("WATER", "fresh water"), Map.entry("ORES", "ore extraction"),
      Map.entry("FUEL", "fuel production"), Map.entry("PROTECTION", "village defense"),
      Map.entry("GRAIN", "grain production"), Map.entry("TRADE", "trade"),
      Map.entry("TRADE_INITIATIVE", "trade outreach"), Map.entry("REPAIR", "gear repair"),
      Map.entry("SMELTING", "metal smelting"), Map.entry("CUT_STONE", "stone cutting"),
      Map.entry("LOGS", "logging"), Map.entry("PLANKS", "plank production"),
      Map.entry("MEAT", "meat production"), Map.entry("BREAD", "bread production"),
      Map.entry("LEATHER", "leather production"), Map.entry("WOOL", "wool production"),
      Map.entry("HEALING", "healing"), Map.entry("WANDERERS", "newcomer attraction"));

  private BuildingImpact() {
  }

  public record Capacity(int generalBeds, int workerBeds, int containers, Map<Occupation, Integer> jobs,
      int cropPlots) {
    public Capacity {
      jobs = Map.copyOf(jobs);
    }

    public int workplaces() {
      return jobs.values().stream().mapToInt(Integer::intValue).sum();
    }

    /** Zero general beds is stated too, so storage and worker accommodation cannot look like housing. */
    public String describe(boolean net) {
      return number(generalBeds, net) + " general beds, " + number(workerBeds, net)
          + " beds reserved for workers, " + number(workplaces(), net) + " workplaces, "
          + number(containers, net) + " shared containers, " + number(cropPlots, net) + " crop plots";
    }
  }

  public record Services(Set<String> lostDuring, Set<String> lostAfter, Set<String> gained) {
    public Services {
      lostDuring = Set.copyOf(lostDuring);
      lostAfter = Set.copyOf(lostAfter);
      gained = Set.copyOf(gained);
    }

    public String describe() {
      Set<String> restored = new HashSet<>(lostDuring);
      restored.removeAll(lostAfter);
      return "Services unavailable only during work: " + describeServices(restored)
          + ". Services lost after completion: " + describeServices(lostAfter)
          + (lostAfter.contains("WATER") ? " (removes the village's last fresh-water source)" : "")
          + ". New services: " + describeServices(gained) + ".";
    }
  }

  public record Redevelopment(Capacity net, Services services, int displacedResidents, int affectedWorkplaces,
      int staffedFoodRemaining) {
  }

  /** Matches the housing rule: only the village center's workplace beds are general housing. */
  public static int generalBeds(BuildingInfo info) {
    return info.getWorkLocations().isEmpty() || Buildings.VILLAGE_CENTER_CATEGORY.equals(info.getCategory())
        ? info.getBedLocations().size() : 0;
  }

  public static Capacity capacity(BuildingInfo info, int cropPlots) {
    Map<Occupation, Integer> jobs = new HashMap<>();
    info.getWorkLocations().values().forEach(job -> jobs.merge(job, 1, Integer::sum));
    int general = generalBeds(info);
    return new Capacity(general, info.getBedLocations().size() - general,
        info.getContainerLocations().size(), jobs, cropPlots);
  }

  public static Capacity net(Capacity target, Collection<Capacity> affected) {
    int general = target.generalBeds();
    int workers = target.workerBeds();
    int stores = target.containers();
    int plots = target.cropPlots();
    Map<Occupation, Integer> jobs = new HashMap<>(target.jobs());
    for (Capacity loss : affected) {
      general -= loss.generalBeds();
      workers -= loss.workerBeds();
      stores -= loss.containers();
      plots -= loss.cropPlots();
      loss.jobs().forEach((job, count) -> jobs.merge(job, -count, Integer::sum));
    }
    jobs.values().removeIf(count -> count == 0);
    return new Capacity(general, workers, stores, jobs, plots);
  }

  public static Capacity capacity(Village village, BuildingInfo info) {
    return capacity(info, foodPlots(village, info));
  }

  public static Capacity net(Village village, BuildingInfo target, Collection<Building> affected) {
    return net(capacity(village, target), affected.stream().map(building -> capacity(village, building.getInfo())).toList());
  }

  /** Uses the same conditional capability resolution as the live village, holding observed supplies fixed. */
  public static Services services(Collection<BuildingInfo> before, Collection<BuildingInfo> surviving,
      BuildingInfo target, Predicate<Item> hasSupply) {
    Set<String> original = VillageCapabilities.resolve(before, hasSupply);
    Set<String> during = VillageCapabilities.resolve(surviving, hasSupply);
    List<BuildingInfo> completed = new ArrayList<>(surviving);
    completed.add(target);
    Set<String> after = VillageCapabilities.resolve(completed, hasSupply);
    return new Services(difference(original, during), difference(original, after), difference(after, original));
  }

  public static List<Building> affected(Village village, RedevelopmentPlan plan) {
    List<Building> affected = new ArrayList<>(plan.removed());
    plan.source().map(village::getBuilding).ifPresent(affected::add);
    return affected;
  }

  public static Redevelopment redevelopment(Village village, RedevelopmentPlan plan) {
    BuildingInfo target = Buildings.getByName(plan.target());
    List<Building> affected = affected(village, plan);
    Set<UUID> ids = affected.stream().map(Building::getUUID).collect(java.util.stream.Collectors.toSet());
    Set<UUID> displaced = village.getBedAssignmentsView().entrySet().stream()
        .filter(entry -> ids.contains(entry.getValue().getBuildingUUID())).map(Map.Entry::getKey)
        .collect(java.util.stream.Collectors.toSet());
    for (UUID resident : village.getPopulation()) {
      var person = village.getPerson(village.getLevel(), resident);
      Building home = person == null ? null : village.dependentHome(person);
      if (home != null && ids.contains(home.getUUID())) {
        displaced.add(resident);
      }
    }
    List<Building> surviving = village.getBuildings().stream()
        .filter(building -> !ids.contains(building.getUUID()) && building.getInfo() != null).toList();
    int staffedFood = (int) surviving.stream().filter(building -> isFood(building.getInfo()))
        .filter(building -> village.getJobAssignmentsView().values().stream()
            .anyMatch(job -> job.getBuildingUUID().equals(building.getUUID()))).count();
    Services services = services(village.getBuildings().stream().map(Building::getInfo)
        .filter(java.util.Objects::nonNull).toList(), surviving.stream().map(Building::getInfo).toList(), target,
        item -> village.hasItemStackInVillage(new ItemStack(item, 1)));
    return new Redevelopment(net(village, target, affected), services, displaced.size(),
        affected.stream().mapToInt(building -> building.getInfo().getWorkLocations().size()).sum(), staffedFood);
  }

  /** Authored crop capacity is measurable; food yield is not predicted. */
  public static int foodPlots(Village village, BuildingInfo info) {
    if (!isFood(info)) {
      return 0;
    }
    var template = village.getLevel().getStructureManager().getOrCreate(
        ResourceLocation.fromNamespaceAndPath(Kithkyn.MODID, info.getPath()));
    return template.palettes.isEmpty() ? 0 : (int) template.palettes.getFirst().blocks().stream()
        .filter(block -> block.state().is(BlockTags.CROPS)).count();
  }

  public static String describeServices(Collection<String> services) {
    List<String> names = services.stream().filter(service -> !service.equals("STORAGE"))
        .sorted().map(service -> SERVICE_NAMES.getOrDefault(service, service.toLowerCase().replace('_', ' '))).toList();
    return names.isEmpty() ? "none" : String.join(", ", names);
  }

  private static Set<String> difference(Set<String> first, Set<String> second) {
    Set<String> difference = new HashSet<>(first);
    difference.removeAll(second);
    difference.remove("STORAGE");
    return difference;
  }

  private static boolean isFood(BuildingInfo info) {
    return info.getGrants().stream().anyMatch(FOOD_GRANTS::contains);
  }

  private static String number(int number, boolean signed) {
    return signed && number > 0 ? "+" + number : Integer.toString(number);
  }
}
