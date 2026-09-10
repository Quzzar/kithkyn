package com.quzzar.kithkyn.entities.ai.goals.work;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.bookkeeping.NoResourceBookkeepingEvent;
import com.quzzar.kithkyn.village.buildings.Materials;
import com.quzzar.kithkyn.village.buildings.WallProject;
import com.quzzar.kithkyn.village.buildings.WallRaiser;
import com.quzzar.kithkyn.village.buildings.WallTier;
import com.quzzar.kithkyn.village.buildings.WorkerFooting;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.pathfinder.Path;

/**
 * Builds the village wall one explicit construction cell at a time.
 *
 * A {@link WallProject} is divided into short, persistent sections. Each builder
 * leases a different section, walks to its next cell, and places that cell with
 * one swing. The result grows like a building instead of teleporting a complete
 * wall column into existence on one right-click.
 */
public final class WallStep implements BlockWorkStep {

  private static final int LOAD_PER_TRIP = 64;
  private static final int LOW_WATER = 8;
  private static final int PATHS_PER_SCAN = 8;
  /** Wall foundations and overhead courses are built from nearby ground, not by diving or climbing each cell. */
  private static final int VERTICAL_REACH = 12;

  /** Paid-for wall cells left from the current abstract material item. */
  private int credit;
  @Nullable
  private WallRaiser.WallWork targetedWork;
  private WallProject reviewedFoundations;
  private BlockPos lastStand;
  private BlockPos searchCell;
  private List<BlockPos> candidates = List.of();
  private int candidateCursor;
  private final Map<BlockPos, Long> failedUntil = new HashMap<>();

  @Override
  @Nullable
  public BlockPos select(RealPerson person) {
    Village village = person.getVillage();
    if (village == null || (village.getCurrentProject() != null && person.isConstructionLead())) {
      return null;
    }
    WallProject wall = village.getWallProject();
    if (wall != null && wall != reviewedFoundations) {
      WallRaiser.settleFoundations(person.level(), wall);
      reviewedFoundations = wall;
    }
    if (wall == null || wall.isComplete() || !wall.isSiteCleared()) {
      return null;
    }

    WallRaiser.WallWork work = WallRaiser.nextWork(
        person.level(), wall, person.getUUID(), person.blockPosition());
    if (work == null) {
      return null;
    }
    int itemsNeeded = this.credit > 0 ? 0 : 1;
    int carried = PackLogistics.carried(person, wall.getTier().material());
    if (itemsNeeded > 0 && carried < LOW_WATER) {
      BlockPos source = PackLogistics.chestHolding(person, village,
          List.of(new ItemStack(wall.getTier().material(), LOAD_PER_TRIP)));
      if (source != null) {
        this.targetedWork = null;
        return source;
      }
    }
    if (carried < itemsNeeded) {
      return null;
    }
    this.targetedWork = work;
    BlockPos stand = standFor(person, village, work.block().pos());
    if (stand == null && candidateCursor >= candidates.size()) {
      wall.defer(person.getUUID(), work.section(), person.level().getGameTime());
      person.logBlocker("I cannot find safe footing within reach of this wall section.");
      searchCell = null;
    } else if (stand != null) {
      person.clearBlocker("I cannot find safe footing within reach of this wall section.");
    }
    return stand;
  }

  @Override
  public boolean act(RealPerson person, BlockPos target) {
    Village village = person.getVillage();
    if (village == null) {
      return false;
    }
    WallProject wall = village.getWallProject();
    if (wall == null || wall.isComplete()) {
      return false;
    }
    Container chest = PackLogistics.containerAt(person, target);
    if (chest != null) {
      PackLogistics.pullWanted(person, chest,
          List.of(new ItemStack(wall.getTier().material(), LOAD_PER_TRIP)), "BUILDER");
      return false;
    }
    WallRaiser.WallWork work = this.targetedWork;
    if (work == null || !WallRaiser.isCurrent(wall, person.getUUID(), work)) {
      return false;
    }
    if (!inReach(person, target)) return false;
    // Never place the next course into a worker or another living body.
    if (!person.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
        new net.minecraft.world.phys.AABB(work.block().pos())).isEmpty()) return false;
    if (WallRaiser.isSatisfied(
        person.level(), work.block(), wall)) {
      wall.advance(person.getUUID(), work.section());
      finishCompletedWall(person, wall);
      return false;
    }
    if (!payForCell(person, village, wall.getTier())) {
      return false;
    }

