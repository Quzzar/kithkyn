package com.quzzar.kithkyn.village;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OccupationTest {

  @Test
  void knownUnimplementedPostsAreExplicit() {
    assertTrue(Occupation.INNKEEPER.lacksImplementedJobBehavior());
    assertTrue(Occupation.BREWER.lacksImplementedJobBehavior());
    assertTrue(Occupation.LIBRARIAN.lacksImplementedJobBehavior());
    assertFalse(Occupation.BAKER.lacksImplementedJobBehavior());
    assertFalse(Occupation.QUARTERMASTER.lacksImplementedJobBehavior());
    assertFalse(Occupation.GUARD.lacksImplementedJobBehavior());
  }
}
