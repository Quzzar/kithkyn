package com.quzzar.kithkyn.dev;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.PersonEntityType;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.events.AllayEvents;
import com.quzzar.kithkyn.village.Occupation;
import com.quzzar.kithkyn.village.Storehouse;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.VillageAllays;
import com.quzzar.kithkyn.village.VillageManager;
import com.quzzar.kithkyn.village.buildings.Building;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Real adoption, counter binding, gathering, shelving, reload and death for an
 * allay quartermaster, on a disposable local server. Enable with
 * {@code -Dkithkyn.allays.verify=true}; never on a review or play world, since it
 * builds its own fixture and stops the server when finished.
 */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class AllayVerification {
  private static final BlockPos ORIGIN = new BlockPos(84, 5, 84);
  private static int ticks;
  private static FixtureVillage village;
  private static RealPerson guard;
  private static RealPerson secondGuard;
  private static Allay allay;
  private static BlockPos counter;
  private static CompoundTag saved;
  private static String name;
  private static int reloadTick = -1;

  private AllayVerification() {
  }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (!Boolean.getBoolean("kithkyn.allays.verify")) {
      return;
    }
    ticks++;
    ServerLevel level = event.getServer().overworld();
    try {
      if (ticks == 40) {
        setup(level);
      } else if (ticks == 90) {
        check(VillageAllays.villageId(allay).isEmpty(), "storehouse allay joined before its quartermaster took it on");
        check(allay.getBrain().getMemory(MemoryModuleType.LIKED_NOTEBLOCK_POSITION)
            .map(pos -> pos.pos().equals(counter)).orElse(false), "waiting allay is not tethered to the storehouse note block");
      } else if (ticks == 100) {
        recruit();
      } else if (ticks > 100 && ticks % 20 == 0 && saved == null) {
        int shelved = wheatOnShelves(level);
        if (shelved == 20 && sourceContainer(level).isEmpty()) {
          Kithkyn.LOGGER.info("[allay-verify] collected and shelved 20 wheat after {} ticks", ticks - 100);
          reload(level);
        } else if (ticks >= 2400) {
          throw new AssertionError("haul never completed: shelved=" + shelved + " source=" + sourceContainer(level).getItem(0)
              + " carried=" + allay.getInventory().getItem(0) + " at=" + allay.position()
              + " walk=" + allay.getBrain().getMemory(MemoryModuleType.WALK_TARGET).isPresent());
        }
      } else if (reloadTick > 0 && ticks == reloadTick + 60) {
        afterReload(level);
        Kithkyn.LOGGER.info("[allay-verify] RESULT PASS: storehouse tether, quartermaster adoption, collection, delivery to the shelves, reload and death");
        event.getServer().halt(false);
      }
    } catch (Exception | AssertionError failure) {
      Kithkyn.LOGGER.error("[allay-verify] RESULT FAIL at tick {}", ticks, failure);
      event.getServer().halt(false);
    }
  }

  private static void setup(ServerLevel level) {
    for (int x = 4; x <= 6; x++) {
      for (int z = 4; z <= 6; z++) {
        level.setChunkForced(x, z, true);
      }
    }
    level.setDayTime(6000);
    for (BlockPos pos : BlockPos.betweenClosed(64, 4, 64, 110, 14, 110)) {
      level.setBlock(pos, pos.getY() == 4 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
    }
    village = new FixtureVillage();
    village.attach(level);
    VillageManager.get(level).getVillages().put(village.getID(), village);
    List<BlockPos> shelves = Storehouse.chests(village.storehouse);
    check(shelves.size() >= 2, "fixture storehouse declares no containers");
    for (BlockPos shelf : shelves) {
      level.setBlock(shelf, Blocks.BARREL.defaultBlockState(), 3);
    }
    level.setBlock(village.source, Blocks.CHEST.defaultBlockState(), 3);
    sourceContainer(level).setItem(0, new ItemStack(Items.WHEAT, 20));
    counter = shelves.getFirst().offset(-2, 0, 0);
    level.setBlock(counter, Blocks.NOTE_BLOCK.defaultBlockState(), 3);
    check(counter.equals(VillageAllays.home(level, village)), "home search missed the authored note block");
    guard = resident(level, 79.5, 80.5);
    secondGuard = resident(level, 80.5, 81.5);
    allay = EntityType.ALLAY.create(level);
    allay.moveTo(82.5, 6, 80.5, 0, 0);
    allay.getPersistentData().putUUID(VillageAllays.SPAWNED_BY_BUILDING_KEY, village.storehouse.getUUID());
    check(level.addFreshEntity(allay), "allay spawn failed");
    allay.setLeashedTo(guard, false);
    check(!VillageAllays.canAdopt(guard, allay), "leashed allay was eligible");
    allay.dropLeash(false, false);
    guard.setNoAi(true);
    secondGuard.setNoAi(true);
  }

  private static void recruit() {
    guard.setNoAi(false);
    secondGuard.setNoAi(false);
    guard.setOccupation(Occupation.GUARD);
    check(!VillageAllays.canAdopt(guard, allay), "a guard could adopt; only the quartermaster takes on helpers");
    guard.setOccupation(Occupation.QUARTERMASTER);
    check(VillageAllays.canAdopt(guard, allay), "eligible encounter lost before scan: distance="
        + guard.distanceToSqr(allay) + " sight=" + guard.hasLineOfSight(allay) + " village=" + (guard.getVillage() == village));
    guard.tickCount = Math.floorMod(50 - guard.getId(), 100);
    AllayEvents.tick(new EntityTickEvent.Post(guard));
    check(VillageAllays.village(allay) == village && allay.hasCustomName(), "natural hook failed to name or adopt");
    check(!allay.getPersistentData().hasUUID(VillageAllays.SPAWNED_BY_BUILDING_KEY), "waiting tag survived adoption");
    check(!allay.isCustomNameVisible(), "nameplate should stay crosshair-only");
    check(village.getAllays().members().containsKey(allay.getUUID()), "roster missing the adopted allay");
    secondGuard.setOccupation(Occupation.QUARTERMASTER);
    check(!VillageAllays.adopt(secondGuard, allay), "second quartermaster claimed the same allay");
    name = allay.getCustomName().getString();
    guard.setNoAi(true);
    secondGuard.setNoAi(true);
  }

  private static void reload(ServerLevel level) {
    saved = allay.saveWithoutId(new CompoundTag());
    allay.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
    check(village.getAllays().members().containsKey(allay.getUUID()), "unloading was treated as death");
    Allay reloaded = EntityType.ALLAY.create(level);
    reloaded.load(saved);
    check(level.addFreshEntity(reloaded), "reloaded allay could not rejoin");
    allay = reloaded;
    reloadTick = ticks;
  }

  private static void afterReload(ServerLevel level) {
    check(VillageAllays.village(allay) == village, "membership lost across reload");
    check(name.equals(allay.getName().getString()), "name lost across reload");
    check(!allay.isCustomNameVisible(), "reload restored an always-visible nameplate");
    check(allay.getBrain().getMemory(MemoryModuleType.WALK_TARGET).isPresent()
        || allay.position().distanceToSqr(counter.getX() + 0.5D, counter.getY() + 1.5D, counter.getZ() + 0.5D) <= 16.0D,
        "reloaded keeper neither heading home nor at home: " + allay.position());
    UUID id = allay.getUUID();
    allay.kill();
    check(!village.getAllays().members().containsKey(id), "death left the roster entry");
  }

  private static Container sourceContainer(ServerLevel level) {
    return (Container) level.getBlockEntity(village.source);
  }

  private static int wheatOnShelves(ServerLevel level) {
    int total = 0;
    for (Container shelf : Storehouse.containers(level, Storehouse.chests(village.storehouse))) {
      for (int slot = 0; slot < shelf.getContainerSize(); slot++) {
        if (shelf.getItem(slot).is(Items.WHEAT)) {
          total += shelf.getItem(slot).getCount();
        }
      }
    }
    return total;
  }

  private static RealPerson resident(ServerLevel level, double x, double z) {
    RealPerson person = PersonEntityType.PERSON.get().create(level);
    person.setVillage(village.getID());
    person.setVillageName(village.getName());
    person.setOccupation(Occupation.QUARTERMASTER);
    person.moveTo(x, 5, z, 0, 0);
    village.getPopulation().add(person.getUUID());
    check(level.addFreshEntity(person), "guard spawn failed");
    return person;
  }

  private static void check(boolean condition, String message) {
    if (!condition) {
      throw new AssertionError(message);
    }
  }

  /** A fixed settlement with one storehouse and one workplace chest, so no construction or model calls run. */
  private static final class FixtureVillage extends Village {
    private final Building storehouse = new Building(ORIGIN, "storehouse_plains_1", Rotation.NONE);
    private final BlockPos source = ORIGIN.offset(-8, 1, 6);

    private FixtureVillage() {
      super("Allay Test Village");
      storehouse.setCenterLocation(ORIGIN.asLong());
      storehouse.setRadius(4);
    }

    @Override
    public void update(ServerLevel level) { }

    @Override
    public Building getTownCenter() { return storehouse; }

    @Override
    public Collection<Building> getBuildings() { return List.of(storehouse); }

    @Override
    public Building getBuilding(UUID id) { return storehouse.getUUID().equals(id) ? storehouse : null; }

    @Override
    public BlockPos getNearestContainer(BlockPos from, java.util.Collection<BlockPos> skip) {
      return skip.contains(this.source) ? BlockPos.ZERO : this.source;
    }

    @Override
    public boolean hasClaimedWithin(BlockPos pos, int padding) {
      return pos.distSqr(ORIGIN) < 32 * 32;
    }
  }
}
