package io.redspace.ironspell_more.entity.spells.crimson_rain_bathes_moon;

import com.github.L_Ender.cataclysm.client.model.CMModelLayers;
import com.github.L_Ender.cataclysm.client.model.entity.Elemental_Spear_Model;
import com.github.L_Ender.cataclysm.client.render.CMRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CrimsonSpearRenderer extends EntityRenderer<CrimsonSpearEntity> {
    private final Elemental_Spear_Model model;
    private static final ResourceLocation[] TEXTURE_PROGRESS = new ResourceLocation[6];

    static {
        for (int i = 0; i < 6; ++i) {
            TEXTURE_PROGRESS[i] = ResourceLocation.fromNamespaceAndPath("cataclysm", "textures/entity/sea/spear/water_spear_" + i + ".png");
        }
    }

    public CrimsonSpearRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new Elemental_Spear_Model(context.bakeLayer(CMModelLayers.ELEMENTAL_SPEAR_MODEL));
    }

    @Override
    public void render(CrimsonSpearEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        float yRot = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
        float xRot = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        this.model.setupAnim(entity, 0.0F, 0.0F, (float) entity.tickCount + partialTicks, yRot, xRot);
        VertexConsumer vertexConsumer = buffer.getBuffer(CMRenderTypes.getGhost(this.getTextureLocation(entity)));
        // Red tint: Red=1.0F, Green=0.12F, Blue=0.18F, Alpha=1.0F
        this.model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 0.12F, 0.18F, 1.0F);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(CrimsonSpearEntity entity) {
        float f = ((float) entity.tickCount * 0.5F) % 5.0F;
        return TEXTURE_PROGRESS[Mth.clamp((int) f, 0, 5)];
    }
}
