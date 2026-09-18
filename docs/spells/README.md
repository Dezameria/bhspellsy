# Per-spell documentation

This directory is the required source of detailed design and implementation notes for spells in IronSpell More.

## File organization

- Keep exactly one primary Markdown document per spell.
- Name the file after the spell registry path using lowercase snake case: `<spell_id>.md`.
- Example: `ironspell_more:wings_of_tempest` is documented in `wings_of_tempest.md`.
- Do not put the detailed specification for multiple spells in the same file.

## Spell index

- [Spin Strike](spin_strike.md)
- [Pure White Flame Burst](pure_white_flame_burst.md)
- [Lightning Strike](lightning_strike.md)
- [Thunder Step](thunder_step.md)
- [Shackle of Fear](shackle_of_fear.md)
- [Wings of Tempest](wings_of_tempest.md)

## Required workflow

Before an agent creates or modifies a spell, it must read this file and the document for that spell. A new spell requires a new document in this directory. Any behavior or balance change requires the affected document to be updated in the same change as the code.

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
