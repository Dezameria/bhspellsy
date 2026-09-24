# Crimson Thornbind

## Identity and Configuration

- **Registry ID**: `ironspell_more:crimson_thornbind`
- **School**: Fire (`SchoolRegistry.FIRE_RESOURCE`)
- **Rarity**: Rare (`SpellRarity.RARE`)
- **Maximum Level**: 5
- **Cast Type**: Instant (`CastType.INSTANT`)
- **Cast Time**: 0
- **Base Mana Cost**: 40
- **Mana Cost Per Level**: +5
- **Cooldown**: 25.0 seconds
- **Range**: 16.2 blocks (up to 12 connected path segments at 1.35-block spacing)
- **Cast Start Animation**: `SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION`
- **Cast Start Sound**: `SoundEvents.PLAYER_ATTACK_SWEEP`
- **Cast Finish Sound**: `SoundRegistry.ROOT_EMERGE`

## Combat Mechanics and Spell Flow

Crimson Thornbind launches an incrementally propagating, dynamically steerable crimson vine horizontally from the caster's hand. The travelling vine is a visual path that steers with the caster's crosshair, while a separate vertical root performs the bind when the path intersects its first valid target.

### 1. Incremental Dynamically Steered Horizontal Vine Surge (`CrimsonRootEntity` PATH mode)

- Upon cast, segment 1 spawns immediately near the caster's active hand oriented along the caster's initial look angle.
- Segment 1 acts as the server-authoritative propagation coordinator, advancing one segment every 3 ticks (`STEP_INTERVAL = 3`) up to a maximum of 12 segments.
- On each step:
  - The coordinator samples the caster's **current** horizontal look vector.
  - The next segment extends forward by 1.35 blocks along this updated look direction and derives its yaw from that specific step's vector.
  - Previously placed segments remain stationary and preserve their original orientations (for example, segment 1 stays pointed straight while segment 2 curves towards the new crosshair direction).
- Collision and target intersection occur step-by-step:
  - A block raycast clips the path ahead of entity testing. If a solid block is hit, propagation halts.
  - Candidate targets intersecting the current step are tested (`CrimsonThornbindSpell.isValidTarget`).
  - If a valid target is struck, propagation halts immediately and an upright `BIND` root is spawned at the target.
- Path segments have gravity and physics explicitly disabled (`noGravity = true`, `noPhysics = true`, and no-op `travel()`), maintaining mid-air elevation along their trajectory.
- When propagation finishes (by obstacle, target hit, max 12 segments reached, or caster death), all spawned path segments are assigned a synchronized removal time (`level.getGameTime() + PATH_HOLD_TICKS`, where `PATH_HOLD_TICKS = 6`). All horizontal segments hold their pose and vanish together in a crimson root fog burst.
- Path root visuals use enlarged base scale (`DEFAULT_PATH_SCALE = 1.85F`) and increased renderer dimensions (`scale * 0.65F, scale * 0.95F, scale * 0.65F`) for noticeably thicker, bolder vines.

### 2. Single-Target Damage and Vertical Bind (`CrimsonRootEntity` BIND mode)

- When any path step intersects a valid target, one upright `BIND` root is spawned at the target's coordinates (adjusted to ground level via `findGroundHeight`).
- Target eligibility excludes the caster, dead/removed/spectator entities, allies, and friendly-fire-protected entities. Previous path segments are also explicitly excluded.
- The bind root activates immediately upon emergence (`warmup = 0`), dealing direct fire magic damage once (`12.0` base + `2.5`/level + Spell Power scaling).
- Targets tagged `ModTags.CANT_ROOT` take the direct hit but are not mounted or debuffed.
- A rootable victim mounts the vertical root (`target.startRiding(root, true)`), is locked for **10 seconds (200 ticks)**, and receives **Wither I** plus **Slowness III** for 10 seconds.
- The bind root uses an enlarged base scale (`DEFAULT_BIND_SCALE = 1.8F`) and can enlarge further for wider passengers.
- Repaired animation: Both PATH and BIND modes utilize the unified 18-bone `emerge` animation (`RawAnimation.begin().thenPlayAndHold("emerge")`), animating all 6 vine arms across the 18 model bones (`northBase1/2`, `southBase1/2`, `westbase1/2`, and bones 2–13) rising and curling into an enclosing cage around the victim without freezing or skipping arms.

