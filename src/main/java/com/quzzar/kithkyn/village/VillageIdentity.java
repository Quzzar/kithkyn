package com.quzzar.kithkyn.village;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.IntUnaryOperator;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;

/**
 * The visual identity chosen with a village's name and kept for its life.
 * Architecture still comes from the village biome; these two dye colors and
 * the banner design distinguish two villages that share the same architecture.
 */
public record VillageIdentity(
    String name,
    DyeColor primaryColor,
    DyeColor secondaryColor,
    List<BannerLayer> bannerLayers) {

  /** Which of the village's two colors a banner pattern layer uses. */
  public enum ColorRole {
    PRIMARY,
    SECONDARY;

    public static final Codec<ColorRole> CODEC = Codec.STRING.comapFlatMap(value -> {
      try {
        return DataResult.success(valueOf(value.toUpperCase(Locale.ROOT)));
      } catch (IllegalArgumentException exception) {
        return DataResult.error(() -> "Unknown village color role: " + value);
      }
    }, role -> role.name().toLowerCase(Locale.ROOT));

    public DyeColor resolve(VillageIdentity identity) {
      return this == PRIMARY ? identity.primaryColor : identity.secondaryColor;
    }
  }

  /** One overlay in a village's banner, stored as a registry id plus color role. */
  public record BannerLayer(ResourceLocation pattern, ColorRole color) {
    public static final Codec<BannerLayer> CODEC = RecordCodecBuilder.create(inst -> inst.group(
        ResourceLocation.CODEC.fieldOf("pattern").forGetter(BannerLayer::pattern),
        ColorRole.CODEC.fieldOf("color").forGetter(BannerLayer::color)
    ).apply(inst, BannerLayer::new));
  }

  private record StoredIdentity(
      String text,
      DyeColor primaryColor,
      DyeColor secondaryColor,
      List<BannerLayer> banner) {
    private static final Codec<StoredIdentity> CODEC = RecordCodecBuilder.create(inst -> inst.group(
        Codec.STRING.fieldOf("text").forGetter(StoredIdentity::text),
        DyeColor.CODEC.fieldOf("primary_color").forGetter(StoredIdentity::primaryColor),
        DyeColor.CODEC.fieldOf("secondary_color").forGetter(StoredIdentity::secondaryColor),
        BannerLayer.CODEC.listOf().fieldOf("banner").forGetter(StoredIdentity::banner)
    ).apply(inst, StoredIdentity::new));
  }

  private static final Codec<VillageIdentity> STRUCTURED_CODEC = StoredIdentity.CODEC.flatXmap(
      stored -> validate(new VillageIdentity(
          stored.text(), stored.primaryColor(), stored.secondaryColor(), stored.banner())),
      identity -> validate(identity).map(valid -> new StoredIdentity(
          valid.name(), valid.primaryColor(), valid.secondaryColor(), valid.bannerLayers())));

  /**
   * Keeps the existing Village codec at sixteen fields by evolving its old
   * {@code name} string into this record. Old saves still decode from a plain
   * string; new saves write the complete identity into that same slot.
   */
  public static final Codec<VillageIdentity> CODEC = Codec.either(Codec.STRING, STRUCTURED_CODEC)
      .xmap(value -> value.map(VillageIdentity::legacy, identity -> identity), Either::right);

  private static final List<List<BannerLayer>> DESIGNS = List.of(
      design("stripe_center", "border"),
      design("cross", "border"),
      design("rhombus", "border"),
      design("triangles_bottom", "stripe_top"),
      design("diagonal_right", "stripe_downleft"),
      List.of(
          layer("half_vertical", ColorRole.SECONDARY),
          layer("stripe_center", ColorRole.PRIMARY),
          layer("border", ColorRole.SECONDARY)),
      List.of(
          layer("half_horizontal", ColorRole.SECONDARY),
          layer("stripe_middle", ColorRole.PRIMARY),
          layer("border", ColorRole.SECONDARY)));

  public VillageIdentity {
    bannerLayers = List.copyOf(bannerLayers);
  }

  /** Chooses an identity at founding, immediately after the name lands. */
  public static VillageIdentity generate(String name, RandomSource random) {
    return generate(name, random::nextInt);
  }

  /**
   * Gives a legacy village a stable identity derived from its name. Reopening
   * an old world therefore never causes its colors or banner to change.
   */
  public static VillageIdentity legacy(String name) {
    Random random = new Random(0x4B4954484B594EL ^ name.hashCode());
    return generate(name, random::nextInt);
  }

  private static VillageIdentity generate(String name, IntUnaryOperator nextInt) {
    DyeColor primary = DyeColor.values()[nextInt.applyAsInt(DyeColor.values().length)];
    List<DyeColor> secondaries = Arrays.stream(DyeColor.values())
        .filter(color -> color != primary)
        .filter(color -> colorDistance(primary, color) >= 90)
        .toList();
    if (secondaries.isEmpty()) {
      secondaries = Arrays.stream(DyeColor.values()).filter(color -> color != primary).toList();
    }
    DyeColor secondary = secondaries.get(nextInt.applyAsInt(secondaries.size()));
    List<BannerLayer> design = DESIGNS.get(nextInt.applyAsInt(DESIGNS.size()));
    return new VillageIdentity(name, primary, secondary, design);
  }

  private static DataResult<VillageIdentity> validate(VillageIdentity identity) {
    if (identity.name.isBlank()) {
      return DataResult.error(() -> "Village identity has no name");
    }
    if (identity.primaryColor == identity.secondaryColor) {
      return DataResult.error(() -> "Village primary and secondary colors must differ");
    }
    if (identity.bannerLayers.isEmpty() || identity.bannerLayers.size() > 6) {
      return DataResult.error(() -> "Village banner must have between one and six pattern layers");
    }
    return DataResult.success(identity);
  }

  private static int colorDistance(DyeColor first, DyeColor second) {
    int a = first.getTextureDiffuseColor();
    int b = second.getTextureDiffuseColor();
    int red = ((a >> 16) & 0xff) - ((b >> 16) & 0xff);
    int green = ((a >> 8) & 0xff) - ((b >> 8) & 0xff);
    int blue = (a & 0xff) - (b & 0xff);
    return (int) Math.sqrt(red * red + green * green + blue * blue);
  }

  private static List<BannerLayer> design(String first, String second) {
    return List.of(layer(first, ColorRole.SECONDARY), layer(second, ColorRole.SECONDARY));
  }

  private static BannerLayer layer(String pattern, ColorRole color) {
    return new BannerLayer(ResourceLocation.withDefaultNamespace(pattern), color);
  }
}
