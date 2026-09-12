package io.redspace.ironspell_more.client.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.redspace.ironspell_more.registry.ParticleRegistry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;

import java.util.Locale;

public class ShockwaveParticleOptionCustom implements ParticleOptions {
    private final Vector3f color;
    private final float radius;
    private final boolean fullbright;
    private final Vector3f direction;

    public static final Codec<ShockwaveParticleOptionCustom> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.FLOAT.fieldOf("r").forGetter(option -> option.color.x),
                    Codec.FLOAT.fieldOf("g").forGetter(option -> option.color.y),
                    Codec.FLOAT.fieldOf("b").forGetter(option -> option.color.z),
                    Codec.FLOAT.fieldOf("radius").forGetter(option -> option.radius),
                    Codec.BOOL.fieldOf("fullbright").forGetter(option -> option.fullbright),
                    Codec.FLOAT.fieldOf("dx").forGetter(option -> option.direction.x),
                    Codec.FLOAT.fieldOf("dy").forGetter(option -> option.direction.y),
                    Codec.FLOAT.fieldOf("dz").forGetter(option -> option.direction.z)
            ).apply(instance, (r, g, b, radius, fullbright, dx, dy, dz) ->
                    new ShockwaveParticleOptionCustom(new Vector3f(r, g, b), radius, fullbright, new Vector3f(dx, dy, dz)))
    );

    @SuppressWarnings("deprecation")
    public static final Deserializer<ShockwaveParticleOptionCustom> DESERIALIZER = new Deserializer<>() {
        @Override
        public ShockwaveParticleOptionCustom fromCommand(ParticleType<ShockwaveParticleOptionCustom> particleType, StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            float r = reader.readFloat();
            reader.expect(' ');
            float g = reader.readFloat();
            reader.expect(' ');
            float b = reader.readFloat();
            reader.expect(' ');
            float radius = reader.readFloat();
            reader.expect(' ');
            boolean fullbright = reader.readBoolean();
            reader.expect(' ');
            float dx = reader.readFloat();
            reader.expect(' ');
            float dy = reader.readFloat();
            reader.expect(' ');
            float dz = reader.readFloat();
            return new ShockwaveParticleOptionCustom(new Vector3f(r, g, b), radius, fullbright, new Vector3f(dx, dy, dz));
        }

        @Override
        public ShockwaveParticleOptionCustom fromNetwork(ParticleType<ShockwaveParticleOptionCustom> particleType, FriendlyByteBuf buf) {
            float r = buf.readFloat();
            float g = buf.readFloat();
            float b = buf.readFloat();
            float radius = buf.readFloat();
            boolean fullbright = buf.readBoolean();
            float dx = buf.readFloat();
            float dy = buf.readFloat();
            float dz = buf.readFloat();
            return new ShockwaveParticleOptionCustom(new Vector3f(r, g, b), radius, fullbright, new Vector3f(dx, dy, dz));
        }
    };

    public ShockwaveParticleOptionCustom(Vector3f color, float radius, boolean fullbright, Vector3f direction) {
        this.color = color;
        this.radius = radius;
        this.fullbright = fullbright;
        this.direction = direction;
    }

    @Override
    public ParticleType<ShockwaveParticleOptionCustom> getType() {
        return ParticleRegistry.SHOCKWAVE_CUSTOM.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeFloat(color.x);
        buf.writeFloat(color.y);
        buf.writeFloat(color.z);
        buf.writeFloat(radius);
        buf.writeBoolean(fullbright);
        buf.writeFloat(direction.x);
        buf.writeFloat(direction.y);
        buf.writeFloat(direction.z);
    }

    @Override
    public String writeToString() {
        return String.format(Locale.ROOT, "%s %.2f %.2f %.2f %.2f %b %.2f %.2f %.2f", ForgeRegistries.PARTICLE_TYPES.getKey(getType()), color.x, color.y, color.z, radius, fullbright, direction.x, direction.y, direction.z);
    }

    public Vector3f getColor() {
        return color;
    }

    public float getRadius() {
        return radius;
    }

    public boolean isFullbright() {
        return fullbright;
    }

    public Vector3f getDirection() {
        return direction;
    }
}
