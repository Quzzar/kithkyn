package com.quzzar.kithkyn.entities.ai.goals;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.entities.StashOffer;
import com.quzzar.kithkyn.entities.ai.goals.work.ContainerAccess;
import com.quzzar.kithkyn.entities.ai.goals.work.PackLogistics;
import com.quzzar.kithkyn.village.LocationManager;
import com.quzzar.kithkyn.village.PersonalChest;
import com.quzzar.kithkyn.village.buildings.Building;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;

/**
 * The walk that follows a bedtime "keep it" or "take it out": a villager who
 * chose to hold things back from the village stores ({@link StashOffer})
 * carries them home and sets them down in their own chest by hand, and one
 * who chose to take things out lifts them from the chest into the pack at the
 * same visit. Runs ahead of {@link SleepAtNightGoal}, which takes over the
 * moment the chest is done.
 *
 * <p>Two legs when the walk starts outside: to the doorstep first, then to
 * a standing position with hand access to the chest ({@link LocationManager#getEntrance}). A path aimed straight at a
 * chest indoors stalls against the nearest outside wall when the door is on
 * the far side; the level-3 house at Wildflower Downs had its door away from
 * the village and everyone stopped under the upstairs chest, outside, night
 * after night, while a walk that began inside the house reached it. From the
 * doorstep the rest is a dozen nodes.
 *
 * <p>Bounded so a bad night cannot cost the village the goods, and bounded by
 * headway rather than by what the pathfinder says, whose verdict on whether
 * a path reaches proved worthless live: a villager who has not come closer
 * to the leg's target for a stretch (stood at the foot of a ladder, or found
 * no way in) gives up, as does one still walking after a few minutes. A chest
 * that is gone or full gives up on arrival. In every case what was to be kept
 * stays in the pack, where the next bedtime stow returns it to the stores as
 * before, and what was to be taken out stays in the chest.
 */
public class StashAtHomeGoal extends Goal {

  /**
   * Goal ticks of walking before the trip is given up. The selector ticks a
   * goal every other server tick, so this is about three minutes: a worker
   * who chose at the far end of the village still has time to walk home and
   * climb the stairs.
   */
  private static final int GIVE_UP_TICKS = 1800;
  /** Goal ticks without coming closer to the target before the walk counts as going nowhere (about 40 s). */
  private static final int STALL_TICKS = 400;
  /** Closer by this much (blocks) counts as headway. */
  private static final double HEADWAY = 0.5D;
  /** Three blocks to the doorstep, or from the eyes to an unobstructed household container. */
  private static final double REACH_SQR = 9.0D;
  /** Retry a blocked indoor approach at most once per twenty goal ticks. */
  private static final int APPROACH_RETRY_TICKS = 20;

  private final RealPerson person;
  private BlockPos chest;
  private BlockPos leg;
  private BlockPos approach;
  private boolean indoors;
  private int nextApproachTick;
  private int ticks;
  private int stalledTicks;
  private double bestDistance;

  public StashAtHomeGoal(RealPerson person) {
    this.setFlags(EnumSet.of(Flag.MOVE));
    this.person = person;
  }

  @Override
  public boolean canUse() {
    Set<Item> keeping = person.keepingForHome();
    Set<Item> taking = person.takingFromHome();
    if (keeping.isEmpty() && taking.isEmpty()) {
      return false;
    }
    if (!person.level().isNight()) {
      // The night is over. Whatever was not put away is the stores' again at
      // the next bedtime; nobody walks their keepsakes home by daylight.
      person.doneKeeping();
      return false;
    }
    if (taking.isEmpty() && keeping.stream().noneMatch(item -> person.personMainInv.countItem(item) > 0)) {
      person.doneKeeping(); // already set down, or stowed by a refire
      return false;
    }
    this.chest = PersonalChest.of(person);
    if (this.chest == null) {
      person.doneKeeping(); // the home went since bedtime; the next stow takes it
      return false;
    }
    return true;
  }

  @Override
  public boolean canContinueToUse() {
    return (!person.keepingForHome().isEmpty() || !person.takingFromHome().isEmpty())
        && this.chest != null && this.ticks < GIVE_UP_TICKS && person.level().isNight();
  }

  @Override
  public void start() {
    this.ticks = 0;
    this.approach = null;
    this.nextApproachTick = 0;
    this.leg = firstLeg();
    this.indoors = this.leg.equals(this.chest);
    aimAt(this.leg);
  }

  /** The doorstep when the walk starts outside the home; the chest itself when it starts inside or the home has no door. */
  private BlockPos firstLeg() {
    Building home = PersonalChest.home(person);
    if (home == null || !(person.level() instanceof ServerLevel level)) {
      return this.chest;
    }
    LocationManager.Entrance entrance = LocationManager.getEntrance(level, home);
    if (entrance == null || entrance.contains(person.blockPosition())) {
      return this.chest;
    }
    return entrance.doorstep();
  }

