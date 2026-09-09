package com.quzzar.kithkyn.village;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.entities.ai.goals.GuardPatrolGoal;
import com.quzzar.kithkyn.entities.ai.goals.GolemDefendVillageGoal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.GolemRandomStrollInVillageGoal;
import net.minecraft.world.entity.ai.goal.MoveBackToVillageGoal;
import net.minecraft.world.entity.ai.goal.target.DefendVillageTargetGoal;
import net.minecraft.world.level.ChunkPos;

/** Natural recruitment and the persistent tie between a vanilla golem and its village. */
public final class VillageGolems {
  public static final String VILLAGE_KEY = "kithkyn:guardVillage";
  public static final String RECRUITER_KEY = "kithkyn:recruitingGuard";
  public static final double ADOPTION_RADIUS = 8.0D;
  private static final List<String> IRON_NAMES = List.of(
      "Flint", "Anvil", "Bastion", "Bramble", "Oakheart", "Cinder", "Granite", "Aegis", "Copper", "Rowan");
  private static final List<String> SNOW_NAMES = List.of(
      "Flurry", "Frost", "Snowdrop", "Drift", "Winter", "Icicle", "Powder", "Snowbell", "Hail", "Glacier");

  private VillageGolems() {
  }

  /** A bounded encounter check, called every five seconds per active guard. */
  public static void considerAdoption(RealPerson guard) {
    if (!(guard.level() instanceof ServerLevel level) || !eligibleGuard(guard)) {
      return;
    }
    level.getEntitiesOfClass(AbstractGolem.class, guard.getBoundingBox().inflate(ADOPTION_RADIUS),
        golem -> canAdopt(guard, golem)).stream()
        .min(Comparator.comparingDouble(guard::distanceToSqr))
        .ifPresent(golem -> adopt(guard, golem));
  }

  private static boolean eligibleGuard(RealPerson guard) {
    Village village = guard.getVillage();
    return guard.isAlive() && !guard.isRemoved() && !guard.isNoAi() && !guard.isSleeping()
        && guard.getOccupation() == Occupation.GUARD && guard.getTarget() == null
        && village != null && village.getLevel() == guard.level()
        && village.getPopulation().contains(guard.getUUID())
        && village.hasClaimedWithin(guard.blockPosition(), 32);
  }

  /** Leashed, travelling, hostile, already recruited and unreachable golems are left alone. */
  public static boolean canAdopt(RealPerson guard, AbstractGolem golem) {
    return supports(golem) && eligibleGuard(guard) && golem.level() == guard.level()
        && golem.isAlive() && !golem.isRemoved() && !golem.isNoAi()
        && !golem.isLeashed() && !golem.isPassenger() && !golem.isVehicle()
        && villageId(golem).isEmpty() && golem.getTarget() == null
        && (!(golem instanceof IronGolem iron) || iron.getRemainingPersistentAngerTime() == 0)
        && guard.distanceToSqr(golem) <= ADOPTION_RADIUS * ADOPTION_RADIUS
        && guard.hasLineOfSight(golem);
  }

  /** Commit the claim synchronously, so two guards cannot adopt the same golem. */
  public static boolean adopt(RealPerson guard, AbstractGolem golem) {
    if (!(guard.level() instanceof ServerLevel level) || !canAdopt(guard, golem)) {
      return false;
    }
    Village village = guard.getVillage();
    boolean alreadyNamed = golem.hasCustomName();
    golem.getPersistentData().putString(VILLAGE_KEY, village.getID());
    golem.getPersistentData().putUUID(RECRUITER_KEY, guard.getUUID());
    golem.setPersistenceRequired();
    if (!alreadyNamed) {
      List<String> names = golem instanceof SnowGolem ? SNOW_NAMES : IRON_NAMES;
      golem.setCustomName(Component.literal(names.get(golem.getRandom().nextInt(names.size()))));
    }
    attachGoals(golem);
    remember(level, village, golem);
    String species = golem instanceof SnowGolem ? "snow golem" : "iron golem";
    guard.logMemory("I welcomed " + golem.getName().getString() + " the " + species + " into our village guard.",
        Optional.of(golem.getUUID()));
    com.quzzar.kithkyn.chat.VillagerConversation.speak(guard,
        "Welcome to the watch, " + golem.getName().getString() + ".");
    Kithkyn.LOGGER.info("[village golem] {} welcomes {} into {}", guard.getFullName(),
        golem.getName().getString(), village.getName());
    return true;
  }

