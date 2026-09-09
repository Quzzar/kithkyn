package com.quzzar.kithkyn.dev;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.savedata.PlacedBlockStore;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.VillageIdentity;
import com.quzzar.kithkyn.village.buildings.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Opt-in native flag placement checks. Enable only in a disposable test world. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class GateBannerVerification {
  private static int ticks;
  private static final List<WallProject> fixtures = new ArrayList<>();

  private GateBannerVerification() { }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (!Boolean.getBoolean("kithkyn.gateBanner.verify")) return;
    ServerLevel level = event.getServer().overworld();
    try {
      if (++ticks == 1) {
        prepare(level, 128, DyeColor.PURPLE, DyeColor.LIGHT_GRAY, false);
        prepare(level, 224, DyeColor.WHITE, DyeColor.BLUE, true);
      }
      if (ticks == 40) {
        for (WallProject wall : fixtures) verify(level, wall);
        Kithkyn.LOGGER.info("[gate-banner-verify] RESULT PASS: 32 supported flags, four directions, instant/incremental placement, white-primary patterns, save/rebind and player-edit protection");
        event.getServer().halt(false);
      }
    } catch (Exception | AssertionError failure) {
      Kithkyn.LOGGER.error("[gate-banner-verify] RESULT FAIL", failure);
      event.getServer().halt(false);
    }
  }

  private static void prepare(ServerLevel level, int x, DyeColor primary, DyeColor secondary, boolean incremental) {
    List<Long> ring = WallRoute.aroundBox(x, x + 64, 128, 192);
    for (BlockPos pos : BlockPos.betweenClosed(x - 10, 199, 118, x + 74, 210, 202)) {
      level.setBlock(pos, pos.getY() == 199 ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
    }
    Set<Long> gates = Set.of(BlockPos.asLong(x + 32, 0, 128), BlockPos.asLong(x + 64, 0, 160),
        BlockPos.asLong(x + 32, 0, 192), BlockPos.asLong(x, 0, 160));
    WallProject wall = new WallProject(ring, gates, Collections.nCopies(ring.size(), 200), WallTier.WOOD, VillageStyle.BIRCH_FOREST);
    VillageIdentity identity = new VillageIdentity("Gate " + primary.getName(), primary, secondary, List.of(
        new VillageIdentity.BannerLayer(ResourceLocation.withDefaultNamespace("stripe_center"), VillageIdentity.ColorRole.SECONDARY),
        new VillageIdentity.BannerLayer(ResourceLocation.withDefaultNamespace("border"), VillageIdentity.ColorRole.PRIMARY)));
    wall.bindIdentity(identity);
    if (incremental) {
      // Save halfway through, then resume through the real owning-village decoder.
      UUID worker = UUID.randomUUID();
      int total = wall.remainingBlocks();
      for (int done = 0; done < total; done++) {
        if (done == total / 2) wall = reload(wall, identity);
        WallRaiser.WallWork next = WallRaiser.nextWork(level, wall, worker, new BlockPos(x, 200, 128));
        if (next == null) break;
        WallRaiser.place(level, next.block(), wall);
        wall.advance(worker, next.section());
      }
      check(wall.isComplete(), "Incremental project did not complete");
      WallRaiser.finishWall(level, wall);
    } else {
      WallRaiser.placeAll(level, wall);
    }
    fixtures.add(wall);
    verify(level, wall);
    WallBlockPlan banner = wall.plannedBlocks().stream().filter(WallBlockPlan::isBanner).findFirst().orElseThrow();
    var desired = level.getBlockState(banner.pos());
    // A white placeholder has the right block for a white-primary village but no flag layers.
    level.setBlock(banner.pos(), Blocks.AIR.defaultBlockState(), 3);
    level.setBlock(banner.pos(), Blocks.WHITE_WALL_BANNER.defaultBlockState()
        .setValue(WallBannerBlock.FACING, desired.getValue(WallBannerBlock.FACING)), 3);
    check(!WallRaiser.isSatisfied(level, banner, wall), "Unpatterned banner was accepted");
    WallRaiser.place(level, banner, wall);
    var layers = ((BannerBlockEntity) level.getBlockEntity(banner.pos())).getPatterns();
    PlacedBlockStore.get(level).markPlayerPlaced(banner.pos());
    level.setBlock(banner.pos(), Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
    check(WallRaiser.isSatisfied(level, banner, wall), "Player edit not protected");
    WallRaiser.place(level, banner, wall);
    check(level.getBlockState(banner.pos()).is(Blocks.DIAMOND_BLOCK), "Player edit overwritten");
    PlacedBlockStore.get(level).clearPlaced(banner.pos());
    WallRaiser.place(level, banner, wall);
    check(layers.equals(((BannerBlockEntity) level.getBlockEntity(banner.pos())).getPatterns()), "Reapplication changed the flag");
    WallProject finished = WallProject.completed(ring, gates, wall.getGround(), WallTier.WOOD, VillageStyle.BIRCH_FOREST);
    check(reload(finished, identity).isComplete(), "Reload reopened a completed wall");
  }

  private static WallProject reload(WallProject wall, VillageIdentity identity) {
    JsonObject saved = Village.CODEC.encodeStart(JsonOps.INSTANCE, new Village(identity)).getOrThrow().getAsJsonObject();
    JsonObject project = new JsonObject();
    project.add("wall", WallProject.CODEC.encodeStart(JsonOps.INSTANCE, wall).getOrThrow());
    saved.add("project", project);
    WallProject loaded = Village.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow().getWallProject();
    check(identity.equals(loaded.getIdentity()), "Village identity was not rebound");
    return loaded;
  }

  private static void verify(ServerLevel level, WallProject wall) {
    List<WallBlockPlan> banners = wall.plannedBlocks().stream().filter(WallBlockPlan::isBanner).toList();
    check(banners.size() == 16, "Expected four banners per gate");
    for (WallBlockPlan cell : banners) {
      var state = level.getBlockState(cell.pos());
      check(state.getBlock() instanceof WallBannerBlock && state.canSurvive(level, cell.pos()), "Unsupported banner " + cell.pos());
      check(WallRaiser.isSatisfied(level, cell, wall), "Wrong identity " + cell.pos());
      BannerBlockEntity banner = (BannerBlockEntity) level.getBlockEntity(cell.pos());
      check(banner.getBaseColor() == wall.getIdentity().primaryColor(), "Wrong base");
      check(banner.getPatterns().layers().size() == 2, "Missing flag layers");
      check(banner.getPatterns().layers().get(0).color() == wall.getIdentity().secondaryColor(), "Wrong secondary");
      check(banner.getPatterns().layers().get(1).color() == wall.getIdentity().primaryColor(), "Wrong primary overlay");
      check(Component.literal(wall.getIdentity().name() + " Banner").equals(banner.getCustomName()), "Wrong name");
    }
  }

  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
}
