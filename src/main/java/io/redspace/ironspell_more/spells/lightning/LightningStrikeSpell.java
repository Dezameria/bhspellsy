package io.redspace.ironspell_more.spells.lightning;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.client.particle.ShockwaveParticleOptionCustom;
import io.redspace.ironspell_more.client.particle.ZapParticleOptionCustom;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.CameraShakeData;
import io.redspace.ironsspellbooks.api.util.CameraShakeManager;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import io.redspace.ironspell_more.config.SpellConfig;

import java.util.Optional;

@AutoSpellConfig
public class LightningStrikeSpell extends AbstractSpell {
    // ==========================================
    // SPELL TUNING CONSTANTS (Code Defaults)
    // ==========================================
    public static final float BASE_DAMAGE = 10.0F;
    public static final float DAMAGE_PER_LEVEL = 2.0F;
    public static final int BASE_MANA_COST = 50;
    public static final int MANA_COST_PER_LEVEL = 5;
    public static final double COOLDOWN_SECONDS = 15.0;

    private final ResourceLocation spellId = new ResourceLocation(IronSpellMore.MODID, "lightning_strike");

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage",
                        Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.distance",
                        Utils.stringTruncation(getDistance(spellLevel, caster), 2)));
    }

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.LIGHTNING_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(COOLDOWN_SECONDS)
            .build();

    public LightningStrikeSpell() {
        this.manaCostPerLevel = MANA_COST_PER_LEVEL;
        this.baseSpellPower = (int) BASE_DAMAGE;
        this.spellPowerPerLevel = (int) DAMAGE_PER_LEVEL;
        this.castTime = 0;
        this.baseManaCost = BASE_MANA_COST;
    }

    @Override
    public int getManaCost(int spellLevel) {
        return SpellConfig.LightningStrike.getBaseMana() + (spellLevel - 1) * SpellConfig.LightningStrike.getManaPerLevel();
    }

    @Override
    public int getSpellCooldown() {
        return (int) (SpellConfig.LightningStrike.getCooldown() * 20);
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
        return Optional.of(SoundRegistry.LIGHTNING_CAST.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.LIGHTNING_CAST.get());
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource,
            MagicData playerMagicData) {
        float distance = getDistance(spellLevel, entity);
        Vec3 startPos = entity.getBoundingBox().getCenter();
        Vec3 lookAngle = entity.getLookAngle();
        Vector3f lookVec = new Vector3f((float) lookAngle.x, (float) lookAngle.y, (float) lookAngle.z);

        // Raycast หาเป้าหมายตรงหน้าที่หันหน้าไปหา
        HitResult hitResult = Utils.raycastForEntity(level, entity, distance, true);
        Vec3 destPos;
        Entity hitTarget = null;

        if (hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHitResult = (EntityHitResult) hitResult;
            Entity target = entityHitResult.getEntity();
            hitTarget = target;

            // คำนวณตำแหน่งจุดยืนปลายทางให้อยู่ห่างจาก target เล็กน้อยตามทิศทางพุ่ง
            // เพื่อไม่ให้ตัวทับกัน
            Vec3 dirToTarget = target.position().subtract(startPos);
            Vec3 horizontalDir = new Vec3(dirToTarget.x, 0, dirToTarget.z);
            if (horizontalDir.lengthSqr() > 0.0001) {
                horizontalDir = horizontalDir.normalize();
                destPos = target.position().subtract(horizontalDir.scale(1.5));
            } else {
                destPos = target.position();
            }

            if (target instanceof LivingEntity livingEntity && canHit(entity, target)) {
                float damage = getDamage(spellLevel, entity);
                DamageSources.applyDamage(target, damage, getDamageSource(entity));
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                        SoundSource.PLAYERS, 2.0f, 2.0f);
                // เมื่อพุ่งโดนเป้าหมาย Entity และเกิด Damage
                if (level instanceof ServerLevel serverLevel) {
                    float radius = 3.5f;
                    Vector3f centerColor = new Vector3f(1.0f, 0.4f, 0.8f); // สีชมพูสว่างแกนกลาง (Hot Pink)
                    Vector3f edgeColor = new Vector3f(0.9f, 0.1f, 0.6f); // สีชมพูเข้มขอบสายฟ้า (Electric Magenta)

                    double targetX = livingEntity.getX();
                    double targetY = livingEntity.getY() + livingEntity.getBbHeight() * 0.5;
                    double targetZ = livingEntity.getZ();

                    // 1. 3D Directional Shockwave Multi-Rings (สีชมพูตั้งฉากตามทิศทางพุ่ง 100%)
                    for (ServerPlayer player : serverLevel.players()) {
                        serverLevel.sendParticles(player,
                                new ShockwaveParticleOptionCustom(centerColor, radius, true, lookVec), true, targetX,
                                targetY, targetZ, 1, 0, 0, 0, 0);
                    }
                    // 2. Electric Burst Sparks กระจายรอบแรงปะทะ
                    MagicManager.spawnParticles(serverLevel, ParticleHelper.ELECTRICITY, targetX, targetY, targetZ, 50,
                            0.25, 0.25, 0.25, 0.7f + radius * 0.1f, false);

                    // 2. Electric Pink Lightning Burst Sparks
                    Vec3 forwardDir = entity.getLookAngle().normalize();

                    // ปรับทิศทางตรงนี้: 1.0 (ไปข้างหน้า/ทะลุออกด้านหลังเป้าหมาย) หรือ -1.0
                    // (สะท้อนย้อนกลับ)
                    double directionMultiplier = 1.0;
                    Vec3 targetDir = forwardDir.scale(directionMultiplier);

                    double spread = 0.85;
                    int particleCount = 35;

                    for (int i = 0; i < particleCount; i++) {
                        double r = 2.5 + Utils.random.nextDouble() * 4.5;

                        // คำนวณ offset โดยอิงจาก targetDir ที่ปรับทิศทางแล้ว
                        double dx = (targetDir.x + (Utils.random.nextDouble() - 0.5) * spread) * r;
                        double dy = (targetDir.y + (Utils.random.nextDouble() - 0.5) * spread) * r;
                        double dz = (targetDir.z + (Utils.random.nextDouble() - 0.5) * spread) * r;

                        Vec3 sparkDest = new Vec3(targetX + dx, targetY + dy, targetZ + dz);
                        float zapLength = (float) (r * 0.7);

                        for (ServerPlayer otherPlayer : serverLevel.players()) {
                            serverLevel.sendParticles(otherPlayer, new ZapParticleOptionCustom(sparkDest, zapLength),
                                    true,
                                    targetX, targetY, targetZ, 1, 0, 0, 0, 0);
                        }
                    }

                    // 3. Camera Shake สั่นหน้าจอเมื่อพุ่งปะทะ
                    CameraShakeManager.addCameraShake(new CameraShakeData(10, livingEntity.position(), radius * 2.0f));

                    // 4. เสียงเอฟเฟกต์กระแทก Lightning Impact Sound
                    level.playSound(null, targetX, targetY, targetZ, SoundRegistry.LIGHTNING_LANCE_CAST.get(),
                            SoundSource.PLAYERS, 2.0f, 1.2f);
                }
            }
        } else if (hitResult.getType() == HitResult.Type.BLOCK) {
            destPos = hitResult.getLocation();
        } else {
            destPos = entity.getEyePosition().add(lookAngle.scale(distance));
        }

        // ยิงลำแสงสายฟ้า 3D สีชมพูพุ่งจากตำแหน่งเดิมไปยังจุดหมาย (ยิงเข้ากลางลำตัว/อก)
        if (level instanceof ServerLevel serverLevel) {
            float beamScale = 3.5f; // ปรับขนาดความหนาของลำแสงสายฟ้า 3D
            Vec3 beamDest = hitTarget != null ? hitTarget.getBoundingBox().getCenter() : destPos;
            for (ServerPlayer player : serverLevel.players()) {
                serverLevel.sendParticles(player, new ZapParticleOptionCustom(beamDest, beamScale), true, startPos.x,
                        startPos.y, startPos.z, 1, 0, 0, 0, 0);
            }
        }

        // Teleport ผู้ใช้ไปยังจุดหมาย
        entity.teleportTo(destPos.x, destPos.y, destPos.z);
        entity.resetFallDistance();

        // หันแค่ตัวเข้าหา target เมื่อ hit (ไม่เปลี่ยนมุมมองกล้อง/หัว)
        if (hitTarget != null) {
            double d0 = hitTarget.getX() - destPos.x;
            double d2 = hitTarget.getZ() - destPos.z;
            if (d0 * d0 + d2 * d2 > 0.0001) {
                float yaw = (float) (Math.toDegrees(Math.atan2(d2, d0)) - 90.0);
                entity.setYRot(yaw);
                entity.setYBodyRot(yaw);
            }
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private boolean canHit(Entity owner, Entity target) {
        return target != owner && target.isAlive() && target.isPickable() && !target.isSpectator();
    }

    public float getDistance(int spellLevel, LivingEntity caster) {
        return 12 + spellLevel * 2;
    }

    public float getDamage(int spellLevel, LivingEntity caster) {
        float base = SpellConfig.LightningStrike.getBaseDamage();
        float perLevel = SpellConfig.LightningStrike.getDamagePerLevel();
        return (base + (spellLevel - 1) * perLevel) * getEntityPowerMultiplier(caster);
    }

    @Override
    public void playSound(Optional<SoundEvent> sound, Entity entity) {
        sound.ifPresent((soundEvent -> entity.playSound(soundEvent, 3.0f, .9f + Utils.random.nextFloat() * .2f)));
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
    }
}
