package io.redspace.ironspell_more.spells.nature;

import com.github.L_Ender.cataclysm.client.particle.StormParticle;
import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.entity.spells.wings_of_tempest.WingofTempestAoe;
import io.redspace.ironspell_more.registry.EntityRegistry;
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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

import io.redspace.ironspell_more.config.SpellConfig;

@AutoSpellConfig
public class WingsofTempestSpell extends AbstractSpell {
    // ==========================================
    // SPELL TUNING CONSTANTS (Code Defaults)
    // ==========================================
    public static final float BASE_DURATION_SECONDS = 14.25F;
    public static final float DURATION_PER_LEVEL_SECONDS = 2.25F;
    public static final float BASE_RADIUS = 8.0F;
    public static final int BASE_MANA_COST = 40;
    public static final int MANA_COST_PER_LEVEL = 10;
    public static final double COOLDOWN_SECONDS = 22.0D;
    public static final int CAST_TIME_TICKS = 25;

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(IronSpellMore.MODID,
            "wings_of_tempest");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.NATURE_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(COOLDOWN_SECONDS)
            .build();

    public WingsofTempestSpell() {
        this.manaCostPerLevel = MANA_COST_PER_LEVEL;
        this.baseSpellPower = (int) BASE_DURATION_SECONDS;
        this.spellPowerPerLevel = (int) DURATION_PER_LEVEL_SECONDS;
        this.castTime = CAST_TIME_TICKS;
        this.baseManaCost = BASE_MANA_COST;
    }

    @Override
    public int getManaCost(int spellLevel) {
        return SpellConfig.WingsofTempest.getBaseMana() + (spellLevel - 1) * SpellConfig.WingsofTempest.getManaPerLevel();
    }

    @Override
    public int getSpellCooldown() {
        return (int) (SpellConfig.WingsofTempest.getCooldown() * 20);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.radius",
                        Utils.stringTruncation(getRadius(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.duration",
                        Utils.timeFromTicks(getDurationTicks(spellLevel, caster), 1)));
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
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
        return Optional.of(SoundEvents.ELYTRA_FLYING);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.PHANTOM_SWOOP);
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        return true;
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        super.onServerCastTick(level, spellLevel, entity, playerMagicData);
        if (level instanceof ServerLevel serverLevel && entity.tickCount % 4 == 0) {
            float width = 1.5f + entity.getRandom().nextFloat() * 1.5f;
            float height = 0.2f + entity.getRandom().nextFloat() * 1.5f;
            serverLevel.sendParticles(new StormParticle.OrbData(1.0f, 1.0f, 1.0f, width, height, entity.getId()),
                    entity.getX(), entity.getY(), entity.getZ(), 1, 0, 0, 0, 0);
        }
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource,
            MagicData playerMagicData) {
        int duration = getDurationTicks(spellLevel, entity);
        float radius = getRadius(spellLevel, entity);

        WingofTempestAoe aoe = new WingofTempestAoe(EntityRegistry.WING_OF_TEMPEST_AOE.get(), level);
        aoe.moveTo(entity.position());
        aoe.setOwner(entity);
        aoe.setRadius(radius);
        aoe.setDuration(duration);
        level.addFreshEntity(aoe);

        if (level instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 6; i++) {
                float orbRadius = 2.5f + (i % 3) * 2.5f;
                float orbHeight = 0.3f + (i / 3.0f) * 1.2f;
                serverLevel.sendParticles(
                        new StormParticle.OrbData(1.0f, 1.0f, 1.0f, orbRadius, orbHeight, entity.getId()),
                        entity.getX(), entity.getY(), entity.getZ(), 1, 0, 0, 0, 0);
            }
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private int getDurationTicks(int spellLevel, LivingEntity caster) {
        float base = SpellConfig.WingsofTempest.getBaseDuration();
        float perLevel = SpellConfig.WingsofTempest.getDurationPerLevel();
        return (int) (20 * (base + (spellLevel - 1) * perLevel));
    }

    private float getRadius(int spellLevel, LivingEntity caster) {
        return SpellConfig.WingsofTempest.getBaseRadius() + 4f * getEntityPowerMultiplier(caster);
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_LONG_CAST;
    }
}