### 3. Bonus Thorn Rend Damage

- While the target is mounted on a `BIND`-mode `CrimsonRootEntity`, attacks from its caster cause the thorns to slash in tandem.
- Adds bonus thorn rend damage directly to the attack (`6.0` base + `1.5`/level + Spell Power scaling).
- Spawns crimson sweep attack particles (`ParticleTypes.SWEEP_ATTACK`, `ParticleTypes.CRIMSON_SPORE`).
- Plays thorn slicing and sweeping audio (`SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES`, `SoundEvents.PLAYER_ATTACK_SWEEP`).
- `PATH` mode is explicitly excluded from rend handling.

### 4. Release Conditions

The vertical bind root ejects its passenger and safely discards when:

- The 10-second duration expires.
- The target entity dies.
- An antimagic / dispel effect strikes the entity.
- The target is moved more than 5 blocks away (for example, by teleportation).

Horizontal `PATH` entities use their shared cast removal time instead of the 200-tick bind lifecycle.

## Server and Client Responsibilities

- The server manages step-by-step propagation, samples live caster aim, performs per-step block and entity collision, spawns BIND roots, and synchronizes removal times to all path segments.
- Synced entity data exposes mode, warmup, scale, and removal time to tracking clients.
- Clients render each segment with the appropriate scale and orientation, playing the held 18-bone emergence animation smoothly.

## Progression and Scaling

| Level | Mana Cost | Direct Damage | Thorn Rend Damage | Range | Bind Duration | Cooldown |
| :---: | :-------: | :-----------: | :---------------: | :---: | :-----------: | :------: |
| 1     | 40        | 12.0          | 6.0               | 16.2m | 10.0s         | 25.0s    |
| 2     | 45        | 14.5          | 7.5               | 16.2m | 10.0s         | 25.0s    |
| 3     | 50        | 17.0          | 9.0               | 16.2m | 10.0s         | 25.0s    |
| 4     | 55        | 19.5          | 10.5              | 16.2m | 10.0s         | 25.0s    |
| 5     | 60        | 22.0          | 12.0              | 16.2m | 10.0s         | 25.0s    |

Both direct damage and thorn rend damage scale with the caster's Fire Spell Power.

## Assets and Resources

- Model: `assets/ironspell_more/geo/crimson_root.geo.json`
- Animations: `assets/ironspell_more/animations/crimson_root_animations.json` (18-bone `emerge` animation used for both path and bind with `thenPlayAndHold`)
- Entity Texture: `assets/ironspell_more/textures/entity/crimson_root.png`
- Spell Icon: `assets/ironspell_more/textures/gui/spell_icons/crimson_thornbind.png`

## Tooltip UI Information

- **Direct Damage**: `ui.ironspell_more.crimson_thornbind_damage`
- **Thorn Rend Damage**: `ui.ironspell_more.crimson_thornbind_rend_damage`
- **Range**: `ui.irons_spellbooks.distance` (15.0m)
- **Bind Duration**: `ui.ironspell_more.crimson_thornbind_duration` (10.0s)

## Verification Notes

- Automated verification: `gradlew.bat compileJava`, `gradlew.bat build`, and `git diff --check`.
- Manual runtime checks must cover miss, solid obstruction, two collinear hostiles, friendly/invalid entities before a hostile, a target moving away before arrival, path-only non-damage, simultaneous path cleanup, upright bind presentation, and the full 200-tick bind/rend lifecycle.
