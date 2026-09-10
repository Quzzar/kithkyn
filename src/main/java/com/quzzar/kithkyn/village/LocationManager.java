package com.quzzar.kithkyn.village;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.village.buildings.Building;
import com.quzzar.kithkyn.village.buildings.BuildingUpgrade;
import com.quzzar.kithkyn.village.buildings.WallPost;
import com.quzzar.kithkyn.village.buildings.WorkerFooting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;

public class LocationManager {

    private record ResolvedWorkplace(Building building, BlockPos station) {}

    @Nullable
    private static ResolvedWorkplace resolveWorkplace(Village village, JobAssignment job) {
        Building owner = village.getBuilding(job.getBuildingUUID());
        if(owner == null || owner.getInfo() == null || village.isBeingRebuilt(owner.getUUID())){ return null; }

        int entryIndex = 0;
        for(Entry<Long, Occupation> entry : owner.getInfo().getWorkLocations().entrySet()) {
            if(entry.getValue() == job.getOccupation() && entryIndex == job.getStationIndex()) {
                Building physical = owner;
                long physicalStation = entry.getKey();
                String category = owner.getInfo().getWorksiteCategory(entry.getKey());
                if(category != null) {
                    physical = village.getBuildings().stream()
                        .filter(candidate -> candidate.getInfo() != null)
                        .filter(candidate -> category.equals(candidate.getInfo().getCategory()))
                        .filter(candidate -> !village.isBeingRebuilt(candidate.getUUID()))
                        .min(java.util.Comparator
                            .comparingDouble((Building candidate) -> BlockPos.of(candidate.getCenterLocation())
                                .distSqr(BlockPos.of(owner.getCenterLocation())))
                            .thenComparing(candidate -> candidate.getUUID().toString()))
                        .orElse(null);
                    if(physical == null || physical.getInfo() == null) { return null; }
                    physicalStation = physical.getInfo().getWorksiteLocations().entrySet().stream()
                        .filter(worksite -> worksite.getValue() == job.getOccupation())
                        .map(Entry::getKey)
                        .findFirst().orElse(Long.MIN_VALUE);
                    if(physicalStation == Long.MIN_VALUE) { return null; }
                }
                BlockPos station = BlockPos.of(physical.getOriginLocation())
                    .offset(BlockPos.of(physicalStation).rotate(physical.getRotation()));
                return new ResolvedWorkplace(physical, station);
            }
            entryIndex++;
        }
        return null;
    }
    
    public static BlockPos getJobLocation(RealPerson person){

        Village village = person.getVillage();
        if(village == null){ return BlockPos.ZERO; }

        JobAssignment job = village.getJobAssignment(person.getUUID());
        if(job == null){ return BlockPos.ZERO; }

        WallPost wallPost = village.getWallPost(job);
        if(wallPost != null){ return wallPost.position(); }

        ResolvedWorkplace workplace = resolveWorkplace(village, job);
        if(workplace != null){ return workplace.station(); }
        Kithkyn.LOGGER.debug("Couldn't find job index");
        return BlockPos.ZERO;

    }

    public static BlockPos getBedLocation(RealPerson person){

        Village village = person.getVillage();
        if(village == null){ return BlockPos.ZERO; }

        BedAssignment bed = village.getBedAssignment(person.getUUID());
        if(bed == null){ return BlockPos.ZERO; }

        Building building = village.getBuilding(bed.getBuildingUUID());
        if(building == null){ return BlockPos.ZERO; }
        // Same for a bed: the house is a building site until the upgrade finishes.
        if(village.isBeingRebuilt(bed.getBuildingUUID())){ return BlockPos.ZERO; }

        int foundCount = 0;
        for(long longloc : building.getInfo().getBedLocations()) {
            if(foundCount == bed.getBedIndex()){
                return BlockPos.of(building.getOriginLocation()).offset(BlockPos.of(longloc).rotate(building.getRotation()));
            }
            foundCount++;
        }
        Kithkyn.LOGGER.debug("Couldn't find bed index");
        return BlockPos.ZERO;

    }

    /**
     * Where this person rests at night. Adults use their own bed; dependents
     * return to the center of a resident parent's home without claiming or
     * occupying either parent's bed.
     */
    public static BlockPos getNightRestLocation(RealPerson person) {
        BlockPos bed = getBedLocation(person);
        if (!bed.equals(BlockPos.ZERO)) {
            return bed;
        }
        Village village = person.getVillage();
        if (village == null) {
            return BlockPos.ZERO;
        }
        Building home = village.dependentHome(person);
        return home == null ? BlockPos.ZERO : BlockPos.of(home.getCenterLocation());
    }

