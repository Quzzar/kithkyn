package com.quzzar.kithkyn.awareness;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

import com.quzzar.kithkyn.awareness.ObservationLog.Evidence;
import com.quzzar.kithkyn.awareness.ObservationLog.Identity;
import com.quzzar.kithkyn.awareness.ObservationLog.Kind;
import com.quzzar.kithkyn.awareness.ObservationLog.Place;
import com.quzzar.kithkyn.awareness.PerceptionBudget.Work;
import com.quzzar.kithkyn.chat.PersonChatDispatcher;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.wrongdoing.Witnesses;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

/** Server-thread perception feeding existing chat. It makes no inference requests or movement decisions. */
public final class ResidentAwareness {
  public static final double RANGE = Witnesses.SIGHT_RANGE;
  private static final int CANDIDATE_LIMIT = 24;
  private static final Map<MinecraftServer, PerceptionBudget> BUDGETS = new WeakHashMap<>();
  private final RealPerson person;
  private final ObservationLog log = new ObservationLog();
  private String dimension;
  private long nextScan;
  private long deliveryTick = Long.MIN_VALUE;
  private int deliveries;

  public ResidentAwareness(RealPerson person) { this.person = person; }

  public List<ObservationLog.Observation> snapshot() {
    checkDimension();
    return log.snapshot(person.level().getGameTime());
  }

  public String briefing() {
    checkDimension();
    return log.briefing(person.level().getGameTime());
  }

  /** At most one bounded visible-threat query per second per resident, staggered across the server. */
  public void tick() {
    if (!(person.level() instanceof ServerLevel level) || !awake(person)) return;
    checkDimension();
    long now = level.getGameTime();
    if (now < nextScan || Math.floorMod(now + person.getId(), 20) != 0) return;
    if (!take(level, Work.QUERY)) return;
    nextScan = now + 20;
    List<LivingEntity> nearby = new ArrayList<>();
    level.getEntities(EntityTypeTest.forClass(LivingEntity.class), person.getBoundingBox().inflate(RANGE),
        entity -> entity != person && entity.isAlive()
            && (entity instanceof Enemy || entity instanceof Mob mob && mob.getTarget() == person),
        nearby, CANDIDATE_LIMIT);
    for (LivingEntity threat : nearby) {
      if (visible(person, threat)) record(Kind.VISIBLE_THREAT, Evidence.SEEN, threat, null, "A potential threat was visible nearby.");
    }
  }

  /** Only physical player input enters here; synthetic greetings and developer prompt replays do not. */
  public static void addressed(RealPerson listener, LivingEntity speaker, String line) {
    if (visible(listener, speaker)) listener.awareness().record(Kind.SPEECH, Evidence.REPORTED, speaker, speaker, line);
  }

  public static void approached(RealPerson listener, LivingEntity speaker) {
    if (visible(listener, speaker)) listener.awareness().record(Kind.ADDRESSED, Evidence.SEEN, speaker, null,
        "This person approached to talk.");
  }

  /** Existing villager speech is audible within sixteen blocks along an unobstructed line. */
  public static void spoken(RealPerson speaker, String line) {
    if (!(speaker.level() instanceof ServerLevel level)) return;
    for (RealPerson listener : listeners(level, speaker)) {
      if (!visible(listener, speaker)) continue;
      boolean addressed = PersonChatDispatcher.conversingWith(listener).filter(speaker.getUUID()::equals).isPresent();
      listener.awareness().record(Kind.SPEECH, addressed ? Evidence.REPORTED : Evidence.HEARD, speaker, speaker, line);
    }
  }

  /** Post-damage evidence only. Seeing an injury does not identify a hidden or distant attacker. */
  public static Optional<Entity> injured(LivingEntity victim, Entity attacker) {
    if (!(victim.level() instanceof ServerLevel level)) return Optional.empty();
    Entity recognized = null;
    if (victim instanceof RealPerson person) {
      Entity known = attacker != null && visible(person, attacker) ? attacker : null;
      recognized = known;
      person.awareness().record(Kind.INJURY, Evidence.EXPERIENCED, victim, known, "You suffered an injury.");
    }
    for (RealPerson listener : listeners(level, victim)) {
      if (!visible(listener, victim)) continue;
      Entity known = attacker != null && attacker != victim && visible(listener, attacker) ? attacker : null;
      listener.awareness().record(Kind.INJURY, Evidence.SEEN, victim, known, "This person or creature suffered an injury.");
    }
    return Optional.ofNullable(recognized);
  }

  private static List<RealPerson> listeners(ServerLevel level, Entity source) {
    if (!take(level, Work.QUERY)) return List.of();
    List<RealPerson> nearby = new ArrayList<>();
    level.getEntities(EntityTypeTest.forClass(RealPerson.class), new AABB(source.position(), source.position()).inflate(RANGE),
        listener -> listener != source && awake(listener), nearby, CANDIDATE_LIMIT);
    return nearby;
  }

  private static boolean visible(RealPerson observer, Entity subject) {
    return observer.level() instanceof ServerLevel level && observer.level() == subject.level()
        && !subject.isInvisible() && !subject.isRemoved() && awake(observer) && take(level, Work.VISIBILITY)
        && Witnesses.canSee(level, observer, subject.getEyePosition());
  }

  private void record(Kind kind, Evidence evidence, Entity subject, Entity source, String detail) {
    if (!(person.level() instanceof ServerLevel level)) return;
    long now = level.getGameTime();
    if (deliveryTick != now) { deliveryTick = now; deliveries = 0; }
    if (deliveries >= 8 || !take(level, Work.DELIVERY)) return;
    deliveries++;
    checkDimension();
    log.record(kind, evidence, identity(subject), source == null ? null : identity(source),
        new Place(level.dimension().location().toString(), subject.getX(), subject.getY(), subject.getZ()), detail, now);
  }

  private void checkDimension() {
    String current = person.level().dimension().location().toString();
    if (!current.equals(dimension)) { log.clear(); dimension = current; nextScan = 0; }
  }

  private static Identity identity(Entity entity) {
    return new Identity(entity.getUUID(), entity instanceof RealPerson resident ? resident.getFullName() : entity.getName().getString());
  }

  private static boolean awake(RealPerson person) { return person.isAlive() && !person.isRemoved() && !person.isSleeping(); }

  private static boolean take(ServerLevel level, Work work) {
    return BUDGETS.computeIfAbsent(level.getServer(), ignored -> new PerceptionBudget()).take(work, level.getServer().getTickCount());
  }

  public static PerceptionBudget.Counts counts(MinecraftServer server) {
    return BUDGETS.computeIfAbsent(server, ignored -> new PerceptionBudget()).counts();
  }
}
