package com.quzzar.kithkyn.entities.ai.goals;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;

/**
 * Opening a fence gate on the way through, and closing it behind.
 *
 * The gate half of what {@link com.quzzar.kithkyn.entities.ai.PersonPathNavigation}
 * starts: the route now runs through closed gates, and this is what turns the
 * bump against one into an open gate and, a second later, a closed one. It is
 * vanilla's {@code OpenDoorGoal} written for {@link FenceGateBlock}, which
 * that goal cannot be told about: it tests for {@code DoorBlock} by class and
 * looks one block up for the door's top half, where a gate has nothing.
 *
 * The gate swings away from whoever opens it, as it does for a player, and
 * closes twenty ticks after opening whether or not the person is through,
 * which is the door's rule too: a pen gate left standing open is a pen with
 * no animals in it by evening.
 *
 * A route through a row of gates can step from one gate cell into the next,
 * as through the Polynesian Coast mine's three or a pen's double gate
 * (2026-09-12). The gate opened is always the closed one on the path, so a
 * person standing in one open gate and bumping the next opens that one too,
 * and no gate closes while their body is still inside it.
 */
public final class OpenFenceGateGoal extends Goal {

  /** Ticks the gate stays open after the person reaches it, as for doors. */
  private static final int OPEN_TICKS = 20;

  /** A person who stands in an open gate keeps it open at most this long. */
  private static final int MAX_HOLD_TICKS = 100;

  /** Within this of a gate on the path counts as at it; vanilla's door figure. */
  private static final double AT_GATE_SQR = 2.25D;

  private final Mob mob;
  private final List<BlockPos> opened = new ArrayList<>();
  private BlockPos gatePos = BlockPos.ZERO;
  private boolean passed;
  private float openDirX;
  private float openDirZ;
  private int openTicks;
  private int heldTicks;

  public OpenFenceGateGoal(Mob mob) {
    if (!(mob.getNavigation() instanceof GroundPathNavigation)) {
      throw new IllegalArgumentException("Unsupported mob type for OpenFenceGateGoal");
    }
    this.mob = mob;
  }

  @Override
  public boolean canUse() {
    if (!this.mob.horizontalCollision) {
      return false; // not up against anything yet
    }
    BlockPos gate = closedGateAhead();
    if (gate == null) {
      return false;
    }
    this.gatePos = gate;
    return true;
  }

  @Override
  public boolean canContinueToUse() {
    return this.heldTicks < MAX_HOLD_TICKS && (this.openTicks > 0 && !this.passed || insideOpenedGate());
  }

  @Override
  public void start() {
    this.opened.clear();
    this.heldTicks = 0;
    this.mob.swing(InteractionHand.MAIN_HAND);
    open(this.gatePos);
  }

  @Override
  public void stop() {
    for (BlockPos gate : this.opened) {
      if (!inside(gate)) {
        setOpen(gate, false);
      }
    }
    this.opened.clear();
  }

  @Override
  public boolean requiresUpdateEveryTick() {
    return true;
  }

  @Override
  public void tick() {
    this.heldTicks++;
    this.openTicks--;
    if (this.mob.horizontalCollision) {
      // Standing in one open gate and pressed against the next closed one on the path.
      BlockPos next = closedGateAhead();
      if (next != null && !this.opened.contains(next)) {
        this.gatePos = next;
        open(next);
      }
    }
    // Passed once the gate is behind them: the same dot-product test the door
    // goal uses, against the direction they were facing when they opened it.
    float toX = (float) (this.gatePos.getX() + 0.5D - this.mob.getX());
    float toZ = (float) (this.gatePos.getZ() + 0.5D - this.mob.getZ());
    if (this.openDirX * toX + this.openDirZ * toZ < 0.0F) {
      this.passed = true;
    }
  }

  /**
   * The nearest closed gate the person is up against on their path, or null. A
   * gate stands at foot level, so the node itself is the gate's block; the door
   * goal looks one up, for the top half a gate does not have. Only a closed gate
   * counts: in a row of gates the first gate node was the open one they stood in.
   */
  @Nullable
  private BlockPos closedGateAhead() {
    GroundPathNavigation navigation = (GroundPathNavigation) this.mob.getNavigation();
    Path path = navigation.getPath();
    if (path == null || path.isDone() || !navigation.canOpenDoors()) {
      return null;
    }
    for (int i = 0; i < Math.min(path.getNextNodeIndex() + 2, path.getNodeCount()); i++) {
      Node node = path.getNode(i);
      BlockPos candidate = new BlockPos(node.x, node.y, node.z);
      if (this.mob.distanceToSqr(candidate.getX(), this.mob.getY(), candidate.getZ()) <= AT_GATE_SQR
          && isClosedGate(candidate)) {
        return candidate;
      }
    }
    BlockPos here = this.mob.blockPosition();
    return isClosedGate(here) ? here : null;
  }

  private void open(BlockPos gate) {
    this.passed = false;
    this.openTicks = OPEN_TICKS;
    this.openDirX = (float) (gate.getX() + 0.5D - this.mob.getX());
    this.openDirZ = (float) (gate.getZ() + 0.5D - this.mob.getZ());
    setOpen(gate, true);
    this.opened.add(gate);
  }

  private boolean insideOpenedGate() {
    return this.opened.stream().anyMatch(this::inside);
  }

  private boolean inside(BlockPos gate) {
    return this.mob.getBoundingBox().intersects(new AABB(gate));
  }

  private boolean isClosedGate(BlockPos pos) {
    BlockState state = this.mob.level().getBlockState(pos);
    return state.getBlock() instanceof FenceGateBlock && !state.getValue(FenceGateBlock.OPEN);
  }

  /** Swing the gate the way vanilla does for a hand: away from the opener. */
  private void setOpen(BlockPos gate, boolean open) {
    BlockState state = this.mob.level().getBlockState(gate);
    if (!(state.getBlock() instanceof FenceGateBlock) || state.getValue(FenceGateBlock.OPEN) == open) {
      return;
    }
    if (open) {
      Direction facing = this.mob.getDirection();
      if (state.getValue(FenceGateBlock.FACING) == facing.getOpposite()) {
        state = state.setValue(FenceGateBlock.FACING, facing);
      }
    }
    state = state.setValue(FenceGateBlock.OPEN, open);
    this.mob.level().setBlock(gate, state, 10);
    this.mob.level().playSound(null, gate,
        open ? SoundEvents.FENCE_GATE_OPEN : SoundEvents.FENCE_GATE_CLOSE, SoundSource.BLOCKS,
        1.0F, this.mob.getRandom().nextFloat() * 0.1F + 0.9F);
    this.mob.level().gameEvent(this.mob, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, gate);
  }
}
