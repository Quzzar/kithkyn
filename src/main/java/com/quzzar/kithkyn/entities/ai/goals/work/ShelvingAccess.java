package com.quzzar.kithkyn.entities.ai.goals.work;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.quzzar.kithkyn.village.ShelvingPlan;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;

/**
 * The package's shelf operations opened to keepers that live outside it. The
 * human loop keeps using the package-private originals; this is the one door
 * through which another keeper reaches the same arithmetic.
 */
public final class ShelvingAccess {
  private ShelvingAccess() { }

  public static int collectOne(Container source, Container pack) {
    return ShelfTransfers.collectOne(source, pack);
  }

  public static int depositOne(Container pack, Container shelf, int offset, @Nullable ShelvingPlan plan, boolean preferred) {
    return ShelfTransfers.depositOne(pack, shelf, offset, plan, preferred);
  }

  public static boolean canDeposit(Container pack, Container shelf, int offset, @Nullable ShelvingPlan plan, boolean preferred) {
    return ShelfTransfers.canDeposit(pack, shelf, offset, plan, preferred);
  }

  public static int shelfOffset(List<BlockPos> chests, Function<BlockPos, Container> at, BlockPos target) {
    return Shelving.shelfOffset(chests, at, target);
  }

  public static void tidyVisitedShelf(@Nullable ShelvingPlan plan, int offset, Container shelf) {
    Shelving.tidyVisitedShelf(plan, offset, shelf);
  }

  public static boolean collectMisfiled(@Nullable ShelvingPlan plan, List<BlockPos> chests, Function<BlockPos, Container> at,
      Predicate<BlockPos> reachable, Container pack, BlockPos target, Container source) {
    return Shelving.collectMisfiled(plan, chests, at, reachable, pack, target, source);
  }
}
