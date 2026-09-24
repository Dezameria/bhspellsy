package io.redspace.ironspell_more.spells.fire;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.entity.spells.crimson_thornbind.CrimsonRootEntity;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

import io.redspace.ironspell_more.config.SpellConfig;

@AutoSpellConfig
public class CrimsonThornbindSpell extends AbstractSpell {
    // ==========================================
    // SPELL TUNING CONSTANTS (Code Defaults)
    // ==========================================
    public static final float BASE_DAMAGE = 12.0F;
    public static final float DAMAGE_PER_LEVEL = 2.5F;
    public static final float REND_BASE_DAMAGE = 6.0F;
    public static final float REND_DAMAGE_PER_LEVEL = 1.5F;
    public static final int BASE_MANA_COST = 40;
    public static final int MANA_COST_PER_LEVEL = 5;
    public static final double COOLDOWN_SECONDS = 25.0;

    private final ResourceLocation spellId = new ResourceLocation(IronSpellMore.MODID, "crimson_thornbind");

    public static final int ROOT_SEGMENTS = CrimsonRootEntity.MAX_PATH_SEGMENTS;
    public static final float SEGMENT_SPACING = CrimsonRootEntity.SEGMENT_SPACING;
    public static final int WARMUP_DELAY_PER_SEGMENT = CrimsonRootEntity.STEP_INTERVAL;
    public static final int PATH_HOLD_TICKS = CrimsonRootEntity.PATH_HOLD_TICKS;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.FIRE_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(COOLDOWN_SECONDS)
            .build();

    public CrimsonThornbindSpell() {
        this.manaCostPerLevel = MANA_COST_PER_LEVEL;
        this.baseSpellPower = (int) BASE_DAMAGE;
        this.spellPowerPerLevel = (int) DAMAGE_PER_LEVEL;
        this.castTime = 0;
        this.baseManaCost = BASE_MANA_COST;
    }

    @Override
    public int getManaCost(int spellLevel) {
        return SpellConfig.CrimsonThornbind.getBaseMana() + (spellLevel - 1) * SpellConfig.CrimsonThornbind.getManaPerLevel();
    }

