package com.quzzar.kithkyn.dev;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.PersonEntityType;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.events.CoreEvents;
import com.quzzar.kithkyn.village.LocationManager;
import com.quzzar.kithkyn.village.Occupation;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.VillageManager;
import com.quzzar.kithkyn.village.buildings.Building;
import com.quzzar.kithkyn.village.buildings.WorkerFooting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Opt-in real-AI bell recall regression in a disposable world with the approved Birch center. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class BellRecallVerification {
  private static final BlockPos ORIGIN = new BlockPos(96, 160, 96);
  private static final List<RealPerson> people = new ArrayList<>();
  private static FixtureVillage village;
  private static BlockPos bell;
  private static BlockPos destination;
  private static int tick;
  private static boolean finished;
  private static int interruptedWork;

  private BellRecallVerification() { }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (!Boolean.getBoolean("kithkyn.bellRecall.verify") || finished) return;
    try {
      ServerLevel level = event.getServer().overworld();
      if (++tick == 1) setup(level);
      if (tick == 20) ring(level);
      if (tick > 20 && people.getFirst().getTravelTarget() == null) {
        check(people.getFirst().blockPosition().distSqr(destination) <= 4,
            "Recall released before arrival: " + people.getFirst().blockPosition());
        check(interruptedWork == 1, "Active work was not interrupted exactly once");
        verifySafeguards(level);
        finished = true;
        Kithkyn.LOGGER.info("[bell-recall-verify] RESULT PASS: actual Birch bell walk, worker interruption, home/dependent/guard/travel preservation, unreachable bell, danger, timeout and cooldown");
        event.getServer().halt(false);
      }
      check(tick < 700, "Bedless resident never arrived at bell: " + people.getFirst().blockPosition());
    } catch (Exception | AssertionError failure) {
      finished = true;
      Kithkyn.LOGGER.error("[bell-recall-verify] RESULT FAIL", failure);
      event.getServer().halt(false);
    }
  }

  private static void setup(ServerLevel level) {
    level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, level.getServer());
    level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
    level.setDayTime(6000);
    for (int x = 4; x <= 9; x++) for (int z = 4; z <= 9; z++) level.setChunkForced(x, z, true);
    for (BlockPos pos : BlockPos.betweenClosed(70, 159, 70, 145, 183, 145)) {
      // The approved center sinks four blocks: outside ground is at origin Y + 4.
      level.setBlock(pos, (pos.getY() <= 163 ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 2);
    }
    var template = level.getStructureManager().getOrCreate(
        ResourceLocation.fromNamespaceAndPath(Kithkyn.MODID, "village_center_birch_forest_1"));
    check(template.placeInWorld(level, ORIGIN, ORIGIN, new StructurePlaceSettings().setIgnoreEntities(true),
        level.random, 2), "Birch center did not place");
    bell = template.palettes.getFirst().blocks().stream().filter(block -> block.state().is(Blocks.BELL))
        .map(block -> ORIGIN.offset(block.pos())).findFirst().orElseThrow();
    village = new FixtureVillage();
    village.attach(level);
    village.register();
    VillageManager.get(level).getVillages().put(village.getID(), village);
    for (int i = 0; i < 7; i++) resident(level, 80 + i, 110);
    try {
      ApprovedStructureAccess.assignSingle(village, people.get(1).getUUID(), village.center.getUUID());
      ApprovedStructureAccess.assignSingle(village, people.get(2).getUUID(), village.center.getUUID());
    } catch (ReflectiveOperationException failure) {
      throw new AssertionError("Could not assign the recall fixture's two single beds", failure);
    }
    people.get(3).setBaby(true);
    people.get(3).setParents(people.get(1), people.get(2));
    people.get(4).setOccupation(Occupation.GUARD);
    people.get(4).setTravelTarget(new BlockPos(80, 164, 80));
    people.get(5).setTravelTarget(new BlockPos(81, 164, 80));
    people.get(6).moveTo(170, 164, 110, 0, 0);
    for (int i = 1; i < people.size(); i++) people.get(i).setNoAi(true);
    // Mimic the work-loop lifecycle: its stop cancels navigation when a higher-priority walk takes over.
    RealPerson worker = people.getFirst();
    worker.goalSelector.addGoal(2, new Goal() {
      { setFlags(java.util.EnumSet.of(Goal.Flag.MOVE)); }
      @Override public boolean canUse() { return tick < 20; }
      @Override public void start() { worker.getNavigation().moveTo(76, 164, 110, 0.6); }
      @Override public void stop() { interruptedWork++; worker.getNavigation().stop(); }
    });
  }

  private static void ring(ServerLevel level) {
    check(LocationManager.getNightRestLocation(people.getFirst()).equals(BlockPos.ZERO), "Caller already housed");
    check(!LocationManager.getNightRestLocation(people.get(3)).equals(BlockPos.ZERO), "Dependent has no family home");
    people.getFirst().tickCount = 199;
    var event = new PlayerInteractEvent.RightClickBlock(FakePlayerFactory.getMinecraft(level),
        InteractionHand.MAIN_HAND, bell, new BlockHitResult(Vec3.atCenterOf(bell), Direction.UP, bell, false));
    CoreEvents.onBellInteract(event);
    destination = people.getFirst().getTravelTarget();
    check(destination != null && WorkerFooting.canStand(people.getFirst(), destination), "No safe bell destination");
    check(Math.abs(destination.getX() - bell.getX()) <= 4 && Math.abs(destination.getZ() - bell.getZ()) <= 4,
        "Recall does not approach the actual bell");
    for (int i = 1; i <= 3; i++) check(people.get(i).getTravelTarget() == null && people.get(i).isInterrupted(),
        "Housed resident/dependent did not retain home behavior");
    check(people.get(4).getTravelTarget().equals(new BlockPos(80, 164, 80))
        && !people.get(4).isInterrupted() && people.get(4).callToBedCoolDown == 100, "Guard recalled instead of restocked");
    check(people.get(5).getTravelTarget().equals(new BlockPos(81, 164, 80)), "Existing travel replaced");
    check(people.get(6).getTravelTarget() == null && people.get(6).callToBedCoolDown == 0, "Distant resident recalled");
    people.getFirst().respondToBell(bell.offset(20, 0, 0));
    check(destination.equals(people.getFirst().getTravelTarget()), "Cooldown did not hold target");
    Kithkyn.LOGGER.info("[bell-recall-verify] walking from {} toward {} beside bell {}", people.getFirst().blockPosition(), destination, bell);
  }

  private static void verifySafeguards(ServerLevel level) {
    RealPerson person = people.getFirst();
    // Arrival can happen mid-step. Start the independent safety cases back on known dry ground.
    person.getNavigation().stop();
    person.moveTo(80.5, 164, 110.5, 0, 0);
    person.setOnGround(true);
    person.callToBedCoolDown = 0;
    person.respondToBell(bell.above(50));
    check(person.getTravelTarget() == null, "Unreachable bell captured movement");
    person.callToBedCoolDown = 0;
    person.respondToBell(bell);
    check(person.getTravelTarget() != null, "Cannot recall after cooldown: ground=" + person.onGround()
        + " position=" + person.blockPosition() + " home=" + LocationManager.getNightRestLocation(person));
    person.hurtTime = 10;
    person.cancelUnsafeBellRecall();
    check(person.getTravelTarget() == null, "Danger did not cancel recall");
    person.hurtTime = 0;
    person.callToBedCoolDown = 0;
    person.respondToBell(bell);
    level.getServer().getWorldData().overworldData().setGameTime(level.getGameTime() + 1201);
    person.cancelUnsafeBellRecall();
    check(person.getTravelTarget() == null, "Recall did not time out");
    person.setTravelTarget(new BlockPos(80, 160, 80));
    person.hurtTime = 10;
    person.cancelUnsafeBellRecall();
    check(person.getTravelTarget() != null, "Bell safeguard altered ordinary travel");
  }

  private static void resident(ServerLevel level, double x, double z) {
    RealPerson person = PersonEntityType.PERSON.get().create(level);
    person.setVillage(village.getID());
    person.setVillageName(village.getName());
    person.setOccupation(Occupation.WANDERER);
    person.moveTo(x + 0.5, 164, z + 0.5, 0, 0);
    village.getPopulation().add(person.getUUID());
    check(level.addFreshEntity(person), "Resident did not spawn");
    people.add(person);
  }

  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  private static final class FixtureVillage extends Village {
    private final Building center = new Building(ORIGIN, "village_center_birch_forest_1", Rotation.NONE);
    private FixtureVillage() {
      super("Bell Regression");
      center.setCenterLocation(ORIGIN.offset(14, 1, 14).asLong());
      center.setRadius(16);
    }
    private void register() { addBuilding(center); }
    @Override public Building getTownCenter() { return center; }
    @Override public void update(ServerLevel level) { }
  }
}
