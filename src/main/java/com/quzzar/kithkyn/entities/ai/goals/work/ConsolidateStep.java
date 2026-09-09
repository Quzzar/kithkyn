package com.quzzar.kithkyn.entities.ai.goals.work;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.economy.Treasury;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.llm.LlmService;
import com.quzzar.kithkyn.village.QuartermasterPlanner;
import com.quzzar.kithkyn.village.ShelvingPlan;
import com.quzzar.kithkyn.village.Storehouse;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.buildings.WorkerFooting;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Carries workplace output to the storehouse, then visits its shelves to maintain the shelving plan.
 * Every inventory write is to the container being visited or the quartermaster's own pack.
 * Market stock and personal chests are outside this route.
 */
public final class ConsolidateStep implements BlockWorkStep {
  private static final int SLOTS_BEFORE_TRIP = 18;
  private static final int TRANSFER_TICKS = 30;
  private static final int INSPECTION_TICKS = 60;
  private static final int INSPECTION_COOLDOWN_TICKS = 200;
  private static final int MOVES_PER_VISIT = 6;
  private static final int MAX_STALE_CHESTS = 8;
  private static final int PLAN_COOLDOWN_TICKS = 24000;

  private enum Phase { COLLECT, DELIVER, INSPECT }

  private Phase phase = Phase.INSPECT;
  private boolean preferredDelivery;
  private boolean returningToShelf;
  private boolean planning;
  private long nextPlanTick;
  private long openedAt;
  private long nextTransferTick;
  private int moves;
  private BlockPos approach;
  private ContainerVisit visit;
  private final Map<BlockPos, Long> failedUntil = new HashMap<>();
  private final Map<BlockPos, Long> inspectedAt = new HashMap<>();

  @Override
  @Nullable
  public BlockPos select(RealPerson person) {
    Village village = person.getVillage();
    if (village == null || !(person.level() instanceof ServerLevel)) return null;
    this.approach = null;
    this.moves = 0;
    long now = person.level().getGameTime();
    this.failedUntil.entrySet().removeIf(entry -> entry.getValue() <= now);

    int used = usedSlots(person);
    if (used == 0) this.returningToShelf = false;
    BlockPos delivery = used == 0 ? null : storehouseChest(person);
    BlockPos deliveryApproach = this.approach;
    if (used > 0 && delivery == null) {
      village.setStorageStrained(true);
      return inspectionChest(person);
    }
    BlockPos source = used >= SLOTS_BEFORE_TRIP || this.returningToShelf ? null : sourceChest(person, village);
    if (source != null) {
      this.phase = Phase.COLLECT;
      return source;
    }
    if (delivery != null) {
      this.phase = Phase.DELIVER;
      this.approach = deliveryApproach;
      return delivery;
    }
    village.setStorageStrained(false);
    return inspectionChest(person);
  }

  @Override
  public boolean act(RealPerson person, BlockPos target) {
    if (!inReach(person, target) || !(person.level().getBlockEntity(target) instanceof Container container)) return false;
    person.getLookControl().setLookAt(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D);
    long now = person.level().getGameTime();
    if (this.visit == null) {
      this.visit = ContainerVisit.open(person, target);
      this.openedAt = now;
      this.nextTransferTick = now + TRANSFER_TICKS;
      person.swing(InteractionHand.MAIN_HAND);
      return true;
    }
    if (now < this.nextTransferTick) return true;
    this.nextTransferTick = now + TRANSFER_TICKS;

    if (this.phase == Phase.COLLECT) {
      int moved = ShelfTransfers.collectOne(container, person.personMainInv);
      return moved > 0 && ++this.moves < MOVES_PER_VISIT && usedSlots(person) < SLOTS_BEFORE_TRIP;
    }
    if (this.phase == Phase.DELIVER) {
      int moved = ShelfTransfers.depositOne(person.personMainInv, container, shelfOffset(person, target),
          ShelvingPlan.load(person.getVillage()), this.preferredDelivery);
      if (moved > 0) {
        Kithkyn.LOGGER.debug("[resource-flow] {} (QUARTERMASTER) shelved {} item(s) at {}",
            person.getName().getString(), moved, target.toShortString());
      }
      if (person.personMainInv.isEmpty()) person.getVillage().setStorageStrained(false);
      return moved > 0 && ++this.moves < MOVES_PER_VISIT && !person.personMainInv.isEmpty();
    }
    if (this.moves++ == 0) {
      tidyVisitedShelf(person, target, container);
      planIfOutgrown(person);
      if (collectMisfiled(person, target, container)) {
        this.returningToShelf = true;
        return false;
      }
    }
    return now - this.openedAt < INSPECTION_TICKS;
  }

