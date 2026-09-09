package com.quzzar.kithkyn.dev;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.PersonEntityType;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.entities.Virtue;
import com.quzzar.kithkyn.entities.genetics.StatBlock;
import com.quzzar.kithkyn.village.Occupation;
import com.quzzar.kithkyn.village.buildings.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Opt-in duty, three-dimensional targeting, and connected-wall regression. Disposable worlds only. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class GuardCombatVerification {
  private static boolean ran;
  private static RealPerson shooter;
  private static Mob shootingTarget;
  private static net.minecraft.world.entity.ai.goal.Goal attack;
  private static int shootingTicks;
  private static boolean crossbow;
  private static int warmupTicks;

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (!Boolean.getBoolean("kithkyn.guardCombat.verify")) return;
    try {
      ServerLevel level = event.getServer().overworld();
      if (++warmupTicks == 1) {
        for (int x = 17; x <= 25; x++) for (int z = 11; z <= 14; z++) level.setChunkForced(x, z, true);
      }
      if (warmupTicks < 40) return;
      if (!ran) {
        ran = true;
        verify(level);
        beginShooting(level);
      }
      attack.tick();
      boolean fired = !level.getEntitiesOfClass(net.minecraft.world.entity.projectile.AbstractArrow.class,
          shooter.getBoundingBox().inflate(8), arrow -> arrow.getOwner() == shooter).isEmpty();
      if (fired) {
        Kithkyn.LOGGER.info("[guard-combat-verify] PASS {} fired at a hostile 32 blocks away and 12 below", crossbow ? "crossbow" : "bow");
        attack.stop();
        shooter.discard();
        shootingTarget.discard();
        if (!crossbow) { crossbow = true; beginShooting(level); }
        else {
          Kithkyn.LOGGER.info("[guard-combat-verify] RESULT PASS: guard duty, distant elevated acquisition and real bow/crossbow shots, sight/range limits, wall connections");
          event.getServer().halt(false);
        }
      }
      check(++shootingTicks < 400, "Ranged guard never fired: entity ticks=" + shooter.tickCount
          + ", using item=" + shooter.isUsingItem() + ", use ticks=" + shooter.getTicksUsingItem());
    } catch (Exception | AssertionError failure) {
      Kithkyn.LOGGER.error("[guard-combat-verify] RESULT FAIL", failure);
      event.getServer().halt(false);
    }
  }

  private static void beginShooting(ServerLevel level) {
    shootingTicks = 0;
    shooter = new RealPerson(PersonEntityType.PERSON.get(), level) {
      @Override public boolean isFixedRangedGuard() { return true; }
    };
    shooter.setOccupation(Occupation.GUARD);
    shooter.setStatBlock(StatBlock.roll(new java.util.Random(7)));
    shooter.setNoAi(true);
    shooter.setNoGravity(true);
    shooter.moveTo(300.5, 180, 200.5, 0, 0);
    shooter.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(crossbow ? Items.CROSSBOW : Items.BOW));
    level.addFreshEntity(shooter);
    shootingTarget = EntityType.PILLAGER.create(level);
    shootingTarget.setNoAi(true);
    shootingTarget.setNoGravity(true);
    shootingTarget.moveTo(332.5, 168, 200.5, 0, 0);
    level.addFreshEntity(shootingTarget);
    shooter.setTarget(shootingTarget);
    attack = crossbow ? new com.quzzar.kithkyn.entities.ai.goals.RangedCrossbowAttackPassiveGoal<>(shooter, 1, 8)
        : new com.quzzar.kithkyn.entities.ai.goals.RangedBowAttackPassiveGoal<>(shooter, 0.5, 20, 15);
    check(attack.canUse(), "Ranged combat did not supersede the post");
    attack.start();
  }

  private static void verify(ServerLevel level) throws ReflectiveOperationException {
    for (int x = 17; x <= 25; x++) for (int z = 11; z <= 14; z++) level.getChunk(x, z);
    RealPerson guard = PersonEntityType.PERSON.get().create(level);
    guard.setStatBlock(StatBlock.roll(new java.util.Random(7)));
    guard.setOccupation(Occupation.GUARD);
    var virtue = RealPerson.class.getDeclaredMethod("setVirtue", Virtue.class, float.class);
    virtue.setAccessible(true);
    virtue.invoke(guard, Virtue.AGGRESSION, -0.5F);
    virtue.invoke(guard, Virtue.PROTECT_SELF, -0.5F);
    guard.reloadState();
    check(guard.doesCombat() && guard.willInitiateCombat(), "Gentle guard received civilian fleeing behavior");
    guard.setNoAi(true);
    guard.setNoGravity(true);
    guard.moveTo(300.5, 180, 200.5, 0, 0);
    level.addFreshEntity(guard);
    Mob enemy = EntityType.PILLAGER.create(level);
    enemy.setNoAi(true);
    enemy.setNoGravity(true);
    enemy.moveTo(332.5, 168, 200.5, 0, 0);
    level.addFreshEntity(enemy);
    for (var weapon : java.util.List.of(Items.BOW, Items.CROSSBOW)) {
      guard.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(weapon));
      guard.reloadState();
      for (int distance : new int[] {12, 32, 80}) {
        enemy.moveTo(300.5 + distance, 168, 200.5, 0, 0);
        check(acquires(guard, enemy) == (distance < 80), "Wrong elevated targeting at " + distance + " with " + weapon);
      }
      enemy.moveTo(332.5, 168, 200.5, 0, 0);
      for (BlockPos p : BlockPos.betweenClosed(308, 166, 198, 308, 184, 202)) level.setBlock(p, Blocks.STONE.defaultBlockState(), 2);
      guard.getSensing().tick();
      check(!acquires(guard, enemy), "Guard saw a hostile through solid stone");
      for (BlockPos p : BlockPos.betweenClosed(308, 166, 198, 308, 184, 202)) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
      guard.getSensing().tick();
    }
    guard.discard();
    enemy.discard();
    BlockPos rail = new BlockPos(320, 150, 200);
    level.setBlock(rail.below(), Blocks.STONE.defaultBlockState(), 2);
    level.setBlock(rail.east(), Blocks.COBBLESTONE.defaultBlockState(), 2);
    WallRaiser.place(level, new WallBlockPlan(rail.asLong(), WallBlockPlan.Piece.COBBLE_WALL, WallCellRole.EXACT),
        WallTier.WOOD, VillageStyle.BIRCH_FOREST);
    var state = level.getBlockState(rail);
    check(state.equals(Block.updateFromNeighbourShapes(state, level, rail)), "Placed wall failed to connect to its existing deck");
  }

  private static boolean acquires(RealPerson guard, Mob enemy) throws ReflectiveOperationException {
    var selector = Mob.class.getDeclaredField("targetSelector");
    selector.setAccessible(true);
    var goals = ((GoalSelector) selector.get(guard)).getAvailableGoals();
    guard.setTarget(null);
    guard.getRandom().setSeed(1);
    for (int attempt = 0; attempt < 100; attempt++) {
      for (var wrapped : goals) {
        if (!(wrapped.getGoal() instanceof NearestAttackableTargetGoal<?> goal) || !goal.canUse()) continue;
        goal.start();
        boolean selected = guard.getTarget() == enemy;
        goal.stop();
        if (selected) return true;
      }
    }
    return false;
  }

  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
}
