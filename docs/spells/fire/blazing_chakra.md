# Blazing Chakra

## Identity and Configuration

- Registry ID: ironspell_more:blazing_chakra
- School: Fire (SchoolRegistry.FIRE_RESOURCE)
- Rarity: Epic (SpellRarity.EPIC)
- Maximum Level: 1 (Every scroll in-game exists as 1 tier at Level 1; level scaling is available via `/cast <player> <spell> <level>` command and server configuration `ironspell_more-server.toml`)
- Cast Type: INSTANT
- Cast Time: 0
- Base Mana Cost: 50
- Mana Cost Per Level: +10
- Cooldown: 15.0 seconds
- Cast Finish Animation: SpellAnimations.OVERHEAD_MELEE_SWING_ANIMATION (Fallback) / BlazingChakraAnimations.BLAZING_CHAKRA (Epic Fight)

## Combat Mechanics & Area of Effect

Blazing Chakra combines Epic Fight attack animations with Iron's Spells fire magic:

### 1. Synchronized Epic Fight Execution (Frame 59 Impact)
- **Startup Phase (0.0s - 0.98s)**:
  - Caster leaps into the air with white afterimages (AfterimageVfx).
  - Pre-attack charging thunder sound (SoundEvents.TRIDENT_THUNDER) plays at 0.2s.
  - Temporary damage resistance buff (DAMAGE_RESISTANCE V for 6 seconds) ensures uninterrupted execution.
- **Ground Impact (Frame 59 / ~0.983s)**:
  - **Melee Strike**: Hits in a 4x4x4m oriented bounding box (BlazingChakraColliders.IMPACT) with 100% armor negation and knockdown stun.
  - **Camera Shake & Fracture**: The shared Epic Fight helper finds supporting ground and calls `LevelUtil.circleSlamFracture` with a 5-block radius, particles enabled, sound suppressed, and entity damage disabled. It plays `SLAM_LIGHT` at 0.2 volume instead, while the camera shakes with strength 4.0.
  - **Concentric Flame Shockwave**: Concentric fiery ripple waves and orange/red particle bursts scatter up to 12 blocks (BlazingChakraVfx.spawnImpactClientVfx).
  - **Explosion Sound**: SoundEvents.GENERIC_EXPLODE at impact origin.
  - **Proximity Damage Falloff**:
    - **Close Range (\le 3 blocks)**: Deals 100% full spell power / maximum heavy damage (base 20 + 4/lvl) + 5s fire duration.
    - **Outer Range (3 - 12 blocks)**: Smooth linear falloff scaling down to 25% damage at 12 blocks + 2s fire duration.
  - **Unconditional Fire Ignition**: Every living entity within 12 blocks catches on fire immediately upon ground impact, regardless of whether damage was blocked or shielded.
  - **Radial Knockback**: Hits push targets outward from the point of impact with force scaling inversely with distance.
- **Secondary Expanding Shockwave (1.85s)**:
  - 4 expanding concentric fire rings propagate outward with lingering flame and smoke particles.

### 2. Standalone Fallback Mode (Without Epic Fight)
When Epic Fight is not installed:
- Caster plays overhead melee swing animation.
- Impact damage, fire ignition, radial knockback, explosion sound, and spiral particles trigger automatically at 20 ticks (1.0s) via server tick task.

## Progression & Scaling

| Level | Mana Cost | Base Damage (Close) | Min Damage (12m) | Fire Duration (Close) | Fire Duration (Far) | Cooldown |
| :---: | :-------: | :-----------------: | :--------------: | :-------------------: | :-----------------: | :------: |
| 1     | 50        | 24.0                | 6.0              | 6.0s                  | 3.0s                | 15.0s    |
| 2     | 60        | 28.0                | 7.0              | 7.0s                  | 4.0s                | 15.0s    |
| 3     | 70        | 32.0                | 8.0              | 8.0s                  | 5.0s                | 15.0s    |
| 4     | 80        | 36.0                | 9.0              | 9.0s                  | 6.0s                | 15.0s    |
| 5     | 90        | 40.0                | 10.0             | 10.0s                 | 7.0s                | 15.0s    |

## Tooltip UI Information
- **Damage**: ui.irons_spellbooks.damage
- **Radius**: ui.irons_spellbooks.radius (12.0 blocks)
- **Effect Length**: ui.irons_spellbooks.effect_length ((5 + spellLevel) seconds)
