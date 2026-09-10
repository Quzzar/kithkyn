package com.quzzar.kithkyn.events;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.wrongdoing.VillageCustody;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Shared combat boundaries cover swords, attributed projectiles and recruited village golems. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class CustodyEvents {
  private CustodyEvents() { }

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public static void lethalHit(LivingDamageEvent.Pre event) {
    if (!(event.getEntity() instanceof ServerPlayer player)
        || !VillageCustody.isLethal(event.getNewDamage(), player.getHealth(), player.getAbsorptionAmount())) return;
    var village = VillageCustody.guardVillage(event.getSource().getEntity());
    if (village != null && VillageCustody.apprehend(village, player, event.getSource())) event.setNewDamage(0);
  }

  /** The ceasefire applies only to the responsible village's guards, not to unrelated damage. */
  @SubscribeEvent
  public static void incoming(LivingIncomingDamageEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) return;
    var village = VillageCustody.guardVillage(event.getSource().getEntity());
    if (village != null && VillageCustody.protects(village, player)) event.setCanceled(true);
  }

  @SubscribeEvent
  public static void targeting(LivingChangeTargetEvent event) {
    if (!(event.getNewAboutToBeSetTarget() instanceof ServerPlayer player)) return;
    var village = VillageCustody.guardVillage(event.getEntity());
    if (village != null && VillageCustody.protects(village, player)) event.setNewAboutToBeSetTarget(null);
  }

  @SubscribeEvent
  public static void tick(EntityTickEvent.Post event) {
    if ((event.getEntity().tickCount + event.getEntity().getId()) % 20 != 0) return;
    if (event.getEntity() instanceof ServerPlayer player) VillageCustody.tickPlayer(player);
    else if (event.getEntity() instanceof Mob mob && mob.getTarget() instanceof ServerPlayer player) {
      var village = VillageCustody.guardVillage(mob);
      if (village != null && VillageCustody.protects(village, player)) {
        mob.setTarget(null);
        mob.getNavigation().stop();
      }
    }
  }

  @SubscribeEvent
  public static void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) VillageCustody.tickPlayer(player);
  }

  @SubscribeEvent
  public static void serverTick(ServerTickEvent.Post event) {
    if (event.getServer().overworld().getGameTime() % 200 == 0) VillageCustody.tickServer(event.getServer().overworld());
  }

  @SubscribeEvent
  public static void died(LivingDeathEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) VillageCustody.died(player);
  }
}
