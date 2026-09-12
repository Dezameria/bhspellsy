# ⚡ IronSpell More

An addon mod for **[Iron's Spells 'n Spellbooks](https://github.com/iron431/Irons-Spells-n-Spellbooks)** for Minecraft **1.20.1 (Forge)**.

Adds powerful new spells, custom 3D particle effects, Effekseer integration, custom entity mechanics, and spell weapons to expand the magical arsenal.

---

## 🔮 Featured Spells

### 🔥 Fire
| Spell | Description |
| :--- | :--- |
| **Pure White Flame Burst** | Charges a frontal 1-block capture zone with continuous suction flames that pulls enemies in, followed by a devastating white flame explosion using custom Effekseer VFX (`pure_white_flame.efkefc`), heavy knockback, and a sweeping 20-block rushing firestorm corridor (20m length x 7m width x 7m height). |

### ✨ Gold
| Spell | Description |
| :--- | :--- |
| **Shackle of Fear** | Launches a golden arcane shackle projectile that chains targets with physical animated 3D chains (`GoldChain`), binding their movement with severe Slowness VI. |

### ⚡ Lightning
| Spell | Description |
| :--- | :--- |
| **Lightning Strike** | Rapidly locks on and lunges toward a target with high-speed lightning impact, 3D directional shockwave rings, and screen shake. |
| **Thunder Step** | Blinks instantly forward across a path, zapping and electrocuting all enemies between the starting point and destination. |
| **Spin Strike** | Charges forward in a fiery spin attack (Riptide spin animation), smoothly piercing through all enemies and dealing fire damage along the path. |

---

## ✨ Key Features

- **Effekseer & AAA Particles Support**: Integration with AAA Particles for cinematic visual effects (e.g. `pure_white_flame`).
- **Physical Entity Chains**: Custom animated entity models for chains and projectiles (`ArcaneShackleProjectile`, `GoldChain`).
- **Custom 3D Visuals & FX**: Custom directional shockwave particles (`ShockwaveParticleCustom`) and 3D electric zap particles (`ZapParticleCustom`).
- **Smooth Piercing & Suction Physics**: Fluid suction and dash mechanics that cleanly interact with hitboxes without jitter or rubber-banding.
- **Origins & Command Integration**: Seamless casting with commands (e.g. `/cast @s ironspell_more:<spell_name> 1`).

---

## 🛠️ Requirements & Dependencies

- **Minecraft**: `1.20.1`
- **Forge**: `47.4.20` or newer
- **[Iron's Spells 'n Spellbooks](https://curseforge.com/minecraft/mc-mods/irons-spells-n-spellbooks)**: `1.20.1-3.16.1+`
- **[AAA Particles](https://modrinth.com/mod/aaa-particles)**: `1.20.1+` (Effekseer Loader)
- **[GeckoLib](https://curseforge.com/minecraft/mc-mods/geckolib)**: `4.8.3+`
- **[Player Animator](https://curseforge.com/minecraft/mc-mods/playeranimator)**: `1.0.2+`
- **[Curios API](https://curseforge.com/minecraft/mc-mods/curios)**: `5.14.1+`

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