  @Override public String describe() { return "the storehouse containers"; }
  @Override public String activity() {
    return switch (this.phase) {
      case COLLECT -> "collecting goods from the village's workplace containers";
      case DELIVER -> "carrying goods to their storehouse shelves";
      case INSPECT -> "checking and organizing the storehouse containers";
    };
  }
  @Override public int actEveryTicks() { return 1; }
  @Override public boolean requiresUpdateEveryTick() { return true; }
  @Override public boolean swingsOnAct() { return false; }
  @Override public double reachSqr(RealPerson person) { return 6.0D; }
  @Override public BlockPos positionOf(BlockPos target) { return this.approach == null ? target : this.approach; }
  @Override public boolean requiresExactArrival() { return true; }

  @Override
  public boolean inReach(RealPerson person, BlockPos target) {
    boolean arrived = this.approach != null
        && person.position().distanceToSqr(Vec3.atBottomCenterOf(this.approach)) <= 0.36D
        && person.blockPosition().distSqr(target) <= reachSqr(person)
        && canReachContainer(person, person.getEyePosition(), target);
    if (!arrived && this.visit != null) closeVisit();
    return arrived;
  }

  @Override
  public void released(RealPerson person, BlockPos target) {
    closeVisit();
    if (this.phase == Phase.INSPECT) this.inspectedAt.put(target, person.level().getGameTime());
  }

  @Override
  public void unreachable(RealPerson person, BlockPos target) {
    this.failedUntil.put(target, person.level().getGameTime() + 1200);
  }

  private void closeVisit() {
    if (this.visit != null) this.visit.close();
    this.visit = null;
  }

  /** Supported ground beside the actual container, with an exact navigable endpoint. */
  @Nullable
  private BlockPos approachTo(RealPerson person, BlockPos target) {
    if (this.failedUntil.containsKey(target)) return null;
    List<BlockPos> candidates = new ArrayList<>();
    for (BlockPos pos : BlockPos.betweenClosed(target.offset(-2, -2, -2), target.offset(2, 2, 2))) {
      if (pos.distSqr(target) <= reachSqr(person) && WorkerFooting.canStand(person, pos)
          && canReachContainer(person, Vec3.atBottomCenterOf(pos).add(0.0D, person.getEyeHeight(), 0.0D), target)) {
        candidates.add(pos.immutable());
      }
    }
    candidates.sort(Comparator.<BlockPos>comparingDouble(pos -> pos.distSqr(target))
        .thenComparingDouble(pos -> pos.distSqr(person.blockPosition())));
    for (BlockPos candidate : candidates.stream().limit(12).toList()) {
      var path = person.getNavigation().createPath(candidate, 0);
      if (path != null && path.canReach() && path.getEndNode() != null
          && path.getEndNode().asBlockPos().equals(candidate)) return candidate;
    }
    this.failedUntil.put(target, person.level().getGameTime() + 200);
    return null;
  }

  /** A nearby wall does not turn an outdoor foothold into access to an indoor shelf. */
  private static boolean canReachContainer(RealPerson person, Vec3 eye, BlockPos target) {
    var hit = person.level().clip(new ClipContext(eye, Vec3.atCenterOf(target),
        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, person));
    return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(target);
  }

