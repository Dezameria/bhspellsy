# Crimson Thornbind

## Identity and Configuration

- **Registry ID**: `ironspell_more:crimson_thornbind`
- **School**: Fire (`SchoolRegistry.FIRE_RESOURCE`)
- **Rarity**: Rare (`SpellRarity.RARE`)
- **Maximum Level**: 1 (Every scroll in-game exists as 1 tier at Level 1; level scaling is available via `/cast <player> <spell> <level>` command and server configuration `ironspell_more-server.toml`)
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

### 1. Incremental Dynamically Steered 3D Vine Surge (`CrimsonRootEntity` PATH mode)

- **Cast Origin Visual Sheet (`textures/entity/crimson_root.png`)**: Upon cast, a flat planar sheet formed by the `crimson_root.png` texture fans out radially in all directions (360 degrees, 16 interleaved root petals) perpendicular to the launch vector at the caster's hand origin (`start`). It rapidly unfolds over 4 ticks (0.2s) and lingers in place with a subtle magical breathing pulse throughout the vine's travel and hazard linger duration, clearly marking the exact origin of the spell. Ambient crimson spores and red dust float around it (`spawnOriginBurst`), while generic root fog and block particles are omitted.
- Segment 1 spawns immediately near the caster's active hand oriented along the caster's initial 3D look angle (pitch and yaw).
- Segment 1 acts as the server-authoritative propagation coordinator, advancing one segment every 3 ticks (`STEP_INTERVAL = 3`) up to a maximum of 12 segments.
- On each step:
  - The coordinator preserves an explicit tail and head for the latest segment. The current segment is always resolved from `currentSegmentTail` to `currentTip` before another segment is created.
  - The next segment's tail is spawned at the previous segment's exact head (`nextTail = currentTip`), so changing aim never recalculates or displaces an already established joint.
  - The coordinator samples the caster's **current** 3D look vector (aiming up, down, or flat with no horizontal lock).
  - From that fixed joint, the next head extends forward by 1.35 blocks along the updated 3D direction and derives both its yaw and pitch from that specific step's vector.
  - Previously placed segments remain stationary and preserve their original orientations (for example, segment 1 stays pointed straight while segment 2 curves towards the new crosshair direction, whether turning horizontally or tilting vertically).
- Collision and target intersection occur step-by-step:
  - A block raycast clips the path ahead of entity testing. If a solid block is hit, propagation halts.
  - Candidate targets intersecting the current step are tested (`CrimsonThornbindSpell.isValidTarget`).
  - If a valid target is struck, propagation halts immediately and an upright `BIND` root is spawned at the target.
- Path segments have gravity and physics explicitly disabled (`noGravity = true`, `noPhysics = true`, and no-op `travel()`), maintaining mid-air elevation and orientation along their 3D trajectory.
- **Lingering Hazard Snare Trail (`PATH_LINGER_TICKS = 60`)**:
  - When propagation halts (due to target hit, block collision, max 12 segments reached, or caster death), the vine path does not vanish immediately.
  - The entire vine trail remains frozen in place in mid-air for 3.0 seconds (60 ticks).
  - During this lingering duration, every segment actively tests its inflated bounding box (`checkHazardSnare()`, inflated by 0.65m horizontally and 0.5m vertically) every 2 ticks.
  - If any valid enemy entity walks into, jumps through, or touches any segment of the lingering vine trail, an upright `BIND` root is immediately spawned on them, dealing direct spell damage and binding them with Wither + Slowness for 10 seconds.
- **Sequential Dissolve Wave (`DISSOLVE_INTERVAL_TICKS = 2`)**:
  - After the 3.0-second lingering period ends, the segments dissolve sequentially from origin to tip rather than disappearing all at once.
  - Segment 1 / Origin sheet dissolves first at `baseDissolveTime`, followed by segment 2 at `+2 ticks`, segment 3 at `+4 ticks`, cascading all the way to the tip.
  - When each segment dissolves (`removeRoot()`), it emits a burst of pure red particles (`CRIMSON_SPORE` and bright red dust `Vector3f(0.88F, 0.06F, 0.14F)`) accompanied by `SoundEvents.SWEET_BERRY_BUSH_BREAK` audio until the entire vine has dissolved.
- Path root visuals use enlarged base scale (`DEFAULT_PATH_SCALE = 1.85F`) and increased renderer dimensions (`scale * 0.65F, scale * 0.95F, scale * 0.65F`) for noticeably thicker, bolder vines.
- In `CrimsonRootRenderer`, PATH mode bypasses GeckoLib's interpolated LivingEntity body yaw and explicitly applies the segment trajectory rotation using synced `DATA_YAW` and `DATA_PITCH`: `Axis.YP.rotationDegrees(180.0F - animatable.getPathYaw())`, followed by `Axis.XP.rotationDegrees(-animatable.getPathPitch() - 90.0F)`. The non-uniform path scale is applied in model-local space without off-axis translation offsets, keeping the vine's visual forward axis aligned with its server-side 3D propagation vector at all yaw/pitch angles (aiming upwards, downwards, or diagonally).

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

`PATH` entities persist through their 3.0s lingering hazard period (`PATH_LINGER_TICKS = 60`) before sequentially dissolving from origin to tip (`DISSOLVE_INTERVAL_TICKS = 2`).

## Server and Client Responsibilities

- The server manages step-by-step propagation in full 3D, samples live caster aim (pitch and yaw), performs per-step block and entity collision, spawns BIND roots, executes hazard snare checks during linger, and schedules sequential dissolve waves to all path segments.
- Synced entity data exposes mode, warmup, scale, yaw (`DATA_YAW`), pitch (`DATA_PITCH`), and removal time to tracking clients.
- Clients render each segment with the appropriate scale and 3D orientation (yaw and pitch tilt), playing the held 18-bone emergence animation smoothly, and rendering ambient path spores.

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
- Manual runtime checks must cover miss, solid obstruction, cast origin burst, lingering vine hazard snare for walking entities, sequential dissolve wave from origin to tip, upright bind presentation, and the full 200-tick bind/rend lifecycle.
