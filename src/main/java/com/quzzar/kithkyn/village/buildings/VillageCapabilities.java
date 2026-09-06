package com.quzzar.kithkyn.village.buildings;

import java.util.Collection;
import java.util.HashSet;
import java.util.function.Predicate;
import java.util.List;
import java.util.Set;

import com.quzzar.kithkyn.village.Village;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * What a village can do, derived from what it has built (decided on #55).
 *
 * Never stored. The set is the union of what the standing buildings grant,
 * recomputed rather than maintained, so it cannot drift out of step with
 * reality — the failure mode this project keeps finding elsewhere. Capability
 * names are plain datapack strings, so a pack can invent one without touching
 * Java; this replaces the closed {@code Benefit} enum, which was authored in
 * most building files and read by nothing.
 *
 * Capabilities gate what a village can MAKE, never what it can build, so a
 * village can always build its way toward one it lacks.
 */
public final class VillageCapabilities {

  /** Guards against a pathological datapack: resolution converges long before this. */
  private static final int MAX_PASSES = 8;

  private VillageCapabilities() {
  }

  /**
   * Resolves the village's capabilities as a fixed point: everything
   * unconditional first, then conditional grants re-evaluated until a pass
   * adds nothing. Two buildings that each require the other's capability
   * simply never grant, which is the correct quiet failure.
   */
  public static Set<String> resolve(Village village) {
    return resolve(village.getBuildings().stream().map(Building::getInfo)
        .filter(java.util.Objects::nonNull).toList(),
        item -> village.hasItemStackInVillage(new ItemStack(item, 1)));
  }

  /** Resolves hypothetical building sets against observed supplies without changing village state. */
  public static Set<String> resolve(Collection<BuildingInfo> buildings, Predicate<Item> hasSupply) {
    List<Grant> conditional = new java.util.ArrayList<>();
    Set<String> capabilities = new HashSet<>();

    for (BuildingInfo info : buildings) {
      capabilities.addAll(info.getGrants());
      for (Grant grant : info.getConditionalGrants()) {
        if (grant.isUnconditional()) {
          capabilities.add(grant.capability());
        } else {
          conditional.add(grant);
        }
      }
    }

    for (int pass = 0; pass < MAX_PASSES && !conditional.isEmpty(); pass++) {
      boolean grewThisPass = false;
      for (var iterator = conditional.iterator(); iterator.hasNext();) {
        Grant grant = iterator.next();
        if (capabilities.contains(grant.capability())) {
          iterator.remove();
          continue;
        }
        if (isSatisfied(hasSupply, grant, capabilities)) {
          capabilities.add(grant.capability());
          iterator.remove();
          grewThisPass = true;
        }
      }
      if (!grewThisPass) {
        break;
      }
    }

    return capabilities;
  }

  private static boolean isSatisfied(Predicate<Item> hasSupply, Grant grant, Set<String> capabilities) {
    for (String required : grant.requiresCapability()) {
      if (!capabilities.contains(required)) {
        return false;
      }
    }
    for (Item required : grant.requiresSupply()) {
      // Currency is not a supply: the treasury is physical emeralds sitting in
      // a village container, and a grant asking for emeralds must not be paid
      // for out of the village's own money.
      if (required == net.minecraft.world.item.Items.EMERALD) {
        continue;
      }
      if (!hasSupply.test(required)) {
        return false;
      }
    }
    return true;
  }

}