  /** Sources remain the village's shared workplace containers, excluding the market and storehouse. */
  @Nullable
  private BlockPos sourceChest(RealPerson person, Village village) {
    List<BlockPos> skip = new ArrayList<>(Storehouse.chests(person));
    skip.addAll(Treasury.chestPositions(village, (ServerLevel) person.level()));
    for (int attempt = 0; attempt < MAX_STALE_CHESTS; attempt++) {
      BlockPos found = village.getNearestContainer(person.blockPosition(), skip);
      if (found.equals(BlockPos.ZERO)) return null;
      Container container = containerAt(person, found);
      if (container != null && !container.isEmpty()) {
        this.approach = approachTo(person, found);
        if (this.approach != null) return found;
      }
      skip.add(found);
    }
    return null;
  }

  /** Prefer the actual shelf that owns carried goods; visit overflow shelves only when needed. */
  @Nullable
  private BlockPos storehouseChest(RealPerson person) {
    ShelvingPlan plan = ShelvingPlan.load(person.getVillage());
    for (boolean preferred : new boolean[] {true, false}) {
      int offset = 0;
      for (BlockPos pos : Storehouse.chests(person)) {
        Container shelf = containerAt(person, pos);
        if (shelf == null) continue;
        if (ShelfTransfers.canDeposit(person.personMainInv, shelf, offset, plan, preferred)) {
          this.approach = approachTo(person, pos);
          if (this.approach != null) {
            this.preferredDelivery = preferred;
            return pos;
          }
        }
        offset += shelf.getContainerSize();
      }
    }
    return null;
  }

  /** Quiet work rotates across real shelves instead of holding the movement goal at the job station. */
  @Nullable
  private BlockPos inspectionChest(RealPerson person) {
    this.phase = Phase.INSPECT;
    long now = person.level().getGameTime();
    List<BlockPos> shelves = new ArrayList<>(Storehouse.chests(person));
    shelves.sort(Comparator.comparingLong(pos -> this.inspectedAt.getOrDefault(pos, Long.MIN_VALUE)));
    for (BlockPos shelf : shelves) {
      Long visited = this.inspectedAt.get(shelf);
      if (visited != null && now - visited < INSPECTION_COOLDOWN_TICKS) continue;
      if (containerAt(person, shelf) == null) continue;
      this.approach = approachTo(person, shelf);
      if (this.approach != null) return shelf;
    }
    return null;
  }

  /** Applies the shared layout algorithm to just the shelf in arm's reach. */
  private static void tidyVisitedShelf(RealPerson person, BlockPos target, Container shelf) {
    ShelvingPlan plan = ShelvingPlan.load(person.getVillage());
    if (plan != null) {
      int offset = shelfOffset(person, target);
      List<ShelvingPlan.Category> categories = new ArrayList<>();
      for (ShelvingPlan.Category category : plan.categories()) {
        int first = Math.max(0, category.firstSlot() - offset);
        int end = Math.min(shelf.getContainerSize(), category.firstSlot() + category.slotCount() - offset);
        if (end > first) categories.add(new ShelvingPlan.Category(category.name(), category.itemIds(), first, end - first));
      }
      plan = new ShelvingPlan(categories, shelf.getContainerSize());
    }
    Storehouse.arrange(List.of(shelf), plan);
    shelf.setChanged();
  }

  /** Carry a misplaced stack only when its intended shelf can accept it or exchange another misplaced stack. */
  private boolean collectMisfiled(RealPerson person, BlockPos target, Container source) {
    ShelvingPlan plan = ShelvingPlan.load(person.getVillage());
    if (plan == null || !person.personMainInv.isEmpty()) return false;
    int sourceOffset = shelfOffset(person, target);
    for (int slot = 0; slot < source.getContainerSize(); slot++) {
      ItemStack stack = source.getItem(slot);
      if (stack.isEmpty() || plan.categoryFor(stack.getItem()) == null
          || ShelfTransfers.owns(plan, stack, sourceOffset + slot)) continue;
      Container carried = ShelfTransfers.copy(person.personMainInv);
      if (!HopperBlockEntity.addItem(null, carried, stack.copy(), null).isEmpty()) continue;
      int offset = 0;
      for (BlockPos other : Storehouse.chests(person)) {
        Container shelf = containerAt(person, other);
        if (shelf == null) continue;
        if (!other.equals(target) && ShelfTransfers.canDeposit(carried, shelf, offset, plan, true)
            && approachTo(person, other) != null) {
          return ShelfTransfers.collectSlot(source, slot, person.personMainInv) > 0;
        }
        offset += shelf.getContainerSize();
      }
    }
    return false;
  }

