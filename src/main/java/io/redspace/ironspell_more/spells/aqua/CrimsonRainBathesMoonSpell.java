package io.redspace.ironspell_more.spells.aqua;

import com.gametechbc.traveloptics.api.init.TravelopticsSchools;
import com.gametechbc.traveloptics.api.spells.AbstractUniqueSpell;
import com.gametechbc.traveloptics.api.utils.TOGeneralUtils;
import com.gametechbc.traveloptics.entity.misc.TOFollowingScreenShakeEntity;
import com.gametechbc.traveloptics.init.TravelopticsSounds;
import com.github.L_Ender.cataclysm.client.particle.CircleLightningParticle;
import com.github.L_Ender.cataclysm.client.particle.StormParticle;
import com.github.L_Ender.cataclysm.init.ModParticle;
import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.entity.spells.crimson_rain_bathes_moon.CrimsonSpearEntity;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.network.casting.SyncTargetingDataPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@AutoSpellConfig
public class CrimsonRainBathesMoonSpell extends AbstractUniqueSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(IronSpellMore.MODID,
            "crimson_rain_bathes_moon");
    private final DefaultConfig defaultConfig;

    public CrimsonRainBathesMoonSpell() {
        this.defaultConfig = (new DefaultConfig())
                .setMinRarity(SpellRarity.EPIC)
                .setSchoolResource(TravelopticsSchools.AQUA_RESOURCE)
                .setMaxLevel(3)
                .setCooldownSeconds(60.0D)
                .build();
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 1;
        this.castTime = 200;
        this.baseManaCost = 5;
    }

    @Override
    public CastType getCastType() {
        return CastType.CONTINUOUS;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return this.defaultConfig;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return this.spellId;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.LIGHTNING_BOLT_THUNDER);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public void onCast(Level world, int spellLevel, LivingEntity entity, CastSource castSource,
            MagicData playerMagicData) {
        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onServerCastTick(Level world, int spellLevel, LivingEntity entity,
            @Nullable MagicData playerMagicData) {
        if (playerMagicData != null) {
            Vec3 stormCenter = new Vec3(entity.getX(), entity.getY(), entity.getZ());
            float radius = this.getStormRadius(spellLevel);
            int currentCastTime = this.castTime - playerMagicData.getCastDurationRemaining();
            double stormHeight = 8.0D;

            // Phase 1: Charging Lightning (Blue Lightning)
            if (currentCastTime >= 15 && currentCastTime < 40) {
                this.spawnChargingLightning(world, stormCenter, stormHeight * 2.0D, radius, currentCastTime);
            }

            // Phase 2: Buildup (Rain clouds & aqua sparkles)
            if (currentCastTime >= 40 && currentCastTime <= 70) {
                this.spawnStormBuildupEffects(world, entity, stormCenter, stormHeight, radius, currentCastTime);
            }

            // Expanding massive vortex & outward wind gusts pushing enemies (Ticks 40 to
            // 200)
            if (currentCastTime >= 40) {
                this.spawnExpandingStormWind(world, entity, stormCenter, radius, currentCastTime);
                this.applyStormKnockback(world, entity, stormCenter, radius);
            }

            // Phase 3: Crimson Spear Barrage (Ticks 70 to 200)
            if (currentCastTime >= 70) {
                this.spawnStormBarrage(world, entity, stormCenter, stormHeight, radius, spellLevel, currentCastTime);
            }

            // Sync visual target lock-on outline to player while channeling barrage
            if (currentCastTime >= 70 && entity instanceof ServerPlayer player) {
                LivingEntity lookTarget = this.findLookTarget(world, entity, this.getRange());
                if (lookTarget != null) {
                    PacketDistributor.sendToPlayer(player, new SyncTargetingDataPacket(lookTarget, this));
                } else {
                    PacketDistributor.sendToPlayer(player, new SyncTargetingDataPacket(this, Collections.emptyList()));
                }
            }

            this.applyScreenShake(world, entity, currentCastTime);
        }

        super.onServerCastTick(world, spellLevel, entity, playerMagicData);
    }

    @Override
    public void onServerCastComplete(Level level, int spellLevel, LivingEntity caster, MagicData magicData,
            boolean cancelled) {
        try {
            if (caster instanceof ServerPlayer player) {
                PacketDistributor.sendToPlayer(player, new SyncTargetingDataPacket(this, Collections.emptyList()));
            }
        } catch (Exception ignored) {
        }
        super.onServerCastComplete(level, spellLevel, caster, magicData, cancelled);
    }

    private void spawnChargingLightning(Level world, Vec3 center, double height, float radius, int castTime) {
        if (castTime % 6 == 0) {
            for (int i = 0; i < 4; ++i) {
                double angle = (Math.PI / 2.0D) * (double) i + (double) castTime * 0.15D;
                double lightningRadius = (double) radius * 8.5D;
                double posX = lightningRadius * Math.cos(angle);
                double posY = lightningRadius * 0.8D * Math.sin(angle) + 8.0D;
                double posZ = lightningRadius * Math.sin(angle);
                // Blue Lightning (RGB: 99, 194, 224)
                MagicManager.spawnParticles(world, new CircleLightningParticle.CircleData(99, 194, 224),
                        center.x + posX, center.y + height + posY, center.z + posZ, 0,
                        center.x, center.y + height, center.z, 1.0D, true);
            }
        }
    }

    private void spawnStormBuildupEffects(Level world, LivingEntity caster, Vec3 center, double height, float radius,
            int castTime) {
        // Vibrant Crimson Red StormParticles
        MagicManager.spawnParticles(world,
                new StormParticle.OrbData(0.95F, 0.08F, 0.15F,
                        2.5F + caster.getRandom().nextFloat() * 0.5F, 0.8F, caster.getId()),
                center.x, center.y, center.z, 2, 0.0D, 0.0D, 0.0D, 0.0D, true);
        MagicManager.spawnParticles(world,
                new StormParticle.OrbData(1.0F, 0.18F, 0.22F,
                        2.2F + caster.getRandom().nextFloat() * 0.3F, 0.8F, caster.getId()),
                center.x, center.y, center.z, 2, 0.0D, 0.0D, 0.0D, 0.0D, true);
        if (castTime % 2 == 0) {
            this.spawnRainClouds(world, caster, center, height, radius, 20, 15);
        }

        if (castTime % 5 == 0) {
            this.spawnCircleLightning(world, caster, center, height, radius);
        }
    }

    private void spawnExpandingStormWind(Level world, LivingEntity caster, Vec3 center, float maxRadius, int castTime) {
        // Storm gradually expands outward from tick 40 to tick 100, reaching full
        // radius
        float expansionProgress = Mth.clamp((float) (castTime - 40) / 60.0F, 0.35F, 1.0F);
        float currentRadius = maxRadius * expansionProgress;

        // 1. Swirling Storm Vortex (Red StormParticle Trails orbiting at expanding
        // layers)
        if (castTime % 2 == 0) {
            // Core swirling layer
            MagicManager.spawnParticles(world,
                    new StormParticle.OrbData(0.95F, 0.08F, 0.15F,
                            1.8F + caster.getRandom().nextFloat() * 1.5F, 0.5F + caster.getRandom().nextFloat() * 1.5F,
                            caster.getId()),
                    center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D, true);

            // Mid-radius expanding vortex layer
            float midRadius = 3.5F + caster.getRandom().nextFloat() * (currentRadius * 0.5F);
            MagicManager.spawnParticles(world,
                    new StormParticle.OrbData(1.0F, 0.18F, 0.22F,
                            midRadius, 0.8F + caster.getRandom().nextFloat() * 2.2F, caster.getId()),
                    center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D, true);

            // Outer perimeter storm ring
            if (currentRadius > 5.0F) {
                float outerRadius = currentRadius * (0.8F + caster.getRandom().nextFloat() * 0.2F);
                MagicManager.spawnParticles(world,
                        new StormParticle.OrbData(0.9F, 0.05F, 0.1F,
                                outerRadius, 0.3F + caster.getRandom().nextFloat() * 2.8F, caster.getId()),
                        center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D, true);
            }
        }

        // 2. Expanding Wind Gusts (กระแสลมแผ่ขยายออกเป็นวงกว้าง)
        int windStreams = 7;
        double baseAngle = (double) castTime * 0.22D;
        for (int i = 0; i < windStreams; ++i) {
            double angle = baseAngle + (Math.PI * 2.0D / (double) windStreams) * (double) i
                    + (caster.getRandom().nextDouble() - 0.5D) * 0.2D;
            double spawnDist = 1.0D + caster.getRandom().nextDouble() * (double) currentRadius * 0.85D;
            double px = center.x + Math.cos(angle) * spawnDist;
            double py = center.y + 0.2D + caster.getRandom().nextDouble() * 1.8D;
            double pz = center.z + Math.sin(angle) * spawnDist;

            // Outward radial velocity + tangential rotation
            double outwardSpeed = 0.35D + caster.getRandom().nextDouble() * 0.25D;
            double swirlSpeed = 0.18D;
            double vx = Math.cos(angle) * outwardSpeed - Math.sin(angle) * swirlSpeed;
            double vz = Math.sin(angle) * outwardSpeed + Math.cos(angle) * swirlSpeed;
            double vy = 0.03D + caster.getRandom().nextDouble() * 0.05D;

            MagicManager.spawnParticles(world, ParticleTypes.CLOUD, px, py, pz, 0, vx, vy, vz, 1.0D, false);
            if (caster.getRandom().nextFloat() < 0.35F) {
                MagicManager.spawnParticles(world, ParticleTypes.POOF, px, py, pz, 0, vx * 0.7D, vy, vz * 0.7D, 1.0D,
                        false);
            }
        }
    }

    private void spawnStormBarrage(Level world, LivingEntity caster, Vec3 center, double height, float radius,
            int spellLevel, int castTime) {
        if (castTime % 5 == 0) {
            world.playSound((Player) null, caster.getX(), caster.getY(), caster.getZ(),
                    TravelopticsSounds.AQUA_CAST_2.get(), SoundSource.NEUTRAL, 2.0F,
                    0.8F + caster.getRandom().nextFloat() * 0.4F);
        }

        if (castTime % 8 == 0) {
            LivingEntity target = this.findLookTarget(world, caster, this.getRange());
            Vec3 hitLocation;
            if (target != null) {
                hitLocation = target.getBoundingBox().getCenter();
            } else {
                HitResult hitResult = Utils.raycastForEntity(world, caster, this.getRange(), true, 0.6F);
                hitLocation = hitResult.getLocation();
            }

            int spearCount = this.getSpearCount();

            for (int i = 0; i < spearCount; ++i) {
                float angle = caster.getRandom().nextFloat() * (float) Math.PI * 2.0F;
                float distance = Mth.sqrt(caster.getRandom().nextFloat()) * radius;
                double spearX = center.x + Math.cos(angle) * (double) distance;
                double spearZ = center.z + Math.sin(angle) * (double) distance;
                double spearY = center.y + height;
                this.spawnStormSpear(world, caster, spearX, spearY, spearZ, spellLevel, target, hitLocation);
            }
        }

        if (castTime % 2 == 0) {
            this.spawnRainClouds(world, caster, center, height, radius, 25, 20);
        }
    }

    private void spawnStormSpear(Level world, LivingEntity caster, double x, double y, double z, int spellLevel,
            @Nullable LivingEntity target, Vec3 hitLocation) {
        Vec3 spearPos = new Vec3(x, y, z);
        Vec3 targetPos;
        if (target != null && target.isAlive()) {
            Vec3 targetCenter = target.getBoundingBox().getCenter();
            Vec3 targetVelocity = target.getDeltaMovement();
            double dist = spearPos.distanceTo(targetCenter);
            double speedEstimate = 1.6D;
            double travelTime = dist / speedEstimate;
            Vec3 predictedPos = targetCenter.add(targetVelocity.scale(travelTime));
            // Slight natural scatter around target center (0.35m) so spears rain down
            // aesthetically without clipping
            targetPos = predictedPos.add(
                    (caster.getRandom().nextDouble() - 0.5D) * 0.35D,
                    (caster.getRandom().nextDouble() - 0.5D) * 0.25D,
                    (caster.getRandom().nextDouble() - 0.5D) * 0.35D);
        } else {
            targetPos = hitLocation.add(
                    (caster.getRandom().nextDouble() - 0.5D) * 1.2D,
                    (caster.getRandom().nextDouble() - 0.5D) * 0.8D,
                    (caster.getRandom().nextDouble() - 0.5D) * 1.2D);
        }

        double deltaX = targetPos.x - x;
        double deltaY = targetPos.y - y;
        double deltaZ = targetPos.z - z;
        Vec3 direction = (new Vec3(deltaX, deltaY, deltaZ)).normalize();
        float yRot = (float) (Mth.atan2(direction.z, direction.x) * 180.0D / Math.PI) + 90.0F;
        float xRot = (float) (-Mth.atan2(direction.y, Math.sqrt(direction.x * direction.x + direction.z * direction.z))
                * 180.0D / Math.PI);

        // Spawn custom CrimsonSpearEntity with red visuals & trail
        CrimsonSpearEntity spear = new CrimsonSpearEntity(caster, direction, world, this.getDamage(spellLevel, caster));
        spear.accelerationPower = 0.12D + (double) spellLevel * 0.008D;
        spear.setYRot(yRot);
        spear.setXRot(xRot);
        spear.moveTo(x, y, z);
        spear.setTotalBounces(2 + spellLevel);
        if (target != null && target.isAlive()) {
            spear.setHomingTarget(target);
        }
        world.addFreshEntity(spear);
    }

    @Nullable
    private LivingEntity findLookTarget(Level world, LivingEntity caster, float range) {
        // 1. Direct crosshair raycast with generous hitbox inflate (0.6F)
        HitResult hitResult = Utils.raycastForEntity(world, caster, range, true, 0.6F);
        if (hitResult instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof LivingEntity livingTarget) {
            if (isValidTarget(caster, livingTarget)) {
                return livingTarget;
            }
        }

        // 2. Cone search in look direction for valid targets within LOS
        Vec3 eyePos = caster.getEyePosition();
        Vec3 lookVec = caster.getLookAngle();
        AABB searchBox = caster.getBoundingBox().inflate(range);
        List<LivingEntity> candidates = world.getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> isValidTarget(caster, e) && e.distanceToSqr(caster) <= (double) (range * range));

        LivingEntity bestTarget = null;
        double bestScore = 0.85D; // Within ~31 degree cone of crosshair

        for (LivingEntity candidate : candidates) {
            Vec3 toCandidate = candidate.getBoundingBox().getCenter().subtract(eyePos).normalize();
            double dot = lookVec.dot(toCandidate);
            if (dot > bestScore && Utils.hasLineOfSight(world, caster, candidate, true)) {
                bestScore = dot;
                bestTarget = candidate;
            }
        }
        return bestTarget;
    }

    private boolean isValidTarget(LivingEntity caster, LivingEntity target) {
        return target != caster
                && target.isAlive()
                && !target.isSpectator()
                && !DamageSources.isFriendlyFireBetween(caster, target)
                && !target.isAlliedTo(caster);
    }

    private void spawnRainClouds(Level world, LivingEntity caster, Vec3 center, double height, float radius, int amount,
            int randomAmount) {
        for (int j = 0; j < amount + caster.getRandom().nextInt(randomAmount); ++j) {
            float angle = caster.getRandom().nextFloat() * (float) Math.PI * 2.0F;
            float distance = caster.getRandom().nextFloat() * radius * 0.9F;
            double cloudX = center.x + Math.cos(angle) * (double) distance;
            double cloudY = center.y + height;
            double cloudZ = center.z + Math.sin(angle) * (double) distance;
            MagicManager.spawnParticles(world, (ParticleOptions) ModParticle.RAIN_CLOUD.get(), cloudX, cloudY, cloudZ,
                    1, caster.getRandom().nextGaussian() * 0.02D, caster.getRandom().nextGaussian() * 0.005D,
                    caster.getRandom().nextGaussian() * 0.02D, 0.0D, false);
        }
    }

    private void spawnCircleLightning(Level world, LivingEntity caster, Vec3 center, double height, float radius) {
        for (int i = 0; i < 3 + caster.getRandom().nextInt(1); ++i) {
            double theta = caster.getRandom().nextDouble() * 2.0D * Math.PI;
            double phi = caster.getRandom().nextDouble() * Math.PI;
            double lightningRadius = (double) radius * 0.7D;
            double posX = lightningRadius * Math.sin(phi) * Math.cos(theta);
            double posY = lightningRadius * Math.cos(phi);
            double posZ = lightningRadius * Math.sin(phi) * Math.sin(theta);
            // Light Blue / Aqua Lightning Sparkles (RGB: 143, 241, 215)
            MagicManager.spawnParticles(world, new CircleLightningParticle.CircleData(143, 241, 215),
                    center.x + posX, center.y + height + posY, center.z + posZ, 0,
                    center.x, center.y + height, center.z, 1.0D, true);
        }
    }

    private void applyStormKnockback(Level world, LivingEntity caster, Vec3 center, float radius) {
        for (Entity target : world.getEntitiesOfClass(Entity.class, caster.getBoundingBox().inflate(radius),
                (entity) -> entity != caster && !DamageSources.isFriendlyFireBetween(caster, entity)
                        && entity.distanceTo(caster) <= radius)) {
            double deltaX = target.getX() - center.x;
            double deltaZ = target.getZ() - center.z;
            double distance = Math.max(Math.sqrt(deltaX * deltaX + deltaZ * deltaZ), 0.001D);
            // Dynamic outward push with lift
            double force = target.isCrouching() ? 0.20D : 0.40D;
            target.push(deltaX / distance * force, 0.08D, deltaZ / distance * force);
            target.hurtMarked = true;
        }
    }

    private void applyScreenShake(Level world, LivingEntity caster, int castTime) {
        if (castTime == 25) {
            TOFollowingScreenShakeEntity.createFollowingScreenShake(world, caster, 15.0F, 0.005F, 5, 10, 0, true);
        } else if (castTime == 40) {
            TOFollowingScreenShakeEntity.createFollowingScreenShake(world, caster, 18.0F, 0.01F, 16, 0, 0, true);
        } else if (castTime == 55) {
            TOFollowingScreenShakeEntity.createFollowingScreenShake(world, caster, 22.0F, 0.015F, 16, 0, 0, true);
        } else if (castTime == 70) {
            TOFollowingScreenShakeEntity.createFollowingScreenShake(world, caster, 28.0F, 0.02F, 19, 1, 10, true);
        }
    }

    private float getStormRadius(int spellLevel) {
        return 9.0F + (float) spellLevel * 1.0F;
    }

    public float getDamage(int spellLevel, LivingEntity caster) {
        return 2.0F + this.getSpellPower(spellLevel, caster) * 5.0F;
    }

    private float getRange() {
        return 32.0F;
    }

    private int getSpearCount() {
        return 3;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return TOGeneralUtils.buildAquaSpellInfo(1, true, new MutableComponent[] {
                Component.translatable("ui.traveloptics.range", Utils.stringTruncation(this.getRange(), 2)),
                Component.translatable("ui.traveloptics.multi_hit_direct_damage",
                        Utils.stringTruncation(this.getDamage(spellLevel, caster), 2))
        });
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
