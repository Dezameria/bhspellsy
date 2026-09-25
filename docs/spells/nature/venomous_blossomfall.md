# Venomous Blossomfall

## Identity and configuration

- ตัวละคร: หูเหยียน เฟิงเซียว (Huyan Fengxiao)
- Registry ID: `ironspell_more:venomous_blossomfall`
- School: Nature
- Rarity: Legendary
- Maximum level: 1
- Cast type: `LONG`
- Stage 2 threshold: 100 ticks (5 seconds)
- Full charge/Stage 3 threshold: 300 ticks (15 seconds)
- Mana cost: 100
- Base cooldown: 30 seconds
- Animation: Iron's Spells Bow-charge animation
- Sounds:
  - Custom Charge Sounds:
    - Phase 1: `ironspell_more:venomous_blossomfall_charge_1` (`sounds/venomousblossomfall_charge_1.ogg`), เล่นเมื่อเริ่มชาร์จ 1 รอบจนจบ
    - Phase 2: `ironspell_more:venomous_blossomfall_charge_2` (`sounds/venomousblossomfall_charge_2.ogg`), เริ่มเล่นหลังชาร์จไปได้ประมาณ 21 วินาที (420 ticks) และวนลูปซ้ำทุกๆ 11 วินาที (220 ticks)
    - ทันทีที่ปล่อยเวทหรือยกเลิกการชาร์จ: ตัดเสียงทั้งหมด (`stopSound`) ทันที
  - Charge Stage Sounds: เสียงแจ้งเตือนระดับการชาร์จ (Stage 2: Amethyst/Experience, Stage 3: LevelUp/Beacon)
  - Release Sounds: ไล่ระดับความหนักแน่นตาม Stage:
    - Stage 1 (Short): Arrow Shoot + `traveloptics:blast_stage_one` (vol 0.5, pitch 2.0)
    - Stage 2 (Medium): Crossbow Shoot + Arrow Shoot + `traveloptics:blast_stage_one` (vol 0.7, pitch 1.0)
    - Stage 3 (Full): Generic Explode + Lightning Thunder + Crossbow Shoot + `wom:sfx.solar_hit` (vol 0.2, pitch 1.0)

All gameplay and visual thresholds, charge stages, and charge helpers are centralized in `VenomousBlossomfallSpell`.

## Starting and aiming

The spell does not require the crosshair to hit a target. It can be charged and fired into empty space like an FPS projectile.

Starting the cast is rejected only when a living entity is both:

1. Within 5 blocks of the caster's eye position, measured to the entity bounding-box center.
2. Inside the caster's authoritative 140-degree view cone (70 degrees either side of the look vector).
3. In line of sight according to `LivingEntity.hasLineOfSight`.

An entity behind the caster or hidden by blocks does not prevent casting. The server owns this check; the fixed cone approximates the visible screen without trusting client FOV settings.

Yaw and pitch remain free throughout the cast, and the release direction is the caster's current server-side look vector. Forward/backward movement, strafing, jumping, upward velocity, and sprinting are suppressed while casting. The server stores an X/Z anchor, authoritatively anchors horizontal displacement, suppresses upward motion, and only sets velocity/hurt marks when needed, permitting completely smooth downward falling without recurring packet jitter. The client also clears movement input for responsiveness. The anchor is removed when casting ends or is cancelled, and explicit cleanup handles death, disconnect, and dimension changes.

Releasing before 300 ticks is a valid cast, not a cancelled spell. The accumulated authoritative charge determines projectile speed and maximum traveled distance. Stage 1 lasts from cast start until just before 100 ticks, Stage 2 starts at 100 ticks, and Stage 3 starts at 300 ticks. Gameplay charge reaches 100% at 300 ticks independently of the native LONG-cast timer. The native timer is deliberately kept far beyond any practical session, while client and server use-item timers are refreshed when necessary. Full charge therefore remains at 100% until explicit release instead of firing automatically. Releasing the use button still fires immediately. Non-release cancellation paths for a living caster that Iron's Spells reports through the same LONG-cast cancellation callback are also resolved as an early shot.

