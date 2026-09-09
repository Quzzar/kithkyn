package com.quzzar.kithkyn.entities.ai.goals.work;

import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.savedata.PlacedBlockStore;
import com.quzzar.kithkyn.village.LocationManager;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.VillagePaths;
import com.quzzar.kithkyn.village.buildings.Building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SaplingBlock;

/**
 * Wearing a route from the campfire to a building's door: a PLACE step whose
 * act belongs to the journey rather than to the destination.
 *
 * The only step that lays as it walks, and the reason {@link WorkStep} has a
 * travelling variant at all. The builder starts at the town centre, where the
 * campfire is, walks quietly out to a building, and treads a path to its
 * doorstep, turning ground to dirt path beneath and beside them as they go
 * ({@link LocationManager#getEntrance}), so a villager can always get from the
 * campfire in at the door (docs/worker-loops.md, doorstep spokes 2026-09-03).
 * A spoke to each door, rather than a route between two building centres, is
 * what makes a building reachable: a path aimed at a building's middle stops at
 * the near wall when the door is round the back. The {@link GradingSurvey} path
 * pass then settles the worn spoke into a gentle ramp.
 *
 * Paths are what a builder does when there is nothing to build, so the step
 * finds no work at all while a project stands. Refusing outright rather than
 * relying on goal priority keeps the two duties in the order the village cares
 * about: the movement flag alone would let path-laying take over the moment
 * construction paused for a tick.
 *
 * <b>This also ends.</b> The goal it replaces finished a path by calling
 * {@code stop()} from inside {@code tick()}, which cleared its two buildings
 * but did not end anything - {@code canContinueToUse} still said yes, so the
 * builder held the movement flag and did nothing at all until nightfall or the
 * next project.
 */
public final class PathStep implements BlockWorkStep {

  private final boolean initialOnly;
  private final Map<String, Long> retryAfter = new HashMap<>();
  private static final String INITIAL_PATHS = "initial_building_paths";

  public PathStep() { this(false); }

  /** One successful access trip per building revision; ordinary maintenance remains lower priority. */
  public PathStep(boolean initialOnly) { this.initialOnly = initialOnly; }

  private static final List<Block> PAVEABLE = Arrays.asList(
      Blocks.GRASS_BLOCK, Blocks.MYCELIUM, Blocks.STONE, Blocks.SAND, Blocks.RED_SAND,
      Blocks.GRAVEL, Blocks.CLAY, Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT,
      Blocks.PODZOL);

  /** Fraction of a building's radius that counts as having arrived at it. */
  private static final double ARRIVAL_FRACTION = 0.4D;

  private Building nearEnd;
  private VillagePaths.Destination destination;
  @Nullable
  private BlockPos destinationTarget;
  private boolean laying;

  @Override
  @Nullable
  public BlockPos select(RealPerson person) {
    Village village = person.getVillage();
    if (village == null || (village.getCurrentProject() != null && person.isConstructionLead())) {
      return null; // there is something to build; paths can wait
    }
    if (!(person.level() instanceof ServerLevel level)) {
      return null;
    }
    if (this.laying && this.destinationTarget != null) {
      return this.destinationTarget; // still treading the spoke out to the far end
    }
    Building hub = village.getTownCenter();
    List<VillagePaths.Destination> destinations = VillagePaths.destinations(level, village);
    if (hub == null || destinations.isEmpty()) {
      return null;
    }
    retryAfter.entrySet().removeIf(entry -> entry.getValue() <= level.getGameTime());
    CompoundTag done = village.getBrain().getStrategyData().getCompound(INITIAL_PATHS);
    List<VillagePaths.Destination> available = destinations.stream()
        .filter(entry -> entry.target() != null && !retryAfter.containsKey(entry.id()))
        .filter(entry -> !initialOnly || entry.initialEligible()
            && !entry.revision().equals(done.getString(entry.id()))).toList();
    VillagePaths.Destination to = available.isEmpty() ? null : initialOnly ? available.getFirst()
        : available.get(person.getRandom().nextInt(available.size()));
    if (to == null) return null;
    this.nearEnd = hub;
    this.destination = to;
    this.destinationTarget = to.target();
    this.laying = false;
    return BlockPos.of(hub.getCenterLocation());
  }

