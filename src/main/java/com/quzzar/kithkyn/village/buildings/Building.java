package com.quzzar.kithkyn.village.buildings;

import java.util.ArrayList;
import java.util.UUID;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.block.Rotation;

public class Building {

  public static final Codec<Building> CODEC = RecordCodecBuilder.create(inst -> inst.group(
      UUIDUtil.CODEC.fieldOf("id").forGetter(Building::getUUID),
      Codec.STRING.fieldOf("name").forGetter(Building::getName),
      Codec.LONG.fieldOf("center").forGetter(Building::getCenterLocation),
      Codec.LONG.fieldOf("origin").forGetter(Building::getOriginLocation),
      Codec.DOUBLE.fieldOf("radius").forGetter(Building::getRadius),
      Rotation.CODEC.fieldOf("rotation").forGetter(Building::getRotation),
      MaterialAmount.CODEC.listOf().optionalFieldOf("investment", List.of()).forGetter(Building::getInvestment),
      Codec.LONG.optionalFieldOf("completed_at", -1L).forGetter(Building::getCompletedAt),
      MineBranch.CODEC.listOf().optionalFieldOf("mine_branches", List.of()).forGetter(Building::getMineBranches),
      BuildingEntities.State.CODEC.optionalFieldOf("initial_entities")
          .forGetter(building -> java.util.Optional.of(building.getInitialEntities())),
      BuildingInfo.MineEntrance.CODEC.optionalFieldOf("mine_entrance", BuildingInfo.MineEntrance.DEFAULT)
          .forGetter(Building::getMineEntrance),
      Codec.INT.optionalFieldOf("placed_sink").forGetter(building -> java.util.Optional.of(building.getPlacedSink()))
  ).apply(inst, Building::new));

  private UUID id;
  private String name;
  private long centerLoc;
  private long originLoc;
  private double radius;
  private Rotation rotation;
  private List<MaterialAmount> investment = List.of();
  private long completedAt = -1;
  private List<MineBranch> mineBranches = List.of();
  /** Null only while decoding a save that predates explicit initial-entity receipts. */
  private BuildingEntities.State initialEntities = BuildingEntities.State.PENDING;
  private BuildingInfo.MineEntrance mineEntrance = BuildingInfo.MineEntrance.DEFAULT;
  private Integer placedSink;

  public Building(String name, Rotation rotation) {
    this.id = UUID.randomUUID();
    this.name = name;
    this.rotation = rotation;
    capturePlacement();
  }

  public Building(BlockPos originLoc, String name, Rotation rotation) {
    this.id = UUID.randomUUID();
    this.originLoc = originLoc.asLong();
    this.name = name;
    this.rotation = rotation;
    capturePlacement();
  }

  /**
   * The same building, one level up: identity and orientation kept, definition
   * swapped. The origin may slide so a larger footprint can extend into the side
   * that has room. Keeping the id is what lets a worker hold their job through
   * an upgrade (docs/building-spec.md): assignments point at a building by id
   * and station index, so a new id would release every one of them.
   */
  public static Building upgradeOf(Building from, String newName, BlockPos origin, Rotation rotation) {
    return new Building(from.getUUID(), newName, from.getCenterLocation(),
        origin.asLong(), from.getRadius(), rotation, from.investment, from.completedAt,
        from.mineBranches, java.util.Optional.of(from.getInitialEntities()), from.mineEntrance,
        java.util.Optional.ofNullable(Buildings.getByName(newName)).map(BuildingInfo::getSink));
  }

  private Building(UUID id, String name, long centerLoc, long originLoc, double radius, Rotation rotation,
      List<MaterialAmount> investment, long completedAt, List<MineBranch> mineBranches,
      java.util.Optional<BuildingEntities.State> initialEntities, BuildingInfo.MineEntrance mineEntrance,
      java.util.Optional<Integer> placedSink) {
    this.id = id;
    this.name = name;
    this.centerLoc = centerLoc;
    this.originLoc = originLoc;
    this.radius = radius;
    this.rotation = rotation;
    this.investment = List.copyOf(investment);
    this.completedAt = completedAt;
    this.mineBranches = List.copyOf(mineBranches);
    this.initialEntities = initialEntities.orElse(null);
    this.mineEntrance = mineEntrance;
    this.placedSink = placedSink.orElse(null);
  }

  /** Definition changes must not rotate an already excavated mine or move standing ground. */
  private void capturePlacement() {
    BuildingInfo info = getInfo();
    mineEntrance = info == null ? BuildingInfo.MineEntrance.DEFAULT : info.getMineEntrance();
    placedSink = info == null ? 0 : info.getSink();
  }

  public BuildingInfo.MineEntrance getMineEntrance() { return mineEntrance; }

  public int getPlacedSink() {
    if (placedSink != null) return placedSink;
    // This was the only sink changed when placement snapshots were introduced.
    if (name.equals("storehouse_birch_forest_1")) return 0;
    return getInfo() == null ? 0 : getInfo().getSink();
  }

  /** Legacy standing buildings are sealed, never silently restocked by a new release. */
  public BuildingEntities.State getInitialEntities() {
    return initialEntities == null ? BuildingEntities.State.COMPLETE : initialEntities;
  }

  public void setInitialEntities(BuildingEntities.State state) {
    initialEntities = state;
  }

  /** Old fresh projects have not run their entity tail yet; upgrades keep their old inhabitants. */
  void resumeLegacyInitialEntities(boolean finished, ConstructionMode mode) {
    if (initialEntities == null) {
      initialEntities = !finished && mode == ConstructionMode.FRESH
          ? BuildingEntities.State.PENDING : BuildingEntities.State.COMPLETE;
    }
  }

  /** Actual paid materials; founding, developer placement, and legacy saves begin with no credit. */
  public List<MaterialAmount> getInvestment() {
    return investment;
  }

  public void recordInvestment(List<MaterialAmount> payment) {
    investment = MaterialAmount.combine(investment, payment);
  }

  public long getCompletedAt() {
    return completedAt;
  }

  public void markCompletedAt(long gameTime) {
    completedAt = gameTime;
  }

  /** The second-generation shafts this mine has opened, in work order. */
  public List<MineBranch> getMineBranches() {
    return mineBranches;
  }

  public void addMineBranch(MineBranch branch) {
    ArrayList<MineBranch> updated = new ArrayList<>(mineBranches);
    updated.add(branch);
    mineBranches = List.copyOf(updated);
  }

  public void completeMineBranch(MineBranch branch) {
    mineBranches = mineBranches.stream()
        .map(existing -> existing.equals(branch) ? existing.completed() : existing)
        .toList();
  }

  public UUID getUUID() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setOriginLocation(long location) {
    this.originLoc = location;
  }

  public long getOriginLocation() {
    return originLoc;
  }

  public void setCenterLocation(long location) {
    this.centerLoc = location;
  }

  public long getCenterLocation() {
    return centerLoc;
  }

  public void setRadius(double radius) {
    this.radius = radius;
  }

  public double getRadius() {
    return radius;
  }

  public Rotation getRotation() {
    return rotation;
  }

  public BuildingInfo getInfo() {
    return Buildings.getByName(name);
  }


}
