package io.redspace.ironspell_more.entity.spells.crimson_thornbind;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.redspace.ironsspellbooks.render.GeoLivingEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;

public class CrimsonRootRenderer extends GeoLivingEntityRenderer<CrimsonRootEntity> {
    public CrimsonRootRenderer(EntityRendererProvider.Context context) {
        super(context, new CrimsonRootModel());
    }

    @Override
    public void render(CrimsonRootEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (entity.getWarmup() > 0) {
            return;
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public void preRender(PoseStack poseStack, CrimsonRootEntity animatable, BakedGeoModel model, @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float r, float g, float b, float a) {
        float scale = animatable.getBaseScale();
        if (animatable.isPathMode()) {
            poseStack.translate(0.0D, 0.18D, 0.0D);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.scale(scale * 0.65F, scale * 0.95F, scale * 0.65F);
        } else {
            var rooted = animatable.getFirstPassenger();
            if (rooted != null) {
                float passengerScale = rooted.getBbWidth() / 0.6F;
                if (passengerScale > 1.0F) {
                    scale *= passengerScale;
                }
            }
            poseStack.scale(scale, scale, scale);
        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, r, g, b, a);
    }
}
