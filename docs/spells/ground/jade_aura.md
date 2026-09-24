# Jade Aura

## Identity and Configuration

- **Registry ID**: `ironspell_more:jade_aura`
- **School**: Ground (`bhspells:ground`, with dynamic fallback to `SchoolRegistry.EVOCATION` when BHSpells is absent)
- **Rarity**: Rare (`SpellRarity.RARE`)
- **Maximum Level**: 3
- **Cast Type**: Instant (`CastType.INSTANT`)
- **Cast Time**: 0
- **Base Mana Cost**: 50
- **Mana Cost Per Level**: +25 (Level 1: 50, Level 2: 75, Level 3: 100)
- **Cooldown**: 40.0 seconds
- **Target Range**: 16.0 blocks
- **Cast Start Animation**: `SpellAnimations.SELF_CAST_ANIMATION`
- **Cast Start Sound**: None
- **Cast Finish Sound**: `SoundEvents.AMETHYST_BLOCK_CHIME`

## Combat Mechanics & Spell Flow

Jade Aura channels the earthen resonance of ancient jade to empower the caster and optionally an allied combatant:

### 1. Dual-Targeting Mechanism (Self & Ally Selection)
- **Direct Raycast Aiming**:
  - Performs a precise block-aware raycast up to **16 blocks** along the caster's look angle (`Utils.raycastForEntity(level, caster, 16.0F, true, 0.5F)`).
  - Target eligibility is determined through `isTargetAlly(caster, target)`:
    - Target must be alive and not a spectator.
    - Matches if `target.isAlliedTo(caster)` is true.
    - Matches if both are players and friendly fire is disabled between them.
    - Matches if target is a tamed animal owned by the caster.
- **Branching Behavior**:
  - **Aiming at Ally**: Both the targeted ally and the caster receive the `JadeAuraEffect` buff and trigger the cast burst VFX.
  - **Aiming Elsewhere (Air, Block, Enemy, Obstructed, Out of Range)**: Only the caster receives the buff and triggers the cast burst VFX.
  - No cone fallback is used, preventing accidental ally buffing when aiming past an ally or looking at enemies.

### 2. Pre-Buff Duration Calculation
- Duration is calculated exactly once prior to applying the effect to either entity:
  - `duration = (int) (getSpellPower(spellLevel, caster) * 20)`
  - Base duration scales from 45 seconds at Level 1 up to 65 seconds at Level 3 (Level 1: 45s, Level 2: 55s, Level 3: 65s, further boosted by external Ground/Evocation Spell Power equipment).
  - Pre-computing the duration ensures that the caster's newly gained Spell Power modifier does not artificially inflate the ally's buff duration or create order-dependent asymmetry.

### 3. Buff Modifiers (`JadeAuraEffect`)
Modeled after `ChargeSpell` from Iron's Spells 'n Spellbooks, but ground/jade-themed and completely decoupled from lightning attributes and overlays:
- **Beneficial `MagicMobEffect`** with custom Verdigris tint (`#3EB9A8` / `0x3EB9A8`), fully integrating with Iron's counterspell and dispel mechanics.
- **Attack Damage**: `+10%` per level (`Attributes.ATTACK_DAMAGE`, `MULTIPLY_TOTAL`, +10% / +20% / +30%).
- **Movement Speed**: `+20%` per level (`Attributes.MOVEMENT_SPEED`, `MULTIPLY_TOTAL`, +20% / +40% / +60%).
- **Spell Power**: `+5%` per level (`AttributeRegistry.SPELL_POWER`, `MULTIPLY_TOTAL`, +5% / +10% / +15%).

### 4. Visual Effects & Palette

The spell features custom visual work utilizing a vibrant, high-luminance electric jade palette:
- **Electric Jade Core**: `#D2FFEE` (RGB: `210, 255, 238`)
- **Luminous Verdigris**: `#69F8DC` (RGB: `105, 248, 220`)
- **Radiant Jade**: `#87FAB4` (RGB: `135, 250, 180`)
- **Electric Jade Glow**: `#32DAB9` (RGB: `50, 218, 185`)
- **Verdigris**: `#3EB9A8` (RGB: `62, 185, 168`)
- **Forest Green**: `#5AA371` (RGB: `90, 163, 113`)

