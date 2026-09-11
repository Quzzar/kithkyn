package com.quzzar.kithkyn.dev;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.entities.AgeStage;
import com.quzzar.kithkyn.entities.ai.goals.GuardPatrolGoal;
import com.quzzar.kithkyn.village.GuardDuty;
import com.quzzar.kithkyn.village.GuardRole;
import com.quzzar.kithkyn.village.Occupation;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.buildings.Building;
import com.quzzar.kithkyn.village.buildings.Buildings;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Runs the real sentry goals against each rotated private castle; timeouts cannot count as arrivals. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class CastleOperationsVerification {
  private static final String PREFIX = "[castle-operations-verify]";
  private static final List<Patrol> patrols = new ArrayList<>();
  private static int ticks;
  private static int started;
  private static int rotation;
  private static int completedSentries;
  private static boolean finished;

  private CastleOperationsVerification() { }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (!Boolean.getBoolean("kithkyn.castleOperations.verify") || finished || ++ticks < 40) return;
    ServerLevel level = event.getServer().overworld();
    try {
      level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, event.getServer());
      level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, event.getServer());
      level.setDayTime(6000);
      if (patrols.isEmpty()) setup(level);
      for (Patrol patrol : patrols) {
        for (BlockPos point : patrol.route) {
          if (Math.abs(patrol.person.getY() - point.getY()) < .6
              && patrol.person.distanceToSqr(point.getX() + .5, point.getY(), point.getZ() + .5) < .36) {
            patrol.visited.add(point);
          }
        }
        int lowest = patrol.route.stream().mapToInt(BlockPos::getY).min().orElseThrow();
        int highest = patrol.route.stream().mapToInt(BlockPos::getY).max().orElseThrow();
        if (highest - lowest <= 1) {
          check(patrol.person.getY() >= lowest - 1.5D && patrol.person.getY() <= highest + 1.5D,
              "Sentry left authored patrol floor: " + patrol.person.position());
        }
      }
      if (ticks - started > 3200) throw new AssertionError("Patrol stalled " + patrols.stream().map(p ->
          p.person.position() + " visited=" + p.visited + " expected=" + p.route).toList());
      if (patrols.stream().allMatch(p -> p.visited.size() == p.route.size())) {
        completedSentries += patrols.size();
        Kithkyn.LOGGER.info("{} ROTATION PASS {}: all {} actual guard goals visited every authored waypoint",
            PREFIX, Rotation.values()[rotation], patrols.size());
        patrols.forEach(p -> p.person.discard());
        patrols.clear();
        if (++rotation == 4) {
          finished = true;
          Kithkyn.LOGGER.info("{} RESULT PASS: {} real sentries visited all authored patrol levels in four rotations and retained loadouts",
              PREFIX, completedSentries);
          event.getServer().halt(false);
        }
      }
    } catch (Exception | AssertionError failure) {
      finished = true;
      Kithkyn.LOGGER.error(PREFIX + " RESULT FAIL", failure);
      event.getServer().halt(false);
    }
  }

  private static void setup(ServerLevel level) throws ReflectiveOperationException {
    BlockPos origin = new BlockPos(2400, 159, 2400);
    for (int x = 147; x <= 153; x++) for (int z = 147; z <= 153; z++) level.setChunkForced(x, z, true);
    String castleId = System.getProperty("kithkyn.castleOperations.id", "castle_desert_1");
    var castleInfo = Buildings.getByName(castleId);
    check(castleInfo != null, "Missing castle definition " + castleId);
    Building castle = ApprovedStructureAccess.place(level, origin, castleInfo, Rotation.values()[rotation]);
    Village village = new ApprovedStructureAccess.VillageFixture(level, castle, true);
    for (var job : List.copyOf(village.getUnassignedJobs())) {
      if (job.getOccupation() != Occupation.GUARD) continue;
      var person = new ApprovedStructureAccess.Person(level, village);
      person.setLifeStage(AgeStage.ADULT);
      village.getPopulation().add(person.getUUID());
      village.assignJob(person.getUUID(), job);
      person.setOccupation(Occupation.GUARD);
      person.issueStartingKit();
      var route = GuardDuty.patrolRoute(person);
      if (GuardDuty.isJailer(person)) {
        check(route.isEmpty(), "Jailer inherited a sentry route");
        check(person.guardRoutine() == com.quzzar.kithkyn.entities.ai.GuardNightRoutine.POST, "Jailer leaves cell during daytime");
        person.discard();
        continue;
      }
      check(!route.isEmpty(), "Sentry has no floor route");
      if (GuardDuty.of(person).ranged()) {
        check(person.getMainHandItem().is(Items.CROSSBOW), "Crossbow sentry did not receive crossbow");
        check(person.personMainInv.countItem(Items.STONE_SWORD) >= 1, "Crossbow sentry has no backup sword");
      } else {
        check(person.getMainHandItem().is(Items.STONE_SWORD) && person.getOffhandItem().is(Items.SHIELD), "Lower sentry lacks sword and shield");
      }
      ApprovedStructureAccess.enableWalking(person);
      person.goalSelector.addGoal(2, new GuardPatrolGoal(person));
      ApprovedStructureAccess.moveTo(person, route.get(patrols.size() % route.size()));
      check(level.addFreshEntity(person), "Could not spawn sentry");
      patrols.add(new Patrol(person, route, new HashSet<>()));
    }
    int station = 0;
    long expected = 0;
    for (Occupation occupation : castleInfo.getWorkLocations().values()) {
      if (occupation == Occupation.GUARD && castleInfo.getGuardRole(station) != GuardRole.JAILER) {
        expected++;
      }
      station++;
    }
    check(patrols.size() == expected, "Expected " + expected + " patrolling sentries, found " + patrols.size());
    started = ticks;
  }

  private record Patrol(ApprovedStructureAccess.Person person, List<BlockPos> route, Set<BlockPos> visited) { }
  private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
