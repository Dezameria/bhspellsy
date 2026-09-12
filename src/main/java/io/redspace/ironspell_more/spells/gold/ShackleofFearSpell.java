package io.redspace.ironspell_more.spells.gold;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.entity.spells.gold_chain.ArcaneShackleProjectile;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

@AutoSpellConfig
public class ShackleofFearSpell extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(IronSpellMore.MODID,
            "shackle_of_fear");

    // MERGE: swap to BHSchoolRegistry.GOLD_RESOURCE once this moves into bhspells
    // proper.
    private static final ResourceLocation GOLD_SCHOOL_RESOURCE = ResourceLocation.fromNamespaceAndPath("bhspells",
            "gold");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(GOLD_SCHOOL_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(0.0)
            .build();

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.hp",
                        Utils.stringTruncation(getChainHealth(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.duration",
                        Utils.timeFromTicks(getChainDuration(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.distance",
                        Utils.stringTruncation(getLashRadius(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.slowness_effect", 6));
    }

    // Fallback for dev environment when bhspells is not loaded (can be removed once
    // moved to bhspells)
    @Override
    public SchoolType getSchoolType() {
        SchoolType school = SchoolRegistry.getSchool(GOLD_SCHOOL_RESOURCE);
        return school != null ? school : SchoolRegistry.ENDER.get();
    }

    public ShackleofFearSpell() {
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 1;
        this.baseManaCost = 40;
        this.manaCostPerLevel = 8;
        this.castTime = 10;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(net.minecraft.sounds.SoundEvents.CHAIN_STEP);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource,
            MagicData playerMagicData) {
        ArcaneShackleProjectile projectile = new ArcaneShackleProjectile(level, entity);
        projectile.setPos(
                entity.position().add(0, entity.getEyeHeight() - projectile.getBoundingBox().getYsize() * 0.5f, 0)
                        .add(entity.getForward()));
        projectile.shoot(entity.getLookAngle());
        projectile.setChainHealth(getChainHealth(spellLevel, entity));
        projectile.setChainLifetime(getChainDuration(spellLevel, entity));
        projectile.setLashRadius(getLashRadius(spellLevel, entity));
        projectile.setRestraintStrength(0.015f);
        level.addFreshEntity(projectile);
        level.playSound(null, entity.blockPosition(), net.minecraft.sounds.SoundEvents.TRIDENT_THROW,
                entity.getSoundSource(), 1f, 0.7f + entity.getRandom().nextFloat() * .1f);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private float getChainHealth(int spellLevel, LivingEntity entity) {
        return 15 * getEntityPowerMultiplier(entity);
    }

    private int getChainDuration(int spellLevel, LivingEntity entity) {
        return 20 * 20;
    }

    private float getLashRadius(int spellLevel, LivingEntity entity) {
        return 5f;
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.pass();
    }
}
