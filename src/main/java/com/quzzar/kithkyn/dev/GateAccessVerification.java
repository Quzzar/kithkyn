package com.quzzar.kithkyn.dev;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.PersonEntityType;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.village.Occupation;
import com.quzzar.kithkyn.village.buildings.*;
import com.quzzar.kithkyn.savedata.PlacedBlockStore;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Opt-in physical ascent/descent regression. Enable only in a disposable test world. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class GateAccessVerification {
  private static int ticks;
  private static boolean descending;
  private static int phaseStarted;
  private static final List<Walker> walkers = new ArrayList<>();
  private record Walker(RealPerson person, BlockPos bottom, BlockPos top, String label) { }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (!Boolean.getBoolean("kithkyn.gateAccess.verify")) return;
    ServerLevel level = event.getServer().overworld();
    try {
      if (++ticks == 1) prepare(level);
      boolean all = true;
      for (Walker walker : walkers) {
        BlockPos target = descending ? walker.bottom() : walker.top();
        RealPerson person = walker.person();
        boolean arrived = person.distanceToSqr(target.getX() + 0.5, target.getY(), target.getZ() + 0.5) < 0.36;
        all &= arrived;
        if (!arrived && (ticks % 40 == 1 || person.getNavigation().isDone())) {
          var path = person.getNavigation().createPath(target, 0);
          if (path != null && path.canReach()) person.getNavigation().moveTo(path, 0.6);
        }
        if (ticks % 100 == 0 && !arrived) Kithkyn.LOGGER.info(
            "[gate-access-verify] {} {} tick={} pos={} target={} next={}", walker.label(), descending ? "down" : "up",
            ticks, person.position(), target, person.getNavigation().isDone() ? null : person.getNavigation().getPath().getNextNodePos());
      }
      if (all && ticks > 1) {
        if (!descending) {
          Kithkyn.LOGGER.info("[gate-access-verify] PASS all {} ascents", walkers.size());
          descending = true;
          phaseStarted = ticks;
          for (Walker walker : walkers) walker.person().getNavigation().stop();
        } else {
          Kithkyn.LOGGER.info("[gate-access-verify] RESULT PASS: all {} guards/workers ascended and descended", walkers.size());
          event.getServer().halt(false);
        }
      }
      if (ticks - phaseStarted >= 800) throw new AssertionError("Residents failed to " + (descending ? "descend" : "ascend") + " the authored ladders");
    } catch (Exception | AssertionError failure) {
      Kithkyn.LOGGER.error("[gate-access-verify] RESULT FAIL", failure);
      event.getServer().halt(false);
    }
  }

  private static void prepare(ServerLevel level) throws ReflectiveOperationException {
    for (int x = 7; x <= 13; x++) for (int z = 7; z <= 13; z++) level.setChunkForced(x, z, true);
    for (BlockPos p : BlockPos.betweenClosed(118, 149, 118, 202, 163, 202)) {
      level.setBlock(p, p.getY() == 150 ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
    }
    List<Long> ring = WallRoute.aroundBox(128, 192, 128, 192);
    List<Integer> ground = Collections.nCopies(ring.size(), 151);
    Set<Long> gates = Set.of(BlockPos.asLong(160, 0, 128), BlockPos.asLong(192, 0, 160),
        BlockPos.asLong(160, 0, 192), BlockPos.asLong(128, 0, 160));
    WallProject wall = WallProject.completed(ring, gates, ground,
        WallRaiser.deckProfile(level, ring, ground, WallTier.WOOD.height()), WallTier.WOOD, VillageStyle.BIRCH_FOREST, Set.of());
    for (WallBlockPlan cell : wall.plannedBlocks()) {
      level.setBlock(cell.pos(), cell.desiredState(wall.getTier(), wall.getStyle()), 2);
      PlacedBlockStore.get(level).markVillagePlaced(cell.pos());
    }
    for (WallBlockPlan cell : wall.plannedBlocks()) {
      var state = level.getBlockState(cell.pos());
      level.setBlock(cell.pos(), Block.updateFromNeighbourShapes(state, level, cell.pos()), 3);
    }
    // Natural-terrain sampling skips village-owned floors and would resolve
    // these stations far below the platform instead of using the saved ground.
    for (WallPost post : WallPosts.plan(wall)) {
      if (post.duty() != WallPost.Duty.GATE_SWORD_PRIMARY) continue;
      BlockPos floor = post.position().below();
      level.setBlock(floor, Blocks.COBBLESTONE.defaultBlockState(), 3);
      PlacedBlockStore.get(level).markVillagePlaced(floor);
    }
    for (WallPost post : WallPosts.plan(wall, level)) {
      if (post.duty() != WallPost.Duty.GATE_SWORD_PRIMARY) continue;
      check(post.position().getY() == 151 && WorkerFooting.canStand(level, post.position()),
          "Gate post dropped below its constructed floor: " + post.position());
    }
    int index = 0;
    for (WallPost post : WallPosts.plan(wall, level)) {
      if (!post.duty().usesCrossbow()) continue;
      RealPerson person = PersonEntityType.PERSON.get().create(level);
      person.setStatBlock(com.quzzar.kithkyn.entities.genetics.StatBlock.roll(new java.util.Random(index + 1)));
      person.setOccupation(Occupation.LUMBERJACK);
      person.setPersistenceRequired();
      for (String name : List.of("goalSelector", "targetSelector")) {
        var field = Mob.class.getDeclaredField(name);
        field.setAccessible(true);
        ((GoalSelector) field.get(person)).removeAllGoals(goal -> true);
      }
      BlockPos top = post.position();
      check(WorkerFooting.canStand(person, top), "Unsupported/blocked post " + top);
      BlockPos rung = wall.plannedBlocks().stream().filter(cell -> cell.piece().name().startsWith("LADDER_"))
          .map(WallBlockPlan::pos).min(Comparator.comparingDouble((BlockPos p) ->
              Math.pow(p.getX() - top.getX(), 2) + Math.pow(p.getZ() - top.getZ(), 2))
              .thenComparingInt(BlockPos::getY)).orElseThrow();
      while (level.getBlockState(rung.below()).is(net.minecraft.tags.BlockTags.CLIMBABLE)) rung = rung.below();
      BlockPos bottom = rung.relative(level.getBlockState(rung).getValue(net.minecraft.world.level.block.LadderBlock.FACING));
      if (post.duty() == WallPost.Duty.GATE_CROSSBOW) {
        // Cover an entrance one block above its approach, including the final
        // step down out of the lowest rung when returning from the platform.
        level.setBlock(bottom.below(), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(bottom.below(2), Blocks.STONE.defaultBlockState(), 3);
        bottom = bottom.below();
      }
      check(WorkerFooting.canStand(person.level(), bottom), "No front approach " + rung);
      person.moveTo(top.getX() + 0.5, top.getY(), top.getZ() + 0.5, 0, 0);
      person.setOnGround(true);
      var descent = person.getNavigation().createPath(new BlockPos(160, 151, 160), 0);
      check(descent != null, "No route away from " + top);
      for (int n = 1; n < descent.getNodeCount(); n++) {
        var previous = descent.getNode(n - 1);
        if (level.getBlockState(previous.asBlockPos()).is(net.minecraft.tags.BlockTags.CLIMBABLE)) {
          check(descent.getNode(n).y >= previous.y - 1, "Route jumps sideways off a high rung at " + previous);
        }
      }
      person.moveTo(bottom.getX() + 0.5, bottom.getY(), bottom.getZ() + 0.5, 0, 0);
      person.setOnGround(true);
      level.addFreshEntity(person);
      var path = person.getNavigation().createPath(top, 0);
      check(path != null && path.canReach() && path.getEndNode().asBlockPos().equals(top), "No ascent path " + bottom + " -> " + top);
      walkers.add(new Walker(person, bottom, top, post.duty() + "-" + index++));
    }
  }

  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
}
