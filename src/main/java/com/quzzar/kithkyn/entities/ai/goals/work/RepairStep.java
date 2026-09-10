package com.quzzar.kithkyn.entities.ai.goals.work;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;

import com.quzzar.kithkyn.Utils;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.savedata.PlacedBlockStore;
import com.quzzar.kithkyn.savedata.RepairStore;
import com.quzzar.kithkyn.savedata.RepairStore.Repair;
import com.quzzar.kithkyn.village.BuildingRepairs;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.buildings.Building;
import com.quzzar.kithkyn.village.buildings.VillageIdentityApplier;
import com.quzzar.kithkyn.village.buildings.WorkerFooting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Small paid repairs between projects. Missing materials and failed approaches quietly yield to other work. */
public final class RepairStep implements WorkStep<RepairStep.Job> {
  private static final int SURVEY_BLOCKS = 128;
  private static final int CANDIDATES = 32;
  private static final int RETRY_TICKS = 1200;
  private static final int LOAD = 8;
  private int buildingCursor;
  private int blockCursor;

  public record Job(Repair repair, BlockPos stand, boolean fetching) { }

  @Override
  @Nullable
  public Job select(RealPerson person) {
    Village village = person.getVillage();
    if (village == null || village.getCurrentProject() != null) return null;
    ServerLevel level = village.getLevel();
    RepairStore store = RepairStore.get(level);
    survey(village, store);
    long now = level.getGameTime();
    List<Repair> repairs = new ArrayList<>(store.candidates(village.getID(), now, CANDIDATES));
    // Within this low-priority work, restore lower footing before walls and roofs.
    repairs.sort(Comparator.comparingInt((Repair repair) -> repair.ground() ? 0 : 1)
        .thenComparingInt(repair -> repair.pos().getY())
        .thenComparingDouble(repair -> repair.pos().distSqr(person.blockPosition())));
    Job fetch = null;
    int pathBudget = 8;
    for (Repair repair : repairs) {
      if (!level.hasChunkAt(repair.pos())) {
        store.defer(repair.pos(), now + RETRY_TICKS);
        continue;
      }
      if (!BuildingRepairs.stillWanted(village, repair)) {
        store.forget(repair.pos());
        continue;
      }
      ItemStack cost = BuildingRepairs.cost(level, repair.pos(), repair.state());
      if (cost.isEmpty()) {
        store.forget(repair.pos());
        continue;
      }
      if (!BuildingRepairs.supported(level, repair)) {
        store.defer(repair.pos(), now + 100);
        continue;
      }
      if (PackLogistics.carriedExact(person, cost) < cost.getCount()) {
        if (fetch == null) {
          BlockPos chest = PackLogistics.chestWhere(person, village,
              stack -> ItemStack.isSameItemSameComponents(stack, cost));
          if (chest != null) fetch = new Job(repair, chest, true);
        }
        store.defer(repair.pos(), now + RETRY_TICKS);
        continue;
      }
      if (pathBudget <= 0) break;
      // Test a few nearby safe surfaces, never the missing block itself or the space it will occupy.
      List<BlockPos> stands = stands(person, repair);
      for (BlockPos stand : stands) {
        if (--pathBudget < 0) break;
        Path path = person.getNavigation().createPath(stand, 0);
        if (person.position().distanceToSqr(Vec3.atBottomCenterOf(stand)) <= 0.5D
            || path != null && path.canReach() && path.getEndNode() != null
                && path.getEndNode().asBlockPos().equals(stand)) return new Job(repair, stand, false);
      }
      store.defer(repair.pos(), now + RETRY_TICKS);
    }
    return fetch;
  }

  /** Ownership left behind by old non-player destruction is evidence; missing unowned template cells are not. */
  private void survey(Village village, RepairStore store) {
    List<Building> buildings = List.copyOf(village.getBuildings());
    if (buildings.isEmpty()) return;
    buildingCursor %= buildings.size();
    Building building = buildings.get(buildingCursor);
    ServerLevel level = village.getLevel();
    List<StructureTemplate.StructureBlockInfo> shell = BuildingRepairs.shell(level, building);
    if (blockCursor >= shell.size()) {
      blockCursor = 0;
      buildingCursor = (buildingCursor + 1) % buildings.size();
      return;
    }
    PlacedBlockStore placed = PlacedBlockStore.get(level);
    VillageIdentityApplier.Placement identity = VillageIdentityApplier.placement(building, village.getIdentity());
    BlockPos origin = BlockPos.of(building.getOriginLocation());
    int end = Math.min(shell.size(), blockCursor + SURVEY_BLOCKS);
    while (blockCursor < end) {
      StructureTemplate.StructureBlockInfo block = shell.get(blockCursor++);
      if (block.state().isAir() || block.nbt() != null) continue;
      BlockPos pos = origin.offset(block.pos().rotate(building.getRotation()));
      if (!level.hasChunkAt(pos) || !level.getBlockState(pos).isAir()
          || !placed.isVillagePlaced(pos) || placed.isPlayerPlaced(pos) || BuildingRepairs.inExcavation(village, pos)) continue;
      BlockState state = identity.state(pos, block.state().rotate(building.getRotation()));
      if (BuildingRepairs.cost(level, pos, state).isEmpty()) continue;
      store.remember(new Repair(village.getID(), building.getUUID(), BuildingRepairs.revision(building), pos, state, false));
    }
  }