  private void aimAt(BlockPos target) {
    this.leg = target;
    this.stalledTicks = 0;
    this.bestDistance = Double.MAX_VALUE;
    walk();
  }

  @Override
  public void tick() {
    this.ticks++;
    if (this.indoors && ContainerAccess.canReach(person, person.getEyePosition(), this.chest, REACH_SQR)) {
      person.getNavigation().stop();
      putAway();
      return;
    }
    if (this.indoors && this.approach != null) {
      BlockPos resolved = ContainerAccess.resolveOpenedDoor(person, this.chest, this.approach, REACH_SQR);
      if (!resolved.equals(this.approach)) {
        this.approach = resolved;
        aimAt(resolved);
      }
    }
    double distance = person.position().distanceTo(this.indoors
        ? Vec3.atBottomCenterOf(this.leg) : Vec3.atCenterOf(this.leg));
    if (!this.indoors && distance * distance <= REACH_SQR) {
      this.indoors = true;
      aimAt(this.chest); // choose a supported indoor approach from the doorstep
      return;
    }
    if (distance < this.bestDistance - HEADWAY) {
      this.bestDistance = distance;
      this.stalledTicks = 0;
    } else {
      this.stalledTicks++;
    }
    if (this.stalledTicks >= STALL_TICKS) {
      Kithkyn.LOGGER.info("'{}' made no headway toward their chest at home ({} blocks off, {}); {}",
          person.getFullName(), Math.round(distance), this.indoors ? "indoors" : "on the way to the door",
          pending());
      person.doneKeeping();
      return;
    }
    if (this.ticks >= GIVE_UP_TICKS) {
      Kithkyn.LOGGER.info("'{}' could not reach their chest at home tonight; {}",
          person.getFullName(), pending());
      person.doneKeeping();
      return;
    }
    if (!person.getNavigation().isInProgress()) {
      walk();
    }
  }

  private void walk() {
    if (this.indoors && this.approach == null) {
      person.getNavigation().stop();
      if (this.ticks < this.nextApproachTick) return;
      this.nextApproachTick = this.ticks + APPROACH_RETRY_TICKS;
      this.approach = ContainerAccess.approachTo(person, this.chest, REACH_SQR);
      if (this.approach == null) return;
      this.leg = this.approach;
      this.bestDistance = Double.MAX_VALUE;
      this.stalledTicks = 0;
    }
    if (this.indoors) {
      // The selected foothold is exact; coordinate movement accepts stopping one cell short.
      person.getNavigation().moveTo(person.getNavigation().createPath(this.leg, 0), 0.5D);
    } else {
      person.getNavigation().moveTo(this.leg.getX() + 0.5D, this.leg.getY(), this.leg.getZ() + 0.5D, 0.5D);
    }
  }

  /** What an abandoned visit leaves where it is: "the wheat stays in the pack, the apple stays in the chest". */
  private String pending() {
    List<String> parts = new ArrayList<>();
    if (!person.keepingForHome().isEmpty()) {
      parts.add("the " + StashOffer.names(person.keepingForHome()) + " stays in the pack");
    }
    if (!person.takingFromHome().isEmpty()) {
      parts.add("the " + StashOffer.names(person.takingFromHome()) + " stays in the chest");
    }
    return String.join(", ", parts);
  }

  /** At the chest: set the kept kinds down first, which frees the pack, then lift the taken kinds out. */
  private void putAway() {
    Set<Item> keeping = person.keepingForHome();
    Set<Item> taking = person.takingFromHome();
    Container container = PersonalChest.container(person, this.chest);
    if (container == null) {
      Kithkyn.LOGGER.info("'{}' found no chest at home; {}", person.getFullName(), pending());
      person.doneKeeping();
      return;
    }
    int put = 0;
    for (Item item : keeping) {
      put += PackLogistics.depositCarried(person, container, item, "home");
    }
    if (put > 0) {
      Kithkyn.LOGGER.info("'{}' put {} item(s) of {} away in their chest at home", person.getFullName(),
          put, StashOffer.names(keeping));
    } else if (!keeping.isEmpty()) {
      Kithkyn.LOGGER.info("'{}' found no room in their chest at home for the {}", person.getFullName(),
          StashOffer.names(keeping));
    }
    int took = 0;
    for (Item item : taking) {
      took += PackLogistics.takeStored(person, container, item, "home");
    }
    if (took > 0) {
      Kithkyn.LOGGER.info("'{}' took {} item(s) of {} out of their chest at home", person.getFullName(),
          took, StashOffer.names(taking));
    } else if (!taking.isEmpty()) {
      Kithkyn.LOGGER.info("'{}' found none of the {} left in their chest at home, or no room in the pack for it",
          person.getFullName(), StashOffer.names(taking));
    }
    person.doneKeeping();
  }
}
