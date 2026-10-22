package com.quzzar.kithkyn.chat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import com.mojang.authlib.GameProfile;
import com.quzzar.kithkyn.Utils;
import com.quzzar.kithkyn.compat.AccessoryCompat;
import com.quzzar.kithkyn.entities.MarriageStatus;
import com.quzzar.kithkyn.entities.PersonalLogData;
import com.quzzar.kithkyn.entities.ClericPotions;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.entities.KithkynAttachments;
import com.quzzar.kithkyn.llm.LlmService.FewShotExample;
import com.quzzar.kithkyn.relationships.RelationshipPair;
import com.quzzar.kithkyn.other.YearManager;
import com.quzzar.kithkyn.persona.PersonaData;
import com.quzzar.kithkyn.village.FarmedStock;
import com.quzzar.kithkyn.village.LocationManager;
import com.quzzar.kithkyn.village.Occupation;
import com.quzzar.kithkyn.village.PersonalChest;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.buildings.Building;
import com.quzzar.kithkyn.village.buildings.UrbanPlanner;
import com.quzzar.kithkyn.village.buildings.VillageContextSnapshot;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Assembles a villager's per-turn briefing per the chat contract (conversation
 * map #24): everything relevant, compressed — persona, village situation, the
 * speaker and what they have thrown, recent pickups, pockets, and this
 * session's history — plus the few-shot example turns that the prototype
 * (#27) showed carry the format at small model sizes.
 *
 * <p>The speaker is whoever is talking to this villager: a player at the chat
 * screen, or a fellow villager (VillagerConversation). The briefing is the
 * same either way; only where the standing feeling is read from differs
 * (OpinionService routes residents to the relationship pair).
 */
public final class PersonChatContext {

  public record AssembledChat(String system, String user, List<FewShotExample> examples, String localJsonSchema) {
  }

  /** One exchange, stamped so physical events can be placed around it coherently. */
  public record Turn(String playerLine, String villagerLine, long dayTime) {
  }

  private static final String RULES_BODY = "Rules: Answer in one or two short sentences, always in character. "
      + "Never use an em dash; use a comma, period, or semicolon instead. "
      + "Never invent events, people, places, or items that are not in your briefing above: no family, "
      + "children, journeys, or plans that are not written there. If you are asked about something your "
      + "briefing does not cover, say you do not know or that there is nothing to tell; never fill the gap "
      + "with a guess. "
      + "Recorded memories and outcomes support claims about what you did; merely seeing an event does not. "
      + "The examples below teach the reply format. Their names, objects and events are not your own memories. "
      + "Small talk is about what IS there: your work, what the village is building and still short of, the time of day, and the people you know. "
      + "You may hand over ONLY a permitted requested item listed for this turn, using \"give\" (the item id) and \"give_count\" (a whole number, default 1). Possessing an item alone does not permit a gift. A request for information about an item is not a request to receive it. Never offer items unprompted or give anything precious. Say you are handing something over only when you include its authorized \"give\" field; otherwise decline the gift. Most replies have no \"give\" at all. "
      + "Before writing your reply, judge only the CURRENT SPEAKER'S words, not your own reply. "
      + "Change your opinion ONLY for how the current speaker actually treats you. Use \"opinion\": "
      + "{\"kind\":\"insult\" or \"threat\" or \"kindness\" or \"apology\", \"quote\":\"<the current speaker's entire line copied exactly>\", \"delta\":<whole number>}. "
      + "Insults and threats use -1 to -10; kindness and apologies use 1 to 10. Most replies OMIT opinion. "
      + "An injury, danger, your own inaction or your inability to remember is not the speaker mistreating you. "
      + "Asking what happened, whether you helped, or what you remember is neutral. A request for an item is neutral. "
      + "Do not mark these questions as insults, threats or kindness. Never invent offending words. "
      + "Add \"fight\": true ONLY when you have decided to come to blows with this person over what they have "
      + "just said or done. A fight is real: you strike them until one of you goes down or they run, with your "
      + "weapon if you carry one and your fists if not, and everyone remembers it. Most replies have no "
      + "\"fight\" at all. "
      + "Add \"done\": true when you have said what you have to say and mean to end the talk: a goodbye, "
      + "a turn back to your work, nothing more to add. Put the farewell itself in \"say\" on that same "
      + "reply. Most replies are not the last one, so most have no \"done\" at all. ";

  private static final String RULES = RULES_BODY
      + "Answer with ONLY a JSON object: {\"say\": \"<reply>\"}. Add optional fields only when they apply. "
      + "For a requested item use \"give\": \"minecraft:<item_id>\" and \"give_count\": <whole number>. "
      + "The item id is a string, never a number. Most replies only contain \"say\".";

  /**
   * Collapses a run of turns where the villager gave the same answer down to
   * the last of them.
   *
   * A small model that has said a line once is fairly likely to say it again;
   * once the transcript shows it saying that line TWICE, it is close to
   * certain. Measured on the live Qwen2.5-3B, with the same sentence in the
   * transcript: once, it never repeated; twice, 2 of 4; three times, 4 of 4.
   * That is the shape Aaron hit - a reasonable first answer, then the same
   * sentence to everything.
   *
   * So the escalation is the actual defect, and it is cheaper to prevent than
   * to sample around: the model never sees itself say the same thing twice,
   * so the attractor cannot build. Dropping those turns loses nothing worth
   * keeping - they are precisely the exchanges where the villager was not
   * really answering.
   */
  private static List<Turn> withoutRepeats(List<Turn> history) {
    List<Turn> kept = new ArrayList<>(history.size());
    for (Turn turn : history) {
      if (!kept.isEmpty() && sameAnswer(kept.get(kept.size() - 1), turn)) {
        kept.remove(kept.size() - 1);
      }
      kept.add(turn);
    }
    return kept;
  }

  /**
   * Delegates to the dispatcher's comparison so the transcript collapses on the
   * same rule the retry guard uses. Whole line OR the same opening sentence:
   * the model locks onto an opening and varies the tail, so four replies that
   * all begin "Just a bit on my mind, that's all." are four distinct lines and
   * a whole-line check leaves every one of them in the transcript to reinforce
   * the next.
   */
  private static boolean sameAnswer(Turn a, Turn b) {
    return a.villagerLine() != null && b.villagerLine() != null
        && PersonChatDispatcher.sameAnswer(a.villagerLine(), b.villagerLine());
  }

  private static final List<FewShotExample> EXAMPLES = List.of(
      new FewShotExample("The speaker says: \"Hello.\"\nYour JSON answer:",
          "{\"say\": \"Hello there.\"}"),
      new FewShotExample("Example briefing: no shared earlier event is recorded. The speaker says: \"Do you remember that thing we did together?\"\nYour JSON answer:",
          "{\"say\": \"I don't recall that.\"}"),
      new FewShotExample("The speaker asks: \"Did you help? What do you remember doing?\"\nYour JSON answer:",
          "{\"say\": \"I don't remember helping.\"}"),
      new FewShotExample("The speaker says: \"You're a worthless idiot.\"\nYour JSON answer:",
          "{\"opinion\": {\"kind\":\"insult\",\"quote\":\"You're a worthless idiot.\",\"delta\":-3}, \"say\": \"There's no call to speak to me like that.\"}"),
      // The one example that ENDS a talk: a goodbye is not enough on its own, the
      // farewell needs "done": true on the same reply or the pair say their
      // goodbyes over and over and never part (Aaron, 2026-09-02). Shown last, so
      // it is the freshest pattern for the turn where the talk has run its course.
      new FewShotExample("The speaker says: \"Anyway, I'll not keep you. Good day to you.\"\nYour JSON answer:",
          "{\"say\": \"And to you. I'll get back to my work.\", \"done\": true}"));

  /**
   * Offered when the player is urging a village-wide direction. The villager may
   * forward it to the brain as a request but never decides it: the request only
   * reaches the planner's option list and states its case (VillageRequests,
   * UrbanPlanner). Gated to direct village proposals so ordinary talk does not
   * invite a planning request.
   */
  private static final String RULES_REQUEST = RULES_BODY
      + "This person is urging a direction for the whole village. You do NOT decide what the village "
      + "does. But if you genuinely agree it should, you may put the idea to the village's judgement: "
      + "\"request\": {\"subject\": \"<what to build, a word or two>\", \"reason\": \"<why, a few words>\"}. "
      + "Add it only when you truly back the idea; if you do not, simply answer in character with no request. "
      + "Answer with ONLY a JSON object: {\"say\": \"<reply>\"}, adding \"give\", \"opinion\", \"done\", or \"request\" "
      + "only when it applies.";

  /** Teaches the request field, and (second) that most urgings are declined, not forwarded. */
  private static final List<FewShotExample> REQUEST_EXAMPLES = List.of(
      new FewShotExample("Steve says: \"With those wolves about, you lot should build a wall.\"\nYour JSON answer:",
          "{\"say\": \"Aye, a wall would let us sleep easier. I'll put it to the village.\", "
          + "\"request\": {\"subject\": \"walls\", \"reason\": \"wolves at the fields after dark\"}}"),
      new FewShotExample("Steve says: \"You should build a great statue of me in the square.\"\nYour JSON answer:",
          "{\"say\": \"Ha. I think the village has better uses for its stone, friend.\"}"));

  /** The base examples plus the request few-shots, for a village-direction turn. */
  private static List<FewShotExample> withRequestExamples() {
    List<FewShotExample> all = new ArrayList<>(EXAMPLES);
    all.addAll(REQUEST_EXAMPLES);
    return all;
  }

  /**
   * Whether the player's line plausibly urges a village-wide building direction,
   * the gate for offering the request tool. Deliberately narrow: it looks for
   * building and priority language, so an ordinary "you should rest" does not
   * open it.
   */
  static boolean proposesVillageChange(String playerLine) {
    return VillageChangeIntent.proposes(playerLine);
  }

  /** A harmlessly broad topic gate for showing planning options without offering an action. */
  static boolean discussesVillagePlanning(String playerLine) {
    return VillageChangeIntent.discusses(playerLine);
  }

  private PersonChatContext() {
  }

  public static AssembledChat assemble(RealPerson person, String playerName, java.util.UUID playerUUID,
      List<Turn> history, String playerLine) {
    StringBuilder system = new StringBuilder();

    String occupation = person.getOccupation() == Occupation.LEADER
        ? com.quzzar.kithkyn.village.VillageRuler.title(person.getGender())
        : Utils.capitalize(person.getOccupation().name().toLowerCase());
    Village village = person.getVillage();
    String villageName = village != null ? village.getName() : "no village";
    system.append("You are ").append(person.getFullName())
        .append(", a villager in a village simulation. You are the ").append(occupation)
        .append(" of ").append(villageName).append(".\n");

    // Gender leads the self-description and is always stated: a villager knows
    // their own, and it is stated as a plain fact about themselves, not a label.
    // The persona blurb (which the persona writer already wrote from this same
    // gender) follows when one has been generated.
    PersonaData persona = person.getData(KithkynAttachments.PERSONA.get());
    system.append("About you: You are ").append(person.getGender().describe());
    if (person.getKind() == com.quzzar.kithkyn.entities.Kind.UNDEAD) {
      system.append(", and ").append(person.getKind().describe());
    }
    system.append('.');
    if (!persona.isEmpty()) {
      system.append(' ').append(persona.blurb());
      if (!persona.quirk().isBlank()) {
        system.append(" Quirk: ").append(persona.quirk());
      }
    }
    system.append('\n');

    if (village != null) {
      system.append(VillageContextSnapshot.capture(village).chatBriefing());
      com.quzzar.kithkyn.village.VillageRuler.incumbent(village).ifPresent(ruler -> {
        if (ruler.getUUID().equals(person.getUUID())) {
          system.append("You are this village's ruling voice. You deliberate on its construction, labor, trade and"
              + " marriage requests through the village's ordinary decisions. In conversation, discuss those"
              + " responsibilities using the actual village facts; requests still need their normal validation.\n");
        } else {
          system.append("The village is ruled by ").append(com.quzzar.kithkyn.village.VillageRuler.title(ruler.getGender()))
              .append(' ').append(ruler.getFullName()).append(".\n");
        }
      });
    }

    // The villager's own work in hand. Their job is a title; this is what a
    // person asked "what are you up to" answers with. Only the builder used to
    // get it, and only for a building; every other occupation had a title and
    // nothing to say about their day, and said something invented instead.
    system.append(activityLine(person)).append('\n');

    // The herder knows its pen kind by kind. A kind it holds none or one of
    // cannot breed, and it cannot make animals itself, so a missing or lone kind
    // is a standing need only a visitor can fill -- exactly the thing a herder
    // asked "how can I help" should raise. Facts only (the counts and the
    // pairing rule); how loudly to ask for a second sheep is the villager's.
    if (person.getOccupation() == Occupation.HERDER) {
      String herd = herdLine(person);
      if (!herd.isEmpty()) {
        system.append(herd).append('\n');
      }
    }

    // Everything on their person is ALWAYS stated, even when empty: an omitted
    // line is an invitation for the model to invent contents (dogfood finding).
    system.append("You are holding: ").append(heldSummary(person)).append(".\n");
    String armor = armorSummary(person);
    if (!armor.isEmpty()) {
      system.append("You are wearing: ").append(armor).append(".\n");
    }
    // Accessory-mod slots, when such a mod is installed (#46).
    List<String> accessories = AccessoryCompat.wornAccessories(person);
    if (!accessories.isEmpty()) {
      system.append("You also have on: ").append(String.join(", ", accessories)).append(".\n");
    }
    String pockets = pocketsSummary(person);
    system.append("Your pockets: ").append(pockets.isEmpty() ? "empty" : pockets).append(".\n");
    String reserve = potionReserveLine(person);
    if (!reserve.isEmpty()) {
      system.append(reserve).append('\n');
    }
    system.append(personalHousingLine(person)).append('\n');
    // Their own chest at home, stated on the same rule as the pockets: what it
    // holds when it is in sight, and that they have none when they have none,
    // so the model neither invents a hoard nor forgets it has one.
    system.append(homeChestLine(person)).append('\n');
    String spouse = spouseLine(person);
    if (!spouse.isEmpty()) {
      system.append(spouse).append('\n');
    }

    system.append("It is now ").append(PersonalLogData.formatDay(person.level().getDayTime())).append(".\n");
    // Villagers reckon the year from the world's age (the Days in Year config),
    // one-based so a fresh world is year 1. The time of day above says where in
    // the day they are; this gives the larger calendar for a chat to lean on.
    int worldYear = (int) YearManager.getYears(person.level()) + 1;
    system.append("The year is ").append(worldYear).append(" MCE.\n");
    system.append(person.awareness().briefing());

    PersonalLogData log = person.getData(KithkynAttachments.PERSONAL_LOG.get());
    String memories = memorySummary(log, playerUUID, 3);
    if (!memories.isEmpty()) {
      system.append("Things you remember happening: ").append(memories).append(".\n");
    }
    Optional<PersonalLogData.ActiveBlocker> blocker = log.standingBlocker(person.level().getGameTime());
    if (blocker.isPresent()) {
      system.append("A problem currently blocking your work: ").append(blocker.get().text()).append(".\n");
    }

    String relations = relationSummary(person, village, 3);
    if (!relations.isEmpty()) {
      system.append("People in your life: ").append(relations).append('\n');
    }

    system.append("You are talking to ").append(playerName);
    int thrownBySpeaker = log.thrownBy(playerUUID).size();
    if (thrownBySpeaker > 0) {
      system.append(", who has thrown you items before");
    }
    system.append(". ").append(opinionLine(person, playerName, playerUUID)).append('\n');

    // What the villager remembers of earlier days' talks with this player: a
    // compact summary a new-day chat picks up from, in place of the full
    // transcript (PersonChatDispatcher.consolidate). Empty until the first
    // consolidation, or when the model produced none.
    String pastTalks = person.getData(KithkynAttachments.CHAT_SUMMARY.get()).with(playerUUID);
    if (!pastTalks.isBlank()) {
      system.append("Conversation quotes from ").append(playerName)
          .append(" in earlier talks. Questions and claims are not proof of events; current facts and recorded outcomes override them:\n")
          .append(pastTalks).append('\n');
    }

    system.append(com.quzzar.kithkyn.entities.UndertakingService.briefing(
        person.getData(KithkynAttachments.UNDERTAKINGS.get()), person.getUUID(), Optional.of(playerUUID), 8));
    boolean discussesPlanning = village != null && discussesVillagePlanning(playerLine);
    boolean proposesChange = village != null && proposesVillageChange(playerLine);
    if (discussesPlanning) {
      // Ground a build the player is urging: hand the villager the menu of what the
      // village can actually raise and what each brings, so "build a lumberjack" has a
      // real building to speak to (it fells logs) rather than being taken for a spare
      // worker (a live-chat finding).
      system.append("Village planning options right now: ")
          .append(UrbanPlanner.buildableCatalogue(village)).append('\n');
    }
    List<String> giftItemIds = carriedItemIds(person).stream().filter(id -> ChatGiftPolicy.requested(id, playerLine)).toList();
    system.append(proposesChange ? RULES_REQUEST : RULES);
    system.append(giftItemIds.isEmpty()
        ? " Current turn: no permitted gift. If asked for an item, decline. Do not say you will hand over or promise items. "
        : " Current turn permitted requested item IDs: " + giftItemIds + ". If you choose to hand one over, include give and give_count; otherwise decline. ");

    StringBuilder user = new StringBuilder();
    List<Turn> transcript = withoutRepeats(history);
    String timeline = recentTimeline(transcript, log.pickupsNewestFirst(), playerName,
        uuid -> resolveName(person, uuid), 5);
    if (!timeline.isEmpty()) {
      user.append("Recent event timeline, oldest to newest. Earlier dialogue is not authoritative world fact; "
          + "the current briefing above overrides it:\n").append(timeline);
    }
    user.append(playerName).append(" says: \"").append(playerLine).append("\"\nYour JSON answer:");

    return new AssembledChat(system.toString(), user.toString(),
        proposesChange ? withRequestExamples() : EXAMPLES, PersonChatReplySchema.build(giftItemIds, proposesChange, playerLine));
  }

  private static List<String> carriedItemIds(RealPerson person) {
    var ids = new java.util.TreeSet<String>();
    for (ItemStack stack : person.getAllSlots()) if (ChatGiftPolicy.giveable(person, stack) > 0)
      ids.add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    for (int i = 0; i < person.personMainInv.getContainerSize(); i++) {
      ItemStack stack = person.personMainInv.getItem(i);
      if (ChatGiftPolicy.giveable(person, stack) > 0)
        ids.add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }
    return List.copyOf(ids);
  }

  /** What's in their hands — held items live outside the pockets inventory. */
  private static String heldSummary(RealPerson person) {
    ItemStack main = person.getMainHandItem();
    ItemStack off = person.getOffhandItem();
    if (main.isEmpty() && off.isEmpty()) {
      return "nothing";
    }
    StringBuilder held = new StringBuilder();
    if (!main.isEmpty()) {
      held.append("a ").append(itemName(main));
    }
    if (!off.isEmpty()) {
      if (held.length() > 0) {
        held.append(" and ");
      }
      held.append("a ").append(itemName(off));
    }
    return held.toString();
  }

  /**
   * The villager's standing feeling about the speaker — always stated so the
   * opinion tool compounds across conversations (#44). Read through
   * {@link com.quzzar.kithkyn.relationships.OpinionService}, which knows
   * where a feeling lives: the relationship pair for a fellow resident, the
   * villager's own social attachment for a player or stranger.
   */
  private static String opinionLine(RealPerson person, String playerName, java.util.UUID playerUUID) {
    int opinion = com.quzzar.kithkyn.relationships.OpinionService.opinionOf(person, playerUUID);
    String feeling;
    if (opinion >= 60) {
      feeling = "You consider " + playerName + " a dear friend";
    } else if (opinion >= 25) {
      feeling = "You rather like " + playerName;
    } else if (opinion >= 10) {
      feeling = "You are warming to " + playerName;
    } else if (opinion > -10) {
      feeling = "You have no strong feelings about " + playerName + " yet";
    } else if (opinion > -25) {
      feeling = playerName + " has annoyed you before";
    } else if (opinion > -60) {
      feeling = "You dislike " + playerName;
    } else {
      feeling = "You despise " + playerName;
    }
    return feeling + ".";
  }

  /** Worn armor, e.g. "an iron helmet and iron boots". */
  private static String armorSummary(RealPerson person) {
    List<String> parts = new ArrayList<>();
    for (ItemStack stack : person.getArmorSlots()) {
      if (!stack.isEmpty()) {
        parts.add(itemName(stack));
      }
    }
    return String.join(" and ", parts);
  }

  /**
   * The FULL pocket contents, aggregated by item — they know their own bags.
   * With one omission: a cleric is told the bottles they can spare of each
   * brew, never the seed ({@link ClericPotions#sparePackCount}). A model that
   * gives only what the briefing says it has then never offers the last bottle,
   * which is the cleaner half of the rule; the take in
   * {@code PersonChatDispatcher.takeFromSlots} is the backstop.
   */
  private static String pocketsSummary(RealPerson person) {
    Map<String, Integer> counts = new LinkedHashMap<>();
    boolean cleric = ClericPotions.isCleric(person);
    for (int i = 0; i < person.personMainInv.getContainerSize(); i++) {
      ItemStack stack = person.personMainInv.getItem(i);
      if (stack.isEmpty()) {
        continue;
      }
      if (cleric && ClericPotions.isStock(stack)) {
        // Counted once per brew, as the spare, however many slots it fills.
        counts.putIfAbsent(itemName(stack), ClericPotions.sparePackCount(person, stack));
        continue;
      }
      counts.merge(itemName(stack), stack.getCount(), Integer::sum);
    }
    counts.values().removeIf(count -> count <= 0);
    List<String> parts = new ArrayList<>();
    counts.forEach((name, count) -> parts.add(count + " " + name));
    return String.join(", ", parts);
  }

  /**
   * A cleric's seeds, said plainly: one bottle of each brew is kept back as the
   * recipe they brew more from at their station, and is not for giving. This
   * is the context behind the spare counts in the pockets line, and it tells
   * the cleric what they can make (Aaron, 2026-09-12). Empty for anyone else.
   */
  private static String potionReserveLine(RealPerson person) {
    if (!ClericPotions.isCleric(person)) {
      return "";
    }
    List<String> brews = new ArrayList<>();
    for (ClericPotions.Stock stock : ClericPotions.stock(person)) {
      brews.add(itemName(stock.sample()));
    }
    if (brews.isEmpty()) {
      return "You carry no potions at all, so there is nothing you can brew more of until someone hands you a bottle.";
    }
    return "In reserve, not counted above and not for giving: one " + String.join(", one ", brews)
        + ". Each is the recipe you brew three more of at your station; give the last one away and that brew is lost to you."
        + " You drink ordinary potions yourself when one would help you, and throw splash and lingering potions:"
        + " helpful ones at friends when no enemy stands in the splash, harmful ones at enemies when no friend does.";
  }

  /**
   * The villager's own chest at home (PersonalChest): who shares it and what it
   * holds. Contents are read only when the chest's chunk is resident; a
   * villager far from home cannot recall, rather than paging the chunk in.
   */
  private static String homeChestLine(RealPerson person) {
    BlockPos chest = PersonalChest.of(person);
    if (chest == null) {
      return "You have no chest of your own; whatever you set down goes to the village stores.";
    }
    StringBuilder line = new StringBuilder("You have a chest of your own at home");
    List<String> housemates = PersonalChest.housemateNames(person);
    if (!housemates.isEmpty()) {
      line.append(", shared with ").append(String.join(" and ", housemates));
    }
    Container container = PersonalChest.container(person, chest);
    if (container == null) {
      return line.append("; you cannot recall exactly what is in it right now.").toString();
    }
    String holds = PersonalChest.summarize(container);
    return line.append(holds.isEmpty() ? "; it is empty." : "; it holds " + holds + ", at home, not on you.")
        .toString();
  }

  /** Where this resident actually sleeps, including dependent and live-in housing. */
  private static String personalHousingLine(RealPerson person) {
    Village village = person.getVillage();
    if (village == null) {
      return "You have no village home or bed.";
    }
    Building dependentHome = village.dependentHome(person);
    if (dependentHome != null && dependentHome.getInfo() != null) {
      return "You live in your parents' " + dependentHome.getInfo().displayLabel()
          + " as a dependent and do not claim a separate bed yet.";
    }
    var bed = village.getBedAssignment(person.getUUID());
    if (bed == null) {
      return "You have no bed and stay awake by the village fire at night.";
    }
    Building home = village.getBuilding(bed.getBuildingUUID());
    String label = home != null && home.getInfo() != null ? home.getInfo().displayLabel() : "village home";
    var job = village.getJobAssignment(person.getUUID());
    if (job != null && village.sleepsInReservedBedAt(person.getUUID(), job.getBuildingUUID())) {
      return "You sleep in the live-in bed at your " + label
          + "; that bed is reserved for whoever holds your current post.";
    }
    return "Your assigned bed is in a " + label + ".";
  }

  /**
   * Whether this villager is married and to whom (docs/marriage.md). Who is
   * married to whom is the pair edge's to hold, so the spouse is read from the
   * married relationship rather than a second field; an empty line for a single
   * villager, and a named spouse only when they are a fellow resident in sight.
   */
  private static String spouseLine(RealPerson person) {
    if (person.getMarriageStatus() != MarriageStatus.MARRIED) {
      return "";
    }
    Village village = person.getVillage();
    if (village == null || !(person.level() instanceof ServerLevel level)) {
      return "You are married.";
    }
    for (RelationshipPair pair : village.relationshipsOf(person.getUUID())) {
      if (!pair.married()) {
        continue;
      }
      UUID spouseId = pair.other(person.getUUID());
      RealPerson spouse = village.getPerson(level, spouseId);
      if (spouse != null) {
        return "You are married to " + spouse.getFullName() + ".";
      }
    }
    return "You are married.";
  }

  /** What a villager holds, by its plain name: "stone axe" for "minecraft:stone_axe". */
  private static String itemName(ItemStack stack) {
    return stack.getItem().toString().replace("minecraft:", "").replace('_', ' ');
  }

  /**
   * The villager's own work in hand, always stated. Sleep first, since a
   * sleeper's last activity is yesterday's; then what the work loops noted
   * within the last few minutes ("just before" is the honest tense, because
   * opening a chat pauses the loop); then the truth about having nothing on.
   */
  private static String activityLine(RealPerson person) {
    if (person.isSleeping()) {
      return "Right now you are asleep in your bed.";
    }
    Optional<String> activity = person.recentActivity();
    if (activity.isPresent()) {
      return "Just before this conversation you were " + activity.get() + ".";
    }
    if (person.getOccupation().isIdle()) {
      return "You have no job yet and no work in hand; you pass the day about the camp.";
    }
    return "You have no work in hand at the moment.";
  }

  /**
   * The herder's herd, kind by kind: what stands in the pen now, and which kinds
   * fall short of a breeding pair. Breeding a kind takes two grown of it
   * ({@link FarmedStock#BREED_MIN}), so a kind the pen has none or one of cannot
   * grow on its own; the herder cannot make animals, so those kinds are the one
   * thing only the world outside the pen can fix, and its standing need. Given
   * as plain counts, so the villager speaks to the real herd rather than an
   * invented one; whether to press a visitor for a second sheep is its own call.
   */
  private static String herdLine(RealPerson person) {
    BlockPos pasture = LocationManager.getJobLocation(person);
    if (pasture == BlockPos.ZERO) {
      return "";
    }
    List<String> have = new ArrayList<>();
    List<String> need = new ArrayList<>();
    for (FarmedStock.HerdCount count : FarmedStock.census(person.level(), pasture)) {
      have.add(count.kind().count(count.total()));
      int shortfall = count.shortOfPair();
      if (shortfall > 0) {
        need.add(count.kind().plural() + " (" + count.grown() + " grown, need "
            + shortfall + " more)");
      }
    }
    StringBuilder line = new StringBuilder();
    line.append("Your pasture holds ").append(String.join(", ", have)).append(". ");
    if (need.isEmpty()) {
      line.append("You have a breeding pair of every kind you keep.");
    } else {
      line.append("Breeding a kind takes two grown animals of it, so you cannot breed: ")
          .append(String.join(", ", need))
          .append(". You cannot make animals yourself; only more brought into the pen"
              + " will let those kinds breed.");
    }
    return line.toString();
  }

  private record TimelineLine(long dayTime, int kindOrder, int sequence, String text) {
  }

  /**
   * Interleaves observed pickups with the persisted conversation instead of
   * presenting them as a detached fact in the system briefing. A villager can
   * now connect "I handed that over" with "I immediately picked it up" in the
   * order those events happened.
   */
  static String recentTimeline(List<Turn> history, List<PersonalLogData.Entry> pickups,
      String speakerName, Function<UUID, String> resolveName, int pickupLimit) {
    List<TimelineLine> lines = new ArrayList<>();
    int sequence = 0;
    for (Turn turn : history) {
      StringBuilder exchange = new StringBuilder();
      if (turn.playerLine() != null && !turn.playerLine().isBlank()) {
        exchange.append(speakerName).append(" said: \"").append(turn.playerLine()).append("\"\n");
      }
      String groundedAnswer = ConversationMemoryPrompt.withoutVolatileWorldState(turn.villagerLine());
      if (!groundedAnswer.isBlank()) {
        exchange.append("You answered: \"").append(groundedAnswer).append("\"");
      } else if (turn.villagerLine() != null && !turn.villagerLine().isBlank()) {
        exchange.append("Your earlier answer discussed mutable village state; disregard it and use the current "
            + "briefing.");
      }
      if (!exchange.isEmpty()) {
        lines.add(new TimelineLine(turn.dayTime(), 0, sequence++, exchange.toString()));
      }
    }

    int kept = 0;
    for (PersonalLogData.Entry pickup : pickups) {
      if (!PersonalLogData.KIND_PICKUP.equals(pickup.kind()) || kept++ >= pickupLimit) {
        continue;
      }
      String item = pickup.item().replace("minecraft:", "").replace('_', ' ');
      String source = pickup.who()
          .map(uuid -> " that " + resolveName.apply(uuid) + " threw to you")
          .orElse(" from the ground");
      lines.add(new TimelineLine(pickup.dayTime(), 1, sequence++,
          "You picked up " + pickup.count() + " " + item + source + "."));
    }

    lines.sort(java.util.Comparator.comparingLong(TimelineLine::dayTime)
        .thenComparingInt(TimelineLine::kindOrder)
        .thenComparingInt(TimelineLine::sequence));
    StringBuilder timeline = new StringBuilder();
    for (TimelineLine line : lines) {
      timeline.append('[').append(PersonalLogData.formatDay(line.dayTime())).append("] ")
          .append(line.text()).append('\n');
    }
    return timeline.toString();
  }

  /** The person's strongest relationships, with names, feeling, and flavor. */
  private static String relationSummary(RealPerson person, Village village, int limit) {
    if (village == null || !(person.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
      return "";
    }
    java.util.UUID self = person.getUUID();
    List<String> parts = new ArrayList<>();
    village.relationshipsOf(self).stream()
        .sorted((a, b) -> Integer.compare(Math.abs(b.opinionOf(self)), Math.abs(a.opinionOf(self))))
        .limit(limit)
        .forEach(pair -> {
          if (serverLevel.getEntity(pair.other(self)) instanceof RealPerson other) {
            int opinion = pair.opinionOf(self);
            String feeling = opinion > 50 ? "you are close to them"
                : opinion > 15 ? "you are fond of them"
                : opinion >= -15 ? "you know them"
                : opinion >= -50 ? "you dislike them"
                : "you despise them";
            String flavor = pair.flavor().isBlank() ? "" : " " + pair.flavor();
            parts.add(other.getFullName() + " (" + feeling + "." + flavor + ")");
          }
        });
    return String.join(" ", parts);
  }

  private static String memorySummary(PersonalLogData log, UUID counterpart, int limit) {
    List<String> parts = new ArrayList<>();
    for (PersonalLogData.Entry entry : log.memoriesFor(counterpart)) {
      if (parts.size() >= limit) {
        break;
      }
      parts.add(entry.text() + " (" + PersonalLogData.formatDay(entry.dayTime()) + ")");
    }
    return String.join("; ", parts);
  }

  /** A name for a UUID: a loaded villager's full name, a player's profile name, or "someone". */
  private static String resolveName(RealPerson person, java.util.UUID uuid) {
    if (person.level() instanceof net.minecraft.server.level.ServerLevel serverLevel
        && serverLevel.getEntity(uuid) instanceof RealPerson other) {
      return other.getFullName();
    }
    return person.getServer() != null
        ? person.getServer().getProfileCache().get(uuid).map(GameProfile::getName).orElse("someone")
        : "someone";
  }

}