  @Override
  public boolean act(RealPerson person, BlockPos target) {
    if (!this.laying) {
      // Arrived at the near end: from here the walking itself is the work.
      this.laying = true;
      return false;
    }
    if (arrived(person)) {
      Village village = person.getVillage();
      if (village != null && destination != null) {
        CompoundTag strategy = village.getBrain().getStrategyData();
        CompoundTag done = strategy.getCompound(INITIAL_PATHS);
        var registered = VillagePaths.registeredIds(village);
        done.getAllKeys().removeIf(key -> !registered.contains(key));
        done.putString(destination.id(), destination.revision());
        strategy.put(INITIAL_PATHS, done);
        if (person.level() instanceof ServerLevel level)
          com.quzzar.kithkyn.village.VillageManager.get(level).setDirty();
      }
      this.laying = false;
      this.nearEnd = null;
      this.destination = null;
      this.destinationTarget = null;
      return false; // spoke complete; the next scan picks a fresh building
    }
    pave(person);
    return true;
  }

  @Override
  public void released(RealPerson person, BlockPos target) {
    if (!this.laying) {
      this.nearEnd = null;
      this.destination = null;
      this.destinationTarget = null;
    }
  }

  @Override
  public void unreachable(RealPerson person, BlockPos target) {
    if (destination != null) retryAfter.put(destination.id(), person.level().getGameTime() + 1200);
    laying = false;
  }

  @Override
  public String describe() {
    return "the way between our buildings and gates";
  }

  @Override
  public String activity() {
    return "laying paths between our buildings and gates";
  }

  /** Laying happens on the move; walking to the near end does not. */
  @Override
  public boolean actWhileTravelling() {
    return this.laying;
  }

  @Override
  public int actEveryTicks() {
    return 10;
  }

  /**
   * A builder is at a building once well inside its radius - and which building
   * that is depends on which half of the trip they are on. Measuring the near
   * end against the FAR end's radius, as this did at first, means a builder
   * setting off from a large building toward a small one has to reach a point
   * they may not be able to stand on, never arrives, and is eventually
   * teleported home as stranded.
   */
  @Override
  public double reachSqr(RealPerson person) {
    if (this.laying) {
      return farReachSqr(); // arrive at the doorstep, or within a doorless building's radius
    }
    Building at = this.nearEnd;
    if (at == null) {
      return 9.0D;
    }
    double radius = at.getRadius();
    return radius * radius * ARRIVAL_FRACTION;
  }

  @Override
  public double speed() {
    return this.laying ? 0.3D : 0.5D; // a path is trodden slowly
  }

  /** How close counts as arrived at the far end: right at a doorstep, or within a doorless building's radius. */
  private double farReachSqr() {
    return this.destination == null ? 3.6D : this.destination.reachSqr();
  }

  private boolean arrived(RealPerson person) {
    if (this.destinationTarget == null) {
      return true;
    }
    return this.destinationTarget.distSqr(person.blockPosition()) <= farReachSqr();
  }

  /** Turns one square under or beside the builder into path, if it may be turned. */
  private void pave(RealPerson person) {
    BlockPos ground = switch (person.getRandom().nextInt(5)) {
      case 1 -> person.getOnPos().relative(Direction.EAST);
      case 2 -> person.getOnPos().relative(Direction.NORTH);
      case 3 -> person.getOnPos().relative(Direction.SOUTH);
      case 4 -> person.getOnPos().relative(Direction.WEST);
      default -> person.getOnPos();
    };
    if (!(person.level() instanceof ServerLevel level)) return;
    PlacedBlockStore placed = PlacedBlockStore.get(level);
    Village village = person.getVillage();
    if (placed.isVillagePlaced(ground) || placed.isPlayerPlaced(ground)
        || placed.isVillagePlaced(ground.above()) || placed.isPlayerPlaced(ground.above())
        || level.getBlockEntity(ground) != null || level.getBlockEntity(ground.above()) != null
        || (village != null && village.hasClaimed(ground))
        || !level.getFluidState(ground).isEmpty() || !level.getFluidState(ground.above()).isEmpty()
        || !PAVEABLE.contains(level.getBlockState(ground).getBlock())) {
      return;
    }
    // Never pave out something that is growing.
    Block above = person.level().getBlockState(ground.above()).getBlock();
    if (above instanceof SaplingBlock || above instanceof CropBlock
        || !level.getBlockState(ground.above()).getCollisionShape(level, ground.above()).isEmpty()) {
      return;
    }
    person.level().setBlock(ground, Blocks.DIRT_PATH.defaultBlockState(), 2);
    person.level().playSound((Player) null, ground.getX(), ground.getY(), ground.getZ(),
        SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F,
        person.getRandom().nextFloat() * 0.4F + 0.8F);
  }

}
