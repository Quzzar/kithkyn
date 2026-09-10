package com.quzzar.kithkyn.dev;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.village.FarmedStock;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.VillageGolems;
import com.quzzar.kithkyn.village.VillageIdentity;
import com.quzzar.kithkyn.village.buildings.BuildProgress;
import com.quzzar.kithkyn.village.buildings.Building;
import com.quzzar.kithkyn.village.buildings.BuildingEntities;
import com.quzzar.kithkyn.village.buildings.Buildings;
import com.quzzar.kithkyn.village.buildings.ConstructionMode;
import com.quzzar.kithkyn.village.buildings.InstantBuildStructure;
import com.quzzar.kithkyn.village.buildings.StructureInProgress;
import com.quzzar.kithkyn.village.buildings.VillageIdentityApplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Real approved assets on a disposable server only: -Dkithkyn.buildingPlacement.verify=true. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class BuildingPlacementVerification {
  private static final String[] ASSETS = System.getProperty("kithkyn.buildingPlacement.assets",
      "village_center_birch_forest_1,butchery_birch_forest_1,bakery_birch_forest_1").split(",");
  private static final List<Fixture> fixtures = new ArrayList<>();
  private static final List<CompoundTag> savedEntities = new ArrayList<>();
  private static final VillageIdentity FIRST = new VillageIdentity("Purple", DyeColor.PURPLE, DyeColor.LIME,
      List.of(new VillageIdentity.BannerLayer(ResourceLocation.withDefaultNamespace("cross"),
          VillageIdentity.ColorRole.SECONDARY)));
  private static final VillageIdentity SECOND = new VillageIdentity("Cyan", DyeColor.CYAN, DyeColor.RED,
      FIRST.bannerLayers());
  private static int ticks;
  private static int fixtureIndex;
  private static int finishedAt = -1;
  private static boolean restarting;

  private record Fixture(Building building, List<UUID> entities, VillageIdentity identity) {
    private static final Codec<Fixture> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Building.CODEC.fieldOf("building").forGetter(Fixture::building),
        UUIDUtil.CODEC.listOf().fieldOf("entities").forGetter(Fixture::entities),
        VillageIdentity.CODEC.fieldOf("identity").forGetter(Fixture::identity)
    ).apply(instance, Fixture::new));
  }

  /** Only this opt-in fixture owns this disposable-world data, never the village registry. */
  private static final class Manifest extends SavedData {
    private static final Factory<Manifest> FACTORY = new Factory<>(Manifest::new, Manifest::load, null);
    private final List<Fixture> stored = new ArrayList<>();

    private static Manifest load(CompoundTag tag, HolderLookup.Provider registries) {
      Manifest result = new Manifest();
      result.stored.addAll(Fixture.CODEC.listOf().parse(NbtOps.INSTANCE, tag.get("fixtures")).getOrThrow());
      return result;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
      tag.put("fixtures", Fixture.CODEC.listOf().encodeStart(NbtOps.INSTANCE, stored).getOrThrow());
      return tag;
    }

    private static Manifest get(ServerLevel level) {
      return level.getDataStorage().computeIfAbsent(FACTORY, "kithkyn_building_placement_verification");
    }
  }

  private BuildingPlacementVerification() {
  }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (!Boolean.getBoolean("kithkyn.buildingPlacement.verify")) {
      return;
    }
    ticks++;
    ServerLevel level = event.getServer().overworld();
    try {
      if (ticks == 40) {
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, event.getServer());
        level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, event.getServer());
        level.setDayTime(6000);
        Manifest manifest = Manifest.get(level);
        if (!manifest.stored.isEmpty()) {
          fixtures.addAll(manifest.stored);
          restarting = true;
        }
      }
      if (restarting) {
        if (ticks == 200) {
          verify(level);
          Set<UUID> before = ApprovedStructureAccess.entityIds(level);
          for (Fixture fixture : fixtures) {
            Building building = fixture.building();
            check(BuildingEntities.placeOnce(level, building, template(level, building),
                BlockPos.of(building.getOriginLocation()), settings(building)), "restart receipt rejected");
          }
          check(ApprovedStructureAccess.entityIds(level).equals(before), "restart repeated initial entities");
          Kithkyn.LOGGER.info("[building-placement-verify] RESTART PASS: {} saved buildings and {} original entities survived real server shutdown/restart without replenishment",
              fixtures.size(), fixtures.stream().mapToInt(fixture -> fixture.entities().size()).sum());
          event.getServer().halt(false);
        }
        return;
      }
      if (ticks >= 41 && fixtureIndex < ASSETS.length * Rotation.values().length * 2) {
        if ((ticks - 41) % 8 == 0) {
          prepare(level, buildingFor(fixtureIndex));
        } else if ((ticks - 41) % 8 == 6) {
          if (!entitiesReady(level, buildingFor(fixtureIndex))) {
            if (++readinessWait > 600) throw new AssertionError("Fixture entity chunks never became ready");
            if (readinessWait == 1) Kithkyn.LOGGER.info("[building-placement-verify] Waiting for fresh entity chunks before counting inhabitants");
            ticks--;
            return;
          }
          readinessWait = 0;
          build(level, fixtureIndex++);
          if (fixtureIndex == ASSETS.length * Rotation.values().length * 2) {
            finishedAt = ticks;
          }
        }
      } else if (finishedAt > 0 && ticks == finishedAt + 130) {
        verifyAndUnload(level);
      } else if (finishedAt > 0 && ticks == finishedAt + 131) {
        for (CompoundTag tag : savedEntities) {
          Entity restored = EntityType.create(tag, level).orElseThrow();
          check(level.addFreshEntity(restored), "entity reload rejected");
        }
      } else if (finishedAt > 0 && ticks == finishedAt + 265) {
        verify(level);
        Manifest manifest = Manifest.get(level);
        manifest.stored.addAll(fixtures);
        manifest.setDirty();
        Kithkyn.LOGGER.info("[building-placement-verify] RESULT PASS: {} real-template placements, both paths, four rotations, colors, frames, entity/save receipts and upgrade preservation", fixtures.size());
        event.getServer().halt(false);
      }
    } catch (Exception | AssertionError failure) {
      Kithkyn.LOGGER.error("[building-placement-verify] RESULT FAIL", failure);
      event.getServer().halt(false);
    }
  }

  private static void build(ServerLevel level, int index) {
    String name = ASSETS[index % ASSETS.length];
    Rotation rotation = Rotation.values()[(index / ASSETS.length) % Rotation.values().length];
    boolean incremental = index >= ASSETS.length * Rotation.values().length;
    VillageIdentity identity = incremental ? SECOND : FIRST;
    check(Buildings.getByName(name) != null, "missing definition " + name);
    Building building = buildingFor(index);
    StructureTemplate template = template(level, building);
    Set<UUID> before = ApprovedStructureAccess.entityIds(level);
    if (incremental) {
      StructureInProgress project = new StructureInProgress(building, new java.util.Random(index), ConstructionMode.FRESH);
      project.setOriginLocation(BlockPos.of(building.getOriginLocation()));
      project.attach(level, identity);
      SimpleContainer payment = new SimpleContainer(project.requiredMaterials().stream().map(ItemStack::copy).toArray(ItemStack[]::new));
      check(project.commitFromBuilder(payment, new Village(identity)), "fixture payment rejected");
      boolean reloaded = false;
      for (int step = 0; step < 100_000 && project.getProgress() != BuildProgress.COMPLETE; step++) {
        project.startBuilding();
        BlockPos next = project.peekNextBlockPos();
        BlockState preview = project.peekNextBlockState();
        project.updateBuilding();
        if (next != null && preview != null && preview.getBlock() instanceof BedBlock) {
          check(level.getBlockState(next).equals(preview), "bed preview differs from placed color " + next);
        }
        // Small templates finish before a fixed step count. Save after an actual
        // construction step while work remains, regardless of building size.
        if (!reloaded && next != null && project.getProgress() != BuildProgress.COMPLETE) {
          DynamicOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, level.registryAccess());
          project = StructureInProgress.CODEC.parse(ops,
              StructureInProgress.CODEC.encodeStart(ops, project).getOrThrow()).getOrThrow();
          project.attach(level, identity);
          building = project.getBuilding();
          reloaded = true;
        }
      }
      check(reloaded, "fixture never tested mid-build save");
      check(project.getProgress() == BuildProgress.COMPLETE, "construction did not finish " + name);
    } else {
      check(new InstantBuildStructure(building, new java.util.Random(index), level).withIdentity(identity)
          .seatAtOrigin(BlockPos.of(building.getOriginLocation()), new HashSet<>()).buildInstantly(), "instant build failed " + name);
    }
    List<UUID> added = ApprovedStructureAccess.entityIds(level).stream().filter(id -> !before.contains(id)).toList();
    check(added.size() == template.entityInfoList.size(), "initial entity count differs for " + name + ": " + added.size());
    for (UUID id : added) {
      if (level.getEntity(id) instanceof Mob mob) {
        mob.setNoAi(true);
      }
    }
    DynamicOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, level.registryAccess());
    Building restored = Building.CODEC.parse(ops, Building.CODEC.encodeStart(ops, building).getOrThrow()).getOrThrow();
    check(BuildingEntities.placeOnce(level, restored, template, BlockPos.of(restored.getOriginLocation()), settings(restored)), "receipt retry failed");
    Building upgraded = Building.upgradeOf(restored, restored.getName(), BlockPos.of(restored.getOriginLocation()), restored.getRotation());
    check(BuildingEntities.placeOnce(level, upgraded, template, BlockPos.of(upgraded.getOriginLocation()), settings(upgraded)), "upgrade retry failed");
    check(ApprovedStructureAccess.entityIds(level).size() == before.size() + added.size(), "save/retry/upgrade duplicated entities");
    Fixture fixture = new Fixture(restored, added, identity);
    verifyIdentity(level, fixture);
    fixtures.add(fixture);
    Kithkyn.LOGGER.info("[building-placement-verify] {} {} {} passed", name, rotation, incremental ? "incremental/reload" : "instant");
  }

  private static void verifyAndUnload(ServerLevel level) {
    verify(level);
    for (Fixture fixture : fixtures) {
      for (UUID id : fixture.entities()) {
        Entity entity = level.getEntity(id);
        CompoundTag tag = new CompoundTag();
        check(entity.save(tag), "entity save rejected");
        savedEntities.add(tag);
        entity.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
      }
    }
  }

  private static Building buildingFor(int index) {
    return new Building(new BlockPos(2000 + index % 6 * 80, 160, 2000 + index / 6 * 80),
        ASSETS[index % ASSETS.length], Rotation.values()[(index / ASSETS.length) % Rotation.values().length]);
  }

  private static int readinessWait;

  /** A generated block chunk can still be waiting for its asynchronous entity load. */
  private static boolean entitiesReady(ServerLevel level, Building building) {
    BoundingBox bounds = template(level, building).getBoundingBox(settings(building), BlockPos.of(building.getOriginLocation()));
    for (int x = bounds.minX() >> 4; x <= bounds.maxX() >> 4; x++) {
      for (int z = bounds.minZ() >> 4; z <= bounds.maxZ() >> 4; z++) {
        if (!level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(x, z))
            || !level.isPositionEntityTicking(new BlockPos(x * 16, bounds.minY(), z * 16))) return false;
      }
    }
    return true;
  }

  /** Chunk tickets need several ordinary server ticks before fresh entities become query-visible. */
  private static void prepare(ServerLevel level, Building building) {
    BoundingBox bounds = template(level, building).getBoundingBox(settings(building), BlockPos.of(building.getOriginLocation()));
    for (int x = (bounds.minX() - 2) >> 4; x <= (bounds.maxX() + 2) >> 4; x++) {
      for (int z = (bounds.minZ() - 2) >> 4; z <= (bounds.maxZ() + 2) >> 4; z++) {
        level.setChunkForced(x, z, true);
      }
    }
    for (BlockPos pos : BlockPos.betweenClosed(bounds.minX() - 2, bounds.minY() - 1, bounds.minZ() - 2,
        bounds.maxX() + 2, bounds.maxY() + 2, bounds.maxZ() + 2)) {
      level.setBlock(pos, pos.getY() == bounds.minY() - 1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
    }
  }

  private static void verify(ServerLevel level) {
    for (Fixture fixture : fixtures) {
      verifyIdentity(level, fixture);
      Building building = fixture.building();
      if (building.getName().equals("village_center_birch_forest_1")) {
        BlockPos origin = BlockPos.of(building.getOriginLocation());
        for (var entity : template(level, building).entityInfoList) {
          check(!entity.nbt.getBoolean("NoAI"), "frozen workshop preview entity was imported");
        }
        // Preserve the approved workshop changes through real placement and ticking,
        // including the new canopy tips outside the previous capture height.
        for (var block : template(level, building).palettes.getFirst().blocks()) {
          if (block.pos().getY() >= 8 || block.state().is(Blocks.CANDLE) || block.state().is(Blocks.TALL_GRASS)) {
            BlockPos world = origin.offset(block.pos().rotate(building.getRotation()));
            check(level.getBlockState(world).equals(block.state().rotate(building.getRotation())),
                "revised center decoration changed after placement/reload: " + block.pos() + " " + building.getRotation());
          }
        }
      }
      for (UUID id : fixture.entities()) {
        Entity entity = level.getEntity(id);
        check(entity != null && entity.isAlive(), "initial entity lost after ticking/reload: " + id);
        if (entity instanceof Animal animal && FarmedStock.isStock(animal)) {
          check(FarmedStock.isFarmed(animal), "initial livestock is not farmed");
        }
        if (entity instanceof IronGolem golem) {
          check(VillageGolems.village(golem) == null, "initial golem was adopted before any guard arrived");
        }
        if (entity instanceof ItemFrame frame) {
          check(frame.survives(), "frame lost its wall after rotation/reload");
          check(!frame.getItem().isEmpty(), "frame lost its decorative item");
        }
      }
    }
  }

  private static void verifyIdentity(ServerLevel level, Fixture fixture) {
    Building building = fixture.building();
    var slots = building.getInfo().getVillageIdentitySlots();
    BlockPos origin = BlockPos.of(building.getOriginLocation());
    for (BlockPos pos : slots.primaryBlocks()) {
      check(colorAt(level, origin.offset(pos.rotate(building.getRotation())), fixture.identity().primaryColor()), "primary slot color mismatch");
    }
    for (BlockPos pos : slots.secondaryBlocks()) {
      check(colorAt(level, origin.offset(pos.rotate(building.getRotation())), fixture.identity().secondaryColor()), "secondary slot color mismatch");
    }
    for (BlockPos pos : java.util.stream.Stream.concat(slots.primaryBlocks().stream(), slots.secondaryBlocks().stream()).toList()) {
      BlockPos world = origin.offset(pos.rotate(building.getRotation()));
      if (!slots.banners().contains(pos) && level.getBlockEntity(world) instanceof BannerBlockEntity banner) {
        check(banner.getPatterns().layers().isEmpty(), "Plain awning cloth became a patterned village flag");
        check(banner.getCustomName() == null, "Plain awning cloth acquired the village flag name");
        check(colorAt(level, world, banner.getBaseColor()), "Awning block entity kept the previous base color");
      }
    }
    for (BlockPos pos : slots.banners()) {
      BlockPos world = origin.offset(pos.rotate(building.getRotation()));
      check(level.getBlockState(world).getBlock() instanceof AbstractBannerBlock, "banner missing");
      check(colorAt(level, world, fixture.identity().primaryColor()), "banner base color mismatch");
      check(level.getBlockEntity(world) instanceof BannerBlockEntity banner && banner.getPatterns().layers().size() == 1,
          "banner pattern overwritten by capture NBT");
    }
    // The finishing pass must be idempotent, including bed partners.
    VillageIdentityApplier.apply(level, building, fixture.identity());
  }

  private static boolean colorAt(ServerLevel level, BlockPos pos, DyeColor color) {
    return BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).getPath().startsWith(color.getName() + "_");
  }

  private static StructureTemplate template(ServerLevel level, Building building) {
    return level.getStructureManager().getOrCreate(ResourceLocation.fromNamespaceAndPath(Kithkyn.MODID, building.getName()));
  }

  private static StructurePlaceSettings settings(Building building) {
    return new StructurePlaceSettings().setRotation(building.getRotation());
  }

  private static void check(boolean condition, String message) {
    if (!condition) {
      throw new AssertionError(message);
    }
  }
}
