package com.quzzar.kithkyn.dev;

import java.util.ArrayList;
import java.util.List;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.entities.AgeStage;
import com.quzzar.kithkyn.entities.JobTool;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.entities.ai.goals.work.ContainerAccess;
import com.quzzar.kithkyn.village.GuardDuty;
import com.quzzar.kithkyn.village.GuardRole;
import com.quzzar.kithkyn.village.JobAssignment;
import com.quzzar.kithkyn.village.Occupation;
import com.quzzar.kithkyn.village.PersonalChest;
import com.quzzar.kithkyn.village.buildings.Building;
import com.quzzar.kithkyn.village.buildings.BuildingInfo;
import com.quzzar.kithkyn.village.buildings.Buildings;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Opt-in access and room checks for the private Pueblo review pack, never a live-world operation. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class ApprovedCenterVerification {
  private static final BlockPos ORIGIN = new BlockPos(2400, 159, 2400);
  private static int ticks;
  private static int rotationIndex;
  private static int walkIndex;
  private static int walkStarted;
  private static int routes;
  private static Building building;
  private static ApprovedStructureAccess.VillageFixture village;
  private static ApprovedStructureAccess.Person walker;
  private static List<BlockPos> posts;
  private static final List<ApprovedStructureAccess.Person> residents = new ArrayList<>();

  private ApprovedCenterVerification() { }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (!Boolean.getBoolean("kithkyn.approvedCenter.verify")) return;
    ticks++;
    ServerLevel level = event.getServer().overworld();
    try {
      if (ticks < 40) return;
      if (ticks == 40) {
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, event.getServer());
        level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, event.getServer());
        level.setDayTime(6000);
        for (int x = 147; x <= 153; x++) {
          for (int z = 147; z <= 153; z++) level.setChunkForced(x, z, true);
        }
        event.getServer().tickRateManager().setTickRate(100.0F);
      }
      if (building == null) {
        if (rotationIndex == Rotation.values().length) {
          Kithkyn.LOGGER.info("[approved-center-verify] RESULT PASS: 4 rotations, {} access routes, 8 real rooftop walks, 10 beds, 7 private and 3 communal containers, room/dependent ownership and mixed guard kits", routes);
          event.getServer().halt(false);
          return;
        }
        prepare(level);
      } else {
        walk(level);
      }
    } catch (Exception | AssertionError failure) {
      Kithkyn.LOGGER.error("[approved-center-verify] RESULT FAIL", failure);
      event.getServer().halt(false);
    }
  }

  private static void prepare(ServerLevel level) throws ReflectiveOperationException {
    BuildingInfo info = Buildings.getByName(System.getProperty("kithkyn.approvedCenter.id", "village_center_pueblo_1"));
    check(info != null, "The approved Pueblo review datapack was not loaded");
    check(info.validate() == null, "Invalid Pueblo metadata: " + info.validate());
    check(info.getBedLocations().size() == 10, "Expected 10 beds");
    check(info.getPersonalContainerLocations().size() == 7, "Expected 7 personal containers");
    check(info.getContainerLocations().size() == 3, "Expected 3 communal containers");
    building = ApprovedStructureAccess.place(level, ORIGIN, info, Rotation.values()[rotationIndex]);
    village = new ApprovedStructureAccess.VillageFixture(level, building, true);
    walker = new ApprovedStructureAccess.Person(level, village);
    walker.setNoAi(true);
    moveToMeeting(walker);

    for (long bed : info.getBedLocations()) {
      check(level.getBlockState(world(BlockPos.of(bed))).getBlock() instanceof BedBlock,
          "Authored bed is absent at " + BlockPos.of(bed));
      route(world(BlockPos.of(bed)), false);
    }
    for (long chest : info.getPersonalContainerLocations()) verifyContainer(level, chest);
    for (long chest : info.getContainerLocations()) verifyContainer(level, chest);
    verifyRooms(level, info);
    posts = new ArrayList<>();
    int station = 0;
    int captains = 0;
    int patrols = 0;
    for (var entry : info.getWorkLocations().entrySet()) {
      if (entry.getValue() == Occupation.GUARD) {
        village.assignJob(walker.getUUID(), new JobAssignment(walker.getUUID(), Occupation.GUARD,
            building.getUUID(), station));
        walker.setOccupation(Occupation.GUARD);
        walker.issueStartingKit();
        GuardRole role = info.getGuardRole(station);
        check(GuardDuty.isCaptain(walker) == (role == GuardRole.CAPTAIN), "Incorrect captain title at " + station);
        GuardDuty duty = GuardDuty.of(walker);
        if (role == GuardRole.CROSSBOW_POST) {
          check(duty != null && duty.ranged() && duty.backupSword(), "Crossbow station lacks its fixed duty");
          check(JobTool.of(walker) == JobTool.CROSSBOW && walker.getMainHandItem().is(Items.CROSSBOW),
              "Crossbow sentry did not start with a crossbow");
          posts.add(duty.position());
          route(duty.position(), true);
        } else {
          check(duty == null, "A roaming guard received a fixed post");
          if (role == GuardRole.CAPTAIN) captains++;
          if (role == GuardRole.PATROL) {
            patrols++;
            check(JobTool.of(walker) == JobTool.SWORD && walker.getMainHandItem().is(Items.STONE_SWORD),
                "Sword patrol did not receive its stone sword");
          }
        }
      }
      station++;
    }
    check(captains == 1 && patrols == 2 && posts.size() == 2, "Expected captain, 2 patrols and 2 crossbow posts");
    Kithkyn.LOGGER.info("[approved-center-verify] ACCESS PASS {}: beds, containers, rooms and guards", building.getRotation());
    village.releaseJob(walker.getUUID());
    walker.setOccupation(Occupation.WANDERER);
    ApprovedStructureAccess.enableWalking(walker);
    moveToMeeting(walker);
    check(level.addFreshEntity(walker), "Could not spawn the physical walker");
    walkIndex = 0;
    walkStarted = ticks;
  }

  private static void verifyContainer(ServerLevel level, long local) {
    BlockPos target = world(BlockPos.of(local));
    check(level.getBlockEntity(target) instanceof Container, "Missing container at " + BlockPos.of(local));
    moveToMeeting(walker);
    BlockPos approach = ContainerAccess.approachTo(walker, target, 9.0D);
    check(approach != null, "No physical container approach at " + BlockPos.of(local) + " " + building.getRotation());
    check(ContainerAccess.canReach(walker, Vec3.atBottomCenterOf(approach).add(0, walker.getEyeHeight(), 0),
        target, 9.0D), "Container approach reaches through a wall");
    routes++;
  }

  private static void route(BlockPos target, boolean exact) {
    Path path = ApprovedStructureAccess.route(walker, village.getGatheringPoint(), target,
        exact ? 0 : 1, exact ? 0 : 4);
    check(path != null && path.getEndNode() != null
        && path.getEndNode().asBlockPos().distSqr(target) <= (exact ? 0 : 4),
        "Unreachable " + (exact ? "post" : "bed") + " at " + target.subtract(ORIGIN)
            + " " + building.getRotation() + "; end=" + (path == null || path.getEndNode() == null
                ? "none" : path.getEndNode().asBlockPos().subtract(ORIGIN)));
    routes++;
  }

  private static void verifyRooms(ServerLevel level, BuildingInfo info) throws ReflectiveOperationException {
    for (int index = 0; index < info.getBedLocations().size(); index++) {
      ApprovedStructureAccess.Person resident = new ApprovedStructureAccess.Person(level, village);
      resident.setNoAi(true);
      resident.setLifeStage(AgeStage.ADULT);
      village.getPopulation().add(resident.getUUID());
      ApprovedStructureAccess.assignSingle(village, resident.getUUID(), building.getUUID());
      check(village.getBedAssignment(resident.getUUID()).getBedIndex() == index, "Unexpected bed assignment order");
      var binding = info.getBedContainers().get(index);
      BlockPos expected = binding.containers().isEmpty() ? null : world(binding.containers().getFirst());
      check(java.util.Objects.equals(expected, PersonalChest.of(resident)), "Resident borrowed another room's chest at bed " + index);
      moveToMeeting(resident);
      check(level.addFreshEntity(resident), "Could not register the room resident");
      residents.add(resident);
    }
    ApprovedStructureAccess.Person child = new ApprovedStructureAccess.Person(level, village);
    child.setNoAi(true);
    child.setLifeStage(AgeStage.KID);
    child.setParents(residents.getFirst(), residents.get(1));
    village.getPopulation().add(child.getUUID());
    check(PersonalChest.of(child).equals(PersonalChest.of(residents.getFirst())), "Child did not inherit the parent's exact room chest");
    moveToMeeting(child);
    check(level.addFreshEntity(child), "Could not register the dependent");
    check(PersonalChest.housemateNames(residents.getFirst()).equals(List.of(child.getFullName())),
        "A personal chest was described as shared with the entire town center");
    child.setParents(residents.get(4), residents.getFirst());
    check(PersonalChest.of(child) == null, "Child borrowed a different parent's chest despite living in the no-container room");
    child.discard();
    residents.forEach(RealPerson::discard);
    residents.clear();
  }

  private static void walk(ServerLevel level) {
    BlockPos target = posts.get(walkIndex);
    // GuardPostGoal holds watch within 1.5 blocks. Also require the actual roof
    // level so a body on the last ladder rung cannot masquerade as arrival.
    if (walker.distanceToSqr(Vec3.atBottomCenterOf(target)) <= 2.25D
        && Math.abs(walker.getY() - target.getY()) < 0.51D) {
      Kithkyn.LOGGER.info("[approved-center-verify] WALK PASS {} post {} in {} ticks", building.getRotation(), walkIndex + 1, ticks - walkStarted);
      walkIndex++;
      if (walkIndex == posts.size()) {
        walker.discard();
        residents.forEach(RealPerson::discard);
        residents.clear();
        building = null;
        rotationIndex++;
        return;
      }
      moveToMeeting(walker);
      walkStarted = ticks;
      return;
    }
    check(ticks - walkStarted < 1600, "Physical rooftop walk stalled at "
        + walker.position().subtract(Vec3.atLowerCornerOf(ORIGIN)) + " toward " + target.subtract(ORIGIN)
        + " in " + building.getRotation());
    if (ticks - walkStarted > 5 && (walker.getNavigation().isDone() || ticks % 10 == 0)) {
      walker.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 0.6D);
    }
  }

  private static BlockPos world(BlockPos local) {
    return ORIGIN.offset(local.rotate(building.getRotation()));
  }

  private static void moveToMeeting(RealPerson person) {
    ApprovedStructureAccess.moveTo(person, village.getGatheringPoint());
  }

  private static void check(boolean condition, String reason) {
    if (!condition) throw new AssertionError(reason);
  }

}