    WallRaiser.place(person.level(), work.block(), wall);
    wall.advance(person.getUUID(), work.section());
    if (wall.section(work.section()).isComplete() && !wall.isComplete()) {
      WallRaiser.settleFoundations(person.level(), wall);
    }
    finishCompletedWall(person, wall);
    BlockPos pos = work.block().pos();
    person.level().playSound((Player) null, pos,
        work.block().desiredState(wall.getTier(), wall.getStyle()).getSoundType().getPlaceSound(),
        SoundSource.BLOCKS, 1.0F, person.getRandom().nextFloat() * 0.3F + 0.85F);
    return false;
  }

  private static void finishCompletedWall(RealPerson person, WallProject wall) {
    if (wall.isComplete()) {
      WallRaiser.finishWall(person.level(), wall);
      Village village = person.getVillage();
      if (village != null) {
        com.quzzar.kithkyn.village.JobClaiming.registerMissingStations(village);
      }
    }
  }

  private boolean payForCell(RealPerson person, Village village, WallTier tier) {
    if (this.credit > 0) {
      this.credit--;
      return true;
    }
    if (PackLogistics.carried(person, tier.material()) < 1) {
      if (Materials.counted(village.stockTally(), tier.material()) == 0) {
        village.logEvent(new NoResourceBookkeepingEvent(tier.material(), 1));
        person.logBlocker("we are short of materials to raise the village wall");
      }
      return false;
    }
    person.clearBlocker("we are short of materials to raise the village wall");
    Materials.take(person.personMainInv, tier.material(), 1);
    this.credit = WallTier.BLOCKS_PER_ITEM - 1;
    return true;
  }

  /** Postpones one inaccessible section while every other section remains available. */
  @Override
  public void unreachable(RealPerson person, BlockPos target) {
    failedUntil.put(target, person.level().getGameTime() + 1200);
    lastStand = null;
    searchCell = null;
    WallRaiser.WallWork work = this.targetedWork;
    Village village = person.getVillage();
    WallProject wall = village == null ? null : village.getWallProject();
    if (work == null || wall == null || !WallRaiser.isCurrent(wall, person.getUUID(), work)) {
      return;
    }
    wall.defer(person.getUUID(), work.section(), person.level().getGameTime());
    BlockPos pos = work.block().pos();
    Kithkyn.LOGGER.debug("[wall] {} deferred unreachable {} section at {}, {}",
        person.getFullName(), wall.section(work.section()).kind().name().toLowerCase(),
        pos.getX(), pos.getZ());
  }

  @Override
  public void released(RealPerson person, BlockPos target) {
    this.targetedWork = null;
  }

  @Override
  public String describe() {
    return "the village wall";
  }

  @Override
  public String activity() {
    return "raising the village wall";
  }

  @Override
  public int actEveryTicks() {
    return 10;
  }

  @Override
  public double speed() {
    return 0.45D;
  }

  @Override
  public double reachSqr(RealPerson person) {
    return targetedWork == null ? 6.0D : 1.0D;
  }

  @Override
  public boolean requiresExactArrival() {
    return targetedWork != null;
  }

  /** A wider build radius does not let a worker act while still in water or far from its safe foothold. */
  @Override
  public boolean inReach(RealPerson person, BlockPos target) {
    if (targetedWork == null) return person.blockPosition().distSqr(target) <= reachSqr(person);
    return person.position().distanceToSqr(net.minecraft.world.phys.Vec3.atBottomCenterOf(target)) <= 1.0D
        && WorkerFooting.canStand(person, target)
        && person.level().getFluidState(person.blockPosition()).isEmpty()
        && person.level().noCollision(person, person.getBoundingBox())
        && withinWorkRange(person.blockPosition(), targetedWork.block().pos());
  }

  private static boolean withinWorkRange(BlockPos stand, BlockPos cell) {
    long dx = (long) stand.getX() - cell.getX(), dz = (long) stand.getZ() - cell.getZ();
    return dx * dx + dz * dz > 0
        && dx * dx + dz * dz <= WallWorkPlanner.MAXIMUM_OFFSET * WallWorkPlanner.MAXIMUM_OFFSET
        && Math.abs(stand.getY() - cell.getY()) <= VERTICAL_REACH;
  }

  /** Searches real walking surfaces in bounded path batches, including side approaches and raised ground. */
  @Nullable
  private BlockPos standFor(RealPerson person, Village village, BlockPos wallCell) {
    long now = person.level().getGameTime();
    failedUntil.entrySet().removeIf(entry -> entry.getValue() <= now);
    if (lastStand != null && !failedUntil.containsKey(lastStand)
        && withinWorkRange(lastStand, wallCell) && WorkerFooting.canStand(person, lastStand)
        && reachable(person, lastStand)) return lastStand;
    lastStand = null;
    if (!wallCell.equals(searchCell)) {
      searchCell = wallCell;
      candidateCursor = 0;
      BlockPos centre = village.getCenterPosition();
      int inwardX = Integer.signum(centre.getX() - wallCell.getX());
      int inwardZ = Integer.signum(centre.getZ() - wallCell.getZ());
      List<BlockPos> found = new ArrayList<>();
      for (WallWorkPlanner.Offset offset : WallWorkPlanner.offsets(inwardX, inwardZ)) {
        for (int dy = -VERTICAL_REACH; dy <= VERTICAL_REACH; dy++) {
          BlockPos at = wallCell.offset(offset.x(), dy, offset.z());
          if (WorkerFooting.canStand(person, at)) found.add(at);
        }
      }
      found.sort(Comparator.comparingDouble(at -> at.distSqr(person.blockPosition())));
      candidates = List.copyOf(found);
    }
    for (int tested = 0; tested < PATHS_PER_SCAN && candidateCursor < candidates.size();) {
      BlockPos at = candidates.get(candidateCursor++);
      if (failedUntil.containsKey(at) || !WorkerFooting.canStand(person, at)) continue;
      tested++;
      if (reachable(person, at)) {
        lastStand = at;
        return at;
      }
    }
    return null;
  }

  private static boolean reachable(RealPerson person, BlockPos spot) {
    if (person.position().distanceToSqr(net.minecraft.world.phys.Vec3.atBottomCenterOf(spot)) < 0.25D) {
      return true;
    }
    Path path = person.getNavigation().createPath(spot, 0);
    return path != null && path.canReach() && path.getEndNode() != null
        && path.getEndNode().asBlockPos().equals(spot);
  }
}
