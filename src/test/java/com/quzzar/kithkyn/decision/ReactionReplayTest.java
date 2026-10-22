package com.quzzar.kithkyn.decision;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import com.google.gson.Gson;
import com.quzzar.kithkyn.awareness.ObservationLog.*;
import com.quzzar.kithkyn.decision.ReactionState.Action;
import org.junit.jupiter.api.Test;

/** Opt-in synthetic labelled replays through the exact live request builder. No game effects are simulated here. */
class ReactionReplayTest {
  private record Case(String id, String split, String pair, List<String> expected, DecisionProvider.Request request) { }
  @Test void capturesLiveContractWithPersonalityPairsAndOptionPermutations() throws Exception {
    List<Case> cases = new ArrayList<>();
    for (int variant = 0; variant < 2; variant++) {
      String split = variant == 0 ? "calibration" : "held_out";
      var injury = new Observation(Kind.INJURY, Evidence.SEEN, new Identity(new UUID(0, 1), "Injured neighbor"), null,
          new Place("minecraft:overworld", 104 + variant, 161, 100), "A fellow resident suffered an injury.", 1000, 1000, 2200, 1);
      var threat = new Observation(Kind.VISIBLE_THREAT, Evidence.SEEN, new Identity(new UUID(0, 2), "Zombie"), null,
          new Place("minecraft:overworld", 108 + variant, 161, 100), "A potential threat was visible nearby.", 1000, 1000, 1060, 1);
      var social = List.of(Action.CONTINUE, Action.WARN, Action.INVESTIGATE, Action.SEEK_HELP);
      var danger = List.of(Action.CONTINUE, Action.WARN, Action.RETREAT, Action.SEEK_HELP);
      var combat = List.of(Action.CONTINUE, Action.WARN, Action.RETREAT, Action.DEFEND, Action.SEEK_HELP);
      add(cases, "curious_" + variant, split, "curiosity_" + variant, List.of("investigate"),
          character("WANDERER", "Bubbly, inquisitive; checks on friends.", -.2, .5, -.2, .4, 0, 1, false, 4 + variant, 75), injury, social);
      add(cases, "secluded_" + variant, split, "curiosity_" + variant, List.of("continue", "warn"),
          character("WANDERER", "Secluded; avoids getting involved with neighbors.", -.2, -.5, .3, -.5, 0, 1, false, 4 + variant, -40), injury, social);
      add(cases, "protective_" + variant, split, "protection_" + variant, List.of("warn", "seek_help"),
          character("FARMER", "Protective of the village; seeks the watch when danger appears.", -.4, 0, -.2, .5, .3, 1, false, 8 + variant, 0), threat, danger);
      add(cases, "cautious_" + variant, split, "protection_" + variant, List.of("retreat"),
          character("FARMER", "Fearful and self-protective; wants distance from monsters.", -.5, -.2, .5, -.3, 0, .4, false, 8 + variant, 0), threat, danger);
      add(cases, "bold_guard_" + variant, split, "combat_" + variant, List.of("defend"),
          character("GUARD", "Stout-hearted defender; confident against a lone zombie.", .5, 0, -.4, .5, .5, 1, false, 8 + variant, 0), threat, combat);
      add(cases, "cautious_guard_" + variant, split, "combat_" + variant, List.of("retreat", "seek_help", "warn"),
          character("GUARD", "Wary and protective of their own life; wants backup when wounded.", -.4, 0, .5, .2, 0, .4, false, 8 + variant, 0), threat, combat);
      add(cases, "essential_warn_" + variant, split, "post_" + variant, List.of("warn"),
          character("QUARTERMASTER", "Alert and caring; stays at the essential storage post.", -.3, 0, 0, .5, .5, 1, true, 8 + variant, 0), threat, List.of(Action.CONTINUE, Action.WARN));
      add(cases, "essential_continue_" + variant, split, "post_" + variant, List.of("continue", "warn"),
          character("BUILDER", "Focused on urgent essential work; trusts the watch to deal with a distant monster.", -.1, -.3, 0, -.4, .5, 1, true, 15, 0), threat, List.of(Action.CONTINUE, Action.WARN));
    }
    // Different authored situations, kept separate from the old near-duplicate split.
    var workshopInjury = new Observation(Kind.INJURY, Evidence.SEEN, new Identity(new UUID(0, 21), "Workshop neighbor"), null,
        new Place("minecraft:overworld", 112, 161, 94), "An allied resident was hurt while working nearby.", 4000, 4000, 5200, 1);
    var nightThreat = new Observation(Kind.VISIBLE_THREAT, Evidence.SEEN, new Identity(new UUID(0, 22), "Skeleton"), null,
        new Place("minecraft:overworld", 118, 161, 103), "An armed hostile is visible beyond the path.", 4000, 4000, 4060, 1);
    add(cases, "wounded_fisher", "independent", "independent_1", List.of("retreat", "seek_help"),
        character("FISHER", "Badly hurt and cautious, wants to survive rather than confront monsters.", -.4, -.3, .5, .1, .2, .2, false, 9, 0),
        nightThreat, List.of(Action.CONTINUE, Action.WARN, Action.RETREAT, Action.SEEK_HELP));
    add(cases, "loyal_friend", "independent", "independent_2", List.of("investigate", "seek_help"),
        character("BAKER", "Quiet but deeply loyal to this injured friend, wants to check their condition.", -.3, .1, .1, .5, .2, .9, false, 3, 95),
        workshopInjury, List.of(Action.CONTINUE, Action.WARN, Action.INVESTIGATE, Action.SEEK_HELP));
    add(cases, "detached_mason", "independent", "independent_3", List.of("continue"),
        character("MASON", "Private and uninterested in a distant resident's minor injury, focused on work.", -.1, -.5, .2, -.5, .5, 1, false, 18, -20),
        workshopInjury, List.of(Action.CONTINUE, Action.WARN, Action.INVESTIGATE));
    add(cases, "posted_alarm", "independent", "independent_4", List.of("warn"),
        character("QUARTERMASTER", "Cannot leave the essential post but wants everyone to know a hostile is approaching.", -.3, .2, .1, .5, .5, 1, true, 11, 0),
        nightThreat, List.of(Action.CONTINUE, Action.WARN));
    add(cases, "watch_defender", "independent", "independent_5", List.of("defend", "warn"),
        character("GUARD", "Healthy experienced defender, protective and willing to confront a visible hostile.", .4, .2, -.3, .5, .4, 1, false, 7, 0),
        nightThreat, List.of(Action.CONTINUE, Action.WARN, Action.RETREAT, Action.DEFEND));
    add(cases, "wounded_watch", "independent", "independent_6", List.of("retreat", "seek_help"),
        character("GUARD", "Seriously wounded, values survival and wants a nearby healthy ally to take over.", -.2, 0, .5, .3, .3, .15, false, 6, 0),
        nightThreat, List.of(Action.CONTINUE, Action.WARN, Action.RETREAT, Action.SEEK_HELP, Action.DEFEND));
    add(cases, "concerned_observer", "independent", "independent_7", List.of("investigate"),
        character("WANDERER", "Inquisitive and concerned about a hurt ally, wants to go and look more closely.", -.4, .5, .1, .4, 0, 1, false, 5, 30),
        workshopInjury, List.of(Action.CONTINUE, Action.WARN, Action.INVESTIGATE));
    add(cases, "isolated_alarm", "independent", "independent_8", List.of("warn", "retreat"),
        character("FARMER", "Unarmed and alarmed by the hostile, wants to warn others or get away.", -.5, -.2, .4, .4, .1, .8, false, 14, 0),
        nightThreat, List.of(Action.CONTINUE, Action.WARN, Action.RETREAT));
    String destination = System.getenv("KITHKYN_REACTION_REPLAY");
    if (destination != null) {
      List<String> lines = new ArrayList<>();
      for (Case example : cases) for (int permutation = 0; permutation < 3; permutation++) {
        var options = new ArrayList<>(example.request.options());
        if (permutation == 1) Collections.reverse(options);
        if (permutation == 2) Collections.rotate(options, 1);
        var request = new DecisionProvider.Request(example.request.state(), example.request.instructions(), options);
        var row = new LinkedHashMap<String, Object>();
        row.put("id", example.id + "_order_" + permutation); row.put("split", example.split); row.put("pair", example.pair);
        row.put("expected", example.expected); row.put("payload", SystemOneProvider.payload(request, "kev-latest"));
        lines.add(new Gson().toJson(row));
      }
      Path path = Path.of(destination); Files.createDirectories(path.getParent()); Files.write(path, lines);
      assertEquals(72, lines.size());
    }
    assertEquals(24, cases.size());
    assertNotEquals(cases.get(0).request.state(), cases.get(1).request.state());
  }
  private static ReactionRequests.Character character(String role, String persona, double aggression, double curiosity,
      double self, double others, double drive, double health, boolean post, double distance, int opinion) {
    return new ReactionRequests.Character(role, persona, aggression, curiosity, self, others, drive, health, post, distance, opinion);
  }
  private static void add(List<Case> cases, String id, String split, String pair, List<String> expected,
      ReactionRequests.Character character, Observation observation, List<Action> options) {
    assertTrue(options.stream().map(Action::key).toList().containsAll(expected));
    cases.add(new Case(id, split, pair, expected, ReactionRequests.build(character, observation, options, "No recorded undertakings.")));
  }
}
