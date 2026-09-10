package com.quzzar.kithkyn.dev;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.entities.AgeStage;
import com.quzzar.kithkyn.relationships.RelationshipPair;
import com.quzzar.kithkyn.village.BedAssignment;
import com.quzzar.kithkyn.village.JobAssignment;
import com.quzzar.kithkyn.village.Occupation;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.VillageRuler;
import com.quzzar.kithkyn.village.buildings.Building;
import com.quzzar.kithkyn.village.buildings.Buildings;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Native role-room transfers against the edited castle, including blocked destinations and save reloads. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class CastleRolesVerification {
  private static final String PREFIX = "[castle-roles-verify]";
  private static int ticks;
  private static int rotations;
  private static boolean finished;

  private CastleRolesVerification() {}

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (!Boolean.getBoolean("kithkyn.castleRoles.verify") || finished) return;
    ServerLevel level = event.getServer().overworld();
    try {
      if (++ticks < 40) return;
      if (ticks == 40) {
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, event.getServer());
        level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, event.getServer());
        level.setDayTime(6000);
        for (int x = 147; x <= 153; x++) {
          for (int z = 147; z <= 159; z++) level.setChunkForced(x, z, true);
        }
      }
      if (ticks < 45) return;
      verify(level, Rotation.values()[rotations]);
      if (++rotations == Rotation.values().length) {
        finished = true;
        Kithkyn.LOGGER.info(PREFIX + " RESULT PASS: four native rotations; blocked royal and missing captain beds "
            + "preserve old homes; restored rooms relocate existing workers; royal spouse pairing, reload, "
            + "ruler availability and vacancy reconciliation");
        event.getServer().halt(false);
      }
    } catch (Exception | AssertionError failure) {
      finished = true;
      Kithkyn.LOGGER.error(PREFIX + " RESULT FAIL", failure);
      event.getServer().halt(false);
    }
  }

  private static void verify(ServerLevel level, Rotation rotation) throws ReflectiveOperationException {
    BlockPos origin = new BlockPos(2400, 159, 2400);
    Building castle = ApprovedStructureAccess.place(level, origin, Buildings.getByName("castle_desert_1"), rotation);
    Building center = ApprovedStructureAccess.place(level, origin.offset(0, 0, 96),
        Buildings.getByName("village_center_desert_1"), rotation);
    Fixture village = new Fixture(level, center, castle);
    check(castle.getInfo().getRoomReservations().size() == 2, "Review pack needs royal and captain reservations");
    check(village.getFreeGeneralBedCount() == center.getInfo().getSingleBedCount() + 5,
        "Castle reservations leaked into general housing");
    List<ApprovedStructureAccess.Person> residents = new ArrayList<>();
    var ruler = person(level, village, origin.above(), residents);
    var captain = person(level, village, origin.above(), residents);
    var spouse = person(level, village, origin.above(), residents);
    ApprovedStructureAccess.assignSingle(village, ruler.getUUID(), center.getUUID());
    ApprovedStructureAccess.assignSingle(village, captain.getUUID(), center.getUUID());
    ApprovedStructureAccess.assignSingle(village, spouse.getUUID(), center.getUUID());
    BedAssignment oldRuler = village.getBedAssignment(ruler.getUUID());
    BedAssignment oldCaptain = village.getBedAssignment(captain.getUUID());
    check(oldRuler != null && oldCaptain != null, "Fixture workers need existing homes");

    BlockPos royalBed = world(castle, new BlockPos(27, 6, 15));
    BlockPos captainBed = world(castle, new BlockPos(22, 1, 19));
    var royalAbove = level.getBlockState(royalBed.above());
    var captainState = level.getBlockState(captainBed);
    BlockPos captainFoot = captainBed.relative(captainState.getValue(net.minecraft.world.level.block.BedBlock.FACING).getOpposite());
    var captainFootState = level.getBlockState(captainFoot);
    level.setBlock(royalBed.above(), Blocks.STONE.defaultBlockState(), 2);
    level.setBlock(captainBed, Blocks.AIR.defaultBlockState(), 2);
    JobAssignment rulerJob = village.getUnassignedJobs().stream()
        .filter(job -> job.getOccupation() == Occupation.LEADER).findFirst().orElseThrow();
    JobAssignment captainJob = village.getUnassignedJobs().stream()
        .filter(job -> job.getOccupation() == Occupation.GUARD && job.getBuildingUUID().equals(center.getUUID()))
        .findFirst().orElseThrow();
    village.assignJob(ruler.getUUID(), rulerJob);
    ruler.setOccupation(Occupation.LEADER);
    village.assignJob(captain.getUUID(), captainJob);
    captain.setOccupation(Occupation.GUARD);
    // A blocked role room may not displace the incumbent's valid previous household.
    check(sameBed(oldRuler, village.getBedAssignment(ruler.getUUID())), "Blocked royal room released old ruler bed");
    check(sameBed(oldCaptain, village.getBedAssignment(captain.getUUID())), "Missing captain bed released old bed");
    check(VillageRuler.incumbent(village).map(person -> person.getUUID().equals(ruler.getUUID())).orElse(false),
        "Loaded ruler did not become decision speaker");

    level.setBlock(royalBed.above(), royalAbove, 2);
    // Removing one bed half also removes its neighbor; restore the complete native two-block bed.
    level.setBlock(captainFoot, captainFootState, 18);
    level.setBlock(captainBed, captainState, 18);
    ApprovedStructureAccess.reconcileBeds(village);
    check(village.getBedAssignment(ruler.getUUID()).getBuildingUUID().equals(castle.getUUID()), "Ruler did not move into repaired suite");
    check(village.getBedAssignment(captain.getUUID()).getBuildingUUID().equals(castle.getUUID())
        && village.getBedAssignment(captain.getUUID()).getBedIndex() == 2, "Captain did not move to restored room");
    check(village.getJobAssignment(captain.getUUID()).getBuildingUUID().equals(center.getUUID())
        && village.getJobAssignmentsView().size() == 2, "Captain accommodation created or moved a job");
    village.putRelationship(RelationshipPair.create(ruler.getUUID(), spouse.getUUID(), 75, 0, 0, false, "", true));
    ApprovedStructureAccess.reconcileBeds(village);
    check(village.sharesCoupleHome(ruler.getUUID(), spouse.getUUID()), "Ruler's spouse did not receive adjacent reserved bed");
    check(village.getJobAssignment(spouse.getUUID()) == null, "Spouse inherited the ruling job");

    var ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
    CompoundTag saved = (CompoundTag) Village.CODEC.encodeStart(ops, village).getOrThrow();
    saved.put("town_center", UUIDUtil.CODEC.encodeStart(ops, center.getUUID()).getOrThrow());
    Village restored = Village.CODEC.parse(ops, saved).getOrThrow();
    restored.attach(level);
    ApprovedStructureAccess.reconcileBeds(restored);
    check(restored.sharesCoupleHome(ruler.getUUID(), spouse.getUUID()), "Royal household changed across reload");
    check(sameBed(village.getBedAssignment(captain.getUUID()), restored.getBedAssignment(captain.getUUID())),
        "Captain room changed across reload");
    restored.removePerson(ruler.getUUID());
    ApprovedStructureAccess.reconcileBeds(restored);
    check(VillageRuler.incumbent(restored).isEmpty(), "Departed ruler still speaks for settlement");
    check(restored.getBedAssignment(spouse.getUUID()) == null
        || restored.getBedAssignment(spouse.getUUID()).getBedIndex() > 2
        || !restored.getBedAssignment(spouse.getUUID()).getBuildingUUID().equals(castle.getUUID()),
        "Former ruler's spouse kept a reserved royal bed after succession vacancy");
    residents.forEach(person -> person.discard());
    Kithkyn.LOGGER.info("{} ROTATION PASS {}: blocked transfers, restored roles, paired household and codec reload", PREFIX, rotation);
  }

  private static ApprovedStructureAccess.Person person(ServerLevel level, Village village, BlockPos position,
      List<ApprovedStructureAccess.Person> residents) {
    var person = new ApprovedStructureAccess.Person(level, village);
    person.setLifeStage(AgeStage.ADULT);
    person.setNoAi(true);
    ApprovedStructureAccess.moveTo(person, position);
    village.getPopulation().add(person.getUUID());
    check(level.addFreshEntity(person), "Unable to create role resident");
    residents.add(person);
    return person;
  }

  private static boolean sameBed(BedAssignment first, BedAssignment second) {
    return second != null && first.getBuildingUUID().equals(second.getBuildingUUID())
        && first.getBedIndex() == second.getBedIndex();
  }

  private static BlockPos world(Building building, BlockPos local) {
    return BlockPos.of(building.getOriginLocation()).offset(local.rotate(building.getRotation()));
  }

  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  private static final class Fixture extends Village {
    private final Building center;
    Fixture(ServerLevel level, Building center, Building castle) {
      super("Castle roles fixture");
      this.center = center;
      attach(level);
      addBuilding(center);
      addBuilding(castle);
    }
    @Override public Building getTownCenter() { return center; }
    @Override public void update(ServerLevel ignored) {}
  }
}
