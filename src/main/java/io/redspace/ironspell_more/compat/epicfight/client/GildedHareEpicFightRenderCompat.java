package io.redspace.ironspell_more.compat.epicfight.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.redspace.ironspell_more.client.renderer.GildedHareRibbonGeometry;
import io.redspace.ironspell_more.registry.MobEffectsRegistry;
import io.redspace.ironsspellbooks.render.RenderHelper;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.client.forgeevent.PatchedRenderersEvent;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.client.renderer.patched.entity.PatchedEntityRenderer;
import yesman.epicfight.client.renderer.patched.entity.PatchedLivingEntityRenderer;
import yesman.epicfight.client.renderer.patched.layer.PatchedLayer;
import yesman.epicfight.model.armature.HumanoidArmature;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Registers Gilded Hare visuals with Epic Fight's patched living renderers.
 * This class is loaded only on the physical client when Epic Fight is present.
 */
public final class GildedHareEpicFightRenderCompat {
    private GildedHareEpicFightRenderCompat() {
    }

    public static void registerModEvents(IEventBus modEventBus) {
        modEventBus.addListener(GildedHareEpicFightRenderCompat::onModifyPatchedRenderers);
    }

    private static void onModifyPatchedRenderers(PatchedRenderersEvent.Modify event) {
        Set<PatchedLivingEntityRenderer<?, ?, ?, ?, ?>> attached =
                Collections.newSetFromMap(new IdentityHashMap<>());

        for (EntityType<?> entityType : ForgeRegistries.ENTITY_TYPES.getValues()) {
            PatchedEntityRenderer<?, ?, ?, ?> renderer = event.get(entityType);
            if (renderer instanceof PatchedLivingEntityRenderer<?, ?, ?, ?, ?> livingRenderer
                    && attached.add(livingRenderer)) {
                attachLayer(livingRenderer);
            }
        }
    }

    private static <E extends LivingEntity,
            T extends LivingEntityPatch<E>,
            M extends EntityModel<E>,
            R extends LivingEntityRenderer<E, M>,
            AM extends SkinnedMesh>
    void attachLayer(PatchedLivingEntityRenderer<E, T, M, R, AM> renderer) {
        renderer.addCustomLayer(new GildedHareEpicFightLayer<>());
    }

    private static final class GildedHareEpicFightLayer<
            E extends LivingEntity,
            T extends LivingEntityPatch<E>,
            M extends EntityModel<E>> extends PatchedLayer<E, T, M, RenderLayer<E, M>> {

        @Override
        protected void renderLayer(T patch, E entity, RenderLayer<E, M> unusedVanillaLayer,
                                   PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                   OpenMatrix4f[] poseMatrices, float bob, float netHeadYaw,
                                   float headPitch, float partialTicks) {
            if (!entity.isAlive()) {
                return;
            }

            MobEffectInstance mark = entity.getEffect(MobEffectsRegistry.GILDED_HARE_MARK.get());
            boolean renderCasterRibbons = entity instanceof Player
                    && entity.hasEffect(MobEffectsRegistry.GILDED_HARE.get());
            if (mark == null && !renderCasterRibbons) {
                return;
            }

            RenderType renderType = RenderHelper.CustomerRenderType.magic(
                    GildedHareRibbonGeometry.TEXTURE_RIBBON);
            VertexConsumer consumer = bufferSource.getBuffer(renderType);
            float ageInTicks = entity.tickCount + partialTicks;

            if (mark != null) {
                poseStack.pushPose();
                GildedHareRibbonGeometry.renderBinding(poseStack, consumer,
                        entity.getBbWidth(), entity.getBbHeight(), ageInTicks,
                        mark.getAmplifier());
                poseStack.popPose();
            }

            if (renderCasterRibbons && patch.getArmature() instanceof HumanoidArmature armature) {
                renderAtHeadJoint(poseStack, poseMatrices, armature.head, () ->
                        GildedHareRibbonGeometry.renderRabbitEars(poseStack, consumer, ageInTicks));
                renderAtLegJoint(poseStack, poseMatrices, armature.legL, () ->
                        GildedHareRibbonGeometry.renderLegRibbons(poseStack, consumer, true, ageInTicks));
                renderAtLegJoint(poseStack, poseMatrices, armature.legR, () ->
                        GildedHareRibbonGeometry.renderLegRibbons(poseStack, consumer, false, ageInTicks));
            }
        }

        private static void renderAtHeadJoint(PoseStack poseStack, OpenMatrix4f[] poseMatrices,
                                              Joint joint, Runnable renderer) {
            if (joint == null || poseMatrices == null) {
                return;
            }

            int jointId = joint.getId();
            if (jointId < 0 || jointId >= poseMatrices.length || poseMatrices[jointId] == null) {
                return;
            }

            OpenMatrix4f transform = new OpenMatrix4f();
            transform.scale(new Vec3f(-1.0F, -1.0F, 1.0F));
            transform.mulFront(poseMatrices[jointId]);

            poseStack.pushPose();
            MathUtils.mulStack(poseStack, transform);
            renderer.run();
            poseStack.popPose();
        }

        private static void renderAtLegJoint(PoseStack poseStack, OpenMatrix4f[] poseMatrices,
                                             Joint joint, Runnable renderer) {
            if (joint == null || poseMatrices == null) {
                return;
            }

            int jointId = joint.getId();
            if (jointId < 0 || jointId >= poseMatrices.length || poseMatrices[jointId] == null) {
                return;
            }

            OpenMatrix4f transform = new OpenMatrix4f();
            // Epic Fight's legL/legR joints are inverted along Y in biped.json relative to torso/head.
            // Using (-1, 1, -1) correctly aligns the local leg axes with vanilla ModelPart (+Y pointing down).
            transform.scale(new Vec3f(-1.0F, 1.0F, -1.0F));
            transform.mulFront(poseMatrices[jointId]);

            poseStack.pushPose();
            MathUtils.mulStack(poseStack, transform);
            // Epic Fight's legL/legR joint pivots at the knee (6 units = 0.375m down from hip).
            // Shift reference origin up by 0.375m along Y so vanilla leg coordinates (y = 9.2 to 11.75)
            // place the ribbon bands and bow accurately around the ankle and foot.
            poseStack.translate(0.0F, -0.375F, 0.0F);
            renderer.run();
            poseStack.popPose();
        }
    }
}
