package com.quzzar.kithkyn.worldgen;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.configuration.KithkynConfig;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.neoforge.event.AddPackFindersEvent;

/**
 * The data packs behind the two replace-or-vanilla switches (docs/undead.md).
 *
 * Each pack empties one vanilla structure set: the village set when our
 * villages replace Minecraft's, the pillager outpost set when the undead
 * replace pillagers. A pack is only offered to the world when its switch is
 * on, so turning a switch off leaves the vanilla structure set untouched and
 * vanilla villages or outposts generate exactly as they always did. That is
 * the whole reason these are packs rather than files in the mod's data folder:
 * a file there overrides vanilla unconditionally.
 */
public final class ReplacementPacks {

  private ReplacementPacks() {
  }

  public static void onAddPackFinders(AddPackFindersEvent event) {
    if (event.getPackType() != PackType.SERVER_DATA) {
      return;
    }
    if (!KithkynConfig.COMMON_SPEC.isLoaded()) {
      Kithkyn.LOGGER.warn("Data packs were assembled before the common config loaded; the replacement packs stay off");
      return;
    }
    if (KithkynConfig.COMMON.GenerateVillages.get()) {
      add(event, "replace_villages", "Kithkyn villages in place of Minecraft's");
    }
    if (KithkynConfig.COMMON.ReplacePillagers.get()) {
      add(event, "replace_pillagers", "Kithkyn undead in place of pillager outposts");
    }
  }

  private static void add(AddPackFindersEvent event, String pack, String name) {
    event.addPackFinders(
        ResourceLocation.fromNamespaceAndPath(Kithkyn.MODID, "packs/" + pack),
        PackType.SERVER_DATA,
        Component.literal(name),
        PackSource.BUILT_IN,
        true,
        Pack.Position.TOP);
  }
}
