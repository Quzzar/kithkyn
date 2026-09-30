package com.quzzar.kithkyn.dev;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.llm.LlmService;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.buildings.Buildings;
import com.quzzar.kithkyn.village.buildings.FoundingLayout;
import com.quzzar.kithkyn.village.buildings.VillageStyle;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Release fixtures are destructive and require an explicit JVM flag in a disposable world. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class BundledCatalogVerification {
  private static final String PREFIX = "[bundled-catalog-verify]";
  private static final BlockPos SITE = new BlockPos(1000, 80, 1000);
  private static int ticks;
  private static int index;
  private static int groundY;
  private static boolean completed;
  private static boolean finished;

  private BundledCatalogVerification() { }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (!Boolean.getBoolean("kithkyn.bundledCatalog.verify") || finished || ++ticks < 40) return;
    ServerLevel level = event.getServer().overworld();
    try {
      if (index == 0 && !completed) {
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, event.getServer());
        level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, event.getServer());
        level.setDayTime(6000);
        check(Buildings.allBuildings().size() == 394, "Expected all 394 bundled definitions");
        for (var info : Buildings.allBuildings().values()) {
          check(level.getStructureManager().get(ResourceLocation.fromNamespaceAndPath(Kithkyn.MODID, info.getPath())).isPresent(),
              "Missing template " + info.getPath());
        }
        level.getChunk(SITE);
        groundY = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, SITE).getY();
        for (int x = 56; x <= 68; x++) for (int z = 56; z <= 68; z++) level.setChunkForced(x, z, true);
      }
      if (!completed) {
        VillageStyle style = VillageStyle.values()[index / 4];
        Rotation rotation = Rotation.values()[index % 4];
        verify(level, style, rotation);
        index++;
        completed = index == VillageStyle.values().length * 4;
        return;
      }
      if (Boolean.getBoolean("kithkyn.bundledCatalog.requireLlm")) {
        LlmService.Status status = LlmService.get().getStatus();
        check(status != LlmService.Status.FAILED, "AI failed: " + LlmService.get().getStatusDetail());
        if (status != LlmService.Status.READY) return;
      }
      finished = true;
      Kithkyn.LOGGER.info("{} RESULT PASS: 394 definitions, 17 founding sets in four rotations, physical beds/containers and save round trips", PREFIX);
      if (!Boolean.getBoolean("kithkyn.bundledCatalog.keepAlive")) event.getServer().halt(false);
    } catch (Exception | AssertionError failure) {
      finished = true;
      Kithkyn.LOGGER.error(PREFIX + " RESULT FAIL", failure);
      event.getServer().halt(false);
    }
  }

  private static void verify(ServerLevel level, VillageStyle style, Rotation rotation) {
    check(Buildings.hasFoundingSet(style), "Missing founding set " + style);
    Village village = new Village("Release fixture " + style.id());
    village.attach(level);
    village.setStyle(style);
    var plan = village.planFounding(SITE, rotation, true).orElseThrow(() -> new AssertionError("No founding plan " + style + " " + rotation));
    check(village.found(plan), "Founding rejected " + style + " " + rotation);
    check(village.getBuildings().size() == plan.structures().size(), "Incomplete founding set");
    for (var building : village.getBuildings()) {
      BlockPos origin = BlockPos.of(building.getOriginLocation());
      for (long bed : building.getInfo().getBedLocations()) {
        check(level.getBlockState(origin.offset(BlockPos.of(bed).rotate(building.getRotation()))).getBlock() instanceof BedBlock,
            "Missing physical bed " + building.getName());
      }
      for (long container : building.getInfo().getContainerLocations()) {
        check(level.getBlockEntity(origin.offset(BlockPos.of(container).rotate(building.getRotation()))) instanceof Container,
            "Missing physical shared container " + building.getName());
      }
    }
    var ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
    Village restored = Village.CODEC.parse(ops, Village.CODEC.encodeStart(ops, village).getOrThrow()).getOrThrow();
    restored.attach(level);
    check(restored.getStyle() == style && restored.getTotalBeds() == village.getTotalBeds()
        && restored.getBuildings().size() == village.getBuildings().size(), "Save changed founding state");
    Kithkyn.LOGGER.info("{} {} {} PASS: {} buildings, {} beds", PREFIX, style.id(), rotation, village.getBuildings().size(), village.getTotalBeds());
    if (Boolean.getBoolean("kithkyn.bundledCatalog.keepAlive") && index == VillageStyle.values().length * 4 - 1) return;
    // Reuse the flat arena without keeping thousands of chunks forced or changing player worlds.
    var ownership = com.quzzar.kithkyn.savedata.PlacedBlockStore.get(level);
    for (long recorded : ownership.save(new net.minecraft.nbt.CompoundTag(), level.registryAccess()).getLongArray("VillagePlaced")) {
      BlockPos position = BlockPos.of(recorded);
      ownership.clearPlaced(position);
      level.setBlock(position, position.getY() < groundY ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
    }
    for (var structure : plan.structures()) {
      var bounds = FoundingLayout.worldBounds(structure);
      for (BlockPos position : BlockPos.betweenClosed(bounds.minX(), Math.min(groundY - 8, bounds.minY()), bounds.minZ(),
          bounds.maxX(), bounds.maxY(), bounds.maxZ())) {
        com.quzzar.kithkyn.savedata.PlacedBlockStore.get(level).clearPlaced(position);
        level.setBlock(position, position.getY() < groundY ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
      }
    }
    java.util.List<net.minecraft.world.entity.Entity> entities = new java.util.ArrayList<>();
    level.getAllEntities().forEach(entity -> { if (entity != null) entities.add(entity); });
    for (var entity : entities) if (!(entity instanceof net.minecraft.server.level.ServerPlayer)) entity.discard();
  }

  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
}