    /** A reachable dry gathering spot near the rung bell, including ground below an elevated bell. */
    @Nullable
    public static BlockPos getBellApproach(RealPerson person, BlockPos bell) {
        java.util.Set<BlockPos> candidates = new java.util.HashSet<>();
        for (BlockPos pos : BlockPos.betweenClosed(bell.offset(-4, -8, -4), bell.offset(4, 2, 4))) {
            if (!pos.equals(bell) && WorkerFooting.canStand(person, pos)) candidates.add(pos.immutable());
        }
        if (candidates.isEmpty()) return null;
        // Set-based search checks the actual destinations, not a partial route or mine waypoint.
        var path = person.getNavigation().createPath(candidates, 0);
        if (path == null || !path.canReach() || path.getEndNode() == null) return null;
        BlockPos end = path.getEndNode().asBlockPos();
        return candidates.contains(end) ? end : null;
    }

    public static BlockPos getVillageCenter(RealPerson person){

        Village village = person.getVillage();
        if(village == null){ return BlockPos.ZERO; }
        return village.getGatheringPoint();

    }

    @Nullable
    public static Building getJobBuilding(RealPerson person){

        Village village = person.getVillage();
        if(village == null){ return null; }

        JobAssignment job = village.getJobAssignment(person.getUUID());
        if(job == null){ return null; }
        
        ResolvedWorkplace workplace = resolveWorkplace(village, job);
        return workplace == null ? null : workplace.building();

    }

    /**
     * The assigned workplace's exact rotated template footprint in world space.
     * Null means there is no safe workplace area to mutate, including while the
     * building is being rebuilt.
     */
    @Nullable
    public static WorkArea getJobWorkArea(RealPerson person){

        Village village = person.getVillage();
        if(village == null){ return null; }

        JobAssignment job = village.getJobAssignment(person.getUUID());
        if(job == null){ return null; }

        ResolvedWorkplace workplace = resolveWorkplace(village, job);
        if(workplace == null){ return null; }
        Building building = workplace.building();

        if(!(person.level() instanceof ServerLevel level)){ return null; }
        BoundingBox local = BuildingUpgrade.footprintOf(level, building);
        if(local == null){ return null; }
        BlockPos origin = BlockPos.of(building.getOriginLocation());
        return new WorkArea(
            local.minX() + origin.getX(), local.minY() + origin.getY(), local.minZ() + origin.getZ(),
            local.maxX() + origin.getX(), local.maxY() + origin.getY(), local.maxZ() + origin.getZ());

    }

    /**
     * The container positions of the worker's own workplace, in world space, in
     * the order the structure declares them. Empty when the worker has no job
     * building or it declares none; a caller that can settle for any village
     * container falls back to {@link #getNearestContainerPos} itself.
     */
    public static List<BlockPos> getJobContainerPositions(RealPerson person){

        Building building = getJobBuilding(person);
        if(building == null || building.getInfo() == null){ return List.of(); }

        ArrayList<BlockPos> positions = new ArrayList<>();
        BlockPos origin = BlockPos.of(building.getOriginLocation());
        for(Long local : building.getInfo().getContainerLocations()) {
            positions.add(origin.offset(BlockPos.of(local).rotate(building.getRotation())));
        }
        return positions;

    }

    @Nullable
    /** Where the nearest village container is, for a worker who needs to walk to it. */
    public static BlockPos getNearestContainerPos(RealPerson person){

        Village village = person.getVillage();
        if(village == null){ return null; }

        BlockPos location = village.getNearestContainer(BlockPos.containing(person.getEyePosition()));
        return location == BlockPos.ZERO ? null : location;

    }

    public static Container getNearestContainer(RealPerson person){

        Village village = person.getVillage();
        if(village == null){ return null; }

        BlockPos location = village.getNearestContainer(BlockPos.containing(person.getEyePosition()));
        if(location == BlockPos.ZERO){ return null; }

        BlockEntity entity = person.level().getBlockEntity(location);
        if(entity instanceof Container){
            return (Container) entity;
        }

        Kithkyn.LOGGER.debug(location.toShortString());

        Kithkyn.LOGGER.debug("No container at location");
        return null;

    }

    /** A building's way in: the cell outside its lowest door nearest the authored front, and the box the building stands in. */
    public record Entrance(BlockPos doorstep, BoundingBox bounds) {
        public boolean contains(BlockPos pos) {
            return bounds.isInside(pos);
        }
    }

