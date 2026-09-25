# Gale Piercer

## Identity and configuration

- ตัวละคร: ลันลัน (Lanlan)
- Registry ID: `ironspell_more:gale_piercer`
- School: Nature
- Rarity: Rare
- Maximum level: 1 (Every scroll in-game exists as 1 tier at Level 1; level scaling is available via `/cast <player> <spell> <level>` command and server configuration `ironspell_more-server.toml`)
- Cast type: `LONG` (Hold-to-charge, release-to-shoot)
- Full charge threshold ("ศรวายุ"): 200 ticks (10.0 seconds)
- Mana cost: Base 40, +5 per level
- Cooldown: 20.0 seconds
- Cast start animation: `SpellAnimations.BOW_CHARGE_ANIMATION` (ยกคันธนูขึ้นน้าวสาย)
- In-hand charge model: 4-crossed-quad Fire Arrow model attached to the caster's main hand via `GalePiercerChargeLayer`:
  - **Initial / Charging (< 200 ticks)**: Rendered as a white-gray arrow (`ศรสีขาวเทา`) using `textures/entity/gale_piercer/white_gray_arrow.png`.
  - **Full Charge (>= 200 ticks)**: Transforms into a blazing flame arrow (`ศรเพลิง`) using `textures/entity/fire_arrow.png` with full-bright emissive rendering.
- Particle visual effects:
  - **Arrow Position Alignment**: Every charging particle is based on the physical in-hand arrow anchor (`arrowCenter = eyePos + lookDir*0.42 + rightDir*sideOffset - 0.35Y`) and points sampled along its shaft. The side offset follows `HumanoidArm.RIGHT` or `LEFT`, and the perpendicular basis remains stable while aiming vertically.
  - **Charging Wind (< 200 ticks)**: Two white/pale wind ribbons form a double helix around the arrow shaft. Three surrounding spiral streams each display three samples from a distant 1.80-block radius down to 0.08 blocks plus a fixed sink point on the shaft. Each cycle advances slowly over 40 ticks (`0.025` normalized progress/tick), and the middle sample uses a directional Iron's Spells `ParticleHelper.EMBERS` particle moving toward the sink.
  - **Full Charge Entrance (= 200 ticks)**: The wind pattern remains on the arrow while 18 directional Iron's Spells `ParticleHelper.EMBERS` particles collapse inward from three rings, followed by an arrow-centered `FLASH` and the full-charge chimes. The transition gathers inward; it does not burst outward.
  - **Full Charge Hold (> 200 ticks)**: The arrow-centered wind helix and slow inward gathering continue at a faster rotation. Three `SMALL_FLAME` samples follow one rotating helix arm along the shaft, while sparse orange and crimson dust sparks remain embedded in the wind.
  - **Particle budget**: Continuous charging VFX is emitted every 2 ticks: 18 wind/convergence particles normally, or 23 at full charge after adding 3 helical flames and 2 colored sparks. The threshold adds one `FLASH` and 18 one-shot Iron's Spells embers.
- Sounds:
  - Cast Start: Bow pull sound (`SoundEvents.ARROW_SHOOT` / `BOW_PULL`)
  - Charge Complete (10s): Level up sound and beacon chime (`SoundEvents.PLAYER_LEVELUP`, `SoundEvents.BEACON_ACTIVATE`)
  - Early Release: Arrow release with wind whoosh (`SoundEvents.ARROW_SHOOT`, `SoundEvents.CROSSBOW_SHOOT`)
  - Full Charge Release: Thunderous wind explosive blast (`SoundEvents.GENERIC_EXPLODE`, `SoundEvents.LIGHTNING_BOLT_THUNDER`, `SoundEvents.CROSSBOW_SHOOT`)
