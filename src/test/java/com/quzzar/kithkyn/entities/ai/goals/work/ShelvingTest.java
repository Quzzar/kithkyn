package com.quzzar.kithkyn.entities.ai.goals.work;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.quzzar.kithkyn.village.ShelvingPlan;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The shelf arithmetic both keepers share, exercised with a one-slot pack like an allay's. */
class ShelvingTest {
  private static final BlockPos A = new BlockPos(0, 0, 0);
  private static final BlockPos B = new BlockPos(1, 0, 0);
  private static final BlockPos C = new BlockPos(2, 0, 0);

  @Test
  void shelfOffsetCountsOnlyLoadedShelvesBeforeTheTarget() {
    SimpleContainer first = new SimpleContainer(9);
    SimpleContainer third = new SimpleContainer(27);
    Map<BlockPos, Container> loaded = Map.of(A, first, C, third);

    assertEquals(0, Shelving.shelfOffset(List.of(A, B, C), loaded::get, A));
    assertEquals(9, Shelving.shelfOffset(List.of(A, B, C), loaded::get, C), "the unloaded middle shelf adds nothing");
  }

  @Test
  void misfiledStackMovesIntoAnEmptyPackWhenItsOwnShelfCanTakeIt() {
    SimpleContainer wrongShelf = new SimpleContainer(3);
    SimpleContainer rightShelf = new SimpleContainer(3);
    wrongShelf.setItem(0, new ItemStack(Items.WHEAT, 10));
    ShelvingPlan plan = new ShelvingPlan(List.of(
        new ShelvingPlan.Category("stone", List.of("minecraft:cobblestone"), 0, 3),
        new ShelvingPlan.Category("grain", List.of("minecraft:wheat"), 3, 3)), 6);
    Map<BlockPos, Container> loaded = Map.of(A, wrongShelf, B, rightShelf);
    SimpleContainer pack = new SimpleContainer(1);

    boolean carried = Shelving.collectMisfiled(plan, List.of(A, B), loaded::get, pos -> true, pack, A, wrongShelf);

    assertTrue(carried);
    assertEquals(10, pack.getItem(0).getCount());
    assertTrue(wrongShelf.isEmpty());
  }

  @Test
  void misfiledStackStaysWhenThePackIsBusyOrTheRightShelfIsUnreachable() {
    SimpleContainer wrongShelf = new SimpleContainer(3);
    SimpleContainer rightShelf = new SimpleContainer(3);
    wrongShelf.setItem(0, new ItemStack(Items.WHEAT, 10));
    ShelvingPlan plan = new ShelvingPlan(List.of(
        new ShelvingPlan.Category("stone", List.of("minecraft:cobblestone"), 0, 3),
        new ShelvingPlan.Category("grain", List.of("minecraft:wheat"), 3, 3)), 6);
    Map<BlockPos, Container> loaded = Map.of(A, wrongShelf, B, rightShelf);

    SimpleContainer busy = new SimpleContainer(1);
    busy.setItem(0, new ItemStack(Items.COBBLESTONE, 4));
    assertFalse(Shelving.collectMisfiled(plan, List.of(A, B), loaded::get, pos -> true, busy, A, wrongShelf));
    assertFalse(Shelving.collectMisfiled(plan, List.of(A, B), loaded::get, pos -> !pos.equals(B), new SimpleContainer(1), A, wrongShelf));
    assertEquals(10, wrongShelf.getItem(0).getCount());
  }
}
