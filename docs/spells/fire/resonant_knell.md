# Resonant Knell

## Core specification

| Item | Current value |
| --- | --- |
| Registry ID | `ironspell_more:resonant_knell` |
| Spell class | `spells/fire/ResonantKnellSpell.java` |
| School | Fire |
| Rarity | Epic |
| Maximum level | 1 |
| Cast type | Instant |
| Cast time | 0 ticks |
| Mana | 75 |
| Recasts | 6 total casts across 3 open/blast cycles |
| Recast window | 300 ticks (15 seconds) |
| Cooldown effect | 600 ticks (30 seconds) |
| Spawned entity | `ironspell_more:resonant_knell_dome` |
| Base dome radius | 8 blocks |

## Cast sequence

1. The first cast creates an open dome centered on and following the caster.
2. The second cast immediately damages and launches enemies within 15 blocks, then starts the first visual shockwave.
3. The third cast reopens the dome at stage 2.
4. The fourth cast immediately damages and launches enemies within 20 blocks, then starts the second visual shockwave.
5. The fifth cast reopens the dome at stage 3.
6. The sixth cast immediately damages and launches enemies within 30 blocks, starts the final visual shockwave, and applies cooldown.

The first, second, and final blasts deal 6, 10, and 16 direct spell damage respectively. Targets must be alive, hostile to the caster, not the caster, and not an armor stand. The horizontal and vertical launch strengths are `1.8/1.2`, `2.5/1.5`, and `3.8/2.0` respectively. Normal fall damage may occur after the launch.

Each cast refreshes Strength II, Resistance II, and Fire Resistance I on the caster for 300 ticks. If the recast sequence expires or finishes, the cooldown effect is applied and any non-exploding dome is removed.

## Entity lifecycle and synchronization

`ResonantKnellDomeAoe` stores its state, stage, shockwave start signal, and shockwave radius in synchronized entity data.

- `STATE_OPEN`: follows the living caster and renders the open dome.
- `STATE_EXPLODING`: renders for 24 ticks. Stages 1 and 2 then become inactive; stage 3 discards the entity.
- `STATE_INACTIVE`: remains attached to the caster but renders nothing until the next open cast.
- The entity is not saved and discards itself when it no longer has a living owner.
- Gameplay damage and knockback happen immediately on the server when the blast cast is performed; the expanding shockwave is client-side visual feedback.

## Client visuals

`ResonantKnellDomeRenderer` uses full-bright additive geometry for the gold-red shell, plasma streaks, ground effects, spirit wisps, and shockwave. Taoist talismans use `textures/entity/resonant_knell/talisman.png` with an emissive translucent render type.

While the dome is open:

- Every time a stage opens, the shell, talismans, and spirit wisps begin at `0.22` scale and `1.4` blocks below the caster's feet, then rise and unfold over 12 ticks. A light ease-out overshoot prevents the opening from looking rigid, while alpha fades in over the first 6 ticks.
- The perimeter sigil expands on the ground with the opening shell but remains anchored beneath the caster.
- Four golden ground rings continuously contract from radius 7.6 to radius 0.45 beneath the caster over a 36-tick loop, visually gathering energy at the caster's feet.
- Once fully open, the perimeter sigil remains at radius 8 while 24 plasma streaks, 22 orbiting talismans, and 16 spirit wisps supplement the dome.

During each 24-tick blast:

- The dome shell expands with cubic ease-out from radius 8 to the synchronized gameplay radius of 15, 20, or 30 blocks.
- The leading ground shockwave begins at radius 0.35 beneath the caster and expands to the synchronized gameplay radius.
- Three staggered echo rings also expand from beneath the caster to the same target radius and fade as they travel outward.
- Talismans and spirit wisps scatter outward while the ground wall, blast rays, and ring alpha fade.

The renderer stops drawing beyond 64 blocks from the entity center. Additional client particles are emitted around the open dome and throughout the blast.

## Sounds and assets

- Opening: bell and amethyst chime sounds.
- Normal blasts: generic explosion and dragon fireball explosion sounds.
- Final blast: lower-pitched explosion layers plus bell resonance.
- Language keys: `spell.ironspell_more.resonant_knell`, `.guide`, and `entity.ironspell_more.resonant_knell_dome`.
- Spell icon: `textures/gui/spell_icons/resonant_knell.png`.

## Verification

- Compile the main source set after renderer changes.
- Verify that the 12-tick rise-and-unfold animation replays on stages 1, 2, and 3, including the light overshoot, while the ground effects remain anchored beneath the caster.
- In game, verify the inward rings remain centered below a moving caster while the dome is open.
- Verify all three blasts expand their ground rings to 15, 20, and 30 blocks respectively.
- Verify stage transitions, recast expiry, and cooldown behavior.
- Verify that gameplay damage remains immediate and is not delayed until the visual ring reaches a target.
