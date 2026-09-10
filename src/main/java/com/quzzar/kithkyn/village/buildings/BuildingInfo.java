package com.quzzar.kithkyn.village.buildings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.quzzar.kithkyn.utils.KithkynCodecs;
import com.quzzar.kithkyn.village.Occupation;
import com.quzzar.kithkyn.village.GuardRole;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class BuildingInfo {

  /** Two authored bed coordinates that belong to one couple, independent of array ordering. */
  public record CoupleBeds(BlockPos first, BlockPos second) {
    public static final Codec<CoupleBeds> CODEC = BlockPos.CODEC.listOf().comapFlatMap(
        positions -> positions.size() == 2
            ? com.mojang.serialization.DataResult.success(new CoupleBeds(positions.get(0), positions.get(1)))
            : com.mojang.serialization.DataResult.error(() -> "couple_beds entries require exactly two beds"),
        pair -> List.of(pair.first(), pair.second()));
  }

  /** A mine's excavation frame, independent of the building's front and job station. */
  public record MineEntrance(Direction facing, BlockPos offset) {
    public static final MineEntrance DEFAULT = new MineEntrance(Direction.SOUTH, BlockPos.ZERO);
    public static final Codec<MineEntrance> CODEC = RecordCodecBuilder.create(inst -> inst.group(
        Direction.CODEC.optionalFieldOf("facing", Direction.SOUTH).forGetter(MineEntrance::facing),
        BlockPos.CODEC.optionalFieldOf("offset", BlockPos.ZERO).forGetter(MineEntrance::offset)
    ).apply(inst, MineEntrance::new));
  }

  /** A work station inside a building: a position (relative to the structure origin) plus the job worked there. */
  public record WorkStation(BlockPos pos, Occupation occupation, java.util.Optional<GuardRole> guardDuty) {
    public static final Codec<WorkStation> CODEC = RecordCodecBuilder.create(inst -> inst.group(
        BlockPos.CODEC.fieldOf("pos").forGetter(WorkStation::pos),
        KithkynCodecs.forEnum(Occupation.class).fieldOf("occupation").forGetter(WorkStation::occupation),
        KithkynCodecs.forEnum(GuardRole.class).optionalFieldOf("guard_duty").forGetter(WorkStation::guardDuty)
    ).apply(inst, WorkStation::new));
  }

  /** Explicit room storage, bound to an authored bed rather than guessed across floors. */
  public record BedContainers(BlockPos bed, List<BlockPos> containers) {
    public static final Codec<BedContainers> CODEC = RecordCodecBuilder.create(inst -> inst.group(
        BlockPos.CODEC.fieldOf("bed").forGetter(BedContainers::bed),
        BlockPos.CODEC.listOf().fieldOf("containers").forGetter(BedContainers::containers)
    ).apply(inst, BedContainers::new));
  }

  /** A material cost entry, kept simple on purpose (item id + count). */
  public record ItemCost(Item item, int count) {
    public static final Codec<ItemCost> CODEC = RecordCodecBuilder.create(inst -> inst.group(
        BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(ItemCost::item),
        Codec.INT.optionalFieldOf("count", 1).forGetter(ItemCost::count)
    ).apply(inst, ItemCost::new));
  }

  private static final MapCodec<BuildingInfo> BASE_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
      Codec.STRING.fieldOf("structure").forGetter(BuildingInfo::getName),
      BlockPos.CODEC.listOf().optionalFieldOf("beds", List.of()).forGetter(BuildingInfo::bedPositions),
      WorkStation.CODEC.listOf().optionalFieldOf("work_stations", List.of()).forGetter(BuildingInfo::workStations),
      BlockPos.CODEC.listOf().optionalFieldOf("containers", List.of()).forGetter(BuildingInfo::containerPositions),
      BlockPos.CODEC.listOf().optionalFieldOf("personal_containers", List.of())
          .forGetter(BuildingInfo::personalContainerPositions),
      ItemCost.CODEC.listOf().optionalFieldOf("cost", List.of()).forGetter(BuildingInfo::itemCosts),
      Codec.STRING.listOf().optionalFieldOf("grants", List.of()).forGetter(BuildingInfo::getGrants),
      Grant.CODEC.listOf().optionalFieldOf("grants_if", List.of()).forGetter(BuildingInfo::getConditionalGrants),
      BlockPos.CODEC.optionalFieldOf("gathering_point").forGetter(info ->
          java.util.Optional.ofNullable(info.gatheringPoint).map(BlockPos::of)),
      Codec.STRING.optionalFieldOf("category").forGetter(info -> java.util.Optional.ofNullable(info.explicitCategory)),
      Codec.STRING.optionalFieldOf("variant").forGetter(info -> java.util.Optional.ofNullable(info.explicitVariant)),
      Codec.STRING.optionalFieldOf("upgrades_from").forGetter(info -> java.util.Optional.ofNullable(info.upgradesFrom)),
      VillageIdentitySlots.CODEC.optionalFieldOf("village_identity", VillageIdentitySlots.EMPTY)
          .forGetter(BuildingInfo::getVillageIdentitySlots),
      Codec.INT.optionalFieldOf("sink", 0).forGetter(BuildingInfo::getSink),
      Direction.CODEC.optionalFieldOf("entrance_facing")
          .forGetter(info -> java.util.Optional.ofNullable(info.entranceFacing)),
      MineEntrance.CODEC.optionalFieldOf("mine_entrance", MineEntrance.DEFAULT)
          .forGetter(BuildingInfo::getMineEntrance)
  ).apply(inst, BuildingInfo::fromCodec));

  /** Meeting ground and fire blocks are independent, authored in structure coordinates. */
  private record GatheringPlaces(java.util.Optional<BlockPos> meetingPoint,
      java.util.Optional<List<BlockPos>> campfires) {
    private static final GatheringPlaces EMPTY = new GatheringPlaces(
        java.util.Optional.empty(), java.util.Optional.empty());
    private static final MapCodec<GatheringPlaces> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
        BlockPos.CODEC.optionalFieldOf("meeting_point").forGetter(GatheringPlaces::meetingPoint),
        BlockPos.CODEC.listOf().optionalFieldOf("campfires").forGetter(GatheringPlaces::campfires)
    ).apply(inst, GatheringPlaces::new));
  }

  public static final Codec<BuildingInfo> CODEC = RecordCodecBuilder.create(inst -> inst.group(
      BASE_CODEC.forGetter(info -> info),
      GatheringPlaces.CODEC.forGetter(info -> info.gatheringPlaces),
      BedContainers.CODEC.listOf().optionalFieldOf("bed_containers")
          .forGetter(info -> java.util.Optional.ofNullable(info.bedContainers)),
      CoupleBeds.CODEC.listOf().optionalFieldOf("couple_beds")
          .forGetter(info -> java.util.Optional.ofNullable(info.coupleBeds)),
      BlockPos.CODEC.listOf().optionalFieldOf("worker_beds")
          .forGetter(info -> java.util.Optional.ofNullable(info.workerBeds)),
      Codec.BOOL.optionalFieldOf("standalone", false).forGetter(BuildingInfo::isStandalone)
  ).apply(inst, (info, places, bedContainers, coupleBeds, workerBeds, standalone) -> {
    info.gatheringPlaces = places;
    info.bedContainers = bedContainers.orElse(null);
    info.coupleBeds = coupleBeds.orElse(null);
    info.workerBeds = workerBeds.orElse(null);
    info.standalone = standalone;
    return info;
  }));

  private static BuildingInfo fromCodec(String structure, List<BlockPos> beds, List<WorkStation> workStations,
      List<BlockPos> containers, List<BlockPos> personalContainers, List<ItemCost> costs, List<String> grants,
      List<Grant> conditionalGrants, java.util.Optional<BlockPos> gatheringPoint,
      java.util.Optional<String> category, java.util.Optional<String> variant,
      java.util.Optional<String> upgradesFrom, VillageIdentitySlots villageIdentitySlots, int sink,
      java.util.Optional<Direction> entranceFacing, MineEntrance mineEntrance) {
    BuildingInfo info = new BuildingInfo(structure);
    beds.forEach(pos -> info.addBedLocation(pos.getX(), pos.getY(), pos.getZ()));
    workStations.forEach(station -> {
      info.addWorkLocation(station.pos().getX(), station.pos().getY(), station.pos().getZ(), station.occupation());
      station.guardDuty().ifPresent(duty -> info.guardRoles.put(station.pos().asLong(), duty));
    });
    containers.forEach(pos -> info.addContainerLocation(pos.getX(), pos.getY(), pos.getZ()));
    personalContainers.forEach(pos -> info.addPersonalContainerLocation(pos.getX(), pos.getY(), pos.getZ()));
    info.setMaterialCost(costs.stream().map(cost -> new ItemStack(cost.item(), cost.count())).toList());
    info.grants = List.copyOf(grants);
    info.conditionalGrants = List.copyOf(conditionalGrants);
    gatheringPoint.ifPresent(pos -> info.gatheringPoint = pos.asLong());
    info.explicitCategory = category.orElse(null);
    info.explicitVariant = variant.orElse(null);
    info.upgradesFrom = upgradesFrom.orElse(null);
    info.villageIdentitySlots = villageIdentitySlots;
    info.sink = sink;
    info.entranceFacing = entranceFacing.orElse(null);
    info.mineEntrance = mineEntrance;
    return info;
  }

  private String path;
  private ArrayList<Long> bedLocs;
  // Insertion-ordered: JobAssignment station indexes rely on a stable iteration order.
  private LinkedHashMap<Long, Occupation> workLocs;
  private final Map<Long, GuardRole> guardRoles = new LinkedHashMap<>();
  @javax.annotation.Nullable
  private List<BedContainers> bedContainers;
  @javax.annotation.Nullable
  private List<CoupleBeds> coupleBeds;
  @javax.annotation.Nullable
  private List<BlockPos> workerBeds;
  private ArrayList<Long> containerLocs;
  // Chests that belong to the people who sleep here, never to the village (PersonalChest).
  private ArrayList<Long> personalContainerLocs;

  private List<ItemStack> materialCost;
  /** Capabilities this building always grants, as datapack strings (#55). */
  private List<String> grants = List.of();
  /** Capabilities it grants only while a condition holds. */
  private List<Grant> conditionalGrants = List.of();
  // Old datapacks use one campfire as both the civic anchor and cooking amenity.
  private Long gatheringPoint;
  private GatheringPlaces gatheringPlaces = GatheringPlaces.EMPTY;
  // Explicit JSON category/variant, validated against the id-derived values; null = derive.
  private String explicitCategory;
  private String explicitVariant;
  // The id this building can replace in place; it also defines the fresh-build cost chain.
  private String upgradesFrom;
  private boolean standalone;
  private VillageIdentitySlots villageIdentitySlots = VillageIdentitySlots.EMPTY;
  private int sink;
  private Direction entranceFacing;
  private MineEntrance mineEntrance = MineEntrance.DEFAULT;

  /** Authored outward door direction; old catalogs keep their established fronts. */
  public Direction getEntranceFacing() {
    return entranceFacing != null ? entranceFacing
        : "storehouse".equals(getCategory()) ? Direction.SOUTH : Direction.NORTH;
  }

  public MineEntrance getMineEntrance() {
    return mineEntrance;
  }

  /** Turns the authored front toward the center, independently for each companion. */
  public Rotation rotationFacing(Direction toward) {
    for (Rotation rotation : Rotation.values()) {
      if (rotation.rotate(getEntranceFacing()) == toward) return rotation;
    }
    throw new IllegalArgumentException("Building entrances must face horizontally");
  }

  public BuildingInfo(String path) {

    this.path = path;
    this.bedLocs = new ArrayList<>();
    this.workLocs = new LinkedHashMap<>();
    this.containerLocs = new ArrayList<>();
    this.personalContainerLocs = new ArrayList<>();
    this.materialCost = new ArrayList<>();

  }

  public String getName() {
    return path;
  }

  public String getPath() {
    return path;
  }

  /**
   * The id scheme is {@code <category>_<variant>_<level>[__<design>]}:
   * after separating the optional design, the last token is the level, then the longest registered style suffix
   * separates variant from category. Unknown custom variants retain the old
   * single-token convention, including the developer placeholder catalog.
   */
  @javax.annotation.Nullable
  private ParsedId parsedId() {
    int designSeparator = path.indexOf("__");
    String base = designSeparator < 0 ? path : path.substring(0, designSeparator);
    String design = designSeparator < 0 ? null : path.substring(designSeparator + 2);
    if (design != null && !design.matches("[a-z0-9]+(?:_[a-z0-9]+)*")) {
      return null;
    }
    int levelSeparator = base.lastIndexOf('_');
    if (levelSeparator <= 0 || levelSeparator == base.length() - 1) {
      return null;
    }
    int level;
    try {
      level = Integer.parseInt(base.substring(levelSeparator + 1));
    } catch (NumberFormatException e) {
      return null;
    }
    String stem = base.substring(0, levelSeparator);
    String variant = null;
    for (VillageStyle style : VillageStyle.values()) {
      String candidate = style.id();
      if (stem.endsWith("_" + candidate) && stem.length() > candidate.length() + 1
          && (variant == null || candidate.length() > variant.length())) {
        variant = candidate;
      }
    }
    if (variant == null) {
      int variantSeparator = stem.lastIndexOf('_');
      if (variantSeparator <= 0 || variantSeparator == stem.length() - 1) {
        return null;
      }
      variant = stem.substring(variantSeparator + 1);
    }
    return new ParsedId(stem.substring(0, stem.length() - variant.length() - 1), variant, level, design);
  }

  private record ParsedId(String category, String variant, int level, String design) {}

  /** True when the id parses, including an optional {@code __<design>} alternative. */
  public boolean hasWellFormedId() {
    return parsedId() != null;
  }

  public String getCategory() {
    if (explicitCategory != null) {
      return explicitCategory;
    }
    return java.util.Objects.requireNonNull(parsedId(), "Malformed building id: " + path).category();
  }

  public String getVariant() {
    if (explicitVariant != null) {
      return explicitVariant;
    }
    return java.util.Objects.requireNonNull(parsedId(), "Malformed building id: " + path).variant();
  }

  public int getLevel() {
    return java.util.Objects.requireNonNull(parsedId(), "Malformed building id: " + path).level();
  }

  /** An optional layout within the same category, style and level; null identifies the canonical design. */
  @javax.annotation.Nullable
  public String getDesign() {
    return java.util.Objects.requireNonNull(parsedId(), "Malformed building id: " + path).design();
  }

  /**
   * The short human label for this building: the datapack category with its
   * underscores spaced ("couple cottage"), or the raw name when the id is not
   * well formed. The single home for a mapping the briefing, the planner, and
   * the recent-build trail had each been spelling out inline.
   */
  public String displayLabel() {
    if (!hasWellFormedId()) return getName();
    String category = getCategory().replace('_', ' ');
    return getDesign() == null ? category : category + " (" + getDesign().replace('_', ' ') + ")";
  }

  /**
   * How many of the structure's bottom layers sit below the ground plane. A
   * building is seated with its layer 0 on the ground's top block; a well
   * declares one so its water and rim lie flush with the ground and its base
   * course is buried, which is also what keeps the pool walled in by earth
   * while the builder fills it.
   */
  public int getSink() {
    return sink;
  }

  /** The exact predecessor this building can replace, or null for an independent design. */
  @javax.annotation.Nullable
  public String getUpgradesFrom() {
    return upgradesFrom;
  }

  /** Explicitly authorizes a higher tier without a compatible predecessor. */
  public boolean isStandalone() {
    return standalone;
  }

  public VillageIdentitySlots getVillageIdentitySlots() {
    return villageIdentitySlots;
  }

  /**
   * Definition-consistency check, run by the loader: a malformed id, an explicit
   * category/variant contradicting the id, or a level above 1 with no
   * {@code upgrades_from} or an explicit {@code standalone} declaration is an error.
   * Returns the problem, or null when the definition is consistent.
   */
  @javax.annotation.Nullable
  public String validate() {
    if (!hasWellFormedId()) {
      return "id '" + path + "' does not match <category>_<variant>_<level>[__<design>]";
    }
    if (getEntranceFacing().getAxis().isVertical() || mineEntrance.facing().getAxis().isVertical()) {
      return "building and mine entrances must face horizontally";
    }
    ParsedId parsed = parsedId();
    String derivedCategory = parsed.category();
    String derivedVariant = parsed.variant();
    if (explicitCategory != null && !explicitCategory.equals(derivedCategory)) {
      return "category '" + explicitCategory + "' contradicts id-derived '" + derivedCategory + "'";
    }
    if (explicitVariant != null && !explicitVariant.equals(derivedVariant)) {
      return "variant '" + explicitVariant + "' contradicts id-derived '" + derivedVariant + "'";
    }
    if (standalone && upgradesFrom != null) {
      return "standalone building cannot also declare upgrades_from";
    }
    if (getLevel() >= 2 && upgradesFrom == null && !standalone) {
      return "level " + getLevel() + " building requires upgrades_from or standalone: true";
    }
    for (Long personal : personalContainerLocs) {
      if (containerLocs.contains(personal)) {
        return "container " + BlockPos.of(personal).toShortString()
            + " is listed as both village storage and a personal chest";
      }
    }
    if (!personalContainerLocs.isEmpty() && bedLocs.isEmpty()) {
      return "a personal chest is declared but nobody sleeps here (no beds)";
    }
    if (guardRoles.values().stream().filter(role -> role == GuardRole.CAPTAIN).count() > 1) {
      return "a building may declare only one guard captain";
    }
    if (guardRoles.containsValue(GuardRole.CAPTAIN) && !"village_center".equals(getCategory())) {
      return "guard captain must belong to the village center";
    }
    for (var entry : guardRoles.entrySet()) {
      if (workLocs.get(entry.getKey()) != Occupation.GUARD) return "guard_duty requires a GUARD station";
    }
    if (bedContainers != null) {
      java.util.Set<BlockPos> mappedBeds = new java.util.HashSet<>();
      java.util.Set<Long> mappedContainers = new java.util.HashSet<>();
      for (BedContainers room : bedContainers) {
        if (!bedLocs.contains(room.bed().asLong())) return "bed_containers names an undeclared bed";
        if (!mappedBeds.add(room.bed())) return "bed_containers repeats a bed";
        java.util.Set<BlockPos> roomContainers = new java.util.HashSet<>();
        for (BlockPos container : room.containers()) {
          if (!personalContainerLocs.contains(container.asLong())) return "bed_containers names a non-personal container";
          if (!roomContainers.add(container)) return "bed_containers repeats a container within one bed mapping";
          mappedContainers.add(container.asLong());
        }
      }
      if (!mappedContainers.containsAll(personalContainerLocs)) return "personal container has no bed_containers mapping";
    }
    java.util.Set<BlockPos> pairedBeds = new java.util.HashSet<>();
    if (workerBeds != null) {
      if (!workerBeds.isEmpty() && workLocs.isEmpty()) return "worker_beds requires a workplace";
      if (new java.util.HashSet<>(workerBeds).size() != workerBeds.size()) return "worker_beds repeats a bed";
      if (workerBeds.stream().anyMatch(bed -> !bedLocs.contains(bed.asLong()))) {
        return "worker_beds names an undeclared bed";
      }
    }
    for (CoupleBeds pair : getCoupleBeds()) {
      for (BlockPos bed : List.of(pair.first(), pair.second())) {
        if (!bedLocs.contains(bed.asLong())) return "couple_beds names an undeclared bed";
        if (!pairedBeds.add(bed)) return "couple_beds repeats a bed";
      }
      if (pair.first().getY() != pair.second().getY()
          || pair.first().distManhattan(pair.second()) != 1) {
        return "couple_beds must name neighboring beds on the same floor";
      }
      if (isWorkerBed(bedLocs.indexOf(pair.first().asLong()))
          != isWorkerBed(bedLocs.indexOf(pair.second().asLong()))) {
        return "worker_beds must reserve both beds of a couple room or neither";
      }
    }
    return null;
  }

  /** Omitted metadata preserves the original two-bed cottage; ordinary homes infer no pairs. */
  public List<CoupleBeds> getCoupleBeds() {
    if (coupleBeds != null) return coupleBeds;
    if (hasWellFormedId() && Buildings.COUPLE_COTTAGE_CATEGORY.equals(getCategory()) && bedLocs.size() >= 2) {
      return List.of(new CoupleBeds(BlockPos.of(bedLocs.get(0)), BlockPos.of(bedLocs.get(1))));
    }
    return List.of();
  }

  /** Single capacity excludes both beds in every declared couple room. */
  public int getSingleBedCount() {
    return bedLocs.size() - getCoupleBeds().size() * 2;
  }

  /** Explicit coordinates allow a workplace to mix staff rooms with general accommodation. */
  public boolean isWorkerBed(int index) {
    if (index < 0 || index >= bedLocs.size()) return false;
    if (workerBeds != null) return workerBeds.contains(BlockPos.of(bedLocs.get(index)));
    return !workLocs.isEmpty() && hasWellFormedId()
        && !Buildings.VILLAGE_CENTER_CATEGORY.equals(getCategory());
  }

  public int getWorkerSingleBedCount() {
    int count = 0;
    for (int index = 0; index < bedLocs.size(); index++) {
      if (isWorkerBed(index) && !isCoupleBed(index)) count++;
    }
    return count;
  }

  /** One household qualifies through either spouse's job, never two separate worker claims. */
  public int getWorkerCoupleRoomCount() {
    return (int) getCoupleBeds().stream()
        .filter(pair -> isWorkerBed(bedLocs.indexOf(pair.first().asLong()))).count();
  }

  public boolean isCoupleBed(int index) {
    if (index < 0 || index >= bedLocs.size()) return false;
    BlockPos bed = BlockPos.of(bedLocs.get(index));
    return getCoupleBeds().stream().anyMatch(pair -> pair.first().equals(bed) || pair.second().equals(bed));
  }

  /** Sharing a building is insufficient: spouses must occupy the two beds of one room. */
  public boolean sharesCoupleBeds(int firstIndex, int secondIndex) {
    if (firstIndex == secondIndex || !isCoupleBed(firstIndex) || !isCoupleBed(secondIndex)) return false;
    BlockPos first = BlockPos.of(bedLocs.get(firstIndex));
    BlockPos second = BlockPos.of(bedLocs.get(secondIndex));
    return getCoupleBeds().stream().anyMatch(pair ->
        pair.first().equals(first) && pair.second().equals(second)
            || pair.first().equals(second) && pair.second().equals(first));
  }

  /** Null preserves old nearest-container selection; an explicit list maps only the named beds. */
  @javax.annotation.Nullable
  public List<BedContainers> getBedContainers() {
    return bedContainers;
  }

  /** Station indexes include every occupation, exactly as JobAssignment does. */
  @javax.annotation.Nullable
  public GuardRole getGuardRole(int stationIndex) {
    if (stationIndex < 0 || stationIndex >= workLocs.size()) return null;
    Long position = new ArrayList<>(workLocs.keySet()).get(stationIndex);
    return guardRoles.get(position);
  }

  public ArrayList<Long> getBedLocations() {
    return bedLocs;
  }

  public Map<Long, Occupation> getWorkLocations() {
    return workLocs;
  }

  /**
   * Village storage: every chest here that any worker may fetch from or fill.
   * A home's own chest is not among them ({@link #getPersonalContainerLocations}).
   */
  public ArrayList<Long> getContainerLocations() {
    return containerLocs;
  }

  /**
   * The chests that belong to whoever sleeps in this building rather than to
   * the village: a house's chest, or the bedside chest of a workplace with a
   * live-in bed. Never registered as village storage (PersonalChest).
   */
  public ArrayList<Long> getPersonalContainerLocations() {
    return personalContainerLocs;
  }

  public BuildingInfo addBedLocation(int x, int y, int z) {
    bedLocs.add(BlockPos.asLong(x, y, z));
    return this;
  }

  public BuildingInfo addWorkLocation(int x, int y, int z, Occupation occupation) {
    workLocs.put(BlockPos.asLong(x, y, z), occupation);
    return this;
  }

  public BuildingInfo addContainerLocation(int x, int y, int z) {
    containerLocs.add(BlockPos.asLong(x, y, z));
    return this;
  }

  public BuildingInfo addPersonalContainerLocation(int x, int y, int z) {
    personalContainerLocs.add(BlockPos.asLong(x, y, z));
    return this;
  }

  public List<ItemStack> getMaterialCost() {
    return materialCost;
  }

  public BuildingInfo setMaterialCost(List<ItemStack> materialCost) {
    this.materialCost = materialCost;
    return this;
  }

  @javax.annotation.Nullable
  public Long getGatheringPoint() {
    return gatheringPoint;
  }

  /** Authored civic anchor; legacy definitions keep their original fire anchor. */
  @javax.annotation.Nullable
  public BlockPos getMeetingPoint() {
    return gatheringPlaces.meetingPoint().orElseGet(() ->
        gatheringPoint == null ? null : BlockPos.of(gatheringPoint));
  }

  /** Explicit meeting coordinates name standing ground rather than a campfire block. */
  public boolean hasExplicitMeetingPoint() {
    return gatheringPlaces.meetingPoint().isPresent();
  }

  /** An explicit empty list intentionally disables fire use, even with a legacy anchor. */
  public List<BlockPos> getCampfireLocations() {
    return gatheringPlaces.campfires().orElseGet(() ->
        gatheringPoint == null ? List.of() : List.of(BlockPos.of(gatheringPoint)));
  }

  public List<String> getGrants() {
    return grants;
  }

  public List<Grant> getConditionalGrants() {
    return conditionalGrants;
  }

  private List<BlockPos> bedPositions() {
    return bedLocs.stream().map(BlockPos::of).toList();
  }

  private List<WorkStation> workStations() {
    return workLocs.entrySet().stream()
        .map(entry -> new WorkStation(BlockPos.of(entry.getKey()), entry.getValue(),
            java.util.Optional.ofNullable(guardRoles.get(entry.getKey())))).toList();
  }

  private List<BlockPos> containerPositions() {
    return containerLocs.stream().map(BlockPos::of).toList();
  }

  private List<BlockPos> personalContainerPositions() {
    return personalContainerLocs.stream().map(BlockPos::of).toList();
  }

  private List<ItemCost> itemCosts() {
    return materialCost.stream().map(stack -> new ItemCost(stack.getItem(), stack.getCount())).toList();
  }

}
