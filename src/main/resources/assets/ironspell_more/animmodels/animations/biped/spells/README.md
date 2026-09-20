# Epic Fight Animation Assets Directory

This folder contains humanoid biped animation keyframe JSON files for `ironspell_more`.

## Structure Guidelines
1. **Animation Files (`.json`):**
   - Place animation JSON files in this directory or categorized subdirectories (e.g., `spells/`, `meen_lance/`).
   - Referenced in Java code as `"biped/spells/<name>"` or `"biped/<category>/<name>"`.
   - Uses `Armatures.BIPED` humanoid skeleton.

2. **Trail Effect Metadata (`data/` subdirectory):**
   - **MUST** be placed in a subdirectory strictly named `data/` at the same folder level as the animation JSON.
   - Example: For animation `spells/blazing_chakra.json`, the trail metadata must be at `spells/data/blazing_chakra.json`.
   - Points to trail textures located at `assets/ironspell_more/textures/trail/<texture>.png`.
