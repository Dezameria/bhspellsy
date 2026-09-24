package io.redspace.ironspell_more.spells.aqua;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import com.gametechbc.traveloptics.api.init.TravelopticsSchools;
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
import io.redspace.ironsspellbooks.entity.spells.ice_spike.IceSpikeEntity;
import io.redspace.ironsspellbooks.network.particles.ShockwaveParticlesPacket;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import io.redspace.ironsspellbooks.registries.ParticleRegistry;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Optional;

import io.redspace.ironspell_more.config.SpellConfig;

@AutoSpellConfig
public class GlacialVeilSpell extends AbstractSpell {
    // ==========================================
    // SPELL TUNING CONSTANTS (Code Defaults)
    // ==========================================
    public static final float BASE_DAMAGE = 20.0F;
    public static final float DAMAGE_PER_LEVEL = 3.0F;
    public static final int BASE_MANA_COST = 50;
    public static final int MANA_COST_PER_LEVEL = 10;
    public static final double COOLDOWN_SECONDS = 20.0D;

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(IronSpellMore.MODID, "glacial_veil");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(TravelopticsSchools.AQUA_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(COOLDOWN_SECONDS)
            .build();

    public GlacialVeilSpell() {
        this.manaCostPerLevel = MANA_COST_PER_LEVEL;
        this.baseSpellPower = (int) BASE_DAMAGE;
        this.spellPowerPerLevel = (int) DAMAGE_PER_LEVEL;
        this.castTime = 0;
        this.baseManaCost = BASE_MANA_COST;
    }

    @Override
    public int getManaCost(int spellLevel) {
        return SpellConfig.GlacialVeil.getBaseMana() + (spellLevel - 1) * SpellConfig.GlacialVeil.getManaPerLevel();
    }

    @Override
    public int getSpellCooldown() {
        return (int) (SpellConfig.GlacialVeil.getCooldown() * 20);
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
                Component.translatable("ui.irons_spellbooks.radius", 10),
                Component.translatable("ui.irons_spellbooks.effect_length", Utils.timeFromTicks(200, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource,
            MagicData playerMagicData) {
        float radius = 10.0f;
        Vec3 center = entity.position();

        // 1. Frostwave Visual Effects (10 blocks radius)
        MagicManager.spawnParticles(level,
                new BlastwaveParticleOptions(SchoolRegistry.ICE.get().getTargetingColor(), radius),
                center.x, center.y + 0.165f, center.z,
                1, 0, 0, 0, 0, true);

        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity,
                new ShockwaveParticlesPacket(new Vec3(center.x, center.y + 0.165f, center.z), radius,
                        ParticleRegistry.SNOWFLAKE_PARTICLE.get()));

        // 2. Debuff: dungeons_and_combat:frostbite 10 seconds (200 ticks) to targets in
        // 10-block radius
        MobEffect frostbite = ForgeRegistries.MOB_EFFECTS
                .getValue(ResourceLocation.fromNamespaceAndPath("dungeons_and_combat", "frostbite"));
        if (frostbite != null && !level.isClientSide) {
            level.getEntities(entity, entity.getBoundingBox().inflate(radius, 4, radius),
                    (target) -> !DamageSources.isFriendlyFireBetween(target, entity)
                            && Utils.hasLineOfSight(level, entity, target, true))
                    .forEach(target -> {
                        if (target instanceof LivingEntity livingEntity
                                && livingEntity.distanceToSqr(entity) <= radius * radius) {
                            livingEntity.addEffect(new MobEffectInstance(frostbite, 200, 0));
                            MagicManager.spawnParticles(level, ParticleHelper.SNOWFLAKE, livingEntity.getX(),
                                    livingEntity.getY() + livingEntity.getBbHeight() * 0.5f, livingEntity.getZ(), 30,
                                    livingEntity.getBbWidth() * 0.5f, livingEntity.getBbHeight() * 0.5f,
                                    livingEntity.getBbWidth() * 0.5f, 0.03, false);
                        }
                    });
        }

        // 3. Ice Spikes in 8 directions up to 10 blocks
        if (!level.isClientSide) {
            float damage = getDamage(spellLevel, entity);
            int directions = 8;
            double angleStep = (2 * Math.PI) / directions;

            Vec3 lookVec = entity.getLookAngle();
            double baseAngle = Math.atan2(lookVec.z, lookVec.x);
            if (Math.abs(lookVec.x) < 1.0E-5D && Math.abs(lookVec.z) < 1.0E-5D) {
                baseAngle = Math.toRadians(entity.getYRot() + 90.0F);
            }

            for (int dirIndex = 0; dirIndex < directions; dirIndex++) {
                double angle = baseAngle + (dirIndex * angleStep);
                Vec3 dirVec = new Vec3(Math.cos(angle), 0, Math.sin(angle)).normalize();

                int stepIndex = 0;
                for (float dist = 2.0f; dist <= radius; dist += 1.25f, stepIndex++) {
                    Vec3 targetPos = center.add(dirVec.scale(dist));
                    Vec3 groundPos = Utils.moveToRelativeGroundLevel(level, targetPos, 4);

                    BlockPos belowPos = BlockPos.containing(groundPos).below();
                    if (level.getBlockState(belowPos).isFaceSturdy(level, belowPos, Direction.UP)) {
                        IceSpikeEntity spike = new IceSpikeEntity(level, entity);
                        float scale = Mth.lerp(dist / radius, 1.0f, 2.2f);
                        spike.setSpikeSize(scale);
                        spike.moveTo(groundPos);
                        spike.setDamage(damage);
                        spike.setWaitTime(stepIndex + 1);
                        float dirYaw = (float) Math.toDegrees(Math.atan2(-dirVec.x, dirVec.z));
                        spike.setYRot(dirYaw - 45.0f + Utils.random.nextIntBetweenInclusive(-15, 15));
                        spike.setXRot(Utils.random.nextIntBetweenInclusive(-10, 10));
                        spike.setSilent(true);
                        level.addFreshEntity(spike);
                    }
                }
            }
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    public float getDamage(int spellLevel, LivingEntity entity) {
        float base = SpellConfig.GlacialVeil.getBaseDamage();
        float perLevel = SpellConfig.GlacialVeil.getDamagePerLevel();
        return (base + (spellLevel - 1) * perLevel) * getEntityPowerMultiplier(entity);
    }

    @Override
    public boolean shouldAIStopCasting(int spellLevel, Mob mob, LivingEntity target) {
        return mob.distanceToSqr(target) > (10.0f * 10.0f);
    }
}