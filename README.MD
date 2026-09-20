# ⚡ IronSpell More

An addon mod for **[Iron's Spells 'n Spellbooks](https://github.com/iron431/Irons-Spells-n-Spellbooks)** for Minecraft **1.20.1 (Forge)**.

Adds powerful new spells, custom 3D entity models and animations, Effekseer integration, custom projectile physics, Epic Fight compatibility, and cinematic combat mechanics to expand the magical arsenal.

---

## 🔮 Featured Spells

For comprehensive documentation, see [docs/all_spells.md](docs/all_spells.md) (Thai Overview) and [docs/spells/README.md](docs/spells/README.md) (Technical Specifications).

### 🔥 Fire
| Spell | Rarity | Type | Description |
| :--- | :--- | :--- | :--- |
| **Pure White Flame Burst** | Legendary | Continuous | Charges a suction flame zone pulling enemies in, followed by a devastating white flame explosion (`pure_white_flame.efkefc`), heavy knockback, and a sweeping 20-block rushing firestorm corridor (20m x 7m x 7m). |
| **Blazing Chakra** | Epic | Instant | Leaps into the air with afterimages, slamming down with a 4x4x4m melee strike and an expansive 12m concentric flame shockwave with proximity damage falloff, radial knockback, and unconditional fire ignition. Synchronized with Epic Fight animations. |
| **Spin Strike** | Common | Instant | Charges forward in a fiery spin attack (Riptide animation), piercing smoothly through enemies, dealing fire damage and knocking them aside. |
| **Gale Drive** | Rare | Instant | Dashes forward at gale speed granting Speed II & Strength I, locking onto the first target and summoning a violent vortex that suspends them 6 blocks high for 7 seconds before slamming them to the ground. |

### ⚡ Lightning
| Spell | Rarity | Type | Description |
| :--- | :--- | :--- | :--- |
| **Lightning Strike** | Common | Instant | Locks on and rapidly lunges toward a target with high-speed lightning impact, 3D electric pink directional shockwave rings, and screen shake. |
| **Thunder Step** | Uncommon | Instant | Blinks instantly forward to the crosshair location, electrocuting and shocking all enemies standing between the starting point and destination. |

### 🍃 Nature
| Spell | Rarity | Type | Description |
| :--- | :--- | :--- | :--- |
| **Wings of Tempest** | Rare | Long | Summons an orbiting tempest that travels with the caster, continuously spinning and sweeping enemies in a violent vortex while inflicting Nausea II, Blight V, and Slowness III. |
| **Venomous Blossomfall** | Legendary | Long / Charge | Multi-stage charged azure venom needle shot from FPS crosshair (30m / 60m / 90m range) with continuous collision sweep, custom audio stages, and lethal damage scaling. |
| **Gale Piercer** | Common | Instant | Fires a high-velocity piercing gale needle that punctures smoothly through enemies in a line with custom aerodynamic physics and wind burst. |

### 🌊 Aqua / Ice
| Spell | Rarity | Type | Description |
| :--- | :--- | :--- | :--- |
| **Crimson Rain Bathes Moon** | Epic | Continuous | Summons an expanding crimson hurricane that repels enemies and unleashes an unrelenting barrage of homing, ricocheting Crimson Spears from the sky. |
| **Glacial Veil** | Rare | Instant | Releases a 10-block frostwave shockwave inflicting Frostbite, erupting 8-directional scaling ice spikes along the ground aligned with player facing. |

### 🪙 Gold
| Spell | Rarity | Type | Description |
| :--- | :--- | :--- | :--- |
| **Shackle of Fear** | Rare | Instant | Launches an arcane projectile creating 3 physical animated 3D golden chain anchors (`GoldChain`) that bind the target with Slowness VI and forcefully yank them back if they try to escape. |

---

## ✨ Key Features

- **Effekseer & AAA Particles Support**: Integration with AAA Particles for cinematic 3D visual effects (e.g. `pure_white_flame`).
- **Custom 3D GeckoLib Entities & Renderers**: Custom animated models for projectiles, chains, needles, and vortexes (`CrimsonSpear`, `GaleDriveVortex`, `AzureVenomNeedle`, `GoldChain`).
- **Predictive Aiming & Target Tracking**: Advanced projectile physics with predictive lead aiming, crosshair line-of-sight tracking, and custom turn rates.
- **Physical Suction & Momentum Physics**: Fluid suction, smooth dash collisions, and vortex tethering mechanics without jitter or rubber-banding.
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
- **[AAA Particles](https://modrinth.com/mod/aaa-particles)**: `1.20.1+` (Effekseer Loader)
- **[Curios API](https://curseforge.com/minecraft/mc-mods/curios)**: `5.14.1+`

### 🔌 Optional Compatibilities
- **[Epic Fight Mod](https://curseforge.com/minecraft/mc-mods/epic-fight-mod)**: `1.20.1-20.9.11+`
- **[Epic Fight - Avalon](https://curseforge.com/minecraft/mc-mods/epic-fight-avalon)**: `1.20.1+`
- **[Moonlight Lib](https://curseforge.com/minecraft/mc-mods/selene)**: `1.20-2.16.19+`
- **[MmmMmmMmmMmm (Target Dummy)](https://curseforge.com/minecraft/mc-mods/mmmmmmmmmmmm)**: `1.20-2.0.12+`

---

## 🚀 Building & Development

### 1. Clone the repository
```bash
git clone https://github.com/Deizyn/ironspell_more.git
cd ironspell_more
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
Distributed under the project repository terms. See [Iron's Spells 'n Spellbooks](https://github.com/iron431/Irons-Spells-n-Spellbooks) for core mod API licensing.
