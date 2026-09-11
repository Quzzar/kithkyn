package com.quzzar.kithkyn.entities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.llm.LlmSelection;
import com.quzzar.kithkyn.llm.LlmService;
import com.quzzar.kithkyn.village.PersonalChest;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.buildings.BuildingInfo;
import com.quzzar.kithkyn.village.buildings.Buildings;
import com.quzzar.kithkyn.village.buildings.ConstructionQuote;
import com.quzzar.kithkyn.village.buildings.Materials;
import com.quzzar.kithkyn.village.buildings.StructureInProgress;
import com.quzzar.kithkyn.village.buildings.VillageGoal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Container;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.TieredItem;

/**
 * Puts one bedtime question to a villager who has a chest of their own, in
 * two halves: of what they are carrying home tonight, what would they rather
 * keep than hand back to the village stores, and of what the chest already
 * holds, what would they rather take out and carry? The rules lay out the
 * facts, what is in the pack and what the chest holds, and one option per
 * kind of item on either side; the model picks any number of them, or none,
 * in character, over the multi-pick sibling of decide() (docs/llm-brain.md).
 *
 * <p>The chest is for keepsakes, and the briefing says so: the village runs on
 * what its workers bring in, so the question is what is truly personal, not
 * what to store. Two things learned live (2026-09-02): a chest introduced as
 * "shared with X and Y" read as shared storage, and a whole flock's wool went
 * home night after night as "personal supplies"; and a 3B model asked what to
 * keep "for yourself" heard an invitation. The job's kit is never on the list
 * at all, since the miner once kept the bucket and the torches the restock had
 * just handed over, and the shaft flooded while they sat in a barrel at home.
 *
 * <p>Taking out (2026-09-11) is the other direction of the same visit: a
 * keepsake to use or give, or supplies the village has since run short of.
 * What is taken out rides in the pack and meets the next bedtime like
 * anything else carried, so it returns to the stores then unless it is kept
 * again. The chest is read only when its chunk is resident; out of sight it
 * offers nothing to take out, the same "cannot recall" the briefing states.
 *
 * <p>Silence keeps nothing and takes nothing. The rules' own choice is what
 * always happened, the whole pack back to the stores and the chest left as it
 * is, so a mute or absent model costs the village no goods; only an explicit
 * pick holds something back or lifts something out. Both happen by hand at
 * the chest ({@link com.quzzar.kithkyn.entities.ai.goals.StashAtHomeGoal}),
 * so nothing teleports. The answer lands through {@link RealPerson#settleStash},
 * which stows the rest of the pack: the pack is held whole while the question
 * is out, or the answer would arrive to empty pockets.
 */
public final class StashOffer {

  /** One option of the question: a kind of item, held back from the stores or taken out of the chest. */
  public record Pick(Item item, int count, boolean fromChest) {
    /** The option as the model reads it, naming the cost of holding back and the act of taking out. */
    public String option() {
      return fromChest
          ? "Take the " + count + " " + plain(item) + " out of your chest to carry"
          : "Hold back the " + count + " " + plain(item) + " from the village stores";
    }
  }

  private StashOffer() {
  }

  /**
   * Asks, and settles the villager's stash on the main thread when the answer
   * lands. A villager with nothing to keep or take out, or a village with no
   * brain ready, is settled at once with nothing kept and nothing taken.
   */
  public static void offer(RealPerson person, BlockPos chest) {
    Map<Item, Integer> carried = counts(person.personMainInv);
    Container container = PersonalChest.container(person, chest);
    Map<Item, Integer> stored = container == null ? Map.of() : counts(container);
    List<Pick> picks = picks(carried, stored);
    LlmService llm = LlmService.get();
    MinecraftServer server = person.getServer();
    if (picks.isEmpty() || !llm.isReady() || server == null) {
      person.settleStash(Set.of(), Set.of());
      return;
    }
    List<String> options = new ArrayList<>();
    for (Pick pick : picks) {
      options.add(pick.option());
    }
    String purpose = person.getFullName() + "'s chest at home";
    llm.choose(purpose, situation(person, container, carried, stored), options).whenComplete((selection, error) -> {
      if (error != null) {
        Kithkyn.LOGGER.error("'{}' could not weigh what to keep at home", person.getFullName(), error);
      }
      Optional<LlmSelection> settled = selection == null ? Optional.empty() : selection;
      server.execute(() -> finish(person, picks, settled));
    });
  }

  /**
   * The question's options in order: each kind carried that is not the job's
   * kit, then each kind the chest holds. The chest side has no kit filter:
   * whatever is in there is the villager's own.
   */
  public static List<Pick> picks(Map<Item, Integer> carried, Map<Item, Integer> stored) {
    List<Pick> picks = new ArrayList<>();
    carried.forEach((item, count) -> {
      if (!isKit(item)) {
        picks.add(new Pick(item, count, false));
      }
    });
    stored.forEach((item, count) -> picks.add(new Pick(item, count, true)));
    return picks;
  }

  /**
   * The job's kit: any tool, and what the bedtime restock hands out (torches,
   * the bucket, the sponge). It was the village's when the day began and is
   * not the villager's to keep, so it is never offered.
   */
  private static boolean isKit(Item item) {
    return item instanceof TieredItem || item instanceof BucketItem || item instanceof ShearsItem
        || item instanceof FishingRodItem || item instanceof ProjectileWeaponItem
        || item == Items.TORCH || item == Items.SPONGE;
  }

