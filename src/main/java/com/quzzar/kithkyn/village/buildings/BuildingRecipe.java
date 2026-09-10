package com.quzzar.kithkyn.village.buildings;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.quzzar.kithkyn.Kithkyn;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Validated construction price used by category-level defaults and explicit building overrides. */
public record BuildingRecipe(List<BuildingRecipe.Material> cost) {
  public static final String DIRECTORY = "kithkyn/construction_recipes";

  /** Quantities are not inventory stacks; a recipe may require more than one stack of an item. */
  public record Material(Item item, int count) {
    private static final Codec<Material> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(Material::item),
        Codec.intRange(1, Integer.MAX_VALUE).fieldOf("count").forGetter(Material::count)
    ).apply(instance, Material::new));
  }

  public static final Codec<BuildingRecipe> CODEC = Material.CODEC.listOf().fieldOf("cost").codec()
      .comapFlatMap(BuildingRecipe::validate, BuildingRecipe::cost);

  public BuildingRecipe {
    cost = List.copyOf(cost);
  }

  private static DataResult<BuildingRecipe> validate(List<Material> cost) {
    if (cost.isEmpty()) return DataResult.error(() -> "Construction recipe must contain a material");
    Set<Item> seen = new HashSet<>();
    for (Material material : cost) {
      if (material.item() == Items.AIR || !seen.add(material.item())) {
        return DataResult.error(() -> "Construction recipe contains air or a repeated material");
      }
    }
    return DataResult.success(new BuildingRecipe(cost));
  }

  /** Regional names select an economic category and level without depending on another building. */
  public static ResourceLocation idFor(BuildingInfo info) {
    return ResourceLocation.fromNamespaceAndPath(Kithkyn.MODID, info.getCategory() + "_" + info.getLevel());
  }

  /** Each definition receives independent stacks so mutating one cannot reprice another. */
  public List<ItemStack> materials() {
    return cost.stream().map(material -> new ItemStack(material.item(), material.count())).toList();
  }
}
