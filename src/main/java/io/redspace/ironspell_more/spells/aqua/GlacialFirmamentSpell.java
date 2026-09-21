package io.redspace.ironspell_more.spells.aqua;

import com.gametechbc.traveloptics.api.init.TravelopticsSchools;
import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.entity.spells.glacial_firmament.GlacialSpikeEntity;
import io.redspace.ironspell_more.entity.spells.glacial_firmament.GlacialTombEntity;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.network.particles.ShockwaveParticlesPacket;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import io.redspace.ironsspellbooks.registries.ParticleRegistry;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

@AutoSpellConfig
public class GlacialFirmamentSpell extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(IronSpellMore.MODID, "glacial_firmament");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(TravelopticsSchools.AQUA_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(25)
            .build();

    public GlacialFirmamentSpell() {
        this.manaCostPerLevel = 15;
        this.baseSpellPower = 35;
        this.spellPowerPerLevel = 5;
        this.castTime = 0;
        this.baseManaCost = 60;
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.FROSTWAVE_PREPARE.get());
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return SpellAnimations.TOUCH_GROUND_ANIMATION;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage",
                        Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", 15),
                Component.translatable("ui.irons_spellbooks.effect_length", Utils.timeFromTicks(60, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource,
                       MagicData playerMagicData) {
        float radius = 15.0f;
        Vec3 center = entity.position();

        // 1. Visual shockwave & frostwave around caster
        MagicManager.spawnParticles(level,
                new BlastwaveParticleOptions(SchoolRegistry.ICE.get().getTargetingColor(), radius),
                center.x, center.y + 0.165f, center.z,
                1, 0, 0, 0, 0, true);

        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity,
                new ShockwaveParticlesPacket(new Vec3(center.x, center.y + 0.165f, center.z), radius,
                        ParticleRegistry.SNOWFLAKE_PARTICLE.get()));

        // Radial outward-shooting ice mist & particle blast
        if (level instanceof ServerLevel serverLevel) {
            spawnOutwardIceParticles(serverLevel, center);
        }

        // 2. Caster Buff: Speed II for 2 seconds (40 ticks)
        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1));

        // 3. Enemy Damage (35 base) & Slowness II for 3 seconds (60 ticks) in 15-block radius
        if (!level.isClientSide) {
            float damage = getDamage(spellLevel, entity);
            level.getEntities(entity, entity.getBoundingBox().inflate(radius, 4, radius),
                            (target) -> !DamageSources.isFriendlyFireBetween(target, entity)
                                    && Utils.hasLineOfSight(level, entity, target, true))
                    .forEach(target -> {
                        if (target instanceof LivingEntity livingEntity
                                && livingEntity.distanceToSqr(entity) <= radius * radius) {
                            DamageSources.applyDamage(livingEntity, damage, getDamageSource(entity));
                            livingEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                            MagicManager.spawnParticles(level, ParticleHelper.SNOWFLAKE, livingEntity.getX(),
                                    livingEntity.getY() + livingEntity.getBbHeight() * 0.5f, livingEntity.getZ(), 30,
                                    livingEntity.getBbWidth() * 0.5f, livingEntity.getBbHeight() * 0.5f,
                                    livingEntity.getBbWidth() * 0.5f, 0.03, false);
                        }
                    });

            // 4. Glacial Spike & Glacial Tomb circular eruptions spaced out across 3 distinct rings
            spawnIceEruptions(level, entity, center);
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private void spawnOutwardIceParticles(ServerLevel serverLevel, Vec3 center) {
        int rays = 48;
        for (int i = 0; i < rays; i++) {
            double angle = (i * 2.0 * Math.PI) / rays;
            double vx = Math.cos(angle);
            double vz = Math.sin(angle);

            // Fast outward exploding snowflake particles and snow dust
            serverLevel.sendParticles(ParticleHelper.SNOWFLAKE, center.x, center.y + 0.25, center.z, 0, vx, 0.05, vz, 0.85);
            serverLevel.sendParticles(ParticleHelper.SNOW_DUST, center.x, center.y + 0.25, center.z, 0, vx, 0.07, vz, 0.95);
            serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y + 0.35, center.z, 0, vx, 0.09, vz, 0.70);

            // Intermediate velocity drifting particles for fuller coverage
            serverLevel.sendParticles(ParticleHelper.SNOWFLAKE, center.x, center.y + 0.2, center.z, 0, vx, 0.03, vz, 0.45);
        }
        // Dense ground burst of snow dust at center
        serverLevel.sendParticles(ParticleHelper.SNOW_DUST, center.x, center.y + 0.2, center.z, 40, 1.5, 0.2, 1.5, 0.08);
    }

    private void spawnIceEruptions(Level level, LivingEntity entity, Vec3 center) {
        // 3 well-spaced rings: Inner (close), Mid, and Outer (perimeter)
        float[] ringDistances = {4.0f, 8.5f, 14.0f};
        int[] ringCounts = {6, 8, 11};
        int[] ringWaitTimes = {2, 5, 8};
        float[] ringMinTilts = {8.0f, 22.0f, 46.0f};
        float[] ringMaxTilts = {14.0f, 30.0f, 56.0f};

        for (int ringIndex = 0; ringIndex < ringDistances.length; ringIndex++) {
            float dist = ringDistances[ringIndex];
            int count = ringCounts[ringIndex];
            int baseWait = ringWaitTimes[ringIndex];
            double angleStep = (2 * Math.PI) / count;
            double ringOffset = ringIndex * 0.55; // Rotate rings so pillars stagger naturally

            for (int i = 0; i < count; i++) {
                double angle = (i * angleStep) + ringOffset;
                Vec3 dirVec = new Vec3(Math.cos(angle), 0, Math.sin(angle)).normalize();
                Vec3 targetPos = center.add(dirVec.scale(dist));
                Vec3 groundPos = Utils.moveToRelativeGroundLevel(level, targetPos, 4);

                BlockPos belowPos = BlockPos.containing(groundPos).below();
                if (level.getBlockState(belowPos).isFaceSturdy(level, belowPos, Direction.UP)) {
                    float dirYaw = (float) Math.toDegrees(Math.atan2(-dirVec.x, dirVec.z));
                    int waitTime = baseWait + (i % 2);

                    // Tilts: Inner ring has gentle tilt (8°-14°), mid ring (22°-30°), outer ring sharp thrust out (46°-56°)
                    float tilt = Mth.lerp(dist / 14.0f, ringMinTilts[ringIndex], ringMaxTilts[ringIndex])
                            + Utils.random.nextIntBetweenInclusive(-2, 2);

                    // Alternate between GlacialTomb and GlacialSpike so they are spread out evenly
                    if (i % 3 == 0) {
                        GlacialTombEntity tomb = new GlacialTombEntity(level, entity);
                        float scale = Mth.lerp(dist / 14.0f, 2.0f, 3.8f);
                        tomb.setTombSize(scale);
                        tomb.moveTo(groundPos);
                        tomb.setWaitTime(waitTime);
                        tomb.setYRot(dirYaw);
                        tomb.setXRot(tilt * 0.75f);
                        tomb.setSilent(true);
                        level.addFreshEntity(tomb);
                    } else {
                        GlacialSpikeEntity spike = new GlacialSpikeEntity(level, entity);
                        float scale = Mth.lerp(dist / 14.0f, 2.6f, 5.5f);
                        spike.setSpikeSize(scale);
                        spike.moveTo(groundPos);
                        spike.setWaitTime(waitTime);
                        spike.setYRot(dirYaw - 45.0f + Utils.random.nextIntBetweenInclusive(-5, 5));
                        spike.setXRot(tilt);
                        spike.setSilent(true);
                        level.addFreshEntity(spike);
                    }
                }
            }
        }
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        return getSpellPower(spellLevel, entity);
    }

    @Override
    public boolean shouldAIStopCasting(int spellLevel, Mob mob, LivingEntity target) {
        return mob.distanceToSqr(target) > (15.0f * 15.0f);
    }
}