  /** A backlink is enough to reattach behavior when an entity loads, with no duplicate goals. */
  public static Optional<String> villageId(Entity entity) {
    String id = entity.getPersistentData().getString(VILLAGE_KEY);
    return id.isBlank() ? Optional.empty() : Optional.of(id);
  }

  /** Explicit species support avoids recruiting other mobs that happen to extend AbstractGolem. */
  public static boolean supports(Entity entity) {
    return entity instanceof IronGolem || entity instanceof SnowGolem;
  }

  public static Village village(AbstractGolem golem) {
    if (!supports(golem) || !(golem.level() instanceof ServerLevel level)
        || level.dimension() != net.minecraft.world.level.Level.OVERWORLD) {
      return null;
    }
    return villageId(golem).map(id -> VillageManager.get(level).getVillage(id)).orElse(null);
  }

  /** Only recruited golems lose vanilla-village routing; combat and repair remain vanilla. */
  public static void attachGoals(AbstractGolem golem) {
    if (!supports(golem) || villageId(golem).isEmpty()) {
      return;
    }
    // Adoption and reload both restore vanilla's crosshair-only nameplate.
    golem.setCustomNameVisible(false);
    golem.goalSelector.getAvailableGoals().stream()
        .filter(wrapped -> wrapped.getGoal().getClass() == MoveBackToVillageGoal.class
            || wrapped.getGoal().getClass() == GolemRandomStrollInVillageGoal.class
            || golem instanceof SnowGolem && wrapped.getGoal().getClass() == WaterAvoidingRandomStrollGoal.class)
        .toList().forEach(wrapped -> golem.goalSelector.removeGoal(wrapped.getGoal()));
    golem.targetSelector.getAvailableGoals().stream()
        .filter(wrapped -> wrapped.getGoal().getClass() == DefendVillageTargetGoal.class)
        .toList().forEach(wrapped -> golem.targetSelector.removeGoal(wrapped.getGoal()));
    if (golem.goalSelector.getAvailableGoals().stream()
        .noneMatch(wrapped -> wrapped.getGoal() instanceof GuardPatrolGoal)) {
      // Keep each species' native strolling pace and priority, below its combat movement.
      boolean snow = golem instanceof SnowGolem;
      golem.goalSelector.addGoal(snow ? 2 : 4,
          new GuardPatrolGoal(golem, () -> village(golem), snow ? 1.0D : 0.6D));
    }
    if (golem.targetSelector.getAvailableGoals().stream()
        .noneMatch(wrapped -> wrapped.getGoal() instanceof GolemDefendVillageGoal)) {
      golem.targetSelector.addGoal(1, new GolemDefendVillageGoal(golem));
    }
  }

  /** Refresh last-known location without counting an unloaded entity as dead. */
  public static void tickMember(AbstractGolem golem) {
    Village village = village(golem);
    if (village != null && golem.isAlive()) {
      remember((ServerLevel) golem.level(), village, golem);
    }
  }

  public static void remember(ServerLevel level, Village village, AbstractGolem golem) {
    village.getGolems().remember(golem.getUUID(), golem.getName().getString(),
        ChunkPos.asLong(golem.chunkPosition().x, golem.chunkPosition().z));
    VillageManager.get(level).setDirty();
  }

  /** Death is explicit; merely unloading or changing the recruiting guard's job is not removal. */
  public static void died(AbstractGolem golem) {
    if (!(golem.level() instanceof ServerLevel level)) {
      return;
    }
    villageId(golem).map(id -> VillageManager.get(level).getVillage(id)).ifPresent(village -> {
      village.getGolems().remove(golem.getUUID());
      VillageManager.get(level).setDirty();
    });
  }

  /** Friends are the village's people, other recruited golems and its companion pets. */
  public static boolean isMember(Village village, Entity entity) {
    return village != null && entity != null
        && (entity instanceof RealPerson person && person.getVillage() == village
            || supports(entity) && villageId(entity).filter(village.getID()::equals).isPresent()
            || CompanionPets.isCompanionPet(entity)
                && CompanionPets.villageUuid(entity).filter(village.getID()::equals).isPresent());
  }

  /** Never turn an adopted golem and its own village against one another. */
  public static boolean areFriendly(LivingEntity actor, LivingEntity target) {
    if (target == null) {
      return false;
    }
    if (actor instanceof AbstractGolem golem && supports(golem)) {
      return isMember(village(golem), target);
    }
    return target instanceof AbstractGolem golem && supports(golem) && isMember(village(golem), actor);
  }
}
