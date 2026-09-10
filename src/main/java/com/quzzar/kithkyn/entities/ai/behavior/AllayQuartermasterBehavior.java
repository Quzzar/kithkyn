package com.quzzar.kithkyn.entities.ai.behavior;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.economy.Treasury;
import com.quzzar.kithkyn.entities.ai.goals.work.ShelvingAccess;
import com.quzzar.kithkyn.village.ShelvingPlan;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.VillageAllays;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The quartermaster's minding loop, run by an adopted allay ({@code docs/allay-quartermasters.md}).
 *
 * <p>The same three phases as the human {@code ConsolidateStep}, at the same pace:
 * collect from the village's shared workplace containers, deliver to the shelf the
 * shelving plan assigns (overflow shelves only when needed), then inspect the shelves
 * in rotation, tidying each and carrying misfiled stacks to where they belong. The
 * allay's one inventory slot is its pack, so a trip moves one stack; it flies to
 * within arm's reach of a container instead of finding ground beside it, and it opens
 * the wooden doors it meets. It follows the village's shelving plan and never draws
 * one up: planning is a conversation only a person can hold.
 */
public final class AllayQuartermasterBehavior extends Behavior<Allay> {
  private static final int TRANSFER_TICKS = 30;
  private static final int INSPECTION_TICKS = 60;
  private static final int INSPECTION_COOLDOWN_TICKS = 200;
  private static final int MOVES_PER_VISIT = 6;
  private static final int MAX_STALE_CHESTS = 8;
  private static final int TRAVEL_TIMEOUT_TICKS = 400;
  private static final int UNREACHABLE_TICKS = 1200;
  private static final double REACH_SQR = 6.0D;
  private static final float SPEED = 1.0F;

  private enum Phase { COLLECT, DELIVER, INSPECT }

  private Phase phase = Phase.INSPECT;
  private boolean preferredDelivery;
  private boolean returningToShelf;
  @Nullable
  private BlockPos target;
  private long travelStarted;
  private long openedAt;
  private long nextTransferTick;
  private int moves;
  private final Map<BlockPos, Long> failedUntil = new HashMap<>();
  private final Map<BlockPos, Long> inspectedAt = new HashMap<>();

  public AllayQuartermasterBehavior() {
    super(Map.of(), 1, 100_000);
  }

  @Override
  protected boolean checkExtraStartConditions(ServerLevel level, Allay allay) {
    return VillageAllays.village(allay) != null;
  }

  @Override
  protected boolean canStillUse(ServerLevel level, Allay allay, long gameTime) {
    return checkExtraStartConditions(level, allay);
  }

  @Override
  protected void stop(ServerLevel level, Allay allay, long gameTime) {
    release(gameTime);
  }

  @Override
  protected void tick(ServerLevel level, Allay allay, long gameTime) {
    Village village = VillageAllays.village(allay);
    if (village == null) {
      return;
    }
    // A player's gift must not turn the village's keeper into that player's courier.
    allay.getBrain().eraseMemory(MemoryModuleType.LIKED_PLAYER);
    this.failedUntil.entrySet().removeIf(entry -> entry.getValue() <= gameTime);
    if (this.target == null) {
      this.target = select(level, allay, village, gameTime);
      this.travelStarted = gameTime;
      if (this.target == null) {
        hoverHome(level, allay, village);
        return;
      }
    }
    openDoorsAround(level, allay);
    Container container = containerAt(level, this.target);
    if (container == null) {
      release(gameTime);
      return;
    }
    if (!inReach(allay, this.target)) {
      BehaviorUtils.setWalkAndLookTargetMemories(allay, this.target, SPEED, 1);
      if (gameTime - this.travelStarted > TRAVEL_TIMEOUT_TICKS) {
        this.failedUntil.put(this.target, gameTime + UNREACHABLE_TICKS);
        release(gameTime);
      }
      return;
    }
    allay.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
    allay.getLookControl().setLookAt(Vec3.atCenterOf(this.target));
    if (!act(level, allay, village, container, gameTime)) {
      release(gameTime);
    }
  }

  /** The same choice the human loop makes, with a one-slot pack. */
  @Nullable
  private BlockPos select(ServerLevel level, Allay allay, Village village, long now) {
    this.moves = 0;
    Container pack = allay.getInventory();
    boolean carrying = !pack.isEmpty();
    if (!carrying) {
      this.returningToShelf = false;
    }
    BlockPos delivery = carrying ? storehouseChest(level, village, pack) : null;
    if (carrying && delivery == null) {
      village.setStorageStrained(true);
      return inspectionChest(level, village, now);
    }
    BlockPos source = carrying || this.returningToShelf ? null : sourceChest(level, allay, village);
    if (source != null) {
      this.phase = Phase.COLLECT;
      return source;
    }
    if (delivery != null) {
      this.phase = Phase.DELIVER;
      return delivery;
    }
    village.setStorageStrained(false);
    return inspectionChest(level, village, now);
  }

