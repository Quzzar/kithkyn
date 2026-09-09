package com.quzzar.kithkyn.village.buildings;

import java.util.Set;
import java.util.stream.Collectors;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.village.VillageIdentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.BedPart;

/** Resolves authored identity slots during placement and verifies them again on completion. */
public final class VillageIdentityApplier {

  private static final Set<String> COLORABLE_SUFFIXES = Set.of(
      "wool",
      "carpet",
      "concrete",
      "concrete_powder",
      "terracotta",
      "glazed_terracotta",
      "stained_glass",
      "stained_glass_pane",
      "candle",
      "bed");

  private VillageIdentityApplier() {
  }

  /** Cached, rotated slot positions shared by preview, matching, and both block-placement paths. */
  public static final class Placement {
    private final Building building;
    private final VillageIdentity identity;
    private final Set<BlockPos> primary;
    private final Set<BlockPos> secondary;
    private final Set<BlockPos> banners;

    private Placement(Building building, VillageIdentity identity) {
      this.building = building;
      this.identity = identity;
      VillageIdentitySlots slots = building.getInfo() == null ? VillageIdentitySlots.EMPTY
          : building.getInfo().getVillageIdentitySlots();
      BlockPos origin = BlockPos.of(building.getOriginLocation());
      primary = slots.primaryBlocks().stream().map(pos -> worldPosition(origin, pos, building)).collect(Collectors.toSet());
      secondary = slots.secondaryBlocks().stream().map(pos -> worldPosition(origin, pos, building)).collect(Collectors.toSet());
      banners = slots.banners().stream().map(pos -> worldPosition(origin, pos, building)).collect(Collectors.toSet());
    }

    /** Receives a state already rotated into the world, preserving all shared state properties. */
    public BlockState state(BlockPos pos, BlockState authored) {
      if (identity == null) {
        return authored;
      }
      if (banners.contains(pos) && authored.getBlock() instanceof AbstractBannerBlock) {
        return bannerState(authored, identity.primaryColor());
      }
      BlockPos partner = authored.getBlock() instanceof BedBlock ? bedPartner(pos, authored) : pos;
      if (primary.contains(pos) || primary.contains(partner)) {
        return recolor(authored, identity.primaryColor());
      }
      if (secondary.contains(pos) || secondary.contains(partner)) {
        return recolor(authored, identity.secondaryColor());
      }
      return authored;
    }

    /** Runs after captured block-entity NBT, so a captured banner cannot replace the village flag. */
    public void afterBlockPlaced(ServerLevel level, BlockPos pos) {
      if (identity != null && banners.contains(pos)) {
        applyBanner(level, pos, identity, building.getName());
      }
    }
  }

  public static Placement placement(Building building, VillageIdentity identity) {
    return new Placement(building, identity);
  }

  public static void apply(ServerLevel level, Building building, VillageIdentity identity) {
    BuildingInfo info = building.getInfo();
    if (info == null || info.getVillageIdentitySlots().equals(VillageIdentitySlots.EMPTY)) {
      return;
    }
    BlockPos origin = BlockPos.of(building.getOriginLocation());
    VillageIdentitySlots slots = info.getVillageIdentitySlots();
    slots.primaryBlocks().forEach(relative -> applyAccent(
        level, worldPosition(origin, relative, building), identity.primaryColor(), building));
    slots.secondaryBlocks().forEach(relative -> applyAccent(
        level, worldPosition(origin, relative, building), identity.secondaryColor(), building));
    slots.banners().forEach(relative -> applyBanner(
        level, worldPosition(origin, relative, building), identity, building.getName()));
  }

  private static BlockPos worldPosition(BlockPos origin, BlockPos relative, Building building) {
    return origin.offset(relative.rotate(building.getRotation()));
  }

  private static void applyAccent(ServerLevel level, BlockPos pos, DyeColor color, Building building) {
    BlockState current = level.getBlockState(pos);
    if (current.getBlock() instanceof BedBlock) {
      applyBedAccent(level, pos, current, color, building);
      return;
    }
    BlockState replacement = recolor(current, color);
    if (replacement == current) {
      if (isColorable(current)) {
        return;
      }
      Kithkyn.LOGGER.warn("Village identity slot in '{}' points at non-colorable block {} at {}",
          building.getName(), BuiltInRegistries.BLOCK.getKey(current.getBlock()), pos.toShortString());
      return;
    }
    level.setBlock(pos, replacement, 3);
  }

  /** A single authored bed slot colors both halves without a transient mismatched bed breaking. */
  private static void applyBedAccent(
      ServerLevel level, BlockPos pos, BlockState current, DyeColor color, Building building) {
    BlockPos partnerPos = bedPartner(pos, current);
    BlockState partner = level.getBlockState(partnerPos);
    if (!isMatchingBedPartner(current, partner)) {
      Kithkyn.LOGGER.warn("Village bed slot in '{}' has no matching second half at {}",
          building.getName(), partnerPos.toShortString());
      return;
    }
    BlockState replacement = recolor(current, color);
    BlockState partnerReplacement = recolor(partner, color);
    // Vanilla removes a bed when a neighbor has a different bed block (including color).
    // Suppress shape propagation until both replacements exist, then notify normally.
    int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    level.setBlock(pos, replacement, flags);
    level.setBlock(partnerPos, partnerReplacement, flags);
    replacement.updateNeighbourShapes(level, pos, Block.UPDATE_ALL);
    partnerReplacement.updateNeighbourShapes(level, partnerPos, Block.UPDATE_ALL);
    level.updateNeighborsAt(pos, replacement.getBlock());
    level.updateNeighborsAt(partnerPos, partnerReplacement.getBlock());
  }

