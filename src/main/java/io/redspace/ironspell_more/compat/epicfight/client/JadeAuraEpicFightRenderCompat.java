package io.redspace.ironspell_more.compat.epicfight.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.redspace.ironspell_more.client.renderer.JadeAuraRibbonGeometry;
import io.redspace.ironspell_more.registry.MobEffectsRegistry;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.api.client.forgeevent.PatchedRenderersEvent;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.renderer.patched.entity.PatchedEntityRenderer;
import yesman.epicfight.client.renderer.patched.entity.PatchedLivingEntityRenderer;
import yesman.epicfight.client.renderer.patched.layer.PatchedLayer;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Registers sustained Jade Aura ribbons with Epic Fight's patched living renderers.
 * This class is loaded only on the physical client when Epic Fight is present.
 */
public final class JadeAuraEpicFightRenderCompat {
    private static final ResourceLocation WHITE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");
    private static final RenderType AURA_RENDER_TYPE =
            RenderType.entityTranslucentEmissive(WHITE_TEXTURE, false);
    private static final double FULL_DETAIL_DISTANCE_SQR = 24.0D * 24.0D;
    private static final double MAX_RENDER_DISTANCE_SQR = 48.0D * 48.0D;

    private JadeAuraEpicFightRenderCompat() {
    }

    public static void registerModEvents(IEventBus modEventBus) {
        modEventBus.addListener(JadeAuraEpicFightRenderCompat::onModifyPatchedRenderers);
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
        renderer.addCustomLayer(new JadeAuraEpicFightLayer<>());
    }

    private static final class JadeAuraEpicFightLayer<
            E extends LivingEntity,
            T extends LivingEntityPatch<E>,
            M extends EntityModel<E>> extends PatchedLayer<E, T, M, RenderLayer<E, M>> {

        @Override
        protected void renderLayer(T patch, E entity, RenderLayer<E, M> unusedVanillaLayer,
                                   PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                   OpenMatrix4f[] poseMatrices, float bob, float netHeadYaw,
                                   float headPitch, float partialTicks) {
            if (!entity.isAlive() || !entity.hasEffect(MobEffectsRegistry.JADE_AURA.get())) {
                return;
            }

            Minecraft minecraft = Minecraft.getInstance();
            Camera camera = minecraft.gameRenderer.getMainCamera();
            double distanceSqr = camera.getPosition().distanceToSqr(entity.position());
            if (distanceSqr > MAX_RENDER_DISTANCE_SQR) {
                return;
            }

            VertexConsumer consumer = bufferSource.getBuffer(AURA_RENDER_TYPE);
            float ageInTicks = entity.tickCount + partialTicks;

            poseStack.pushPose();
            try {
                JadeAuraRibbonGeometry.render(poseStack, consumer,
                        entity.getBbWidth(), entity.getBbHeight(), ageInTicks,
                        entity.getId(), distanceSqr > FULL_DETAIL_DISTANCE_SQR);
            } finally {
                poseStack.popPose();
            }
        }
    }
}
