package io.redspace.ironspell_more.effect;

import io.redspace.ironspell_more.registry.SpellRegistry;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class SpinStrikeEffect extends MobEffect {
    public SpinStrikeEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    // 1. สั่งให้ผู้เล่นเข้าสู่ท่า Spin Attack ทันทีที่ได้รับ Effect
    @Override
    public void addAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        super.addAttributeModifiers(entity, attributeMap, amplifier);
        if (entity instanceof Player player) {
            player.startAutoSpinAttack(10);
        }
    }

    // 2. ปิดท่า Spin Attack เมื่อ Effect หมดเวลา
    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true; // ทำงานทุกๆ Tick
    }

    // 3. ทำงานทุก Tick: หมุนตัว พุ่งทะลุ Entity สร้างดาเมจ และเสก Particle
    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        var level = entity.level();

        // 1. หมุนตัวต่อเนื่องตลอดเวลาที่ยังมี Effect (ไม่ให้ท่าหมุนหลุดเมื่อชนมอน)
        if (entity instanceof Player player) {
            player.startAutoSpinAttack(10);
        }

        // 2. เสก Particle ไฟ/สายฟ้ารอบตัวขณะพุ่ง
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    entity.getX(), entity.getY() + entity.getBbHeight() / 2.0, entity.getZ(),
                    4, 0.3, 0.3, 0.3, 0.03);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    entity.getX(), entity.getY() + entity.getBbHeight() / 2.0, entity.getZ(),
                    4, 0.3, 0.3, 0.3, 0.05);
        }

        // 3. รักษาแรงพุ่งไปข้างหน้าอย่างต่อเนื่องทุก Tick
        // ทำให้พุ่งทะลุมอนสเตอร์ได้โดยไม่ชะงัก
        Vec3 forward = entity.getLookAngle();
        double yMotion = entity.onGround() && forward.y < 0 ? 0.05 : forward.y;
        Vec3 dashVelocity = new Vec3(forward.x, yMotion, forward.z).normalize().scale(1.4);
        entity.setDeltaMovement(dashVelocity);
        entity.hurtMarked = true;

        // 4. ค้นหา Entity รอบๆ ที่ตัวเราพุ่งผ่าน ทำดาเมจ (amplifier) และผลักออกด้านข้าง
        AABB hitBox = entity.getBoundingBox().inflate(1.2, 0.6, 1.2);
        List<Entity> targets = level.getEntities(entity, hitBox);

        for (Entity target : targets) {
            if (target instanceof LivingEntity livingTarget && target.isAlive() && !target.isAlliedTo(entity)
                    && target != entity) {
                // เช็คว่าศัตรูยังไม่ติด i-frame (ป้องกันการโดนดาเมจซ้ำซ้อนในเสี้ยววินาที)
                if (livingTarget.invulnerableTime <= 0 && livingTarget.hurtTime <= 0) {
                    // สร้าง Damage ตามค่าที่ส่งมาจาก SpinStrikeSpell (amplifier)
                    var damageSource = SpellRegistry.SPIN_STRIKE_SPELL.get().getDamageSource(entity);
                    DamageSources.applyDamage(livingTarget, (float) amplifier, damageSource);
                    livingTarget.invulnerableTime = 20; // ติดคูลดาวน์ป้องกันการโดนซ้ำตลอดการพุ่งรอบนี้

                    // ผลักศัตรูออกไปด้านข้าง เพื่อเปิดทางให้ตัวเราพุ่งทะลุผ่าน
                    Vec3 away = livingTarget.position().subtract(entity.position());
                    Vec3 knockback = new Vec3(away.x, 0.2, away.z).normalize().scale(0.6);
                    livingTarget.setDeltaMovement(knockback);
                    livingTarget.hurtMarked = true;
                }
            }
        }

        // 5. ชนกำแพงบล็อกแล้วหยุดพุ่ง และไม่รับ Fall Damage
        if (entity.horizontalCollision) {
            entity.removeEffect(this);
        }
        entity.fallDistance = 0;
    }
}
