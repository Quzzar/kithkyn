package com.quzzar.kithkyn.events;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.agency.PointedLocations;
import com.quzzar.kithkyn.agency.ResidentActionState.End;
import com.quzzar.kithkyn.entities.RealPerson;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Lifecycle cleanup for transient references and accepted resident actions. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class ResidentAgencyEvents {
  private ResidentAgencyEvents() { }

  @SubscribeEvent
  public static void tick(EntityTickEvent.Post event) {
    if (event.getEntity() instanceof RealPerson person && !person.level().isClientSide()) person.agency().maintain();
  }

  @SubscribeEvent
  public static void left(EntityLeaveLevelEvent event) {
    if (event.getEntity() instanceof RealPerson person && !person.level().isClientSide()) {
      person.agency().finish(End.INTERRUPTED, "The resident left the loaded world.");
    }
  }

  @SubscribeEvent
  public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) PointedLocations.forget(player);
  }

  @SubscribeEvent
  public static void stopped(ServerStoppedEvent event) {
    PointedLocations.forget(event.getServer());
  }
}
