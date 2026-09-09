package com.quzzar.kithkyn.events;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.VillageGolems;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Snowball;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Server-only lifecycle hooks for recruited golems; ordinary golems remain untouched. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class GolemEvents {
  private GolemEvents() {
  }

  @SubscribeEvent
  public static void joined(EntityJoinLevelEvent event) {
    if (event.getLevel() instanceof ServerLevel && event.getEntity() instanceof AbstractGolem golem
        && VillageGolems.supports(golem)) {
      VillageGolems.attachGoals(golem);
    }
  }

  @SubscribeEvent
  public static void tick(EntityTickEvent.Post event) {
    if (!(event.getEntity().level() instanceof ServerLevel)) {
      return;
    }
    if (event.getEntity() instanceof RealPerson guard && (guard.tickCount + guard.getId()) % 100 == 0) {
      VillageGolems.considerAdoption(guard);
    } else if (event.getEntity() instanceof AbstractGolem golem && VillageGolems.supports(golem)
        && (golem.tickCount + golem.getId()) % 20 == 0 && VillageGolems.villageId(golem).isPresent()) {
      VillageGolems.tickMember(golem);
    }
  }

  @SubscribeEvent
  public static void changingTarget(LivingChangeTargetEvent event) {
    if (!event.getEntity().level().isClientSide()
        && VillageGolems.areFriendly(event.getEntity(), event.getNewAboutToBeSetTarget())) {
      event.setNewAboutToBeSetTarget(null);
    }
  }

  @SubscribeEvent
  public static void died(LivingDeathEvent event) {
    if (event.getEntity() instanceof AbstractGolem golem && VillageGolems.supports(golem)) {
      VillageGolems.died(golem);
    }
  }

  /** Discarding an entity is removal; chunk unload and dimension travel preserve membership. */
  @SubscribeEvent
  public static void left(EntityLeaveLevelEvent event) {
    if (event.getEntity() instanceof AbstractGolem golem && VillageGolems.supports(golem) && golem.getRemovalReason() != null
        && golem.getRemovalReason().shouldDestroy()) {
      VillageGolems.died(golem);
    }
  }

  /** Give recruited snow defenders a modest attack without changing ordinary snowballs. */
  @SubscribeEvent
  public static void snowballDamage(LivingIncomingDamageEvent event) {
    if (!(event.getEntity().level() instanceof ServerLevel)
        || !(event.getSource().getDirectEntity() instanceof Snowball snowball)
        || !(snowball.getOwner() instanceof SnowGolem golem)) {
      return;
    }
    Village village = VillageGolems.village(golem);
    if (village == null) {
      return;
    }
    if (VillageGolems.isMember(village, event.getEntity())) {
      // Even vanilla's zero-damage hits can cause knockback and retaliation.
      event.setCanceled(true);
    } else if (event.getEntity() instanceof Enemy || event.getEntity() == golem.getTarget()) {
      // One heart before armor; retain the blaze's three damage and any larger modded value.
      event.setAmount(Math.max(event.getAmount(), 2.0F));
    }
  }
}
