package com.quzzar.kithkyn.dev;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.resources.ResourceKey;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.entities.AgeStage;
import com.quzzar.kithkyn.savedata.PlacedBlockStore;
import com.quzzar.kithkyn.village.GuardRole;
import com.quzzar.kithkyn.village.Occupation;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.buildings.Building;
import com.quzzar.kithkyn.village.buildings.BuildingInfo;
import com.quzzar.kithkyn.village.buildings.BuildingUpgrade;
import com.quzzar.kithkyn.village.buildings.Buildings;
import com.quzzar.kithkyn.village.buildings.MineShaft;
import com.quzzar.kithkyn.village.buildings.VillageStyle;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Shared private-catalog checks for the reviewed regional villages. Opt in with
 * the legacy Badlands flag or {@code -Dkithkyn.reviewedVillage.style=<style>}
 * for desert, floodplain or Jungle; each catalog's authored numbers live in its
 * {@link Catalog} record so the checks read facts rather than guess them.
 */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class BadlandsVillageVerification {
  private static final BlockPos UPGRADE_SITE = new BlockPos(2400, 159, 2400);
  private static final VillageStyle STYLE = VillageStyle.parse(
      System.getProperty("kithkyn.reviewedVillage.style", "badlands"));

  /**
   * What one reviewed catalog authored: template and housing counts, the
   * founding set the centre declares, the centre's civic layout and the biome
   * its disposable world must carry. A null bell means the centre has none.
   */
  private record Catalog(String prefix, int templates, int homes, int houseBeds, int[] houseTiers,
      Map<String, Integer> coupleRooms, List<String> marriedWorkplaces, boolean tavern, String[][] upgrades,
      int foundingBuildings, int foundingBeds, int foundingJobs, int guards, int centerJobs, int centerRooms,
      int roomsWithoutStorage, BlockPos plaza, @Nullable BlockPos bell, int campfires,
      ResourceKey<Biome> biome) { }

  private static final Catalog CATALOG = switch (STYLE) {
    case DESERT -> new Catalog("[desert-verify]", 28, 9, 15, new int[] {6, 2, 1}, Map.of(), List.of(), true,
        new String[][] {{id("storehouse", 1), id("storehouse", 2)}, {id("market", 1), id("market", 2)},
            {id("market", 2), id("market", 3)}},
        3, 5, 4, 1, 3, 4, 0, new BlockPos(8, 1, 5), new BlockPos(8, 4, 7), 1, Biomes.DESERT);
    case BADLANDS -> new Catalog("[badlands-verify]", 30, 12, 25, new int[] {6, 4, 2},
        Map.of("house_badlands_1__small_house_3", 1, "house_badlands_3", 1, "house_badlands_3__large_house_3", 2),
        List.of("farm_badlands_1", "butchery_badlands_1"), true,
        new String[][] {{id("storehouse", 1), id("storehouse", 2)}, {id("market", 1), id("market", 2)},
            {id("market", 2), id("market", 3)}},
        3, 10, 8, 5, 7, 10, 3, new BlockPos(12, 1, 17), new BlockPos(13, 2, 17), 2, Biomes.BADLANDS);
    // The floodplain centre has no beds, so its founding set adds three homes; its
    // one storehouse level and its two markets and farms are the only upgrades.
    case FLOODPLAIN -> new Catalog("[floodplain-verify]", 20, 2, 3, new int[] {1, 1, 0}, Map.of(), List.of(), false,
        new String[][] {{id("market", 1), id("market", 2)}, {id("market", 2), id("market", 3)},
            {id("farm", 1), id("farm", 2)}},
        6, 5, 4, 1, 3, 0, 0, new BlockPos(4, 1, 2), null, 1, Biomes.MANGROVE_SWAMP);
    case JUNGLE -> new Catalog("[jungle-verify]", 22, 4, 6, new int[] {2, 2, 0}, Map.of(), List.of(), false,
        new String[][] {{id("market", 1), id("market", 2)}, {id("market", 2), id("market", 3)}},
        7, 4, 4, 1, 4, 0, 0, new BlockPos(3, 1, 5), new BlockPos(2, 2, 5), 1, Biomes.JUNGLE);
    case BIRCH_FOREST -> null;
  };
  private static final String PREFIX = CATALOG == null ? "[reviewed-village-verify]" : CATALOG.prefix();
  private static int ticks;
  private static int upgrades;
  private static int foundingRotations;
  private static boolean naturalStarted;
  private static boolean finished;

  private BadlandsVillageVerification() { }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if ((!Boolean.getBoolean("kithkyn.badlands.verify")
        && System.getProperty("kithkyn.reviewedVillage.style") == null) || finished) return;
    ServerLevel level = event.getServer().overworld();
    if (++ticks < 40) return;
    try {
      if (ticks == 40) {
        check(CATALOG != null, "Only the reviewed private catalogs are supported");
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, event.getServer());
        level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, event.getServer());
        level.setDayTime(6000);
        verifyBiomeCoverage(level);
        verifyCatalogue(level);
        forceChunks(level, UPGRADE_SITE, 48);
      } else if (upgrades < CATALOG.upgrades().length * 4) {
        verifyUpgrade(level, CATALOG.upgrades()[upgrades / 4], Rotation.values()[upgrades % 4]);
        upgrades++;
      } else if (foundingRotations < 4) {
        verifyFounding(level, Rotation.values()[foundingRotations], foundingRotations);
        foundingRotations++;
      } else if (!naturalStarted) {
        NaturalFoundingVerification.start(level, STYLE, CATALOG.foundingBuildings(), CATALOG.foundingBeds());
        naturalStarted = true;
      } else {
        NaturalFoundingVerification.tick(level);
        if (NaturalFoundingVerification.hasPassed()) {
          verifyVillage(level, NaturalFoundingVerification.foundedVillage());
          finished = true;
          Kithkyn.LOGGER.info("{} RESULT PASS: {} strict native templates, {} housing alternatives, "
              + "{} upgrade fits, four founding rotations and codec reloads, natural biome selection, "
              + "{} beds, {} positions, distinct meeting/fire locations and village identity", PREFIX,
              CATALOG.templates(), CATALOG.homes(), CATALOG.upgrades().length * 4, CATALOG.foundingBeds(),
              CATALOG.foundingJobs());
          event.getServer().halt(false);
        }
      }
    } catch (Exception | AssertionError failure) {
      finished = true;
      Kithkyn.LOGGER.error(PREFIX + " RESULT FAIL", failure);
      event.getServer().halt(false);
    }
  }

  /** The biome mapping itself, asked as if every catalog were installed; the disposable world only carries one. */
  private static void verifyBiomeCoverage(ServerLevel level) {
    var registry = level.registryAccess().registryOrThrow(Registries.BIOME);
    java.util.function.Predicate<VillageStyle> everything = ignored -> true;
    for (var biome : List.of(Biomes.BADLANDS, Biomes.ERODED_BADLANDS, Biomes.WOODED_BADLANDS,
        Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.WINDSWEPT_SAVANNA)) {
      check(VillageStyle.fromBiome(registry.getHolderOrThrow(biome), level.getSeed(), UPGRADE_SITE, everything)
          == VillageStyle.BADLANDS, "Pueblo coverage missing " + biome.location());
    }
    check(VillageStyle.fromBiome(registry.getHolderOrThrow(Biomes.DESERT), 0L, BlockPos.ZERO, everything)
        == VillageStyle.DESERT, "Sandy desert must remain Desert");
    for (var biome : List.of(Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST)) {
      check(VillageStyle.fromBiome(registry.getHolderOrThrow(biome), 0L, BlockPos.ZERO, everything)
          == VillageStyle.BIRCH_FOREST, "Birch coverage changed " + biome.location());
    }
    check(VillageStyle.fromBiome(registry.getHolderOrThrow(Biomes.MANGROVE_SWAMP), 0L, BlockPos.ZERO, everything)
        == VillageStyle.FLOODPLAIN, "Mangrove swamp must select Floodplain");
    check(VillageStyle.fromBiome(registry.getHolderOrThrow(Biomes.SWAMP), 0L, BlockPos.ZERO, everything)
        == VillageStyle.FLOODPLAIN, "Plain swamp is hot and wet and builds floodplain until it has a catalog of its own");
    for (var biome : List.of(Biomes.JUNGLE, Biomes.BAMBOO_JUNGLE, Biomes.SPARSE_JUNGLE)) {
      check(VillageStyle.fromBiome(registry.getHolderOrThrow(biome), 0L, BlockPos.ZERO, everything)
          == VillageStyle.JUNGLE, "Jungle coverage missing " + biome.location());
    }
    Kithkyn.LOGGER.info("{} BIOMES PASS: Pueblo, Desert, Birch, Floodplain and all three Jungle biomes", PREFIX);
  }

  private static void verifyCatalogue(ServerLevel level) {
    check(Buildings.hasFoundingSet(STYLE), "Missing full " + STYLE + " founding set");
    List<BuildingInfo> catalogue = Buildings.catalogue(STYLE);
    check(catalogue.size() == CATALOG.templates(),
        "Expected " + CATALOG.templates() + " catalogue entries, got " + catalogue.size());
    for (BuildingInfo info : catalogue) {
      check(info.hasWellFormedId() && info.getVariant().equals(STYLE.id()), "Foreign catalogue entry " + info.getName());
      check(info.validate() == null, info.getName() + ": " + info.validate());
      var template = level.getStructureManager().get(ResourceLocation.fromNamespaceAndPath(Kithkyn.MODID, info.getPath()))
          .orElseThrow(() -> new AssertionError("Missing native template " + info.getName()));
      check(!template.palettes.isEmpty() && !template.palettes.getFirst().blocks().isEmpty(), "Empty " + info.getName());
      check(template.getSize().getX() > 0 && template.getSize().getY() > 0 && template.getSize().getZ() > 0,
          "Invalid template dimensions " + info.getName());
      var blocks = template.palettes.getFirst().blocks();
      var states = blocks.stream().collect(java.util.stream.Collectors.toMap(
          block -> block.pos(), block -> block.state()));
      var physicalBeds = blocks.stream().filter(block -> block.state().getBlock() instanceof BedBlock
          && block.state().getValue(BedBlock.PART) == net.minecraft.world.level.block.state.properties.BedPart.HEAD)
          .map(block -> block.pos().asLong()).collect(java.util.stream.Collectors.toSet());
      check(physicalBeds.equals(new java.util.HashSet<>(info.getBedLocations())),
          "Physical and assigned beds differ in " + info.getName());
      var slots = info.getVillageIdentitySlots();
      for (long bed : info.getBedLocations()) {
        BlockPos position = BlockPos.of(bed);
        var state = states.get(position);
        check(state.is(Blocks.WHITE_BED), "Authored bed must be neutral in " + info.getName());
        check(slots.primaryBlocks().contains(position) ^ slots.secondaryBlocks().contains(position),
            "Every bed must have one village color in " + info.getName());
        var foot = states.get(position.relative(state.getValue(BedBlock.FACING).getOpposite()));
        check(foot != null && foot.is(Blocks.WHITE_BED)
            && foot.getValue(BedBlock.PART) == net.minecraft.world.level.block.state.properties.BedPart.FOOT,
            "Incomplete bed in " + info.getName() + " at " + position);
      }
      for (var pair : info.getCoupleBeds()) {
        check(slots.primaryBlocks().contains(pair.first()) == slots.primaryBlocks().contains(pair.second()),
            "Couple beds must share a village color in " + info.getName());
      }
      for (BlockPos flag : slots.banners()) {
        var state = states.get(flag);
        check(state != null && (state.is(Blocks.WHITE_BANNER) || state.is(Blocks.WHITE_WALL_BANNER)),
            "Missing neutral village flag in " + info.getName() + " at " + flag);
      }
      for (long container : java.util.stream.Stream.concat(info.getContainerLocations().stream(),
          info.getPersonalContainerLocations().stream()).toList()) {
        check(blocks.stream().anyMatch(block -> block.pos().asLong() == container && block.nbt() != null),
            "Declared container has no block entity in " + info.getName() + " at " + BlockPos.of(container));
      }
    }
    var homes = catalogue.stream().filter(info -> info.getCategory().equals("house")).toList();
    check(homes.size() == CATALOG.homes(), "Expected " + CATALOG.homes() + " distinct houses");
    check(homes.stream().mapToInt(info -> info.getBedLocations().size()).sum() == CATALOG.houseBeds(),
        "Wrong combined house bed count");
    for (int tier = 1; tier <= 3; tier++) {
      int selectedTier = tier;
      int expected = CATALOG.houseTiers()[tier - 1];
      check(Buildings.alternatives("house", tier, STYLE).size() == expected
          && homes.stream().filter(info -> info.getLevel() == selectedTier).count() == expected,
          "Unavailable housing alternatives at tier " + tier);
    }
    if (STYLE == VillageStyle.DESERT) {
      check(homes.stream().mapToInt(home -> home.getCoupleBeds().size()).sum() == 1,
          "Desert must retain its one approved couple room");
      for (BuildingInfo home : homes) {
        check(home.getUpgradesFrom() == null && (home.getLevel() == 1 || home.isStandalone()),
            "Unrelated Desert homes must remain independent construction choices: " + home.getName());
      }
    } else {
      for (BuildingInfo home : homes) {
        check(home.getCoupleBeds().size() == CATALOG.coupleRooms().getOrDefault(home.getName(), 0),
            "Wrong couple rooms " + home.getName());
      }
      for (String workplace : CATALOG.marriedWorkplaces()) {
        check(info(workplace).getWorkerCoupleRoomCount() == 1, "Lost married worker room " + workplace);
      }
    }
    for (BuildingInfo home : homes) {
      check(home.getBedContainers() != null, "Lost explicit room storage " + home.getName());
    }
    if (CATALOG.tavern()) {
      BuildingInfo tavern = info(id("tavern", 1));
      check(tavern.getBedLocations().size() == 2 && tavern.getWorkerSingleBedCount() == 1,
          "Tavern must keep one staff bed and one general bed");
    }
    Kithkyn.LOGGER.info("{} CATALOGUE PASS: {} actual templates and all {} authored housing choices", PREFIX,
        CATALOG.templates(), CATALOG.homes());
  }

  private static void verifyUpgrade(ServerLevel level, String[] edge, Rotation rotation) {
    BuildingInfo target = info(edge[1]);
    check(edge[0].equals(target.getUpgradesFrom()), "Wrong predecessor for " + edge[1]);
    Building standing = ApprovedStructureAccess.place(level, UPGRADE_SITE, info(edge[0]), rotation);
    Village village = new ApprovedStructureAccess.VillageFixture(level, standing, false);
    village.setStyle(STYLE);
    var placement = BuildingUpgrade.findPlacement(village, target);
    check(placement != null, "Upgrade cannot fit " + edge[0] + " -> " + edge[1] + " " + rotation);
    check(placement.standing().getUUID().equals(standing.getUUID()) && placement.rotation() == rotation,
        "Upgrade changed the standing building identity/orientation");
    Kithkyn.LOGGER.info("{} UPGRADE PASS {} -> {} {}: {}", PREFIX, edge[0], edge[1], rotation, placement.bounds());
  }

  private static void verifyFounding(ServerLevel level, Rotation rotation, int index) throws ReflectiveOperationException {
    BlockPos site = new BlockPos(3000 + index * 256, 160, 3000);
    forceChunks(level, site, 72);
    var ownership = PlacedBlockStore.get(level);
    for (BlockPos position : BlockPos.betweenClosed(site.offset(-72, -7, -72), site.offset(72, 32, 72))) {
      ownership.clearPlaced(position);
      level.setBlock(position, position.getY() < site.getY() ? Blocks.STONE.defaultBlockState()
          : Blocks.AIR.defaultBlockState(), 2);
    }
    check(level.getBiome(site).is(CATALOG.biome()), "Disposable world biome must match " + STYLE);
    Village village = new Village(STYLE + " integration " + rotation);
    village.attach(level);
    village.setStyle(VillageStyle.fromBiome(level.getBiome(site), level.getSeed(), site));
    check(village.getStyle() == STYLE, "Actual biome selected " + village.getStyle());
    var plan = village.planFounding(site, rotation, true).orElseThrow(() -> new AssertionError("Founding preflight failed " + rotation));
    check(village.getBuildings().isEmpty() && !village.hasClaimed(site), "Preflight published buildings or claims");
    // A pocket under a column the centre actually seats: a patchy earthen course
    // leaves the footprint's corners to the terrain, and nothing fills under those.
    BlockPos inside = seatedColumn(level, plan.center().getBuilding()).atY(plan.planeY() - 2);
    BlockPos outside = site.offset(65, -2, 65);
    // A small erosion pocket is fillable; an equivalent pocket beyond all three envelopes must remain untouched.
    level.setBlock(inside, Blocks.AIR.defaultBlockState(), 2);
    level.setBlock(inside.above(), Blocks.AIR.defaultBlockState(), 2);
    level.setBlock(outside, Blocks.AIR.defaultBlockState(), 2);
    level.setBlock(outside.above(), Blocks.AIR.defaultBlockState(), 2);
    check(village.found(plan), "Final founding rejected a supported shallow foundation " + rotation);
    check(!level.getBlockState(inside).isAir() && !level.getBlockState(inside.above()).isAir(), "Missing founding foundation");
    check(level.getBlockState(outside).isAir() && level.getBlockState(outside.above()).isAir(), "Foundation spread outside footprints");
    verifyVillage(level, village);
    verifyCompanions(level, village);
    verifyReload(level, village);
    Kithkyn.LOGGER.info("{} FOUNDING PASS {}: filled foundation, ordinary growth spacing and durable village state", PREFIX, rotation);
  }

  private static void verifyVillage(ServerLevel level, Village village) {
    check(village.getStyle() == STYLE, "Founded style changed");
    check(village.getBuildings().size() == CATALOG.foundingBuildings() && village.getTotalBeds() == CATALOG.foundingBeds(),
        "Wrong founding building/bed count");
    Building center = village.getTownCenter();
    check(center != null && center.getName().equals(id("village_center", 1)), "Wrong town center");
    check(center.getInfo().getWorkLocations().size() == CATALOG.centerJobs(), "Wrong center starting jobs");
    List<Occupation> jobs = new ArrayList<>();
    village.getUnassignedJobs().forEach(job -> jobs.add(job.getOccupation()));
    village.getJobAssignmentsView().values().forEach(job -> jobs.add(job.getOccupation()));
    Map<Occupation, Long> counts = jobs.stream().collect(java.util.stream.Collectors.groupingBy(
        occupation -> occupation, () -> new EnumMap<>(Occupation.class), java.util.stream.Collectors.counting()));
    check(jobs.size() == CATALOG.foundingJobs() && counts.equals(Map.of(Occupation.GUARD, (long) CATALOG.guards(),
        Occupation.BUILDER, 1L, Occupation.QUARTERMASTER, 1L, Occupation.MINER, 1L)),
        "Wrong starting job positions " + counts);
    check(center.getInfo().getGuardRole(2) == GuardRole.CAPTAIN, "Lost center captain duty");
    if (STYLE == VillageStyle.BADLANDS) {
      check(center.getInfo().getGuardRole(3) == GuardRole.CROSSBOW_POST
          && center.getInfo().getGuardRole(4) == GuardRole.CROSSBOW_POST
          && center.getInfo().getGuardRole(5) == GuardRole.PATROL
          && center.getInfo().getGuardRole(6) == GuardRole.PATROL, "Lost mixed center guard duties");
    }
    check(center.getInfo().getBedContainers() != null && center.getInfo().getBedContainers().size() == CATALOG.centerRooms()
        && center.getInfo().getBedContainers().stream().filter(room -> room.containers().isEmpty()).count()
            == CATALOG.roomsWithoutStorage(),
        "Center room ownership changed");
    BlockPos plaza = village.getCenterPosition();
    check(plaza.equals(world(center, CATALOG.plaza())), "Wrong civic anchor");
    if (CATALOG.bell() != null) {
      check(level.getBlockState(world(center, CATALOG.bell())).is(Blocks.BELL), "Civic bell missing");
    }
    check(village.getCampfirePositions().size() == CATALOG.campfires(), "Lost authored center fires");
    for (BlockPos fire : village.getCampfirePositions()) {
      check(!fire.equals(plaza) && level.getBlockState(fire).is(Blocks.CAMPFIRE), "Missing or conflated campfire " + fire);
    }
    for (Building building : village.getBuildings()) {
      verifyBanners(level, village, building);
      for (long bed : building.getInfo().getBedLocations()) {
        verifyBedIdentity(level, village, building, BlockPos.of(bed));
      }
      for (long container : java.util.stream.Stream.concat(building.getInfo().getContainerLocations().stream(),
          building.getInfo().getPersonalContainerLocations().stream()).toList()) {
        check(level.getBlockEntity(world(building, BlockPos.of(container))) instanceof Container, "Actual founding container missing");
      }
    }
  }

  private static void verifyCompanions(ServerLevel level, Village village) {
    List<BoundingBox> footprints = village.getBuildings().stream()
        .map(building -> ApprovedStructureAccess.footprint(level, building)).toList();
    for (int first = 0; first < footprints.size(); first++) {
      for (int second = first + 1; second < footprints.size(); second++) {
        BoundingBox other = footprints.get(second);
        check(!footprints.get(first).intersects(other.minX() - 1, other.minZ() - 1,
            other.maxX() + 1, other.maxZ() + 1), "Founding footprints lost their walking gap");
      }
    }
    BlockPos site = village.getCenterPosition();
    for (BlockPos position : BlockPos.betweenClosed(site.offset(-72, 0, -72), site.offset(72, 0, 72))) {
      boolean expected = footprints.stream().anyMatch(box -> position.getX() >= box.minX() && position.getX() <= box.maxX()
          && position.getZ() >= box.minZ() && position.getZ() <= box.maxZ());
      check(village.hasClaimed(position) == expected, "Claim escaped the three authored footprints at " + position);
    }
  }

  private static void verifyReload(ServerLevel level, Village village) throws ReflectiveOperationException {
    List<ApprovedStructureAccess.Person> residents = new ArrayList<>();
    for (var job : List.copyOf(village.getUnassignedJobs())) {
      var person = new ApprovedStructureAccess.Person(level, village);
      person.setLifeStage(AgeStage.ADULT);
      person.setNoAi(true);
      ApprovedStructureAccess.moveTo(person, village.getGatheringPoint());
      village.getPopulation().add(person.getUUID());
      check(level.addFreshEntity(person), "Could not create allocation probe");
      check(village.canHouseForJob(person.getUUID(), job.getBuildingUUID()), "Starting job cannot be housed");
      village.assignJob(person.getUUID(), job);
      person.setOccupation(job.getOccupation());
      residents.add(person);
    }
    ApprovedStructureAccess.reconcileBeds(village);
    check(village.getJobAssignmentsView().size() == CATALOG.foundingJobs()
        && village.getBedAssignmentsView().size() == CATALOG.foundingJobs()
        && village.getUnassignedBeds().size() == CATALOG.foundingBeds() - CATALOG.foundingJobs(),
        "Founding workers did not receive distinct beds");
    if (STYLE == VillageStyle.JUNGLE) {
      verifyRoutedWorksite(village, residents, Occupation.MINER, "mine");
      verifyRoutedWorksite(village, residents, Occupation.QUARTERMASTER, "storehouse");
      Building mine = village.getBuildings().stream()
          .filter(building -> building.getInfo().getCategory().equals("mine")).findFirst().orElseThrow();
      check(MineShaft.of(mine).size() == 1, "Jungle physical mine lost its one shaft frame");
    }
    Building store = village.getBuildings().stream().filter(building -> building.getName().equals(id("storehouse", 1)))
        .findFirst().orElseThrow();
    BlockPos storage = world(store, BlockPos.of(store.getInfo().getContainerLocations().getFirst()));
    Container chest = (Container) level.getBlockEntity(storage);
    chest.setItem(0, new ItemStack(Items.COPPER_INGOT, 13));
    chest.setChanged();
    village.queuePendingVillageItems(List.of(new ItemStack(Items.AMETHYST_SHARD, 7)));
    Map<?, ?> stock = village.stockTally();
    var containers = village.getVillageContainerPositions();
    var ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
    CompoundTag saved = (CompoundTag) Village.CODEC.encodeStart(ops, village).getOrThrow();
    Village restored = Village.CODEC.parse(ops, saved).getOrThrow();
    restored.attach(level);
    CompoundTag again = (CompoundTag) Village.CODEC.encodeStart(ops, restored).getOrThrow();
    for (String field : List.of("id", "name", "town_center", "buildings", "people", "job_assignments",
        "bed_assignments", "unassigned_jobs", "unassigned_beds", "brain")) {
      check(Objects.equals(saved.get(field), again.get(field)), "Save/reload changed " + field);
    }
    // Claims are a set; decoding may change its iteration order without changing any owned column.
    check(new java.util.HashSet<>(saved.getList("claim_grid", net.minecraft.nbt.Tag.TAG_LONG))
        .equals(new java.util.HashSet<>(again.getList("claim_grid", net.minecraft.nbt.Tag.TAG_LONG))),
        "Save/reload changed claimed columns");
    verifyVillage(level, restored);
    check(restored.getVillageContainerPositions().equals(containers), "Reload lost container ownership");
    check(restored.stockTally().equals(stock) && chest.getItem(0).is(Items.COPPER_INGOT)
        && chest.getItem(0).getCount() == 13, "Reload changed actual world storage");
    check(restored.pendingVillageItems().size() == 1 && restored.pendingVillageItems().getFirst().is(Items.AMETHYST_SHARD)
        && restored.pendingVillageItems().getFirst().getCount() == 7, "Reload lost pending inventory");
    for (Building before : village.getBuildings()) {
      Building after = restored.getBuilding(before.getUUID());
      check(after != null && after.getRotation() == before.getRotation() && after.getPlacedSink() == before.getPlacedSink()
          && Objects.equals(after.getMineEntrance(), before.getMineEntrance()), "Reload changed authored placement frame");
      check(Objects.equals(after.getInfo().getBedContainers(), before.getInfo().getBedContainers())
          && after.getInfo().getCoupleBeds().equals(before.getInfo().getCoupleBeds()), "Reload changed room metadata");
    }
    residents.forEach(net.minecraft.world.entity.Entity::discard);
  }

  private static void verifyRoutedWorksite(Village village, List<ApprovedStructureAccess.Person> residents,
      Occupation occupation, String category) {
    ApprovedStructureAccess.Person worker = residents.stream()
        .filter(person -> person.getOccupation() == occupation).findFirst().orElseThrow();
    Building owner = village.getBuilding(village.getJobAssignment(worker.getUUID()).getBuildingUUID());
    Building physical = com.quzzar.kithkyn.village.LocationManager.getJobBuilding(worker);
    check(owner != null && owner.equals(village.getTownCenter()), occupation + " vacancy left the Jungle center");
    check(physical != null && physical.getInfo().getCategory().equals(category),
        occupation + " did not route to its physical " + category);
    check(!com.quzzar.kithkyn.village.LocationManager.getJobLocation(worker).equals(BlockPos.ZERO),
        occupation + " physical station did not resolve");
  }

  private static void verifyBedIdentity(ServerLevel level, Village village, Building building, BlockPos local) {
    BlockPos position = world(building, local);
    var state = level.getBlockState(position);
    check(state.getBlock() instanceof BedBlock, "Actual founding bed missing");
    var slots = building.getInfo().getVillageIdentitySlots();
    boolean primary = slots.primaryBlocks().contains(local);
    check(primary ^ slots.secondaryBlocks().contains(local), "Founding bed must declare one village color");
    var expected = primary ? village.getIdentity().primaryColor() : village.getIdentity().secondaryColor();
    check(((BedBlock) state.getBlock()).getColor() == expected, "Founding bed has the wrong village color");
    var foot = level.getBlockState(position.relative(state.getValue(BedBlock.FACING).getOpposite()));
    check(foot.is(state.getBlock()), "Founding bed halves have different village colors");
  }

  private static void verifyBanners(ServerLevel level, Village village, Building building) {
    var identity = village.getIdentity();
    for (BlockPos local : building.getInfo().getVillageIdentitySlots().banners()) {
      BlockPos position = world(building, local);
      var block = level.getBlockState(position).getBlock();
      check(block instanceof AbstractBannerBlock && ((AbstractBannerBlock) block).getColor() == identity.primaryColor(),
          "Founding flag lost the village base color");
      check(level.getBlockEntity(position) instanceof BannerBlockEntity, "Founding flag block entity missing");
      var layers = ((BannerBlockEntity) level.getBlockEntity(position)).getPatterns().layers();
      check(layers.size() == identity.bannerLayers().size(), "Founding flag lost its saved pattern layers");
      for (int i = 0; i < layers.size(); i++) {
        var expected = identity.bannerLayers().get(i);
        check(layers.get(i).pattern().unwrapKey().orElseThrow().location().equals(expected.pattern())
            && layers.get(i).color() == expected.color().resolve(identity), "Founding flag differs from village identity");
      }
    }
  }

  /** The world column of the first block in the centre template's ground layer. */
  private static BlockPos seatedColumn(ServerLevel level, Building building) {
    var template = level.getStructureManager()
        .get(ResourceLocation.fromNamespaceAndPath(Kithkyn.MODID, building.getInfo().getPath()))
        .orElseThrow(() -> new AssertionError("Missing centre template"));
    for (var block : template.palettes.getFirst().blocks()) {
      if (block.pos().getY() == 0 && !block.state().isAir()) {
        return world(building, block.pos());
      }
    }
    throw new AssertionError("Centre template has no ground layer");
  }

  private static String id(String category, int tier) {
    return category + "_" + STYLE.id() + "_" + tier;
  }

  private static BuildingInfo info(String name) {
    BuildingInfo value = Buildings.getByName(name);
    check(value != null, "Missing definition " + name);
    return value;
  }

  private static BlockPos world(Building building, BlockPos local) {
    return BlockPos.of(building.getOriginLocation()).offset(local.rotate(building.getRotation()));
  }

  private static void forceChunks(ServerLevel level, BlockPos center, int radius) {
    for (int x = (center.getX() - radius) >> 4; x <= (center.getX() + radius) >> 4; x++) {
      for (int z = (center.getZ() - radius) >> 4; z <= (center.getZ() + radius) >> 4; z++) level.setChunkForced(x, z, true);
    }
  }

  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
}
