package com.quzzar.kithkyn.dev;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.entities.Kind;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.VillageManager;
import com.quzzar.kithkyn.village.buildings.VillageStyle;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Founds one village at a given spot on whatever world the server was started
 * with, and samples the server thread every two seconds until it stands
 * (#138: a manual founding near Sorevia held one tick for over a minute).
 *
 * <p>{@code -Dkithkyn.foundingProbe=x,y,z,style[;x,y,z,style...]}: each site is
 * founded once the one before it stands, which is how the live hang was made
 * (a second founding nine blocks from a fresh one). Every server tick longer
 * than a second is logged with its length. Runs on a copy of the live world,
 * never the live server: it halts the server when the last village stands or
 * after ten minutes, and logs {@code [founding-probe] RESULT}.
 */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class FoundingProbe {

  private static final String PREFIX = "[founding-probe]";
  private static final int SETTLE_TICKS = 100;
  private static final long GIVE_UP_NANOS = 10L * 60L * 1_000_000_000L;

  private static int ticks;
  private static boolean started;
  private static boolean done;
  private static long startedAt;
  private static long tickStartedAt;
  private static java.util.List<String> sites;
  private static int next;
  private static BlockPos site;
  private static Thread sampler;

  private FoundingProbe() { }

  @SubscribeEvent
  public static void tickStart(ServerTickEvent.Pre event) {
    tickStartedAt = System.nanoTime();
  }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    String spec = System.getProperty("kithkyn.foundingProbe");
    if (spec == null || done) return;
    long tickMillis = (System.nanoTime() - tickStartedAt) / 1_000_000L;
    if (started && tickMillis > 1_000L) {
      Kithkyn.LOGGER.info("{} LONG TICK: {} ms", PREFIX, tickMillis);
    }
    if (++ticks < SETTLE_TICKS) return;
    ServerLevel level = event.getServer().overworld();
    if (!started) {
      started = true;
      sites = java.util.List.of(spec.split(";"));
      Thread server = Thread.currentThread();
      sampler = new Thread(() -> sample(server), "founding-probe-sampler");
      sampler.setDaemon(true);
      sampler.start();
      request(event, level);
      return;
    }
    long elapsed = System.nanoTime() - startedAt;
    int standing = 0;
    for (Village village : VillageManager.get(level).getVillages().values()) {
      if (village.getTownCenter() != null && village.getCenterPosition().closerThan(site, 64.0D)
          && village.getBuildings().size() >= 7) {
        standing++;
      }
    }
    // Every earlier site in this run sits within 64 blocks too, so count them.
    if (standing >= next) {
      Kithkyn.LOGGER.info("{} site {} of {} stands, {} s after its request", PREFIX, next, sites.size(),
          String.format("%.1f", elapsed / 1e9));
      if (next >= sites.size()) {
        finish(event, "RESULT PASS: every site founded");
        return;
      }
      request(event, level);
      return;
    }
    if (elapsed > GIVE_UP_NANOS) finish(event, "RESULT FAIL: site " + (next) + " not founded within ten minutes");
  }

  private static void request(ServerTickEvent.Post event, ServerLevel level) {
    String[] parts = sites.get(next).split(",");
    next++;
    site = new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    VillageStyle style = parts.length > 3 ? VillageStyle.parse(parts[3]) : null;
    startedAt = System.nanoTime();
    Kithkyn.LOGGER.info("{} requesting founding {} at {} in the {} style", PREFIX, next, site.toShortString(),
        style == null ? "biome" : style.id());
    boolean accepted = VillageManager.get(level).registerVillage(level, site, style, Kind.LIVING, false);
    Kithkyn.LOGGER.info("{} request {}", PREFIX, accepted ? "accepted; waiting for the name" : "REFUSED");
    if (!accepted) finish(event, "RESULT FAIL: founding " + next + " refused");
  }

  private static void finish(ServerTickEvent.Post event, String result) {
    done = true;
    Kithkyn.LOGGER.info("{} {}", PREFIX, result);
    event.getServer().halt(false);
  }

  /** Every two seconds, the server thread's innermost frames, so a long tick shows where it is. */
  private static void sample(Thread server) {
    long lastTickSeen = -1;
    while (!done) {
      try {
        Thread.sleep(2_000L);
      } catch (InterruptedException interrupted) {
        return;
      }
      StackTraceElement[] stack = server.getStackTrace();
      StringBuilder out = new StringBuilder();
      int shown = 0;
      for (StackTraceElement frame : stack) {
        if (shown >= 14) break;
        String cls = frame.getClassName();
        if (!cls.startsWith("com.quzzar") && !cls.startsWith("net.minecraft") && shown > 0) continue;
        out.append("\n    ").append(cls.substring(cls.lastIndexOf('.') + 1)).append('.')
            .append(frame.getMethodName()).append(':').append(frame.getLineNumber());
        shown++;
      }
      Kithkyn.LOGGER.info("{} sample{}", PREFIX, out);
    }
  }

}
