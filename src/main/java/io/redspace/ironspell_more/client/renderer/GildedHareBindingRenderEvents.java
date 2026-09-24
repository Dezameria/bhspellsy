package io.redspace.ironspell_more.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.registry.MobEffectsRegistry;
import io.redspace.ironsspellbooks.render.RenderHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE, modid = IronSpellMore.MODID)
public class GildedHareBindingRenderEvents {
    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (entity == null || !entity.isAlive()) {
            return;
        }

        MobEffectInstance mark = entity.getEffect(MobEffectsRegistry.GILDED_HARE_MARK.get());
        if (mark == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource bufferSource = event.getMultiBufferSource();
        RenderType renderType = RenderHelper.CustomerRenderType.magic(GildedHareRibbonGeometry.TEXTURE_RIBBON);
        VertexConsumer consumer = bufferSource.getBuffer(renderType);
        float ageInTicks = entity.tickCount + event.getPartialTick();

        poseStack.pushPose();
        GildedHareRibbonGeometry.renderBinding(poseStack, consumer,
                entity.getBbWidth(), entity.getBbHeight(), ageInTicks, mark.getAmplifier());
        poseStack.popPose();
    }
}
