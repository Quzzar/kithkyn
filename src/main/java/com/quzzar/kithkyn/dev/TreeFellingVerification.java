package com.quzzar.kithkyn.dev;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.savedata.PlacedBlockStore;
import com.quzzar.kithkyn.village.TreeFelling;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Native block-entity, ownership and loot regression; opt in only in a disposable world. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class TreeFellingVerification {
  private static final int HIVE_CASES = 10;
  private static final int CANOPY_CASES = 3;
  private static boolean ran;
  private static int tick;

  private TreeFellingVerification() { }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (!Boolean.getBoolean("kithkyn.treeFelling.verify") || ran) return;
    if (++tick == 1) {
      for (int mode = 0; mode < HIVE_CASES + CANOPY_CASES; mode++) {
        int x = (-3000 + mode * 24) >> 4;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
          event.getServer().overworld().setChunkForced(x + dx, (-3000 >> 4) + dz, true);
        }
      }
    }
    if (tick < 40) return;
    ran = true;
    try {
      ServerLevel level = event.getServer().overworld();
      level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, event.getServer());
      for (int mode = 0; mode < HIVE_CASES; mode++) verifyHive(level, mode);
      verifyOwnedLogCannotHoldCanopy(level, HIVE_CASES);
      verifyNaturalTreeKeepsSharedCanopy(level, HIVE_CASES + 1);
      verifyProtectedLeavesRemain(level, HIVE_CASES + 2);
      Kithkyn.LOGGER.info("[tree-felling-verify] RESULT PASS: {} native tree/hive/canopy cases",
          HIVE_CASES + CANOPY_CASES);
    } catch (Exception | AssertionError failure) {
      Kithkyn.LOGGER.error("[tree-felling-verify] RESULT FAIL", failure);
    }
    event.getServer().halt(false);
  }

  private static void verifyHive(ServerLevel level, int mode) {
    BlockPos log = new BlockPos(-3000 + mode * 24, 160, -3000);
    BlockPos hive = log.east();
    level.getEntitiesOfClass(Bee.class, new AABB(log).inflate(8)).forEach(Bee::discard);
    PlacedBlockStore owned = PlacedBlockStore.get(level);
    for (BlockPos pos : BlockPos.betweenClosed(log.offset(-3, -1, -3), log.offset(5, 4, 3))) {
      owned.clearPlaced(pos);
      level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
    }
    level.setBlock(log, Blocks.BIRCH_LOG.defaultBlockState(), 2);
    level.setBlock(log.above(), Blocks.BIRCH_LOG.defaultBlockState(), 2);
    level.setBlock(log.above(2), Blocks.BIRCH_LEAVES.defaultBlockState(), 2);
    if (mode == 9) level.setBlock(log.above().east(), Blocks.BIRCH_LOG.defaultBlockState(), 2);
    boolean crafted = mode == 1;
    level.setBlock(hive, (crafted ? Blocks.BEEHIVE : Blocks.BEE_NEST).defaultBlockState(), 2);
    BeehiveBlockEntity bees = (BeehiveBlockEntity) level.getBlockEntity(hive);
    bees.addOccupant(EntityType.BEE.create(level));
    if (mode == 2 || mode == 7) owned.markPlayerPlaced(hive);
    if (mode == 3) owned.markVillagePlaced(hive);
    if (mode == 4) {
      owned.markPlayerPlaced(log);
      owned.markPlayerPlaced(log.above());
    }
    if (mode == 7) owned.markVillagePlaced(log);
    BlockPos chest = log.west();
    level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 2);
    BlockPos disconnected = log.east(4);
    level.setBlock(disconnected, Blocks.BEE_NEST.defaultBlockState(), 2);
    ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
    if (mode == 5) axe.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
        .getOrThrow(Enchantments.SILK_TOUCH), 1);
    List<ItemStack> drops;
    if (mode == 6) {
      drops = TreeFelling.fellWithin(level, Set.of(BlockPos.asLong(log.getX(), 0, log.getZ())))
          .stream().flatMap(tree -> tree.drops().stream()).toList();
    } else if (mode == 7 || mode == 8) {
      drops = TreeFelling.fellStand(level, log, null, axe);
    } else {
      drops = TreeFelling.fell(level, log, null, axe);
    }
    boolean protectedHive = mode == 2 || mode == 3 || mode == 4 || mode == 7;
    check(level.getBlockState(hive).isAir() != protectedHive,
        "mode=" + mode + ": attached bee nest/hive was not removed correctly");
    check(level.getBlockState(chest).is(Blocks.CHEST), "Unrelated block entity removed");
    check(level.getBlockState(disconnected).is(Blocks.BEE_NEST), "Disconnected nest removed");
    check(level.getBlockState(log).isAir() == (mode != 4), "Log ownership changed");
    int released = level.getEntitiesOfClass(Bee.class, new AABB(log).inflate(7)).size();
    check(released == (protectedHive || mode == 5 ? 0 : 1), "Occupant lost or duplicated: " + released);
    int savedBees = drops.stream().mapToInt(drop -> drop.getOrDefault(DataComponents.BEES, List.of()).size()).sum();
    check(savedBees == (mode == 5 ? 1 : 0), "Silk Touch occupant preservation failed");
    if (crafted) check(drops.stream().filter(drop -> drop.is(Items.BEEHIVE))
        .mapToInt(ItemStack::getCount).sum() == 1, "Hive loot missing or duplicated");
    Kithkyn.LOGGER.info("[tree-felling-verify] PASS mode={} released={} saved={}", mode, released, savedBees);
  }

  /** A village beam must not preserve the natural canopy of the tree beside it. */
  private static void verifyOwnedLogCannotHoldCanopy(ServerLevel level, int mode) {
    CanopyFixture fixture = canopyFixture(level, mode);
    BlockPos buildingLog = fixture.leaves().getLast().east();
    level.setBlock(buildingLog, Blocks.OAK_LOG.defaultBlockState(), 2);
    PlacedBlockStore.get(level).markVillagePlaced(buildingLog);

    TreeFelling.fell(level, fixture.trunk(), null, ItemStack.EMPTY);

    check(level.getBlockState(buildingLog).is(Blocks.OAK_LOG), "Village beam was felled");
    for (BlockPos leaf : fixture.leaves()) {
      check(level.getBlockState(leaf).isAir(), "Village beam kept orphaned leaf at " + leaf);
    }
    Kithkyn.LOGGER.info("[tree-felling-verify] PASS owned log cannot hold felled canopy");
  }

  /** Leaves shared with another living natural tree still belong in the world. */
  private static void verifyNaturalTreeKeepsSharedCanopy(ServerLevel level, int mode) {
    CanopyFixture fixture = canopyFixture(level, mode);
    BlockPos livingLog = fixture.leaves().getLast().east();
    level.setBlock(livingLog, Blocks.OAK_LOG.defaultBlockState(), 2);

    TreeFelling.fell(level, fixture.trunk(), null, ItemStack.EMPTY);

    check(level.getBlockState(livingLog).is(Blocks.OAK_LOG), "Neighboring natural tree was felled");
    for (BlockPos leaf : fixture.leaves()) {
      check(level.getBlockState(leaf).is(Blocks.OAK_LEAVES),
          "Shared living canopy was removed at " + leaf);
    }
    Kithkyn.LOGGER.info("[tree-felling-verify] PASS neighboring tree keeps shared canopy");
  }

  /** Persistent leaves and explicitly owned natural-state leaves are never canopy cleanup. */
  private static void verifyProtectedLeavesRemain(ServerLevel level, int mode) {
    CanopyFixture fixture = canopyFixture(level, mode);
    BlockPos persistent = fixture.trunk().above(2);
    BlockPos owned = fixture.trunk().above().west();
    BlockState persistentLeaf = Blocks.OAK_LEAVES.defaultBlockState()
        .setValue(LeavesBlock.PERSISTENT, true);
    level.setBlock(persistent, persistentLeaf, 2);
    level.setBlock(owned, Blocks.OAK_LEAVES.defaultBlockState(), 2);
    PlacedBlockStore.get(level).markPlayerPlaced(owned);

    TreeFelling.fell(level, fixture.trunk(), null, ItemStack.EMPTY);

    check(level.getBlockState(persistent).is(Blocks.OAK_LEAVES), "Persistent leaf was removed");
    check(level.getBlockState(owned).is(Blocks.OAK_LEAVES), "Player-owned leaf was removed");
    Kithkyn.LOGGER.info("[tree-felling-verify] PASS protected leaves remain");
  }

  /** A minimal canopy spanning from a two-log trunk toward a possible support log. */
  private static CanopyFixture canopyFixture(ServerLevel level, int mode) {
    BlockPos trunk = new BlockPos(-3000 + mode * 24, 160, -3000);
    PlacedBlockStore placed = PlacedBlockStore.get(level);
    for (BlockPos pos : BlockPos.betweenClosed(trunk.offset(-3, -1, -3), trunk.offset(7, 4, 3))) {
      placed.clearPlaced(pos);
      level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
    }
    level.setBlock(trunk, Blocks.OAK_LOG.defaultBlockState(), 2);
    level.setBlock(trunk.above(), Blocks.OAK_LOG.defaultBlockState(), 2);
    List<BlockPos> leaves = List.of(
        trunk.above(2), trunk.above(2).east(), trunk.above(2).east(2), trunk.above(2).east(3));
    for (BlockPos leaf : leaves) {
      level.setBlock(leaf, Blocks.OAK_LEAVES.defaultBlockState(), 2);
    }
    return new CanopyFixture(trunk, leaves);
  }

  private record CanopyFixture(BlockPos trunk, List<BlockPos> leaves) {
  }

  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
}
