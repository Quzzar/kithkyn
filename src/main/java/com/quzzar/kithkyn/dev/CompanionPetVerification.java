package com.quzzar.kithkyn.dev;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.entities.ai.goals.CompanionSitGoal;
import com.quzzar.kithkyn.entities.ai.goals.PetFollowOwnerGoal;
import com.quzzar.kithkyn.events.CoreEvents;
import com.quzzar.kithkyn.village.CompanionPets;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Real Minecraft regression checks: use only on a disposable world containing a loaded villager. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class CompanionPetVerification {
  private static int ticks;
  private static RealPerson owner;
  private static final List<TamableAnimal> followers = new ArrayList<>();
  private static final List<TamableAnimal> resting = new ArrayList<>();
  private static long restStart;

  private CompanionPetVerification() {
  }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (!Boolean.getBoolean("kithkyn.companions.verify")) {
      return;
    }
    ticks++;
    try {
      if (ticks == 40) {
        setup(event.getServer().overworld());
      } else if (ticks == 160) {
        verifyMovementAndRest(event.getServer().overworld());
        Kithkyn.LOGGER.info("[companion-verify] RESULT PASS");
        event.getServer().halt(false);
      }
    } catch (Exception | AssertionError failure) {
      Kithkyn.LOGGER.error("[companion-verify] RESULT FAIL", failure);
      event.getServer().halt(false);
    }
  }

  private static void setup(ServerLevel level) {
    for (var entity : level.getAllEntities()) {
      if (entity instanceof RealPerson person) {
        owner = person;
        break;
      }
    }
    check(owner != null, "fixture needs a loaded villager");
    for (BlockPos pos : BlockPos.betweenClosed(64, 4, 64, 90, 8, 90)) {
      level.setBlock(pos, pos.getY() == 4 ? Blocks.GRASS_BLOCK.defaultBlockState()
          : Blocks.AIR.defaultBlockState(), 3);
    }
    owner.setNoAi(true);
    owner.getNavigation().stop();
    owner.moveTo(84.5, 5, 76.5, 0, 0);
    restStart = level.getGameTime();
    for (var type : List.of(EntityType.CAT, EntityType.WOLF)) {
      TamableAnimal pet = create(level, type, true);
      pet.moveTo(76.5, 5, 76.5, 0, 0);
      check(level.addFreshEntity(pet), "pet spawn failed");
      pet.setOnGround(true);
      CompanionPets.commandSit(pet, false);
      var sit = pet.goalSelector.getAvailableGoals().stream()
          .filter(goal -> goal.getGoal() instanceof SitWhenOrderedToGoal).findFirst().orElseThrow();
      check(sit.getGoal() instanceof CompanionSitGoal && sit.getPriority() == 2,
          "companion must receive the replacement at vanilla priority");
      check(pet.getOwner() == null && CompanionPets.loadedOwner(pet) == owner, "owner resolution fixture");
      check(!sit.canUse(), "uncommanded sit must be ineligible");
      pet.goalSelector.tick();
      check(!pet.isInSittingPose(), "pet sits without a command");
      check(pet.goalSelector.getAvailableGoals().stream()
          .anyMatch(goal -> goal.isRunning() && goal.getGoal() instanceof PetFollowOwnerGoal),
          "following must run");
      CompanionPets.commandSit(pet, true);
      check(sit.canUse() && pet.isInSittingPose(), "deliberate sitting must work");
      CompanionPets.commandSit(pet, false);
      check(!sit.canUse() && !pet.isInSittingPose(), "recall must clear sitting");
      CoreEvents.onLivingSpawned(new EntityJoinLevelEvent(pet, level));
      check(pet.goalSelector.getAvailableGoals().stream()
          .filter(goal -> goal.getGoal() instanceof CompanionSitGoal).count() == 1, "duplicate sit goal");
      check(pet.goalSelector.getAvailableGoals().stream()
          .filter(goal -> goal.getGoal() instanceof PetFollowOwnerGoal).count() == 1, "duplicate follow goal");
      followers.add(pet);

      TamableAnimal ordinary = create(level, type, false);
      check(level.addFreshEntity(ordinary), "ordinary pet spawn failed");
      check(ordinary.goalSelector.getAvailableGoals().stream()
          .anyMatch(goal -> goal.getGoal().getClass() == SitWhenOrderedToGoal.class), "ordinary pet lost vanilla sit");
      check(ordinary.goalSelector.getAvailableGoals().stream()
          .noneMatch(goal -> goal.getGoal() instanceof CompanionSitGoal
              || goal.getGoal() instanceof PetFollowOwnerGoal), "ordinary pet gained companion goals");
      ordinary.discard();

      TamableAnimal sleeper = create(level, type, true);
      sleeper.moveTo(70.5, 5, 70.5, 0, 0);
      CompanionPets.commandSit(sleeper, true);
      CompoundTag saved = new CompoundTag();
      sleeper.saveWithoutId(saved);
      TamableAnimal reloaded = type.create(level);
      reloaded.load(saved);
      check(level.addFreshEntity(reloaded), "reloaded pet spawn failed");
      check(reloaded.isOrderedToSit(), "reload lost sit order");
      check(reloaded.goalSelector.getAvailableGoals().stream()
          .anyMatch(goal -> goal.getGoal() instanceof CompanionSitGoal), "reload lost corrected goal");
      resting.add(reloaded);
      Kithkyn.LOGGER.info("[companion-verify] {} spawn, explicit sit/recall, idempotent join, ordinary pet and NBT reload PASS", type);
    }
  }

  private static TamableAnimal create(ServerLevel level, EntityType<? extends TamableAnimal> type, boolean companion) {
    TamableAnimal pet = type.create(level);
    pet.setTame(true, true);
    pet.setOwnerUUID(companion ? owner.getUUID() : UUID.randomUUID());
    if (companion) {
      pet.getPersistentData().putBoolean(CompanionPets.COMPANION_PET_KEY, true);
      pet.getPersistentData().putUUID(CompanionPets.PET_OWNER_KEY, owner.getUUID());
    }
    return pet;
  }

  private static void verifyMovementAndRest(ServerLevel level) {
    for (TamableAnimal pet : followers) {
      double distance = pet.distanceTo(owner);
      check(distance < 4, "pet did not walk from 8 blocks away to its owner: " + distance);
      check(!pet.isOrderedToSit() && !pet.isInSittingPose(), "following pet returned to uncommanded sitting");
      Kithkyn.LOGGER.info("[companion-verify] {} walked to owner, distance={} PASS", pet.getType(), distance);
    }
    for (TamableAnimal pet : resting) {
      long elapsed = level.getGameTime() - restStart;
      check(elapsed > 0 && CompanionPets.observedRestTicks(pet) == elapsed, "rest clock lost across reload");
      CompanionPets.commandSit(pet, true);
      check(CompanionPets.observedRestTicks(pet) == elapsed, "repeated sit reset rest clock");
      CompoundTag saved = new CompoundTag();
      pet.saveWithoutId(saved);
      TamableAnimal reloaded = (TamableAnimal) pet.getType().create(level);
      reloaded.load(saved);
      check(CompanionPets.observedRestTicks(reloaded) == elapsed, "elapsed rest lost on another reload");
      CompanionPets.commandSit(pet, false);
      check(CompanionPets.observedRestTicks(pet) == 0 && !pet.isInSittingPose(), "recall did not reset rest");
      pet.setOrderedToSit(true); // Simulate an old save with no rest timestamp.
      check(CompanionPets.observedRestTicks(pet) == 0, "old pet invented rest history");
      Kithkyn.LOGGER.info("[companion-verify] {} rest persistence, repeated sit, recall and old-save migration PASS", pet.getType());
    }
  }

  private static void check(boolean condition, String message) {
    if (!condition) {
      throw new AssertionError(message);
    }
  }
}
