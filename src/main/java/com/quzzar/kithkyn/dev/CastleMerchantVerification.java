package com.quzzar.kithkyn.dev;

import com.mojang.authlib.GameProfile;
import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.economy.Treasury;
import com.quzzar.kithkyn.menu.MarketMenu;
import com.quzzar.kithkyn.village.Occupation;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.buildings.Buildings;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Exercises actual castle merchant container transactions against the village's physical market treasury. */
@EventBusSubscriber(modid = Kithkyn.MODID)
public final class CastleMerchantVerification {
  private static final String PREFIX = "[castle-merchant-verify]";
  private static int ticks;
  private static boolean finished;
  private CastleMerchantVerification() { }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (!Boolean.getBoolean("kithkyn.castleMerchant.verify") || finished || ++ticks < 40) return;
    try {
      verify(event.getServer().overworld());
      Kithkyn.LOGGER.info("{} RESULT PASS: castle merchant menu buys and sells, exact goods/payment conservation, returned staged payment, shared treasury and excluded evidence", PREFIX);
    } catch (Exception | AssertionError failure) {
      Kithkyn.LOGGER.error(PREFIX + " RESULT FAIL", failure);
    }
    finished = true;
    event.getServer().halt(false);
  }

  private static void verify(ServerLevel level) {
    BlockPos origin = new BlockPos(2400, 159, 2400);
    var castle = ApprovedStructureAccess.place(level, origin, Buildings.getByName("castle_desert_1"), Rotation.NONE);
    var market = ApprovedStructureAccess.place(level, origin.offset(96,0,0), Buildings.getByName("market_desert_1"), Rotation.NONE);
    Village village = new ApprovedStructureAccess.VillageFixture(level, castle, true, market);
    var merchant = new ApprovedStructureAccess.Person(level, village);
    village.getPopulation().add(merchant.getUUID());
    var castleJob = village.getUnassignedJobs().stream().filter(j -> j.getOccupation() == Occupation.MERCHANT
        && j.getBuildingUUID().equals(castle.getUUID())).findFirst().orElseThrow();
    village.assignJob(merchant.getUUID(), castleJob);
    merchant.setOccupation(Occupation.MERCHANT);
    merchant.setNoAi(true);
    merchant.moveTo(2424.5,170,2411.5);
    check(level.addFreshEntity(merchant), "Merchant failed to spawn");
    check(Treasury.tradeBlocker(village, level).isPresent(), "Castle stall incorrectly substitutes for market staffing");
    var marketWorker = new ApprovedStructureAccess.Person(level, village);
    village.getPopulation().add(marketWorker.getUUID());
    var marketJob = village.getUnassignedJobs().stream().filter(j -> j.getOccupation() == Occupation.MERCHANT
        && j.getBuildingUUID().equals(market.getUUID())).findFirst().orElseThrow();
    village.assignJob(marketWorker.getUUID(), marketJob);
    marketWorker.setOccupation(Occupation.MERCHANT);
    marketWorker.setNoAi(true);
    level.addFreshEntity(marketWorker);
    check(Treasury.tradeBlocker(village, level).isEmpty(), "Staffed market cannot trade");
    var till = Treasury.chests(village, level).getFirst();
    till.clearContent();
    till.setItem(0,new ItemStack(Items.EMERALD,64));
    till.setItem(1,new ItemStack(Items.DIAMOND,64));
    for (BlockPos local : castle.getInfo().getCastleLayout().evidenceContainers()) {
      Container evidence = (Container)level.getBlockEntity(origin.offset(local));
      evidence.setItem(0,new ItemStack(Items.NETHERITE_INGOT,64));
      check(!village.getVillageContainerPositions().contains(origin.offset(local)), "Evidence became trade stock");
    }
    var player = new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"CastleShopper"),ClientInformation.createDefault());
    player.connection = new ServerGamePacketListenerImpl(level.getServer(),new Connection(PacketFlow.SERVERBOUND),player,
        CommonListenerCookie.createInitial(player.getGameProfile(),false)) {
      @Override public void send(Packet<?> packet) { }
      @Override public void send(Packet<?> packet, PacketSendListener listener) { }
    };
    player.moveTo(merchant.getX()+1,merchant.getY(),merchant.getZ());
    player.getInventory().add(new ItemStack(Items.EMERALD,64));
    var menu = new MarketMenu(1,player.getInventory(),merchant.getId(),"Castle merchant","",true);
    player.containerMenu = menu;
    check(menu.stillValid(player),"Castle merchant menu invalid at stall");
    var selling=menu.sellingSnapshot();
    check(!selling.isEmpty(),"No goods offered");
    var buy=selling.getFirst();
    int playerMoney=count(player,Items.EMERALD), playerGoods=count(player,buy.item());
    int treasury=Treasury.balance(village,level), villageGoods=villageCount(village,buy.item());
    menu.select(0);
    check(!menu.getSlot(MarketMenu.RESULT).getItem().isEmpty(),"Buy did not stage");
    menu.clicked(MarketMenu.RESULT,0,ClickType.QUICK_MOVE,player);
    menu.removed(player);
    int bought=count(player,buy.item())-playerGoods;
    check(bought>0 && bought%buy.itemCount()==0,"Buy goods missing or fractional");
    int paid=buy.emeralds()*(bought/buy.itemCount());
    check(count(player,Items.EMERALD)==playerMoney-paid,"Buy payment missing or duplicated");
    check(Treasury.balance(village,level)==treasury+paid && villageCount(village,buy.item())==villageGoods-bought,"Buy did not use real treasury/stock");
    menu=new MarketMenu(2,player.getInventory(),merchant.getId(),"Castle merchant","",true);
    player.containerMenu=menu;
    check(!menu.wantedSnapshot().isEmpty(),"No wanted goods");
    var sell=menu.wantedSnapshot().getFirst();
    player.getInventory().add(new ItemStack(sell.item(),sell.itemCount()));
    playerMoney=count(player,Items.EMERALD); playerGoods=count(player,sell.item());
    treasury=Treasury.balance(village,level); villageGoods=villageCount(village,sell.item());
    menu.select(menu.sellingSnapshot().size());
    check(!menu.getSlot(MarketMenu.RESULT).getItem().isEmpty(),"Sell did not stage");
    menu.clicked(MarketMenu.RESULT,0,ClickType.QUICK_MOVE,player);
    menu.removed(player);
    check(count(player,sell.item())==playerGoods-sell.itemCount() && count(player,Items.EMERALD)==playerMoney+sell.emeralds(),"Sell lost or duplicated goods/payment");
    check(Treasury.balance(village,level)==treasury-sell.emeralds() && villageCount(village,sell.item())==villageGoods+sell.itemCount(),"Sell did not use real treasury/stock");
    check(villageCount(village,Items.NETHERITE_INGOT)==0,"Evidence sold by merchant");
  }
  private static int count(ServerPlayer player, Item item) { return player.getInventory().countItem(item); }
  private static int villageCount(Village village,Item item) { return village.getVillageInventory().stream().filter(s->s.is(item)).mapToInt(ItemStack::getCount).sum(); }
  private static void check(boolean value,String message) { if(!value)throw new AssertionError(message); }
}
