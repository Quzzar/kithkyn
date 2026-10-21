package com.quzzar.kithkyn.decision;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.entities.UndertakingService;
import com.quzzar.kithkyn.entities.RealPerson;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Existing undertaking maintenance and cleanup of session-local autonomous reactions. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class ResidentReactionEvents {
  private ResidentReactionEvents() { }

  @SubscribeEvent
  public static void tick(EntityTickEvent.Post event) {
    if (event.getEntity() instanceof RealPerson person && !person.level().isClientSide()) {
      UndertakingService.maintain(person);
      person.reactions().tick();
    }
  }

  @SubscribeEvent
  public static void left(EntityLeaveLevelEvent event) {
    if (event.getEntity() instanceof RealPerson person && !person.level().isClientSide()) {
      person.reactions().interrupt("The resident left the loaded world.");
    }
  }

  @SubscribeEvent
  public static void stopped(ServerStoppedEvent event) {
    ReactionDecisions.stop(event.getServer());
  }
}
