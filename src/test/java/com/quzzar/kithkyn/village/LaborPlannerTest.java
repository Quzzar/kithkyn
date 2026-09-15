package com.quzzar.kithkyn.village;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

class LaborPlannerTest {

  @Test
  void aSavedFarmBlockedOnLogsSelectsAnExistingVacantLumberjackPost() {
    UUID lumberyard = UUID.randomUUID();
    UUID field = UUID.randomUUID();
    JobAssignment lumberjack = new JobAssignment(null, Occupation.LUMBERJACK, lumberyard, 0);
    JobAssignment farmer = new JobAssignment(null, Occupation.FARMER, field, 0);

    JobAssignment selected = LaborPlanner.openProjectProducerPost(
        List.of(new ItemStack(Items.OAK_LOG, 2)), List.of(farmer, lumberjack), List.of(),
        building -> Map.of(lumberyard, List.of("LOGS", "PLANKS"), field, List.of("FOOD", "CROPS"))
            .getOrDefault(building, List.of()));

    assertEquals(lumberjack, selected);
  }

  @Test
  void aMissingMaterialWithoutAProducerDoesNotInventAReassignment() {
    JobAssignment farmer = new JobAssignment(null, Occupation.FARMER, UUID.randomUUID(), 0);

    assertNull(LaborPlanner.openProjectProducerPost(
        List.of(new ItemStack(Items.IRON_INGOT, 2)), List.of(farmer), List.of(),
        ignored -> List.of("FOOD", "CROPS")));
  }

  @Test
  void aStaffedProducerPreventsRepeatedReassignmentIntoEquivalentVacancies() {
    UUID staffedLumberyard = UUID.randomUUID();
    UUID vacantLumberyard = UUID.randomUUID();
    JobAssignment staffed = new JobAssignment(UUID.randomUUID(), Occupation.LUMBERJACK, staffedLumberyard, 0);
    JobAssignment vacancy = new JobAssignment(null, Occupation.LUMBERJACK, vacantLumberyard, 0);

    assertNull(LaborPlanner.openProjectProducerPost(
        List.of(new ItemStack(Items.OAK_LOG, 2)), List.of(vacancy), List.of(staffed),
        building -> List.of("LOGS", "PLANKS")));
  }

  @Test
  void anActiveProjectSelectsItsVacantBuilderPostBeforeOtherLaborNeeds() {
    UUID center = UUID.randomUUID();
    JobAssignment farmer = new JobAssignment(null, Occupation.FARMER, UUID.randomUUID(), 0);
    JobAssignment builder = new JobAssignment(null, Occupation.BUILDER, center, 1);

    assertEquals(builder, LaborPlanner.openConstructionPost(
        true, List.of(farmer, builder), center::equals));
    assertNull(LaborPlanner.openConstructionPost(
        false, List.of(farmer, builder), center::equals));
  }

  @Test
  void anUrgentVacancyMayBorrowOneOfSeveralFoodWorkersButNeverTheLast() {
    assertFalse(LaborPlanner.mustKeepForNeed(
        Occupation.FARMER, 2, 3, true, false, Occupation.BUILDER));
    assertFalse(LaborPlanner.mustKeepForNeed(
        Occupation.FARMER, 2, 3, true, false, Occupation.LUMBERJACK));
    assertFalse(LaborPlanner.mustKeepForNeed(
        Occupation.FISHER, 1, 3, true, true, Occupation.QUARTERMASTER));
    assertTrue(LaborPlanner.mustKeepForNeed(
        Occupation.FARMER, 1, 1, true, false, Occupation.BUILDER));
    assertTrue(LaborPlanner.mustKeepForNeed(
        Occupation.FARMER, 1, 1, true, false, Occupation.LUMBERJACK));
  }

  @Test
  void aHungryVillageNeverMovesAnActiveFoodProducerToMaterials() {
    assertTrue(LaborPlanner.mustKeep(Occupation.FARMER, 2, true, false));
    assertTrue(LaborPlanner.mustKeep(Occupation.BAKER, 1, true, false));
    assertTrue(LaborPlanner.mustKeep(Occupation.BUTCHER, 1, true, false));
    assertTrue(LaborPlanner.mustKeep(Occupation.MINER, 1, false, false));
    assertTrue(LaborPlanner.mustKeep(Occupation.QUARTERMASTER, 1, false, false));
    assertTrue(LaborPlanner.mustKeep(Occupation.QUARTERMASTER, 1, false, true));
    assertTrue(LaborPlanner.mustKeep(Occupation.FARMER, 1, true, false));
    assertFalse(LaborPlanner.mustKeep(Occupation.FARMER, 1, false, false));
    assertFalse(LaborPlanner.mustKeep(Occupation.GUARD, 1, true, true));
  }

  @Test
  void aBedlessIdleResidentDoesNotHideAnUrgentVacancy() {
    UUID bedless = UUID.randomUUID();

    assertFalse(LaborPlanner.anyIdleCanTake(List.of(bedless), id -> false));
    assertTrue(LaborPlanner.anyIdleCanTake(List.of(bedless), bedless::equals));
    assertFalse(LaborPlanner.anyIdleCanTake(List.of(), id -> true));
  }
}
