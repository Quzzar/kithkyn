package com.quzzar.kithkyn.relationships;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RelationshipDriftTest {

  @Test
  void forgivenessStepsTowardIndifferenceForTheLiving() {
    assertEquals(-9, RelationshipDrift.forgiven(-10, 0));
    assertEquals(9, RelationshipDrift.forgiven(10, 0));
    assertEquals(0, RelationshipDrift.forgiven(0, 0));
  }

  @Test
  void forgivenessStepsTowardTheUndeadBaselineFromEitherSide() {
    assertEquals(-41, RelationshipDrift.forgiven(-42, -40));
    assertEquals(-39, RelationshipDrift.forgiven(-38, -40));
    assertEquals(-40, RelationshipDrift.forgiven(-40, -40));
  }

  @Test
  void forgivenessNeverOvershootsTheBaseline() {
    assertEquals(-40, RelationshipDrift.forgiven(-40, -40));
    assertEquals(0, RelationshipDrift.forgiven(0, 0));
    assertEquals(-40, RelationshipDrift.forgiven(-41, -40));
    assertEquals(-40, RelationshipDrift.forgiven(-39, -40));
  }
}
