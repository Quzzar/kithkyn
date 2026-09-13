package com.quzzar.kithkyn.entities.ai.goals;

import java.util.EnumSet;

import com.quzzar.kithkyn.entities.RealPerson;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;

/**
 * Walks the person toward a village-directed travel target: an arriving
 * newcomer heading for the campfire, an emigrant heading for the village edge,
 * a worker walking to a job they have just been given, or an unhoused resident
 * gathering near a rung bell. Bell-origin walks yield to danger and expire;
 * ordinary village travel keeps its existing lifecycle.
 *
 * The goal ends itself on arrival. It has to: it holds the movement flag at the
 * highest priority any villager goal has, and the village only clears the
 * targets it set itself, so a commute set by anything else used to hold a
 * villager's legs for the rest of their life. A builder who walked to work
 * stood at the door of their workplace forever, holding a job they could never
 * start.
 */
public class VillageTravelGoal extends Goal {

    private static final double SPEED = 0.6D;

    /** Close enough to call it arrived: two blocks. */
    private static final double ARRIVED_DISTANCE_SQR = 4.0D;

    /** Ticks of a finished navigation before the walk is over, one way or another. */
    private static final int SETTLED_TICKS = 60;

    /** A failed route is reconsidered once a second, not once per server tick. */
    private static final int PATH_RETRY_TICKS = 20;

    private int settledTicks = 0;
    private int nextPathAttempt;

    private final RealPerson person;

    public VillageTravelGoal(RealPerson person) {
        this.person = person;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        person.cancelUnsafeBellRecall();
        return person.getTravelTarget() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        settledTicks = 0;
        nextPathAttempt = person.tickCount;
    }

    @Override
    public void tick() {
        BlockPos target = person.getTravelTarget();
        if (target == null) {
            return;
        }
        if (person.blockPosition().distSqr(target) <= ARRIVED_DISTANCE_SQR) {
            person.setTravelTarget(null);
            return;
        }
        // A finished navigation means either this is as close as the ground
        // allows — a workplace centre sits inside its own walls — or there is
        // no way there at all. Both are the end of the walk, and holding on
        // would mean holding the villager.
        if (person.getNavigation().isDone()) {
            if (++settledTicks >= SETTLED_TICKS) {
                person.setTravelTarget(null);
                return;
            }
            tryPath(target);
            return;
        }
        settledTicks = 0;
        if (person.tickCount >= nextPathAttempt) {
            tryPath(target);
        }
    }

    /** A travel target tolerates its two-block arrival radius, never a distant cave endpoint. */
    static boolean acceptsTravelEndpoint(BlockPos target, BlockPos endpoint) {
        return endpoint.distSqr(target) <= ARRIVED_DISTANCE_SQR;
    }

    private void tryPath(BlockPos target) {
        if (person.tickCount < nextPathAttempt) {
            return;
        }
        nextPathAttempt = person.tickCount + PATH_RETRY_TICKS;
        Path path = person.getNavigation().createPath(target, 0);
        if (path != null && path.getEndNode() != null
                && acceptsTravelEndpoint(target, path.getEndNode().asBlockPos())) {
            person.getNavigation().moveTo(path, SPEED);
        } else {
            person.getNavigation().stop();
        }
    }

    @Override
    public void stop() {
        person.getNavigation().stop();
    }
}
