package com.quzzar.kithkyn.village;

import java.util.List;

import com.quzzar.kithkyn.entities.Kind;
import com.quzzar.kithkyn.entities.MarriageStatus;

import net.minecraft.nbt.CompoundTag;

/**
 * What of a dead person's record comes back when they rise, and what stays in
 * the grave (docs/undead.md). Pure: a saved entity tag in, the tag of the
 * risen out, so a test can pin down exactly which parts of a life survive.
 *
 * Everything that was the person survives: name, gender, genes, stats,
 * virtues, personality, age, parents, and every attachment, which is where the
 * persona, the personal log, the chat history and the opinions of players
 * live. What was the old life is dropped: the village, the job, the title,
 * the marriage, the camp, the pack and what they wore. What was the body's
 * moment is dropped too: position, health, wounds, effects, anger. The kind
 * becomes undead. The id is kept, because it is the same person.
 */
public final class Raising {

  /** The old life: village, work, standing in it, and what it was carrying. */
  static final List<String> OLD_LIFE = List.of(
      "VillageUUID", "VillageName", "Occupation", "Title", "Camp", "SpouseUUID", "AwaitingAdultHome",
      "Raider", "RaidTarget", "RaidSource", "WanderingMerchant", "SourceVillageUUID", "WanderingStock",
      "MerchantDespawnCountdown", "RoamHeading", "RoamDay", "RoamOrigin",
      "GuiOpen", "Immobile", "Eating", "RunningToEat", "Interrupted", "CallToBedCooldown",
      "CampfireRecoveryAvailableAt", "DaysSinceSleep", "ShieldCooldown",
      "Inventory", "MainInventory", "ArmorItems", "HandItems", "ArmorDropChances", "HandDropChances",
      "body_armor_item", "body_armor_drop_chance", "DeathLootTable", "DeathLootTableSeed");

  /** The body's moment: where it stood and how it was, at the end. */
  static final List<String> BODY = List.of(
      "Pos", "Motion", "Rotation", "FallDistance", "Fire", "Air", "OnGround", "PortalCooldown", "Passengers",
      "Tags", "TicksFrozen", "Health", "HurtTime", "HurtByTimestamp", "DeathTime", "AbsorptionAmount",
      "active_effects", "ActiveEffects", "FallFlying", "SleepingX", "SleepingY", "SleepingZ", "Brain",
      "Leash", "leash", "NoAI", "AngerTime", "AngryAt");

  private Raising() {
  }

  /** The tag a person rises from: their record with the old life and the body's moment stripped out. */
  public static CompoundTag prepare(CompoundTag dead) {
    CompoundTag risen = dead.copy();
    for (String key : OLD_LIFE) {
      risen.remove(key);
    }
    for (String key : BODY) {
      risen.remove(key);
    }
    risen.putString("Kind", Kind.UNDEAD.name());
    risen.putString("MarriageStatus", MarriageStatus.SINGLE.name());
    risen.putString("Occupation", Occupation.WANDERER.name());
    return risen;
  }

  /** The pack and what they wore, dropped where they fell and so not kept in the grave. */
  public static CompoundTag withoutBelongings(CompoundTag dead) {
    CompoundTag kept = dead.copy();
    for (String key : List.of("Inventory", "MainInventory", "ArmorItems", "HandItems", "body_armor_item")) {
      kept.remove(key);
    }
    return kept;
  }
}
