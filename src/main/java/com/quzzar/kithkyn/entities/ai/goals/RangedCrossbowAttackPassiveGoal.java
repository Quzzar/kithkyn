package com.quzzar.kithkyn.entities.ai.goals;

import java.util.EnumSet;

import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.Utils;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;

public class RangedCrossbowAttackPassiveGoal<T extends PathfinderMob & RangedAttackMob & CrossbowAttackMob> extends Goal {
    private final T entity;
    private RangedCrossbowAttackPassiveGoal.CrossbowState crossbowState = RangedCrossbowAttackPassiveGoal.CrossbowState.UNCHARGED;
    private final double speed;
    private final float distanceMoveToEntity;
    private int seeTicks;
    private int timeUntilStrike;

    public RangedCrossbowAttackPassiveGoal(T entity, double p_i50322_2_, float p_i50322_4_) {
        this.entity = entity;
        this.speed = p_i50322_2_;
        this.distanceMoveToEntity = p_i50322_4_ * p_i50322_4_;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.hasAttackTarget() && this.isHoldingCrossbow() && !((RealPerson) this.entity).isEating()
                && inRange();
    }

    private boolean inRange() {
        RealPerson person = (RealPerson) this.entity;
        return person.getOccupation() != com.quzzar.kithkyn.village.Occupation.GUARD
                || person.getTarget() != null && person.distanceToSqr(person.getTarget())
                <= Math.pow(GuardThreatGoal.range(person), 2);
    }

    private boolean isHoldingCrossbow() {
        return this.entity.getMainHandItem().getItem() instanceof CrossbowItem;
    }

    @Override
    public boolean canContinueToUse() {
        return this.hasAttackTarget() && (this.canUse() || !this.entity.getNavigation().isDone()) && this.isHoldingCrossbow() && inRange();
    }

    private boolean hasAttackTarget() {
        return this.entity.getTarget() != null && this.entity.getTarget().isAlive();
    }

    @Override
    public void start() {
        this.crossbowState = CrossbowItem.isCharged(this.entity.getMainHandItem())
                ? CrossbowState.READY_TO_ATTACK : CrossbowState.UNCHARGED;
        this.timeUntilStrike = 0;
    }

    @Override
    public void stop() {
        super.stop();
        this.entity.setAggressive(false);
        // The target belongs to target selection. Drawing a sword or pausing
        // to eat must not erase it before the other combat goal can take over.
        this.seeTicks = 0;
        this.timeUntilStrike = 0;
        this.crossbowState = CrossbowState.UNCHARGED;
        if (this.entity.getPose() == Pose.CROUCHING)
            this.entity.setPose(Pose.STANDING);
        if (this.entity.isUsingItem()) {
            this.entity.stopUsingItem();
            ((CrossbowAttackMob) this.entity).setChargingCrossbow(false);
        }
    }

    public boolean checkFriendlyFire() {
        LivingEntity target = this.entity.getTarget();
        return target != null && RangedShotSafety.blockedByFriendly(this.entity, target);
    }

    @Override
    public void tick() {
        LivingEntity livingentity = this.entity.getTarget();
        if (livingentity != null) {
            this.entity.setAggressive(true);
            boolean flag = this.entity.getSensing().hasLineOfSight(livingentity);
            boolean flag1 = this.seeTicks > 0;
            if (flag != flag1) {
                this.seeTicks = 0;
            }

            if (flag) {
                ++this.seeTicks;
            } else {
                --this.seeTicks;
            }

            boolean holdsPost = ((RealPerson) this.entity).isFixedRangedGuard();
            if (holdsPost && this.entity.getPose() == Pose.CROUCHING) this.entity.setPose(Pose.STANDING);
            if (!holdsPost && this.entity.getPose() == Pose.STANDING && this.entity.level().random.nextInt(4) == 0 && entity.tickCount % 50 == 0) {
                this.entity.setPose(Pose.CROUCHING);
            }

            if (this.entity.getPose() == Pose.CROUCHING && this.entity.level().random.nextInt(4) == 0 && entity.tickCount % 100 == 0) {
                this.entity.setPose(Pose.STANDING);
            }

            double d1 = livingentity.distanceTo(entity);
            if (d1 <= 2.0D && !holdsPost) {
                this.entity.getMoveControl().strafe(this.entity.isUsingItem() ?- 0.5F : -3.0F, 0.0F);
                this.entity.lookAt(livingentity, 30.0F, 30.0F);
            }

            double d0 = this.entity.distanceToSqr(livingentity);
            boolean flag2 = (d0 > (double) this.distanceMoveToEntity || this.seeTicks < 5) && this.timeUntilStrike == 0;
            if (flag2 && !holdsPost) {
                this.entity.getNavigation().moveTo(livingentity, this.isCrossbowUncharged() ? this.speed : this.speed * 0.5D);
            } else {
                this.entity.getNavigation().stop();
            }
            this.entity.lookAt(livingentity, 30.0F, 30.0F);
            this.entity.getLookControl().setLookAt(livingentity, 30.0F, 30.0F);
            if (this.crossbowState == RangedCrossbowAttackPassiveGoal.CrossbowState.UNCHARGED && !CrossbowItem.isCharged(entity.getUseItem()) && !entity.isBlocking()) {
                if (flag) {
                    this.entity.startUsingItem(Utils.getHandWith(entity, item -> item instanceof CrossbowItem));
                    this.crossbowState = RangedCrossbowAttackPassiveGoal.CrossbowState.CHARGING;
                    ((CrossbowAttackMob) this.entity).setChargingCrossbow(true);
                }
            } else if (this.crossbowState == RangedCrossbowAttackPassiveGoal.CrossbowState.CHARGING) {
                if (!this.entity.isUsingItem())
                    this.crossbowState = RangedCrossbowAttackPassiveGoal.CrossbowState.UNCHARGED;
                int i = this.entity.getTicksUsingItem();
                ItemStack itemstack = this.entity.getUseItem();
                if (i >= CrossbowItem.getChargeDuration(itemstack, this.entity) || CrossbowItem.isCharged(entity.getUseItem())) {
                    this.entity.releaseUsingItem();
                    this.crossbowState = RangedCrossbowAttackPassiveGoal.CrossbowState.CHARGED;
                    this.timeUntilStrike = 20 + this.entity.getRandom().nextInt(20);
                    ((CrossbowAttackMob) this.entity).setChargingCrossbow(false);
                }
            } else if (this.crossbowState == RangedCrossbowAttackPassiveGoal.CrossbowState.CHARGED) {
                --this.timeUntilStrike;
                if (this.timeUntilStrike == 0) {
                    this.crossbowState = RangedCrossbowAttackPassiveGoal.CrossbowState.READY_TO_ATTACK;
                }
            } else if (this.crossbowState == RangedCrossbowAttackPassiveGoal.CrossbowState.READY_TO_ATTACK && flag && !checkFriendlyFire() && !entity.isBlocking()) {
                ((RangedAttackMob) this.entity).performRangedAttack(livingentity, 1.0F);
                this.crossbowState = RangedCrossbowAttackPassiveGoal.CrossbowState.UNCHARGED;
            }
        }
    }

    private boolean isCrossbowUncharged() {
        return this.crossbowState == RangedCrossbowAttackPassiveGoal.CrossbowState.UNCHARGED;
    }

    static enum CrossbowState {
        UNCHARGED, CHARGING, CHARGED, READY_TO_ATTACK;
    }
}
