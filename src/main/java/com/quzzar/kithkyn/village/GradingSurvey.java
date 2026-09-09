package com.quzzar.kithkyn.village;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.savedata.GradedColumnStore;
import com.quzzar.kithkyn.savedata.PlacedBlockStore;
import com.quzzar.kithkyn.village.buildings.Building;
import com.quzzar.kithkyn.village.buildings.BuildingUpgrade;
import com.quzzar.kithkyn.village.buildings.SitePreparation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * One reading of the ground around a village's buildings, and the walkable
 * surface each column is being graded toward (docs/worker-loops.md, "The
 * builder builds, and between builds it makes the village walkable").
 *
 * The rule the survey encodes is walkability: small deep openings are covered,
 * then between two neighbouring columns the ground should never step more than
 * one block, which is the step a villager climbs without jumping. Everything
 * else follows from choosing each column's target with care:
 *
 * <ul>
 * <li><b>Each column is read once.</b> A column of soft ground (the
 * {@code kithkyn:gradeable} tag: dirt, grass, sand, gravel, clay) can be
 * moved. Ground the village has claimed for a building stands at that
 * building's own plane and is fixed, so graded ground meets a building's
 * floor rather than stopping a step short of it. Natural rock (the
 * {@code kithkyn:firm_ground} tag: stone, terracotta, sandstone, ice) is
 * fixed too: the builder has no tool for it, and it is the shape of the land.
 * Everything else is not ground at all and takes no part: water, trees,
 * anything the village or a player built, and any block the two tags do not
 * name. That last rule was learned live. Rock used to be "whatever is left",
 * so a mangrove's stilt roots read as a rock ledge four blocks up and the
 * builder set about raising the whole swamp toward the trees. The edge of the
 * area is ground like any other; what lies beyond it is not read, so a step
 * may remain where the village's ground meets the world's.</li>
 * <li><b>A cliff is a feature.</b> Neighbours that differ by more than
 * {@link #MAX_STEP} are not connected, so nothing ramps up to a cliff or
 * chews its crown down.</li>
 * <li><b>The target comes from the original surface.</b> Every column's target
 * sits halfway between the steepest one-step surface that fits under the
 * original ground and the one that fits over it, with half-block ties resolved
 * upward. A partial pass therefore cannot make its own next target drift, and
 * ordinary smoothing prefers fill over another cut. The target is clamped to
 * what the fixed columns allow where those anchors agree.</li>
 * </ul>
 *
 * The current surface still determines what work remains. The reference kept
 * in {@code GradedColumnStore} determines the destination, so a goal released
 * for night or conversation can safely re-read without ratcheting a cut farther
 * downward. The same reference makes concurrent builders agree on the target.
 */
public final class GradingSurvey {

  public static final TagKey<Block> GRADEABLE = TagKey.create(Registries.BLOCK,
      ResourceLocation.fromNamespaceAndPath(Kithkyn.MODID, "gradeable"));

  /** Natural ground the builder cannot move but grades up to: the shape of the land. */
  public static final TagKey<Block> FIRM_GROUND = TagKey.create(Registries.BLOCK,
      ResourceLocation.fromNamespaceAndPath(Kithkyn.MODID, "firm_ground"));

  /** How far past the buildings' own ground the village grades. */
  public static final int MARGIN = 8;

  /** The widest area one survey reads, so a sprawling village's scan stays bounded. */
  public static final int MAX_SPAN = 96;

  /**
   * A step taller than this between neighbours is a cliff: left alone, and
   * never ramped to. One more than the levelling budget, so a fixed column can
   * always be met within it.
   */
  public static final int MAX_STEP = SitePreparation.MAX_COLUMN_DELTA + 1;

  /**
   * Movable ground within this many blocks of a building's footprint: the
   * building's apron. Apron columns are graded to a walkable ramp down from the
   * building's own floor even where the land between would otherwise be left a
   * cliff or held within the levelling budget, so a villager can always step up
   * onto the building and in at its door (docs/worker-loops.md, tight apron
   * decided 2026-09-03). Kept small on purpose: the wider ground still keeps its
   * hills, and the graded path to the door carries the rest of the way in.
   */
  public static final int APRON = 3;

  /** Most clearable cover the reading skips on its way down to the ground under it. */
  private static final int COVER_DEPTH = 4;

  private static final int NO_GROUND = HoleCoverPlanner.NO_SURFACE;

  /**
   * A column the survey would move: its ground height now (the Y of its top
   * block) and the height it should stand at.
   */
  public record Column(int x, int z, int height, int target, boolean apron, boolean cover) {

    public boolean wantsCut() {
      return !cover && target < height;
    }
  }

  private final int minX;
  private final int minZ;
  private final int width;
  private final int depth;
  /** The ground that exists now and therefore still needs physical work. */
  private final int[] height;
  /** The ground before grading first touched each movable column. */
  private final int[] referenceHeight;
  private final boolean[] fixed;
  /** Columns a building has claimed for its footprint, standing at its floor plane. */
  private final boolean[] claimed;
  /** Movable ground within {@link #APRON} of a footprint: ramped to the floor even across a cliff or past the budget. */
  private final boolean[] apron;
  /** Columns whose top is a worn path, given a gentler grade than the rest. */
  private final boolean[] path;
  /** Narrow shared gaps and doorstep approaches, worked before the wider landscape. */
  private final boolean[] access;
  /** Original cave-floor height for a column whose opening will be covered. */
  private final int[] coverFrom;
  private final int[] target;

  private GradingSurvey(int minX, int minZ, int width, int depth) {
    this.minX = minX;
    this.minZ = minZ;
    this.width = width;
    this.depth = depth;
    this.height = new int[width * depth];
    this.referenceHeight = new int[width * depth];
    this.fixed = new boolean[width * depth];
    this.claimed = new boolean[width * depth];
    this.apron = new boolean[width * depth];
    this.path = new boolean[width * depth];
    this.access = new boolean[width * depth];
    this.coverFrom = new int[width * depth];
    Arrays.fill(this.coverFrom, HoleCoverPlanner.NO_COVER);
    this.target = new int[width * depth];
  }

  /**
   * Reads the ground around every building of the village, out to
   * {@link #MARGIN} past their footprints and clamped to {@link #MAX_SPAN}
   * about the town centre. Null for a village with nothing built.
   */
  @Nullable
  public static GradingSurvey of(ServerLevel level, Village village) {
    Collection<Building> buildings = village.getBuildings();
    if (buildings.isEmpty()) {
      return null;
    }
    int minX = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE;
    int minZ = Integer.MAX_VALUE;
    int maxZ = Integer.MIN_VALUE;
    for (Building building : buildings) {
      BlockPos centre = BlockPos.of(building.getCenterLocation());
      int radius = (int) Math.ceil(building.getRadius());
      minX = Math.min(minX, centre.getX() - radius);
      maxX = Math.max(maxX, centre.getX() + radius);
      minZ = Math.min(minZ, centre.getZ() - radius);
      maxZ = Math.max(maxZ, centre.getZ() + radius);
    }
    for (VillagePaths.Gate gate : VillagePaths.gates(village.getWallProject())) {
      minX = Math.min(minX, gate.anchor().getX());
      maxX = Math.max(maxX, gate.anchor().getX());
      minZ = Math.min(minZ, gate.anchor().getZ());
      maxZ = Math.max(maxZ, gate.anchor().getZ());
    }
    minX -= MARGIN;
    maxX += MARGIN;
    minZ -= MARGIN;
    maxZ += MARGIN;

    Building townCenter = village.getTownCenter();
    int midX = townCenter != null ? BlockPos.of(townCenter.getCenterLocation()).getX() : (minX + maxX) / 2;
    int midZ = townCenter != null ? BlockPos.of(townCenter.getCenterLocation()).getZ() : (minZ + maxZ) / 2;
    if (maxX - minX + 1 > MAX_SPAN) {
      minX = midX - MAX_SPAN / 2;
      maxX = minX + MAX_SPAN - 1;
    }
    if (maxZ - minZ + 1 > MAX_SPAN) {
      minZ = midZ - MAX_SPAN / 2;
      maxZ = minZ + MAX_SPAN - 1;
    }

    GradingSurvey survey = new GradingSurvey(minX, minZ, maxX - minX + 1, maxZ - minZ + 1);
    List<BuildingGround> grounds = new ArrayList<>();
    for (Building building : buildings) {
      BoundingBox local = BuildingUpgrade.footprintOf(level, building);
      if (local == null) continue;
      BlockPos origin = BlockPos.of(building.getOriginLocation());
      grounds.add(new BuildingGround(local.moved(origin.getX(), origin.getY(), origin.getZ()),
          origin.getY() + building.getPlacedSink()));
    }
    survey.read(level, village, grounds);
    survey.planHoleCovers();
    survey.markApron();
    survey.markAccess(level, village, grounds);
    survey.aim();
    return survey;
  }

  /**
   * Marks every movable column within {@link #APRON} of a building's footprint,
   * so the aim ramps it to the building's floor even where the raw land would
   * leave a step. Claimed footprint columns and rock are never apron: only soft
   * ground the builder can actually move.
   */
  private void markApron() {
    for (int i = 0; i < claimed.length; i++) {
      if (!claimed[i]) {
        continue;
      }
      int cx = i % width;
      int cz = i / width;
      for (int dz = -APRON; dz <= APRON; dz++) {
        for (int dx = -APRON; dx <= APRON; dx++) {
          int x = cx + dx;
          int z = cz + dz;
          if (x < 0 || z < 0 || x >= width || z >= depth) {
            continue;
          }
          int j = z * width + x;
          if (height[j] != NO_GROUND && !fixed[j]) {
            apron[j] = true;
          }
        }
      }
    }
  }

  /** The columns standing off their target, nearest to {@code from} first. */
  public List<Column> uneven(BlockPos from) {
    List<Column> out = new ArrayList<>();
    for (int i = 0; i < height.length; i++) {
      if (coverFrom[i] != HoleCoverPlanner.NO_COVER) {
        out.add(new Column(minX + i % width, minZ + i / width, coverFrom[i], height[i], false, true));
      } else if (height[i] != NO_GROUND && !fixed[i] && target[i] != height[i]) {
        out.add(new Column(minX + i % width, minZ + i / width, height[i], target[i], apron[i], false));
      }
    }
    out.sort(Comparator.comparingLong(column -> {
      long dx = column.x() - from.getX();
      long dz = column.z() - from.getZ();
      return dx * dx + dz * dz;
    }));
    return out;
  }

  /** The same grading plan, restricted to the small connections needed first. */
  public List<Column> accessWork(BlockPos from) {
    return uneven(from).stream().filter(column -> !column.cover()
        && access[(column.z() - minZ) * width + column.x() - minX]).toList();
  }

  private record BuildingGround(BoundingBox bounds, int floor) { }

  private void markAccess(ServerLevel level, Village village, List<BuildingGround> grounds) {
    Building hub = village.getTownCenter();
    LocationManager.Entrance hubEntrance = hub == null ? null : LocationManager.getEntrance(level, hub);
    if (hubEntrance != null) {
      BlockPos door = hubEntrance.doorstep();
      markAccess(new BoundingBox(door.getX() - 1, 0, door.getZ() - 1,
          door.getX() + 1, 0, door.getZ() + 1));
    }
    for (int a = 0; a < grounds.size(); a++) {
      for (int b = a + 1; b < grounds.size(); b++) {
        BoundingBox lane = sharedGap(grounds.get(a).bounds(), grounds.get(b).bounds());
        if (lane != null) markAccess(lane);
      }
    }
    for (VillagePaths.Destination destination : VillagePaths.destinations(level, village)) {
      if (!destination.initialEligible() || destination.target() == null) continue;
      BlockPos door = destination.target();
      // Only movable columns survive this mask; the building itself stays fixed.
      markAccess(new BoundingBox(door.getX() - 1, 0, door.getZ() - 1,
          door.getX() + 1, 0, door.getZ() + 1));
    }
  }

  private void markAccess(BoundingBox lane) {
    for (int z = Math.max(minZ, lane.minZ()); z <= Math.min(minZ + depth - 1, lane.maxZ()); z++) {
      for (int x = Math.max(minX, lane.minX()); x <= Math.min(minX + width - 1, lane.maxX()); x++) {
        int i = (z - minZ) * width + x - minX;
        if (height[i] != NO_GROUND && !fixed[i]) access[i] = path[i] = true;
      }
    }
  }

  /** One to three unclaimed columns between overlapping facing edges, never a broad apron. */
  @Nullable
  static BoundingBox sharedGap(BoundingBox a, BoundingBox b) {
    if (a.minX() > b.maxX()) return sharedGap(b, a);
    int gapX = b.minX() - a.maxX() - 1;
    int minZ = Math.max(a.minZ(), b.minZ()), maxZ = Math.min(a.maxZ(), b.maxZ());
    if (gapX >= 1 && gapX <= APRON && minZ <= maxZ)
      return new BoundingBox(a.maxX() + 1, 0, minZ, b.minX() - 1, 0, maxZ);
    if (a.minZ() > b.maxZ()) {
      BoundingBox swap = a; a = b; b = swap;
    }
    int gapZ = b.minZ() - a.maxZ() - 1;
    int minX = Math.max(a.minX(), b.minX()), maxX = Math.min(a.maxX(), b.maxX());
    return gapZ >= 1 && gapZ <= APRON && minX <= maxX
        ? new BoundingBox(minX, 0, a.maxZ() + 1, maxX, 0, b.minZ() - 1) : null;
  }

  /**
   * Reads every column: what stands on top once loose cover is looked past,
   * and whether the builder may move it, must respect it, or should ignore it.
   */
  private void read(ServerLevel level, Village village, List<BuildingGround> grounds) {
    PlacedBlockStore placed = PlacedBlockStore.get(level);
    GradedColumnStore graded = GradedColumnStore.get(level);
    BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    for (int z = 0; z < depth; z++) {
      for (int x = 0; x < width; x++) {
        int i = z * width + x;
        int worldX = minX + x;
        int worldZ = minZ + z;
        cursor.set(worldX, 0, worldZ);
        if (!level.isLoaded(cursor)) {
          height[i] = NO_GROUND;
          referenceHeight[i] = NO_GROUND;
          continue;
        }
        boolean isClaimed = village.hasClaimed(cursor);
        fixed[i] = isClaimed;
        claimed[i] = isClaimed;
        if (isClaimed) {
          // A building's ground is its plane, whatever the roof above it reads.
          BuildingGround owner = buildingOver(grounds, worldX, worldZ);
          if (owner != null) {
            // A sunk building's origin is below its ground; the plane is what
            // the surrounding ground is graded to.
            height[i] = owner.floor();
            referenceHeight[i] = height[i];
            continue;
          }
        }

        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, worldX, worldZ) - 1;
        BlockState top = level.getBlockState(cursor.set(worldX, y, worldZ));
        for (int skipped = 0; skipped < COVER_DEPTH && top.is(SitePreparation.CLEARABLE); skipped++) {
          top = level.getBlockState(cursor.set(worldX, --y, worldZ));
        }

        if (top.isAir() || top.is(SitePreparation.CLEARABLE) || !top.getFluidState().isEmpty()
            || placed.isVillagePlaced(cursor) || placed.isPlayerPlaced(cursor)) {
          height[i] = NO_GROUND; // void, a tall tree, water, or somebody's structure
          referenceHeight[i] = NO_GROUND;
        } else if (top.is(GRADEABLE)) {
          height[i] = y;
          referenceHeight[i] = graded.originalHeight(worldX, worldZ, y);
          path[i] = top.is(Blocks.DIRT_PATH);
        } else if (top.is(FIRM_GROUND)) {
          height[i] = y; // rock: ground, but the shape of the land
          referenceHeight[i] = y;
          fixed[i] = true;
        } else {
          height[i] = NO_GROUND; // a stilt root, a fence, a mushroom stem: not ground
          referenceHeight[i] = NO_GROUND;
        }
      }
    }
  }

  /**
   * Replaces a small deep opening with the surface that would cover its
   * mouth before the grading envelopes are calculated. The real floor height is
   * retained in {@link #coverFrom}, so the worker receives one cover operation
   * at the mouth rather than a stack of fill operations from the cave floor up.
   * The planned cap is fixed for this reading. Once physically covered, the
   * next survey sees ordinary ground there and may grade it normally.
   */
  private void planHoleCovers() {
    boolean[] coverable = new boolean[height.length];
    for (int i = 0; i < height.length; i++) {
      coverable[i] = height[i] != NO_GROUND && !claimed[i];
    }
    int[] covers = HoleCoverPlanner.plan(width, depth, height, coverable);
    for (int i = 0; i < height.length; i++) {
      if (covers[i] == HoleCoverPlanner.NO_COVER) {
        continue;
      }
      coverFrom[i] = height[i];
      height[i] = covers[i];
      referenceHeight[i] = covers[i];
      fixed[i] = true;
    }
  }

  /** Use the actual rotated footprint, the same geometry that owns the ground. */
  @Nullable
  private static BuildingGround buildingOver(List<BuildingGround> grounds, int x, int z) {
    for (BuildingGround ground : grounds) {
      BoundingBox box = ground.bounds();
      if (x >= box.minX() && x <= box.maxX() && z >= box.minZ() && z <= box.maxZ()) return ground;
    }
    return null;
  }

  /** Chooses the stable, fill-biased target for every movable column. */
  private void aim() {
    int[] planned = GradingTargets.plan(width, depth, height, referenceHeight, fixed, apron, path);
    System.arraycopy(planned, 0, target, 0, target.length);
  }

}
