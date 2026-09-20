package io.redspace.ironspell_more.entity.spells.gale_piercer;

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

public class WindArrowRenderer extends EntityRenderer<WindArrowEntity> {
    public static final ResourceLocation TEXTURE =
            IronSpellMore.id("textures/particle/white_fire_1.png");
    public static final RenderType RENDER_TYPE = RenderType.entityTranslucent(TEXTURE);

    private final GalePiercerArrowModel model;

    public WindArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new GalePiercerArrowModel(context.bakeLayer(GalePiercerArrowModel.LAYER_LOCATION));
    }

    @Override
    public void render(WindArrowEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        Vec3 motion = entity.getDeltaMovement();
        orientToDirection(poseStack, motion);

        // Faint wind pale green/cyan tint
        model.render(poseStack, bufferSource.getBuffer(RENDER_TYPE), LightTexture.FULL_BRIGHT,
                0.7F, 1.0F, 0.85F, 0.9F);

        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, bufferSource, packedLight);
    }

    public static void orientToDirection(PoseStack poseStack, Vec3 direction) {
        Vec3 normalized = direction.lengthSqr() < 1.0E-8D ? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
        float xRot = -((float) (Mth.atan2(normalized.horizontalDistance(), normalized.y) * Mth.RAD_TO_DEG) - 90.0F);
        float yRot = -((float) (Mth.atan2(normalized.z, normalized.x) * Mth.RAD_TO_DEG) + 90.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(xRot));
    }

    @Override
    public ResourceLocation getTextureLocation(WindArrowEntity entity) {
        return TEXTURE;
    }
}
