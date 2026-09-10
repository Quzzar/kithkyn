package com.quzzar.kithkyn.dev;

import java.util.Collection;
import java.util.List;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.PersonEntityType;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.entities.ai.goals.GolemDefendVillageGoal;
import com.quzzar.kithkyn.entities.ai.goals.GuardPatrolGoal;
import com.quzzar.kithkyn.events.GolemEvents;
import com.quzzar.kithkyn.village.Occupation;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.VillageGolems;
import com.quzzar.kithkyn.village.VillageManager;
import com.quzzar.kithkyn.village.buildings.Building;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Real-entity regression checks, gated behind an explicit disposable-server flag. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class GolemVerification {
  private static int ticks;
  private static Village village;
  private static RealPerson guard;
  private static RealPerson secondGuard;
  private static IronGolem golem;
  private static CompoundTag saved;
  private static Vec3 patrolStart;
  private static Zombie attacker;
  private static SnowGolem snow;
  private static CompoundTag savedSnow;
  private static String snowName;
  private static Vec3 snowPatrolStart;
  private static Spider snowAttacker;

  private GolemVerification() {
  }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (!Boolean.getBoolean("kithkyn.golems.verify")) {
      return;
    }
    ticks++;
    ServerLevel level = event.getServer().overworld();
    try {
      if (ticks == 40) {
        setup(level);
      } else if (ticks == 100) {
        recruit();
      } else if (ticks == 105) {
        saved = new CompoundTag();
        golem.saveWithoutId(saved);
        golem.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        check(village.getGolems().members().size() == 1, "unload removed membership");
      } else if (ticks == 110) {
        golem = EntityType.IRON_GOLEM.create(level);
        golem.load(saved);
        check(level.addFreshEntity(golem), "reloaded golem did not join");
        VillageGolems.tickMember(golem);
        GolemEvents.joined(new EntityJoinLevelEvent(golem, level));
        check(goals(golem, GuardPatrolGoal.class) == 1, "duplicate or missing reloaded patrol");
        check(VillageGolems.village(golem) == village, "reload lost village");
        check(golem.hasCustomName(), "reload lost name");
        checkPatrolPace(golem, patrol(golem), 0.6D);
        guard.setOccupation(Occupation.WANDERER);
        guard.setNoAi(true);
        patrolStart = golem.position();
        level.setDayTime(18000);
      } else if (ticks == 310) {
        check(golem.position().distanceToSqr(patrolStart) > 1.0D, "golem did not patrol at night");
        check(VillageGolems.village(golem) == village, "recruiter's job change removed golem");
        golem.setTarget(guard);
        check(golem.getTarget() == null, "golem targeted its own resident");
        guard.setTarget(golem);
        check(guard.getTarget() == null, "resident targeted own golem");
        attacker = EntityType.ZOMBIE.create(level);
        attacker.setNoAi(true);
        attacker.setPersistenceRequired();
        attacker.moveTo(golem.getX() + 2, 5, golem.getZ(), 0, 0);
        check(level.addFreshEntity(attacker), "attacker spawn failed");
        attacker.setTarget(guard);
        GolemDefendVillageGoal defense = new GolemDefendVillageGoal(golem);
        check(defense.canUse(), "did not recognize an attacker of a resident");
        defense.start();
        check(golem.getTarget() == attacker, "did not defend resident");
      } else if (ticks == 370) {
        check(!attacker.isAlive() || attacker.getHealth() < attacker.getMaxHealth(), "no actual combat damage");
        check(village.getPopulation().size() == 2 && village.getTotalBeds() == 0
            && village.getJobAssignmentsView().isEmpty(), "golem changed human population, beds or jobs");
        golem.kill();
        check(village.getGolems().members().isEmpty(), "dead golem remained in roster");
        checkMultipleRecruits(level);
        setupSnow(level);
      } else if (ticks == 400) {
        recruitSnow(level);
      } else if (ticks == 405) {
        savedSnow = new CompoundTag();
        snow.saveWithoutId(savedSnow);
        snow.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        check(village.getGolems().members().size() == 9, "snow unload removed membership");
      } else if (ticks == 410) {
        snow = EntityType.SNOW_GOLEM.create(level);
        snow.load(savedSnow);
        check(level.addFreshEntity(snow), "snow reload failed");
        GolemEvents.joined(new EntityJoinLevelEvent(snow, level));
        check(goals(snow, GuardPatrolGoal.class) == 1, "snow reload duplicated patrol");
        check(snow.targetSelector.getAvailableGoals().stream().filter(g ->
            g.getGoal() instanceof GolemDefendVillageGoal).count() == 1, "snow reload duplicated defense");
        check(VillageGolems.village(snow) == village && snow.getName().getString().equals(snowName),
            "snow reload lost membership or name");
        check(!snow.hasPumpkin() && snow.getMaxHealth() == 4.0F, "snow native traits changed");
        checkPatrolPace(snow, patrol(snow), 1.0D);
        guard.setOccupation(Occupation.WANDERER);
        guard.setNoAi(true);
        snowPatrolStart = snow.position();
        level.setDayTime(18000);
      } else if (ticks == 610) {
        check(snow.position().distanceToSqr(snowPatrolStart) > 1.0D, "snow did not patrol at night");
        check(VillageGolems.village(snow) == village, "snow followed recruiter's job change");
        checkSnowballs(level);
        snowAttacker = EntityType.SPIDER.create(level);
        snowAttacker.setNoAi(true);
        snowAttacker.setPersistenceRequired();
        snowAttacker.moveTo(snow.getX() + 4, 5, snow.getZ(), 0, 0);
        check(level.addFreshEntity(snowAttacker), "snow attacker spawn failed");
        snowAttacker.setTarget(guard);
      } else if (ticks == 612) {
        GolemDefendVillageGoal defense = new GolemDefendVillageGoal(snow);
        check(defense.canUse(), "snow did not recognize resident's attacker");
        defense.start();
      } else if (ticks == 690) {
        check(!snowAttacker.isAlive() || snowAttacker.getHealth() < snowAttacker.getMaxHealth(),
            "snow native ranged attack dealt no actual damage");
        check(village.getPopulation().size() == 2 && village.getTotalBeds() == 0
            && village.getJobAssignmentsView().isEmpty(), "snow consumed human capacity");
        snow.kill();
        check(village.getGolems().members().size() == 8, "snow death removed other mixed-roster members");
        Kithkyn.LOGGER.info("[golem-verify] RESULT PASS: iron and snow natural hooks, double-claim, eligibility, vanilla preservation, "
            + "NBT reload, species-scaled patrol speed, unchanged human pace, multiple recruits per guard, "
            + "no bed/job/population, night patrol, guard reassignment, friendly fire, snowball damage, combat, death");
        event.getServer().halt(false);
      }
    } catch (Exception | AssertionError failure) {
      Kithkyn.LOGGER.error("[golem-verify] RESULT FAIL", failure);
      event.getServer().halt(false);
    }
  }

  private static void setup(ServerLevel level) {
    for (int x = 4; x <= 6; x++) {
      for (int z = 4; z <= 6; z++) {
        level.setChunkForced(x, z, true);
      }
    }
    for (BlockPos pos : BlockPos.betweenClosed(64, 4, 64, 110, 12, 110)) {
      level.setBlock(pos, pos.getY() == 4 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
    }
    village = new FixtureVillage();
    village.attach(level);
    VillageManager.get(level).getVillages().put(village.getID(), village);
    guard = resident(level, 79.5, 80.5);
    secondGuard = resident(level, 80.5, 81.5);
    golem = EntityType.IRON_GOLEM.create(level);
    golem.setPlayerCreated(true);
    golem.moveTo(83.5, 5, 80.5, 0, 0);
    check(level.addFreshEntity(golem), "golem spawn failed");
    check(goals(golem, GuardPatrolGoal.class) == 0, "ordinary golem altered");
    golem.setRemainingPersistentAngerTime(100);
    check(!VillageGolems.canAdopt(guard, golem), "angry golem was eligible");
    golem.setRemainingPersistentAngerTime(0);
    golem.setLeashedTo(guard, false);
    check(!VillageGolems.canAdopt(guard, golem), "leashed golem was eligible");
    golem.dropLeash(false, false);
    guard.setNoAi(true);
    secondGuard.setNoAi(true);
  }

  /** Wait for the new entities to enter the world's spatial index before exercising encounter scanning. */
  private static void recruit() {
    guard.setNoAi(false);
    secondGuard.setNoAi(false);
    guard.setOccupation(Occupation.WANDERER);
    check(!VillageGolems.canAdopt(guard, golem), "civilian could recruit");
    guard.setOccupation(Occupation.GUARD);
    check(VillageGolems.canAdopt(guard, golem), "eligible encounter lost before scan: "
        + "alive=" + guard.isAlive() + " ai=" + !guard.isNoAi() + " sleeping=" + guard.isSleeping()
        + " target=" + guard.getTarget() + " village=" + (guard.getVillage() == village)
        + " distance=" + guard.distanceToSqr(golem) + " sight=" + guard.hasLineOfSight(golem)
        + " golemTarget=" + golem.getTarget() + " anger=" + golem.getRemainingPersistentAngerTime()
        + " membership=" + VillageGolems.villageId(golem));
    check(guard.level().getEntitiesOfClass(IronGolem.class, guard.getBoundingBox().inflate(8)).contains(golem),
        "new golem is absent from loaded encounter index: guardBox=" + guard.getBoundingBox()
            + " golemBox=" + golem.getBoundingBox() + " pos=" + golem.position()
            + " loaded=" + ((ServerLevel) guard.level()).isPositionEntityTicking(golem.blockPosition())
            + " lookup=" + (((ServerLevel) guard.level()).getEntity(golem.getUUID()) == golem));
    guard.tickCount = Math.floorMod(-guard.getId(), 100);
    GolemEvents.tick(new EntityTickEvent.Post(guard));
    check(VillageGolems.village(golem) == village && golem.hasCustomName(), "natural hook failed to name/adopt");
    check(!VillageGolems.adopt(secondGuard, golem), "second guard claimed same golem");
    check(golem.isPlayerCreated() && !golem.canAttackType(EntityType.PLAYER), "player-created protection changed");
    check(goals(golem, GuardPatrolGoal.class) == 1, "missing patrol");
    checkPatrolPace(guard, new GuardPatrolGoal(guard), 0.45D);
    checkPatrolPace(golem, patrol(golem), 0.6D);
    check(golem.goalSelector.getAvailableGoals().stream().anyMatch(goal ->
        goal.getGoal() instanceof net.minecraft.world.entity.ai.goal.MeleeAttackGoal), "vanilla melee removed");
    check(golem.goalSelector.getAvailableGoals().stream().noneMatch(goal ->
        goal.getGoal() instanceof net.minecraft.world.entity.ai.goal.MoveBackToVillageGoal), "vanilla routing retained");
    guard.setNoAi(true);
    secondGuard.setNoAi(true);
  }

  /** Exercise the installed patrol through vanilla navigation and movement, not just a constant. */
  private static void checkPatrolPace(PathfinderMob member, GuardPatrolGoal patrol, double multiplier) {
    Vec3 position = member.position();
    double baseSpeed = member.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue();
    member.moveTo(70.5D, 5, 84.5D, 0, 0);
    member.setOnGround(true);
    member.getRandom().setSeed(42L);
    check(patrol.canUse(), "patrol could not start for speed check");
    patrol.start();
    try {
      for (double factor : new double[] { 1.0D, 1.5D }) {
        member.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(baseSpeed * factor);
        member.getNavigation().tick();
        check(member.getMoveControl().hasWanted(), "patrol did not give navigation a destination");
        double actual = member.getMoveControl().getSpeedModifier();
        check(Math.abs(actual - multiplier) < 0.000001D,
            "patrol speed multiplier: expected " + multiplier + ", got " + actual);
        member.getMoveControl().tick();
        double expectedSpeed = multiplier * member.getAttributeValue(Attributes.MOVEMENT_SPEED);
        check(Math.abs(member.getSpeed() - expectedSpeed) < 0.000001D,
            "patrol did not scale with its own movement attribute");
      }
    } finally {
      member.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(baseSpeed);
      patrol.stop();
      member.moveTo(position.x, position.y, position.z, 0, 0);
    }
  }

  private static GuardPatrolGoal patrol(AbstractGolem member) {
    return member.goalSelector.getAvailableGoals().stream().map(goal -> goal.getGoal())
        .filter(GuardPatrolGoal.class::isInstance).map(GuardPatrolGoal.class::cast).findFirst().orElseThrow();
  }

  /** Continue in the same fixture so mixed-species membership exercises the existing roster. */
  private static void setupSnow(ServerLevel level) {
    level.getEntitiesOfClass(IronGolem.class, guard.getBoundingBox().inflate(64)).forEach(mob -> mob.setNoAi(true));
    guard.moveTo(82.5D, 5, 91.5D, 0, 0);
    guard.setTarget(null);
    guard.setNoAi(true);
    secondGuard.setNoAi(true);
    snow = EntityType.SNOW_GOLEM.create(level);
    snow.moveTo(85.5D, 5, 91.5D, 0, 0);
    snow.setPumpkin(false);
    snow.setNoAi(true);
    check(level.addFreshEntity(snow), "snow spawn failed");
    check(goals(snow, GuardPatrolGoal.class) == 0, "ordinary snow golem altered");
  }

  private static void recruitSnow(ServerLevel level) {
    guard.setNoAi(false);
    check(!VillageGolems.canAdopt(guard, snow), "AI-disabled snow golem was eligible");
    snow.setNoAi(false);
    snow.setLeashedTo(guard, false);
    check(!VillageGolems.canAdopt(guard, snow), "leashed snow golem was eligible");
    snow.dropLeash(false, false);
    guard.tickCount = Math.floorMod(-guard.getId(), 100);
    GolemEvents.tick(new EntityTickEvent.Post(guard));
    check(VillageGolems.village(snow) == village && snow.hasCustomName(), "natural hook failed for snow");
    snowName = snow.getName().getString();
    check(!VillageGolems.adopt(guard, snow), "snow adopted twice");
    check(snow.goalSelector.getAvailableGoals().stream().anyMatch(g ->
        g.getGoal() instanceof net.minecraft.world.entity.ai.goal.RangedAttackGoal), "snow lost ranged attack");
    check(snow.goalSelector.getAvailableGoals().stream().noneMatch(g ->
        g.getGoal() instanceof net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal), "snow kept competing stroll");
    checkPatrolPace(snow, patrol(snow), 1.0D);
    SnowGolem named = EntityType.SNOW_GOLEM.create(level);
    named.moveTo(84.5D, 5, 91.5D, 0, 0);
    named.setCustomName(net.minecraft.network.chat.Component.literal("Snow Friend"));
    check(level.addFreshEntity(named) && VillageGolems.adopt(guard, named), "second snow recruit failed");
    check(named.getName().getString().equals("Snow Friend"), "existing snow name overwritten");
    named.setNoAi(true);
    check(village.getGolems().members().size() == 9, "mixed roster replaced existing iron golems");
    Village restored = Village.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE,
        Village.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, village).getOrThrow()).getOrThrow();
    check(restored.getGolems().members().size() == 9, "mixed roster failed save/load");
    guard.setNoAi(true);
  }

  /** Exercise vanilla snowball collision and NeoForge's real damage pipeline, not a mocked event. */
  private static void checkSnowballs(ServerLevel level) {
    Spider hostile = EntityType.SPIDER.create(level);
    float health = hostile.getHealth();
    new CollisionSnowball(level, snow).hit(hostile);
    check(Math.abs(hostile.getHealth() - (health - 2F)) < 0.001F, "adopted snowball did not deal two damage");
    check(hostile.getLastHurtByMob() == snow, "snowball lost damage attribution");
    var blaze = EntityType.BLAZE.create(level);
    health = blaze.getHealth();
    new CollisionSnowball(level, snow).hit(blaze);
    check(blaze.getHealth() == health - 3F, "blaze's vanilla three damage changed");
    SnowGolem wild = EntityType.SNOW_GOLEM.create(level);
    Spider wildTarget = EntityType.SPIDER.create(level);
    health = wildTarget.getHealth();
    new CollisionSnowball(level, wild).hit(wildTarget);
    check(wildTarget.getHealth() == health, "unadopted snowball damage changed");
    Spider playerTarget = EntityType.SPIDER.create(level);
    health = playerTarget.getHealth();
    new CollisionSnowball(level, guard).hit(playerTarget);
    check(playerTarget.getHealth() == health, "non-golem snowball damage changed");
    var cow = EntityType.COW.create(level);
    health = cow.getHealth();
    new CollisionSnowball(level, snow).hit(cow);
    check(cow.getHealth() == health, "accidental livestock hit gained damage");
    for (var member : village.getGolems().members().keySet()) {
      Entity entity = level.getEntity(member);
      if (entity instanceof LivingEntity friend && friend != snow) {
        checkFriendlySnowball(level, friend);
      }
    }
    checkFriendlySnowball(level, guard);
    snow.setTarget(guard);
    guard.setTarget(snow);
    check(snow.getTarget() == null && guard.getTarget() == null, "snow friendly targeting permitted");
  }

  private static void checkFriendlySnowball(ServerLevel level, LivingEntity friend) {
    friend.setLastHurtByMob(null);
    friend.invulnerableTime = 0;
    float health = friend.getHealth();
    Vec3 velocity = friend.getDeltaMovement();
    new CollisionSnowball(level, snow).hit(friend);
    check(friend.getHealth() == health && friend.getLastHurtByMob() == null
        && friend.getDeltaMovement().equals(velocity), "friendly snowball caused damage, retaliation or knockback");
  }

  private static final class CollisionSnowball extends Snowball {
    private CollisionSnowball(ServerLevel level, LivingEntity owner) { super(level, owner); }
    private void hit(Entity target) {
      onHit(new EntityHitResult(target));
      check(isRemoved(), "snowball collision did not discard the projectile");
    }
  }

  /** One human can recruit many distinct golems without consuming housing or employment. */
  private static void checkMultipleRecruits(ServerLevel level) {
    guard.setOccupation(Occupation.GUARD);
    guard.setNoAi(false);
    guard.setTarget(null);
    guard.moveTo(78.5D, 5, 72.5D, 0, 0);
    secondGuard.setNoAi(false);
    secondGuard.setTarget(null);
    secondGuard.moveTo(78.5D, 5, 74.5D, 0, 0);
    java.util.ArrayList<IronGolem> recruits = new java.util.ArrayList<>();
    for (int i = 0; i < 8; i++) {
      IronGolem recruit = EntityType.IRON_GOLEM.create(level);
      recruit.moveTo(80.5D, 5, 72.5D, 0, 0);
      check(level.addFreshEntity(recruit), "additional golem did not spawn");
      check(VillageGolems.adopt(guard, recruit), "same guard could not recruit golem " + (i + 1));
      check(!VillageGolems.adopt(secondGuard, recruit), "an adopted golem was claimed again");
      recruits.add(recruit);
      check(village.getGolems().members().size() == i + 1, "additional recruit replaced another golem");
    }
    CompoundTag rosterSave = (CompoundTag) Village.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, village)
        .getOrThrow();
    Village restored = Village.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, rosterSave).getOrThrow();
    check(restored.getGolems().members().size() == 8, "save/load lost multiple golems");
    check(village.getPopulation().size() == 2 && village.getTotalBeds() == 0
        && village.getJobAssignmentsView().isEmpty(), "multiple golems consumed human capacity");
    recruits.getFirst().kill();
    check(village.getGolems().members().size() == 7, "one death removed other golems");
  }

  private static RealPerson resident(ServerLevel level, double x, double z) {
    RealPerson person = PersonEntityType.PERSON.get().create(level);
    person.setVillage(village.getID());
    person.setVillageName(village.getName());
    person.setOccupation(Occupation.GUARD);
    person.moveTo(x, 5, z, 0, 0);
    village.getPopulation().add(person.getUUID());
    check(level.addFreshEntity(person), "guard spawn failed");
    return person;
  }

  private static long goals(AbstractGolem entity, Class<?> type) {
    return entity.goalSelector.getAvailableGoals().stream().filter(goal -> type.isInstance(goal.getGoal())).count();
  }

  private static void check(boolean condition, String message) {
    if (!condition) {
      throw new AssertionError(message);
    }
  }

  /** A fixed small settlement avoids unrelated construction and model calls during the test. */
  private static final class FixtureVillage extends Village {
    private final Building center = new Building(new BlockPos(84, 5, 84), "house_birch_forest_1", Rotation.NONE);

    private FixtureVillage() {
      super("Golem Test Village");
      center.setCenterLocation(new BlockPos(84, 5, 84).asLong());
      center.setRadius(4);
    }

    @Override
    public void update(ServerLevel level) { }

    @Override
    public Building getTownCenter() { return center; }

    @Override
    public Collection<Building> getBuildings() { return List.of(center); }

    @Override
    public boolean hasClaimedWithin(BlockPos pos, int padding) {
      return pos.distSqr(new BlockPos(84, 5, 84)) < 32 * 32;
    }
  }
}
