package io.redspace.ironspell_more.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.redspace.ironspell_more.registry.ParticleRegistry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Locale;

public class ZapParticleOptionCustom implements ParticleOptions {
    private final Vec3 destination;
    private final float scale;

    public static final Codec<ZapParticleOptionCustom> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("x").forGetter(option -> option.destination.x),
            Codec.DOUBLE.fieldOf("y").forGetter(option -> option.destination.y),
            Codec.DOUBLE.fieldOf("z").forGetter(option -> option.destination.z),
            Codec.FLOAT.fieldOf("scale").forGetter(option -> option.scale))
            .apply(instance, (x, y, z, scale) -> new ZapParticleOptionCustom(new Vec3(x, y, z), scale)));

    @SuppressWarnings("deprecation")
    public static final Deserializer<ZapParticleOptionCustom> DESERIALIZER = new Deserializer<>() {
        @Override
        public ZapParticleOptionCustom fromCommand(ParticleType<ZapParticleOptionCustom> particleType,
                StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            double x = reader.readDouble();
            reader.expect(' ');
            double y = reader.readDouble();
            reader.expect(' ');
            double z = reader.readDouble();
            reader.expect(' ');
            float scale = reader.readFloat();
            return new ZapParticleOptionCustom(new Vec3(x, y, z), scale);
        }

        @Override
        public ZapParticleOptionCustom fromNetwork(ParticleType<ZapParticleOptionCustom> particleType,
                FriendlyByteBuf buf) {
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            float scale = buf.readFloat();
            return new ZapParticleOptionCustom(new Vec3(x, y, z), scale);
        }
    };

    public ZapParticleOptionCustom(Vec3 destination, float scale) {
        this.destination = destination;
        this.scale = scale;
    }

    public ZapParticleOptionCustom(Vec3 destination) {
        this(destination, 1.0f);
    }

    @Override
    public ParticleType<ZapParticleOptionCustom> getType() {
        return ParticleRegistry.ZAP_CUSTOM.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeDouble(destination.x);
        buf.writeDouble(destination.y);
        buf.writeDouble(destination.z);
        buf.writeFloat(scale);
    }

    @Override
    public String writeToString() {
        return String.format(Locale.ROOT, "%s %.2f %.2f %.2f %.2f", ForgeRegistries.PARTICLE_TYPES.getKey(getType()),
                destination.x, destination.y, destination.z, scale);
    }

    public Vec3 getDestination() {
        return destination;
    }

    public float getScale() {
        return scale;
    }
}
