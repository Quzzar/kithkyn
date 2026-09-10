package com.quzzar.kithkyn.dev;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.PersonEntityType;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.entities.ai.goals.work.RepairStep;
import com.quzzar.kithkyn.savedata.PlacedBlockStore;
import com.quzzar.kithkyn.savedata.RepairStore;
import com.quzzar.kithkyn.savedata.RepairStore.Repair;
import com.quzzar.kithkyn.village.BuildingRepairs;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.VillageManager;
import com.quzzar.kithkyn.village.buildings.Building;
import com.quzzar.kithkyn.village.buildings.MineShaft;
import com.quzzar.kithkyn.village.buildings.VillageIdentityApplier;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Native world and inventory coverage on a disposable server: -Dkithkyn.repairs.verify=true. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class RepairVerification {
  private static int ticks;
  private RepairVerification() { }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (!Boolean.getBoolean("kithkyn.repairs.verify") || ++ticks != 40) return;
    try {
      verify(event.getServer().overworld());
      Kithkyn.LOGGER.info("[repairs-verify] RESULT PASS: affordable repairs bypass missing stock; exact paid fetch and placement; explosion confirmation; persistence; player edits; mine excavation and special block protection");
    } catch (Exception | AssertionError failure) {
      Kithkyn.LOGGER.error("[repairs-verify] RESULT FAIL", failure);
    } finally {
      event.getServer().halt(false);
    }
  }

  private static void verify(ServerLevel level) {
    BlockPos origin = new BlockPos(-7200, 150, -7200);
    FixtureVillage village = new FixtureVillage(level, origin, "house_birch_forest_1", Rotation.NONE);
    for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-10, -1, -10), origin.offset(25, 8, 25))) {
      level.setBlock(pos, pos.getY() == origin.getY() - 1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
    }
    VillageManager.get(level).getVillages().put(village.getID(), village);
    RealPerson person = new RealPerson(PersonEntityType.PERSON.get(), level) {
      @Override public Village getVillage() { return village; }
    };
    person.setNoAi(true);
    RepairStore store = RepairStore.get(level);
    try {
      Repair unavailable = repair(village, origin.offset(1, 0, 1), Blocks.GOLD_BLOCK.defaultBlockState(), false);
      Repair affordable = repair(village, origin.offset(3, 0, 1), Blocks.COBBLESTONE.defaultBlockState(), false);
      remember(level, store, unavailable);
      remember(level, store, affordable);
      person.personMainInv.setItem(0, new ItemStack(Items.COBBLESTONE));
      stand(person, origin.offset(3, 0, 3));
      RepairStep step = new RepairStep();
      RepairStep.Job job = step.select(person);
      check(job != null && job.repair().equals(affordable) && !job.fetching(), "missing gold blocked affordable cobblestone");
      stand(person, job.stand());
      step.act(person, job);
      check(level.getBlockState(affordable.pos()).is(Blocks.COBBLESTONE), "builder did not restore cobblestone");
      check(person.personMainInv.countItem(Items.COBBLESTONE) == 0 && store.contains(unavailable), "repair did not pay exactly one block");

      level.setBlock(affordable.pos(), Blocks.AIR.defaultBlockState(), 2);
      store.remember(affordable);
      village.chest = origin.offset(3, 0, 5);
      level.setBlock(village.chest, Blocks.CHEST.defaultBlockState(), 3);
      Container chest = (Container) level.getBlockEntity(village.chest);
      chest.setItem(0, new ItemStack(Items.COBBLESTONE, 3));
      chest.setItem(1, new ItemStack(Items.SANDSTONE, 10));
      job = step.select(person);
      check(job != null && job.fetching(), "repair failed to fetch exact stock");
      stand(person, village.chest.west());
      step.act(person, job);
      check(person.personMainInv.countItem(Items.COBBLESTONE) == 3 && chest.countItem(Items.COBBLESTONE) == 0
          && chest.countItem(Items.SANDSTONE) == 10, "exact fetch changed or copied the wrong material");
      job = step.select(person);
      check(job != null && !job.fetching(), "fetched stock did not release deferred repair");
      stand(person, job.stand());
      step.act(person, job);
      check(person.personMainInv.countItem(Items.COBBLESTONE) == 2, "placement failed to spend fetched block");

      Repair replacement = repair(village, origin.offset(4, 0, 1), Blocks.COBBLESTONE.defaultBlockState(), false);
      remember(level, store, replacement);
      level.setBlock(replacement.pos(), Blocks.OAK_PLANKS.defaultBlockState(), 3);
      check(!BuildingRepairs.stillWanted(village, replacement), "repair would overwrite a replacement");
      store.forget(replacement.pos());
      level.setBlock(replacement.pos(), Blocks.AIR.defaultBlockState(), 2);
      PlacedBlockStore.get(level).markPlayerPlaced(replacement.pos());
      check(!BuildingRepairs.stillWanted(village, replacement), "player ownership failed to protect empty position");

      var bounds = com.quzzar.kithkyn.village.buildings.RedevelopmentPlanner.worldBounds(village, village.building);
      check(bounds != null, "house fixture has no measured footprint");
      BlockPos destroyed = new BlockPos(bounds.minX() - 1, bounds.minY(), bounds.minZ() + 3);
      BlockPos survived = destroyed.south();
      level.setBlock(destroyed, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
      level.setBlock(survived, Blocks.DIRT.defaultBlockState(), 3);
      BuildingRepairs.observeExplosion(level, List.of(destroyed, survived));
      level.setBlock(destroyed, Blocks.AIR.defaultBlockState(), 2);
      store.confirmExplosions(pos -> level.getBlockState(pos).isAir());
      Repair terrain = store.candidates(village.getID(), Long.MAX_VALUE, 100).stream()
          .filter(repair -> repair.pos().equals(destroyed)).findFirst().orElse(null);
      check(terrain != null && terrain.ground() && terrain.state().is(Blocks.DIRT),
          "exploded turf did not become dirt repair: pos=" + destroyed + ", bounds=" + bounds
              + ", ground=" + level.getBlockState(destroyed)
              + ", queued=" + store.candidates(village.getID(), Long.MAX_VALUE, 100));
      check(store.candidates(village.getID(), Long.MAX_VALUE, 100).stream().noneMatch(repair -> repair.pos().equals(survived)),
          "surviving explosion candidate became repair work");
      RepairStore restored = RepairStore.load(store.save(new CompoundTag(), level.registryAccess()), level.registryAccess());
      check(restored.contains(terrain) && restored.contains(unavailable), "damage did not survive save/load");
      // A crater's upper missing cell must not pin the lower supported layer.
      Repair upper = repair(village, destroyed.above(), Blocks.DIRT.defaultBlockState(), true);
      store.remember(upper);
      level.setBlock(destroyed.west(2), Blocks.DIRT.defaultBlockState(), 3);
      level.setBlock(destroyed.west(2).above(), Blocks.DIRT.defaultBlockState(), 3);
      person.personMainInv.setItem(2, new ItemStack(Items.DIRT, 2));
      stand(person, destroyed.west());
      for (int action = 0; action < 3 && !level.getBlockState(upper.pos()).is(Blocks.DIRT); action++) {
        job = step.select(person);
        check(job != null && !job.fetching(), "bottom-up crater restoration stalled");
        stand(person, job.stand());
        step.act(person, job);
      }
      check(level.getBlockState(destroyed).is(Blocks.DIRT) && level.getBlockState(upper.pos()).is(Blocks.DIRT)
          && person.personMainInv.countItem(Items.DIRT) == 0, "crater layers were not paid and restored bottom-up");

      check(BuildingRepairs.cost(level, origin, Blocks.CHEST.defaultBlockState()).isEmpty(), "chest NBT could be regenerated");
      check(BuildingRepairs.cost(level, origin, Blocks.OAK_DOOR.defaultBlockState()).isEmpty(), "door was priced as one cell");
      check(BuildingRepairs.cost(level, origin, Blocks.STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE)).getCount() == 2,
          "double slab did not cost two slabs");
      for (Rotation rotation : Rotation.values()) {
        FixtureVillage mine = new FixtureVillage(level, origin, "mine_birch_forest_1", rotation);
        MineShaft shaft = MineShaft.of(mine.building).getFirst();
        BlockPos corridor = shaft.mouth().offset(new BlockPos(2, -2, 0).rotate(shaft.rotation()));
        check(BuildingRepairs.inExcavation(mine, corridor), "repair can fill mine edge: " + rotation);
        check(BuildingRepairs.inExcavation(mine, shaft.entranceClearance()), "repair can close mine entrance: " + rotation);
        verifyOldShell(level, origin.offset(100 + rotation.ordinal() * 50, 0, 0), rotation);
      }
      check(person.getData(com.quzzar.kithkyn.entities.KithkynAttachments.PERSONAL_LOG.get()).entries().stream()
          .noneMatch(entry -> entry.kind().equals(com.quzzar.kithkyn.entities.PersonalLogData.KIND_BLOCKER)),
          "unavailable repairs created worker blockers");
    } finally {
      person.discard();
      VillageManager.get(level).getVillages().remove(village.getID());
    }
  }

  /** Real authored stairs prove position and facing reconstruction, while a removed ownership marker prevents rediscovery. */
  private static void verifyOldShell(ServerLevel level, BlockPos origin, Rotation rotation) {
    FixtureVillage village = new FixtureVillage(level, origin, "house_birch_forest_1", rotation);
    List<net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo> shell =
        BuildingRepairs.shell(level, village.building);
    var authored = shell.stream().filter(block -> block.nbt() == null && block.state().getBlock() instanceof StairBlock)
        .findFirst().orElseThrow(() -> new AssertionError("house fixture has no authored stairs"));
    BlockPos pos = origin.offset(authored.pos().rotate(rotation));
    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
    level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 2);
    PlacedBlockStore ownership = PlacedBlockStore.get(level);
    ownership.markVillagePlaced(pos);
    BlockState expected = VillageIdentityApplier.placement(village.building, village.getIdentity())
        .state(pos, authored.state().rotate(rotation));
    RepairStore store = RepairStore.get(level);
    RealPerson builder = new RealPerson(PersonEntityType.PERSON.get(), level) {
      @Override public Village getVillage() { return village; }
    };
    builder.setNoAi(true);
    stand(builder, origin);
    RepairStep step = new RepairStep();
    int scans = (shell.size() + 127) / 128 + 2;
    try {
      for (int scan = 0; scan < scans; scan++) step.select(builder);
      Repair found = store.candidates(village.getID(), Long.MAX_VALUE, 100).stream()
          .filter(repair -> repair.pos().equals(pos)).findFirst().orElse(null);
      check(found != null && found.state().equals(expected), "old damaged stair was not discovered with its rotated state: " + rotation);
      ownership.clearPlaced(pos);
      store.forget(pos);
      for (int scan = 0; scan < scans; scan++) step.select(builder);
      check(store.candidates(village.getID(), Long.MAX_VALUE, 100).isEmpty(), "player removal was rediscovered as damage: " + rotation);
    } finally {
      builder.discard();
    }
  }

  private static Repair repair(FixtureVillage village, BlockPos pos, BlockState state, boolean ground) {
    return new Repair(village.getID(), village.building.getUUID(), BuildingRepairs.revision(village.building), pos, state, ground);
  }

  private static void remember(ServerLevel level, RepairStore store, Repair repair) {
    if (!repair.ground()) PlacedBlockStore.get(level).markVillagePlaced(repair.pos());
    store.remember(repair);
  }

  private static void stand(RealPerson person, BlockPos pos) {
    person.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
    person.setOnGround(true);
  }

  private static void check(boolean condition, String reason) {
    if (!condition) throw new AssertionError(reason);
  }

  private static final class FixtureVillage extends Village {
    private final Building building;
    private BlockPos chest = BlockPos.ZERO;
    private FixtureVillage(ServerLevel level, BlockPos origin, String name, Rotation rotation) {
      super("Repair verification");
      building = new Building(origin, name, rotation);
      building.setCenterLocation(origin.asLong());
      attach(level);
    }
    @Override public Collection<Building> getBuildings() { return List.of(building); }
    @Override public Building getBuilding(UUID id) { return building.getUUID().equals(id) ? building : null; }
    @Override public BlockPos getNearestContainer(BlockPos from, Collection<BlockPos> excluding) {
      return excluding.contains(chest) ? BlockPos.ZERO : chest;
    }
  }
}
