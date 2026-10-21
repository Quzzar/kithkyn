package com.quzzar.kithkyn.networking;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.agency.PointedLocations;
import com.quzzar.kithkyn.configuration.KithkynConfig;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C2S: asks the server to capture the player's current pointing ray; carries no target data. */
public record PointAtLocationPacket() implements CustomPacketPayload {
  public static final Type<PointAtLocationPacket> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath(Kithkyn.MODID, "point_at_location"));
  public static final StreamCodec<ByteBuf, PointAtLocationPacket> STREAM_CODEC = StreamCodec.unit(new PointAtLocationPacket());

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(PointAtLocationPacket packet, IPayloadContext context) {
    context.enqueueWork(() -> {
      if (context.player() instanceof ServerPlayer player && player.isAlive() && !player.isSpectator()) {
        if (KithkynConfig.LlmResidentActions) PointedLocations.capture(player);
        else player.displayClientMessage(net.minecraft.network.chat.Component.literal("Resident actions are disabled."), true);
      }
    });
  }
}
