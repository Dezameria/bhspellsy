package io.redspace.ironspell_more.client.event;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.registry.MobEffectsRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = IronSpellMore.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TigershadeClientEvents {
    private static final Set<Integer> GLOWING_ENTITY_IDS = new HashSet<>();

    private TigershadeClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;

        if (player == null || level == null) {
            GLOWING_ENTITY_IDS.clear();
            return;
        }

        boolean hasStance = player.hasEffect(MobEffectsRegistry.TIGERSHADE_STANCE.get());
        Set<Integer> activeGlowing = new HashSet<>();

        if (hasStance) {
            // Search for marked entities within tracking distance (up to 40 blocks)
            AABB searchBox = player.getBoundingBox().inflate(40.0D);
            List<LivingEntity> markedEntities = level.getEntitiesOfClass(LivingEntity.class, searchBox,
                    e -> e != player && e.isAlive() && e.hasEffect(MobEffectsRegistry.TIGERSHADE_MARK.get()));

            for (LivingEntity marked : markedEntities) {
                marked.setGlowingTag(true);
                activeGlowing.add(marked.getId());
            }
        }

        // Clean up glowing flag on entities that are no longer marked or when stance is lost
        for (Integer id : GLOWING_ENTITY_IDS) {
            if (!activeGlowing.contains(id)) {
                Entity entity = level.getEntity(id);
                if (entity != null) {
                    entity.setGlowingTag(false);
                }
            }
        }

        GLOWING_ENTITY_IDS.clear();
        GLOWING_ENTITY_IDS.addAll(activeGlowing);
    }
}
