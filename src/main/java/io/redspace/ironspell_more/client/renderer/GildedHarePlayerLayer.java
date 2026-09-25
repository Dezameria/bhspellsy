package io.redspace.ironspell_more.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.redspace.ironspell_more.registry.MobEffectsRegistry;
import io.redspace.ironsspellbooks.render.RenderHelper;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

public class GildedHarePlayerLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ResourceLocation TEXTURE_RIBBON = GildedHareRibbonGeometry.TEXTURE_RIBBON;

    public GildedHarePlayerLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!player.hasEffect(MobEffectsRegistry.GILDED_HARE.get())) {
            return;
        }

        if (io.redspace.ironspell_more.compat.epicfight.EpicFightCompat.isBattleMode(player)) {
            return;
        }

        RenderType renderType = RenderHelper.CustomerRenderType.magic(TEXTURE_RIBBON);
        VertexConsumer consumer = bufferSource.getBuffer(renderType);

        poseStack.pushPose();
        this.getParentModel().head.translateAndRotate(poseStack);
        GildedHareRibbonGeometry.renderRabbitEars(poseStack, consumer, ageInTicks);
        poseStack.popPose();
    }
}
