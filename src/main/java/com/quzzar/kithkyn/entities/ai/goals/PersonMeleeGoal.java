package com.quzzar.kithkyn.entities.ai.goals;

import com.quzzar.kithkyn.entities.RealPerson;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.item.CrossbowItem;

public class PersonMeleeGoal extends MeleeAttackGoal {
    public final RealPerson guard;
    private int fixedAttackCooldown;

    public PersonMeleeGoal(RealPerson guard, double speedIn, boolean useLongMemory) {
        super(guard, speedIn, useLongMemory);
        this.guard = guard;
    }

    @Override
    public boolean canUse() {
        if (this.guard.isFixedRangedGuard()) {
            return canDefendPost();
        }
        return !(this.guard.getMainHandItem().getItem() instanceof CrossbowItem) && this.guard.getTarget() != null
                && !this.guard.isEating() && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        if (this.guard.isFixedRangedGuard()) {
            return canDefendPost();
        }
        return super.canContinueToUse() && this.guard.getTarget() != null
                && !(this.guard.getMainHandItem().getItem() instanceof CrossbowItem);
    }

    @Override
    public void tick() {
        LivingEntity target = guard.getTarget();
        if (this.guard.isFixedRangedGuard()) {
            // A sidearm protects the platform, not permission to chase a
            // ground-level enemy off it. No movement or strafing here.
            this.guard.getNavigation().stop();
            if (fixedAttackCooldown > 0) {
                fixedAttackCooldown--;
            }
            if (target != null) {
                guard.getLookControl().setLookAt(target, 30.0F, 30.0F);
                if (fixedAttackCooldown == 0 && guard.hasLineOfSight(target)
                        && this.mob.distanceToSqr(target) <= this.getAttackReachSqr(target)) {
                    fixedAttackCooldown = 20;
                    guard.stopUsingItem();
                    guard.swing(InteractionHand.MAIN_HAND);
                    guard.doHurtTarget(target);
                }
            }
            return;
        }
        if (target != null) {
            if (target.distanceTo(guard) <= 3.0D && !guard.isBlocking()) {
                guard.getMoveControl().strafe(-2.0F, 0.0F);
                guard.lookAt(target, 30.0F, 30.0F);
            }
            if (this.mob.getNavigation().getPath() != null && target.distanceTo(guard) <= 2.0D)
                guard.getNavigation().stop();
            super.tick();
        }
    }

    @Override
    public void start() {
        if (guard.isFixedRangedGuard()) {
            guard.getNavigation().stop();
            guard.setAggressive(true);
        } else {
            super.start();
        }
    }

    private boolean canDefendPost() {
        LivingEntity target = guard.getTarget();
        return target != null && target.isAlive() && !guard.isEating()
                && !(guard.getMainHandItem().getItem() instanceof CrossbowItem)
                && guard.distanceToSqr(target) <= 16.0D && guard.hasLineOfSight(target);
    }

    protected double getAttackReachSqr(LivingEntity attackTarget) {
        return (this.mob.getBbWidth() * 2.0F * this.mob.getBbWidth() * 2.0F + attackTarget.getBbWidth()) * 3.55D;
    }

    @Override
    protected boolean canPerformAttack(LivingEntity entity) {
        return this.isTimeToAttack() && this.mob.distanceToSqr(entity) <= this.getAttackReachSqr(entity);
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity enemy) {
        if (this.canPerformAttack(enemy)) {
            this.resetAttackCooldown();
            this.guard.stopUsingItem();
            //if (guard.shieldCoolDown == 0)
                //this.guard.shieldCoolDown = 8;
            this.guard.swing(InteractionHand.MAIN_HAND);
            this.guard.doHurtTarget(enemy);
        }
    }
}
