package com.quzzar.kithkyn.dev;

import com.mojang.authlib.GameProfile;
import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.relationships.OpinionService;
import com.quzzar.kithkyn.village.Occupation;
import com.quzzar.kithkyn.village.VillageManager;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.buildings.Building;
import com.quzzar.kithkyn.village.buildings.Buildings;
import com.quzzar.kithkyn.wrongdoing.VillageCustody;
import com.quzzar.kithkyn.wrongdoing.EvidenceInventory;
import com.quzzar.kithkyn.wrongdoing.TheftEvents;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.Difficulty;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;

/** Real damage and persisted custody against the authored castle, only on an opted-in disposable server. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class CustodyVerification {
  private static final String PREFIX = "[custody-verify]";
  private static int ticks;
  private static boolean finished;

  private CustodyVerification() { }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (!Boolean.getBoolean("kithkyn.custody.verify") || finished || ++ticks < 40) return;
    try {
      verify(event.getServer().overworld());
      Kithkyn.LOGGER.info("{} RESULT PASS: real melee/arrow arrest, private minute notices, saved expiry, "
          + "inventory capacity and identity, escape and damaged-cell release, full-cell fallback and village-only grace", PREFIX);
    } catch (Exception | AssertionError failure) {
      Kithkyn.LOGGER.error(PREFIX + " RESULT FAIL", failure);
    }
    finished = true;
    event.getServer().halt(false);
  }

  private static void verify(ServerLevel level) throws ReflectiveOperationException {
    level.getServer().setDifficulty(Difficulty.NORMAL, true);
    level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
    level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(false, level.getServer());
    BlockPos origin = new BlockPos(2400, 159, 2400);
    for (int x = 147; x <= 153; x++) for (int z = 147; z <= 153; z++) level.setChunkForced(x, z, true);
    Building castle = ApprovedStructureAccess.place(level, origin, Buildings.getByName("castle_desert_1"), Rotation.NONE);
    var village = new TestVillage(level, castle);
    VillageManager.get(level).getVillages().put(village.getID(), village);
    var guard = new ApprovedStructureAccess.Person(level, village);
    guard.setOccupation(Occupation.GUARD);
    guard.setNoAi(true);
    guard.moveTo(origin.getX(), origin.getY() + 1, origin.getZ());
    level.addFreshEntity(guard);
    village.getPopulation().add(guard.getUUID());
    BlockPos cell = origin.offset(castle.getInfo().getCastleLayout().custodyCell());
    BlockPos release = origin.offset(castle.getInfo().getCastleLayout().releasePoint());
    check(VillageCustody.validCell(level, cell), "Authored cell is not enclosed and safe: " + cell);
    List<Container> evidence = castle.getInfo().getCastleLayout().evidenceContainers().stream()
        .map(local -> (Container) level.getBlockEntity(origin.offset(local))).toList();
    check(evidence.size() == 2 && evidence.stream().allMatch(java.util.Objects::nonNull), "Two authored evidence barrels missing");
    verifyEvidenceTheft(level, village, evidence, release);
    verifyInventory(level, evidence, release, level.damageSources().mobAttack(guard));

    TestPlayer absorbed = player(level, "Absorbed", release);
    hostile(guard, absorbed);
    absorbed.setHealth(4);
    absorbed.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 0));
    absorbed.setAbsorptionAmount(4);
    absorbed.hurt(level.damageSources().mobAttack(guard), 6);
    check(VillageCustody.get(level).getSentence(absorbed.getUUID()) == null && absorbed.getHealth() == 2,
        "Absorption case: health=" + absorbed.getHealth() + ", absorption=" + absorbed.getAbsorptionAmount()
            + ", sentence=" + VillageCustody.get(level).getSentence(absorbed.getUUID()));
    for (Container container : evidence) for (int slot = 0; slot < container.getContainerSize(); slot++) container.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
    ItemStack named = new ItemStack(Items.DIAMOND, 60);
    named.set(DataComponents.CUSTOM_NAME, Component.literal("Custody capacity specimen"));
    evidence.getFirst().setItem(0, named.copy());
    TestPlayer prisoner = player(level, "CustodyPrisoner", release);
    hostile(guard, prisoner);
    prisoner.getInventory().setItem(0, named.copyWithCount(10));
    prisoner.getInventory().setItem(40, new ItemStack(Items.GOLDEN_APPLE, 3));
    prisoner.setHealth(4);
    prisoner.hurt(level.damageSources().mobAttack(guard), 6);
    check(prisoner.isAlive() && prisoner.getHealth() > 0 && prisoner.blockPosition().equals(cell), "Real guard melee did not arrest");
    check(evidence.getFirst().getItem(0).getCount() == 64 && prisoner.getInventory().getItem(0).getCount() == 6,
        "Evidence transfer lost or duplicated a partial stack");
    check(ItemStack.isSameItemSameComponents(evidence.getFirst().getItem(0), prisoner.getInventory().getItem(0)), "Evidence transfer changed item components");
    check(prisoner.getInventory().getItem(40).getCount() == 3, "Full barrels removed offhand overflow");
    check(prisoner.getEffect(MobEffects.DIG_SLOWDOWN) != null && prisoner.getEffect(MobEffects.DIG_SLOWDOWN).getAmplifier() == 2,
        "Missing Mining Fatigue III");
    guard.setTarget(prisoner);
    check(guard.getTarget() == null, "A guard targets its prisoner");
    prisoner.invulnerableTime = 0;
    float heldHealth = prisoner.getHealth();
    prisoner.hurt(level.damageSources().mobAttack(guard), 20);
    check(prisoner.getHealth() == heldHealth, "Guard damage bypasses custody protection");
    prisoner.invulnerableTime = 0;
    prisoner.hurt(level.damageSources().generic(), 1);
    check(prisoner.getHealth() < heldHealth, "Custody incorrectly blocks unrelated damage");

    TestPlayer fullCell = player(level, "FullCell", release);
    hostile(guard, fullCell);
    check(!VillageCustody.apprehend(village, fullCell, level.damageSources().mobAttack(guard)), "A second prisoner entered a full cell");
    var sentence = VillageCustody.get(level).getSentence(prisoner.getUUID());
    for (int minute = 1; minute <= 4; minute++) {
      time(level, sentence.startedAt() + minute * 1200L);
      VillageCustody.tickPlayer(prisoner);
    }
    for (int remaining = 1; remaining <= 4; remaining++) {
      String expected = "You have " + remaining + (remaining == 1 ? " minute" : " minutes") + " remaining in custody.";
      check(prisoner.messages.stream().filter(expected::equals).count() == 1, "Private remaining-minute notice missing or duplicated: " + expected);
    }
    check(fullCell.messages.isEmpty(), "Custody notices leaked to another player");
    CompoundTag saved = VillageCustody.get(level).save(new CompoundTag(), level.registryAccess());
    level.getDataStorage().set(Kithkyn.MODID + "~custody", VillageCustody.load(saved, level.registryAccess()));
    time(level, sentence.releaseAt());
    VillageCustody.tickPlayer(prisoner);
    check(prisoner.blockPosition().equals(release), "Expired saved sentence did not release beside the cell");
    check(VillageCustody.protects(village, prisoner), "Release lost its grace period");
    check(prisoner.getInventory().getItem(0).getCount() == 6 && evidence.getFirst().getItem(0).getCount() == 64,
        "Release automatically returned or duplicated evidence");
    time(level, sentence.releaseAt() + VillageCustody.GRACE_TICKS);
    check(!VillageCustody.protects(village, prisoner), "Grace never expires");
    Kithkyn.LOGGER.info("{} MELEE/INVENTORY/TIMER PASS: partial evidence transfer, saved expiry, four private notices", PREFIX);

    // Same damage boundary, but the causing entity is the arrow's owner.
    TestPlayer archerTarget = player(level, "ArrowPrisoner", release);
    hostile(guard, archerTarget);
    archerTarget.setHealth(4);
    Arrow arrow = new Arrow(EntityType.ARROW, level);
    arrow.setOwner(guard);
    archerTarget.hurt(level.damageSources().arrow(arrow, guard), 6);
    check(archerTarget.blockPosition().equals(cell) && archerTarget.isAlive(), "Attributed arrow did not arrest");
    archerTarget.moveTo(release.getX() + 0.5D, release.getY(), release.getZ() + 0.5D);
    VillageCustody.tickPlayer(archerTarget);
    check(!VillageCustody.get(level).getSentence(archerTarget.getUUID()).jailed() && VillageCustody.protects(village, archerTarget),
        "Escaping did not end custody with grace");

    TestPlayer brokenCell = player(level, "BrokenCell", release);
    hostile(guard, brokenCell);
    check(VillageCustody.apprehend(village, brokenCell, level.damageSources().mobAttack(guard)), "Reusable freed cell rejected arrest");
    level.setBlock(cell.below(), Blocks.AIR.defaultBlockState(), 2);
    VillageCustody.tickPlayer(brokenCell);
    check(brokenCell.blockPosition().equals(release) && !VillageCustody.get(level).getSentence(brokenCell.getUUID()).jailed(),
        "Destroyed cell did not release its prisoner");
    check(!VillageCustody.apprehend(village, fullCell, level.damageSources().mobAttack(guard)), "Destroyed cell accepts another prisoner");
    Kithkyn.LOGGER.info("{} PROJECTILE/ESCAPE/CELL PASS: arrows share capture, escape releases, destroyed cells refuse and release", PREFIX);

    level.setBlock(cell.below(), Blocks.STONE.defaultBlockState(), 2);
    TestPlayer obstructedExit = player(level, "ObstructedExit", release);
    hostile(guard, obstructedExit);
    check(VillageCustody.apprehend(village, obstructedExit, level.damageSources().mobAttack(guard)), "Intact cell rejected fallback fixture");
    var blockedSentence = VillageCustody.get(level).getSentence(obstructedExit.getUUID());
    for (BlockPos pos : BlockPos.betweenClosed(release.offset(-7, -5, -7), release.offset(7, 5, 7))) {
      if (!pos.equals(cell) && !pos.equals(cell.above())) level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
    }
    time(level, blockedSentence.releaseAt());
    VillageCustody.tickPlayer(obstructedExit);
    check(!obstructedExit.blockPosition().equals(cell)
        && com.quzzar.kithkyn.village.buildings.WorkerFooting.canStand(level, obstructedExit.blockPosition()),
        "Blocked release point trapped the prisoner or teleported them into solid blocks");
    Kithkyn.LOGGER.info("{} BLOCKED RELEASE PASS: obstructed cell exit falls back to safe village/spawn ground", PREFIX);
  }

  private static void hostile(ApprovedStructureAccess.Person guard, TestPlayer player) {
    for (int i = 0; i < 8; i++) OpinionService.apply(guard, player.getUUID(), -15, "Custody verification offence");
  }

  private static void verifyInventory(ServerLevel level, List<Container> evidence, BlockPos release, DamageSource source)
      throws ReflectiveOperationException {
    for (Container container : evidence) container.clearContent();
    TestPlayer inventoryPlayer = player(level, "InventoryPlayer", release);
    List<ItemStack> original = new ArrayList<>();
    var armor = List.of(Items.IRON_BOOTS, Items.IRON_LEGGINGS, Items.IRON_CHESTPLATE, Items.IRON_HELMET);
    int total = 0;
    for (int slot = 0; slot < inventoryPlayer.getInventory().getContainerSize(); slot++) {
      ItemStack stack = new ItemStack(slot >= 36 && slot < 40 ? armor.get(slot - 36) : slot == 40 ? Items.ARROW : Items.DIAMOND,
          slot >= 36 && slot < 40 ? 1 : slot + 1);
      CompoundTag identity = new CompoundTag();
      identity.putInt("original_inventory_slot", slot);
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(identity));
      inventoryPlayer.getInventory().setItem(slot, stack);
      original.add(stack.copy());
      total += stack.getCount();
    }
    level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(true, level.getServer());
    check(EvidenceInventory.confiscate(inventoryPlayer, source, evidence) == 0, "keepInventory confiscated equipment");
    for (int slot = 0; slot < original.size(); slot++) check(ItemStack.matches(original.get(slot), inventoryPlayer.getInventory().getItem(slot)),
        "keepInventory changed a source slot");
    level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(false, level.getServer());
    check(EvidenceInventory.confiscate(inventoryPlayer, source, evidence) == total && inventoryPlayer.getInventory().isEmpty(),
        "Not all 41 carried, armor and offhand slots transferred");
    List<ItemStack> recovered = new ArrayList<>();
    for (Container container : evidence) {
      for (int slot = 0; slot < container.getContainerSize(); slot++) if (!container.getItem(slot).isEmpty()) recovered.add(container.getItem(slot));
    }
    check(recovered.size() == 41 && original.stream().allMatch(before -> recovered.stream().filter(after -> ItemStack.matches(before, after)).count() == 1),
        "Evidence dropped, duplicated or changed custom components");
    for (Container container : evidence) container.clearContent();
    ItemStack vanishing = new ItemStack(Items.DIAMOND_HELMET);
    vanishing.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.VANISHING_CURSE), 1);
    inventoryPlayer.getInventory().setItem(39, vanishing);
    inventoryPlayer.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 2));
    check(EvidenceInventory.confiscate(inventoryPlayer, source, evidence) == 2
        && ItemStack.matches(vanishing, inventoryPlayer.getInventory().getItem(39)), "A non-dropping vanishing item was confiscated");
    for (Container container : evidence) container.clearContent();
    Kithkyn.LOGGER.info("{} ALL INVENTORY PASS: 41 slots conserved with custom data; keepInventory and vanishing retained", PREFIX);
  }

  private static void verifyEvidenceTheft(ServerLevel level, TestVillage village, List<Container> evidence, BlockPos release)
      throws ReflectiveOperationException {
    for (BlockPos local : village.castle.getInfo().getCastleLayout().evidenceContainers()) {
      BlockPos at = BlockPos.of(village.castle.getOriginLocation()).offset(local.rotate(village.castle.getRotation()));
      check(!village.getVillageContainerPositions().contains(at), "Evidence was registered as village stores");
      check(at.distSqr(release) < 36, "Nearby-container regression fixture is not nearby");
    }
    check(level.getBlockState(release).isAir(), "The authored release tile must be clear before the temporary storage fixture");
    level.setBlock(release, Blocks.CHEST.defaultBlockState(), 2);
    village.extraShared = release;
    Container shared = (Container) level.getBlockEntity(release);
    TestPlayer visitor = player(level, "EvidenceVisitor", release.above());
    var resolve = TheftEvents.class.getDeclaredMethod("openedVillageContainer", ServerPlayer.class, PlayerContainerEvent.class);
    resolve.setAccessible(true);
    for (Container barrel : evidence) {
      var menu = ChestMenu.threeRows(1, visitor.getInventory(), barrel);
      check(resolve.invoke(null, visitor, new PlayerContainerEvent.Open(visitor, menu)) == null,
          "Opening evidence was misidentified as the neighboring shared chest");
    }
    var sharedMenu = ChestMenu.threeRows(2, visitor.getInventory(), shared);
    check(resolve.invoke(null, visitor, new PlayerContainerEvent.Open(visitor, sharedMenu)) == shared,
        "Real shared storage no longer triggers the theft boundary");
    village.extraShared = null;
    level.setBlock(release, Blocks.AIR.defaultBlockState(), 2);
    Kithkyn.LOGGER.info("{} EVIDENCE THEFT PASS: actual menu identity excludes both barrels beside registered shared storage", PREFIX);
  }

  private static TestPlayer player(ServerLevel level, String name, BlockPos position) throws ReflectiveOperationException {
    TestPlayer player = new TestPlayer(level, name);
    player.setGameMode(GameType.SURVIVAL);
    player.moveTo(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D);
    var invulnerability = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
    invulnerability.setAccessible(true);
    invulnerability.setInt(player, 0);
    return player;
  }

  private static void time(ServerLevel level, long gameTime) { level.getServer().getWorldData().overworldData().setGameTime(gameTime); }

  private static final class TestPlayer extends ServerPlayer {
    private final List<String> messages = new ArrayList<>();
    TestPlayer(ServerLevel level, String name) {
      super(level.getServer(), level, new GameProfile(UUID.randomUUID(), name), ClientInformation.createDefault());
      connection = new ServerGamePacketListenerImpl(level.getServer(), new Connection(PacketFlow.SERVERBOUND), this,
          CommonListenerCookie.createInitial(getGameProfile(), false)) {
        @Override public void send(Packet<?> packet) { }
        @Override public void send(Packet<?> packet, PacketSendListener listener) { }
      };
    }
    @Override public void sendSystemMessage(Component message) { messages.add(message.getString()); }
  }

  /** A temporary registered shared chest isolates the actual-menu theft check from gallery geometry. */
  private static final class TestVillage extends Village {
    private final Building castle;
    private BlockPos extraShared;
    TestVillage(ServerLevel level, Building castle) {
      super("Custody verification village");
      this.castle = castle;
      attach(level);
      addBuilding(castle);
    }
    @Override public Building getTownCenter() { return castle; }
    @Override public void update(ServerLevel level) { }
    @Override public java.util.Set<BlockPos> getVillageContainerPositions() {
      var positions = new java.util.HashSet<>(super.getVillageContainerPositions());
      if (extraShared != null) positions.add(extraShared);
      return positions;
    }
  }

  private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