    @Override
    public int getSpellCooldown() {
        return (int) (SpellConfig.CrimsonThornbind.getCooldown() * 20);
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
        return Optional.of(SoundEvents.PLAYER_ATTACK_SWEEP);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.ROOT_EMERGE.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.ironspell_more.crimson_thornbind_damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.ironspell_more.crimson_thornbind_rend_damage", Utils.stringTruncation(getRendDamage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(ROOT_SEGMENTS * SEGMENT_SPACING, 1)),
                Component.translatable("ui.ironspell_more.crimson_thornbind_duration", Utils.timeFromTicks(200, 1))
        );
    }

    public float getDamage(int spellLevel, LivingEntity caster) {
        float base = SpellConfig.CrimsonThornbind.getBaseDamage();
        float perLevel = SpellConfig.CrimsonThornbind.getDamagePerLevel();
        return base + (spellLevel - 1) * perLevel + (getSpellPower(spellLevel, caster) - 1.0F) * 1.5F;
    }

    public float getRendDamage(int spellLevel, LivingEntity caster) {
        float base = SpellConfig.CrimsonThornbind.getRendBaseDamage();
        float perLevel = SpellConfig.CrimsonThornbind.getRendDamagePerLevel();
        return base + (spellLevel - 1) * perLevel + (getSpellPower(spellLevel, caster) - 1.0F) * 0.8F;
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData playerMagicData) {
        return true;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide) {
            Vec3 lookDir = entity.getLookAngle().normalize();
            if (lookDir.lengthSqr() < 1e-4) {
                float yawRadians = entity.getYRot() * Mth.DEG_TO_RAD;
                float pitchRadians = entity.getXRot() * Mth.DEG_TO_RAD;
                lookDir = new Vec3(
                        -Mth.sin(yawRadians) * Mth.cos(pitchRadians),
                        -Mth.sin(pitchRadians),
                        Mth.cos(yawRadians) * Mth.cos(pitchRadians)
                ).normalize();
            }

            Vec3 start = getHandOrigin(entity, lookDir);

            double horizDist = Math.sqrt(lookDir.x * lookDir.x + lookDir.z * lookDir.z);
            float baseYaw = horizDist > 1e-4
                    ? (float) (Mth.atan2(-lookDir.x, lookDir.z) * (180.0D / Math.PI))
                    : entity.getYRot();
            float basePitch = (float) (-Mth.atan2(lookDir.y, horizDist) * (180.0D / Math.PI));

            float damage = getDamage(spellLevel, entity);
            float rendDamage = getRendDamage(spellLevel, entity);

            CrimsonRootEntity coordinator = new CrimsonRootEntity(level, entity);
            coordinator.setMode(CrimsonRootEntity.RootMode.PATH);
            coordinator.moveTo(start.x, start.y, start.z, baseYaw, basePitch);
            coordinator.setPathYaw(baseYaw);
            coordinator.setPathPitch(basePitch);
            coordinator.setWarmup(0);
            coordinator.setBaseScale(CrimsonRootEntity.DEFAULT_PATH_SCALE);
            coordinator.setDamage(damage);
            coordinator.setRendDamage(rendDamage);
            coordinator.setIsOrigin(true);
            coordinator.initCoordinator(start, lookDir, ROOT_SEGMENTS, damage, rendDamage);
            level.addFreshEntity(coordinator);

            if (level instanceof ServerLevel serverLevel) {
                spawnOriginBurst(serverLevel, start);
            }

            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundRegistry.ROOT_EMERGE.get(), SoundSource.PLAYERS, 1.2F, 0.8F);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.PLAYERS, 1.2F, 0.8F);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    public static void spawnOriginBurst(ServerLevel level, Vec3 pos) {
        // Ambient crimson spores floating around the fanned-out origin sheet
        level.sendParticles(ParticleTypes.CRIMSON_SPORE,
                pos.x, pos.y, pos.z, 20, 0.35D, 0.35D, 0.35D, 0.02D);
        // Bright crimson red magical dust
        level.sendParticles(new DustParticleOptions(new Vector3f(0.85F, 0.08F, 0.15F), 1.4F),
                pos.x, pos.y, pos.z, 16, 0.35D, 0.35D, 0.35D, 0.04D);
    }

    private static Vec3 getHandOrigin(LivingEntity caster, Vec3 direction) {
        double horizLen = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        Vec3 right;
        if (horizLen > 1e-4) {
            right = new Vec3(-direction.z / horizLen, 0.0D, direction.x / horizLen);
        } else {
            float yawRadians = caster.getYRot() * Mth.DEG_TO_RAD;
            right = new Vec3(Mth.cos(yawRadians), 0.0D, Mth.sin(yawRadians));
        }
        double handSide = caster.getMainArm() == HumanoidArm.RIGHT ? 0.32D : -0.32D;
        return caster.getEyePosition()
                .add(0.0D, -0.35D, 0.0D)
                .add(direction.scale(0.45D))
                .add(right.scale(handSide));
    }

    public static double findGroundHeight(Level level, double x, double prevY, double z) {
        BlockPos base = BlockPos.containing(x, prevY, z);
        for (int dy = 1; dy >= -2; dy--) {
            BlockPos check = base.above(dy);
            BlockState state = level.getBlockState(check);
            BlockState above = level.getBlockState(check.above());
            if (!state.getCollisionShape(level, check).isEmpty() && above.getCollisionShape(level, check.above()).isEmpty()) {
                return check.getY() + state.getCollisionShape(level, check).max(net.minecraft.core.Direction.Axis.Y);
            }
        }
        return prevY;
    }

    @Nullable
    public static TargetHit findTargetAlongSegment(Level level, LivingEntity caster, Vec3 start, Vec3 end) {
        return findFirstTarget(level, caster, start, end);
    }

    @Nullable
    public static TargetHit findFirstTarget(Level level, LivingEntity caster, Vec3 start, Vec3 end) {
        AABB searchBox = new AABB(start, end).inflate(0.75D);
        LivingEntity nearestTarget = null;
        Vec3 nearestLocation = null;
        double nearestDistanceSqr = Double.MAX_VALUE;
        Vec3 direction = end.subtract(start);
        double clippedDistance = direction.length();
        if (clippedDistance <= 1.0E-6D) {
            return null;
        }
        direction = direction.scale(1.0D / clippedDistance);

        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, searchBox,
                target -> isValidTarget(caster, target))) {
            if (nearestProjection(candidate.getBoundingBox(), start, direction) > clippedDistance + 1.0E-4D
                    || isOccluded(level, caster, start, candidate.getBoundingBox().getCenter())) {
                continue;
            }
            Optional<Vec3> intersection = candidate.getBoundingBox().inflate(0.45D).clip(start, end);
            if (intersection.isEmpty()) {
                continue;
            }
            double distanceSqr = start.distanceToSqr(intersection.get());
            if (distanceSqr < nearestDistanceSqr) {
                nearestDistanceSqr = distanceSqr;
                nearestTarget = candidate;
                nearestLocation = intersection.get();
            }
        }
        return nearestTarget == null ? null : new TargetHit(nearestTarget, nearestLocation);
    }

    private static double nearestProjection(AABB box, Vec3 start, Vec3 direction) {
        double nearestX = direction.x >= 0.0D ? box.minX : box.maxX;
        double nearestY = direction.y >= 0.0D ? box.minY : box.maxY;
        double nearestZ = direction.z >= 0.0D ? box.minZ : box.maxZ;
        return new Vec3(nearestX, nearestY, nearestZ).subtract(start).dot(direction);
    }

    private static boolean isOccluded(Level level, LivingEntity caster, Vec3 start, Vec3 targetPoint) {
        BlockHitResult occlusion = level.clip(new ClipContext(start, targetPoint,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        return occlusion.getType() == HitResult.Type.BLOCK
                && start.distanceToSqr(occlusion.getLocation()) + 1.0E-4D < start.distanceToSqr(targetPoint);
    }

    public static boolean isValidTarget(LivingEntity caster, LivingEntity target) {
        return target != caster
                && !(target instanceof CrimsonRootEntity)
                && target.isAlive()
                && !target.isRemoved()
                && !target.isSpectator()
                && !target.isAlliedTo(caster)
                && !DamageSources.isFriendlyFireBetween(caster, target);
    }

    public record TargetHit(LivingEntity target, Vec3 location) {
    }
}
