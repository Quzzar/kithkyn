package com.quzzar.kithkyn.entities.ai.goals.work;

import javax.annotation.Nullable;

import com.quzzar.kithkyn.village.ShelvingPlan;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

/** One physical inventory move at a time, including exchanges between full, misfiled shelves. */
final class ShelfTransfers {
  private ShelfTransfers() { }

  /** Reads capacity without mutating either inventory. Preferred moves may displace a misfiled stack. */
  static boolean canDeposit(Container pack, Container shelf, int offset, @Nullable ShelvingPlan plan,
      boolean preferred) {
    return nextDeposit(pack, shelf, offset, plan, preferred) != null;
  }

  /** Transfers one carried stack into this shelf only, keeping displaced goods in the real pack. */
  static int depositOne(Container pack, Container shelf, int offset, @Nullable ShelvingPlan plan,
      boolean preferred) {
    Move move = nextDeposit(pack, shelf, offset, plan, preferred);
    if (move == null) return 0;
    ItemStack arriving = pack.removeItem(move.from(), move.count());
    if (move.exchange()) {
      ItemStack displaced = shelf.removeItem(move.to(), shelf.getItem(move.to()).getCount());
      shelf.setItem(move.to(), arriving);
      // The exact post-withdrawal pack was checked before either real inventory changed.
      HopperBlockEntity.addItem(shelf, pack, displaced, null);
    } else {
      ItemStack existing = shelf.getItem(move.to());
      if (existing.isEmpty()) shelf.setItem(move.to(), arriving);
      else existing.grow(arriving.getCount());
    }
    pack.setChanged();
    shelf.setChanged();
    return move.count();
  }

  /** Takes at most one source stack, retaining any part the pack cannot hold. */
  static int collectOne(Container source, Container pack) {
    for (int slot = 0; slot < source.getContainerSize(); slot++) {
      int moved = collectSlot(source, slot, pack);
      if (moved > 0) return moved;
    }
    return 0;
  }

  static int collectSlot(Container source, int slot, Container pack) {
    ItemStack offered = source.getItem(slot);
    if (offered.isEmpty()) return 0;
    int before = offered.getCount();
    ItemStack leftover = HopperBlockEntity.addItem(source, pack, offered.copy(), null);
    int moved = before - leftover.getCount();
    if (moved > 0) {
      source.setItem(slot, leftover);
      source.setChanged();
    }
    return moved;
  }

  /** A temporary capacity probe, never used as an inventory write path. */
  static SimpleContainer copy(Container source) {
    SimpleContainer copy = new SimpleContainer(source.getContainerSize());
    for (int slot = 0; slot < source.getContainerSize(); slot++) {
      copy.setItem(slot, source.getItem(slot).copy());
    }
    return copy;
  }

  static boolean owns(@Nullable ShelvingPlan plan, ItemStack stack, int globalSlot) {
    ShelvingPlan.Category category = plan == null || stack.isEmpty() ? null : plan.categoryFor(stack.getItem());
    return category != null && globalSlot >= category.firstSlot()
        && globalSlot < category.firstSlot() + category.slotCount();
  }

  @Nullable
  private static Move nextDeposit(Container pack, Container shelf, int offset, @Nullable ShelvingPlan plan,
      boolean preferred) {
    for (int from = 0; from < pack.getContainerSize(); from++) {
      ItemStack offered = pack.getItem(from);
      if (offered.isEmpty()) continue;
      // Merge before using an empty slot so partial stacks stay compact.
      for (int pass = 0; pass < 3; pass++) {
        for (int to = 0; to < shelf.getContainerSize(); to++) {
          if (!shelf.canPlaceItem(to, offered) || (preferred && !owns(plan, offered, offset + to))) continue;
          ItemStack existing = shelf.getItem(to);
          int capacity = Math.min(offered.getMaxStackSize(), shelf.getMaxStackSize());
          boolean matches = !existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, offered);
          if (pass == 0 && matches && existing.getCount() < capacity) {
            return new Move(from, to, Math.min(offered.getCount(), capacity - existing.getCount()), false);
          }
          if (pass == 1 && existing.isEmpty()) {
            return new Move(from, to, Math.min(offered.getCount(), capacity), false);
          }
          if (pass == 2 && preferred && !existing.isEmpty() && !matches
              && !owns(plan, existing, offset + to)) {
            int count = Math.min(offered.getCount(), capacity);
            SimpleContainer after = copy(pack);
            after.removeItem(from, count);
            if (HopperBlockEntity.addItem(null, after, existing.copy(), null).isEmpty()) {
              return new Move(from, to, count, true);
            }
          }
        }
      }
    }
    return null;
  }

  private record Move(int from, int to, int count, boolean exchange) { }
}
