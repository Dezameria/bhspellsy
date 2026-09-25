# Per-spell documentation

This directory is the required source of detailed design and implementation notes for spells in IronSpell More. The index currently covers all **21 registered spells across 6 magic schools**.

## File organization

- Keep exactly one primary Markdown document per spell.
- Place the document inside its corresponding magic school directory (for example, `fire/`, `lightning/`, `nature/`, `aqua/`, `gold/`, or `ground/`).
- Name the file after the spell registry path using lowercase snake case: `<school>/<spell_id>.md`.
- Example: `ironspell_more:wings_of_tempest` is documented in `nature/wings_of_tempest.md`.
- Do not put detailed specifications for multiple spells in the same file.

## Spell index

### 🔥 Fire

- [Blazing Chakra](fire/blazing_chakra.md) — จาง เจียอี้ (Zhang Jiayi)
- [Spin Strike](fire/spin_strike.md)
- [Pure White Flame Burst](fire/pure_white_flame_burst.md) — ฮุ่นตุ้น ฮ่าวเหยียน (Hundun Haoyan)
- [Gale Drive](fire/gale_drive.md) — อู่ฉ่าย กวนหลง (Wucai Guanlong)
- [Resonant Knell](fire/resonant_knell.md) — โม่ ซินซิน (Mo Xinxin)
- [Crimson Thornbind](fire/crimson_thornbind.md) — ซานฉา กุยจง (Shancha Guizhong)

### ⚡ Lightning

- [Lightning Strike](lightning/lightning_strike.md)
- [Thunder Step](lightning/thunder_step.md)

### 🌿 Nature

- [Wings of Tempest](nature/wings_of_tempest.md) — จี่ จื่อเกอ (Ji Zige)
- [Venomous Blossomfall](nature/venomous_blossomfall.md) — หูเหยียน เฟิงเซียว (Huyan Fengxiao)
- [Gale Piercer](nature/gale_piercer.md) — ลันลัน (Lanlan)
- [Rapturous Bloom](nature/rapturous_bloom.md) — ฮวา เหมยเซียง (Hua Meixiang)

### 🌊 Aqua

- [Crimson Rain Bathes Moon](aqua/crimson_rain_bathes_moon.md) — หาน หลิงเหวิน (Han Lingwen)
- [Glacial Veil](aqua/glacial_veil.md) — หยิง ซีหยาง (Ying Xiyang)
- [Glacial Firmament](aqua/glacial_firmament.md) — ปิงเยว่ (Bingyue)
- [Toxic Salvation](aqua/toxic_salvation.md) — ซงหลิน (Song Lin)

### 🪙 Gold

- [Shackle of Fear](gold/shackle_of_fear.md) — อ็อตโต (Otto)
- [Hymn of Purification](gold/hymn_of_purification.md) — เจียง หลิงหยวน (Jiang Lingyuan)
- [Gilded Hare](gold/gilded_hare.md) — เว่ยลู่เยี่ยน (Wei Luyan)

### ⛰️ Ground

- [Tigershade Terrabreak](ground/tigershade_terrabreak.md) — มู่ หลิงเยว่ (Mu Lingyue)
- [Jade Aura](ground/jade_aura.md) — มู่หรง เฟิงอี้ (Murong Fengyi)

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