  /** One paced transfer at a time, like a pair of hands would manage. */
  private boolean act(ServerLevel level, Allay allay, Village village, Container container, long now) {
    if (this.nextTransferTick == 0) {
      this.openedAt = now;
      this.nextTransferTick = now + TRANSFER_TICKS;
      return true;
    }
    if (now < this.nextTransferTick) {
      return true;
    }
    this.nextTransferTick = now + TRANSFER_TICKS;
    Container pack = allay.getInventory();
    List<BlockPos> chests = VillageAllays.shelfPositions(village);
    if (this.phase == Phase.COLLECT) {
      int moved = ShelvingAccess.collectOne(container, pack);
      return moved > 0 && ++this.moves < MOVES_PER_VISIT && hasRoom(pack);
    }
    if (this.phase == Phase.DELIVER) {
      int moved = ShelvingAccess.depositOne(pack, container, shelfOffset(level, chests, this.target),
          ShelvingPlan.load(village), this.preferredDelivery);
      if (moved > 0) {
        Kithkyn.LOGGER.debug("[resource-flow] {} (allay) shelved {} item(s) at {}",
            allay.getName().getString(), moved, this.target.toShortString());
      }
      if (pack.isEmpty()) {
        village.setStorageStrained(false);
      }
      return moved > 0 && ++this.moves < MOVES_PER_VISIT && !pack.isEmpty();
    }
    if (this.moves++ == 0) {
      ShelvingAccess.tidyVisitedShelf(ShelvingPlan.load(village), shelfOffset(level, chests, this.target), container);
      if (ShelvingAccess.collectMisfiled(ShelvingPlan.load(village), chests, pos -> containerAt(level, pos),
          other -> !this.failedUntil.containsKey(other), pack, this.target, container)) {
        this.returningToShelf = true;
        return false;
      }
    }
    return now - this.openedAt < INSPECTION_TICKS;
  }

  private void release(long now) {
    if (this.target != null && this.phase == Phase.INSPECT) {
      this.inspectedAt.put(this.target, now);
    }
    this.target = null;
    this.nextTransferTick = 0;
    this.moves = 0;
  }

  /** Sources remain the village's shared workplace containers, excluding the market and storehouse. */
  @Nullable
  private BlockPos sourceChest(ServerLevel level, Allay allay, Village village) {
    List<BlockPos> skip = new ArrayList<>(VillageAllays.shelfPositions(village));
    skip.addAll(Treasury.chestPositions(village, level));
    for (int attempt = 0; attempt < MAX_STALE_CHESTS; attempt++) {
      BlockPos found = village.getNearestContainer(allay.blockPosition(), skip);
      if (found.equals(BlockPos.ZERO)) {
        return null;
      }
      Container container = containerAt(level, found);
      if (container != null && !container.isEmpty() && !this.failedUntil.containsKey(found)) {
        return found;
      }
      skip.add(found);
    }
    return null;
  }

  /** Prefer the actual shelf that owns carried goods; visit overflow shelves only when needed. */
  @Nullable
  private BlockPos storehouseChest(ServerLevel level, Village village, Container pack) {
    ShelvingPlan plan = ShelvingPlan.load(village);
    List<BlockPos> chests = VillageAllays.shelfPositions(village);
    for (boolean preferred : new boolean[] {true, false}) {
      int offset = 0;
      for (BlockPos pos : chests) {
        Container shelf = containerAt(level, pos);
        if (shelf == null) {
          continue;
        }
        if (!this.failedUntil.containsKey(pos) && ShelvingAccess.canDeposit(pack, shelf, offset, plan, preferred)) {
          this.preferredDelivery = preferred;
          return pos;
        }
        offset += shelf.getContainerSize();
      }
    }
    return null;
  }

  /** Quiet work rotates across real shelves rather than hovering at one. */
  @Nullable
  private BlockPos inspectionChest(ServerLevel level, Village village, long now) {
    this.phase = Phase.INSPECT;
    List<BlockPos> shelves = new ArrayList<>(VillageAllays.shelfPositions(village));
    shelves.sort(Comparator.comparingLong(pos -> this.inspectedAt.getOrDefault(pos, Long.MIN_VALUE)));
    for (BlockPos shelf : shelves) {
      Long visited = this.inspectedAt.get(shelf);
      if (visited != null && now - visited < INSPECTION_COOLDOWN_TICKS) {
        continue;
      }
      if (containerAt(level, shelf) != null && !this.failedUntil.containsKey(shelf)) {
        return shelf;
      }
    }
    return null;
  }

  /** Between rounds the keeper waits above its counter, or over the first shelf when there is none. */
  private static void hoverHome(ServerLevel level, Allay allay, Village village) {
    BlockPos home = VillageAllays.home(level, village);
    if (home != null && allay.position().distanceToSqr(Vec3.atCenterOf(home.above())) > 9.0D) {
      BehaviorUtils.setWalkAndLookTargetMemories(allay, home.above(), SPEED, 2);
    }
  }

  /** Allays cannot open doors on their own; the keeper opens the wooden ones it brushes past. */
  private static void openDoorsAround(ServerLevel level, Allay allay) {
    BlockPos at = allay.blockPosition();
    for (BlockPos pos : BlockPos.betweenClosed(at.offset(-1, -1, -1), at.offset(1, 1, 1))) {
      BlockState state = level.getBlockState(pos);
      if (DoorBlock.isWoodenDoor(state) && !state.getValue(DoorBlock.OPEN)) {
        ((DoorBlock) state.getBlock()).setOpen(allay, level, state, pos, true);
      }
    }
  }

  private static boolean inReach(Allay allay, BlockPos target) {
    return allay.getEyePosition().distanceToSqr(Vec3.atCenterOf(target)) <= REACH_SQR;
  }

  private static boolean hasRoom(Container pack) {
    for (int slot = 0; slot < pack.getContainerSize(); slot++) {
      if (pack.getItem(slot).getCount() < pack.getItem(slot).getMaxStackSize()) {
        return true;
      }
    }
    return false;
  }

  private static int shelfOffset(ServerLevel level, List<BlockPos> chests, BlockPos target) {
    return ShelvingAccess.shelfOffset(chests, pos -> containerAt(level, pos), target);
  }

  @Nullable
  private static Container containerAt(ServerLevel level, BlockPos pos) {
    return level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof Container container ? container : null;
  }
}
