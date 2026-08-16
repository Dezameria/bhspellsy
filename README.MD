# ⚡ IronSpell More

An addon mod for **[Iron's Spells 'n Spellbooks](https://github.com/iron431/Irons-Spells-n-Spellbooks)** for Minecraft **1.20.1 (Forge)**.

Adds powerful new spells, custom 3D particle effects, custom mechanics, and spell weapons to expand the magical arsenal.

---

## 🔮 Featured Spells

| Spell | School | Description |
| :--- | :---: | :--- |
| **Spin Strike** | 🔥 Fire | Charges forward in a fiery spin attack (Riptide spin animation), smoothly piercing through all enemies and dealing fire damage along the path. |
| **Lightning Strike** | ⚡ Lightning | Rapidly locks on and lunges toward a target with high-speed lightning impact, 3D directional shockwave rings, and screen shake. |
| **Thunder Step** | ⚡ Lightning | Blinks instantly forward across a path, zapping and electrocuting all enemies between the starting point and destination. |

---

## ✨ Features

- **Custom 3D Visuals & FX**: Custom directional shockwave particles (`ShockwaveParticleCustom`) and 3D electric zap particles (`ZapParticleCustom`).
- **Smooth Piercing Physics**: Dash mechanics that cleanly pierce through enemy hitboxes without stopping or rubber-banding.
- **Origins Datapack Compatibility**: Seamless integration with commands (e.g. `/cast @s ironspell_more:<spell_name> 1`).
- **Single-Scroll Simplicity**: All spells are configured with 1 level (`maxLevel = 1`) for streamlined balance and clean item lists.

---

## 🛠️ Requirements & Dependencies

- **Minecraft**: `1.20.1`
- **Forge**: `47.1.1` or newer
- **[Iron's Spells 'n Spellbooks](https://curseforge.com/minecraft/mc-mods/irons-spells-n-spellbooks)**: `1.20.1-2.2+`
- **[GeckoLib](https://curseforge.com/minecraft/mc-mods/geckolib)**: `4.3.1+`
- **[Player Animator](https://curseforge.com/minecraft/mc-mods/playeranimator)**: `1.0.2+`
- **[Curios API](https://curseforge.com/minecraft/mc-mods/curios)**: `5.4.7+`

---

## 🚀 Building & Development

### 1. Clone the repository
```bash
git clone https://github.com/<your-username>/ironspell_more.git
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
