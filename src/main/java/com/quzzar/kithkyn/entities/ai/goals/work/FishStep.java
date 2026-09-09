package com.quzzar.kithkyn.entities.ai.goals.work;

import java.util.List;

import javax.annotation.Nullable;

import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.entities.FishingCast;
import com.quzzar.kithkyn.village.LocationManager;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * The fisher's wait: stand at the fishery and draw a fish from adjacent water on
 * a slow cadence.
 *
 * docs/worker-loops.md singles the fisher out as the one job whose real content
 * -- waiting -- does not fit a target-and-verb like the others. So the target is
 * simply the fishing spot (the station, once there is water beside it) and the
 * "act" is time passing: one catch per interval, held as long as the water and
 * the daylight last. Fish land in the fisher's pack; a {@link FishCookStep} at
 * higher priority roasts a batch of the catch at the village fire, and a
 * {@link HaulStep} carries a full pack back, exactly as the miner's ore does.
 * The fisher works with a fishing rod (kit and {@link com.quzzar.kithkyn.entities.JobTool}),
 * though the catch does not depend on the rod: it is the mark of the trade and
 * the thing a better or enchanted pole upgrades, as the hunter's bow is.
 */
public final class FishStep implements BlockWorkStep {

  /**
   * How far around the station, horizontally, to look for water to fish. The
   * fishery's own pool sits a few blocks off the station, so this comfortably
   * covers it with room to spare, and a fisher whose pool is dry works any
   * water source near the post too (Aaron: his pool, or any water nearby).
   */
  private static final int SEARCH = 6;

  /** How far above and below the station water counts: the pool sits a step down. */
  private static final int DEPTH = 2;

  private BlockPos water;
  private int fishingTicks;

  @Override
  @Nullable
  public BlockPos select(RealPerson person) {
    BlockPos station = LocationManager.getJobLocation(person);
    water = station == BlockPos.ZERO ? null : waterNear(person, station);
    if (water == null) {
      return null;
    }
    return station;
  }

  @Override
  public boolean act(RealPerson person, BlockPos target) {
    if (water == null || !openWater(person, water)) {
      return false; // the water dried up or was built over: look again
    }
    person.getLookControl().setLookAt(water.getX() + 0.5D, water.getY() + 0.9D,
        water.getZ() + 0.5D, 30.0F, 30.0F);
    if (fishingTicks == 0) {
      person.castFishingLine(water);
      person.swing(person.getMainHandItem().is(Items.FISHING_ROD)
          ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
      person.playSound(SoundEvents.FISHING_BOBBER_THROW, 0.5F, 1.0F);
    }
    if (++fishingTicks == FishingCast.CAST_TICKS) {
      person.level().playSound((Player) null, water.getX(), water.getY(), water.getZ(),
        SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.PLAYERS, 0.5F,
        0.8F + person.getRandom().nextFloat() * 0.4F);
    }
    if (fishingTicks < FishingCast.CATCH_TICKS) {
      return true;
    }
    person.swing(person.getMainHandItem().is(Items.FISHING_ROD)
          ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
    person.playSound(SoundEvents.FISHING_BOBBER_RETRIEVE, 0.5F, 1.0F);
    ItemStack caught = new ItemStack(person.getRandom().nextInt(4) == 0 ? Items.SALMON : Items.COD);
    person.addItems(List.of(caught));
    person.clearFishingLine();
    fishingTicks = 0;
    return true; // keep fishing; the loop's interrupts and its night check end it
  }

  @Override
  public boolean inReach(RealPerson person, BlockPos target) {
    boolean atStation = BlockWorkStep.super.inReach(person, target);
    if (!atStation) {
      person.clearFishingLine();
      fishingTicks = 0;
    }
    return atStation;
  }

  @Override
  public void released(RealPerson person, BlockPos target) {
    person.clearFishingLine();
    fishingTicks = 0;
    water = null;
  }

  @Override
  public boolean swingsOnAct() {
    return false;
  }

  @Override
  public boolean requiresUpdateEveryTick() {
    return true;
  }

  @Override
  public String describe() {
    return "the water's edge";
  }

  @Override
  public String activity() {
    return "fishing at the water's edge";
  }

  /** Advance the cast and wait every tick; catching still takes twenty seconds at the station. */
  @Override
  public int actEveryTicks() {
    return 1;
  }

  @Nullable
  private BlockPos waterNear(RealPerson person, BlockPos station) {
    BlockPos nearest = null;
    for (BlockPos p : BlockPos.betweenClosed(station.offset(-SEARCH, -DEPTH, -SEARCH),
        station.offset(SEARCH, DEPTH, SEARCH))) {
      if (openWater(person, p) && (nearest == null || p.distSqr(station) < nearest.distSqr(station))) {
        nearest = p.immutable();
      }
    }
    return nearest;
  }

  private boolean openWater(RealPerson person, BlockPos pos) {
    return person.level().hasChunkAt(pos)
        && person.level().getBlockState(pos).is(Blocks.WATER)
        && person.level().getBlockState(pos.above()).isAir();
  }
}
