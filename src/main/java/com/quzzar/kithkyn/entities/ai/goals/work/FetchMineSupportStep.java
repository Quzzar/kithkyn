package com.quzzar.kithkyn.entities.ai.goals.work;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.village.Village;
import com.quzzar.kithkyn.village.buildings.MineSupportMaterials;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Restocking the shaft's lining from the stores by day: a CARRY step, container
 * to pack, and the daytime half of the miner's bedtime support restock.
 *
 * The bedtime restock fills the pack once a night; a shaft that runs out of
 * lining before then stands down at a face it may not break without a seal
 * ({@link MineStep}). In sand country that was a whole first day idle, since
 * the pack held only sand and the builder's cleared sand sat in the stores.
 * This walks to a chest holding dirt or stone, or the sand that presses into
 * sandstone at the wall ({@link MineSupportMaterials}), and carries a working
 * load back. It engages only when the pack holds no lining at all, so it never
 * competes with the digging, and a village with nothing in store leaves it
 * dormant, which is what a real shortage looks like.
 */
public final class FetchMineSupportStep implements BlockWorkStep {

  /** Same helping as the bedtime restock takes; the pack is not a warehouse. */
  private static final int PACK_TARGET = 32;

  @Override
  @Nullable
  public BlockPos select(RealPerson person) {
    Village village = person.getVillage();
    if (village == null || person.isInventoryFull()
        || MineSupportMaterials.held(person.personMainInv) > 0) {
      return null;
    }
    return PackLogistics.chestHolding(person, village, wanted());
  }

  @Override
  public boolean act(RealPerson person, BlockPos target) {
    Container chest = PackLogistics.containerAt(person, target);
    if (chest == null) {
      return false; // the chest was taken out from under them
    }
    PackLogistics.pullWanted(person, chest, wanted(), person.getOccupation().name());
    return false; // one visit per select: full or not, back to the shaft
  }

  /** Every lining item the shaft can place, then the sand kinds that press into one. */
  private static List<ItemStack> wanted() {
    List<ItemStack> wanted = new ArrayList<>();
    for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(MineSupportMaterials.TAG)) {
      wanted.add(new ItemStack(holder.value(), PACK_TARGET));
    }
    for (Item sand : MineSupportMaterials.pressableSand()) {
      wanted.add(new ItemStack(sand, PACK_TARGET * MineSupportMaterials.SAND_PER_BLOCK));
    }
    return wanted;
  }

  @Override
  public String describe() {
    return "dirt, stone or sand for the shaft's lining";
  }

  @Override
  public String activity() {
    return "fetching lining for the mine from the stores";
  }
}
