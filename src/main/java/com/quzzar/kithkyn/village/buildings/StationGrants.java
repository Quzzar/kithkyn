package com.quzzar.kithkyn.village.buildings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.quzzar.kithkyn.village.Occupation;

/**
 * The capability a work station stands for, so a definition cannot add the
 * post without telling the planner what the post gives (Aaron, 2026-09-12: a
 * building with a cleric station obviously grants HEALING; a building with a
 * blacksmith and a cleric grants both).
 *
 * <p>Only the trades with one unmistakable grant are listed. A guard post is
 * not: the watchtower's guards grant PROTECTION while the center's captain and
 * a castle's sentries do not, and that is a planning decision the definitions
 * make on purpose. The builder and the leader grant nothing.
 */
public final class StationGrants {

  private static final Map<Occupation, String> CANONICAL = Map.ofEntries(
      Map.entry(Occupation.CLERIC, "HEALING"),
      Map.entry(Occupation.BLACKSMITH, "REPAIR"),
      Map.entry(Occupation.FARMER, "GRAIN"),
      Map.entry(Occupation.BAKER, "BREAD"),
      Map.entry(Occupation.BUTCHER, "MEAT"),
      Map.entry(Occupation.HERDER, "MEAT"),
      Map.entry(Occupation.HUNTER, "MEAT"),
      Map.entry(Occupation.FISHER, "MEAT"),
      Map.entry(Occupation.LUMBERJACK, "LOGS"),
      Map.entry(Occupation.MASON, "CUT_STONE"),
      Map.entry(Occupation.MERCHANT, "TRADE"),
      Map.entry(Occupation.MINER, "ORES"),
      Map.entry(Occupation.QUARTERMASTER, "STORAGE"),
      Map.entry(Occupation.INNKEEPER, "WANDERERS"));

  private StationGrants() {
  }

  /** The grant a station of this trade must come with, or null for a trade that grants nothing by itself. */
  public static String canonical(Occupation occupation) {
    return CANONICAL.get(occupation);
  }

  /**
   * Every station whose canonical grant the definition lacks, as
   * "OCCUPATION needs GRANT" lines. Conditional grants count: an inn whose
   * WANDERERS waits on ale has still declared it.
   */
  public static List<String> missing(BuildingInfo info) {
    List<String> problems = new ArrayList<>();
    for (Occupation occupation : info.getWorkLocations().values().stream().distinct().toList()) {
      String grant = canonical(occupation);
      if (grant == null) {
        continue;
      }
      boolean declared = info.getGrants().contains(grant)
          || info.getConditionalGrants().stream().anyMatch(conditional -> conditional.capability().equals(grant));
      if (!declared) {
        problems.add(occupation + " station needs the " + grant + " grant");
      }
    }
    return problems;
  }

}