  private static List<BlockPos> stands(RealPerson person, Repair repair) {
    BlockPos block = repair.pos();
    List<BlockPos> result = new ArrayList<>();
    for (int dx = -2; dx <= 2; dx++) {
      for (int dz = -2; dz <= 2; dz++) {
        if (dx == 0 && dz == 0 || dx * dx + dz * dz > 5) continue;
        for (int dy = -2; dy <= 2; dy++) {
          BlockPos stand = block.offset(dx, dy, dz);
          if ((!repair.ground() || stand.getY() >= block.getY())
              && stand.distSqr(block) <= 9 && WorkerFooting.canStand(person, stand)) result.add(stand);
        }
      }
    }
    result.sort(Comparator.comparingDouble(pos -> person.position().distanceToSqr(Vec3.atBottomCenterOf(pos))));
    return result;
  }

  @Override
  public boolean act(RealPerson person, Job job) {
    Village village = person.getVillage();
    if (village == null || village.getCurrentProject() != null) return false;
    ServerLevel level = village.getLevel();
    RepairStore store = RepairStore.get(level);
    Repair repair = job.repair();
    if (!level.hasChunkAt(repair.pos()) || !store.contains(repair)
        || !BuildingRepairs.stillWanted(village, repair) || !inReach(person, job)) return false;
    ItemStack cost = BuildingRepairs.cost(level, repair.pos(), repair.state());
    if (cost.isEmpty()) return false;
    if (job.fetching()) {
      Container chest = PackLogistics.containerAt(person, job.stand());
      if (chest != null && PackLogistics.pullExact(person, chest, cost.copyWithCount(LOAD)) > 0) {
        // All cells paid by this same material can now compete again without waiting out an unavailable retry.
        for (Repair candidate : store.candidates(village.getID(), Long.MAX_VALUE, RepairStore.MAX_REPAIRS)) {
          if (candidate.state().getBlock().asItem() == cost.getItem()) store.defer(candidate.pos(), 0);
        }
      }
      return false;
    }
    if (!BuildingRepairs.supported(level, repair)
        || !level.getEntitiesOfClass(LivingEntity.class, new AABB(repair.pos())).isEmpty()) {
      store.defer(repair.pos(), level.getGameTime() + 100);
      return false;
    }
    if (PackLogistics.carriedExact(person, cost) < cost.getCount()) return false;
    ItemStack paid = Utils.removeItem(person.personMainInv, cost, cost.getCount());
    if (level.setBlock(repair.pos(), repair.state(), Block.UPDATE_ALL)) {
      if (!repair.ground()) PlacedBlockStore.get(level).markVillagePlaced(repair.pos());
      store.forget(repair.pos());
      level.playSound(null, repair.pos(), repair.state().getSoundType().getPlaceSound(), SoundSource.BLOCKS,
          1.0F, person.getRandom().nextFloat() * 0.3F + 0.85F);
    } else {
      Utils.insertItems(person.personMainInv, List.of(paid), person);
      store.defer(repair.pos(), level.getGameTime() + RETRY_TICKS);
    }
    return false;
  }

  @Override
  public void unreachable(RealPerson person, Job job) {
    RepairStore.get((ServerLevel) person.level()).defer(job.repair().pos(), person.level().getGameTime() + RETRY_TICKS);
  }

  @Override public BlockPos positionOf(Job job) { return job.stand(); }
  @Override public String describe() { return "a small village repair"; }
  @Override public String activity() { return "repairing damaged village blocks"; }
  @Override public int selectEveryTicks() { return 40; }
  @Override public boolean requiresExactArrival() { return true; }

  @Override
  public boolean inReach(RealPerson person, Job job) {
    return job.fetching() ? person.blockPosition().distSqr(job.stand()) <= 6.0D
        : person.position().distanceToSqr(Vec3.atBottomCenterOf(job.stand())) <= 0.8D
            && WorkerFooting.canStand(person, job.stand()) && person.blockPosition().distSqr(job.repair().pos()) <= 9.0D
            && (!job.repair().ground() || person.blockPosition().getY() >= job.repair().pos().getY());
  }
}
