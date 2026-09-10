package com.quzzar.kithkyn.dev;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.quzzar.kithkyn.PersonEntityType;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.entities.ai.goals.OpenFenceGateGoal;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.buildings.Building;
import com.quzzar.kithkyn.village.buildings.BuildingInfo;
import com.quzzar.kithkyn.village.buildings.BuildingFootprint;
import com.quzzar.kithkyn.village.buildings.InstantBuildStructure;
import com.quzzar.kithkyn.village.buildings.MineShaft;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;

/** Shared native placement, navigation and resident fixtures for approved private structures. */
final class ApprovedStructureAccess {
  private ApprovedStructureAccess() { }

  /** The tight authored envelope in world coordinates, excluding capture padding. */
  static BoundingBox footprint(ServerLevel level, Building building) {
    var template = level.getStructureManager().getOrCreate(ResourceLocation.fromNamespaceAndPath(
        com.quzzar.kithkyn.Kithkyn.MODID, building.getInfo().getPath()));
    BlockPos origin = BlockPos.of(building.getOriginLocation());
    return BuildingFootprint.bounds(template, building.getRotation()).moved(origin.getX(), origin.getY(), origin.getZ());
  }

  /** Snapshot used to identify the exact entities added by a native structure placement. */
  static Set<UUID> entityIds(ServerLevel level) {
    Set<UUID> ids = new HashSet<>();
    level.getAllEntities().forEach(entity -> ids.add(entity.getUUID()));
    return ids;
  }

  static Building place(ServerLevel level, BlockPos origin, BuildingInfo info, Rotation rotation) {
    int groundY = origin.getY() + info.getSink();
    for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-36, -3, -36), origin.offset(36, 25, 36))) {
      level.setBlock(pos, pos.getY() <= groundY ? Blocks.STONE.defaultBlockState()
          : Blocks.AIR.defaultBlockState(), 2);
    }
    Building building = new Building(origin, info.getName(), rotation);
    if (!new InstantBuildStructure(building, new java.util.Random(1), level)
        .seatAtOrigin(origin, new HashSet<>()).buildInstantly()) {
      throw new AssertionError("Placement failed for " + info.getName() + " " + rotation);
    }
    // Replacing a container drops its previous test contents; do not feed those to the next walker.
    level.getEntitiesOfClass(ItemEntity.class, new AABB(origin.getX() - 38, origin.getY() - 4, origin.getZ() - 38,
        origin.getX() + 38, origin.getY() + 27, origin.getZ() + 38))
        .forEach(ItemEntity::discard);
    return building;
  }

  static void moveTo(RealPerson person, BlockPos position) {
    person.getNavigation().stop();
    person.moveTo(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D, 0, 0);
    person.setOnGround(true);
  }

  /** Replays the first ramp, seeding only underground terrain and retaining everything above the mouth. */
  static void excavateMine(ServerLevel level, MineShaft shaft, int surfaceY) {
    for (BlockPos local : BlockPos.betweenClosed(-shaft.radius(), -13, -2, shaft.radius(), -1, 8)) {
      BlockPos position = shaft.mouth().offset(local.rotate(shaft.rotation()));
      if (position.getY() < surfaceY) level.setBlock(position, Blocks.STONE.defaultBlockState(), 2);
    }
    for (BlockPos local : BlockPos.betweenClosed(-shaft.radius(), -12, -1, shaft.radius(), -1, 8)) {
      if (MineShaft.withinCorridor(local, shaft.radius())) {
        level.setBlock(shaft.mouth().offset(local.rotate(shaft.rotation())), Blocks.AIR.defaultBlockState(), 2);
      }
    }
    // Include the slightly lowered support left by normal path wear.
    level.setBlock(shaft.entry().below(), Blocks.DIRT_PATH.defaultBlockState(), 2);
  }

  /** Replans from each reachable partial endpoint as ordinary sleep and movement goals do. */
  static Path route(RealPerson person, BlockPos start, BlockPos target, int accuracy, double reachSquared) {
    moveTo(person, start);
    Path path = person.getNavigation().createPath(target, accuracy);
    HashSet<BlockPos> reached = new HashSet<>();
    for (int hop = 0; path != null && path.getEndNode() != null && hop < 8; hop++) {
      BlockPos end = path.getEndNode().asBlockPos();
      if (end.distSqr(target) <= reachSquared || !reached.add(end)) break;
      moveTo(person, end);
      path = person.getNavigation().createPath(target, accuracy);
    }
    return path;
  }

  /** Keeps native physics and opening doors, excluding unrelated work, social and combat behavior. */
  static void enableWalking(RealPerson person) {
    person.setPersistenceRequired();
    person.setNoAi(false);
    person.goalSelector.removeAllGoals(goal -> !(goal instanceof OpenDoorGoal) && !(goal instanceof OpenFenceGateGoal));
    person.targetSelector.removeAllGoals(goal -> true);
  }

  /** The runtime single-bed allocation path, without invoking unrelated arrival conversations. */
  static void assignSingle(Village village, UUID person, UUID building) throws ReflectiveOperationException {
    var assign = Village.class.getDeclaredMethod("preferWorkplaceBed", UUID.class, UUID.class);
    assign.setAccessible(true);
    assign.invoke(village, person, building);
  }

  /** Runs the normal housing pass after worker claims, leaving reserved rooms to their owners. */
  static void reconcileBeds(Village village) throws ReflectiveOperationException {
    var reconcile = Village.class.getDeclaredMethod("reconcileBeds");
    reconcile.setAccessible(true);
    reconcile.invoke(village);
  }

  static final class Person extends RealPerson {
    private final Village home;
    Person(ServerLevel level, Village home) {
      super(PersonEntityType.PERSON.get(), level);
      this.home = home;
    }
    @Override public Village getVillage() { return home; }
  }

  /** Uses the real building and bed ledgers without unrelated village planning. */
  static final class VillageFixture extends Village {
    private final Building center;
    VillageFixture(ServerLevel level, Building building, boolean asCenter, Building... additionalBuildings) {
      super("Approved structure access fixture");
      center = asCenter ? building : null;
      attach(level);
      addBuilding(building);
      for (Building additional : additionalBuildings) addBuilding(additional);
    }
    @Override public Building getTownCenter() { return center; }
    @Override public void update(ServerLevel ignored) { }
  }
}
