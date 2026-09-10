package com.quzzar.kithkyn.events;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.savedata.RepairStore;
import com.quzzar.kithkyn.village.BuildingRepairs;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Observes explosion damage without changing explosion results or charging for candidate blocks that survived. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class RepairEvents {
  private RepairEvents() { }

  @SubscribeEvent
  public static void detonate(ExplosionEvent.Detonate event) {
    if (event.getLevel() instanceof ServerLevel level) {
      BuildingRepairs.observeExplosion(level, event.getAffectedBlocks());
    }
  }

  @SubscribeEvent
  public static void afterLevelTick(LevelTickEvent.Post event) {
    if (event.getLevel() instanceof ServerLevel level) {
      RepairStore.get(level).confirmExplosions(pos -> level.hasChunkAt(pos) && level.getBlockState(pos).isAir());
    }
  }
}
