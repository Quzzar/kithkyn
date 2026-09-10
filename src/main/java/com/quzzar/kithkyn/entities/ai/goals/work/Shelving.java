package com.quzzar.kithkyn.entities.ai.goals.work;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.quzzar.kithkyn.village.ShelvingPlan;
import com.quzzar.kithkyn.village.Storehouse;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

/**
 * The shelf arithmetic shared by every keeper of the stores: the human
 * quartermaster's {@link ConsolidateStep} and the adopted allay's
 * {@code AllayQuartermasterBehavior}. Nothing here knows who is carrying the
 * pack or how they got to the shelf; callers supply the shelves, a way to see
 * each one, and a way to tell whether one can be reached.
 */
final class Shelving {
  private Shelving() { }

  /** Global slot number of the first slot of this shelf, counting loaded shelves in storehouse order. */
  static int shelfOffset(List<BlockPos> chests, Function<BlockPos, Container> at, BlockPos target) {
    int offset = 0;
    for (BlockPos pos : chests) {
      if (pos.equals(target)) break;
      Container shelf = at.apply(pos);
      if (shelf != null) offset += shelf.getContainerSize();
    }
    return offset;
  }

  /** Applies the shared layout algorithm to just the shelf in arm's reach. */
  static void tidyVisitedShelf(@Nullable ShelvingPlan plan, int offset, Container shelf) {
    if (plan != null) {
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

  /**
   * Carry a misplaced stack out of this shelf only when its intended shelf can
   * accept it or exchange another misplaced stack, and only into an empty pack.
   */
  static boolean collectMisfiled(@Nullable ShelvingPlan plan, List<BlockPos> chests, Function<BlockPos, Container> at,
      Predicate<BlockPos> reachable, Container pack, BlockPos target, Container source) {
    if (plan == null || !pack.isEmpty()) return false;
    int sourceOffset = shelfOffset(chests, at, target);
    for (int slot = 0; slot < source.getContainerSize(); slot++) {
      ItemStack stack = source.getItem(slot);
      if (stack.isEmpty() || plan.categoryFor(stack.getItem()) == null
          || ShelfTransfers.owns(plan, stack, sourceOffset + slot)) continue;
      Container carried = ShelfTransfers.copy(pack);
      if (!HopperBlockEntity.addItem(null, carried, stack.copy(), null).isEmpty()) continue;
      int offset = 0;
      for (BlockPos other : chests) {
        Container shelf = at.apply(other);
        if (shelf == null) continue;
        if (!other.equals(target) && ShelfTransfers.canDeposit(carried, shelf, offset, plan, true)
            && reachable.test(other)) {
          return ShelfTransfers.collectSlot(source, slot, pack) > 0;
        }
        offset += shelf.getContainerSize();
      }
    }
    return false;
  }
}
