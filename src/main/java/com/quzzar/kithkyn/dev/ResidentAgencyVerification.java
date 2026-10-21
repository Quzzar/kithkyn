package com.quzzar.kithkyn.dev;

import java.util.Optional;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.PersonEntityType;
import com.quzzar.kithkyn.agency.PointedLocations;
import com.quzzar.kithkyn.agency.ResidentActionSelection;
import com.quzzar.kithkyn.agency.ResidentActionState.Action;
import com.quzzar.kithkyn.agency.ResidentAgency;
import com.quzzar.kithkyn.configuration.KithkynConfig;
import com.quzzar.kithkyn.entities.AgeStage;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.entities.ai.goals.PersonAvoidGoal;
import com.quzzar.kithkyn.village.Occupation;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.VillageManager;
import com.quzzar.kithkyn.village.buildings.Building;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Disposable-world execution checks with real registered goals. Inference is deliberately excluded. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class ResidentAgencyVerification {
  private static final BlockPos START = new BlockPos(100, 161, 100);
  private static RealPerson resident;
  private static ServerPlayer requester;
  private static BlockPos destination;
  private static Vec3 waitingAt;
  private static Zombie threat;
  private static float healthBeforeThreat;
  private static int ticks;
  private static int phase;
  private static int phaseStarted;
  private static boolean finished;

  private ResidentAgencyVerification() { }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (!Boolean.getBoolean("kithkyn.residentAgency.verify") || finished || ++ticks < 40) return;
    try {
      ServerLevel level = event.getServer().overworld();
      if (resident == null) setup(level);
      observe(level);
      check(ticks < 1600, "Timed out in phase " + phase + " at " + resident.position()
          + ": " + resident.agency().describe());
      if (finished) {
        Kithkyn.LOGGER.info("[resident-agency-verify] RESULT PASS: server raycast, actual pointed walk, "
            + "follow, wait, release, danger interruption, existing avoidance, quarrel precedence, "
            + "blocked route and invalidated ground; no inference");
        event.getServer().halt(false);
      }
    } catch (Exception | AssertionError failure) {
      finished = true;
      Kithkyn.LOGGER.error("[resident-agency-verify] RESULT FAIL", failure);
      event.getServer().halt(false);
    }
  }

  private static void setup(ServerLevel level) {
    KithkynConfig.LlmResidentActions = true;
    level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
    level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, level.getServer());
    level.setDayTime(6000);
    level.updateSkyBrightness();
    level.getServer().tickRateManager().setTickRate(100);
    for (int x = 5; x <= 8; x++) for (int z = 5; z <= 8; z++) level.setChunkForced(x, z, true);
    for (BlockPos pos : BlockPos.betweenClosed(80, 160, 80, 135, 168, 135)) {
      level.setBlock(pos, (pos.getY() == 160 ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 2);
    }
    Village village = new Village("Resident agency verification") {
      private final Building center = new Building(START.below(), "village_center_birch_forest_1", Rotation.NONE);
      @Override public Building getTownCenter() { return center; }
      @Override public void update(ServerLevel ignored) { }
    };
    village.attach(level);
    VillageManager.get(level).getVillages().put(village.getID(), village);
    resident = PersonEntityType.PERSON.get().create(level);
    resident.setVillage(village.getID());
    resident.setOccupation(Occupation.WANDERER);
    resident.setLifeStage(AgeStage.ADULT);
    resident.setPersistenceRequired();
    resident.moveTo(Vec3.atBottomCenterOf(START));
    resident.setOnGround(true);
    village.getPopulation().add(resident.getUUID());
    check(level.addFreshEntity(resident), "Could not spawn resident");
    requester = FakePlayerFactory.getMinecraft(level);
    requester.moveTo(Vec3.atBottomCenterOf(START.offset(0, 0, 3)));
    level.addNewPlayer(requester);
    destination = START.offset(8, 0, 0);
    point(level, destination);
    ask(Action.GO_TO_POINT);
  }

  private static void observe(ServerLevel level) {
    switch (phase) {
      case 0 -> {
        if (resident.agency().current().isPresent()) return;
        check(resident.agency().describe().contains("COMPLETED"), "Pointed walk did not complete");
        check(resident.distanceToSqr(Vec3.atBottomCenterOf(destination)) <= 1, "Completed away from destination");
        requester.moveTo(resident.position().add(0, 0, 8));
        ask(Action.FOLLOW);
        next();
      }
      case 1 -> {
        check(resident.agency().current().isPresent(), "Follow ended before arrival");
        if (resident.distanceToSqr(requester) > 9) return;
        waitingAt = resident.position();
        ask(Action.WAIT);
        requester.moveTo(requester.position().add(4, 0, 0));
        next();
      }
      case 2 -> {
        check(resident.agency().current().isPresent(), "Wait ended without release");
        check(resident.distanceToSqr(waitingAt) <= 1, "Resident followed while asked to wait");
        if (ticks - phaseStarted < 40) return;
        ask(Action.RESUME);
        check(resident.agency().current().isEmpty(), "Release retained active order");
        ask(Action.FOLLOW);
        resident.setRemainingFireTicks(100);
        resident.agency().maintain();
        check(resident.agency().current().isEmpty()
            && resident.agency().describe().contains("INTERRUPTED"), "Danger did not interrupt follow");
        resident.clearFire();
        ask(Action.FOLLOW);
        healthBeforeThreat = resident.getHealth();
        threat = EntityType.ZOMBIE.create(level);
        threat.setNoAi(true);
        threat.moveTo(resident.position().add(5, 0, 0));
        check(level.addFreshEntity(threat), "Could not spawn approaching threat");
        next();
      }
      case 3 -> {
        if (resident.agency().current().isPresent()) return;
        check(resident.agency().describe().contains("INTERRUPTED"), "Threat did not interrupt movement");
        check(resident.getHealth() == healthBeforeThreat, "Resident was hurt before interrupting movement");
        resident.agency().apply(resident.agency().offer(requester),
            new ResidentActionSelection(Optional.of(Action.WAIT), Optional.empty()), requester);
        check(resident.agency().current().isEmpty(), "Accepted an instruction during active avoidance");
        threat.discard();
        next();
      }
      case 4 -> {
        if (resident.goalSelector.getAvailableGoals().stream()
            .anyMatch(running -> running.isRunning() && running.getGoal() instanceof AvoidEntityGoal)) return;
        destination = resident.blockPosition().offset(6, 0, 0);
        for (BlockPos pos : BlockPos.betweenClosed(destination.offset(-1, 0, -1), destination.offset(1, 2, 1))) {
          if (pos.getX() != destination.getX() || pos.getZ() != destination.getZ()) {
            level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
          }
        }
        requester.moveTo(resident.position().add(-2, 0, 0));
        point(level, destination);
        ask(Action.GO_TO_POINT);
        next();
      }
      case 5 -> {
        if (resident.agency().current().isPresent()) return;
        check(resident.agency().describe().contains("FAILED"), "Blocked route was reported as success");
        check(resident.distanceToSqr(Vec3.atBottomCenterOf(destination)) > 1, "Resident crossed sealed walls");
        destination = resident.blockPosition().offset(-4, 0, 0);
        point(level, destination);
        ask(Action.GO_TO_POINT);
        level.setBlock(destination.below(), Blocks.AIR.defaultBlockState(), 2);
        next();
      }
      case 6 -> {
        if (resident.agency().current().isPresent()) return;
        check(resident.agency().describe().contains("FAILED"), "Removed ground was reported as success");
        verifyQuarrelPrecedence();
        finished = true;
      }
      default -> throw new AssertionError("Unknown verification phase " + phase);
    }
  }

  private static void point(ServerLevel level, BlockPos target) {
    Vec3 original = requester.position();
    requester.moveTo(Vec3.atBottomCenterOf(target).add(0, 5, 0));
    requester.setXRot(90);
    check(PointedLocations.capture(requester), "Server raycast did not capture " + target);
    check(PointedLocations.get(requester).orElseThrow().equals(
        ResidentAgency.position(Vec3.atBottomCenterOf(target))), "Raycast captured a different destination");
    requester.moveTo(original);
  }

  private static void ask(Action action) {
    resident.agency().apply(resident.agency().offer(requester),
        new ResidentActionSelection(Optional.of(action), Optional.empty()), requester);
    check(action == Action.RESUME || resident.agency().current().filter(order -> order.action() == action).isPresent(),
        "Action was not accepted: " + action + "; " + resident.agency().describe());
  }

  private static void verifyQuarrelPrecedence() {
    resident.getNavigation().stop();
    requester.moveTo(resident.position().add(2, 0, 0));
    PersonAvoidGoal<ServerPlayer> avoidance = new PersonAvoidGoal<>(resident, ServerPlayer.class, target -> true);
    boolean found = false;
    for (int attempt = 0; attempt < 32 && !found; attempt++) found = avoidance.canUse();
    check(found, "Could not start civilian avoidance for quarrel check");
    avoidance.start();
    check(avoidance.canContinueToUse(), "Avoidance was not running before quarrel");
    resident.pickFightWith(requester);
    check(!avoidance.canContinueToUse() && !avoidance.canUse(), "Avoidance suppressed an explicit quarrel");
    avoidance.stop();
  }

  private static void next() {
    phase++;
    phaseStarted = ticks;
  }

  private static void check(boolean condition, String detail) {
    if (!condition) throw new AssertionError(detail);
  }
}
