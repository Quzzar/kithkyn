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
  private static long arrowSeed;

  /** Pin projectile spread before launch so the aiming regression is repeatable. */
  @SubscribeEvent
  public static void seedArrow(net.neoforged.neoforge.event.entity.EntityEvent.EntityConstructing event) {
    if (Boolean.getBoolean("kithkyn.guardAim.verify")
        && event.getEntity() instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow) {
      arrow.getRandom().setSeed(arrowSeed);
    }
  }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (!Boolean.getBoolean("kithkyn.guardCombat.verify") && !Boolean.getBoolean("kithkyn.guardAim.verify")) return;
    try {
      ServerLevel level = event.getServer().overworld();
      if (++warmupTicks == 1) {
        for (int x = 17; x <= 25; x++) for (int z = 11; z <= 14; z++) level.setChunkForced(x, z, true);
      }
      if (warmupTicks < 40) return;
      if (!ran) {
        ran = true;
        if (Boolean.getBoolean("kithkyn.guardAim.verify")) {
          verifyAim(level);
          event.getServer().halt(false);
          return;
        }
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

  /** Follow real projectiles through real collision and damage, rather than accepting a launch. */
  private static void verifyAim(ServerLevel level) {
    level.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
    RealPerson archer = PersonEntityType.PERSON.get().create(level);
    archer.setNoAi(true);
    archer.setNoGravity(true);
    archer.setOccupation(Occupation.GUARD);
    archer.moveTo(300.5, 180, 200.5, 0, 0);
    level.addFreshEntity(archer);
    boolean passed = true;
    for (var weapon : java.util.List.of(Items.BOW, Items.CROSSBOW)) {
      for (int distance : new int[] {12, 24, 40, 48}) {
        for (int elevation : new int[] {0, -12, 8}) {
          int hits = 0;
          double verticalError = 0;
          int crossings = 0;
          for (int sample = 0; sample < 32; sample++) {
            Mob target = EntityType.PILLAGER.create(level);
            target.setNoAi(true);
            target.setNoGravity(true);
            target.moveTo(300.5 + distance, 180 + elevation, 200.5, 0, 0);
            level.addFreshEntity(target);
            ItemStack held = new ItemStack(weapon);
            if (weapon == Items.CROSSBOW) held.set(net.minecraft.core.component.DataComponents.CHARGED_PROJECTILES,
                net.minecraft.world.item.component.ChargedProjectiles.of(new ItemStack(Items.ARROW)));
            archer.setItemSlot(EquipmentSlot.MAINHAND, held);
            archer.setTarget(target);
            archer.setXRot(17);
            archer.setYHeadRot(63);
            arrowSeed = sample;
            archer.getRandom().setSeed(sample);
            archer.performRangedAttack(target, 1);
            check(archer.getXRot() == 17 && archer.getYHeadRot() == 63 && archer.getTarget() == target,
                "Firing changed the guard's look or combat target");
            check(held.getDamageValue() == 1, "Firing did not apply weapon wear");
            var arrows = level.getEntitiesOfClass(net.minecraft.world.entity.projectile.AbstractArrow.class,
                archer.getBoundingBox().inflate(2), arrow -> arrow.getOwner() == archer);
            check(arrows.size() == 1, "Expected exactly one launched arrow");
            var arrow = arrows.getFirst();
            float health = target.getHealth();
            for (int tick = 0; tick < 100 && !arrow.isRemoved(); tick++) {
              var before = arrow.position();
              arrow.tick();
              if (before.x <= target.getX() && arrow.getX() >= target.getX()) {
                double fraction = (target.getX() - before.x) / (arrow.getX() - before.x);
                verticalError += before.y + (arrow.getY() - before.y) * fraction
                    - target.getBoundingBox().getCenter().y;
                crossings++;
              }
              if (target.getHealth() < health) { hits++; break; }
              if (arrow.getX() > target.getX() + 2 || arrow.getY() < 130) break;
            }
            arrow.discard();
            target.discard();
          }
          Kithkyn.LOGGER.info("[guard-aim-verify] {} distance={} elevation={} hits={}/32 verticalError={}",
              weapon, distance, elevation, hits, crossings == 0 ? "no crossing" : verticalError / crossings);
          passed &= hits >= 24;
        }
      }
    }
    verifyCrossbowComponents(level, archer);
    verifyMovingTargets(level, archer);
    verifyBowDamage(level, archer);
    archer.discard();
    check(passed, "Ranged accuracy below 75% against stationary targets");
    Kithkyn.LOGGER.info("[guard-aim-verify] RESULT PASS: real bow/crossbow hits at 12-48 blocks, above and below the target");
  }

  /** Faster combat flight must not become a second, unintended damage buff. */
  private static void verifyBowDamage(ServerLevel level, RealPerson archer) {
    double[] damage = new double[2];
    for (int mode = 0; mode < 2; mode++) {
      for (int sample = 0; sample < 32; sample++) {
        Mob target = EntityType.PILLAGER.create(level);
        target.setNoAi(true);
        target.setNoGravity(true);
        target.moveTo(304.5, 180, 200.5, 0, 0);
        level.addFreshEntity(target);
        archer.setTarget(target);
        archer.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        archer.getRandom().setSeed(sample);
        arrowSeed = sample;
        if (mode == 0) archer.shootArrowAt(target, 1, 1);
        else archer.performRangedAttack(target, 1);
        var arrow = level.getEntitiesOfClass(net.minecraft.world.entity.projectile.AbstractArrow.class,
            archer.getBoundingBox().inflate(2), shot -> shot.getOwner() == archer).getFirst();
        float health = target.getHealth();
        for (int tick = 0; tick < 10 && !arrow.isRemoved(); tick++) arrow.tick();
        damage[mode] += health - target.getHealth();
        arrow.discard();
        target.discard();
      }
    }
    check(damage[0] > 0 && Math.abs(damage[1] - damage[0]) / 32 < 1,
        "Combat bow damage changed materially: hunting=" + damage[0] / 32 + ", combat=" + damage[1] / 32);
    Kithkyn.LOGGER.info("[guard-aim-verify] PASS close bow damage: previous-speed={} combat={}", damage[0] / 32, damage[1] / 32);
  }

  /** The actual held item must retain special bolts, multishot, rockets, and durability. */
  private static void verifyCrossbowComponents(ServerLevel level, RealPerson archer) {
    Mob target = EntityType.PILLAGER.create(level);
    target.moveTo(276.5, 168, 224.5, 0, 0);
    level.addFreshEntity(target);
    archer.setTarget(target);
    for (int mode = 0; mode < 3; mode++) {
      ItemStack weapon = new ItemStack(Items.CROSSBOW);
      var ammo = mode == 2 ? Items.FIREWORK_ROCKET : Items.SPECTRAL_ARROW;
      int count = mode == 1 ? 3 : 1;
      if (mode == 1) weapon.enchant(level.registryAccess()
          .lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
          .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.MULTISHOT), 1);
      weapon.set(net.minecraft.core.component.DataComponents.CHARGED_PROJECTILES,
          net.minecraft.world.item.component.ChargedProjectiles.of(
              java.util.stream.IntStream.range(0, count).mapToObj(index -> new ItemStack(ammo)).toList()));
      archer.setItemSlot(EquipmentSlot.MAINHAND, weapon);
      archer.performRangedAttack(target, 1);
      var shots = level.getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class,
          archer.getBoundingBox().inflate(2), shot -> shot.getOwner() == archer);
      check(shots.size() == count, "Lost loaded projectiles, mode=" + mode);
      check(!net.minecraft.world.item.CrossbowItem.isCharged(weapon), "Loaded projectiles were not consumed");
      check(weapon.getDamageValue() == (mode == 2 ? 3 : count), "Incorrect crossbow durability, mode=" + mode);
      for (var shot : shots) {
        check(mode == 2 ? shot instanceof net.minecraft.world.entity.projectile.FireworkRocketEntity
            : shot instanceof net.minecraft.world.entity.projectile.SpectralArrow, "Lost special ammunition");
      }
      if (mode == 1) check(shots.get(1).getDeltaMovement().normalize().distanceTo(shots.get(2).getDeltaMovement().normalize()) > 0.2,
          "Multishot lost its spread");
      for (var shot : shots) shot.discard();
    }
    target.discard();
    Kithkyn.LOGGER.info("[guard-aim-verify] PASS special bolts, multishot, rockets, loaded ammunition and wear");
  }

  /** Exercise leading through the real firing adapter and collision path in diagonal directions. */
  private static void verifyMovingTargets(ServerLevel level, RealPerson archer) {
    for (var weapon : java.util.List.of(Items.BOW, Items.CROSSBOW)) {
      int hits = 0;
      for (int sample = 0; sample < 32; sample++) {
        Mob target = EntityType.PILLAGER.create(level);
        target.setNoAi(true);
        target.setNoGravity(true);
        target.moveTo(276.5, 168, sample % 2 == 0 ? 224.5 : 176.5, 0, 0);
        var motion = new net.minecraft.world.phys.Vec3(0, 0, 0.2);
        target.setDeltaMovement(motion);
        level.addFreshEntity(target);
        ItemStack held = new ItemStack(weapon);
        if (weapon == Items.CROSSBOW) held.set(net.minecraft.core.component.DataComponents.CHARGED_PROJECTILES,
            net.minecraft.world.item.component.ChargedProjectiles.of(new ItemStack(Items.ARROW)));
        archer.setItemSlot(EquipmentSlot.MAINHAND, held);
        archer.setTarget(target);
        arrowSeed = sample;
        archer.getRandom().setSeed(sample);
        archer.performRangedAttack(target, 1);
        var arrow = level.getEntitiesOfClass(net.minecraft.world.entity.projectile.AbstractArrow.class,
            archer.getBoundingBox().inflate(2), shot -> shot.getOwner() == archer).getFirst();
        float health = target.getHealth();
        for (int tick = 0; tick < 50 && !arrow.isRemoved(); tick++) {
          target.setPos(target.position().add(motion));
          arrow.tick();
          if (target.getHealth() < health) { hits++; break; }
        }
        arrow.discard();
        target.discard();
      }
      Kithkyn.LOGGER.info("[guard-aim-verify] {} moving diagonal target hits={}/32", weapon, hits);
      check(hits >= 24, "Failed to lead moving targets with " + weapon);
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
