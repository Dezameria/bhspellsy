# ⚡ IronSpell More

An addon mod for **[Iron's Spells 'n Spellbooks](https://github.com/iron431/Irons-Spells-n-Spellbooks)** for Minecraft **1.20.1 (Forge)**.

Adds **21 spells across 6 magic schools**, custom entities and animations, Effekseer integration, custom projectile physics, Epic Fight compatibility, and cinematic combat mechanics to expand the magical arsenal.

---

## 🔮 Spell Catalog

The current registry contains 21 spells. See [the complete spell overview](docs/all_spells.md) and [per-spell technical specifications](docs/spells/README.md) for balance values, lifecycle details, assets, and verification notes.

| School | Spell | Core mechanic | Specification |
| :--- | :--- | :--- | :--- |
| Fire | **Blazing Chakra** | Epic Fight-synchronized ground slam and concentric flame shockwaves | [Details](docs/spells/fire/blazing_chakra.md) |
| Fire | **Spin Strike** | Forward fiery spin dash that damages and knocks targets aside | [Details](docs/spells/fire/spin_strike.md) |
| Fire | **Pure White Flame Burst** | Melee grab, white-flame impact, and forward corridor blast | [Details](docs/spells/fire/pure_white_flame_burst.md) |
| Fire | **Gale Drive** | Piercing dash that creates a target-lifting vortex on collision | [Details](docs/spells/fire/gale_drive.md) |
| Fire | **Resonant Knell** | Six-cast sequence alternating a mobile dome with 15/20/30-block shockwaves | [Details](docs/spells/fire/resonant_knell.md) |
| Fire | **Crimson Thornbind** | Steering thorn vine lash and vertical root bind with rend damage | [Details](docs/spells/fire/crimson_thornbind.md) |
| Lightning | **Lightning Strike** | Lightning lunge/teleport toward a raycast target | [Details](docs/spells/lightning/lightning_strike.md) |
| Lightning | **Thunder Step** | Forward teleport that damages enemies along both path lines | [Details](docs/spells/lightning/thunder_step.md) |
| Nature | **Wings of Tempest** | Caster-following storm that orbits and debuffs nearby enemies | [Details](docs/spells/nature/wings_of_tempest.md) |
| Nature | **Venomous Blossomfall** | Three-stage charged azure venom projectile fired at the crosshair | [Details](docs/spells/nature/venomous_blossomfall.md) |
| Nature | **Gale Piercer** | Swirling wind-charge arrow evolving into wall-piercing flame arrow | [Details](docs/spells/nature/gale_piercer.md) |
| Nature | **Rapturous Bloom** | Target-anchored toxic flower that blooms, applies debuffs, and bursts for damage | [Details](docs/spells/nature/rapturous_bloom.md) |
| Aqua | **Crimson Rain Bathes Moon** | Channeled crimson storm and timed spear barrage | [Details](docs/spells/aqua/crimson_rain_bathes_moon.md) |
| Aqua | **Glacial Veil** | Frostwave and eight-directional ice-spike eruption | [Details](docs/spells/aqua/glacial_veil.md) |
| Aqua | **Glacial Firmament** | Fifteen-block ice domain with concentric spike and tomb eruptions | [Details](docs/spells/aqua/glacial_firmament.md) |
| Aqua | **Toxic Salvation** | Evaporates poison into a toxic mist, curing/healing caster & poisoning foes | [Details](docs/spells/aqua/toxic_salvation.md) |
| Gold | **Shackle of Fear** | Projectile-created physical chains that restrain and pull targets | [Details](docs/spells/gold/shackle_of_fear.md) |
| Gold | **Hymn of Purification** | 20-second continuous golden melody cleansing debuffs and regenerating health | [Details](docs/spells/gold/hymn_of_purification.md) |
| Gold | **Gilded Hare** | Gilded hare mark, phase dash, golden ribbon aura, and combat enhancements | [Details](docs/spells/gold/gilded_hare.md) |
| Ground | **Tigershade Terrabreak** | Mark-and-hunt stance followed by a heavy ground slam with violet dome and golden claws | [Details](docs/spells/ground/tigershade_terrabreak.md) |
| Ground | **Jade Aura** | Protective jade barrier aura granting armor, knockback resist, and jade burst | [Details](docs/spells/ground/jade_aura.md) |

---

## ✨ Key Features

- **Effekseer & AAA Particles Support**: Integration with AAA Particles for cinematic 3D visual effects (e.g. `pure_white_flame`).
- **Custom 3D GeckoLib Entities & Renderers**: Custom animated models for projectiles, chains, needles, and vortexes (`CrimsonSpear`, `GaleDriveVortex`, `AzureVenomNeedle`, `GoldChain`).
- **Predictive Aiming & Target Tracking**: Advanced projectile physics with predictive lead aiming, crosshair line-of-sight tracking, and custom turn rates.
- **Physical Suction & Momentum Physics**: Fluid suction, smooth dash collisions, and vortex tethering mechanics without jitter or rubber-banding.
- **Procedural Spell Rendering**: Rapturous Bloom uses a server-authoritative three-phase lifecycle with a custom layered flower renderer, expanding ground sigils, and directional petal-burst particles.
- **State-Synchronized Recast VFX**: Resonant Knell synchronizes a three-stage mobile dome with side-safe lifecycle cleanup and spam-resistant 24-tick blast transitions, while client rendering provides a rising open motion, overlapping golden energy ribbons flowing upward into the crown, inward-gathering ground rings, and outward shockwaves matching the 15/20/30-block gameplay radii.
- **Feature-Based Epic Fight Compatibility**: Modular architecture separating reusable engine components from isolated skill packages (`common/` vs `skills/<skill_name>/`).
- **Frame-Accurate Animation Synchronization**: Direct frame-event integration ensuring attack colliders, camera shakes, particle waves, and server damage/fire happen in perfect unison.
- **Detailed Documentation**: Every spell has dedicated balance and implementation documentation inside `docs/spells/<school>/`.

---

## ⚔️ Epic Fight & Combat Integration

Full modular compatibility designed under **Codex Clean Architecture** standards:
- **Feature-Based Architecture**:
  - `compat/epicfight/common/`: Shared runtime infrastructure (`animation/`, `collider/`, `particle/` for afterimages, fracture, and ground shockwaves).
  - `compat/epicfight/skills/<skill>/`: Isolated skill domain containing its own animations, builders, colliders, and VFX (e.g. `blazing_chakra`).
- **Blazing Chakra Combat Integration**:
  - **Frame 59 Impact Synchronization (~0.983s)**: Perfect 1-frame alignment of melee strike collider (`BlazingChakraColliders.IMPACT`), camera shake, ground split fracture, explosion sound, client flame ripples, and server damage/fire.
  - **Authentic Combat Properties**: 100% Armor Negation, Knockdown Stun, max strikes modifier, `HIT_BLADE` strike particle, and attack speed cap.
  - **Proximity Damage Falloff & 12m Fire Shockwave**: Deals 100% heavy damage ($\le 3$m), scaling down to 25% at 12m, with unconditional fire ignition for all targets within range.
  - **Secondary Expanding Shockwave (1.85s)**: 4-ring expanding spiral flame and smoke wave.
  - **Zero-Dependency Fallback**: Gracefully falls back to standard Iron's Spells overhead swing and 20-tick shockwave when Epic Fight is not present.
- **In-Game Testing Commands**:
  - `/ism play_animation blazing_chakra` — Triggers the complete synchronized animation, VFX, audio, and shockwave chain.
- **Comprehensive Documentation**: Complete guide for architecture, animations, and VFX in [docs/compat_architecture.md](docs/compat_architecture.md).

---

## 🛠️ Requirements & Dependencies

- **Minecraft**: `1.20.1`
- **Forge**: `47.4.20` or newer
- **[Iron's Spells 'n Spellbooks](https://curseforge.com/minecraft/mc-mods/irons-spells-n-spellbooks)**: `1.20.1-3.16.1+`
- **[GeckoLib](https://curseforge.com/minecraft/mc-mods/geckolib)**: `4.8.3+`
- **[Player Animator](https://curseforge.com/minecraft/mc-mods/playeranimator)**: `1.0.2+`
- **[Caelus API](https://curseforge.com/minecraft/mc-mods/caelus)**: `3.1.0+`
- **[AAA Particles](https://modrinth.com/mod/aaa-particles)**: `1.20.1+` (Effekseer Loader)
- **[Curios API](https://curseforge.com/minecraft/mc-mods/curios)**: `5.14.1+`

### 🔌 Optional Compatibilities
- **[Epic Fight Mod](https://curseforge.com/minecraft/mc-mods/epic-fight-mod)**: `1.20.1-20.14.17+`
- **[Epic Fight - Avalon](https://curseforge.com/minecraft/mc-mods/epic-fight-avalon)**: `1.20.1+`
- **BHSpells**: `1.20.1-1.3.0+` (Gold/Ground school integration with runtime fallbacks)
- **[Moonlight Lib](https://curseforge.com/minecraft/mc-mods/selene)**: `1.20-2.16.19+`
- **[MmmMmmMmmMmm (Target Dummy)](https://curseforge.com/minecraft/mc-mods/mmmmmmmmmmmm)**: `1.20-2.0.12+`

---

## 🚀 Building & Development

### 1. Clone the repository
```bash
git clone https://github.com/Dezameria/bhspellsy.git
cd bhspellsy
```

### 2. Setup Workspace & Run Client
```bash
# Windows
.\gradlew runClient

# Linux / macOS
./gradlew runClient
```

### 3. Build JAR
```bash
# Windows
.\gradlew build

# Linux / macOS
./gradlew build
```
The compiled mod JAR will be located in `build/libs/`.

---

## 📄 License
This project is distributed as **All Rights Reserved**, as declared by the mod metadata. See [Iron's Spells 'n Spellbooks](https://github.com/iron431/Irons-Spells-n-Spellbooks) for the upstream mod and API licensing.