- Balance constants:
  - Max lock-on distance: 48.0 blocks
  - Normal direct damage: Base 12.0 HP (+2.0 HP per level)
  - Full charge direct damage: Base 28.0 HP (+4.0 HP per level)
  - Projectile speed by charge: 0.8 blocks/tick at 0 ticks, smoothly increasing to 6.0 blocks/tick at 200 ticks
  - Speed formula: `0.8 + 5.2 * (p² * (3 - 2p))`, where `p = clamp(chargeTicks, 0, 200) / 200`
  - Full charge projectile speed: 6.0 blocks/tick
  - Wind arrow max turn rate: 0.08 radians/tick (~4.58 degrees/tick)
  - Full charge fire duration: 4.0 seconds
  - Full charge slowness: Slowness I (Amplifier 0) for 5.0 seconds (100 ticks)
  - Wind arrow knockback: 0.4 horizontal force
  - Gale arrow knockback: 0.8 horizontal force + 0.2 vertical lift
  - Arrow maximum lifetime: 300 ticks (15.0 seconds)

All thresholds, scaling, and damage formulas are centralized in `GalePiercerSpell`.

## Starting and aiming (Lock-on targeting)

1. When casting begins, the caster enters the bow-charging stance and holds the button/key.
2. The caster aims toward an intended target within **48 blocks**:
   - The target must be a valid `LivingEntity`, alive, non-spectator, and not friendly/allied.
   - **Line-of-Sight Check**: The target must have direct line of sight from the caster's eye position (`LivingEntity.hasLineOfSight`).
   - If a wall or solid block obstructs the target, the initial lock-on cannot be initiated through the wall.
