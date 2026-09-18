package io.redspace.ironspell_more.entity.spells.venomous_blossomfall;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.redspace.ironspell_more.IronSpellMore;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class AzureVenomNeedleRenderer extends EntityRenderer<AzureVenomNeedleEntity> {
    // The existing white fire frame provides a neutral temporary texture that can be
    // tinted cyan until the final geo model and texture are supplied.
    public static final ResourceLocation TEXTURE =
            IronSpellMore.id("textures/particle/white_fire_1.png");
    public static final RenderType RENDER_TYPE = RenderType.entityTranslucentEmissive(TEXTURE);

    private final AzureVenomNeedleModel model;

    public AzureVenomNeedleRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new AzureVenomNeedleModel(context.bakeLayer(AzureVenomNeedleModel.LAYER_LOCATION));
    }

    @Override
    public void render(AzureVenomNeedleEntity entity, float yaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        float rollSpeed = 18.0F + entity.getChargeProgress() * 42.0F;
        float roll = (entity.tickCount + partialTick) * rollSpeed;
        renderNeedle(model, poseStack, bufferSource, entity.getFlightDirection(), roll,
                0.85F + entity.getChargeProgress() * 0.2F, 1.0F);
        super.render(entity, yaw, partialTick, poseStack, bufferSource, packedLight);
    }

    public static void renderNeedle(AzureVenomNeedleModel model, PoseStack poseStack,
            MultiBufferSource bufferSource, Vec3 direction, float rollDegrees, float scale, float alpha) {
        poseStack.pushPose();
        orientToDirection(poseStack, direction);
        poseStack.mulPose(Axis.ZP.rotationDegrees(rollDegrees));
        poseStack.scale(scale, scale, scale);
        model.render(poseStack, bufferSource.getBuffer(RENDER_TYPE), LightTexture.FULL_BRIGHT,
                0.25F, 1.0F, 0.82F, alpha);
        poseStack.popPose();
    }

    public static void orientToDirection(PoseStack poseStack, Vec3 direction) {
        Vec3 normalized = direction.lengthSqr() < 1.0E-8D ? new Vec3(0.0D, 0.0D, 1.0D)
                : direction.normalize();
        float xRot = -((float) (Mth.atan2(normalized.horizontalDistance(), normalized.y)
                * Mth.RAD_TO_DEG) - 90.0F);
        float yRot = -((float) (Mth.atan2(normalized.z, normalized.x) * Mth.RAD_TO_DEG) + 90.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(xRot));
    }

    @Override
    public ResourceLocation getTextureLocation(AzureVenomNeedleEntity entity) {
        return TEXTURE;
    }
}
