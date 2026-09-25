package io.redspace.ironspell_more.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.client.particle.JadeAuraVfx;
import io.redspace.ironspell_more.registry.MobEffectsRegistry;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE, modid = IronSpellMore.MODID)
public final class JadeAuraRenderEvents {
    private static final ResourceLocation WHITE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");
    private static final RenderType AURA_RENDER_TYPE =
            RenderType.entityTranslucentEmissive(WHITE_TEXTURE, false);
    private static final double FULL_DETAIL_DISTANCE_SQR = 24.0D * 24.0D;
    private static final double MAX_RENDER_DISTANCE_SQR = 48.0D * 48.0D;

    private JadeAuraRenderEvents() {
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (!isActive(entity)) {
            return;
        }

        if (io.redspace.ironspell_more.compat.epicfight.EpicFightCompat.isBattleMode(entity)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = minecraft.gameRenderer.getMainCamera();
        double distanceSqr = camera.getPosition().distanceToSqr(entity.position());
        if (distanceSqr > MAX_RENDER_DISTANCE_SQR) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource bufferSource = event.getMultiBufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(AURA_RENDER_TYPE);
        float ageInTicks = entity.tickCount + event.getPartialTick();

        poseStack.pushPose();
        try {
            JadeAuraRibbonGeometry.render(poseStack, consumer,
                    entity.getBbWidth(), entity.getBbHeight(), ageInTicks,
                    entity.getId(), distanceSqr > FULL_DETAIL_DISTANCE_SQR);
        } finally {
            poseStack.popPose();
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.isPaused() || level.getGameTime() % 3L != 0L) {
            return;
        }

        Camera camera = minecraft.gameRenderer.getMainCamera();
        for (Entity candidate : level.entitiesForRendering()) {
            if (candidate instanceof LivingEntity living
                    && isActive(living)
                    && camera.getPosition().distanceToSqr(living.position()) <= MAX_RENDER_DISTANCE_SQR) {
                JadeAuraVfx.spawnSustainedAura(living);
            }
        }
    }

    private static boolean isActive(LivingEntity entity) {
        return entity != null
                && entity.isAlive()
                && !entity.isRemoved()
                && entity.hasEffect(MobEffectsRegistry.JADE_AURA.get());
    }
}
