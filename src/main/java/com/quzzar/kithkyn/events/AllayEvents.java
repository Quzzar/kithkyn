package com.quzzar.kithkyn.events;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.village.VillageAllays;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.allay.Allay;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Server-only lifecycle hooks for adopted allays; wild allays remain untouched. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class AllayEvents {
  private AllayEvents() {
  }

  /** Adoption and reload both restore the nameplate rule and the keeper's brain behaviour. */
  @SubscribeEvent
  public static void joined(EntityJoinLevelEvent event) {
    if (event.getLevel() instanceof ServerLevel && event.getEntity() instanceof Allay allay) {
      VillageAllays.attach(allay);
    }
  }

  /** Quartermasters scan on the golem cadence, half a period apart; allays refresh their roster entry or their perch once a second. */
  @SubscribeEvent
  public static void tick(EntityTickEvent.Post event) {
    if (!(event.getEntity().level() instanceof ServerLevel level)) {
      return;
    }
    if (event.getEntity() instanceof RealPerson person && (person.tickCount + person.getId()) % 100 == 50) {
      VillageAllays.considerAdoption(person);
    } else if (event.getEntity() instanceof Allay allay && (allay.tickCount + allay.getId()) % 20 == 10) {
      if (VillageAllays.villageId(allay).isPresent()) {
        VillageAllays.tickMember(allay);
      } else {
        VillageAllays.tetherSpawned(level, allay);
      }
    }
  }

  @SubscribeEvent
  public static void died(LivingDeathEvent event) {
    if (event.getEntity() instanceof Allay allay) {
      VillageAllays.died(allay);
    }
  }

  /** Discarding an entity is removal; chunk unload and dimension travel preserve membership. */
  @SubscribeEvent
  public static void left(EntityLeaveLevelEvent event) {
    if (event.getEntity() instanceof Allay allay && allay.getRemovalReason() != null
        && allay.getRemovalReason().shouldDestroy()) {
      VillageAllays.died(allay);
    }
  }
}
