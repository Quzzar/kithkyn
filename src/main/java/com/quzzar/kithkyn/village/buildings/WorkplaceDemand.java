package com.quzzar.kithkyn.village.buildings;

import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;

import com.quzzar.kithkyn.village.Occupation;

/** Shared vacancy facts for ordinary planning and destructive redevelopment. */
public final class WorkplaceDemand {

  private WorkplaceDemand() {
  }

  /** A post of this occupation already stands empty and needs no duplicate workplace. */
  public static boolean alreadyVacant(Occupation occupation, Collection<Occupation> vacancies) {
    return vacancies.contains(occupation);
  }

  /**
   * Whether a producer project would only add more vacant posts for capabilities
   * already present. Homes, storage-only buildings, and a workshop that unlocks
   * anything new are outside this rule.
   */
  public static boolean duplicatesVacantProduction(Collection<Occupation> jobs,
      Collection<String> grants, Set<Occupation> vacancies, Predicate<String> alreadyProvided) {
    // Shared containers provide useful capacity whether or not the attached
    // quartermaster post is filled. A storage shortage must therefore remain
    // able to surface another storehouse or storehouse upgrade.
    if (grants.contains("STORAGE")) {
      return false;
    }
    if (jobs.isEmpty() || !vacancies.containsAll(jobs)) {
      return false;
    }
    return grants.stream().allMatch(alreadyProvided);
  }
}
