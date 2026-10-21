package com.quzzar.kithkyn.client;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;
import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.networking.PointAtLocationPacket;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Explicit, remappable pointing input before opening a resident conversation. */
public final class ResidentPointingInput {
  private static final KeyMapping POINT = new KeyMapping("key.kithkyn.point", InputConstants.Type.KEYSYM,
      GLFW.GLFW_KEY_G, "key.categories.kithkyn");

  private ResidentPointingInput() { }

  @EventBusSubscriber(modid = Kithkyn.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
  public static final class Registration {
    private Registration() { }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
      event.register(POINT);
    }
  }

  @EventBusSubscriber(modid = Kithkyn.MODID, value = Dist.CLIENT)
  public static final class Input {
    private Input() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
      Minecraft client = Minecraft.getInstance();
      while (POINT.consumeClick()) {
        if (client.player != null && client.screen == null && !client.player.isSpectator()) {
          PacketDistributor.sendToServer(new PointAtLocationPacket());
        }
      }
    }
  }
}
