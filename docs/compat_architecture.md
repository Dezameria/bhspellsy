# สถาปัตยกรรมการทำงานร่วมกันของ ironspell_more / Compatibility Layer Architecture (Epic Fight, Avalon, AAA Particles)

> **Platform:** Minecraft 1.20.1, Minecraft Forge, Java 17  
> **Module:** `ironspell_more`  
> **Document scope:** โครงสร้างสถาปัตยกรรมแบบโมดูลาร์ (Modular Architecture) ตามมาตรฐาน Epic Fight, การลงทะเบียนแอนิเมชัน, ระบบ VFX & Particles, Colliders, Assets, การทดสอบ และแนวทางการเพิ่มท่าใหม่

---

## 1. ภาพรวมระบบ (Overview)

Compatibility layer ของ `ironspell_more` ออกแบบด้วยหลักการ **Clean Architecture (Codex Standard)** เพื่อแยกโค้ดหลักของม็อดออกจาก API ภายนอก (Epic Fight, Avalon, AAA Particles) อย่างเด็ดขาด:
* **Zero Hard Dependency:** ตัวม็อดสามารถรันได้ตามปกติแม้ไม่มี Epic Fight หรือ Avalon ติดตั้งอยู่
* **Modular Separation:** แยกโค้ดออกเป็นโดเมนย่อยตามแบบแผนของ Epic Fight: `animation`, `particle` (VFX), `collider`, และ `gameasset`
* **Facade & Safe Bridge:** สื่อสารผ่าน Facade (`EpicFightCompat`) และโหลดคลาส Epic Fight ผ่าน Bridge เฉพาะเมื่อตรวจพบม็อดในระบบแล้วเท่านั้น

---

## 2. แผนผังโครงสร้างแพ็กเกจ (Package & File Structure)

```text
io.redspace.ironspell_more.compat.epicfight/
├── EpicFightCompat.java                 # Facade กลาง ปลอดภัยต่อการเรียกจากภายนอก
├── EpicFightLoadedBridge.java           # ตัวเชื่อมต่อ Forge EventBus (โหลดเมื่อมี Epic Fight เท่านั้น)
├── EpicFightFractureHelper.java         # ตัวช่วยจัดการพิกัดพื้นแตก
│
├── collider/
│   └── IronSpellColliders.java          # ศูนย์รวม OBB/Box Colliders ที่ใช้ซ้ำได้
│
├── particle/                            # ระบบ VFX และการกระจาย Particles แบบแยกโมดูล
│   ├── ShockwaveVfx.java                # คลื่นไฟระเบิด 4 วงแหวน (10-18 บล็อก) + ลาวาตรงกลาง + Frontal Sweep
│   ├── WeaponAuraVfx.java               # วงแหวนไฟที่เท้า 5 ชั้น + ละอองไฟ/ไฟฟ้าวนรอบตัวและมือ + Afterimage
│   ├── FractureVfx.java                 # ตรวจจับพื้นแตก (Circle Slam) + หน้าจอสั่น (CameraShakeManager)
│   └── EpicFightVfx.java                # Facade รวมระบบ VFX ให้คลาสอื่นเรียกใช้ได้ง่าย
│
└── animation/                           # ระบบแอนิเมชัน แยกตามประเภทอาวุธ/เวทมนตร์
    ├── IronSpellAnimations.java         # Registry Orchestrator หลัก + แมปปิ้ง AnimationCue
    ├── MeenLanceAnimations.java         # ตัวสร้างท่าโจมตีชุดหอก Meen (Charge 3 + Event Timing)
    ├── AvalonMeenAnimationBuilder.java  # ตัวสร้างท่าแบบพิเศษเมื่อมีม็อด Avalon ติดตั้งอยู่
    └── EpicFightAnimationPlayer.java    # สั่งเล่นแอนิเมชันแบบ Server-Client Synchronized
```

---

