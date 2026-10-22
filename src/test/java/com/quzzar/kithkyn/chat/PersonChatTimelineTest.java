package com.quzzar.kithkyn.chat;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.quzzar.kithkyn.chat.PersonChatContext.Turn;
import com.quzzar.kithkyn.entities.PersonalLogData;

class PersonChatTimelineTest {

  @Test
  void placesAPickupBetweenTheConversationTurnsThatSurroundedIt() {
    List<Turn> turns = List.of(
        new Turn("Could you spare some dirt?", "I can give you twelve.", 100L),
        new Turn("I did not receive it.", "That is strange.", 120L));
    PersonalLogData.Entry pickup = PersonalLogData.pickup(
        "minecraft:dirt", 12, 110L, 110L, Optional.empty());

    String timeline = PersonChatContext.recentTimeline(
        turns, List.of(pickup), "Aaron", ignored -> "someone", 5);

    int offer = timeline.indexOf("Could you spare some dirt?");
    int pickedUp = timeline.indexOf("You picked up 12 dirt from the ground.");
    int missing = timeline.indexOf("I did not receive it.");
    assertTrue(offer >= 0 && offer < pickedUp && pickedUp < missing, timeline);
  }

  @Test
  void nativeGiftOutcomesRemainDistinctFromOffersAndLaterQuestions() {
    var turns = List.of(new Turn("Hello.", "I would like to give you three diamonds.", 100L),
        new Turn("Did I get three?", "I think so.", 120L));
    var outcome = PersonalLogData.memory("I handed 1 diamond to Aaron; two remained with me.", 110L, 110L, Optional.empty());
    String timeline = PersonChatContext.recentTimeline(turns, List.of(outcome), "Aaron", ignored -> "someone", 8);
    int offer = timeline.indexOf("You answered: \"I would like to give you three diamonds.");
    int actual = timeline.indexOf("Game-recorded event: I handed 1 diamond");
    assertTrue(offer >= 0 && actual > offer && actual < timeline.indexOf("Did I get three?"), timeline);
  }
}
