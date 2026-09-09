package com.quzzar.kithkyn.dev;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.PersonEntityType;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.village.Occupation;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.buildings.Building;
import com.quzzar.kithkyn.village.buildings.BuildingInfo;
import com.quzzar.kithkyn.village.buildings.BuildingFootprint;
import com.quzzar.kithkyn.village.buildings.BuildingUpgrade;
import com.quzzar.kithkyn.village.buildings.Buildings;
import com.quzzar.kithkyn.village.buildings.InstantBuildStructure;
import com.quzzar.kithkyn.village.buildings.VillageStyle;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.pathfinder.Path;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Full catalog access and real founding, only on a disposable -Dkithkyn.birch.verify=true server. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class BirchVillageVerification {
  private static final BlockPos SITE = new BlockPos(2400, 160, 2400);
  private static final List<String> failures = new ArrayList<>();
  private static List<BuildingInfo> catalog;
  private static RealPerson walker;
  private static Building building;
  private static BoundingBox bounds;
  private static int tick;
  private static int index;
  private static int routes;
  private static boolean walking;
  private static int walkIndex;
  private static int walkStarted;
  private static BlockPos walkTarget;
  private static RealPerson liveWalker;
  private static Village walkVillage;
  private static boolean mineReturning;
  private static boolean centerReturning;
  private static final String[] WALK_BUILDINGS = {
      "watchtower_birch_forest_1", "watchtower_birch_forest_2", "bakery_birch_forest_1", "stoneworks_birch_forest_1",
      "village_center_birch_forest_1", "mine_birch_forest_1"
  };
  private static final BlockPos[] WALK_TARGETS = {
      new BlockPos(9, 10, 7), new BlockPos(7, 10, 8), new BlockPos(7, 5, 11), new BlockPos(6, 4, 8),
      new BlockPos(23, 5, 14), new BlockPos(8, 0, 7)
  };

  private BirchVillageVerification() { }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    boolean mineEntryOnly = Boolean.getBoolean("kithkyn.birch.mineEntryVerify");
    if (!Boolean.getBoolean("kithkyn.birch.verify") && !mineEntryOnly) {
      return;
    }
    tick++;
    ServerLevel level = event.getServer().overworld();
    try {
      if (!mineEntryOnly) NaturalFoundingVerification.tick(level);
      if (walking) {
        walk(level);
      } else if (tick == 40) {
        verifySavedVillages(level);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, event.getServer());
        level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, event.getServer());
        level.setDayTime(6000);
        for (int x = 147; x <= 153; x++) {
          for (int z = 147; z <= 153; z++) {
            level.setChunkForced(x, z, true);
          }
        }
        catalog = Buildings.allBuildings().values().stream()
            .filter(info -> info.getVariant().equals("birch_forest"))
            .sorted(java.util.Comparator.comparing(BuildingInfo::getName)).toList();
        check(catalog.size() == 22, "Expected 22 approved buildings");
        walker = PersonEntityType.PERSON.get().create(level);
        setProbeStats(walker);
        walker.setNoAi(true);
        walker.getNavigation().setCanFloat(true);
        if (mineEntryOnly) {
          walking = true;
          walkIndex = (WALK_BUILDINGS.length - 1) * 4;
          event.getServer().tickRateManager().setTickRate(100.0F);
        }
      } else if (tick >= 50 && catalog != null && index < catalog.size() * 4) {
        if ((tick - 50) % 4 == 0) {
          place(level);
        } else if ((tick - 50) % 4 == 3) {
          inspect(level);
          index++;
        }
      } else if (catalog != null && index == catalog.size() * 4) {
        verifyFounding(level);
        NaturalFoundingVerification.start(level);
        check(failures.isEmpty(), String.join("\n", failures));
        Kithkyn.LOGGER.info("[birch-verify] Catalog paths PASS; beginning actual entity walking");
        walking = true;
        event.getServer().tickRateManager().setTickRate(100.0F);
        index++;
      }
    } catch (Exception | AssertionError failure) {
      Kithkyn.LOGGER.error("[birch-verify] RESULT FAIL", failure);
      event.getServer().halt(false);
      index = Integer.MAX_VALUE;
    }
  }

  private static void place(ServerLevel level) {
    BuildingInfo info = catalog.get(index / 4);
    Rotation rotation = Rotation.values()[index % 4];
    place(level, info, rotation);
  }

  private static void place(ServerLevel level, BuildingInfo info, Rotation rotation) {
    level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class,
        new net.minecraft.world.phys.AABB(SITE).inflate(50, 35, 50))
        .forEach(net.minecraft.world.entity.Entity::discard);
    building = new Building(SITE.below(1 + info.getSink()), info.getName(), rotation);
    var template = level.getStructureManager().getOrCreate(ResourceLocation.fromNamespaceAndPath(Kithkyn.MODID, info.getName()));
    BlockPos origin = BlockPos.of(building.getOriginLocation());
    bounds = BuildingFootprint.bounds(template, rotation).moved(origin.getX(), origin.getY(), origin.getZ());
    // The surrounding ground stays at the founding plane; the center's enclosed
    // basement must be excavated by the shipped template, not by this fixture.
    for (BlockPos pos : BlockPos.betweenClosed(SITE.offset(-38, -5, -38), SITE.offset(38, 30, 38))) {
      level.setBlock(pos, pos.getY() < SITE.getY() ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
    }
    BlockPos outside = new BlockPos(bounds.minX() - 1, SITE.getY() + 2, bounds.getCenter().getZ());
    level.setBlock(outside, Blocks.STONE.defaultBlockState(), 2);
    HashSet<Long> claims = new HashSet<>();
    check(new InstantBuildStructure(building, new java.util.Random(index), level)
        .seatAtOrigin(origin, claims).buildInstantly(), "Placement failed");
    // Run the actual village completion sweep for every catalog asset, not just raw template placement.
    new NavigationVillage(level, building);
    for (var authored : template.palettes.getFirst().blocks()) {
      if (com.quzzar.kithkyn.village.BlockOwnership.isPlanted(authored.state()) || authored.state().is(Blocks.TALL_GRASS)) {
        check(level.getBlockState(origin.offset(authored.pos().rotate(rotation))).is(authored.state().getBlock()),
            info.getName() + " lost authored planting during completion at " + authored.pos());
      }
    }
    check(level.getBlockState(outside).is(Blocks.STONE), info.getName()+" carved terrain outside its authored envelope");
    check(!claims.contains(BlockPos.asLong(outside.getX(), 0, outside.getZ())), "Capture padding entered claim grid");
    check(claims.size() == bounds.getXSpan()*bounds.getZSpan(), "Claimed footprint is padded");
  }

  private static void inspect(ServerLevel level) {
    BuildingInfo info = building.getInfo();
    if (building.getName().equals("fishery_birch_forest_1")) {
      BlockPos barrel = BlockPos.of(building.getOriginLocation())
          .offset(new BlockPos(10, 2, 10).rotate(building.getRotation()));
      check(level.getBlockState(barrel).is(Blocks.BARREL), "Missing relocated fishery barrel");
      check(level.getBlockEntity(barrel) instanceof net.minecraft.world.Container container && container.isEmpty(),
          "The relocated barrel must be usable empty storage in " + building.getRotation());
    }
    if (building.getName().equals("watchtower_birch_forest_1")) {
      Village upgradeVillage = new NavigationVillage(level, building);
      check(BuildingUpgrade.fits(upgradeVillage, building, Buildings.getByName("watchtower_birch_forest_2")),
          "The tight second tower cannot upgrade its predecessor in " + building.getRotation());
    }
    for (long position : info.getBedLocations()) {
      route(level, "bed", position, 1);
    }
    for (long position : info.getContainerLocations()) {
      route(level, "communal container", position, 1);
    }
    for (long position : info.getPersonalContainerLocations()) {
      route(level, "personal chest", position, 1);
    }
    info.getWorkLocations().forEach((position, occupation) ->
        route(level, occupation.name(), position, occupation == Occupation.GUARD ? 0 : 1));
    if (info.getName().equals("village_center_birch_forest_1")) verifyTallCenterAccess();
    Kithkyn.LOGGER.info("[birch-verify] {} {} routes checked, cumulative failures={}", info.getName(), building.getRotation(), failures.size());
  }

  /** Test both directions through all four exits using the trapped residents' sizes and the ordinary maximum. */
  private static void verifyTallCenterAccess() {
    BlockPos origin = BlockPos.of(building.getOriginLocation());
    BlockPos inside = origin.offset(new BlockPos(14,1,12).rotate(building.getRotation()));
    for (int size : new int[]{13,15,18}) {
      setProbeStats(walker, size);
      for (BlockPos local : List.of(new BlockPos(14,5,5), new BlockPos(5,5,14),
          new BlockPos(23,5,14), new BlockPos(14,5,23))) {
        BlockPos outside = origin.offset(local.rotate(building.getRotation()));
        for (boolean leaving : new boolean[]{true,false}) {
          BlockPos start = leaving ? inside : outside;
          BlockPos target = leaving ? outside : inside;
          walker.getNavigation().stop();
          walker.moveTo(start.getX()+0.5D,start.getY(),start.getZ()+0.5D,0,0);
          walker.setOnGround(true);
          Path path = walker.getNavigation().createPath(target,0);
          routes++;
          check(path != null && path.canReach(), "Center size="+size+" "+building.getRotation()
              +" "+(leaving?"exit":"return")+" "+local+" is unreachable");
        }
      }
    }
    setProbeStats(walker);
    Kithkyn.LOGGER.info("[birch-verify] Tall center access PASS {}: sizes 13, 15, 18 through all four exits, both directions", building.getRotation());
  }

  private static void route(ServerLevel level, String role, long local, int accuracy) {
    walker.getNavigation().stop();
    walker.moveTo(bounds.minX() - 1.5, SITE.getY(), bounds.minZ() - 1.5, 0, 0);
    walker.setOnGround(true);
    BlockPos target = BlockPos.of(building.getOriginLocation()).offset(BlockPos.of(local).rotate(building.getRotation()));
    Path path = walker.getNavigation().createPath(target, accuracy);
    routes++;
    // Ground navigation raises solid targets to the air above them. A path
    // labelled partial may still finish beside the actual chest or bed, which
    // is exactly where the corresponding work/sleep goal needs the person.
    double reachSquared = role.equals("GUARD") ? 2.25D
        : role.equals("bed") || role.equals("personal chest") ? 4.0D : 9.0D;
    // The normal movement goals walk partial paths and ask again from their new
    // location. A single search from outside is not a full route across a
    // building whose entry is on the opposite side of the local search region.
    HashSet<BlockPos> ends = new HashSet<>();
    for (int hop = 0; path != null && hop < 8; hop++) {
      for (int node = 0; node < path.getNodeCount(); node++) {
        if (path.getNode(node).asBlockPos().distSqr(target) <= reachSquared) {
          return;
        }
      }
      BlockPos end = path.getEndNode().asBlockPos();
      if (!ends.add(end)) {
        break;
      }
      walker.moveTo(end.getX() + 0.5D, end.getY(), end.getZ() + 0.5D, 0, 0);
      walker.setOnGround(true);
      walker.getNavigation().stop();
      path = walker.getNavigation().createPath(target, accuracy);
    }
    if (path == null || path.getEndNode().asBlockPos().distSqr(target) > reachSquared) {
      String failure = building.getName() + " " + building.getRotation() + " " + role + " "
          + BlockPos.of(local).toShortString() + " unreachable; end="
          + (path == null ? "none" : path.getEndNode().asBlockPos().subtract(BlockPos.of(building.getOriginLocation())))
          + " target=" + level.getBlockState(target);
      failures.add(failure);
      Kithkyn.LOGGER.error("[birch-verify] {}", failure);
    }
  }

  /** Real physics, move control and door opening, without unrelated work/social goals. */
  private static void walk(ServerLevel level) {
    if (walkIndex == WALK_BUILDINGS.length * 4) {
      if (Boolean.getBoolean("kithkyn.birch.mineEntryVerify")) {
        Kithkyn.LOGGER.info("[birch-verify] RESULT PASS: excavated mine exit and re-entry in all four rotations (8 actual walks), legacy repair={}",
            Boolean.getBoolean("kithkyn.birch.mineEntryLegacy"));
      } else {
        NaturalFoundingVerification.requirePassed();
        Kithkyn.LOGGER.info("[birch-verify] RESULT PASS: 88 rotated templates, {} planned access routes, 32 actual entity walks including tall center exit/re-entry, and complete biome-selected founding", routes);
      }
      level.getServer().halt(false);
      walking = false;
      return;
    }
    if (liveWalker == null) {
      String name = WALK_BUILDINGS[walkIndex / 4];
      Rotation rotation = Rotation.values()[walkIndex % 4];
      place(level, Buildings.getByName(name), rotation);
      if (name.equals("mine_birch_forest_1") && Boolean.getBoolean("kithkyn.birch.mineEntryLegacy")) {
        // Existing tunnels must keep their saved frame. A local surface-only
        // repair moves the stairs outward, leaving a landing before that frame.
        var saved = Building.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, building).getOrThrow().getAsJsonObject();
        saved.add("mine_entrance", com.google.gson.JsonParser.parseString("{\"facing\":\"east\",\"offset\":[0,0,1]}"));
        building = Building.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, saved).getOrThrow();
        BlockPos origin = BlockPos.of(building.getOriginLocation());
        for (int z = 7; z <= 9; z++) {
          BlockPos step = origin.offset(new BlockPos(6, 0, z).rotate(rotation));
          level.setBlock(origin.offset(new BlockPos(5, 0, z).rotate(rotation)), level.getBlockState(step), 2);
          level.setBlock(step, Blocks.AIR.defaultBlockState(), 2);
          level.setBlock(step.below(), Blocks.COBBLESTONE.defaultBlockState(), 2);
        }
      }
      liveWalker = PersonEntityType.PERSON.get().create(level);
      setProbeStats(liveWalker);
      if (name.equals("village_center_birch_forest_1")) setProbeStats(liveWalker, 18);
      if (name.equals("mine_birch_forest_1")) {
        // Routing must see the actual village-owned shaft. An unaffiliated
        // navigation probe bypasses mine waypoint handling entirely.
        walkVillage = new NavigationVillage(level, building);
        com.quzzar.kithkyn.village.VillageManager.get(level).getVillages().put(walkVillage.getID(), walkVillage);
        liveWalker.setVillage(walkVillage.getID());
        // Replay an excavated entrance, not merely access to the surface station.
        // Real path wear lowers its dirt support by 1/16 of a block as well.
        var shaft = com.quzzar.kithkyn.village.buildings.MineShaft.of(building).getFirst();
        for (BlockPos local : BlockPos.betweenClosed(-2, -13, -2, 2, -1, 8)) {
          level.setBlock(shaft.mouth().offset(local.rotate(shaft.rotation())), Blocks.STONE.defaultBlockState(), 2);
        }
        for (BlockPos local : BlockPos.betweenClosed(-2, -12, -1, 2, -1, 8)) {
          if (com.quzzar.kithkyn.village.buildings.MineShaft.withinCorridor(local)) {
            level.setBlock(shaft.mouth().offset(local.rotate(shaft.rotation())), Blocks.AIR.defaultBlockState(), 2);
          }
        }
        level.setBlock(shaft.entranceClearance(), Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(shaft.entry().below(), Blocks.DIRT_PATH.defaultBlockState(), 2);
      }
      liveWalker.setPersistenceRequired();
      liveWalker.goalSelector.removeAllGoals(goal -> !(goal instanceof net.minecraft.world.entity.ai.goal.OpenDoorGoal)
          && !(goal instanceof com.quzzar.kithkyn.entities.ai.goals.OpenFenceGateGoal));
      liveWalker.targetSelector.removeAllGoals(goal -> true);
      liveWalker.moveTo(bounds.minX() - 1.5, SITE.getY(), bounds.minZ() - 1.5, 0, 0);
      if (name.equals("village_center_birch_forest_1")) {
        BlockPos inside = BlockPos.of(building.getOriginLocation()).offset(new BlockPos(14,1,12).rotate(rotation));
        liveWalker.moveTo(inside.getX()+0.5D,inside.getY(),inside.getZ()+0.5D,0,0);
      }
      if (name.equals("mine_birch_forest_1")) {
        BlockPos inside = mineDepthTarget();
        liveWalker.moveTo(inside.getX() + 0.5D, inside.getY(), inside.getZ() + 0.5D, 0, 0);
      }
      liveWalker.setOnGround(true);
      check(level.addFreshEntity(liveWalker), "Walking probe spawn failed");
      Kithkyn.LOGGER.info("[birch-verify] Walking body width={} height={} stats={}",
          liveWalker.getBbWidth(), liveWalker.getBbHeight(), liveWalker.getStatBlock());
      walkTarget = BlockPos.of(building.getOriginLocation()).offset(WALK_TARGETS[walkIndex / 4].rotate(rotation));
      if (name.equals("mine_birch_forest_1")) {
        walkTarget = BlockPos.of(building.getOriginLocation()).offset(new BlockPos(4, 1, 8).rotate(rotation));
      }
      walkStarted = tick;
    }
    if (liveWalker.distanceToSqr(walkTarget.getX() + 0.5D, walkTarget.getY(), walkTarget.getZ() + 0.5D) <= 2.25D) {
      Kithkyn.LOGGER.info("[birch-verify] WALK PASS {} {} in {} ticks", building.getName(), building.getRotation(), tick - walkStarted);
      if (building.getName().equals("mine_birch_forest_1") && !mineReturning) {
        mineReturning = true;
        walkTarget = mineDepthTarget();
        walkStarted = tick;
        liveWalker.getNavigation().stop();
        return;
      }
      mineReturning = false;
      if (building.getName().equals("village_center_birch_forest_1") && !centerReturning) {
        centerReturning = true;
        walkTarget = BlockPos.of(building.getOriginLocation()).offset(new BlockPos(14,1,12).rotate(building.getRotation()));
        walkStarted = tick;
        liveWalker.getNavigation().stop();
        return;
      }
      centerReturning = false;
      liveWalker.discard();
      liveWalker = null;
      if (walkVillage != null) {
        com.quzzar.kithkyn.village.VillageManager.get(level).getVillages().remove(walkVillage.getID());
        walkVillage = null;
      }
      walkIndex++;
      return;
    }
    check(tick - walkStarted < 1400, "Actual walking stuck: " + building.getName() + " " + building.getRotation()
        + " at " + liveWalker.position().subtract(net.minecraft.world.phys.Vec3.atLowerCornerOf(BlockPos.of(building.getOriginLocation())))
        + " targeting " + walkTarget.subtract(BlockPos.of(building.getOriginLocation())));
    if (tick - walkStarted > 5 && (liveWalker.getNavigation().isDone() || tick % 10 == 0)) {
      liveWalker.getNavigation().moveTo(walkTarget.getX() + 0.5D, walkTarget.getY(), walkTarget.getZ() + 0.5D, 0.6D);
    }
  }

  private static void verifyFounding(ServerLevel level) {
    verifyFounding(level, new BlockPos(3000, 160, 3000), false);
    verifyFounding(level, new BlockPos(-3000, 160, -3000), true);
  }

  /** A supported, excavated cell five columns down the real shaft frame. */
  private static BlockPos mineDepthTarget() {
    var shaft = com.quzzar.kithkyn.village.buildings.MineShaft.of(building).getFirst();
    return shaft.mouth().offset(new BlockPos(0, -7, 5).rotate(shaft.rotation()));
  }

  private static void verifyFounding(ServerLevel level, BlockPos site, boolean obstructed) {
    var ownership = com.quzzar.kithkyn.savedata.PlacedBlockStore.get(level);
    for (BlockPos pos : BlockPos.betweenClosed(site.offset(-60, -5, -60), site.offset(60, 25, 60))) {
      ownership.clearPlaced(pos);
      level.setBlock(pos, pos.getY() < site.getY() ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
    }
    if (obstructed) {
      // Only north and east are open: the old opposing-flank layout must fail here.
      for (BlockPos pos : BlockPos.betweenClosed(site.offset(-60, 0, -60), site.offset(60, 8, 60))) {
        if (pos.getX() < site.getX() - 12 || pos.getZ() > site.getZ() + 12) {
          level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
        }
      }
    }
    // Above founding's preparation headroom: the completion hook must remove the remnant.
    BlockPos overheadTree = site.offset(2, 32, 2);
    level.setBlock(overheadTree, Blocks.OAK_LOG.defaultBlockState(), 2);
    level.setBlock(overheadTree.above(), Blocks.OAK_LEAVES.defaultBlockState(), 2);
    BlockPos overheadLeaves = site.offset(3, 34, 3);
    level.setBlock(overheadLeaves, Blocks.OAK_LEAVES.defaultBlockState(), 2);
    Village village = new Village("Birch verification");
    village.attach(level);
    VillageStyle style = VillageStyle.fromBiome(level.getBiome(site), level.getSeed(), site);
    check(style == VillageStyle.BIRCH_FOREST, "Disposable world must use minecraft:birch_forest, selector got " + style);
    village.setStyle(style);
    village.initNew(site);
    check(village.getBuildings().size() == 3, "Founding must place center, storehouse and mine");
    check(village.getTotalBeds() == 4, "Founding must register four actual basement beds");
    check(level.getBlockState(overheadTree).isAir(), "Founding left an overhead natural trunk");
    check(level.getBlockState(overheadLeaves).isAir(), "Founding left canopy above its center");
    Building center = village.getTownCenter();
    check(center.getName().equals("village_center_birch_forest_1"), "Wrong center");
    check(BlockPos.of(center.getOriginLocation()).getY() == site.getY() - 1 - 4, "Basement not sunk four blocks below the ground course");
    BlockPos fire = BlockPos.of(center.getOriginLocation()).offset(new BlockPos(14, 1, 14).rotate(center.getRotation()));
    check(level.getBlockState(fire).is(Blocks.CAMPFIRE), "Approved basement campfire missing");
    check(fire.getX() == site.getX() && fire.getZ() == site.getZ(), "Founding anchor shifted");
    var sides = java.util.EnumSet.noneOf(net.minecraft.core.Direction.class);
    var centerTemplate = level.getStructureManager().getOrCreate(
        ResourceLocation.fromNamespaceAndPath(Kithkyn.MODID, center.getInfo().getPath()));
    BlockPos centerOrigin = BlockPos.of(center.getOriginLocation());
    BoundingBox centerBounds = BuildingFootprint.bounds(centerTemplate, center.getRotation())
        .moved(centerOrigin.getX(), centerOrigin.getY(), centerOrigin.getZ());
    for (Building companion : village.getBuildings()) {
      String category = companion.getInfo().getCategory();
      if (!category.equals("mine") && !category.equals("storehouse")) continue;
      BlockPos at = BlockPos.of(companion.getCenterLocation());
      var facing = companion.getRotation().rotate(companion.getInfo().getEntranceFacing());
      var companionTemplate = level.getStructureManager().getOrCreate(
          ResourceLocation.fromNamespaceAndPath(Kithkyn.MODID, companion.getInfo().getPath()));
      BlockPos companionOrigin = BlockPos.of(companion.getOriginLocation());
      var companionBounds = BuildingFootprint.bounds(companionTemplate, companion.getRotation())
          .moved(companionOrigin.getX(), companionOrigin.getY(), companionOrigin.getZ());
      int doubledCenterError = facing.getAxis() == net.minecraft.core.Direction.Axis.X
          ? companionBounds.minZ() + companionBounds.maxZ() - centerBounds.minZ() - centerBounds.maxZ()
          : companionBounds.minX() + companionBounds.maxX() - centerBounds.minX() - centerBounds.maxX();
      check(Math.abs(doubledCenterError) <= 1, category + " is not centered alongside the town center");
      sides.add(facing.getOpposite());
      int dot = facing.getStepX()*(fire.getX()-at.getX()) + facing.getStepZ()*(fire.getZ()-at.getZ());
      check(dot > 0, category+" entrance points away from the center");
      if (category.equals("storehouse")) {
        check(BlockPos.of(companion.getOriginLocation()).getY() == site.getY(), "Storehouse first step is still buried");
      }
    }
    check(sides.size() == 2, "Founding companions must use distinct sides");
    var footprints = village.getBuildings().stream().map(companion -> {
      var template = level.getStructureManager().getOrCreate(ResourceLocation.fromNamespaceAndPath(Kithkyn.MODID, companion.getInfo().getPath()));
      BlockPos origin = BlockPos.of(companion.getOriginLocation());
      return BuildingFootprint.bounds(template, companion.getRotation()).moved(origin.getX(), 0, origin.getZ());
    }).toList();
    for (BlockPos pos : BlockPos.betweenClosed(site.offset(-60, 0, -60), site.offset(60, 0, 60))) {
      boolean expected = footprints.stream().anyMatch(box -> pos.getX() >= box.minX() && pos.getX() <= box.maxX()
          && pos.getZ() >= box.minZ() && pos.getZ() <= box.maxZ());
      check(village.hasClaimed(pos) == expected, "A rejected trial site left a claim at " + pos);
    }
    if (obstructed) {
      check(sides.equals(java.util.EnumSet.of(net.minecraft.core.Direction.NORTH, net.minecraft.core.Direction.EAST)),
          "Founding must choose the open north/east sides, got " + sides);
      check(level.getBlockState(site.offset(-14, 3, 0)).is(Blocks.STONE), "Rejected west site was carved");
    } else {
      var iterator = sides.iterator();
      check(iterator.next().getOpposite() != iterator.next(), "Equal-cost flat terrain should prefer adjacent sides");
    }
    Kithkyn.LOGGER.info("[birch-verify] actual biome selection and full three-building founding PASS");
  }

  /** Optional read-only compatibility check of the actual dev save, never attached or ticked here. */
  private static void verifySavedVillages(ServerLevel level) throws java.io.IOException {
    String path = System.getProperty("kithkyn.birch.legacySave");
    if (path == null) return;
    var root = net.minecraft.nbt.NbtIo.readCompressed(java.nio.file.Path.of(path), net.minecraft.nbt.NbtAccounter.unlimitedHeap());
    var villages = root.getCompound("data").getCompound("Villages");
    int count = 0;
    for (String id : villages.getAllKeys()) {
      var source = villages.getCompound(id);
      Village village = Village.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, source).getOrThrow();
      var saved = (net.minecraft.nbt.CompoundTag) Village.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, village).getOrThrow();
      check(id.equals(village.getID()), "Saved village ID changed");
      for (String field : List.of("name", "town_center", "people")) {
        check(java.util.Objects.equals(source.get(field), saved.get(field)), "Saved village field changed: " + field);
      }
      check(source.getList("buildings", 10).size() == village.getBuildings().size(), "Lost saved buildings");
      for (var tag : source.getList("buildings", 10)) {
        var before = (net.minecraft.nbt.CompoundTag) tag;
        Building restored = Building.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, before).getOrThrow();
        var after = (net.minecraft.nbt.CompoundTag) Building.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, restored).getOrThrow();
        for (String field : before.getAllKeys()) {
          check(java.util.Objects.equals(before.get(field), after.get(field)), "Saved building field changed: " + field);
        }
        if (restored.getInfo().getCategory().equals("mine") && !before.contains("mine_entrance")) {
          check(restored.getMineEntrance().equals(BuildingInfo.MineEntrance.DEFAULT), "Legacy mine frame was redirected");
        }
      }
      var project = village.getCurrentProject();
      if (project != null) {
        var work = source.getCompound("project").getCompound("building");
        check(work.contains("template_snapshot"), "Pending old project must have its template frozen before deployment");
        project.attach(level, village.getIdentity());
        var snapshot = com.quzzar.kithkyn.utils.KithkynCodecs.EXACT_NBT.parse(net.minecraft.nbt.NbtOps.INSTANCE,
            work.get("template_snapshot")).getOrThrow();
        var expected = new net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate();
        expected.load(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK), snapshot);
        check(expected.save(new net.minecraft.nbt.CompoundTag()).equals(
            project.getStructureTemplate().save(new net.minecraft.nbt.CompoundTag())), "Pending project loaded a different block list");
        var encoded = (net.minecraft.nbt.CompoundTag) com.quzzar.kithkyn.village.buildings.StructureInProgress.CODEC
            .encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, project).getOrThrow();
        for (String field : List.of("index", "prep_break", "prep_fill", "location", "location2", "placed_blocks")) {
          check(java.util.Objects.equals(work.get(field), encoded.get(field)), "Pending project state changed: " + field);
        }
        check(snapshot.equals(com.quzzar.kithkyn.utils.KithkynCodecs.EXACT_NBT.parse(net.minecraft.nbt.NbtOps.INSTANCE,
            encoded.get("template_snapshot")).getOrThrow()), "Pending snapshot NBT types changed");
      }
      count++;
    }
    Kithkyn.LOGGER.info("[birch-verify] READ-ONLY SAVE PASS: {} existing dev villages decode and retain building state", count);
  }

  private static void check(boolean condition, String message) {
    if (!condition) {
      throw new AssertionError(message);
    }
  }

  private static void setProbeStats(RealPerson person) {
    setProbeStats(person, Integer.getInteger("kithkyn.birch.probeSize", 10));
  }

  private static void setProbeStats(RealPerson person, int size) {
    var stats = new net.minecraft.nbt.CompoundTag();
    stats.putInt(com.quzzar.kithkyn.entities.genetics.Stat.SIZE.getNbtKey(),
        size);
    person.setStatBlock(com.quzzar.kithkyn.entities.genetics.StatBlock.load(stats));
  }

  /** Real owned-building state without unrelated village planning during a route check. */
  private static final class NavigationVillage extends Village {
    private NavigationVillage(ServerLevel level, Building mine) {
      super("Mine access fixture");
      attach(level);
      addBuilding(mine);
    }

    @Override
    public void update(ServerLevel ignored) { }
  }
}