  @Nullable
  private static Container containerAt(RealPerson person, BlockPos pos) {
    return person.level().hasChunkAt(pos) && person.level().getBlockEntity(pos) instanceof Container container ? container : null;
  }

  private static int shelfOffset(RealPerson person, BlockPos target) {
    int offset = 0;
    for (BlockPos pos : Storehouse.chests(person)) {
      if (pos.equals(target)) break;
      Container shelf = containerAt(person, pos);
      if (shelf != null) offset += shelf.getContainerSize();
    }
    return offset;
  }

  private static int usedSlots(RealPerson person) {
    int used = 0;
    for (int slot = 0; slot < person.personMainInv.getContainerSize(); slot++) {
      if (!person.personMainInv.getItem(slot).isEmpty()) used++;
    }
    return used;
  }

  /** Explicit developer command only; ordinary work visits and mutates each shelf separately. */
  public static void tidyStorehouse(RealPerson person) {
    Storehouse.arrange(Storehouse.containers(person), person.getVillage() == null ? null : ShelvingPlan.load(person.getVillage()));
  }
  /**
   * Redraws the shelving plan when the shelves have outgrown it: no plan yet (the
   * first stocked storehouse), a storehouse of another size, or goods on the
   * shelves the plan never placed. The dialogue rides the LLM lane and never
   * blocks a tick; when it settles, the plan is stored and the next minding
   * pass shelves to it in person. One dialogue at a time, and one a day at most.
   */
  private void planIfOutgrown(RealPerson person) {
    Village village = person.getVillage();
    long now = person.level().getGameTime();
    if (village == null || this.planning || now < this.nextPlanTick || !LlmService.get().isReady()) {
      return;
    }
    Map<Item, Integer> shelved = Storehouse.snapshot(person);
    if (shelved.isEmpty()) {
      return; // bare shelves, or the storehouse is out of sight: nothing to plan around
    }
    ShelvingPlan plan = ShelvingPlan.load(village);
    int totalSlots = Storehouse.totalSlots(Storehouse.containers(person));
    if (plan != null && !outgrown(plan, shelved, totalSlots)) {
      return;
    }
    this.planning = true;
    this.nextPlanTick = now + PLAN_COOLDOWN_TICKS;
    QuartermasterPlanner.plan(person).whenComplete((outcome, failure) -> {
      MinecraftServer server = person.getServer();
      if (server == null) {
        return; // the server is gone; the goal goes with it
      }
      server.execute(() -> {
        this.planning = false;
        if (failure != null) {
          Kithkyn.LOGGER.warn("[quartermaster] {}'s shelving dialogue broke off: {}",
              person.getName().getString(), failure.toString());
          return;
        }
        Village home = person.getVillage();
        if (outcome.isEmpty() || home == null || !person.isAlive()) {
          return; // the planner has already said why no plan landed
        }
        QuartermasterPlanner.Outcome settled = outcome.get();
        ShelvingPlan.store(home, settled.plan());
        this.inspectedAt.clear(); // visit each shelf to put the new plan into practice
        Kithkyn.LOGGER.info("[quartermaster] {} adopted a shelving plan of {} categories: \"{}\"",
            person.getName().getString(), settled.plan().categories().size(), settled.note());
        for (ShelvingPlan.Category category : settled.plan().categories()) {
          Kithkyn.LOGGER.info("[quartermaster]   {} (slots {} to {}): {}", category.name(),
              category.firstSlot() + 1, category.firstSlot() + category.slotCount(),
              String.join(", ", category.itemIds()));
        }
      });
    });
  }

  /** Whether the shelves hold anything this plan has no place for, or have changed size under it. */
  private static boolean outgrown(ShelvingPlan plan, Map<Item, Integer> shelved, int totalSlots) {
    if (plan.totalSlots() != totalSlots) {
      return true;
    }
    for (Item item : shelved.keySet()) {
      if (!plan.covers(item)) {
        return true;
      }
    }
    return false;
  }

}
