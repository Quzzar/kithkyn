package com.quzzar.kithkyn.entities.ai.goals.work;

import java.util.List;

import javax.annotation.Nullable;

import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.village.Village;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;

/**
 * Idle hands at the fire: an idle camper takes raw food the village has in
 * store, cooks whatever a campfire can cook, and puts the cooked food back
 * (docs/population-and-labor.md, docs/worker-loops.md).
 *
 * The campfire-model twin of the farmer's idle composter chain
 * ({@link CompostStep}), and a CONVERT step deliberately scoped to idle
 * residents as an early-camp bridge: a young camp has no butchery, so the raw
 * meat a hunter brings home would sit uncooked. Once the village builds a
 * butchery its BUTCHER out-produces the fireside, and this quietly matters less
 * without any explicit hand-off - the butcher simply drains the stores first.
 *
 * <b>Physical all the way through.</b> The raw food is fetched from a real
 * chest into the cook's pack, genuinely roasts on the town's lit campfire, and
 * the cooked food is carried back to a chest by hand (docs/worker-loops.md,
 * "Nothing teleports"). The roasting itself is {@link CampfireRoast}, shared
 * with the roaming wanderer's own camp; this step owns only the chest trips
 * either side of it. The step alternates its target the way {@link GatherStep}
 * does: a chest while the pack is short or holds finished goods, the station
 * while there is a batch in hand.
 */
public final class CookStep implements BlockWorkStep {
  /** Raw items carried per fetch trip: a campfire's four slots' worth. */
  private static final int RAWS_PER_TRIP = 4;

  private final CampfireRoast roast = new CampfireRoast();
  @Nullable
  private CampfireAccess.Target fireTarget;
  @Nullable
  private BlockPos chestTarget;
  @Nullable
  private BlockPos chestApproach;

  @Override
  @Nullable
  public BlockPos select(RealPerson person) {
    Village village = person.getVillage();
    if (village == null) {
      return null;
    }
    if (this.roast.tending() && this.fireTarget != null) return this.fireTarget.fire();
    // A fire route is worth searching only when there is actual raw food to cook.
    boolean rawCarried = this.roast.rawInPack(person) != null;
    BlockPos source = rawCarried ? null : chestWithRaw(person, village);
    if (rawCarried || source != null) {
      this.fireTarget = CampfireAccess.select(person, village, true, Double.MAX_VALUE);
      if (this.fireTarget != null) return rawCarried ? this.fireTarget.fire() : selectChest(person, source);
    }
    // Finished meals can still be put away when every fire is out or full.
    Item cooked = this.roast.cookedInPack(person);
    if (cooked != null) {
      return selectChest(person, PackLogistics.chestWithRoomFor(person, village, new ItemStack(cooked)));
    }
    return null;
  }

  @Override
  public boolean act(RealPerson person, BlockPos target) {
    Village village = person.getVillage();
    if (village == null) {
      return false;
    }
    Container chest = PackLogistics.containerAt(person, target);
    if (chest != null) {
      return visitChest(person, chest);
    }
    CampfireBlockEntity campfire = CampfireRoast.litFireAt(person.level(), target);
    if (campfire == null) {
      // The fire went out or was taken. The raw mid-roast goes back into the
      // pack so nothing is lost, then go and select afresh.
      this.roast.abandon(person, target);
      return false;
    }
    if (!this.roast.tending()) {
      if (!CampfireRoast.hasFreeSlot(campfire)) {
        return false; // reselect another fire with room
      }
      if (this.roast.rawInPack(person) == null) {
        return false; // pack is empty of raw food; back to select for a chest
      }
      this.roast.start(person, campfire); // a slot lost to a race is simply tried again
      return true;
    }
    if (!this.roast.done(person)) {
      return true; // still roasting
    }
    this.roast.finish(person, campfire, target);
    return true;
  }

  /** Shelve the cooked food carried, then load up raw food, in one visit. */
  private boolean visitChest(RealPerson person, Container chest) {
    Item cooked = this.roast.cookedInPack(person);
    if (cooked != null) {
      PackLogistics.depositCarried(person, chest, cooked, "COOK");
    }
    int carriedRaw = this.roast.countRawInPack(person);
    for (Item raw : this.roast.cookableRaws(person.level())) {
      if (carriedRaw >= RAWS_PER_TRIP) {
        break;
      }
      carriedRaw += PackLogistics.pullWanted(person, chest,
          List.of(new ItemStack(raw, RAWS_PER_TRIP - carriedRaw)), "COOK");
    }
    return false; // re-select: the fire if loaded, another chest if not
  }

  @Override
  public void released(RealPerson person, BlockPos fire) {
    this.roast.abandon(person, fire);
    this.fireTarget = null;
    this.chestTarget = null;
    this.chestApproach = null;
  }

  /** The shared loop walks beside the fire and still acts on the actual cooking block. */
  @Override
  public BlockPos positionOf(BlockPos target) {
    return this.fireTarget != null && target.equals(this.fireTarget.fire())
        ? this.fireTarget.approach()
        : target.equals(this.chestTarget) && this.chestApproach != null ? this.chestApproach : target;
  }

  @Override
  public boolean inReach(RealPerson person, BlockPos target) {
    if (this.fireTarget != null && target.equals(this.fireTarget.fire())) return CampfireAccess.inReach(person, target);
    boolean handReach = ContainerAccess.canReach(person, person.getEyePosition(), target, reachSqr(person));
    if (!handReach && this.chestApproach != null) {
      this.chestApproach = ContainerAccess.resolveOpenedDoor(person, target, this.chestApproach, reachSqr(person));
    }
    return handReach;
  }

  @Override
  public boolean requiresExactArrival() {
    return true;
  }

  @Override
  public String describe() {
    return "the campfire";
  }

  @Override
  public String activity() {
    return "cooking at the campfire";
  }

  /** A batch check once a second is plenty; the roast itself is the long wait. */
  @Override
  public int actEveryTicks() {
    return 20;
  }

  /** The fire is the one job that belongs at night, and idle campers keep it. */
  @Override
  public boolean worksAtNight() {
    return true;
  }

  /** A storage trip uses the same supported footholds as a fire, never the solid chest block. */
  @Nullable
  private BlockPos selectChest(RealPerson person, @Nullable BlockPos chest) {
    this.chestTarget = chest;
    this.chestApproach = chest == null ? null : ContainerAccess.approachTo(person, chest, reachSqr(person));
    return this.chestApproach == null ? null : chest;
  }

  /** The nearest chest holding any raw the fire can cook, or null. */
  @Nullable
  private BlockPos chestWithRaw(RealPerson person, Village village) {
    List<ItemStack> wanted = this.roast.cookableRaws(person.level()).stream()
        .map(raw -> new ItemStack(raw, RAWS_PER_TRIP))
        .toList();
    return wanted.isEmpty() ? null : PackLogistics.chestHolding(person, village, wanted);
  }
}