#### Cast Burst ("กระจายป้าง")
- Upon cast, concentric ground rings (radii 0.6m, 1.2m, 1.8m) expand outward around each buffed entity using alternating jade colors.
- Upward wisps and filaments rise around the torso.
- Accompanied by acoustic resonance (`SoundEvents.AMETHYST_BLOCK_CHIME`, `SoundEvents.BEACON_POWER_SELECT`, `SoundEvents.PLAYER_ATTACK_SWEEP`).

#### Sustained Aura ("ออร่าวนรอบตัวจางๆ")
- `JadeAuraRenderEvents` listens to `RenderLivingEvent.Post` on the client Forge event bus and renders the aura around every living entity carrying the Jade Aura effect, including players and allied mobs.
- The primary visual is full-bright translucent geometry: three continuous helical lightning ribbons with broad electric glow layers and bright radiant cores, plus two gently rippling orbit rings at different body heights.
- **Electric Lightning Ribbon Geometry**:
  - Helical strands feature deterministic 3D zig-zag displacements generated from hash noise sampled across segment index, strand ID, and stepped time (`(int)(ageInTicks * 1.6F)`). This produces crisp, crackling electric lightning fractures around the entity's body.
  - Rendered with double-sided quads so that lightning fractures are vivid and sharp from every viewing perspective.
  - Expanded opacity envelope ensures strands remain solidly luminous across >70% of their length before fading gently at the ends.
- Geometry scales from each entity's current bounding-box width and height, uses a deterministic entity-specific phase, and animates smoothly with partial ticks. Opposing rotation and slightly different radii prevent the strands from collapsing into a single flat band.
- The renderer uses the vanilla `minecraft:textures/misc/white.png` texture with `RenderType.entityTranslucentEmissive`, high-luminance electric jade vertex tinting, full-bright lighting, normal depth testing, and no custom network packets or binary texture assets.
- Rendering uses two distance-detail levels: full three-strand/two-ring geometry through 24 blocks, reduced two-strand/one-ring geometry from 24 to 48 blocks, and no custom aura geometry past 48 blocks.
- Supporting `DustParticleOptions` wisps still orbit the body every 3 client ticks, but they are client-local and subordinate to the ribbon geometry. The mob effect itself no longer emits sustained server particles.
- Completely avoids the vanilla lightning Creeper/Charged overlay.

#### Client / Server Responsibilities
- The server remains authoritative for applying and synchronizing `JadeAuraEffect` and for the one-shot cast burst particles and sounds.
- Vanilla mob-effect synchronization is sufficient for the client renderer; Jade Aura adds no networking.
- Continuous ribbons, LOD selection, smooth animation, and sustained sparkle emission are client-only. This avoids sustained `sendParticles` traffic and server tick work while the buff is active.

## Progression & Scaling

| Level | Mana Cost | Attack Damage | Movement Speed | Spell Power | Base Duration | Cooldown |
| :---: | :-------: | :-----------: | :------------: | :---------: | :-----------: | :------: |
| 1     | 50        | +10%          | +20%           | +5%         | 45.0s         | 40.0s    |
| 2     | 75        | +20%          | +40%           | +10%        | 55.0s         | 40.0s    |
| 3     | 100       | +30%          | +60%           | +15%        | 65.0s         | 40.0s    |

*Note: Duration scales with the caster's Ground / Evocation Spell Power.*

## Assets & Resources

- Spell Icon: `assets/ironspell_more/textures/gui/spell_icons/jade_aura.png`
- Effect Icon: `assets/ironspell_more/textures/mob_effect/jade_aura.png`
- Sustained Aura Texture: vanilla `minecraft:textures/misc/white.png` (tinted at render time; no custom texture asset)
- Language Keys:
  - `spell.ironspell_more.jade_aura`: "Jade Aura"
  - `spell.ironspell_more.jade_aura.guide`: Detailed guide description
  - `effect.ironspell_more.jade_aura`: "Jade Aura"
