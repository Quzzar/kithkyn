package com.quzzar.kithkyn.village.buildings;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.quzzar.kithkyn.Kithkyn;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/**
 * Loads building definitions from {@code data/<namespace>/kithkyn/buildings/*.json}
 * into the {@link Buildings} registry, so buildings are datapack content
 * instead of hardcoded Java.
 */
@EventBusSubscriber(modid = Kithkyn.MODID)
public class BuildingDefinitionLoader extends SimpleJsonResourceReloadListener {

    public BuildingDefinitionLoader() {
        super(new Gson(), "kithkyn/buildings");
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new BuildingDefinitionLoader());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, JsonElement> recipes = new HashMap<>();
        scanDirectory(resourceManager, BuildingRecipe.DIRECTORY, new Gson(), recipes);
        Map<String, BuildingInfo> loaded = resolve(jsons, recipes);
        Buildings.reload(loaded);
        Kithkyn.LOGGER.info("Loaded {} village building definitions", loaded.size());
    }

    /** Resolve both collections before publishing, independent of other resource reload listeners. */
    static Map<String, BuildingInfo> resolve(Map<ResourceLocation, JsonElement> jsons,
            Map<ResourceLocation, JsonElement> recipeJsons) {
        Map<ResourceLocation, BuildingRecipe> recipes = new HashMap<>();
        recipeJsons.forEach((id, json) -> BuildingRecipe.CODEC.parse(JsonOps.INSTANCE, json)
                .ifError(error -> Kithkyn.LOGGER.error("Invalid construction recipe {}: {}", id, error.message()))
                .result()
                .ifPresent(recipe -> recipes.put(id, recipe)));
        Map<String, BuildingInfo> loaded = new HashMap<>();
        jsons.forEach((id, json) -> {
            BuildingInfo.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> Kithkyn.LOGGER.error("Invalid building definition {}: {}", id, error))
                .ifPresent(info -> {
                    String problem = info.validate();
                    if (problem != null) {
                        Kithkyn.LOGGER.error("Rejected building definition {} ({})", id, problem);
                        return;
                    }
                    ResourceLocation recipeId = BuildingRecipe.idFor(info);
                    BuildingRecipe recipe;
                    if (json.getAsJsonObject().has("cost")) {
                        recipe = BuildingRecipe.CODEC.parse(JsonOps.INSTANCE, json)
                            .ifError(error -> Kithkyn.LOGGER.error("Rejected building definition {}: invalid cost override: {}", id, error.message()))
                            .result()
                            .orElse(null);
                        if (recipe == null) return;
                    } else {
                        recipe = recipes.get(recipeId);
                        if (recipe == null) {
                            Kithkyn.LOGGER.error("Rejected building definition {}: missing valid construction recipe {}", id, recipeId);
                            return;
                        }
                    }
                    info.setMaterialCost(recipe.materials());
                    BuildingInfo previous = loaded.put(info.getName(), info);
                    if (previous != null) {
                        Kithkyn.LOGGER.warn("Duplicate building definition for '{}' (from {})", info.getName(), id);
                    }
                });
        });
        return Map.copyOf(loaded);
    }

}
