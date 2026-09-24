# Per-spell documentation

This directory is the required source of detailed design and implementation notes for spells in IronSpell More. The index currently covers all **20 registered spells across 6 magic schools**.

## File organization

- Keep exactly one primary Markdown document per spell.
- Place the document inside its corresponding magic school directory (for example, `fire/`, `lightning/`, `nature/`, `aqua/`, `gold/`, or `ground/`).
- Name the file after the spell registry path using lowercase snake case: `<school>/<spell_id>.md`.
- Example: `ironspell_more:wings_of_tempest` is documented in `nature/wings_of_tempest.md`.
- Do not put detailed specifications for multiple spells in the same file.

## Spell index

### 🔥 Fire

- [Blazing Chakra](fire/blazing_chakra.md)
- [Spin Strike](fire/spin_strike.md)
- [Pure White Flame Burst](fire/pure_white_flame_burst.md)
- [Gale Drive](fire/gale_drive.md)
- [Resonant Knell](fire/resonant_knell.md)
- [Crimson Thornbind](fire/crimson_thornbind.md)

### ⚡ Lightning

- [Lightning Strike](lightning/lightning_strike.md)
- [Thunder Step](lightning/thunder_step.md)

### 🌿 Nature

- [Wings of Tempest](nature/wings_of_tempest.md)
- [Venomous Blossomfall](nature/venomous_blossomfall.md)
- [Gale Piercer](nature/gale_piercer.md)
- [Rapturous Bloom](nature/rapturous_bloom.md)

### 🌊 Aqua

- [Crimson Rain Bathes Moon](aqua/crimson_rain_bathes_moon.md)
- [Glacial Veil](aqua/glacial_veil.md)
- [Glacial Firmament](aqua/glacial_firmament.md)

### 🪙 Gold

- [Shackle of Fear](gold/shackle_of_fear.md)
- [Hymn of Purification](gold/hymn_of_purification.md)
- [Gilded Hare](gold/gilded_hare.md)

### ⛰️ Ground

- [Tigershade Terrabreak](ground/tigershade_terrabreak.md)
- [Jade Aura](ground/jade_aura.md)

## Required workflow

Before creating or modifying a spell, read this file and the document for that spell. A new spell requires a new document in this directory. Any behavior or balance change requires the affected document to be updated in the same change as the code.

## Recommended document contents

Each spell document should record, where applicable:

- Display name and full registry ID
- School, rarity, and maximum level
- Cast type, cast time, animation, and sounds
- Base mana cost, per-level mana cost, and cooldown
- Targeting rules, range, radius, and area shape
- Damage formula and spell-power scaling
- Status effects, amplifiers, and durations
- Per-level scaling and fixed constants
- Spawned entities and their lifecycle
- Particles, renderers, models, textures, and language keys
- Server-side and client-side responsibilities
- Friendly-fire, anti-magic, immunity, and edge-case behavior
- External mod/API dependencies
- Verification steps and known limitations

## Consistency requirement

The document must describe the behavior that the current implementation is intended to provide. Registry IDs, values, assets, language keys, and runtime behavior must not silently diverge from the document. If implementation and documentation are intentionally changed, update both and mention the change in the task summary.
