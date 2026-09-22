package io.redspace.ironspell_more.spells.nature;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.entity.spells.rapturous_bloom.RapturousBloomEntity;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

@AutoSpellConfig
public class RapturousBloomSpell extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(IronSpellMore.MODID, "rapturous_bloom");

    private static final float RANGE = 28.0F;
    private static final int MAX_ACTIVE_BLOOMS = 3;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.NATURE_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(16.0)
            .build();

    public RapturousBloomSpell() {
        this.baseSpellPower = 24;
        this.spellPowerPerLevel = 4;
        this.baseManaCost = 45;
        this.manaCostPerLevel = 5;
        this.castTime = 0;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getBurstDamage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.aoe_damage", Utils.stringTruncation(getBurstDamage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(RANGE, 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RapturousBloomEntity.DEFAULT_RADIUS, 1)),
                Component.translatable("ui.irons_spellbooks.effect_length", Utils.timeFromTicks(160, 1))
        );
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.ENCHANTMENT_TABLE_USE);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.AMETHYST_BLOCK_CHIME);
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_INSTANT_CAST;
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData playerMagicData) {
        if (level.isClientSide) {
            return true;
        }

        // 1. Validate concurrent bloom limit (max 3)
        if (RapturousBloomEntity.getActiveBloomCount(caster.getUUID()) >= MAX_ACTIVE_BLOOMS) {
            displayActionBar(caster, "ui.ironspell_more.rapturous_bloom_max_active");
            return false;
        }

        // 2. Target acquisition
        LivingEntity target = findLookTarget(level, caster, RANGE);
        if (target == null) {
            displayActionBar(caster, "ui.ironspell_more.rapturous_bloom_no_target");
            return false;
        }

        // 3. Ground resolution
        Vec3 groundPos = RapturousBloomEntity.findGroundPosition(level, target.position(), 16);
        if (groundPos == null) {
            displayActionBar(caster, "ui.ironspell_more.rapturous_bloom_no_ground");
            return false;
        }

        // 4. Anti-stacking check (6 blocks minimum center distance)
        if (RapturousBloomEntity.isTargetOrAreaOccupied(level, target.getUUID(), groundPos, RapturousBloomEntity.MIN_NON_OVERLAP_DISTANCE)) {
            displayActionBar(caster, "ui.ironspell_more.rapturous_bloom_already_blooming");
            return false;
        }

        return true;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide) {
            LivingEntity target = findLookTarget(level, entity, RANGE);
            if (target != null) {
                Vec3 groundPos = RapturousBloomEntity.findGroundPosition(level, target.position(), 16);
                if (groundPos != null) {
                    if (RapturousBloomEntity.getActiveBloomCount(entity.getUUID()) < MAX_ACTIVE_BLOOMS
                            && !RapturousBloomEntity.isTargetOrAreaOccupied(level, target.getUUID(), groundPos, RapturousBloomEntity.MIN_NON_OVERLAP_DISTANCE)) {

                        RapturousBloomEntity bloom = new RapturousBloomEntity(level);
                        bloom.setPos(groundPos.x, groundPos.y, groundPos.z);
                        bloom.setOwner(entity);
                        bloom.setCasterUUID(entity.getUUID());
                        bloom.setLockedTargetUUID(target.getUUID());
                        bloom.setSpellLevel(spellLevel);
                        bloom.setBurstDamage(getBurstDamage(spellLevel, entity));

                        level.addFreshEntity(bloom);
                    }
                }
            }
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    public float getBurstDamage(int spellLevel, LivingEntity caster) {
        return (baseSpellPower + (spellLevel - 1) * spellPowerPerLevel) * getEntityPowerMultiplier(caster);
    }

    @Nullable
    private LivingEntity findLookTarget(Level level, LivingEntity caster, float range) {
        HitResult hitResult = Utils.raycastForEntity(level, caster, range, true, 0.6F);
        if (hitResult instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof LivingEntity livingTarget
                && isValidTarget(caster, livingTarget)) {
            return livingTarget;
        }

        Vec3 eyePos = caster.getEyePosition();
        Vec3 lookVec = caster.getLookAngle();
        AABB searchBox = caster.getBoundingBox().inflate(range);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, searchBox,
                entity -> isValidTarget(caster, entity)
                        && entity.distanceToSqr(caster) <= (double) (range * range));

        LivingEntity bestTarget = null;
        double bestScore = 0.75D;
        for (LivingEntity candidate : candidates) {
            Vec3 toCandidate = candidate.getBoundingBox().getCenter().subtract(eyePos).normalize();
            double dot = lookVec.dot(toCandidate);
            if (dot > bestScore && Utils.hasLineOfSight(level, caster, candidate, true)) {
                bestScore = dot;
                bestTarget = candidate;
            }
        }
        return bestTarget;
    }

    private boolean isValidTarget(LivingEntity caster, LivingEntity target) {
        return target != caster
                && target.isAlive()
                && !target.isRemoved()
                && !target.isSpectator()
                && !target.isAlliedTo(caster)
                && !DamageSources.isFriendlyFireBetween(caster, target);
    }

    private static void displayActionBar(LivingEntity caster, String translationKey) {
        if (caster instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable(translationKey), true);
        }
    }
}