## 3. โครงสร้างโฟลเดอร์ Assets ตามมาตรฐาน Epic Fight

ไฟล์ทรัพยากรทั้งหมดต้องจัดวางในโครงสร้างที่ Epic Fight กำหนดอย่างเคร่งครัด:

```text
assets/ironspell_more/
│
├── animmodels/
│   ├── animations/
│   │   └── biped/                       # โครงกระดูกมาตรฐานผู้เล่น (Armatures.BIPED)
│   │       └── spells/                  # หมวดหมู่ท่าเวท/สกิล
│   │           ├── meen_charge3.json    # ไฟล์คีย์เฟรมแอนิเมชัน
│   │           └── data/                # ⚠️ สำคัญมาก: ต้องชื่อ data เสมอสำหรับ Epic Fight Trail
│   │               └── meen_charge3.json# ไฟล์กำหนดเส้นแสง Trail
│   │
│   └── weapon/                          # โมเดลอาวุธ 3D (มีจุด locator_edge0, edge1)
│
└── textures/
    ├── trail/                           # ภาพ Texture เส้นแสงอาวุธ
    │   ├── fire.png                     # เส้นแสงไฟสำหรับ meen_charge3
    │   └── meenlance.png
    └── particle/                        # Texture สำหรับอนุภาคพิเศษ
```

---

## 4. รายละเอียดสเปกของท่า `MEEN_CHARGE_3` (Reconstructed from Nightfall 3.3.4)

### 4.1 ข้อมูลเชิงเทคนิค (Technical Specs)
* **Animation Path:** `ironspell_more:biped/spells/meen_charge3`
* **Armature:** `Armatures.BIPED`
* **Transition Time:** `0.1F`
* **Attack Phase Timing:**
  * Start: `59 / 60.0F` (~0.98 วินาที)
  * Contact: `70 / 60.0F` (~1.17 วินาที)
  * Recovery: `100 / 60.0F` (~1.67 วินาที)
* **Collider:** `IronSpellColliders.MEEN_LANCE_CHARGE3` (`OBBCollider(2.0D, 2.0D, 2.0D, 0.0D, 0.0D, 0.0D)`) ยึดที่ `rootJoint`
* **Damage Properties:**
  * `STUN_TYPE = StunType.KNOCKDOWN`
  * `SWING_SOUND = EpicFightSounds.WHOOSH_BIG`
  * `HIT_SOUND = EpicFightSounds.BLADE_RUSH_FINISHER`
  * `PARTICLE = EpicFightParticles.HIT_BLADE`
  * `ARMOR_NEGATION_MODIFIER = ValueModifier.setter(100.0F)` (เจาะเกราะ 100%)
  * `MAX_STRIKES_MODIFIER = ValueModifier.setter(10.0F)`
  * `PLAY_SPEED_MODIFIER = clamp(speed, 0.85F, 1.45F)`
* **Player States:** `MOVEMENT_LOCKED = true`, `INACTION = true`, `CAN_SWITCH_HAND_ITEM = false`

### 4.2 ตาราง Animation Events (9 Events ครบถ้วน)

