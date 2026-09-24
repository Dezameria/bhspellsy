# Gale Piercer

## Identity and configuration

- Registry ID: `ironspell_more:gale_piercer`
- School: Nature
- Rarity: Rare
- Maximum level: 5
- Cast type: `LONG` (Hold-to-charge, release-to-shoot)
- Full charge threshold ("ศรวายุ"): 200 ticks (10.0 seconds)
- Mana cost: Base 40, +5 per level
- Cooldown: 20.0 seconds
- Cast start animation: `SpellAnimations.BOW_CHARGE_ANIMATION` (ยกคันธนูขึ้นน้าวสาย)
- In-hand charge model: 4-crossed-quad Fire Arrow model attached to the caster's right hand via `GalePiercerChargeLayer`:
  - **Initial / Charging (< 200 ticks)**: Rendered as a white-gray arrow (`ศรสีขาวเทา`) using `textures/entity/gale_piercer/white_gray_arrow.png`.
  - **Full Charge (>= 200 ticks)**: Transforms into a blazing flame arrow (`ศรเพลิง`) using `textures/entity/fire_arrow.png` with full-bright emissive rendering.
- Particle visual effects:
  - **Charging Vortex**: Swirling particles continuously orbit and converge inward into the drawn arrow in the caster's hand (white-gray wind embers and enchant runes during stage 1; flame sparks during stage 2).
  - **Full Charge Burst**: Outward radial dispersion burst of fire, white fire, and flash particles when reaching 200 ticks.
- Sounds:
  - Cast Start: Bow pull sound (`SoundEvents.ARROW_SHOOT` / `BOW_PULL`)
  - Charge Complete (10s): Level up sound and beacon chime (`SoundEvents.PLAYER_LEVELUP`, `SoundEvents.BEACON_ACTIVATE`)
  - Early Release: Arrow release with wind whoosh (`SoundEvents.ARROW_SHOOT`, `SoundEvents.CROSSBOW_SHOOT`)
  - Full Charge Release: Thunderous wind explosive blast (`SoundEvents.GENERIC_EXPLODE`, `SoundEvents.LIGHTNING_BOLT_THUNDER`, `SoundEvents.CROSSBOW_SHOOT`)
- Balance constants:
  - Max lock-on distance: 48.0 blocks
  - Normal direct damage: Base 12.0 HP (+2.0 HP per level)
  - Full charge direct damage: Base 28.0 HP (+4.0 HP per level)
  - Normal projectile speed: 3.2 blocks/tick
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
  - Speed: 3.2 blocks/tick.
  - Faint wind trail particles (`ParticleTypes.CLOUD` / wind dust) stream behind the arrow.
  - Homing: Guided toward the locked target with a limited turning rate (clamped to 0.08 rad/tick). If the target dashes, changes direction rapidly, or dodges at close range, the arrow may overshoot and miss.
  - Obstacle collision: Cannot penetrate solid blocks or walls. Destroyed immediately upon colliding with any solid block.
  - Target hit: Deals normal Nature spell damage (12 + 2/lvl) and delivers slight wind knockback.
- **Full Charge — “ศรวายุ” (>= 200 ticks / 10 seconds)**:
  - At 200 ticks, an outward particle dispersion burst erupts and the full charge chime plays.
  - The in-hand arrow model turns into a blazing flame arrow (`textures/entity/fire_arrow.png`).
  - The player can hold the full charge indefinitely until ready to release.
  - When released, fires **`GaleArrowEntity`** (`ironspell_more:gale_arrow`).
  - Speed: 6.0 blocks/tick.
  - Visuals: Violent swirling wind vortex around the arrow, wind trail, and fiery orange-red sparks dancing within the wind torrent.
  - Wall penetration: Phasing mode. Passes through all blocks, terrain, and walls without obstruction.
  - Homing: 100% accurate persistent tracking. Continuously recalculates vector directly to the target's center each tick until impact or target removal.
  - Target hit: Deals heavy Nature spell damage (28 + 4/lvl), causes wind impact knockback, ignites the target for 4 seconds (Fire DoT), and inflicts Slowness I for 5 seconds.

## Projectile entities

### 1. Wind Arrow Entity (`WindArrowEntity`)
- Registry ID: `ironspell_more:wind_arrow`
- Size: 0.3 x 0.3
- Client tracking range: 64 blocks, update interval 1 tick.
- Server-authoritative continuous swept raycasting (blocks first, then entities up to block hit).
- Max lifetime: 300 ticks.

### 2. Gale Arrow Entity (`GaleArrowEntity`)
- Registry ID: `ironspell_more:gale_arrow`
- Size: 0.4 x 0.4
- Client tracking range: 64 blocks, update interval 1 tick.
- Phasing: Bypasses block collision queries completely.
- Continuous entity swept raycasting along movement vector to prevent high-speed tunneling.
- Max lifetime: 300 ticks.

## Server and client responsibilities

- **Server**:
  - Owns the lock-on targeting state in `GalePiercerCastingEvents`.
  - Enforces line-of-sight checks for lock initiation.
  - Spawns inward-swirling particles and full-charge dispersion burst in `onServerCastTick`.
  - Manages projectile spawning, homing vector math, swept collision, damage calculations, and status effect application.
  - Saves target UUID in projectile NBT.
- **Client**:
  - Tracks key holding/releasing in `GalePiercerClientEvents` and notifies server on release.
  - Renders bow draw animation during cast.
  - Renders the in-hand Fire Arrow model transitioning from white-gray to blazing flame in `GalePiercerChargeLayer`.
  - Shows locked target outline synced from server.
  - Renders `WindArrowRenderer` and `GaleArrowRenderer`.
  - Spawns client-side particle trails and wind vortex effects.