    /**
     * Where to walk to get into a building: the cell just outside its lowest
     * door nearest the authored front. A path aimed straight at something indoors stalls against the
     * nearest outside wall when the door is on the far side, because the
     * pathfinder's budget runs out on the open ground before it finds the way
     * round (the level-3 house at Wildflower Downs, whose door faced away from
     * the village: everyone stopped under the upstairs chest, outside). From
     * the doorstep the rest is a dozen nodes. Doorless buildings use a clear,
     * supported opening on their rotated authored front. Null when no such
     * opening exists, the footprint is unknown, or chunks are not resident.
     * This never pages a chunk in.
     */
    @Nullable
    public static Entrance getEntrance(ServerLevel level, Building building){

        BoundingBox local = BuildingUpgrade.footprintOf(level, building);
        if(local == null){ return null; }
        BlockPos origin = BlockPos.of(building.getOriginLocation());
        BoundingBox bounds = local.moved(origin.getX(), origin.getY(), origin.getZ());
        for(BlockPos corner : List.of(
                new BlockPos(bounds.minX(), bounds.minY(), bounds.minZ()),
                new BlockPos(bounds.maxX(), bounds.minY(), bounds.minZ()),
                new BlockPos(bounds.minX(), bounds.minY(), bounds.maxZ()),
                new BlockPos(bounds.maxX(), bounds.minY(), bounds.maxZ()))) {
            if(!level.hasChunkAt(corner)){ return null; }
        }

        Direction approachFront = building.getInfo() == null ? Direction.NORTH
                : building.getRotation().rotate(building.getInfo().getEntranceFacing());
        BlockPos door = null;
        for(BlockPos pos : BlockPos.betweenClosed(bounds.minX(), bounds.minY(), bounds.minZ(),
                bounds.maxX(), bounds.maxY(), bounds.maxZ())) {
            BlockState state = level.getBlockState(pos);
            if(state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
                    && (door == null || pos.getY() < door.getY()
                    || (pos.getY() == door.getY()
                        && frontCoordinate(pos, approachFront) > frontCoordinate(door, approachFront)))){
                door = pos.immutable();
            }
        }
        if(door == null){
            if(building.getInfo() == null) return null;
            Direction front = building.getRotation().rotate(building.getInfo().getEntranceFacing());
            int floor = origin.getY() + building.getPlacedSink();
            BlockPos middle = openFront(bounds, front, floor + 1);
            Direction across = front.getClockWise();
            int half = (front.getAxis() == Direction.Axis.X ? bounds.getZSpan() : bounds.getXSpan()) / 2;
            for(int offset = 0; offset <= half; offset++) {
                for(int sign : offset == 0 ? new int[]{1} : new int[]{1, -1}) {
                    for(int dy : new int[]{0, 1, -1, 2, -2}) {
                        BlockPos outside = middle.relative(across, offset * sign).above(dy);
                        BlockPos inside = outside.relative(front.getOpposite());
                        if(!level.hasChunkAt(outside) || !level.hasChunkAt(inside)) continue;
                        BlockPos support = outside.below();
                        if(!level.getFluidState(support).isEmpty()
                                || !level.getBlockState(support).isFaceSturdy(level, support, Direction.UP)) continue;
                        boolean open = true;
                        for(BlockPos feet : List.of(outside, inside)) {
                            for(int head = 0; head < 3; head++) {
                                BlockPos body = feet.above(head);
                                if(!level.getFluidState(body).isEmpty()
                                        || !level.getBlockState(body).getCollisionShape(level, body).isEmpty()) open = false;
                            }
                        }
                        if(open) return new Entrance(outside, bounds);
                    }
                }
            }
            return null;
        }

        // A door sits in a wall; of its two neighbours the one farther from the
        // building's middle is the outside.
        BlockPos centre = bounds.getCenter();
        Direction facing = level.getBlockState(door).getValue(DoorBlock.FACING);
        BlockPos front = door.relative(facing);
        BlockPos back = door.relative(facing.getOpposite());
        return new Entrance(front.distSqr(centre) >= back.distSqr(centre) ? front : back, bounds);

    }

    /** Equal-height interior doors must not replace the public entrance after a rotation. */
    private static int frontCoordinate(BlockPos pos, Direction front) {
        return pos.getX() * front.getStepX() + pos.getZ() * front.getStepZ();
    }

    /** World-space front of a rotated, doorless footprint. */
    public static BlockPos openFront(BoundingBox bounds, Direction front, int feetY) {
        int x = (bounds.minX() + bounds.maxX()) / 2;
        int z = (bounds.minZ() + bounds.maxZ()) / 2;
        return switch(front) {
            case EAST -> new BlockPos(bounds.maxX() + 1, feetY, z);
            case WEST -> new BlockPos(bounds.minX() - 1, feetY, z);
            case SOUTH -> new BlockPos(x, feetY, bounds.maxZ() + 1);
            case NORTH -> new BlockPos(x, feetY, bounds.minZ() - 1);
            default -> throw new IllegalArgumentException("Entrance fronts must be horizontal");
        };
    }

}
