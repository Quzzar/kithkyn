package com.quzzar.kithkyn.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * A layer of gear that is only worn while awake.
 *
 * <p>Everything a person wears or holds is gear: the armor, the item in each hand, the
 * block or skull on the head, the elytra. A sleeper has none of it on; they went to bed.
 * The wrapped layer renders exactly as vanilla would while the entity is awake and is
 * skipped outright while it sleeps, so a bed shows the person and not their kit.
 */
public final class AwakeOnlyLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    private final RenderLayer<T, M> gear;

    public AwakeOnlyLayer(RenderLayerParent<T, M> renderer, RenderLayer<T, M> gear) {
        super(renderer);
        this.gear = gear;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T entity,
            float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
            float headPitch) {
        if (entity.isSleeping()) {
            return;
        }
        this.gear.render(poseStack, bufferSource, packedLight, entity, limbSwing, limbSwingAmount, partialTick,
                ageInTicks, netHeadYaw, headPitch);
    }
}
