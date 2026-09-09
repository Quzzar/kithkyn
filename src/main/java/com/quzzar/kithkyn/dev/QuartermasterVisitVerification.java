package com.quzzar.kithkyn.dev;

import java.util.List;
import java.util.UUID;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.PersonEntityType;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.entities.ai.goals.work.ConsolidateStep;
import com.quzzar.kithkyn.entities.ai.goals.work.ContainerVisit;
import com.quzzar.kithkyn.entities.ai.goals.work.WorkLoopGoal;
import com.quzzar.kithkyn.village.JobAssignment;
import com.quzzar.kithkyn.village.Occupation;
import com.quzzar.kithkyn.village.Storehouse;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.buildings.Building;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Opt-in native route/lifecycle fixtures, only in a disposable world: -Dkithkyn.quartermaster.verify=true. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class QuartermasterVisitVerification {
  private static int ticks;

  private QuartermasterVisitVerification() { }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (!Boolean.getBoolean("kithkyn.quartermaster.verify") || ++ticks != 40) return;
    try {
      verify(event.getServer().overworld());
      Kithkyn.LOGGER.info("[quartermaster-verify] RESULT PASS: physical shelf visits, paced transfers, full stores, night and removal cleanup");
    } catch (Exception | AssertionError failure) {
      Kithkyn.LOGGER.error("[quartermaster-verify] RESULT FAIL", failure);
    } finally {
      event.getServer().halt(false);
    }
  }

  private static void verify(ServerLevel level) {
    level.setDayTime(6000);
    level.updateSkyBrightness();
    BlockPos origin = new BlockPos(-6200, 150, -6200);
    FixtureVillage village = new FixtureVillage(origin);
    RealPerson worker = new RealPerson(PersonEntityType.PERSON.get(), level) {
      @Override public Village getVillage() { return village; }
    };
    worker.setNoAi(true);
    for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-8, -1, -2), origin.offset(16, 5, 17))) {
      level.setBlock(pos, pos.getY() <= origin.getY() ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
    }
    List<BlockPos> shelves = Storehouse.chests(worker);
    check(shelves.size() > 1, "fixture requires multiple physical storehouse containers");
    for (BlockPos shelf : shelves) level.setBlock(shelf, Blocks.BARREL.defaultBlockState(), 3);
    level.setBlock(village.source, Blocks.CHEST.defaultBlockState(), 3);
    stand(worker, origin.offset(7, 1, 6));
    level.addFreshEntity(worker);

    // A delivery to one barrel cannot write to another barrel ten ticks before it is visited.
    Container first = container(level, shelves.getFirst());
    for (int slot = 0; slot < first.getContainerSize(); slot++) first.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
    first.setItem(0, new ItemStack(Items.APPLE, 63));
    worker.personMainInv.setItem(0, new ItemStack(Items.APPLE, 2));
    ConsolidateStep delivery = new ConsolidateStep();
    BlockPos target = delivery.select(worker);
    check(shelves.getFirst().equals(target), "delivery did not select the available first shelf");
    check(!delivery.act(worker, target), "quartermaster acted before arriving at the selected foothold");
    check(first.countItem(Items.APPLE) == 63, "a remote delivery changed the shelf");
    stand(worker, delivery.positionOf(target));
    delivery.acquired(worker, target);
    check(delivery.act(worker, target), "visit failed to open");
    check(level.getBlockState(target).getValue(BarrelBlock.OPEN), "barrel did not visibly open");
    check(first.countItem(Items.APPLE) == 63, "opening a container moved goods immediately");
    ((net.minecraft.world.level.storage.ServerLevelData) level.getLevelData()).setGameTime(level.getGameTime() + 29);
    delivery.act(worker, target);
    check(first.countItem(Items.APPLE) == 63, "transfer ignored its pacing");
    ((net.minecraft.world.level.storage.ServerLevelData) level.getLevelData()).setGameTime(level.getGameTime() + 1);
    delivery.act(worker, target);
    check(first.countItem(Items.APPLE) == 64 && worker.personMainInv.countItem(Items.APPLE) == 1,
        "single-shelf delivery lost goods or overfilled the shelf");
    check(container(level, shelves.get(1)).isEmpty(), "delivery remotely changed another shelf");
    delivery.released(worker, target);
    check(!level.getBlockState(target).getValue(BarrelBlock.OPEN), "released visit left the barrel open");
    BlockPos next = delivery.select(worker);
    check(next != null && !next.equals(target), "remaining goods did not route to another physical shelf");
    delivery.released(worker, next);

    // Full stores retain the pack and produce a bounded inspection, never repeated collection from town.
    for (BlockPos shelf : shelves) {
      Container full = container(level, shelf);
      for (int slot = 0; slot < full.getContainerSize(); slot++) full.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
    }
    container(level, village.source).setItem(0, new ItemStack(Items.DIAMOND, 20));
    ConsolidateStep blocked = new ConsolidateStep();
    BlockPos inspection = blocked.select(worker);
    check(inspection != null && !inspection.equals(village.source), "full stores kept sending the worker for more goods");
    check(worker.personMainInv.countItem(Items.APPLE) == 1, "full stores discarded carried goods");
    check(container(level, village.source).countItem(Items.DIAMOND) == 20, "full stores remotely collected more goods");
    blocked.released(worker, inspection);
    worker.personMainInv.clearContent();
    container(level, village.source).clearContent();
    for (BlockPos shelf : shelves) container(level, shelf).clearContent();

    ConsolidateStep rounds = new ConsolidateStep();
    BlockPos firstVisit = rounds.select(worker);
    stand(worker, rounds.positionOf(firstVisit));
    rounds.act(worker, firstVisit);
    rounds.released(worker, firstVisit);
    BlockPos secondVisit = rounds.select(worker);
    check(secondVisit != null && !secondVisit.equals(firstVisit), "quiet quartermaster stayed at the same container");
    rounds.released(worker, secondVisit);

    ConsolidateStep nightStep = new ConsolidateStep();
    BlockPos nightTarget = nightStep.select(worker);
    stand(worker, nightStep.positionOf(nightTarget));
    WorkLoopGoal<BlockPos> loop = new WorkLoopGoal<>(worker, nightStep);
    check(loop.canUse(), "quartermaster loop did not find a daylight visit");
    loop.start();
    loop.tick();
    check(level.getBlockState(nightTarget).getValue(BarrelBlock.OPEN), "work loop did not open the visited barrel");
    level.setDayTime(18000);
    level.updateSkyBrightness();
    check(!loop.canContinueToUse(), "quartermaster work did not yield at night");
    loop.stop();
    check(!level.getBlockState(nightTarget).getValue(BarrelBlock.OPEN), "night left a barrel open");

    level.setDayTime(6000);
    level.updateSkyBrightness();
    ContainerVisit.open(worker, nightTarget);
    check(level.getBlockState(nightTarget).getValue(BarrelBlock.OPEN), "removal fixture did not open");
    worker.discard();
    check(!level.getBlockState(nightTarget).getValue(BarrelBlock.OPEN), "entity removal left a barrel open");
  }

  private static Container container(ServerLevel level, BlockPos pos) {
    return (Container) level.getBlockEntity(pos);
  }

  private static void stand(RealPerson worker, BlockPos pos) {
    worker.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
    worker.setOnGround(true);
  }

  private static void check(boolean condition, String reason) {
    if (!condition) throw new AssertionError(reason);
  }

  private static final class FixtureVillage extends Village {
    private final Building building;
    private final BlockPos source;

    private FixtureVillage(BlockPos origin) {
      super("Quartermaster visit fixture");
      this.building = new Building(origin, "storehouse_birch_forest_1", Rotation.NONE);
      this.source = origin.offset(-6, 1, 8);
    }

    @Override public Building getBuilding(UUID id) { return this.building; }
    @Override public JobAssignment getJobAssignment(UUID id) {
      return new JobAssignment(id, Occupation.QUARTERMASTER, this.building.getUUID(), 0);
    }
    @Override public BlockPos getNearestContainer(BlockPos from, java.util.Collection<BlockPos> skip) {
      return skip.contains(this.source) ? BlockPos.ZERO : this.source;
    }
  }
}