  static BlockPos bedPartner(BlockPos pos, BlockState state) {
    Direction facing = state.getValue(BedBlock.FACING);
    return pos.relative(state.getValue(BedBlock.PART) == BedPart.FOOT ? facing : facing.getOpposite());
  }

  static boolean isMatchingBedPartner(BlockState state, BlockState partner) {
    return partner.is(state.getBlock())
        && partner.getValue(BedBlock.PART) != state.getValue(BedBlock.PART)
        && partner.getValue(BedBlock.FACING) == state.getValue(BedBlock.FACING);
  }

  static BlockState recolor(BlockState state, DyeColor color) {
    ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
    String suffix = coloredSuffix(id.getPath());
    if (suffix == null || !COLORABLE_SUFFIXES.contains(suffix)) {
      return state;
    }
    ResourceLocation targetId = ResourceLocation.withDefaultNamespace(color.getName() + "_" + suffix);
    if (!BuiltInRegistries.BLOCK.containsKey(targetId)) {
      return state;
    }
    return copySharedProperties(state, BuiltInRegistries.BLOCK.get(targetId).defaultBlockState());
  }

  private static boolean isColorable(BlockState state) {
    String suffix = coloredSuffix(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath());
    return suffix != null && COLORABLE_SUFFIXES.contains(suffix);
  }

  private static String coloredSuffix(String path) {
    for (DyeColor color : DyeColor.values()) {
      String prefix = color.getName() + "_";
      if (path.startsWith(prefix)) {
        return path.substring(prefix.length());
      }
    }
    return null;
  }

  /** Shared flag application for building slots and authored wall attachments. */
  static void applyBanner(ServerLevel level, BlockPos pos, VillageIdentity identity, String source) {
    BlockState current = level.getBlockState(pos);
    if (!(current.getBlock() instanceof AbstractBannerBlock)) {
      Kithkyn.LOGGER.warn("Village banner slot in '{}' points at {} rather than a banner at {}",
          source, BuiltInRegistries.BLOCK.getKey(current.getBlock()), pos.toShortString());
      return;
    }

    BlockState replacement = bannerState(current, identity.primaryColor());
    level.setBlock(pos, replacement, 3);

    if (level.getBlockEntity(pos) instanceof BannerBlockEntity banner) {
      ItemStack flag = new ItemStack(BannerBlock.byColor(identity.primaryColor()));
      flag.set(DataComponents.BANNER_PATTERNS, bannerPatterns(level, identity));
      flag.set(DataComponents.CUSTOM_NAME, Component.literal(identity.name() + " Banner"));
      banner.fromItem(flag, identity.primaryColor());
      banner.setChanged();
      level.sendBlockUpdated(pos, current, replacement, 3);
    }
  }

  static BlockState bannerState(BlockState current, DyeColor color) {
    String suffix = current.getBlock() instanceof WallBannerBlock ? "_wall_banner" : "_banner";
    ResourceLocation targetId = ResourceLocation.withDefaultNamespace(color.getName() + suffix);
    return copySharedProperties(current, BuiltInRegistries.BLOCK.get(targetId).defaultBlockState());
  }

  /** Block-state equality alone misses an unpatterned white-primary flag. */
  static boolean bannerMatches(ServerLevel level, BlockPos pos, VillageIdentity identity) {
    return level.getBlockEntity(pos) instanceof BannerBlockEntity banner
        && banner.getBaseColor() == identity.primaryColor()
        && banner.getPatterns().equals(bannerPatterns(level, identity))
        && Component.literal(identity.name() + " Banner").equals(banner.getCustomName());
  }

  private static BannerPatternLayers bannerPatterns(ServerLevel level, VillageIdentity identity) {
    var patterns = level.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN);
    BannerPatternLayers.Builder builder = new BannerPatternLayers.Builder();
    for (VillageIdentity.BannerLayer layer : identity.bannerLayers()) {
      ResourceKey<BannerPattern> key = ResourceKey.create(Registries.BANNER_PATTERN, layer.pattern());
      patterns.get(key).ifPresent(pattern -> builder.add(pattern, layer.color().resolve(identity)));
    }
    return builder.build();
  }

  private static BlockState copySharedProperties(BlockState source, BlockState target) {
    BlockState copied = target;
    for (Property<?> property : source.getProperties()) {
      copied = copyProperty(source, copied, property);
    }
    return copied;
  }

  private static <T extends Comparable<T>> BlockState copyProperty(
      BlockState source, BlockState target, Property<T> property) {
    return target.hasProperty(property) ? target.setValue(property, source.getValue(property)) : target;
  }
}
