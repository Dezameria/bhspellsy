package io.redspace.ironspell_more.effect;

import io.redspace.ironspell_more.entity.spells.gale_drive.GaleDriveVortexEntity;
import io.redspace.ironspell_more.registry.EntityRegistry;
import io.redspace.ironspell_more.registry.SpellRegistry;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class GaleDriveDashEffect extends MobEffect {
    private static final String HIT_TAG = "GaleDriveHit";
    private static final String YAW_TAG = "GaleDriveYaw";
    private static final String PITCH_TAG = "GaleDrivePitch";
    private static final String DIR_X_TAG = "GaleDriveDirX";
    private static final String DIR_Y_TAG = "GaleDriveDirY";
    private static final String DIR_Z_TAG = "GaleDriveDirZ";

    public GaleDriveDashEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public void addAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        super.addAttributeModifiers(entity, attributeMap, amplifier);
        entity.getPersistentData().putBoolean(HIT_TAG, false);

        // ล็อกมุมมองและทิศทางการพุ่งเริ่มต้นไว้ เพื่อให้พุ่งตรงดิ่ง
        // ไม่สามารถหันเหทิศทางได้
        float yaw = entity.getYRot();
        float pitch = entity.getXRot();
        entity.getPersistentData().putFloat(YAW_TAG, yaw);
        entity.getPersistentData().putFloat(PITCH_TAG, pitch);

        Vec3 look = entity.getLookAngle();
        double yMotion = entity.onGround() && look.y <= 0 ? 0.05 : look.y;
        Vec3 fixedDir = new Vec3(look.x, yMotion, look.z).normalize().scale(1.35);
        entity.getPersistentData().putDouble(DIR_X_TAG, fixedDir.x);
        entity.getPersistentData().putDouble(DIR_Y_TAG, fixedDir.y);
        entity.getPersistentData().putDouble(DIR_Z_TAG, fixedDir.z);

        if (entity instanceof Player player) {
            player.startAutoSpinAttack(12);
        }
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);
        entity.getPersistentData().remove(HIT_TAG);
        entity.getPersistentData().remove(YAW_TAG);
        entity.getPersistentData().remove(PITCH_TAG);
        entity.getPersistentData().remove(DIR_X_TAG);
        entity.getPersistentData().remove(DIR_Y_TAG);
        entity.getPersistentData().remove(DIR_Z_TAG);

        // หลังจากที่พุ่งเสร็จจะโดน debuff Slowness 1 เป็นเวลา 5 วินาที (100 ticks)
        if (!entity.level().isClientSide) {
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0, false, false, true));
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        var level = entity.level();

        // ตรึงมุมหน้า (Yaw / Pitch) ของผู้เล่นให้หันตรงตามทิศทางพุ่งเดิมตลอดเวลา
        if (entity.getPersistentData().contains(YAW_TAG)) {
            float lockedYaw = entity.getPersistentData().getFloat(YAW_TAG);
            float lockedPitch = entity.getPersistentData().getFloat(PITCH_TAG);
            entity.setYRot(lockedYaw);
            entity.setXRot(lockedPitch);
            entity.yRotO = lockedYaw;
            entity.xRotO = lockedPitch;
            entity.yHeadRot = lockedYaw;
            entity.yHeadRotO = lockedYaw;
            entity.yBodyRot = lockedYaw;
            entity.yBodyRotO = lockedYaw;
        }

        if (entity instanceof Player player) {
            player.startAutoSpinAttack(12);
        }

        // อนุภาคขณะพุ่ง
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    entity.getX(), entity.getY() + entity.getBbHeight() / 2.0, entity.getZ(),
                    4, 0.3, 0.2, 0.3, 0.05);
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    entity.getX(), entity.getY() + entity.getBbHeight() / 2.0, entity.getZ(),
                    1, 0.1, 0.1, 0.1, 0.0);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    entity.getX(), entity.getY() + entity.getBbHeight() / 2.0, entity.getZ(),
                    3, 0.2, 0.2, 0.2, 0.04);
        }

        // ขับเคลื่อนแรงพุ่งตรงดิ่งตามทิศทางที่ล็อกไว้ตั้งแต่เริ่มร่าย
        double vx = entity.getPersistentData().getDouble(DIR_X_TAG);
        double vy = entity.getPersistentData().getDouble(DIR_Y_TAG);
        double vz = entity.getPersistentData().getDouble(DIR_Z_TAG);
        if (vx != 0 || vy != 0 || vz != 0) {
            entity.setDeltaMovement(vx, vy, vz);
        } else {
            Vec3 forward = entity.getLookAngle();
            double yMotion = entity.onGround() && forward.y <= 0 ? 0.05 : forward.y;
            entity.setDeltaMovement(new Vec3(forward.x, yMotion, forward.z).normalize().scale(1.35));
        }
        entity.hurtMarked = true;

        // ตรวจจับและชนเป้าหมาย (ล็อกเป้าหมายเพียง 1 ตัวแรกเท่านั้น)
        if (!level.isClientSide && !entity.getPersistentData().getBoolean(HIT_TAG)) {
            AABB hitBox = entity.getBoundingBox().inflate(1.2, 0.6, 1.2);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, hitBox,
                    target -> target != entity && target.isAlive()
                            && !DamageSources.isFriendlyFireBetween(entity, target));

            for (LivingEntity livingTarget : targets) {
                // มาร์กไว้ว่าชนแล้ว 1 เป้าหมาย
                entity.getPersistentData().putBoolean(HIT_TAG, true);

                // สร้าง Damage จากการพุ่งชนตามค่า Spell Power (amplifier)
                DamageSources.ignoreNextKnockback(livingTarget);
                var damageSource = SpellRegistry.GALE_DRIVE_SPELL.get().getDamageSource(entity);
                DamageSources.applyDamage(livingTarget, (float) amplifier, damageSource);
                livingTarget.invulnerableTime = 10;

                // เสกพายุหมุน GaleDriveVortexEntity ที่จุดยืนของเป้าหมาย
                GaleDriveVortexEntity vortex = new GaleDriveVortexEntity(EntityRegistry.GALE_DRIVE_VORTEX.get(), level);
                vortex.moveTo(livingTarget.getX(), livingTarget.getY(), livingTarget.getZ());
                vortex.setOwner(entity);
                vortex.setCapturedTarget(livingTarget);
                level.addFreshEntity(vortex);

                // เล่นเสียงเมื่อพายุหมุนปะทะเป้าหมาย
                level.playSound(null, livingTarget.getX(), livingTarget.getY(), livingTarget.getZ(),
                        SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 1.5f, 1.0f);
                break;
            }
        }

        // ชนกำแพงบล็อกแล้วหยุดพุ่ง
        if (entity.horizontalCollision) {
            entity.removeEffect(this);
        }
        entity.fallDistance = 0;
    }
}
