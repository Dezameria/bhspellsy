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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

@AutoSpellConfig
public class CrimsonThornbindSpell extends AbstractSpell {
    private final ResourceLocation spellId = new ResourceLocation(IronSpellMore.MODID, "crimson_thornbind");

    public static final int ROOT_SEGMENTS = CrimsonRootEntity.MAX_PATH_SEGMENTS;
    public static final float SEGMENT_SPACING = CrimsonRootEntity.SEGMENT_SPACING;
    public static final int WARMUP_DELAY_PER_SEGMENT = CrimsonRootEntity.STEP_INTERVAL;
    public static final int PATH_HOLD_TICKS = CrimsonRootEntity.PATH_HOLD_TICKS;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.FIRE_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(25)
            .build();

    public CrimsonThornbindSpell() {
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 10;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 40;
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
        return 12.0F + (spellLevel - 1) * 2.5F + (getSpellPower(spellLevel, caster) - 1.0F) * 1.5F;
    }

    public float getRendDamage(int spellLevel, LivingEntity caster) {
        return 6.0F + (spellLevel - 1) * 1.5F + (getSpellPower(spellLevel, caster) - 1.0F) * 0.8F;
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData playerMagicData) {
        return true;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide) {
            Vec3 look = entity.getLookAngle();
            Vec3 flatDir = new Vec3(look.x, 0, look.z).normalize();
            if (flatDir.lengthSqr() < 1e-4) {
                float yawRadians = entity.getYRot() * Mth.DEG_TO_RAD;
                flatDir = new Vec3(-Mth.sin(yawRadians), 0.0D, Mth.cos(yawRadians));
            }

            Vec3 start = getHandOrigin(entity, flatDir);
            float baseYaw = (float) (Mth.atan2(flatDir.z, flatDir.x) * (180.0D / Math.PI)) - 90.0F;
            float damage = getDamage(spellLevel, entity);
            float rendDamage = getRendDamage(spellLevel, entity);

            CrimsonRootEntity coordinator = new CrimsonRootEntity(level, entity);
            coordinator.setMode(CrimsonRootEntity.RootMode.PATH);
            coordinator.moveTo(start.x, start.y, start.z, baseYaw, 0.0F);
            coordinator.setWarmup(0);
            coordinator.setBaseScale(CrimsonRootEntity.DEFAULT_PATH_SCALE);
            coordinator.setDamage(damage);
            coordinator.setRendDamage(rendDamage);
            coordinator.initCoordinator(start, ROOT_SEGMENTS, damage, rendDamage);
            level.addFreshEntity(coordinator);

            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundRegistry.ROOT_EMERGE.get(), SoundSource.PLAYERS, 1.2F, 0.8F);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.PLAYERS, 1.2F, 0.8F);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private static Vec3 getHandOrigin(LivingEntity caster, Vec3 flatDirection) {
        Vec3 right = new Vec3(-flatDirection.z, 0.0D, flatDirection.x);
        double handSide = caster.getMainArm() == HumanoidArm.RIGHT ? 0.32D : -0.32D;
        return caster.getEyePosition()
                .add(0.0D, -0.45D, 0.0D)
                .add(flatDirection.scale(0.45D))
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
