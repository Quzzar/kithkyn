package com.quzzar.kithkyn.dev;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.PersonEntityType;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.entities.GuardWeapons;
import com.quzzar.kithkyn.entities.JobTool;
import com.quzzar.kithkyn.entities.ai.goals.work.BlacksmithStep;
import com.quzzar.kithkyn.entities.ai.goals.work.ChopStep;
import com.quzzar.kithkyn.entities.ai.goals.PersonEatFoodGoal;
import com.quzzar.kithkyn.village.JobAssignment;
import com.quzzar.kithkyn.village.LocationManager;
import com.quzzar.kithkyn.village.Occupation;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.VillageManager;
import com.quzzar.kithkyn.village.buildings.Building;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Opt-in equipment regression. Run only in a disposable test world. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class GuardEquipmentVerification {
  private static boolean finished;

  private GuardEquipmentVerification() { }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (!Boolean.getBoolean("kithkyn.guardEquipment.verify") || finished) return;
    finished = true;
    try {
      verify(event.getServer().overworld());
      Kithkyn.LOGGER.info("[guard-equipment-verify] RESULT PASS: smith reload delivery, mixed-wood shield payment and delivery, patrol stock upgrades, full-pack chop/interrupt switching, reload/reassignment, shield and captain");
    } catch (Exception | AssertionError failure) {
      Kithkyn.LOGGER.error("[guard-equipment-verify] RESULT FAIL", failure);
    }
    event.getServer().halt(false);
  }

  private static void verify(ServerLevel level) throws ReflectiveOperationException {
    FixtureVillage village = new FixtureVillage();
    village.attach(level);
    village.register();
    VillageManager.get(level).getVillages().put(village.getID(), village);
    BlockPos chestPos = new BlockPos(160, 151, 106);
    level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
    Container chest = (Container) level.getBlockEntity(chestPos);
    RealPerson smith = PersonEntityType.PERSON.get().create(level);
    smith.setVillage(village.getID());
    smith.setOccupation(Occupation.BLACKSMITH);
    smith.moveTo(chestPos.getX(), chestPos.getY(), chestPos.getZ(), 0, 0);
    smith.personMainInv.setItem(0, new ItemStack(Items.IRON_SWORD));
    // A new work step is exactly what reload installs, with no production choice yet.
    BlacksmithStep step = new BlacksmithStep();
    BlockPos delivery = step.select(smith);
    check(chestPos.equals(delivery), "Finished gear was not selected for delivery");
    step.act(smith, delivery);
    check(chest.countItem(Items.IRON_SWORD) == 1 && smith.personMainInv.countItem(Items.IRON_SWORD) == 0,
        "Reloaded smith reached storage but did not deposit finished gear");
    verifyShield(level, village, smith, chest, chestPos);
    verifyPatrol(level, village, chest, chestPos);
  }

  private static void verifyShield(ServerLevel level, FixtureVillage village, RealPerson smith,
      Container chest, BlockPos chestPos) {
    village.assignJob(smith.getUUID(), new JobAssignment(smith.getUUID(), Occupation.BLACKSMITH,
        village.forge.getUUID(), 0));
    chest.clearContent();
    var products = java.util.List.of(Items.BUCKET, Items.IRON_PICKAXE, Items.IRON_AXE,
        Items.IRON_SHOVEL, Items.IRON_HOE, Items.IRON_SWORD);
    for (int i = 0; i < products.size(); i++) chest.setItem(i,
        new ItemStack(products.get(i), i == 0 ? 2 : 1));
    chest.setItem(6, new ItemStack(Items.IRON_INGOT));
    chest.setItem(7, new ItemStack(Items.BIRCH_PLANKS, 3));
    chest.setItem(8, new ItemStack(Items.SPRUCE_PLANKS, 3));
    BlacksmithStep step = new BlacksmithStep();
    check(chestPos.equals(step.select(smith)), "Smith did not fetch shield ingredients");
    step.act(smith, chestPos);
    BlockPos forge = LocationManager.getJobLocation(smith);
    check(!forge.equals(BlockPos.ZERO) && forge.equals(step.select(smith)), "Paid smith did not choose forge");
    step.act(smith, forge);
    check(smith.personMainInv.countItem(Items.SHIELD) == 1, "Smith did not forge shield");
    check(smith.personMainInv.countItem(Items.IRON_INGOT) == 0
        && smith.personMainInv.countItem(Items.BIRCH_PLANKS) == 0
        && smith.personMainInv.countItem(Items.SPRUCE_PLANKS) == 0, "Shield did not spend its whole recipe");
    check(chestPos.equals(step.select(smith)), "Shield not routed to storage");
    step.act(smith, chestPos);
    check(chest.countItem(Items.SHIELD) == 1 && smith.personMainInv.countItem(Items.SHIELD) == 0,
        "Shield was not delivered");
    check(step.select(smith) == null, "Smith ignored stock targets or unavailable ingredients");
    Kithkyn.LOGGER.info("[guard-equipment-verify] PASS smith delivery and mixed-wood shield production");
  }

  private static void verifyPatrol(ServerLevel level, FixtureVillage village, Container chest, BlockPos near)
      throws ReflectiveOperationException {
    chest.clearContent();
    RealPerson guard = PersonEntityType.PERSON.get().create(level);
    guard.setVillage(village.getID());
    guard.setOccupation(Occupation.GUARD);
    int station = new java.util.ArrayList<>(village.center.getInfo().getWorkLocations().values())
        .indexOf(Occupation.GUARD);
    check(station >= 0, "Center has no guard station");
    village.assignJob(guard.getUUID(), new JobAssignment(guard.getUUID(), Occupation.GUARD,
        village.center.getUUID(), station));
    guard.reloadState();
    check(guard.getRoleLabel().equals("Guard Captain"), "Center guard not titled captain");
    guard.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_AXE));
    guard.tendJobTool();
    check(JobTool.AXE.inHand(guard) && !GuardWeapons.carries(guard, JobTool.SWORD), "Missing sword was invented");
    chest.setItem(0, new ItemStack(Items.IRON_SWORD));
    chest.setItem(1, new ItemStack(Items.IRON_AXE));
    chest.setItem(2, new ItemStack(Items.SHIELD));
    chest.setItem(3, new ItemStack(Items.IRON_HELMET));
    guard.tendJobTool();
    check(guard.getMainHandItem().is(Items.IRON_SWORD), "Patrol did not take stocked sword");
    GuardWeapons.restock(guard, near);
    check(guard.getMainHandItem().is(Items.IRON_SWORD) && chest.countItem(Items.IRON_AXE) == 0
        && guard.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "Night restock did not upgrade axe/armor");
    for (int i = 0; i < guard.personMainInv.getContainerSize(); i++) {
      if (guard.personMainInv.getItem(i).isEmpty()) guard.personMainInv.setItem(i, new ItemStack(Items.DIAMOND, 64));
    }
    int diamonds = guard.personMainInv.countItem(Items.DIAMOND);
    ItemStack sword = guard.getMainHandItem();
    ChopStep chop = new ChopStep(12, 1, 1);
    ChopStep.Cut cut = new ChopStep.Cut(near.above(4), near.above());
    chop.acquired(guard, cut);
    check(guard.getMainHandItem().is(Items.IRON_AXE), "Chop failed to draw the real axe from a full pack");
    ItemStack axe = guard.getMainHandItem();
    GuardWeapons.tick(guard);
    check(guard.getMainHandItem() == axe, "Idle combat selector undid chopping intent");
    var threat = EntityType.ZOMBIE.create(level);
    guard.setTarget(threat);
    GuardWeapons.tick(guard);
    check(guard.getMainHandItem() == sword && guard.getTarget() == threat, "Combat failed to interrupt axe use");
    chop.released(guard, cut);
    guard.setTarget(null);
    GuardWeapons.tick(guard);
    check(guard.getMainHandItem() == sword && guard.personMainInv.countItem(Items.DIAMOND) == diamonds,
        "Release lost goods or failed to restore sword");
    guard.tendJobTool();
    check(guard.getMainHandItem() == sword && GuardWeapons.carries(guard, JobTool.AXE),
        "Routine tool maintenance deposited the sword or axe");
    CompoundTag saved = new CompoundTag();
    guard.addAdditionalSaveData(saved);
    RealPerson loaded = PersonEntityType.PERSON.get().create(level);
    loaded.setUUID(guard.getUUID());
    loaded.readAdditionalSaveData(saved);
    check(loaded.getRoleLabel().equals("Guard Captain") && GuardWeapons.carries(loaded, JobTool.AXE)
        && loaded.getMainHandItem().is(Items.IRON_SWORD), "Reload lost rank or physical weapons");
    var shieldRestock = RealPerson.class.getDeclaredMethod("maybeEquipOrForgeShield", BlockPos.class);
    shieldRestock.setAccessible(true);
    shieldRestock.invoke(loaded, near);
    check(loaded.getOffhandItem().is(Items.SHIELD) && chest.countItem(Items.SHIELD) == 0,
        "Guard did not equip the stocked shield");
    ItemStack shield = loaded.getOffhandItem();
    ItemStack beforeMeal = loaded.getMainHandItem();
    loaded.personMainInv.setItem(1, new ItemStack(Items.BREAD, 3));
    PersonEatFoodGoal meal = new PersonEatFoodGoal(loaded);
    meal.start();
    GuardWeapons.tick(loaded);
    check(loaded.getOffhandItem().is(Items.BREAD) && loaded.getMainHandItem() == beforeMeal,
        "Weapon selection interfered with the offhand meal");
    meal.stop();
    check(loaded.getOffhandItem() == shield && loaded.getMainHandItem() == beforeMeal,
        "Meal did not restore the shield and preserve the sword");
    loaded.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
    GuardWeapons.tick(loaded);
    check(loaded.getMainHandItem().is(Items.IRON_AXE), "Lost sword did not fall back to carried axe");
    loaded.setOccupation(Occupation.MINER);
    loaded.reloadState();
    check(!loaded.getRoleLabel().equals("Guard Captain") && !GuardWeapons.usesLoadout(loaded),
        "Captain rank or loadout survived a non-guard reassignment");
    verifySentry(level, village, loaded);
  }

  private static void verifySentry(ServerLevel level, FixtureVillage village, RealPerson sentry) {
    Building tower = new Building(new BlockPos(180, 150, 100), "watchtower_birch_forest_1", Rotation.NONE);
    village.register(tower);
    int station = new java.util.ArrayList<>(tower.getInfo().getWorkLocations().values()).indexOf(Occupation.GUARD);
    sentry.setOccupation(Occupation.GUARD);
    village.assignJob(sentry.getUUID(), new JobAssignment(sentry.getUUID(), Occupation.GUARD, tower.getUUID(), station));
    sentry.reloadState();
    check(!sentry.getRoleLabel().equals("Guard Captain") && GuardWeapons.hasSidearmDuty(sentry),
        "Tower inherited the captain title or lost its sentry duty");
    sentry.personMainInv.clearContent();
    sentry.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
    GuardWeapons.issueSidearm(sentry);
    sentry.moveTo(180, 151, 100, 0, 0);
    var threat = EntityType.ZOMBIE.create(level);
    threat.moveTo(182, 151, 100, 0, 0);
    sentry.setTarget(threat);
    GuardWeapons.tick(sentry);
    check(JobTool.SWORD.inHand(sentry), "Close sentry threat did not draw sidearm");
    threat.moveTo(186, 151, 100, 0, 0);
    GuardWeapons.tick(sentry);
    check(JobTool.CROSSBOW.inHand(sentry), "Distant sentry threat did not restore crossbow");
  }

  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  private static final class FixtureVillage extends Village {
    private final Building center = new Building(new BlockPos(100, 150, 100),
        "village_center_birch_forest_1", Rotation.NONE);
    private final Building forge = new Building(new BlockPos(150, 150, 100),
        "blacksmith_birch_forest_1", Rotation.NONE);
    private FixtureVillage() { super("Equipment Regression"); }
    private void register() { addBuilding(center); addBuilding(forge); }
    private void register(Building building) { addBuilding(building); }
    @Override public Building getTownCenter() { return center; }
    @Override public void update(ServerLevel level) { }
  }
}
