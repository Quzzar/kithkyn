package com.quzzar.kithkyn.client.models;

import com.quzzar.kithkyn.entities.Person;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

public class PersonModel extends PlayerModel<Person> {

    /** How far a sleeper's head turns toward a shoulder: a good way over, well short of full profile. */
    private static final float RESTING_HEAD_TURN = (float) Math.toRadians(40.0);

    /** The small roll that goes with the turn, so the head lies to that side rather than merely looks. */
    private static final float RESTING_HEAD_TILT = (float) Math.toRadians(10.0);

    public PersonModel(ModelPart part, boolean slim) {
        super(part, slim);
    }
    
    @Override
    public void setupAnim(Person entityIn, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netbipedHeadYaw, float bipedHeadPitch) {
        // LivingEntityRenderer treats every pre-adult as a vanilla baby. Teenagers
        // remain pre-adults in the simulation but deliberately render with normal
        // adult proportions at their smaller AgeStage scale.
        this.young = entityIn.getLifeStage().usesYoungModel();
        super.setupAnim(entityIn, limbSwing, limbSwingAmount, ageInTicks, netbipedHeadYaw, bipedHeadPitch);
        if (entityIn.isSleeping()) {
            this.restHead(entityIn);
            return;
        }
        if (entityIn.getMainArm() == HumanoidArm.RIGHT) {
            this.eatingAnimationRightHand(InteractionHand.MAIN_HAND, entityIn, ageInTicks);
            this.eatingAnimationLeftHand(InteractionHand.OFF_HAND, entityIn, ageInTicks);
        } else {
            this.eatingAnimationRightHand(InteractionHand.OFF_HAND, entityIn, ageInTicks);
            this.eatingAnimationLeftHand(InteractionHand.MAIN_HAND, entityIn, ageInTicks);
        }
    }
    
    /**
     * A sleeper's head lies to one side. The vanilla pose keeps the head on the
     * entity's look direction, which has nothing to do with rest; this turns the
     * face a good way toward one shoulder and tips the crown toward that same
     * shoulder, on top of the shut eyes the sleeping skin bake provides. Which side
     * is the person's own, fixed by their appearance seed: a row of sleepers is not
     * a row of clones, and a head never flips between frames. The hat layer follows
     * the head as always.
     */
    private void restHead(Person entity) {
        float side = entity.getAppearanceSeed() % 2 == 0 ? 1.0F : -1.0F;
        this.head.xRot = 0.0F;
        this.head.yRot = side * RESTING_HEAD_TURN;
        // Model roll runs against yaw in sign: a negative roll leans the crown toward
        // the shoulder a positive yaw turns the face to (checked in the preview).
        this.head.zRot = -side * RESTING_HEAD_TILT;
        this.hat.copyFrom(this.head);
    }

    /** The wide (Steve) body: 4-pixel arms. */
    public static LayerDefinition createMesh() {
        MeshDefinition meshdefinition = PlayerModel.createMesh(CubeDeformation.NONE, false);
        return LayerDefinition.create(meshdefinition, 64, 64);
     }

    /** The slim (Alex) body: 3-pixel arms, on the same 64x64 skin sheet as the wide model. */
    public static LayerDefinition createSlimMesh() {
        MeshDefinition meshdefinition = PlayerModel.createMesh(CubeDeformation.NONE, true);
        return LayerDefinition.create(meshdefinition, 64, 64);
     }

    public void eatingAnimationRightHand(InteractionHand hand, Person entity, float ageInTicks) {
        ItemStack itemstack = entity.getItemInHand(hand);
        boolean drinkingoreating = itemstack.getUseAnimation() == UseAnim.EAT
                || itemstack.getUseAnimation() == UseAnim.DRINK;
        if (entity.isEating() && drinkingoreating
                || entity.getUseItemRemainingTicks() > 0 && drinkingoreating && entity.getUsedItemHand() == hand) {
            this.rightArm.yRot = -0.5F;
            this.rightArm.xRot = -1.3F;
            this.rightArm.zRot = Mth.cos(ageInTicks) * 0.1F;
            this.head.xRot = Mth.cos(ageInTicks) * 0.2F;
            this.head.yRot = 0.0F;
            this.hat.copyFrom(head);
        }
    }

    public void eatingAnimationLeftHand(InteractionHand hand, Person entity, float ageInTicks) {
        ItemStack itemstack = entity.getItemInHand(hand);
        boolean drinkingoreating = itemstack.getUseAnimation() == UseAnim.EAT
                || itemstack.getUseAnimation() == UseAnim.DRINK;
        if (entity.isEating() && drinkingoreating
                || entity.getUseItemRemainingTicks() > 0 && drinkingoreating && entity.getUsedItemHand() == hand) {
            this.leftArm.yRot = 0.5F;
            this.leftArm.xRot = -1.3F;
            this.leftArm.zRot = Mth.cos(ageInTicks) * 0.1F;
            this.head.xRot = Mth.cos(ageInTicks) * 0.2F;
            this.head.yRot = 0.0F;
            this.hat.copyFrom(head);
        }
    }
}
