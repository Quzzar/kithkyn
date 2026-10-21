package com.quzzar.kithkyn.agency;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Short-lived server raycasts shared by chat and future speech input. No client coordinates are trusted. */
public final class PointedLocations {
  private static final long LIFETIME = 1200L;
  private record Key(MinecraftServer server, UUID player) { }
  private record Point(String dimension, com.quzzar.kithkyn.entities.UndertakingData.Position position, long expiresAt) { }
  private static final Map<Key, Point> POINTS = new HashMap<>();

  private PointedLocations() { }

  /** Pointing is an explicit input action and replaces any previous reference, including on failure. */
  public static boolean capture(ServerPlayer player) {
    Key key = new Key(player.getServer(), player.getUUID());
    POINTS.remove(key);
    Vec3 eyes = player.getEyePosition();
    // clip() may read terrain along the whole ray. Verify that it is already loaded first.
    for (int distance = 0; distance <= 32; distance++) {
      if (!player.serverLevel().hasChunkAt(BlockPos.containing(eyes.add(player.getLookAngle().scale(distance))))) {
        player.displayClientMessage(Component.literal("That direction leads into unloaded terrain."), true);
        return false;
      }
    }
    BlockHitResult hit = player.serverLevel().clip(new ClipContext(eyes,
        eyes.add(player.getLookAngle().scale(32)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
    if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP) {
      player.displayClientMessage(Component.literal("Point at the top of nearby ground."), true);
      return false;
    }
    BlockPos target = hit.getBlockPos().above();
    if (!player.serverLevel().hasChunkAt(target) || !player.serverLevel().getWorldBorder().isWithinBounds(target)) {
      player.displayClientMessage(Component.literal("That location is unavailable."), true);
      return false;
    }
    com.quzzar.kithkyn.entities.UndertakingData.Position position = new com.quzzar.kithkyn.entities.UndertakingData.Position(
        target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D);
    POINTS.put(key, new Point(player.level().dimension().location().toString(), position,
        player.level().getGameTime() + LIFETIME));
    player.displayClientMessage(Component.literal("Pointed at " + target.toShortString() + "."), true);
    return true;
  }

  public static Optional<com.quzzar.kithkyn.entities.UndertakingData.Position> get(ServerPlayer player) {
    Key key = new Key(player.getServer(), player.getUUID());
    Point point = POINTS.get(key);
    if (point == null) return Optional.empty();
    if (player.level().getGameTime() >= point.expiresAt()
        || !player.level().dimension().location().toString().equals(point.dimension())) {
      POINTS.remove(key);
      return Optional.empty();
    }
    return Optional.of(point.position());
  }

  public static void forget(ServerPlayer player) {
    POINTS.remove(new Key(player.getServer(), player.getUUID()));
  }

  public static void forget(MinecraftServer server) {
    POINTS.keySet().removeIf(key -> key.server() == server);
  }
}