For `/cast` and other external starts that do not enter vanilla's use-item state, pressing right click while this spell
is active sends Iron's Spells' existing `CancelCastPacket` as an explicit release request. The server verifies the
currently active spell and resolves that request through the same charged-shot completion path. The input is
consumed so the held item is not also used. This adds no addon-specific packet and keeps charge, spawning, damage,
range, and cooldown authoritative on the server.

## Charging preview and VFX

The charging needle is client-only rendered geometry; it is not an entity or projectile. It uses `AzureVenomNeedleModel`, a temporary `MeshDefinition` cuboid shaped like a slender chopstick/needle, and is positioned relative to the caster's eye, main hand, and interpolated look vector.

The preview follows yaw and pitch, points at the crosshair, and rolls around its local forward axis. Roll speed rises with normalized charge. Green and azure particles progress through these visual tiers:

- Stage 1 (under 5 seconds): sparse dust, slow needle roll, and a private action-bar message for the caster.
- Stage 2 (5 to under 15 seconds): denser spiral particles, sparks, expanding cyan shockwave rings around the caster, and a stage message.
- Stage 3/Full Charge (15 seconds or more): maximum spiral density, fastest roll, expanding waves, Cataclysm `StormParticle` orbs around the caster, and a full-charge release prompt visible only to the caster.

Remote-player charge progress is estimated client-side for visuals only. It never determines projectile speed, damage, collision, cooldown, or status effects.

## Projectile

- Entity ID: `ironspell_more:azure_venom_needle`
- Class: `AzureVenomNeedleEntity`
- Spawn time: only when the spell is released/completes
- Stage 1 maximum traveled distance: 30 blocks
- Stage 2 maximum traveled distance: 60 blocks
- Stage 3 maximum traveled distance: 90 blocks
- Stage 1 direct damage: 50 HP
- Stage 2 direct damage: 50 HP
- Stage 3 direct damage: 50 HP
- Gravity: none
- Motion: fixed normalized direction and fixed speed
- Short-charge speed: 0.8 blocks/tick
- Medium-charge speed: 4 blocks/tick
- Full-charge speed: 7 blocks/tick

Each charge stage selects its independently tunable direct damage, projectile speed, and maximum range. The current
damage defaults remain 50/50/50 HP, so charging does not change damage until those stage constants are deliberately
rebalanced. Charge also increases visual trail intensity.

Collision is continuous. Each server tick sweeps the complete segment from the previous position to the range-clamped next position. The block raycast limits the entity sweep, entity hits are ordered along the segment, and the nearest valid non-cancelled collision resolves first. The projectile does not rely on an overlap test at its end position, preventing high-speed tunnelling. Actual segment distance is accumulated independently of owner movement. A `resolved` guard ensures only one hit/miss result can be applied.

The client trail is an azure/cyan spiral whose length, density, and radius increase with charge. A fully charged
needle also emits expanding fullbright cyan sonic-boom rings continuously along its complete flight path. The rings
are oriented perpendicular to the needle's flight direction and sampled every 3 traveled blocks, rather than once
per tick, so they follow the projectile with stable spacing at high speeds. Their maximum radii cycle through 2.4,
2.95, and 3.5 blocks. This reuses the addon's existing `ShockwaveParticleCustom`; it does not spawn extra entities
or affect collision.

## Damage and Azure Venom

A direct entity hit deals the server-authoritative direct damage configured for the projectile's captured charge
stage, then applies `ironspell_more:azure_venomous` for 100 ticks at amplifier 0. The stage damage defaults are
currently 50/50/50 HP and do not use spell-power scaling.

Azure Venom deals 3 HP magic damage every 20 ticks for five ticks, totaling 15 HP (7.5 hearts). It does not deal or duplicate the initial 50 HP impact damage.

A block hit or reaching the current stage's 30/60/90-block traveled-distance limit is a miss: no direct damage and no Azure Venom.

## Outcome-dependent cooldown

Iron's Spells applies the spell's native 30-second cooldown when the cast completes. The projectile preserves that cooldown for a block hit, maximum-range miss, or empty shot. On a direct entity hit, it retrieves the existing native `PlayerCooldowns` entry, doubles its effective tick duration to 60 seconds, replaces it through the native cooldown API, and synchronizes it to the owning server player.