3. Once a target is successfully acquired:
   - The target is locked on the server and visually highlighted (using Iron's `SyncTargetingDataPacket`).
   - If the locked target temporarily moves behind cover while charging continues, the lock is preserved as long as the target remains within 48 blocks and in the same dimension.
   - If the player aims directly at another valid visible enemy, the lock switches to the new enemy.
4. **No-Lock Fallback**:
   - If the player releases the bow without having locked onto any target, the arrow is fired straight forward along the player's look vector as an unguided ballistic shot.

## Releasing and charge stages (Flowskill)

- **Hold-to-Charge Mechanic**:
  - The caster holds the right-click use button or spell key to keep drawing and charging the bow.
  - Releasing the button/key immediately releases the drawn string and fires the arrow.
- **Early Release (< 200 ticks / 10 seconds)**:
  - Fires **`WindArrowEntity`** (`ironspell_more:wind_arrow`).
  - Speed follows charge progress using the smoothstep formula above: 0.8 at 0 ticks, 1.6125 at 50 ticks, 3.4 at 100 ticks, 5.1875 at 150 ticks, and approximately 5.9996 at 199 ticks.
  - Projectile rendering uses the source arrow sprite on four crossed quads with partial-tick motion interpolation, keeping the texture aligned smoothly with the real flight direction instead of stretching it over cuboids.
  - A thin pale-wind dust trail streams behind the arrow with 1-4 swept-segment samples per tick.
  - Homing: Guided toward the locked target with a limited turning rate (clamped to 0.08 rad/tick). If the target dashes, changes direction rapidly, or dodges at close range, the arrow may overshoot and miss.
  - Obstacle collision: Cannot penetrate solid blocks or walls. Destroyed immediately upon colliding with any solid block.
  - Target hit: Deals normal Nature spell damage (12 + 2/lvl) and delivers slight wind knockback.
- **Full Charge — “ศรวายุ” (>= 200 ticks / 10 seconds)**:
  - At 200 ticks, wind collapses inward from three rings into the arrow, an arrow-centered flash appears, and the full charge chime plays.
  - The in-hand arrow model turns into a blazing flame arrow (`textures/entity/fire_arrow.png`).
  - The player can hold the full charge indefinitely until ready to release.
  - When released, fires **`GaleArrowEntity`** (`ironspell_more:gale_arrow`).
  - Speed: 6.0 blocks/tick.
  - Projectile rendering uses the Iron's Spells fire-arrow sprite on four crossed emissive quads, scaled 25% larger than the early-release arrow and aligned using partial-tick-interpolated motion.
  - Visuals: A large three-strand white/pale wind vortex and two intertwined `SMALL_FLAME` strands form a violent spiraling firestorm along the swept flight segment. The wake tapers from about 0.60 blocks at the rear to 0.30 blocks near the arrowhead; tangential motion (`0.07`) plus rearward drift (`0.04`) makes the particles twist and peel backward as the arrow advances. Three orange/crimson sparks remain embedded across the spiral each tick.
  - Flight particle budget: 4-8 swept axial samples per tick (`ceil(distance * 1.1)`, clamped), each emitting 3 wind particles and 2 flame particles, plus exactly 3 colored sparks. A speed-6 arrow normally uses 7 samples (38 particles/tick), with a fixed maximum of 43 particles/tick.
  - Wall penetration: Phasing mode. Passes through all blocks, terrain, and walls without obstruction.
  - Homing: 100% accurate persistent tracking. Continuously recalculates vector directly to the target's center each tick until impact or target removal.
  - Target hit: Deals heavy Nature spell damage (28 + 4/lvl), causes wind impact knockback, ignites the target for 4 seconds (Fire DoT), and inflicts Slowness I for 5 seconds.

## Projectile entities

### 1. Wind Arrow Entity (`WindArrowEntity`)
- Registry ID: `ironspell_more:wind_arrow`
- Size: 0.3 x 0.3
- Client tracking range: 64 blocks, update interval 1 tick.
- Server-authoritative continuous swept raycasting (blocks first, then entities up to block hit).
- Limited homing rotates the movement direction while preserving the charge-derived speed magnitude; turning does not reset the arrow to the former fixed 3.2 speed.
- Flight and shared impact visuals use pale wind dust; no smoke-puff trail is emitted.
- Max lifetime: 300 ticks.

### 2. Gale Arrow Entity (`GaleArrowEntity`)
- Registry ID: `ironspell_more:gale_arrow`
- Size: 0.4 x 0.4
- Client tracking range: 64 blocks, update interval 1 tick.
- Phasing: Bypasses block collision queries completely.
- Continuous entity swept raycasting along movement vector to prevent high-speed tunneling.
- The projectile renderer uses crossed sprite quads and interpolated motion orientation; no baked cuboid model layer is used.
- Flight VFX uses 4-8 swept samples across three tapered dust helix arms and two full small-flame arms, plus three orange/crimson sparks. The radius narrows from approximately 0.60 to 0.30 blocks and particle velocity combines a 0.07 tangential spiral with 0.04 rearward drift. Impact substitutes white wind dust for smoke while retaining flame, lava, and explosion particles.
- Max lifetime: 300 ticks.

## Server and client responsibilities

- **Server**:
  - Owns the lock-on targeting state in `GalePiercerCastingEvents`.
  - Enforces line-of-sight checks for lock initiation.
  - Spawns the arrow-centered charging helix, phased inward convergence lanes, and the full-charge inward collapse in `onServerCastTick`.
  - Calculates the clamped smoothstep projectile speed from held charge, then manages projectile spawning, homing vector math, swept collision, damage calculations, and status effect application.
  - Saves target UUID in projectile NBT.
- **Client**:
  - Tracks key holding/releasing in `GalePiercerClientEvents` and notifies server on release.
  - Renders bow draw animation during cast.
  - Renders the in-hand Fire Arrow model transitioning from white-gray to blazing flame in `GalePiercerChargeLayer`.
  - Shows locked target outline synced from server.
  - Renders `WindArrowRenderer` and `GaleArrowRenderer`.
  - Spawns the thin `WindArrowEntity` pale-dust trail and the `GaleArrowEntity` two-strand dust vortex with a twisted small-flame trail and orange-red sparks along its swept flight segment.
