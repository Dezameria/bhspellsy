package io.redspace.ironspell_more.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.spells.nature.GalePiercerSpell;
import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.render.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GalePiercerChargeLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ResourceLocation TEXTURE_NORMAL =
            IronSpellMore.id("textures/entity/gale_piercer/white_gray_arrow.png");
    public static final ResourceLocation TEXTURE_FIRE =
            IronsSpellbooks.id("textures/entity/fire_arrow.png");

    private static final Map<UUID, Integer> REMOTE_CAST_START_TICKS = new HashMap<>();

    public GalePiercerChargeLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        var syncedSpellData = ClientMagicData.getSyncedSpellData(player);
        if (!syncedSpellData.isCasting()) {
            REMOTE_CAST_START_TICKS.remove(player.getUUID());
            return;
        }

        if (!GalePiercerSpell.SPELL_ID.toString().equals(syncedSpellData.getCastingSpellId())) {
            REMOTE_CAST_START_TICKS.remove(player.getUUID());
            return;
        }

        HumanoidArm arm = player.getMainArm();
        poseStack.pushPose();
        this.getParentModel().translateToHand(arm, poseStack);

        // Position and orient arrow in hand identically to FireArrowSpell / ChargeSpellLayer
        float xOffset = arm == HumanoidArm.RIGHT ? (1.0F / 32.0F) : (-1.0F / 32.0F);
        poseStack.translate(xOffset, 0.5F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));

        boolean isFullCharge = isFullyCharged(player);
        renderArrow(poseStack, bufferSource, isFullCharge);

        poseStack.popPose();
    }

    public static boolean isFullyCharged(AbstractClientPlayer player) {
        if (player == Minecraft.getInstance().player) {
            int elapsed = ClientMagicData.getCastDuration() - ClientMagicData.getCastDurationRemaining();
            return elapsed >= GalePiercerSpell.FULL_CHARGE_TICKS;
        } else {
            Integer startTick = REMOTE_CAST_START_TICKS.get(player.getUUID());
            if (startTick == null) {
                REMOTE_CAST_START_TICKS.put(player.getUUID(), player.tickCount);
                return false;
            }
            return (player.tickCount - startTick) >= GalePiercerSpell.FULL_CHARGE_TICKS;
        }
    }

    private static void renderArrow(PoseStack poseStack, MultiBufferSource bufferSource, boolean isFullCharge) {
        poseStack.scale(0.13F, 0.13F, 0.13F);

        PoseStack.Pose pose = poseStack.last();
        Matrix4f poseMatrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();

        ResourceLocation texture = isFullCharge ? TEXTURE_FIRE : TEXTURE_NORMAL;
        RenderType renderType = RenderHelper.CustomerRenderType.magic(texture);
        VertexConsumer consumer = bufferSource.getBuffer(renderType);

        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(-2, 0, 0);

        int r = isFullCharge ? 255 : 220;
        int g = isFullCharge ? 255 : 225;
        int b = isFullCharge ? 255 : 230;
        int a = 255;

        for (int j = 0; j < 4; ++j) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            vertex(poseMatrix, normalMatrix, consumer, -8, -2, 0, 0.0F, 0.0F, 0, 1, 0, LightTexture.FULL_BRIGHT, r, g, b, a);
            vertex(poseMatrix, normalMatrix, consumer, 8, -2, 0, 0.5F, 0.0F, 0, 1, 0, LightTexture.FULL_BRIGHT, r, g, b, a);
            vertex(poseMatrix, normalMatrix, consumer, 8, 2, 0, 0.5F, 0.15625F, 0, 1, 0, LightTexture.FULL_BRIGHT, r, g, b, a);
            vertex(poseMatrix, normalMatrix, consumer, -8, 2, 0, 0.0F, 0.15625F, 0, 1, 0, LightTexture.FULL_BRIGHT, r, g, b, a);
        }
    }

    private static void vertex(Matrix4f pMatrix, Matrix3f pNormals, VertexConsumer pVertexBuilder,
                               int pOffsetX, int pOffsetY, int pOffsetZ,
                               float pTextureX, float pTextureY,
                               int pNormalX, int pNormalY, int pNormalZ,
                               int pPackedLight, int r, int g, int b, int a) {
        pVertexBuilder.vertex(pMatrix, (float) pOffsetX, (float) pOffsetY, (float) pOffsetZ)
                .color(r, g, b, a)
                .uv(pTextureX, pTextureY)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(pPackedLight)
                .normal((float) pNormalX, (float) pNormalZ, (float) pNormalY)
                .endVertex();
    }
}
