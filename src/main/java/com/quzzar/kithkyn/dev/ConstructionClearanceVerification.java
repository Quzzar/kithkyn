package com.quzzar.kithkyn.dev;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.savedata.PlacedBlockStore;
import com.quzzar.kithkyn.village.BlockOwnership;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.buildings.*;
import java.util.HashSet;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Opt-in native-world regression for completion cleanup; never runs in ordinary worlds. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class ConstructionClearanceVerification {
  private static int tick;
  private static int index;

  private ConstructionClearanceVerification() { }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (!Boolean.getBoolean("kithkyn.clearance.verify")) return;
    if (++tick < 40 || tick % 4 != 0) return;
    try {
      var level = event.getServer().overworld();
      level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, event.getServer());
      level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, event.getServer());
      verify(level, index++);
      if (index == 12) {
        Kithkyn.LOGGER.info("[clearance-verify] RESULT PASS: 12 real completion paths, all rotations, nearby/overhead trees, ownership, terrain and authored planting preservation");
        event.getServer().halt(false);
      }
    } catch (Exception | AssertionError failure) {
      Kithkyn.LOGGER.error("[clearance-verify] RESULT FAIL", failure);
      event.getServer().halt(false);
    }
  }

  private static void verify(ServerLevel level, int fixture) {
    Rotation rotation = Rotation.values()[fixture % 4];
    int mode = fixture / 4;
    BlockPos origin = new BlockPos(-4000 + fixture * 80, 160, -4000);
    String name = "lumberjack_birch_forest_1";
    Building building = new Building(origin, name, rotation);
    var template = level.getStructureManager().getOrCreate(ResourceLocation.fromNamespaceAndPath(Kithkyn.MODID, name));
    BoundingBox bounds = BuildingFootprint.bounds(template, rotation).moved(origin.getX(), origin.getY(), origin.getZ());
    var owned = PlacedBlockStore.get(level);
    for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(bounds.minX() - 8, 159, bounds.minZ() - 8),
        new BlockPos(bounds.maxX() + 8, bounds.maxY() + 12, bounds.maxZ() + 8))) {
      owned.clearPlaced(pos);
      level.setBlock(pos, pos.getY() == 159 ? Blocks.DIRT.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
    }
    FixtureVillage village = new FixtureVillage(level);
    if (mode == 2) {
      stamp(level, village, building);
      village.register(building);
      building = Building.upgradeOf(building, name, origin, rotation);
    }
    int middleZ = Math.floorDiv(bounds.minZ() + bounds.maxZ(), 2);
    BlockPos near = new BlockPos(bounds.maxX() + 3, 160, middleZ);
    BlockPos far = new BlockPos(bounds.minX() - 4, 160, bounds.minZ() - 4);
    BlockPos overhead = new BlockPos(bounds.minX() + 2, bounds.maxY() + 3, bounds.minZ() + 2);
    BlockPos playerWood = new BlockPos(bounds.minX() - 2, 160, middleZ);
    BlockPos villageWood = playerWood.offset(0, 0, 3);
    for (BlockPos base : java.util.List.of(near, far, overhead, playerWood, villageWood)) tree(level, base);
    for (int y = 0; y < 5; y++) {
      owned.markPlayerPlaced(playerWood.above(y));
      owned.markVillagePlaced(villageWood.above(y));
    }
    BlockPos brush = new BlockPos(bounds.maxX() + 1, 160, bounds.minZ());
    BlockPos outsideBrush = new BlockPos(bounds.maxX() + 2, 160, bounds.minZ() - 2);
    BlockPos canopy = new BlockPos(bounds.maxX() - 2, bounds.maxY() + 9, bounds.maxZ() - 2);
    level.setBlock(brush, Blocks.SHORT_GRASS.defaultBlockState(), 2);
    level.setBlock(outsideBrush, Blocks.SHORT_GRASS.defaultBlockState(), 2);
    level.setBlock(canopy, Blocks.OAK_LEAVES.defaultBlockState(), 2);
    if (mode == 1) {
      var project = new StructureInProgress(building, new Random(fixture), ConstructionMode.FRESH);
      project.setOriginLocation(origin);
      project.attach(level, village.getIdentity());
      var payment = new SimpleContainer(project.requiredMaterials().stream().map(ItemStack::copy).toArray(ItemStack[]::new));
      check(project.commitFromBuilder(payment, village), "payment rejected");
      for (int step = 0; step < 100_000 && project.getProgress() != BuildProgress.COMPLETE; step++) {
        project.startBuilding();
        project.updateBuilding();
        if (step == 100) {
          var ops = RegistryOps.create(NbtOps.INSTANCE, level.registryAccess());
          project = StructureInProgress.CODEC.parse(ops, StructureInProgress.CODEC.encodeStart(ops, project).getOrThrow()).getOrThrow();
          project.attach(level, village.getIdentity());
        }
      }
      check(project.getProgress() == BuildProgress.COMPLETE, "incremental build did not finish");
      building = project.getBuilding();
    } else stamp(level, village, building);
    if (mode == 2) village.replace(building); else village.register(building);
    check(level.getBlockState(near).isAir(), "Natural tree three blocks beside building was not felled");
    check(level.getBlockState(overhead).isAir(), "Overhead trunk was not felled");
    check(level.getBlockState(far).is(Blocks.OAK_LOG), "Disconnected tree beyond clearance was felled");
    check(level.getBlockState(playerWood).is(Blocks.OAK_LOG), "Player timber was removed");
    check(level.getBlockState(villageWood).is(Blocks.OAK_LOG), "Village timber was removed");
    check(level.getBlockState(brush).isAir(), "Adjacent brush remains");
    check(level.getBlockState(canopy).isAir(), "Canopy above roof remains");
    check(level.getBlockState(outsideBrush).is(Blocks.SHORT_GRASS), "Brush outside one-block clearance was removed");
    check(level.getBlockState(brush.below()).is(Blocks.DIRT), "Clearance carved ground");
    int plants = 0;
    for (var info : template.palettes.getFirst().blocks()) {
      if (BlockOwnership.isPlanted(info.state()) || info.state().is(Blocks.TALL_GRASS)) {
        check(level.getBlockState(origin.offset(info.pos().rotate(rotation))).is(info.state().getBlock()),
            "Authored plant erased at " + info.pos());
        plants++;
      }
    }
    check(plants > 0, "Fixture must exercise authored plant protection");
    Kithkyn.LOGGER.info("[clearance-verify] PASS mode={} rotation={} protectedPlants={}", mode, rotation, plants);
  }

  private static void stamp(ServerLevel level, Village village, Building building) {
    check(new InstantBuildStructure(building, new Random(0), level).withIdentity(village.getIdentity())
        .seatAtOrigin(BlockPos.of(building.getOriginLocation()), new HashSet<>()).buildInstantly(), "stamp failed");
  }

  private static void tree(ServerLevel level, BlockPos base) {
    for (int y = 0; y < 5; y++) level.setBlock(base.above(y), Blocks.OAK_LOG.defaultBlockState(), 2);
    level.setBlock(base.above(5), Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, false), 2);
  }

  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  /** Real completion hooks without unrelated autonomous village activity. */
  private static final class FixtureVillage extends Village {
    private FixtureVillage(ServerLevel level) { super("Clearance fixture"); attach(level); }
    private void register(Building building) { addBuilding(building); }
    private void replace(Building building) { replaceBuilding(building); }
    @Override public void update(ServerLevel ignored) { }
  }
}
