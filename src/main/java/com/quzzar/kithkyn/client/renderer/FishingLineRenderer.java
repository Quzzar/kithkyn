package com.quzzar.kithkyn.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.quzzar.kithkyn.entities.FishingCast;
import com.quzzar.kithkyn.entities.RealPerson;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/** A fisher's server-owned cast, using the vanilla bobber sprite and fishing line. */
final class FishingLineRenderer {
  private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/fishing_hook.png");
  private static final int SEGMENTS = 16;

  private FishingLineRenderer() { }

  static void render(RealPerson person, float partialTick, PoseStack poses,
      MultiBufferSource buffers, int light, Quaternionf camera) {
    var target = person.fishingTarget();
    if (target.isEmpty() || person.isInvisible()
        || (!person.getMainHandItem().is(Items.FISHING_ROD) && !person.getOffhandItem().is(Items.FISHING_ROD))) {
      return;
    }
    Vec3 origin = person.getPosition(partialTick);
    Vec3 rod = rodTip(person, partialTick);
    float age = person.level().getGameTime() - person.fishingStarted() + partialTick;
    Vec3 bobber = FishingCast.bobber(rod, target.get(), age);
    Vec3 localBobber = bobber.subtract(origin);
    poses.pushPose();
    poses.translate(localBobber.x, localBobber.y, localBobber.z);
    poses.mulPose(camera);
    poses.scale(0.5F, 0.5F, 0.5F);
    VertexConsumer sprite = buffers.getBuffer(RenderType.entityCutout(TEXTURE));
    vertex(sprite, poses.last(), light, 0, 0);
    vertex(sprite, poses.last(), light, 1, 0);
    vertex(sprite, poses.last(), light, 1, 1);
    vertex(sprite, poses.last(), light, 0, 1);
    poses.popPose();

    VertexConsumer line = buffers.getBuffer(RenderType.lineStrip());
    Vec3 localRod = rod.subtract(origin);
    for (int i = 0; i <= SEGMENTS; i++) {
      double fraction = (double) i / SEGMENTS;
      Vec3 point = linePoint(localRod, localBobber, fraction);
      Vec3 tangent = linePoint(localRod, localBobber, fraction + 1.0D / SEGMENTS).subtract(point).normalize();
      line.addVertex(poses.last(), (float) point.x, (float) point.y, (float) point.z)
          .setColor(0, 0, 0, 255)
          .setNormal(poses.last(), (float) tangent.x, (float) tangent.y, (float) tangent.z);
    }
  }

  /** The third-person rod offset follows the held hand, body yaw, and villager size. */
  private static Vec3 rodTip(RealPerson person, float partialTick) {
    int side = person.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
    if (!person.getMainHandItem().is(Items.FISHING_ROD)) side = -side;
    float yaw = Mth.rotLerp(partialTick, person.yBodyRotO, person.yBodyRot) * Mth.DEG_TO_RAD;
    float scale = person.getScale() * 0.9375F * person.getLifeStage().scale();
    double lateral = side * 0.35D * scale;
    double forward = 0.8D * scale;
    return person.getEyePosition(partialTick).add(
        -Mth.cos(yaw) * lateral - Mth.sin(yaw) * forward,
        -0.45D * scale - (person.isCrouching() ? 0.1875D : 0),
        -Mth.sin(yaw) * lateral + Mth.cos(yaw) * forward);
  }

  private static Vec3 linePoint(Vec3 rod, Vec3 bobber, double fraction) {
    return rod.lerp(bobber, fraction).add(0, -0.35D * Math.sin(Math.PI * fraction), 0);
  }

  private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, int light, int x, int y) {
    buffer.addVertex(pose, x - 0.5F, y - 0.5F, 0).setColor(-1).setUv(x, 1 - y)
        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
  }
}