This avoids a parallel cooldown manager and keeps the spell callable through normal Iron's Spells integrations, including external casting systems. If no native cooldown entry exists (for example, an external source deliberately bypassed cooldown creation), the projectile does not invent one.

Every resolved projectile also replaces `ironspell_more:cooldown` on its caster with a data-only marker
`MobEffect`. The effect has no tick behavior, particles, attribute modifier, damage, or other gameplay action. Its
HUD/inventory icon remains visible, while its amplifier is intended for commands and external systems to inspect:

- Miss, block hit, or maximum range: amplifier 0 for 30 seconds.
- Stage 1 direct hit: amplifier 1 for 60 seconds.
- Stage 2 direct hit: amplifier 2 for 60 seconds.
- Stage 3 direct hit: amplifier 3 for 60 seconds.

The old marker is explicitly removed before the new one is added so a lower result can replace a higher amplifier
when command/external casting bypasses the normal cooldown. This marker supplements the native Iron's Spells
cooldown; it does not replace or enforce it.

## Epic Fight compatibility

Epic Fight is optional. Calls are isolated behind `compat/epicfight/EpicFightFractureHelper`; the projectile itself contains no Epic Fight classes in its public boundary.

Only a fully charged needle requests fracture effects. Sampling occurs every 2.5 blocks of actual projectile travel. At each sample, the fracture origin uses the horizontal sample X/Z combined with the caster's launch-height ground reference Y (`caster.getY() + 0.2`). The loaded-only bridge searches a single-column interval from 2 blocks above to 6 blocks below that ground reference for supporting ground and invokes `LevelUtil.circleSlamFracture` at the supporting block with a 1.75-block radius, `noSound = true`, `noParticle = false`, and `hurtEntities = false`. It then plays a softer `SLAM_LIGHT` sound at 0.2F volume with subtle pitch variation, keeping the ground cracks visible without adding Epic Fight shockwave damage or deafening audio stacking. It does not scan from projectile/eye height, ensuring ground fractures reliably trigger beneath horizontal and upward shots. When Epic Fight is absent or its linkage is unavailable, the helper safely does nothing.

## Ownership

Server-authoritative responsibilities:

- Start eligibility and FOV/line-of-sight proximity check
- Charge state used for projectile speed
- Movement lock
- Projectile spawn, direction, speed, range, continuous collision, and removal
- Direct damage, Azure Venom application, outcome, and cooldown adjustment
- Full-charge fracture sampling request

Client-only responsibilities:

- Charging preview geometry and roll interpolation
- Charge particles
- Projectile model, trail particles, and full-charge flight-path sonic rings
- Camera-relative positioning
- Converting right click into Iron's native release request when the active cast was started externally

No custom networking layer is required. Existing Iron's Spells cast synchronization and vanilla entity synchronized data carry the necessary state.

## Assets and registrations

- Spell icon: `textures/gui/spell_icons/venomous_blossomfall.png`
- Effect icon: `textures/mob_effect/azure_venomous.png`
- Temporary model layer: `ironspell_more:azure_venom_needle#main`
- Renderer: `AzureVenomNeedleRenderer`
- Effect: `AzureVenomousEffect`
- Outcome marker effect: `CooldownEffect` (`ironspell_more:cooldown`)
- Cooldown marker icon: `textures/mob_effect/cooldown.png` (particles hidden, icon visible)
- Language keys cover spell name/guide, entity, effect, proximity rejection, and poison information.

The two icons were generated as project-specific raster assets and resized to 64x64 and 16x16 respectively. The needle entity currently reuses the addon's white-fire texture as a cyan-tinted render layer until the planned Geo model and dedicated texture are available.

## Verification and known limitations

- `compileJava` must pass after implementation.
- A full Gradle build verifies resource processing and packaging.
- Runtime checks still required in a client/server game: FOV edge cases, early release, movement lock cleanup, high-speed hit order, shield/anti-magic interaction, cooldown replacement, remote preview alignment, full-charge sonic-ring orientation/spacing, and behavior with Epic Fight absent/present.
- The server cannot know the user's configurable client FOV exactly, so start eligibility uses the documented fixed 140-degree cone.
