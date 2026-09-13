package io.redspace.ironspell_more.registry;

import com.mojang.serialization.Codec;
import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.client.particle.ShockwaveParticleOptionCustom;
import io.redspace.ironspell_more.client.particle.ZapParticleOptionCustom;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ParticleRegistry {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, IronSpellMore.MODID);

    @SuppressWarnings("deprecation")
    public static final RegistryObject<ParticleType<ZapParticleOptionCustom>> ZAP_CUSTOM = PARTICLE_TYPES.register("zap_custom",
            () -> new ParticleType<ZapParticleOptionCustom>(false, ZapParticleOptionCustom.DESERIALIZER) {
                @Override
                public Codec<ZapParticleOptionCustom> codec() {
                    return ZapParticleOptionCustom.CODEC;
                }
            });

    @SuppressWarnings("deprecation")
    public static final RegistryObject<ParticleType<ShockwaveParticleOptionCustom>> SHOCKWAVE_CUSTOM = PARTICLE_TYPES.register("shockwave_custom",
            () -> new ParticleType<ShockwaveParticleOptionCustom>(false, ShockwaveParticleOptionCustom.DESERIALIZER) {
                @Override
                public Codec<ShockwaveParticleOptionCustom> codec() {
                    return ShockwaveParticleOptionCustom.CODEC;
                }
            });

    public static final RegistryObject<net.minecraft.core.particles.SimpleParticleType> WHITE_FIRE = PARTICLE_TYPES.register("white_fire",
            () -> new net.minecraft.core.particles.SimpleParticleType(false));

    public static final RegistryObject<net.minecraft.core.particles.SimpleParticleType> WHITE_EMBER = PARTICLE_TYPES.register("white_ember",
            () -> new net.minecraft.core.particles.SimpleParticleType(false));

    public static final RegistryObject<net.minecraft.core.particles.SimpleParticleType> WHITE_FIRE_EMITTER = PARTICLE_TYPES.register("white_fire_emitter",
            () -> new net.minecraft.core.particles.SimpleParticleType(false));

    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }
}