  /** The facts the model decides on, kept to a few lines so a small model reads all of them. */
  private static String situation(RealPerson person, @Nullable Container container,
      Map<Item, Integer> carried, Map<Item, Integer> stored) {
    List<String> pack = new ArrayList<>();
    carried.forEach((item, count) -> pack.add(count + " " + plain(item)));
    StringBuilder situation = new StringBuilder(CraftOffer.identityLead(person))
        .append("You are turning in for the night carrying ")
        .append(pack.isEmpty() ? "nothing" : String.join(", ", pack))
        .append(". Your home has a small chest for keepsakes");
    if (container == null) {
      situation.append("; you cannot recall exactly what is in it. ");
    } else {
      String holds = PersonalChest.summarize(container);
      situation.append(holds.isEmpty() ? "; it is empty. " : "; it holds " + holds + ". ");
    }
    situation.append("It is not a store. The village lives on what its workers bring in each day: the")
        .append(" stores are where everyone draws wood, food, wool and tools, and anything held back at")
        .append(" home is lost to the village's work. ").append(villageNeed(person.getVillage()))
        .append(" Most nights a worker keeps nothing. Hold something back only")
        .append(" if it is truly personal, a gift, a memento or a bite of something you fancy, never")
        .append(" supplies or your day's produce.");
    if (!stored.isEmpty()) {
      situation.append(" You may also take something out of the chest to carry: it rides in your pack")
          .append(" from tomorrow and goes to the stores at your next bedtime unless you keep it again,")
          .append(" so take out only what you mean to use or give, or what the village is short of.");
    }
    return situation.append(" Give your reason in a few words.").toString();
  }

  /**
   * What the village is saving for or building and what it is still short of,
   * as the chat briefing states it (PersonChatContext), so the villager weighs
   * the wool in their pack against the beds it is waiting to become. Stated
   * even when there is nothing, since an unmentioned need is a gap the model
   * fills. The first night on the keepsakes wording (2026-09-02), four of nine
   * still held back white wool as a "keepsake" while the village was short of
   * it; the cost was in the pack and never in the question.
   */
  private static String villageNeed(@Nullable Village village) {
    if (village == null) {
      return "";
    }
    Map<Item, Integer> stock = village.stockTally();
    List<String> needs = new ArrayList<>();
    StructureInProgress project = village.getCurrentProject();
    if (project != null && project.isGathering()) {
      BuildingInfo built = project.getBuilding().getInfo();
      needs.add(need("building", built,
          ConstructionQuote.captureProject(village, project, stock).describeMissing()));
    }
    String goal = VillageGoal.current(village);
    BuildingInfo wanted = goal == null ? null : Buildings.getByName(goal);
    if (wanted != null) {
      needs.add(need("saving to build", wanted,
          ConstructionQuote.captureGoal(village, wanted, stock).describeMissing()));
    }
    if (needs.isEmpty()) {
      return "The village is not short of anything for a build tonight.";
    }
    return String.join(" ", needs);
  }

  private static String need(String doing, BuildingInfo building, String shortfall) {
    String label = building.displayLabel();
    return "The village is " + doing + " a " + label
        + (shortfall.isEmpty() ? " and has everything it needs for it." : " and is still short " + shortfall + ".");
  }

  private static void finish(RealPerson person, List<Pick> picks, Optional<LlmSelection> selection) {
    if (!person.isAlive()) {
      return;
    }
    Set<Item> keep = new LinkedHashSet<>();
    Set<Item> takeOut = new LinkedHashSet<>();
    if (selection.isEmpty()) {
      Kithkyn.LOGGER.debug("'{}' had no answer on their chest at home, so the pack goes to the stores and the chest stays as it is",
          person.getFullName());
    } else {
      for (int index : selection.get().choiceIndexes()) {
        Pick pick = picks.get(index);
        (pick.fromChest() ? takeOut : keep).add(pick.item());
      }
      if (keep.isEmpty() && takeOut.isEmpty()) {
        Kithkyn.LOGGER.info("'{}' keeps nothing back tonight and leaves the chest as it is: {}", person.getFullName(),
            selection.get().reason());
      }
      if (!keep.isEmpty()) {
        Kithkyn.LOGGER.info("'{}' keeps the {} for their chest at home: {}", person.getFullName(),
            names(keep), selection.get().reason());
      }
      if (!takeOut.isEmpty()) {
        Kithkyn.LOGGER.info("'{}' takes the {} out of their chest at home: {}", person.getFullName(),
            names(takeOut), selection.get().reason());
      }
    }
    person.settleStash(keep, takeOut);
  }

  /** Each kind of item in a container with how many, in slot order. */
  public static Map<Item, Integer> counts(Container container) {
    Map<Item, Integer> counts = new LinkedHashMap<>();
    for (int slot = 0; slot < container.getContainerSize(); slot++) {
      ItemStack stack = container.getItem(slot);
      if (!stack.isEmpty()) {
        counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
      }
    }
    return counts;
  }

  /** An item as the model reads it: "wheat", "iron pickaxe". */
  public static String plain(@Nullable Item item) {
    return item == null ? "nothing" : item.getDescription().getString().toLowerCase(Locale.ROOT);
  }

  /** Several kinds as a log reads them: "wheat and apple and bread". */
  public static String names(Collection<Item> items) {
    List<String> out = new ArrayList<>();
    for (Item item : items) {
      out.add(plain(item));
    }
    return out.isEmpty() ? "nothing" : String.join(" and ", out);
  }
}
