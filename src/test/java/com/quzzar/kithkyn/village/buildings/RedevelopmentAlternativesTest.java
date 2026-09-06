package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;

import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Rotation;

class RedevelopmentAlternativesTest {
  private static final BuildingImpact.Capacity FARM_GAIN =
      new BuildingImpact.Capacity(0, 0, 2, Map.of(), 26);

  @Test
  void affordableWaterPreservingPlacementIsPreferredButCheaperDestructiveTradeoffRemains() {
    var both = option("farm_plains_2", Optional.empty(), true, 2, 2, 120, Set.of("WATER"));
    var one = option("farm_plains_2", Optional.empty(), true, 1, 4, 52, Set.of());
    var result = RedevelopmentAlternatives.select(List.of(both, one));
    assertEquals(2, result.size());
    assertEquals(one, result.getFirst().evaluated());
    assertTrue(result.getFirst().preferred());
    assertTrue(result.getFirst().preference().contains("Preferred placement"));
    assertFalse(result.getLast().preferred());
    assertEquals(both, result.getLast().evaluated());
  }

  @Test
  void unavailableServicePreservingAlternativeDoesNotHideAffordableDemolition() {
    var saveForOne = option("farm_plains_2", Optional.empty(), false, 1, 4, 52, Set.of());
    var affordableBoth = option("farm_plains_2", Optional.empty(), true, 2, 2, 120, Set.of("WATER"));
    var result = RedevelopmentAlternatives.select(List.of(saveForOne, affordableBoth));
    assertEquals(2, result.size());
    assertEquals(affordableBoth, result.getFirst().evaluated());
  }

  @Test
  void sameImpactAndMaterialsNeedsOnlyTheLowerWorkPlacement() {
    var costly = option("farm_plains_2", Optional.empty(), true, 1, 4, 75, Set.of());
    var simple = option("farm_plains_2", Optional.empty(), true, 1, 4, 52, Set.of());
    var result = RedevelopmentAlternatives.select(List.of(costly, simple));
    assertEquals(1, result.size());
    assertEquals(simple, result.getFirst().evaluated());
    assertFalse(result.getFirst().hasAlternatives());
  }

  @Test
  void lowerMaterialCostVersusLessWorkRemainsARealTradeoff() {
    var lessWork = option("farm_plains_2", Optional.empty(), true, 1, 4, 52, Set.of());
    var lessMaterial = option("farm_plains_2", Optional.empty(), true, 1, 2, 75, Set.of());
    assertEquals(2, RedevelopmentAlternatives.select(List.of(lessWork, lessMaterial)).size());
  }

  @Test
  void materialCountsAreComparedByItemRatherThanAddingUnlikeResources() {
    var logs = option("farm_plains_2", Optional.empty(), true, 1, 4, 52, Set.of());
    var stone = withEconomics(logs, List.of(new MaterialAmount(Items.COBBLESTONE, 1)), List.of());
    assertEquals(2, RedevelopmentAlternatives.select(List.of(logs, stone)).size());
  }

  @Test
  void differentSurplusSalvageIsNotDiscardedJustBecauseBothNetRecipesAreFree() {
    var original = option("farm_plains_2", Optional.empty(), true, 1, 4, 52, Set.of());
    var noSurplus = withEconomics(original, List.of(new MaterialAmount(Items.OAK_LOG, 4)),
        List.of(new MaterialAmount(Items.OAK_LOG, 4)));
    var surplus = withEconomics(original, List.of(new MaterialAmount(Items.OAK_LOG, 4)),
        List.of(new MaterialAmount(Items.OAK_LOG, 8)));
    assertEquals(2, RedevelopmentAlternatives.select(List.of(noSurplus, surplus)).size());
  }

  @Test
  void differentTargetsAndUpgradeSourcesCannotBeGroupedAway() {
    var first = option("farm_plains_2", Optional.of(UUID.randomUUID()), true, 1, 4, 52, Set.of());
    var second = option("farm_plains_2", Optional.of(UUID.randomUUID()), true, 1, 4, 52, Set.of());
    var house = option("house_plains_2", Optional.empty(), true, 1, 4, 52, Set.of());
    assertEquals(3, RedevelopmentAlternatives.select(List.of(first, second, house)).size());
  }

  @Test
  void displacementAndBedCapacityTradeoffsRemainVisible() {
    var original = option("farm_plains_2", Optional.empty(), true, 1, 4, 52, Set.of());
    var displaced = new RedevelopmentAlternatives.Evaluated(original.choice(),
        new BuildingImpact.Redevelopment(FARM_GAIN, original.impact().services(), 3, 0, 1), true);
    var moreHousing = new RedevelopmentAlternatives.Evaluated(original.choice(),
        new BuildingImpact.Redevelopment(new BuildingImpact.Capacity(2, 0, 2, Map.of(), 26),
            original.impact().services(), 0, 0, 1), true);
    assertEquals(3, RedevelopmentAlternatives.select(List.of(original, displaced, moreHousing)).size());
  }

  private static RedevelopmentAlternatives.Evaluated option(String target, Optional<UUID> source,
      boolean affordable, int removed, int logs, int work, Set<String> lost) {
    List<Building> victims = java.util.stream.IntStream.range(0, removed)
        .mapToObj(index -> new Building("well_plains_1", Rotation.NONE)).toList();
    RedevelopmentPlan plan = new RedevelopmentPlan(UUID.randomUUID(), target,
        source.isPresent() ? ConstructionMode.UPGRADE : ConstructionMode.FRESH, source, 0, Rotation.NONE, 0,
        victims, List.of(), List.of(new MaterialAmount(Items.OAK_LOG, logs)), List.of(),
        List.of(), LongStream.range(0, work).boxed().toList());
    var services = new BuildingImpact.Services(lost, lost, Set.of());
    return new RedevelopmentAlternatives.Evaluated(
        new ConstructionChoice(new BuildingInfo(target), plan.mode(), plan),
        new BuildingImpact.Redevelopment(FARM_GAIN, services, 0, 0, 1), affordable);
  }

  private static RedevelopmentAlternatives.Evaluated withEconomics(RedevelopmentAlternatives.Evaluated original,
      List<MaterialAmount> required, List<MaterialAmount> salvage) {
    RedevelopmentPlan before = original.choice().redevelopment();
    RedevelopmentPlan plan = new RedevelopmentPlan(UUID.randomUUID(), before.target(), before.mode(), before.source(),
        before.ground(), before.rotation(), before.targetFingerprint(), before.removed(), before.blocks(), required,
        salvage, before.prepBreak(), before.prepFill());
    return new RedevelopmentAlternatives.Evaluated(new ConstructionChoice(original.choice().info(), plan.mode(), plan),
        original.impact(), original.affordable());
  }
}
