package io.redspace.ironspell_more.config;

import io.redspace.ironspell_more.spells.aqua.CrimsonRainBathesMoonSpell;
import io.redspace.ironspell_more.spells.aqua.GlacialFirmamentSpell;
import io.redspace.ironspell_more.spells.aqua.GlacialVeilSpell;
import io.redspace.ironspell_more.spells.fire.*;
import io.redspace.ironspell_more.spells.gold.GildedHareSpell;
import io.redspace.ironspell_more.spells.gold.HymnofPurificationSpell;
import io.redspace.ironspell_more.spells.gold.ShackleofFearSpell;
import io.redspace.ironspell_more.spells.ground.JadeAuraSpell;
import io.redspace.ironspell_more.spells.ground.TigershadeTerrabreakSpell;
import io.redspace.ironspell_more.spells.lightning.LightningStrikeSpell;
import io.redspace.ironspell_more.spells.lightning.ThunderStepSpell;
import io.redspace.ironspell_more.spells.nature.GalePiercerSpell;
import io.redspace.ironspell_more.spells.nature.RapturousBloomSpell;
import io.redspace.ironspell_more.spells.nature.VenomousBlossomfallSpell;
import io.redspace.ironspell_more.spells.nature.WingsofTempestSpell;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Server configuration for IronSpell More spells.
 * Allows server owners to tune damage, mana cost, and cooldowns directly in ironspell_more-server.toml.
 * If the config is not loaded, it falls back to the in-code constants defined in each Spell class.
 */
