package io.redspace.ironspell_more.spells.fire;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.compat.api.AnimationCue;
import io.redspace.ironspell_more.compat.epicfight.EpicFightCompat;
import io.redspace.ironspell_more.compat.epicfight.skills.blazing_chakra.BlazingChakraVfx;
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
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

@AutoSpellConfig
public class BlazingChakraSpell extends AbstractSpell {
    private final ResourceLocation spellId = new ResourceLocation(IronSpellMore.MODID, "blazing_chakra");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.FIRE_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(15)
            .build();

    public BlazingChakraSpell() {
        this.manaCostPerLevel = 10;
        this.baseSpellPower = 20;
        this.spellPowerPerLevel = 4;
        this.castTime = 0;
        this.baseManaCost = 50;
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
    public AnimationHolder getCastFinishAnimation() {
        return SpellAnimations.OVERHEAD_MELEE_SWING_ANIMATION;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
            Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
            Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(getRadius(spellLevel, caster), 1)),
            Component.translatable("ui.irons_spellbooks.effect_length", Utils.timeFromTicks(getFireDuration(spellLevel, caster), 1))
        );
    }

    public float getDamage(int spellLevel, LivingEntity caster) {
        return getSpellPower(spellLevel, caster);
    }

    public float getRadius(int spellLevel, LivingEntity caster) {
        return BlazingChakraVfx.MAX_RANGE;
    }

    public int getFireDuration(int spellLevel, LivingEntity caster) {
        return (5 + spellLevel) * 20;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        // 1. Play Epic Fight synchronized animation (it triggers shockwave & fire on impact at Frame 59)
        io.redspace.ironspell_more.compat.api.CompatResult result = EpicFightCompat.playAnimation(entity, AnimationCue.BLAZING_CHAKRA);

        // 2. Only use fallback timer if Epic Fight animation is NOT available/applied
        if (result != io.redspace.ironspell_more.compat.api.CompatResult.APPLIED && level instanceof ServerLevel serverLevel) {
            serverLevel.getServer().tell(new TickTask(serverLevel.getServer().getTickCount() + 20, () -> {
                if (!entity.isAlive()) {
                    return;
                }
                dealShockwaveDamage(serverLevel, entity, spellLevel);
            }));
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private void dealShockwaveDamage(ServerLevel serverLevel, LivingEntity caster, int spellLevel) {
        Vec3 origin = caster.position();
        float maxRadius = getRadius(spellLevel, caster);
        float closeRadius = BlazingChakraVfx.CLOSE_RANGE;
        float maxDamage = getDamage(spellLevel, caster);
        float minDamage = maxDamage * 0.25F; // 25% damage falloff at max distance (12 blocks)

        AABB area = new AABB(
            origin.x - maxRadius, origin.y - 3.0D, origin.z - maxRadius,
            origin.x + maxRadius, origin.y + 3.0D, origin.z + maxRadius
        );

        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, area)) {
            if (target == caster || !target.isAlive() || target.isSpectator() || target.isInvulnerable()) {
                continue;
            }
            if (DamageSources.isFriendlyFireBetween(caster, target)) {
                continue;
            }

            double dist = target.position().distanceTo(origin);
            if (dist > maxRadius) {
                continue;
            }

            float damage;
            int fireSeconds;
            if (dist <= closeRadius) {
                damage = maxDamage;
                fireSeconds = 5;
            } else {
                float falloffRatio = (float) ((dist - closeRadius) / (maxRadius - closeRadius));
                falloffRatio = Mth.clamp(falloffRatio, 0.0F, 1.0F);
                damage = Mth.lerp(falloffRatio, maxDamage, minDamage);
                fireSeconds = 2;
            }

            // Always ignite all targets within skill range (even if blocking or in i-frames)
            target.setSecondsOnFire(fireSeconds + spellLevel);

            // Apply Iron's Spells Fire Magic Damage
            boolean damaged = DamageSources.applyDamage(target, damage, this.getDamageSource(caster));

            // Knockback radiating outward from ground slam
            Vec3 pushDir = target.position().subtract(origin);
            double horizontalDist = Math.sqrt(pushDir.x * pushDir.x + pushDir.z * pushDir.z);
            if (horizontalDist > 0.001D) {
                double strength = (1.0D - (dist / maxRadius)) * 1.2D + 0.3D;
                target.push(
                    (pushDir.x / horizontalDist) * strength,
                    0.35D,
                    (pushDir.z / horizontalDist) * strength
                );
                target.hurtMarked = true;
            }
        }

        serverLevel.playSound(null, origin.x, origin.y, origin.z,
            SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.5F, 1.0F);

        // Fallback spiral particles if Epic Fight is not loaded
        if (!EpicFightCompat.isAvailable()) {
            BlazingChakraVfx.spawnSpiralShockwave(serverLevel, caster);
        }
    }
}
