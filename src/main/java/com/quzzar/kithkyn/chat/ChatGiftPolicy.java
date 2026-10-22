package com.quzzar.kithkyn.chat;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import com.quzzar.kithkyn.entities.ClericPotions;
import com.quzzar.kithkyn.entities.RealPerson;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.common.Tags;

/** Conservative authorization for a requested gift, shared by generation, parsing and slot removal. */
final class ChatGiftPolicy {
  private static final Set<String> PROTECTED = Set.of("minecraft:diamond", "minecraft:emerald",
      "minecraft:netherite_ingot", "minecraft:netherite_scrap", "minecraft:ancient_debris",
      "minecraft:gold_ingot", "minecraft:gold_nugget", "minecraft:gold_block", "minecraft:diamond_block",
      "minecraft:emerald_block", "minecraft:netherite_block", "minecraft:golden_apple", "minecraft:enchanted_golden_apple");
  private static final Pattern REQUEST = Pattern.compile("^(?:hey )?(?:please )?(?:"
      + "(?:can|could|would|will) you (?:please )?(?:give|hand|pass|share|spare|lend)"
      + "|(?:can|could|may) (?:i|we) (?:please )?(?:have|get|take|borrow)"
      + "|i (?:need|would like|could use)"
      + "|(?:give|hand|pass|share|spare|lend) (?:me|us))\\b");

  private ChatGiftPolicy() { }

  static boolean protectedId(String itemId) {
    return PROTECTED.contains(itemId.contains(":") ? itemId : "minecraft:" + itemId);
  }

  /** Unknown paraphrases do not authorize an inventory change; speech may still answer normally. */
  static boolean requested(String itemId, String speakerLine) {
    if (protectedId(itemId) || speakerLine == null) return false;
    String words = normalize(speakerLine);
    if (!REQUEST.matcher(words).find()) return false;
    String name = normalize(itemId.substring(itemId.indexOf(':') + 1));
    return (" " + words + " ").contains(" " + name + " ")
        || (" " + words + " ").contains(" " + name + "s ");
  }

  /** Precious stacks and enchanted equipment are never gifts, even if a model names them. */
  static int giveable(RealPerson person, ItemStack stack) {
    if (stack.isEmpty() || protectedId(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
        || stack.is(Tags.Items.GEMS) || stack.getRarity() != Rarity.COMMON
        || stack.isEnchanted()) return 0;
    return ClericPotions.isCleric(person) ? ClericPotions.giveable(person, stack) : stack.getCount();
  }

  private static String normalize(String text) {
    return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").strip();
  }
}