| เวลา / เฟรม | ฝั่งที่รัน | โมดูลที่รับผิดชอบ | รายละเอียดพฤติกรรม |
| :--- | :--- | :--- | :--- |
| **0.0s** | **CLIENT** | `WeaponAuraVfx` | วงแหวนไฟ 5 ชั้นที่เท้า + ละอองไฟและประกายไฟฟ้า `ELECTRIC_SPARK` วนรอบตัวและมือขวา |
| **0.0s** | **CLIENT** | `WeaponAuraVfx` | เงาร่างสีขาวติดตา `EpicFightParticles.WHITE_AFTERIMAGE` |
| **0.0s** | **BOTH** | Game Logic | ให้บัฟกันกระตุก `DAMAGE_RESISTANCE` เลเวล 5 เป็นเวลา 6 วินาที (120 ticks) |
| **0.1s** | **SERVER** | Game Logic | ให้บัฟอาวุธ `efn:meen_lance` (หรือ fallback เป็น `DAMAGE_BOOST II`) 1,000 ticks |
| **0.2s** | **SERVER** | Sound | เล่นเสียงฟ้าร้องง้างตัว `SoundEvents.TRIDENT_THUNDER` (volume 1.5) |
| **59/60s (~0.98s)** | **SERVER / BOTH**| `FractureVfx` | พื้นแตกรัศมี 5 บล็อก (`circleSlamFracture` หรือ `simpleGroundSplit`) + สั่นหน้าจอ (`CameraShakeManager`) |
| **1.1s** | **SERVER** | Sound | เล่นเสียงฟ้าผ่ากระแทกพื้น `SoundEvents.LIGHTNING_BOLT_THUNDER` (volume 1.5) |
| **1.15s** | **SERVER** | `ShockwaveVfx` | คลื่นฟันประกายแสงด้านหน้า (`FLASH`, `SWEEP_ATTACK`) + คลื่นไฟ 4 วงแหวนระเบิดออก (10-18 บล็อก) + ลาวาปะทุตรงกลาง |

---

## 5. คู่มือการเพิ่มท่าใหม่ (How to Add New Animations)

ด้วยโครงสร้างสถาปัตยกรรมแบบโมดูลาร์ การเพิ่มท่าใหม่สามารถทำได้โดยไม่กระทบโค้ดเดิม:

### ขั้นตอนที่ 1: เพิ่ม Cue ใน `AnimationCue.java`
```java
public enum AnimationCue {
    // ... ท่าเดิม ...
    GALE_PIERCER_THRUST,
    BLOSSOMLIGHT_SLASH
}
```

### ขั้นตอนที่ 2: วางไฟล์ Resource
1. วางไฟล์แอนิเมชันที่ `assets/ironspell_more/animmodels/animations/biped/<category>/<name>.json`
2. วางไฟล์ Trail metadata ที่ `assets/ironspell_more/animmodels/animations/biped/<category>/data/<name>.json`

### ขั้นตอนที่ 3: สร้างคลาส Builder ประจำชุดอาวุธ
สร้างไฟล์ใหม่ใน `compat/epicfight/animation/` เช่น `GalePiercerAnimations.java`:
```java
public final class GalePiercerAnimations {
    public static AnimationManager.AnimationAccessor<AttackAnimation> registerThrust(AnimationManager.AnimationBuilder builder) {
        return builder.nextAccessor("biped/spells/gale_thrust", accessor ->
            new AttackAnimation(...)
                .addEvents(
                    // เรียกใช้โมดูล VFX ที่มีอยู่แล้วได้ทันที
                    AnimationEvent.InTimeEvent.create(0.0F, WeaponAuraVfx::spawnChargeRingsAndAura, AnimationEvent.Side.CLIENT)
                )
        );
    }
}
```

### ขั้นตอนที่ 4: เชื่อมต่อใน `IronSpellAnimations.java`
```java
event.newBuilder(IronSpellMore.MODID, builder -> {
    MEEN_CHARGE3 = MeenLanceAnimations.registerCharge3(builder);
    GALE_THRUST  = GalePiercerAnimations.registerThrust(builder); // <-- บรรทัดเดียวจบ
});

CUE_MAP.put(AnimationCue.GALE_PIERCER_THRUST, GALE_THRUST);
```

---

## 6. คำสั่งสำหรับทดสอบในเกม (In-Game Testing)

ผู้เล่นที่มีสิทธิ์ Operator (Permission Level 2) สามารถทดสอบท่าทางในเกมได้ทันที:

```text
# ทดสอบท่า Meen Lance Charge 3 โดยตรง
/ism test_meen_charge3

# หรือเรียกผ่าน Cue Identifier
/ironspell_more play_animation meen_charge_3
/ism play_animation meen_charge_3
```
