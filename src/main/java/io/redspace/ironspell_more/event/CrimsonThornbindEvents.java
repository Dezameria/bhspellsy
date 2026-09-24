package io.redspace.ironspell_more.event;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.entity.spells.crimson_thornbind.CrimsonRootEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = IronSpellMore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CrimsonThornbindEvents {

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getAmount() <= 0.0F) {
            return;
        }

        LivingEntity victim = event.getEntity();
        if (victim.getVehicle() instanceof CrimsonRootEntity root && root.isBindMode()) {
            LivingEntity caster = root.getOwner();
            Entity attacker = event.getSource().getEntity();
            if (caster != null && attacker == caster) {
                float rend = root.getRendDamage();
                if (rend > 0.0F) {
                    event.setAmount(event.getAmount() + rend);

                    if (victim.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
                        serverLevel.sendParticles(ParticleTypes.CRIMSON_SPORE, victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(), 16, 0.3, 0.3, 0.3, 0.05);
                    }
                    victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.PLAYERS, 1.2F, 0.8F);
                    victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.2F);
                }
            }
        }
    }
}