public class SpellConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // ==========================================
    // FIRE SCHOOL
    // ==========================================
    public static class BlazingChakra {
        public static ForgeConfigSpec.DoubleValue baseDamage;
        public static ForgeConfigSpec.DoubleValue damagePerLevel;
        public static ForgeConfigSpec.IntValue baseMana;
        public static ForgeConfigSpec.IntValue manaPerLevel;
        public static ForgeConfigSpec.DoubleValue cooldown;

        public static float getBaseDamage() {
            return (SPEC != null && SPEC.isLoaded()) ? baseDamage.get().floatValue() : BlazingChakraSpell.BASE_DAMAGE;
        }

        public static float getDamagePerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? damagePerLevel.get().floatValue() : BlazingChakraSpell.DAMAGE_PER_LEVEL;
        }

        public static int getBaseMana() {
            return (SPEC != null && SPEC.isLoaded()) ? baseMana.get() : BlazingChakraSpell.BASE_MANA_COST;
        }

        public static int getManaPerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? manaPerLevel.get() : BlazingChakraSpell.MANA_COST_PER_LEVEL;
        }

        public static double getCooldown() {
            return (SPEC != null && SPEC.isLoaded()) ? cooldown.get() : BlazingChakraSpell.COOLDOWN_SECONDS;
        }
    }

    public static class SpinStrike {
        public static ForgeConfigSpec.DoubleValue baseDamage;
        public static ForgeConfigSpec.DoubleValue damagePerLevel;
        public static ForgeConfigSpec.IntValue baseMana;
        public static ForgeConfigSpec.IntValue manaPerLevel;
        public static ForgeConfigSpec.DoubleValue cooldown;

        public static float getBaseDamage() {
            return (SPEC != null && SPEC.isLoaded()) ? baseDamage.get().floatValue() : SpinStrikeSpell.BASE_DAMAGE;
        }

        public static float getDamagePerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? damagePerLevel.get().floatValue() : SpinStrikeSpell.DAMAGE_PER_LEVEL;
        }

        public static int getBaseMana() {
            return (SPEC != null && SPEC.isLoaded()) ? baseMana.get() : SpinStrikeSpell.BASE_MANA_COST;
        }

        public static int getManaPerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? manaPerLevel.get() : SpinStrikeSpell.MANA_COST_PER_LEVEL;
        }

        public static double getCooldown() {
            return (SPEC != null && SPEC.isLoaded()) ? cooldown.get() : SpinStrikeSpell.COOLDOWN_SECONDS;
        }
    }

    public static class PureWhiteFlameBurst {
        public static ForgeConfigSpec.DoubleValue baseDamage;
        public static ForgeConfigSpec.DoubleValue damagePerLevel;
        public static ForgeConfigSpec.DoubleValue aoeRatio;
        public static ForgeConfigSpec.IntValue baseMana;
        public static ForgeConfigSpec.IntValue manaPerLevel;
        public static ForgeConfigSpec.DoubleValue cooldown;

        public static float getBaseDamage() {
            return (SPEC != null && SPEC.isLoaded()) ? baseDamage.get().floatValue() : PureWhiteFlameBurstSpell.BASE_DAMAGE;
        }

        public static float getDamagePerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? damagePerLevel.get().floatValue() : PureWhiteFlameBurstSpell.DAMAGE_PER_LEVEL;
        }

        public static float getAoeRatio() {
            return (SPEC != null && SPEC.isLoaded()) ? aoeRatio.get().floatValue() : PureWhiteFlameBurstSpell.AOE_DAMAGE_RATIO;
        }

        public static int getBaseMana() {
            return (SPEC != null && SPEC.isLoaded()) ? baseMana.get() : PureWhiteFlameBurstSpell.BASE_MANA_COST;
        }

        public static int getManaPerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? manaPerLevel.get() : PureWhiteFlameBurstSpell.MANA_COST_PER_LEVEL;
        }

        public static double getCooldown() {
            return (SPEC != null && SPEC.isLoaded()) ? cooldown.get() : PureWhiteFlameBurstSpell.COOLDOWN_SECONDS;
        }
    }

    public static class GaleDrive {
        public static ForgeConfigSpec.DoubleValue baseDamage;
        public static ForgeConfigSpec.DoubleValue damagePerLevel;
        public static ForgeConfigSpec.IntValue baseMana;
        public static ForgeConfigSpec.IntValue manaPerLevel;
        public static ForgeConfigSpec.DoubleValue cooldown;

        public static float getBaseDamage() {
            return (SPEC != null && SPEC.isLoaded()) ? baseDamage.get().floatValue() : GaleDriveSpell.BASE_DAMAGE;
        }

        public static float getDamagePerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? damagePerLevel.get().floatValue() : GaleDriveSpell.DAMAGE_PER_LEVEL;
        }

        public static int getBaseMana() {
            return (SPEC != null && SPEC.isLoaded()) ? baseMana.get() : GaleDriveSpell.BASE_MANA_COST;
        }

        public static int getManaPerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? manaPerLevel.get() : GaleDriveSpell.MANA_COST_PER_LEVEL;
        }

        public static double getCooldown() {
            return (SPEC != null && SPEC.isLoaded()) ? cooldown.get() : GaleDriveSpell.COOLDOWN_SECONDS;
        }
    }

    public static class ResonantKnell {
        public static ForgeConfigSpec.DoubleValue stage1Damage;
        public static ForgeConfigSpec.DoubleValue stage2Damage;
        public static ForgeConfigSpec.DoubleValue stage3Damage;
        public static ForgeConfigSpec.DoubleValue damagePerLevel;
        public static ForgeConfigSpec.IntValue baseMana;
        public static ForgeConfigSpec.IntValue manaPerLevel;
        public static ForgeConfigSpec.DoubleValue cooldown;

        public static float getStage1Damage() {
            return (SPEC != null && SPEC.isLoaded()) ? stage1Damage.get().floatValue() : ResonantKnellSpell.STAGE_1_DAMAGE;
        }

        public static float getStage2Damage() {
            return (SPEC != null && SPEC.isLoaded()) ? stage2Damage.get().floatValue() : ResonantKnellSpell.STAGE_2_DAMAGE;
        }

        public static float getStage3Damage() {
            return (SPEC != null && SPEC.isLoaded()) ? stage3Damage.get().floatValue() : ResonantKnellSpell.STAGE_3_DAMAGE;
        }

        public static float getDamagePerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? damagePerLevel.get().floatValue() : ResonantKnellSpell.DAMAGE_PER_LEVEL;
        }

        public static int getBaseMana() {
            return (SPEC != null && SPEC.isLoaded()) ? baseMana.get() : ResonantKnellSpell.BASE_MANA_COST;
        }

        public static int getManaPerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? manaPerLevel.get() : ResonantKnellSpell.MANA_COST_PER_LEVEL;
        }

        public static double getCooldown() {
            return (SPEC != null && SPEC.isLoaded()) ? cooldown.get() : ResonantKnellSpell.COOLDOWN_SECONDS;
        }
    }

    public static class CrimsonThornbind {
        public static ForgeConfigSpec.DoubleValue baseDamage;
        public static ForgeConfigSpec.DoubleValue damagePerLevel;
        public static ForgeConfigSpec.DoubleValue rendBaseDamage;
        public static ForgeConfigSpec.DoubleValue rendDamagePerLevel;
        public static ForgeConfigSpec.IntValue baseMana;
        public static ForgeConfigSpec.IntValue manaPerLevel;
        public static ForgeConfigSpec.DoubleValue cooldown;

        public static float getBaseDamage() {
            return (SPEC != null && SPEC.isLoaded()) ? baseDamage.get().floatValue() : CrimsonThornbindSpell.BASE_DAMAGE;
        }

        public static float getDamagePerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? damagePerLevel.get().floatValue() : CrimsonThornbindSpell.DAMAGE_PER_LEVEL;
        }

        public static float getRendBaseDamage() {
            return (SPEC != null && SPEC.isLoaded()) ? rendBaseDamage.get().floatValue() : CrimsonThornbindSpell.REND_BASE_DAMAGE;
        }

        public static float getRendDamagePerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? rendDamagePerLevel.get().floatValue() : CrimsonThornbindSpell.REND_DAMAGE_PER_LEVEL;
        }

        public static int getBaseMana() {
            return (SPEC != null && SPEC.isLoaded()) ? baseMana.get() : CrimsonThornbindSpell.BASE_MANA_COST;
        }

        public static int getManaPerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? manaPerLevel.get() : CrimsonThornbindSpell.MANA_COST_PER_LEVEL;
        }

        public static double getCooldown() {
            return (SPEC != null && SPEC.isLoaded()) ? cooldown.get() : CrimsonThornbindSpell.COOLDOWN_SECONDS;
        }
    }

    // ==========================================
    // LIGHTNING SCHOOL
    // ==========================================
    public static class LightningStrike {
        public static ForgeConfigSpec.DoubleValue baseDamage;
        public static ForgeConfigSpec.DoubleValue damagePerLevel;
        public static ForgeConfigSpec.IntValue baseMana;
        public static ForgeConfigSpec.IntValue manaPerLevel;
        public static ForgeConfigSpec.DoubleValue cooldown;

        public static float getBaseDamage() {
            return (SPEC != null && SPEC.isLoaded()) ? baseDamage.get().floatValue() : LightningStrikeSpell.BASE_DAMAGE;
        }

        public static float getDamagePerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? damagePerLevel.get().floatValue() : LightningStrikeSpell.DAMAGE_PER_LEVEL;
        }

        public static int getBaseMana() {
            return (SPEC != null && SPEC.isLoaded()) ? baseMana.get() : LightningStrikeSpell.BASE_MANA_COST;
        }

        public static int getManaPerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? manaPerLevel.get() : LightningStrikeSpell.MANA_COST_PER_LEVEL;
        }

        public static double getCooldown() {
            return (SPEC != null && SPEC.isLoaded()) ? cooldown.get() : LightningStrikeSpell.COOLDOWN_SECONDS;
        }
    }

    public static class ThunderStep {
        public static ForgeConfigSpec.DoubleValue baseDamage;
        public static ForgeConfigSpec.DoubleValue damagePerLevel;
        public static ForgeConfigSpec.IntValue baseMana;
        public static ForgeConfigSpec.IntValue manaPerLevel;
        public static ForgeConfigSpec.DoubleValue cooldown;

        public static float getBaseDamage() {
            return (SPEC != null && SPEC.isLoaded()) ? baseDamage.get().floatValue() : ThunderStepSpell.BASE_DAMAGE;
        }

        public static float getDamagePerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? damagePerLevel.get().floatValue() : ThunderStepSpell.DAMAGE_PER_LEVEL;
        }

        public static int getBaseMana() {
            return (SPEC != null && SPEC.isLoaded()) ? baseMana.get() : ThunderStepSpell.BASE_MANA_COST;
        }

        public static int getManaPerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? manaPerLevel.get() : ThunderStepSpell.MANA_COST_PER_LEVEL;
        }

        public static double getCooldown() {
            return (SPEC != null && SPEC.isLoaded()) ? cooldown.get() : ThunderStepSpell.COOLDOWN_SECONDS;
        }
    }

    // ==========================================
    // GOLD SCHOOL
    // ==========================================
    public static class ShackleofFear {
        public static ForgeConfigSpec.DoubleValue chainHealth;
        public static ForgeConfigSpec.DoubleValue chainHealthPerLevel;
        public static ForgeConfigSpec.IntValue baseMana;
        public static ForgeConfigSpec.IntValue manaPerLevel;
        public static ForgeConfigSpec.DoubleValue cooldown;

        public static float getChainHealth() {
            return (SPEC != null && SPEC.isLoaded()) ? chainHealth.get().floatValue() : ShackleofFearSpell.CHAIN_HEALTH;
        }

        public static float getChainHealthPerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? chainHealthPerLevel.get().floatValue() : ShackleofFearSpell.CHAIN_HEALTH_PER_LEVEL;
        }

        public static int getBaseMana() {
            return (SPEC != null && SPEC.isLoaded()) ? baseMana.get() : ShackleofFearSpell.BASE_MANA_COST;
        }

        public static int getManaPerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? manaPerLevel.get() : ShackleofFearSpell.MANA_COST_PER_LEVEL;
        }

        public static double getCooldown() {
            return (SPEC != null && SPEC.isLoaded()) ? cooldown.get() : ShackleofFearSpell.COOLDOWN_SECONDS;
        }
    }

    public static class HymnofPurification {
        public static ForgeConfigSpec.DoubleValue baseHeal;
        public static ForgeConfigSpec.DoubleValue healPerLevel;
        public static ForgeConfigSpec.IntValue baseMana;
        public static ForgeConfigSpec.IntValue manaPerLevel;
        public static ForgeConfigSpec.DoubleValue cooldown;

        public static float getBaseHeal() {
            return (SPEC != null && SPEC.isLoaded()) ? baseHeal.get().floatValue() : HymnofPurificationSpell.BASE_HEAL;
        }

        public static float getHealPerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? healPerLevel.get().floatValue() : HymnofPurificationSpell.HEAL_PER_LEVEL;
        }

        public static int getBaseMana() {
            return (SPEC != null && SPEC.isLoaded()) ? baseMana.get() : HymnofPurificationSpell.BASE_MANA_COST;
        }

        public static int getManaPerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? manaPerLevel.get() : HymnofPurificationSpell.MANA_COST_PER_LEVEL;
        }

        public static double getCooldown() {
            return (SPEC != null && SPEC.isLoaded()) ? cooldown.get() : HymnofPurificationSpell.COOLDOWN_SECONDS;
        }
    }

    public static class GildedHare {
        public static ForgeConfigSpec.IntValue baseMana;
        public static ForgeConfigSpec.IntValue manaPerLevel;
        public static ForgeConfigSpec.DoubleValue cooldown;

        public static int getBaseMana() {
            return (SPEC != null && SPEC.isLoaded()) ? baseMana.get() : GildedHareSpell.BASE_MANA_COST;
        }

        public static int getManaPerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? manaPerLevel.get() : GildedHareSpell.MANA_COST_PER_LEVEL;
        }

        public static double getCooldown() {
            return (SPEC != null && SPEC.isLoaded()) ? cooldown.get() : GildedHareSpell.COOLDOWN_SECONDS;
        }
    }

    // ==========================================
    // GROUND SCHOOL
    // ==========================================
    public static class TigershadeTerrabreak {
        public static ForgeConfigSpec.DoubleValue baseDamage;
        public static ForgeConfigSpec.DoubleValue damagePerLevel;
        public static ForgeConfigSpec.IntValue baseMana;
        public static ForgeConfigSpec.IntValue manaPerLevel;
        public static ForgeConfigSpec.DoubleValue cooldown;

        public static float getBaseDamage() {
            return (SPEC != null && SPEC.isLoaded()) ? baseDamage.get().floatValue() : TigershadeTerrabreakSpell.BASE_DAMAGE;
        }

        public static float getDamagePerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? damagePerLevel.get().floatValue() : TigershadeTerrabreakSpell.DAMAGE_PER_LEVEL;
        }

        public static int getBaseMana() {
            return (SPEC != null && SPEC.isLoaded()) ? baseMana.get() : TigershadeTerrabreakSpell.BASE_MANA_COST;
        }

        public static int getManaPerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? manaPerLevel.get() : TigershadeTerrabreakSpell.MANA_COST_PER_LEVEL;
        }

        public static double getCooldown() {
            return (SPEC != null && SPEC.isLoaded()) ? cooldown.get() : TigershadeTerrabreakSpell.COOLDOWN_SECONDS;
        }
    }

    public static class JadeAura {
        public static ForgeConfigSpec.DoubleValue baseDuration;
        public static ForgeConfigSpec.DoubleValue durationPerLevel;
        public static ForgeConfigSpec.IntValue baseMana;
        public static ForgeConfigSpec.IntValue manaPerLevel;
        public static ForgeConfigSpec.DoubleValue cooldown;

        public static float getBaseDuration() {
            return (SPEC != null && SPEC.isLoaded()) ? baseDuration.get().floatValue() : JadeAuraSpell.BASE_DURATION_SECONDS;
        }

        public static float getDurationPerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? durationPerLevel.get().floatValue() : JadeAuraSpell.DURATION_PER_LEVEL_SECONDS;
        }

        public static int getBaseMana() {
            return (SPEC != null && SPEC.isLoaded()) ? baseMana.get() : JadeAuraSpell.BASE_MANA_COST;
        }

        public static int getManaPerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? manaPerLevel.get() : JadeAuraSpell.MANA_COST_PER_LEVEL;
        }

        public static double getCooldown() {
            return (SPEC != null && SPEC.isLoaded()) ? cooldown.get() : JadeAuraSpell.COOLDOWN_SECONDS;
        }
    }

    // ==========================================
    // NATURE SCHOOL
    // ==========================================
    public static class WingsofTempest {
        public static ForgeConfigSpec.DoubleValue baseDuration;
        public static ForgeConfigSpec.DoubleValue durationPerLevel;
        public static ForgeConfigSpec.DoubleValue baseRadius;
        public static ForgeConfigSpec.IntValue baseMana;
        public static ForgeConfigSpec.IntValue manaPerLevel;
        public static ForgeConfigSpec.DoubleValue cooldown;

        public static float getBaseDuration() {
            return (SPEC != null && SPEC.isLoaded()) ? baseDuration.get().floatValue() : WingsofTempestSpell.BASE_DURATION_SECONDS;
        }

        public static float getDurationPerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? durationPerLevel.get().floatValue() : WingsofTempestSpell.DURATION_PER_LEVEL_SECONDS;
        }

        public static float getBaseRadius() {
            return (SPEC != null && SPEC.isLoaded()) ? baseRadius.get().floatValue() : WingsofTempestSpell.BASE_RADIUS;
        }

        public static int getBaseMana() {
            return (SPEC != null && SPEC.isLoaded()) ? baseMana.get() : WingsofTempestSpell.BASE_MANA_COST;
        }

        public static int getManaPerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? manaPerLevel.get() : WingsofTempestSpell.MANA_COST_PER_LEVEL;
        }

        public static double getCooldown() {
            return (SPEC != null && SPEC.isLoaded()) ? cooldown.get() : WingsofTempestSpell.COOLDOWN_SECONDS;
        }
    }

    public static class VenomousBlossomfall {
        public static ForgeConfigSpec.DoubleValue shortDamage;
        public static ForgeConfigSpec.DoubleValue mediumDamage;
        public static ForgeConfigSpec.DoubleValue fullDamage;
        public static ForgeConfigSpec.DoubleValue damagePerLevel;
        public static ForgeConfigSpec.IntValue baseMana;
        public static ForgeConfigSpec.IntValue manaPerLevel;
        public static ForgeConfigSpec.DoubleValue cooldown;

        public static float getShortDamage() {
            return (SPEC != null && SPEC.isLoaded()) ? shortDamage.get().floatValue() : VenomousBlossomfallSpell.SHORT_DIRECT_DAMAGE;
        }

        public static float getMediumDamage() {
            return (SPEC != null && SPEC.isLoaded()) ? mediumDamage.get().floatValue() : VenomousBlossomfallSpell.MEDIUM_DIRECT_DAMAGE;
        }

        public static float getFullDamage() {
            return (SPEC != null && SPEC.isLoaded()) ? fullDamage.get().floatValue() : VenomousBlossomfallSpell.FULL_DIRECT_DAMAGE;
        }

        public static float getDamagePerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? damagePerLevel.get().floatValue() : VenomousBlossomfallSpell.DAMAGE_PER_LEVEL;
        }

        public static int getBaseMana() {
            return (SPEC != null && SPEC.isLoaded()) ? baseMana.get() : VenomousBlossomfallSpell.BASE_MANA_COST;
        }

        public static int getManaPerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? manaPerLevel.get() : VenomousBlossomfallSpell.MANA_COST_PER_LEVEL;
        }

        public static double getCooldown() {
            return (SPEC != null && SPEC.isLoaded()) ? cooldown.get() : VenomousBlossomfallSpell.COOLDOWN_SECONDS;
        }
    }

    public static class GalePiercer {
        public static ForgeConfigSpec.DoubleValue normalBaseDamage;
        public static ForgeConfigSpec.DoubleValue normalDamagePerLevel;
        public static ForgeConfigSpec.DoubleValue fullBaseDamage;
        public static ForgeConfigSpec.DoubleValue fullDamagePerLevel;
        public static ForgeConfigSpec.IntValue baseMana;
        public static ForgeConfigSpec.IntValue manaPerLevel;
        public static ForgeConfigSpec.DoubleValue cooldown;

        public static float getNormalBaseDamage() {
            return (SPEC != null && SPEC.isLoaded()) ? normalBaseDamage.get().floatValue() : GalePiercerSpell.NORMAL_BASE_DAMAGE;
        }

        public static float getNormalDamagePerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? normalDamagePerLevel.get().floatValue() : GalePiercerSpell.NORMAL_DAMAGE_PER_LEVEL;
        }

        public static float getFullBaseDamage() {
            return (SPEC != null && SPEC.isLoaded()) ? fullBaseDamage.get().floatValue() : GalePiercerSpell.FULL_BASE_DAMAGE;
        }

        public static float getFullDamagePerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? fullDamagePerLevel.get().floatValue() : GalePiercerSpell.FULL_DAMAGE_PER_LEVEL;
        }

        public static int getBaseMana() {
            return (SPEC != null && SPEC.isLoaded()) ? baseMana.get() : GalePiercerSpell.BASE_MANA_COST;
        }

        public static int getManaPerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? manaPerLevel.get() : GalePiercerSpell.MANA_COST_PER_LEVEL;
        }

        public static double getCooldown() {
            return (SPEC != null && SPEC.isLoaded()) ? cooldown.get() : GalePiercerSpell.COOLDOWN_SECONDS;
        }
    }

    public static class RapturousBloom {
        public static ForgeConfigSpec.DoubleValue baseDamage;
        public static ForgeConfigSpec.DoubleValue damagePerLevel;
        public static ForgeConfigSpec.IntValue baseMana;
        public static ForgeConfigSpec.IntValue manaPerLevel;
        public static ForgeConfigSpec.DoubleValue cooldown;

        public static float getBaseDamage() {
            return (SPEC != null && SPEC.isLoaded()) ? baseDamage.get().floatValue() : RapturousBloomSpell.BASE_DAMAGE;
        }

        public static float getDamagePerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? damagePerLevel.get().floatValue() : RapturousBloomSpell.DAMAGE_PER_LEVEL;
        }

        public static int getBaseMana() {
            return (SPEC != null && SPEC.isLoaded()) ? baseMana.get() : RapturousBloomSpell.BASE_MANA_COST;
        }

        public static int getManaPerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? manaPerLevel.get() : RapturousBloomSpell.MANA_COST_PER_LEVEL;
        }

        public static double getCooldown() {
            return (SPEC != null && SPEC.isLoaded()) ? cooldown.get() : RapturousBloomSpell.COOLDOWN_SECONDS;
        }
    }

    // ==========================================
    // AQUA SCHOOL
    // ==========================================
    public static class CrimsonRainBathesMoon {
        public static ForgeConfigSpec.DoubleValue baseDamage;
        public static ForgeConfigSpec.DoubleValue damagePerLevel;
        public static ForgeConfigSpec.IntValue baseMana;
        public static ForgeConfigSpec.IntValue manaPerLevel;
        public static ForgeConfigSpec.DoubleValue cooldown;

        public static float getBaseDamage() {
            return (SPEC != null && SPEC.isLoaded()) ? baseDamage.get().floatValue() : CrimsonRainBathesMoonSpell.BASE_DAMAGE;
        }

        public static float getDamagePerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? damagePerLevel.get().floatValue() : CrimsonRainBathesMoonSpell.DAMAGE_PER_LEVEL;
        }

        public static int getBaseMana() {
            return (SPEC != null && SPEC.isLoaded()) ? baseMana.get() : CrimsonRainBathesMoonSpell.BASE_MANA_COST;
        }

        public static int getManaPerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? manaPerLevel.get() : CrimsonRainBathesMoonSpell.MANA_COST_PER_LEVEL;
        }

        public static double getCooldown() {
            return (SPEC != null && SPEC.isLoaded()) ? cooldown.get() : CrimsonRainBathesMoonSpell.COOLDOWN_SECONDS;
        }
    }

    public static class GlacialVeil {
        public static ForgeConfigSpec.DoubleValue baseDamage;
        public static ForgeConfigSpec.DoubleValue damagePerLevel;
        public static ForgeConfigSpec.IntValue baseMana;
        public static ForgeConfigSpec.IntValue manaPerLevel;
        public static ForgeConfigSpec.DoubleValue cooldown;

        public static float getBaseDamage() {
            return (SPEC != null && SPEC.isLoaded()) ? baseDamage.get().floatValue() : GlacialVeilSpell.BASE_DAMAGE;
        }

        public static float getDamagePerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? damagePerLevel.get().floatValue() : GlacialVeilSpell.DAMAGE_PER_LEVEL;
        }

        public static int getBaseMana() {
            return (SPEC != null && SPEC.isLoaded()) ? baseMana.get() : GlacialVeilSpell.BASE_MANA_COST;
        }

        public static int getManaPerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? manaPerLevel.get() : GlacialVeilSpell.MANA_COST_PER_LEVEL;
        }

        public static double getCooldown() {
            return (SPEC != null && SPEC.isLoaded()) ? cooldown.get() : GlacialVeilSpell.COOLDOWN_SECONDS;
        }
    }

    public static class GlacialFirmament {
        public static ForgeConfigSpec.DoubleValue baseDamage;
        public static ForgeConfigSpec.DoubleValue damagePerLevel;
        public static ForgeConfigSpec.IntValue baseMana;
        public static ForgeConfigSpec.IntValue manaPerLevel;
        public static ForgeConfigSpec.DoubleValue cooldown;

        public static float getBaseDamage() {
            return (SPEC != null && SPEC.isLoaded()) ? baseDamage.get().floatValue() : GlacialFirmamentSpell.BASE_DAMAGE;
        }

        public static float getDamagePerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? damagePerLevel.get().floatValue() : GlacialFirmamentSpell.DAMAGE_PER_LEVEL;
        }

        public static int getBaseMana() {
            return (SPEC != null && SPEC.isLoaded()) ? baseMana.get() : GlacialFirmamentSpell.BASE_MANA_COST;
        }

        public static int getManaPerLevel() {
            return (SPEC != null && SPEC.isLoaded()) ? manaPerLevel.get() : GlacialFirmamentSpell.MANA_COST_PER_LEVEL;
        }

        public static double getCooldown() {
            return (SPEC != null && SPEC.isLoaded()) ? cooldown.get() : GlacialFirmamentSpell.COOLDOWN_SECONDS;
        }
    }

    static {
        BUILDER.comment("IronSpell More Spell Tuning Configuration").push("spells");

        // FIRE
        BUILDER.push("blazing_chakra");
        BlazingChakra.baseDamage = BUILDER.comment("Base damage at Level 1").defineInRange("base_damage", 20.0D, 0.0D, 10000.0D);
        BlazingChakra.damagePerLevel = BUILDER.comment("Damage increase per level (when cast via /cast)").defineInRange("damage_per_level", 4.0D, 0.0D, 1000.0D);
        BlazingChakra.baseMana = BUILDER.comment("Base mana cost").defineInRange("base_mana", 50, 0, 10000);
        BlazingChakra.manaPerLevel = BUILDER.comment("Mana cost increase per level").defineInRange("mana_per_level", 10, 0, 1000);
        BlazingChakra.cooldown = BUILDER.comment("Cooldown in seconds").defineInRange("cooldown_seconds", 15.0D, 0.0D, 3600.0D);
        BUILDER.pop();

        BUILDER.push("spin_strike");
        SpinStrike.baseDamage = BUILDER.comment("Base damage at Level 1").defineInRange("base_damage", 10.0D, 0.0D, 10000.0D);
        SpinStrike.damagePerLevel = BUILDER.comment("Damage increase per level").defineInRange("damage_per_level", 1.0D, 0.0D, 1000.0D);
        SpinStrike.baseMana = BUILDER.comment("Base mana cost").defineInRange("base_mana", 30, 0, 10000);
        SpinStrike.manaPerLevel = BUILDER.comment("Mana cost increase per level").defineInRange("mana_per_level", 5, 0, 1000);
        SpinStrike.cooldown = BUILDER.comment("Cooldown in seconds").defineInRange("cooldown_seconds", 10.0D, 0.0D, 3600.0D);
        BUILDER.pop();

        BUILDER.push("pure_white_flame_burst");
        PureWhiteFlameBurst.baseDamage = BUILDER.comment("Base damage at Level 1").defineInRange("base_damage", 80.0D, 0.0D, 10000.0D);
        PureWhiteFlameBurst.damagePerLevel = BUILDER.comment("Damage increase per level").defineInRange("damage_per_level", 10.0D, 0.0D, 1000.0D);
        PureWhiteFlameBurst.aoeRatio = BUILDER.comment("AoE blast damage multiplier relative to direct damage").defineInRange("aoe_damage_ratio", 0.8D, 0.0D, 10.0D);
        PureWhiteFlameBurst.baseMana = BUILDER.comment("Base mana cost").defineInRange("base_mana", 50, 0, 10000);
        PureWhiteFlameBurst.manaPerLevel = BUILDER.comment("Mana cost increase per level").defineInRange("mana_per_level", 10, 0, 1000);
        PureWhiteFlameBurst.cooldown = BUILDER.comment("Cooldown in seconds").defineInRange("cooldown_seconds", 15.0D, 0.0D, 3600.0D);
        BUILDER.pop();

        BUILDER.push("gale_drive");
        GaleDrive.baseDamage = BUILDER.comment("Base collision damage at Level 1").defineInRange("base_damage", 10.0D, 0.0D, 10000.0D);
        GaleDrive.damagePerLevel = BUILDER.comment("Damage increase per level").defineInRange("damage_per_level", 2.0D, 0.0D, 1000.0D);
        GaleDrive.baseMana = BUILDER.comment("Base mana cost").defineInRange("base_mana", 40, 0, 10000);
        GaleDrive.manaPerLevel = BUILDER.comment("Mana cost increase per level").defineInRange("mana_per_level", 5, 0, 1000);
        GaleDrive.cooldown = BUILDER.comment("Cooldown in seconds").defineInRange("cooldown_seconds", 20.0D, 0.0D, 3600.0D);
        BUILDER.pop();

        BUILDER.push("resonant_knell");
        ResonantKnell.stage1Damage = BUILDER.comment("Push 1 shockwave damage").defineInRange("stage1_damage", 6.0D, 0.0D, 10000.0D);
        ResonantKnell.stage2Damage = BUILDER.comment("Push 2 shockwave damage").defineInRange("stage2_damage", 10.0D, 0.0D, 10000.0D);
        ResonantKnell.stage3Damage = BUILDER.comment("Push 3 final nuclear blast shockwave damage").defineInRange("stage3_damage", 16.0D, 0.0D, 10000.0D);
        ResonantKnell.damagePerLevel = BUILDER.comment("Bonus damage per level").defineInRange("damage_per_level", 2.0D, 0.0D, 1000.0D);
        ResonantKnell.baseMana = BUILDER.comment("Base mana cost").defineInRange("base_mana", 75, 0, 10000);
        ResonantKnell.manaPerLevel = BUILDER.comment("Mana cost increase per level").defineInRange("mana_per_level", 0, 0, 1000);
        ResonantKnell.cooldown = BUILDER.comment("Post-barrier cooldown in seconds").defineInRange("cooldown_seconds", 30.0D, 0.0D, 3600.0D);
        BUILDER.pop();

        BUILDER.push("crimson_thornbind");
        CrimsonThornbind.baseDamage = BUILDER.comment("Base root impact damage at Level 1").defineInRange("base_damage", 12.0D, 0.0D, 10000.0D);
        CrimsonThornbind.damagePerLevel = BUILDER.comment("Damage increase per level").defineInRange("damage_per_level", 2.5D, 0.0D, 1000.0D);
        CrimsonThornbind.rendBaseDamage = BUILDER.comment("Base rend (bleed) damage").defineInRange("rend_base_damage", 6.0D, 0.0D, 10000.0D);
        CrimsonThornbind.rendDamagePerLevel = BUILDER.comment("Rend damage increase per level").defineInRange("rend_damage_per_level", 1.5D, 0.0D, 1000.0D);
        CrimsonThornbind.baseMana = BUILDER.comment("Base mana cost").defineInRange("base_mana", 40, 0, 10000);
        CrimsonThornbind.manaPerLevel = BUILDER.comment("Mana cost increase per level").defineInRange("mana_per_level", 5, 0, 1000);
        CrimsonThornbind.cooldown = BUILDER.comment("Cooldown in seconds").defineInRange("cooldown_seconds", 25.0D, 0.0D, 3600.0D);
        BUILDER.pop();

        // LIGHTNING
        BUILDER.push("lightning_strike");
        LightningStrike.baseDamage = BUILDER.comment("Base lightning strike damage at Level 1").defineInRange("base_damage", 10.0D, 0.0D, 10000.0D);
        LightningStrike.damagePerLevel = BUILDER.comment("Damage increase per level").defineInRange("damage_per_level", 2.0D, 0.0D, 1000.0D);
        LightningStrike.baseMana = BUILDER.comment("Base mana cost").defineInRange("base_mana", 50, 0, 10000);
        LightningStrike.manaPerLevel = BUILDER.comment("Mana cost increase per level").defineInRange("mana_per_level", 5, 0, 1000);
        LightningStrike.cooldown = BUILDER.comment("Cooldown in seconds").defineInRange("cooldown_seconds", 15.0D, 0.0D, 3600.0D);
        BUILDER.pop();

        BUILDER.push("thunder_step");
        ThunderStep.baseDamage = BUILDER.comment("Base dash damage at Level 1").defineInRange("base_damage", 10.0D, 0.0D, 10000.0D);
        ThunderStep.damagePerLevel = BUILDER.comment("Damage increase per level").defineInRange("damage_per_level", 2.0D, 0.0D, 1000.0D);
        ThunderStep.baseMana = BUILDER.comment("Base mana cost").defineInRange("base_mana", 75, 0, 10000);
        ThunderStep.manaPerLevel = BUILDER.comment("Mana cost increase per level").defineInRange("mana_per_level", 15, 0, 1000);
        ThunderStep.cooldown = BUILDER.comment("Cooldown in seconds").defineInRange("cooldown_seconds", 8.0D, 0.0D, 3600.0D);
        BUILDER.pop();

        // GOLD
        BUILDER.push("shackle_of_fear");
        ShackleofFear.chainHealth = BUILDER.comment("Base golden chain HP").defineInRange("chain_health", 15.0D, 1.0D, 10000.0D);
        ShackleofFear.chainHealthPerLevel = BUILDER.comment("Chain HP increase per level").defineInRange("chain_health_per_level", 3.0D, 0.0D, 1000.0D);
        ShackleofFear.baseMana = BUILDER.comment("Base mana cost").defineInRange("base_mana", 40, 0, 10000);
        ShackleofFear.manaPerLevel = BUILDER.comment("Mana cost increase per level").defineInRange("mana_per_level", 8, 0, 1000);
        ShackleofFear.cooldown = BUILDER.comment("Cooldown in seconds").defineInRange("cooldown_seconds", 0.0D, 0.0D, 3600.0D);
        BUILDER.pop();

        BUILDER.push("hymn_of_purification");
        HymnofPurification.baseHeal = BUILDER.comment("Base healing amount per pulse").defineInRange("base_heal", 1.0D, 0.1D, 10000.0D);
        HymnofPurification.healPerLevel = BUILDER.comment("Healing amount increase per level").defineInRange("heal_per_level", 0.5D, 0.0D, 1000.0D);
        HymnofPurification.baseMana = BUILDER.comment("Base mana cost").defineInRange("base_mana", 80, 0, 10000);
        HymnofPurification.manaPerLevel = BUILDER.comment("Mana cost increase per level").defineInRange("mana_per_level", 0, 0, 1000);
        HymnofPurification.cooldown = BUILDER.comment("Cooldown in seconds").defineInRange("cooldown_seconds", 60.0D, 0.0D, 3600.0D);
        BUILDER.pop();

        BUILDER.push("gilded_hare");
        GildedHare.baseMana = BUILDER.comment("Base mana cost").defineInRange("base_mana", 80, 0, 10000);
        GildedHare.manaPerLevel = BUILDER.comment("Mana cost increase per level").defineInRange("mana_per_level", 0, 0, 1000);
        GildedHare.cooldown = BUILDER.comment("Cooldown in seconds").defineInRange("cooldown_seconds", 60.0D, 0.0D, 3600.0D);
        BUILDER.pop();

        // GROUND
        BUILDER.push("tigershade_terrabreak");
        TigershadeTerrabreak.baseDamage = BUILDER.comment("Base slam damage at Level 1").defineInRange("base_damage", 30.0D, 0.0D, 10000.0D);
        TigershadeTerrabreak.damagePerLevel = BUILDER.comment("Damage increase per level").defineInRange("damage_per_level", 3.0D, 0.0D, 1000.0D);
        TigershadeTerrabreak.baseMana = BUILDER.comment("Base mana cost").defineInRange("base_mana", 40, 0, 10000);
        TigershadeTerrabreak.manaPerLevel = BUILDER.comment("Mana cost increase per level").defineInRange("mana_per_level", 0, 0, 1000);
        TigershadeTerrabreak.cooldown = BUILDER.comment("Cooldown in seconds").defineInRange("cooldown_seconds", 30.0D, 0.0D, 3600.0D);
        BUILDER.pop();

        BUILDER.push("jade_aura");
        JadeAura.baseDuration = BUILDER.comment("Base aura duration in seconds").defineInRange("base_duration_seconds", 45.0D, 1.0D, 3600.0D);
        JadeAura.durationPerLevel = BUILDER.comment("Duration increase per level in seconds").defineInRange("duration_per_level_seconds", 10.0D, 0.0D, 600.0D);
        JadeAura.baseMana = BUILDER.comment("Base mana cost").defineInRange("base_mana", 50, 0, 10000);
        JadeAura.manaPerLevel = BUILDER.comment("Mana cost increase per level").defineInRange("mana_per_level", 25, 0, 1000);
        JadeAura.cooldown = BUILDER.comment("Cooldown in seconds").defineInRange("cooldown_seconds", 40.0D, 0.0D, 3600.0D);
        BUILDER.pop();

        // NATURE
        BUILDER.push("wings_of_tempest");
        WingsofTempest.baseDuration = BUILDER.comment("Base storm duration in seconds").defineInRange("base_duration_seconds", 14.25D, 1.0D, 3600.0D);
        WingsofTempest.durationPerLevel = BUILDER.comment("Duration increase per level in seconds").defineInRange("duration_per_level_seconds", 2.25D, 0.0D, 600.0D);
        WingsofTempest.baseRadius = BUILDER.comment("Base storm radius").defineInRange("base_radius", 8.0D, 1.0D, 64.0D);
        WingsofTempest.baseMana = BUILDER.comment("Base mana cost").defineInRange("base_mana", 40, 0, 10000);
        WingsofTempest.manaPerLevel = BUILDER.comment("Mana cost increase per level").defineInRange("mana_per_level", 10, 0, 1000);
        WingsofTempest.cooldown = BUILDER.comment("Cooldown in seconds").defineInRange("cooldown_seconds", 22.0D, 0.0D, 3600.0D);
        BUILDER.pop();

        BUILDER.push("venomous_blossomfall");
        VenomousBlossomfall.shortDamage = BUILDER.comment("Short charge direct damage").defineInRange("short_damage", 15.0D, 0.0D, 10000.0D);
        VenomousBlossomfall.mediumDamage = BUILDER.comment("Medium charge direct damage").defineInRange("medium_damage", 35.0D, 0.0D, 10000.0D);
        VenomousBlossomfall.fullDamage = BUILDER.comment("Full charge direct damage").defineInRange("full_damage", 50.0D, 0.0D, 10000.0D);
        VenomousBlossomfall.damagePerLevel = BUILDER.comment("Damage increase per level").defineInRange("damage_per_level", 5.0D, 0.0D, 1000.0D);
        VenomousBlossomfall.baseMana = BUILDER.comment("Base mana cost").defineInRange("base_mana", 40, 0, 10000);
        VenomousBlossomfall.manaPerLevel = BUILDER.comment("Mana cost increase per level").defineInRange("mana_per_level", 5, 0, 1000);
        VenomousBlossomfall.cooldown = BUILDER.comment("Cooldown in seconds").defineInRange("cooldown_seconds", 20.0D, 0.0D, 3600.0D);
        BUILDER.pop();

        BUILDER.push("gale_piercer");
        GalePiercer.normalBaseDamage = BUILDER.comment("Normal release arrow base damage").defineInRange("normal_base_damage", 12.0D, 0.0D, 10000.0D);
        GalePiercer.normalDamagePerLevel = BUILDER.comment("Normal arrow damage increase per level").defineInRange("normal_damage_per_level", 2.0D, 0.0D, 1000.0D);
        GalePiercer.fullBaseDamage = BUILDER.comment("Full charge arrow base damage").defineInRange("full_base_damage", 28.0D, 0.0D, 10000.0D);
        GalePiercer.fullDamagePerLevel = BUILDER.comment("Full charge arrow damage increase per level").defineInRange("full_damage_per_level", 4.0D, 0.0D, 1000.0D);
        GalePiercer.baseMana = BUILDER.comment("Base mana cost").defineInRange("base_mana", 40, 0, 10000);
        GalePiercer.manaPerLevel = BUILDER.comment("Mana cost increase per level").defineInRange("mana_per_level", 5, 0, 1000);
        GalePiercer.cooldown = BUILDER.comment("Cooldown in seconds").defineInRange("cooldown_seconds", 20.0D, 0.0D, 3600.0D);
        BUILDER.pop();

        BUILDER.push("rapturous_bloom");
        RapturousBloom.baseDamage = BUILDER.comment("Base lotus bloom burst damage at Level 1").defineInRange("base_damage", 24.0D, 0.0D, 10000.0D);
        RapturousBloom.damagePerLevel = BUILDER.comment("Damage increase per level").defineInRange("damage_per_level", 4.0D, 0.0D, 1000.0D);
        RapturousBloom.baseMana = BUILDER.comment("Base mana cost").defineInRange("base_mana", 45, 0, 10000);
        RapturousBloom.manaPerLevel = BUILDER.comment("Mana cost increase per level").defineInRange("mana_per_level", 5, 0, 1000);
        RapturousBloom.cooldown = BUILDER.comment("Cooldown in seconds").defineInRange("cooldown_seconds", 16.0D, 0.0D, 3600.0D);
        BUILDER.pop();

        // AQUA
        BUILDER.push("crimson_rain_bathes_moon");
        CrimsonRainBathesMoon.baseDamage = BUILDER.comment("Spear impact base damage at Level 1").defineInRange("base_damage", 7.0D, 0.0D, 10000.0D);
        CrimsonRainBathesMoon.damagePerLevel = BUILDER.comment("Damage increase per level").defineInRange("damage_per_level", 5.0D, 0.0D, 1000.0D);
        CrimsonRainBathesMoon.baseMana = BUILDER.comment("Continuous cast mana cost per tick interval").defineInRange("base_mana", 5, 0, 10000);
        CrimsonRainBathesMoon.manaPerLevel = BUILDER.comment("Mana cost increase per level").defineInRange("mana_per_level", 3, 0, 1000);
        CrimsonRainBathesMoon.cooldown = BUILDER.comment("Cooldown in seconds").defineInRange("cooldown_seconds", 60.0D, 0.0D, 3600.0D);
        BUILDER.pop();

        BUILDER.push("glacial_veil");
        GlacialVeil.baseDamage = BUILDER.comment("Frost shockwave base damage at Level 1").defineInRange("base_damage", 20.0D, 0.0D, 10000.0D);
        GlacialVeil.damagePerLevel = BUILDER.comment("Damage increase per level").defineInRange("damage_per_level", 3.0D, 0.0D, 1000.0D);
        GlacialVeil.baseMana = BUILDER.comment("Base mana cost").defineInRange("base_mana", 50, 0, 10000);
        GlacialVeil.manaPerLevel = BUILDER.comment("Mana cost increase per level").defineInRange("mana_per_level", 10, 0, 1000);
        GlacialVeil.cooldown = BUILDER.comment("Cooldown in seconds").defineInRange("cooldown_seconds", 20.0D, 0.0D, 3600.0D);
        BUILDER.pop();

        BUILDER.push("glacial_firmament");
        GlacialFirmament.baseDamage = BUILDER.comment("Ice spike eruption base damage at Level 1").defineInRange("base_damage", 35.0D, 0.0D, 10000.0D);
        GlacialFirmament.damagePerLevel = BUILDER.comment("Damage increase per level").defineInRange("damage_per_level", 5.0D, 0.0D, 1000.0D);
        GlacialFirmament.baseMana = BUILDER.comment("Base mana cost").defineInRange("base_mana", 60, 0, 10000);
        GlacialFirmament.manaPerLevel = BUILDER.comment("Mana cost increase per level").defineInRange("mana_per_level", 15, 0, 1000);
        GlacialFirmament.cooldown = BUILDER.comment("Cooldown in seconds").defineInRange("cooldown_seconds", 25.0D, 0.0D, 3600.0D);
        BUILDER.pop();

        BUILDER.pop(); // spells
        SPEC = BUILDER.build();
    }
}
