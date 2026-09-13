package com.quzzar.kithkyn.entities.ai.goals.work;

import javax.annotation.Nullable;

import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.village.Village;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.Items;

/** Walks an unlit miner to finished torches or fuel before the next dig. */
public final class FetchMineLightStep implements BlockWorkStep {

  @Override
  @Nullable
  public BlockPos select(RealPerson person) {
    Village village = person.getVillage();
    if (village == null || person.personMainInv.countItem(Items.TORCH) > 0) {
      return null;
    }
    if (PackLogistics.holdsAny(person.personMainInv, MineLightSupplies::isSupply)) {
      return person.blockPosition();
    }
    if (person.isInventoryFull()) {
      return null;
    }
    return PackLogistics.chestWhere(person, village, MineLightSupplies::isSupply);
  }

  @Override
  public boolean act(RealPerson person, BlockPos target) {
    if (MineLightSupplies.craftCarried(person) > 0) {
      return false;
    }
    Container chest = PackLogistics.containerAt(person, target);
    if (chest != null) {
      MineLightSupplies.restock(person, chest);
    }
    return false;
  }

  @Override
  public String describe() {
    return "torches or coal for the mine";
  }

  @Override
  public String activity() {
    return "fetching light for the mine";
  }
}
