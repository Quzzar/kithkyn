package com.quzzar.kithkyn.dev;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.PersonEntityType;
import com.quzzar.kithkyn.awareness.ObservationLog.Evidence;
import com.quzzar.kithkyn.awareness.ObservationLog.Kind;
import com.quzzar.kithkyn.awareness.PerceptionBudget;
import com.quzzar.kithkyn.awareness.ResidentAwareness;
import com.quzzar.kithkyn.chat.PersonChatContext;
import com.quzzar.kithkyn.chat.PersonChatDispatcher;
import com.quzzar.kithkyn.chat.VillagerConversation;
import com.quzzar.kithkyn.configuration.KithkynConfig;
import com.quzzar.kithkyn.entities.AgeStage;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.entities.KithkynAttachments;
import com.quzzar.kithkyn.llm.LlmCallLog;
import com.quzzar.kithkyn.village.Occupation;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.VillageManager;
import com.quzzar.kithkyn.village.buildings.Building;
import com.quzzar.kithkyn.wrongdoing.Witnesses;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Disposable-world visibility/speech/event wiring and bounded population checks. No model is loaded. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class ResidentAwarenessVerification {
  private static final List<RealPerson> PEOPLE = new ArrayList<>();
  private static int ticks;
  private static int phase;
  private static int phaseStart;
  private static boolean finished;
  private static RealPerson observer;
  private static RealPerson victim;
  private static RealPerson hidden;
  private static RealPerson distant;
  private static RealPerson speaker;
  private static RealPerson attacker;
  private static Zombie threat;
  private static ServerPlayer player;
  private static long requests;
  private static PerceptionBudget.Counts populationStart;

  private ResidentAwarenessVerification() { }

  @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
    if (!Boolean.getBoolean("kithkyn.residentAwareness.verify") || finished || ++ticks < 40) return;
    try {
      ServerLevel level = event.getServer().overworld();
      if (observer == null) setup(level);
      if (phase == 0 && ticks - phaseStart >= 40) {
        check(observer.awareness().snapshot().stream().anyMatch(item -> item.kind() == Kind.VISIBLE_THREAT
            && item.subject().id().equals(threat.getUUID())), "Visible threat was not observed by real ticking");
        check(hidden.awareness().snapshot().stream().noneMatch(item -> item.kind() == Kind.VISIBLE_THREAT
            && item.subject().id().equals(threat.getUUID())), "Occluded observer saw the threat");
        threat.discard();
        phase = 1;
        phaseStart = ticks;
      } else if (phase == 1 && ticks - phaseStart >= 65) {
        check(observer.awareness().snapshot().stream().noneMatch(item -> item.kind() == Kind.VISIBLE_THREAT), "Old threat sighting did not expire");
        PEOPLE.forEach(RealPerson::discard);
        PEOPLE.clear();
        player.discard();
        VillageManager.get(level).getVillages().clear();
        populationStart = ResidentAwareness.counts(event.getServer());
        for (int index = 0; index < 64; index++) person(level, "Resident" + index, 200 + index % 8 * 3, 200 + index / 8 * 3);
        threat = zombie(level, 211.5, 211.5);
        phase = 2;
        phaseStart = ticks;
      } else if (phase == 2 && ticks - phaseStart >= 1200) {
        var counts = ResidentAwareness.counts(event.getServer());
        long queries = counts.queries() - populationStart.queries();
        check(queries == 64L * 60, "Expected exactly 64 resident scans per second: " + queries);
        check(LlmCallLog.requestCount() == requests, "Awareness dispatched model requests");
        finished = true;
        Kithkyn.LOGGER.info("[resident-awareness-verify] RESULT PASS: walls, distance, sleeping, invisibility, "
            + "injury wiring, hidden attacker, speaker identities, quoted reports, bounded repeated speech, "
            + "visible-threat scan and expiry; 64 stationary residents for 1200 ticks: queries={}, visibility={}, "
            + "deliveries={}, denied={}, extra model requests=0; no full village/model performance claim",
            queries, counts.visibility() - populationStart.visibility(), counts.deliveries() - populationStart.deliveries(),
            counts.denied() - populationStart.denied());
        event.getServer().halt(false);
      }
      check(ticks < 1600, "Timed out in awareness phase " + phase);
    } catch (Exception | AssertionError failure) {
      finished = true;
      Kithkyn.LOGGER.error("[resident-awareness-verify] RESULT FAIL", failure);
      event.getServer().halt(false);
    }
  }

  private static void setup(ServerLevel level) {
    check(!KithkynConfig.LlmEnabled && !KithkynConfig.GenerateVillages, "Fixture requires production inference and village generation disabled");
    requests = LlmCallLog.requestCount();
    level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
    level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, level.getServer());
    level.setDayTime(6000);
    level.getServer().tickRateManager().setTickRate(100);
    for (int x = 5; x <= 8; x++) for (int z = 5; z <= 8; z++) level.setChunkForced(x, z, true);
    for (int x = 12; x <= 14; x++) for (int z = 12; z <= 14; z++) level.setChunkForced(x, z, true);
    // Re-running this disposable fixture must not count the previous run's saved population.
    for (var entity : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
        new net.minecraft.world.phys.AABB(90, 150, 90, 230, 180, 230))) {
      if (entity instanceof RealPerson || entity instanceof Zombie) entity.discard();
    }
    VillageManager.get(level).getVillages().clear();
    for (BlockPos pos : BlockPos.betweenClosed(96, 160, 96, 120, 165, 142)) {
      level.setBlock(pos, (pos.getY() == 160 ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 2);
    }
    for (BlockPos pos : BlockPos.betweenClosed(96, 161, 107, 120, 165, 107)) level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
    for (BlockPos pos : BlockPos.betweenClosed(198, 160, 198, 224, 165, 224)) {
      level.setBlock(pos, (pos.getY() == 160 ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 2);
    }
    observer = person(level, "Observer", 100, 100);
    victim = person(level, "Victim", 100, 104);
    hidden = person(level, "Occluded", 100, 110);
    distant = person(level, "Distant", 100, 140);
    speaker = person(level, "Elin", 98, 102);
    attacker = person(level, "HiddenAttacker", 100, 112);
    player = FakePlayerFactory.get(level, new GameProfile(UUID.fromString("194cf127-c74b-43d3-9631-97ea270c0703"), "Rowan"));
    player.moveTo(102.5, 161, 101.5);
    level.addNewPlayer(player);
    Village village = new Village("Awareness fixture") {
      private final Building center = new Building(new BlockPos(100, 160, 104), "village_center_birch_forest_1", Rotation.NONE);
      @Override public Building getTownCenter() { return center; }
      @Override public void update(ServerLevel ignored) { }
    };
    village.attach(level);
    VillageManager.get(level).getVillages().put(village.getID(), village);
    victim.setVillage(village.getID());
    village.getPopulation().add(victim.getUUID());

    check(Witnesses.canSee(level, observer, victim.getEyePosition()), "Clear line unexpectedly blocked");
    check(!Witnesses.canSee(level, hidden, victim.getEyePosition()), "Wall did not block sight");
    check(!Witnesses.canSee(level, distant, victim.getEyePosition()), "Distance did not block sight");
    victim.hurt(level.damageSources().mobAttack(attacker), 1);
    var injury = observer.awareness().snapshot().stream().filter(item -> item.kind() == Kind.INJURY).findFirst().orElseThrow();
    check(injury.subject().id().equals(victim.getUUID()) && injury.source() == null, "Injury revealed a hidden attacker");
    check(hidden.awareness().snapshot().stream().noneMatch(item -> item.kind() == Kind.INJURY), "Wall did not block injury witness");
    check(distant.awareness().snapshot().stream().noneMatch(item -> item.kind() == Kind.INJURY), "Distant incident became witnessed");
    String witnessedMemory = "I saw " + victim.getFullName() + " suffer an injury.";
    check(observer.getData(KithkynAttachments.PERSONAL_LOG.get()).memoriesNewestFirst().stream()
        .anyMatch(entry -> entry.text().startsWith(witnessedMemory)), "Witnessed injury was not preserved as a factual memory");
    check(hidden.getData(KithkynAttachments.PERSONAL_LOG.get()).memoriesNewestFirst().stream()
        .noneMatch(entry -> entry.text().startsWith(witnessedMemory)), "Occluded injury became a durable witnessed fact");
    var remembered = new net.minecraft.nbt.CompoundTag(); check(observer.save(remembered), "Could not serialize witnessed memory");
    var restored = (RealPerson) net.minecraft.world.entity.EntityType.create(remembered, level).orElseThrow();
    String reloadedContext = PersonChatContext.assemble(restored, "Rowan", player.getUUID(), List.of(), "Was Victim hurt?").system();
    check(reloadedContext.contains(witnessedMemory) && !reloadedContext.contains("HiddenAttacker"),
        "Reload lost the witnessed injury or invented its attacker");
    check(victim.awareness().snapshot().stream().anyMatch(item -> item.evidence() == Evidence.EXPERIENCED), "Victim did not retain own injury");
    String victimContext = PersonChatContext.assemble(victim, "Rowan", player.getUUID(), List.of(), "Who injured you?").system();
    check(!victimContext.contains("HiddenAttacker"), "Durable memory revealed an unseen attacker");

    player.moveTo(100.5, 161, 112.5);
    victim.invulnerableTime = 0;
    check(victim.hurt(level.damageSources().playerAttack(player), 1), "Hidden player attack did not land");
    check(!remembersCulprit(observer, player.getUUID()), "Existing village blame identified a hidden player");
    player.moveTo(102.5, 161, 101.5);
    player.setInvisible(true);
    victim.invulnerableTime = 0;
    check(victim.hurt(level.damageSources().playerAttack(player), 1), "Invisible player attack did not land");
    check(!remembersCulprit(observer, player.getUUID()), "Existing village blame identified an invisible player");
    player.setInvisible(false);
    victim.invulnerableTime = 0;
    check(victim.hurt(level.damageSources().playerAttack(player), 1), "Visible player attack did not land");
    check(remembersCulprit(observer, player.getUUID()), "Visible culprit was never identified");

    BlockPos partial = new BlockPos(116, 163, 100);
    level.setBlock(partial, Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP), 2);
    observer.moveTo(116.5, 165 - observer.getEyeHeight(), 99.5);
    player.moveTo(116.5, 163.25 - player.getEyeHeight(), 100.5);
    check(Witnesses.canSee(level, observer, player.getEyePosition()), "Solid-target block exception was not exercised");
    check(!Witnesses.canIdentify(level, observer, player), "Partial block containing the eyes did not occlude identity");
    observer.moveTo(100.5, 161, 100.5);
    player.moveTo(102.5, 161, 101.5);

    RealPerson doomed = person(level, "Doomed", 102, 104);
    check(doomed.hurt(level.damageSources().mobAttack(attacker), 100), "Hidden resident lethal attack did not land");
    var memories = observer.getData(KithkynAttachments.PERSONAL_LOG.get()).memoriesNewestFirst();
    check(memories.stream().anyMatch(memory -> memory.text().equals("I saw Doomed Fixture die.")), "Unidentified witnessed death was lost");
    check(memories.stream().noneMatch(memory -> memory.text().contains("HiddenAttacker")), "Death memory revealed a hidden resident killer");
    RealPerson visibleDeath = person(level, "VisibleDeath", 103, 104);
    check(visibleDeath.hurt(level.damageSources().mobAttack(speaker), 100), "Visible resident lethal attack did not land");
    check(observer.getData(KithkynAttachments.PERSONAL_LOG.get()).memoriesNewestFirst().stream()
        .anyMatch(memory -> memory.text().equals("I saw VisibleDeath Fixture killed by Elin Fixture.")), "Visible resident killer was not identified");

    observer.startSleeping(observer.blockPosition());
    ResidentAwareness.addressed(observer, player, "Sleeping test");
    check(observer.awareness().snapshot().stream().noneMatch(item -> item.detail().equals("Sleeping test")), "Sleeping resident heard speech");
    observer.stopSleeping();
    player.setInvisible(true);
    ResidentAwareness.addressed(observer, player, "Invisible test");
    check(observer.awareness().snapshot().stream().noneMatch(item -> item.detail().equals("Invisible test")), "Invisible player became identified");
    player.setInvisible(false);
    ResidentAwareness.addressed(observer, player, "A dragon landed by the river.");
    check(observer.awareness().snapshot().stream().anyMatch(item -> item.evidence() == Evidence.REPORTED
        && item.source().id().equals(player.getUUID())), "Addressed player source was lost");
    PersonChatDispatcher.markTalking(observer, speaker.getUUID());
    VillagerConversation.speak(speaker, "Someone told me a dragon landed.");
    var speech = observer.awareness().snapshot().stream().filter(item -> item.detail().equals("Someone told me a dragon landed.")).findFirst().orElseThrow();
    check(speech.evidence() == Evidence.REPORTED && speech.source().id().equals(speaker.getUUID())
        && speech.subject().id().equals(speaker.getUUID()), "Villager speaker identity or report provenance was wrong");
    check(victim.awareness().snapshot().stream().anyMatch(item -> item.evidence() == Evidence.HEARD
        && item.source().id().equals(speaker.getUUID())), "Bystander did not overhear actual villager speech");
    String context = PersonChatContext.assemble(observer, "Rowan", player.getUUID(), List.of(), "What happened?").system();
    check(context.contains("not verified world facts") && context.contains(speaker.getUUID().toString()), "Chat did not receive sourced observations");
    var before = ResidentAwareness.counts(level.getServer());
    for (int index = 0; index < 1000; index++) VillagerConversation.speak(speaker, "Repeated speech");
    var after = ResidentAwareness.counts(level.getServer());
    check(after.queries() - before.queries() <= PerceptionBudget.QUERIES_PER_TICK, "Speech bypassed query budget");
    check(after.deliveries() - before.deliveries() <= PerceptionBudget.DELIVERIES_PER_TICK, "Speech bypassed delivery budget");
    check(after.denied() > before.denied(), "Repeated speech never exercised budget exhaustion");
    check(observer.awareness().snapshot().stream().filter(item -> item.detail().equals("Repeated speech")).count() <= 1, "Repeated speech flooded memory");
    threat = zombie(level, 104.5, 101.5);
    phaseStart = ticks;
  }

  private static RealPerson person(ServerLevel level, String name, int x, int z) {
    RealPerson person = new FixturePerson(level, name);
    person.setLifeStage(AgeStage.ADULT);
    person.setOccupation(Occupation.WANDERER);
    person.setNoAi(true);
    person.setNoGravity(true);
    person.setPersistenceRequired();
    person.moveTo(x + 0.5, 161, z + 0.5);
    check(level.addFreshEntity(person), "Could not spawn resident");
    PEOPLE.add(person);
    return person;
  }

  private static Zombie zombie(ServerLevel level, double x, double z) {
    Zombie mob = EntityType.ZOMBIE.create(level);
    mob.setNoAi(true);
    mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
    mob.moveTo(x, 161, z);
    check(level.addFreshEntity(mob), "Could not spawn threat");
    return mob;
  }

  private static final class FixturePerson extends RealPerson {
    private FixturePerson(ServerLevel level, String name) {
      super(PersonEntityType.PERSON.get(), level);
      setFirstName(name);
      setLastName("Fixture");
    }
  }

  private static void check(boolean condition, String detail) { if (!condition) throw new AssertionError(detail); }

  private static boolean remembersCulprit(RealPerson person, UUID culprit) {
    return person.getData(KithkynAttachments.PERSONAL_LOG.get()).memoriesNewestFirst().stream()
        .anyMatch(memory -> memory.who().filter(culprit::equals).isPresent());
  }
}
