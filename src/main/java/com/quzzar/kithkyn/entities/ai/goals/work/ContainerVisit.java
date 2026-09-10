package com.quzzar.kithkyn.entities.ai.goals.work;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.entities.RealPerson;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Holds vanilla container visuals open during an NPC visit without pretending the NPC is a player.
 * Player menus retain their own opener counts; a visit ending never closes a player's container.
 */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class ContainerVisit {
  private static final Map<UUID, ContainerVisit> ACTIVE = new HashMap<>();

  private final RealPerson person;
  private final ServerLevel level;
  private final BlockPos position;
  private final BlockEntity container;
  private boolean closed;

  private ContainerVisit(RealPerson person, BlockPos position) {
    this.person = person;
    this.level = (ServerLevel) person.level();
    this.position = position.immutable();
    this.container = this.level.getBlockEntity(position);
  }

  /** Called only after arrival at the container. */
  public static ContainerVisit open(RealPerson person, BlockPos position) {
    ContainerVisit previous = ACTIVE.get(person.getUUID());
    if (previous != null) previous.close();
    ContainerVisit visit = new ContainerVisit(person, position);
    boolean wasOpen = visit.openers() > 0;
    ACTIVE.put(person.getUUID(), visit);
    visit.show(visit.openers());
    if (!wasOpen) visit.sound(true);
    return visit;
  }

  /** A closet door stays open for its active storage visit, including delayed shelf transfers. */
  public static boolean needsDoorOpen(Level level, BlockPos door) {
    var state = level.getBlockState(door);
    if (state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) door = door.below();
    for (ContainerVisit visit : ACTIVE.values()) {
      if (visit.level == level && visit.position.getY() == door.getY()
          && visit.position.distManhattan(door) == 1 && visit.present()
          && ContainerAccess.canReach(visit.person, visit.person.getEyePosition(), visit.position, 9.0D)) return true;
    }
    return false;
  }

  /** Finishing, conversation, danger, night and removal all release the same visible visit. */
  public void close() {
    if (this.closed) return;
    this.closed = true;
    ACTIVE.remove(this.person.getUUID(), this);
    if (!present()) return;
    int remaining = openers();
    show(remaining);
    if (remaining == 0) sound(false);
  }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    for (ContainerVisit visit : new ArrayList<>(ACTIVE.values())) {
      if (visit.level.getServer() != event.getServer()) continue;
      if (!visit.person.isAlive() || visit.person.isRemoved() || visit.person.isSleeping()
          || visit.person.isInterrupted() || visit.level.isNight()
          || !ContainerAccess.canReach(visit.person, visit.person.getEyePosition(), visit.position, 9.0D)
          || !visit.present()) {
        visit.close();
      } else if (visit.level.getGameTime() % 5 == 0) {
        // Vanilla periodically recounts player menus, which intentionally know nothing about NPCs.
        visit.show(visit.openers());
      }
    }
  }

  @SubscribeEvent
  public static void removed(EntityLeaveLevelEvent event) {
    ContainerVisit visit = ACTIVE.get(event.getEntity().getUUID());
    if (visit != null) visit.close();
  }

  @SubscribeEvent
  public static void stopping(ServerStoppingEvent event) {
    for (ContainerVisit visit : new ArrayList<>(ACTIVE.values())) {
      if (visit.level.getServer() == event.getServer()) visit.close();
    }
  }

  private boolean present() {
    return this.level.hasChunkAt(this.position) && this.level.getBlockEntity(this.position) == this.container;
  }

  private int openers() {
    int count = 0;
    for (ContainerVisit visit : ACTIVE.values()) {
      if (visit.level == this.level && visit.position.equals(this.position)) count++;
    }
    for (var player : this.level.players()) {
      if (player.containerMenu == player.inventoryMenu) continue;
      boolean viewing = player.containerMenu.slots.stream().anyMatch(slot -> slot.container == this.container
          || (slot.container instanceof CompoundContainer combined && this.container instanceof Container single
              && combined.contains(single)));
      if (viewing) count++;
    }
    return count;
  }

  /** Uses the native chest/shulker lid events and barrel blockstate, so ordinary clients render the visit. */
  private void show(int count) {
    if (!present()) return;
    var state = this.level.getBlockState(this.position);
    if (this.container instanceof ChestBlockEntity || this.container instanceof ShulkerBoxBlockEntity) {
      this.level.blockEvent(this.position, state.getBlock(), 1, count);
    } else if (this.container instanceof BarrelBlockEntity && state.getValue(BarrelBlock.OPEN) != (count > 0)) {
      this.level.setBlock(this.position, state.setValue(BarrelBlock.OPEN, count > 0), 3);
    }
  }

  private void sound(boolean opening) {
    SoundEvent sound = null;
    if (this.container instanceof ChestBlockEntity) sound = opening ? SoundEvents.CHEST_OPEN : SoundEvents.CHEST_CLOSE;
    else if (this.container instanceof BarrelBlockEntity) sound = opening ? SoundEvents.BARREL_OPEN : SoundEvents.BARREL_CLOSE;
    else if (this.container instanceof ShulkerBoxBlockEntity) sound = opening ? SoundEvents.SHULKER_BOX_OPEN : SoundEvents.SHULKER_BOX_CLOSE;
    if (sound != null) this.level.playSound(null, this.position, sound, SoundSource.BLOCKS, 0.5F,
        0.9F + this.level.random.nextFloat() * 0.1F);
  }
}
