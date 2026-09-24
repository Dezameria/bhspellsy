# Gale Drive

## ข้อมูลหลัก

| รายการ | ค่าปัจจุบัน |
| --- | --- |
| Registry ID | `ironspell_more:gale_drive` |
| `getSpellResource()` | `ironspell_more:gale_drive` |
| Class | `spells/fire/GaleDriveSpell.java` |
| School | Fire |
| Rarity | Rare |
| Max level | 1 (ทุก scroll มี 1 เลเวล; scale ต่อได้ด้วยคำสั่ง /cast และปรับแต่งได้ผ่าน ironspell_more-server.toml) |
| Cast type | Instant |
| Cast time | 0 ticks |
| Cooldown | 20 วินาที |
| Mana | base 40, เพิ่ม 5 ต่อเลเวล |
| Spell power | base 10, เพิ่ม 2 ต่อเลเวล |
| Target type | Linear Dash & Single Target Vortex |
| Dash Distance | ประมาณ 15 blocks |
| Dash Duration | 12 ticks (0.6 วินาที) |
| Vortex Radius | 10 blocks (Suction AoE) |
| Vortex Duration | 140 ticks (7 วินาที) |
| Spawned Entity | `ironspell_more:gale_drive_vortex` (`entity/spells/gale_drive/GaleDriveVortexEntity.java`) |
| Applied Mob Effect | `ironspell_more:gale_drive_dash` (`GaleDriveDashEffect.java`) |
| Language keys | `spell.ironspell_more.gale_drive` และ `.guide` |

## พฤติกรรมการทำงาน

### 1. เมื่อเริ่มร่าย (On Cast)
- มอบ Buff ให้แก่ผู้ร่ายทันที:
  - **Speed II** (140 ticks / 7 วินาที, amplifier 1)
  - **Strength I** (140 ticks / 7 วินาที, amplifier 0)
- ผู้ร่ายจะเริ่มท่าพุ่งทะลวง Spin Attack (Riptide Animation / `startAutoSpinAttack`) เป็นระยะทางประมาณ 15 บล็อก
- ได้รับสถานะ `ironspell_more:gale_drive_dash` ระยะเวลา 12 ticks เพื่อขับเคลื่อนแรงพุ่งทะลุสิ่งกีดขวางและตรวจจับเป้าหมาย
- **Linear Dash Trajectory Lock**: ล็อกทิศทางการพุ่งตรงดิ่งและมุมกล้อง (Yaw / Pitch) ของผู้ร่ายไว้ ไม่สามารถหันหน้าหรือเปลี่ยนทิศทางในระหว่างการพุ่งได้

### 2. เมื่อสิ้นสุดการพุ่ง (Dash End)
- เมื่อการพุ่งสิ้นสุดลง (ครบระยะ 12 ticks หรือชนสิ่งกีดขวางในแนวนอน):
  - ผู้ร่ายจะได้รับ Debuff **Slowness I** (100 ticks / 5 วินาที, amplifier 0)

### 3. การชนเป้าหมาย (Hit Target)
- ในระหว่างการพุ่ง สถานะ dash จะตรวจหาเป้าหมาย LivingEntity ฝ่ายตรงข้ามในเส้นทางพุ่ง (ไม่โดนพวกเดียวกัน / Friendly Fire Filter)
- ล็อกเป้าหมายเพียง **1 ตัวแรก** ที่พุ่งผ่าน (Single Target Capture)
- สร้างความเสียหายจากการพุ่งชนทันทีตามค่า **Spell Power** ของผู้ร่าย (`DamageSources.applyDamage`)
- เสกเอนทิตีพายุหมุน `ironspell_more:gale_drive_vortex` ขึ้นที่ตำแหน่งเท้าของเป้าหมายทันที

### 4. กลไกของพายุหมุน (Whirlwind / Vortex)
- **เป้าหมายหลัก (Captured Target)**:
  - ถูกแรงลมหมุนยกตัวขึ้นและตรึงให้อยู่กลางอากาศสูงจากพื้น 6 บล็อก เป็นเวลา **7 วินาที** (140 ticks)
    - สำหรับ Entity ทั่วไป (Mob): ควบคุมตำแหน่งด้วย `teleportTo` พร้อมรีเซ็ตโมชัน
    - สำหรับผู้เล่น (`ServerPlayer`): ควบคุมการลอยตัวด้วยแรงส่ง Motion (`setDeltaMovement` สเกล 0.35 เข้าหาจุดตรึง) พร้อมส่งแพ็กเก็ตอัปเดตโมชัน (`hurtMarked = true`) เพื่อป้องกันอาการกระตุก (Rubberbanding) และป้องกันการถูกระบบ Vanilla / Anti-Cheat เตะหลุดเซิร์ฟเวอร์
  - ในระหว่างที่ลอยค้างอยู่ จะได้รับความเสียหาย 2 หน่วย ทุกๆ 1 วินาที (20 ticks) รวมทั้งหมด 7 ครั้ง (รวม 14 หน่วย)
- **สิ่งมีชีวิตรอบข้าง (Surrounding Entities)**:
  - ดูดศัตรู/Entity ทั้งหมดในรัศมี **10 บล็อก** เข้าสู่ใจกลางพายุตลอดระยะเวลา 7 วินาที
- **เมื่อครบกำหนดเวลา 7 วินาที (Vortex Expiration)**:
  - พายุจะสลายตัวและปลดปล่อยเป้าหมายหลักให้ร่วงลงสู่พื้น
  - เป้าหมายหลักจะได้รับ **Fall Damage** ตามความสูงจากการตกกระแทกพื้น
  - เมื่อเป้าหมายตกกระทบถึงพื้น (`onGround`) จะได้รับ Debuff:
    - **Nausea II** (100 ticks / 5 วินาที, amplifier 1)
    - **Slowness I** (100 ticks / 5 วินาที, amplifier 0)
    - **Weakness I** (100 ticks / 5 วินาที, amplifier 0)

## เสียงและเอฟเฟกต์ (Audio & Visuals)

- **Dash Sound**: `SoundEvents.TRIDENT_RIPTIDE_3` และ `SoundEvents.WIND_BURST` (หรือ `SoundEvents.ELYTRA_FLYING`)
- **Dash Spin Animation & Texture**:
  - ใช้ `SpinAttackType.GALE_SPIN` เชื่อมโยงกับ Texture เฉพาะ: `ironspell_more:textures/entity/gale_riptide.png`
- **Dash Particles**: อนุภาค `ParticleTypes.SWEEP_ATTACK`, `ParticleTypes.CLOUD`, และ `ParticleTypes.ELECTRIC_SPARK`
- **Vortex Model & Visuals**:
  - ใช้โมเดล 3D จากไฟล์ของม็อด: `ironspell_more:geo/gale_vortex.geo.json`
  - Texture หลักของพายุ: `ironspell_more:textures/entity/gale_vortex.png`
  - Emissive Glow Layer: `ironspell_more:textures/entity/gale_vortex_layer.png` ผ่าน `GaleDriveVortexEmissiveLayer`
  - แอนิเมชันการหมุนวน: `traveloptics:animations/aqua_vortex.animation.json`
  - เสริมด้วยอนุภาค `StormParticle` (Cataclysm โทนสีเงิน-ฟ้าคราม `0.70f, 0.82f, 0.90f` เกิดถี่ทุก 2 ticks) และ `ParticleTypes.CLOUD` หมุนวนเกลียวดูดเข้าสู่ศูนย์กลางพายุ
  - แสดงอนุภาคตัดฟัน `wom:sharpcut_slash` บนตัวเป้าหมายทุกครั้งที่ได้รับดาเมจจากพายุ (ทุก 1 วินาที)
- **Vortex Sound**: เล่นเสียงวนซ้ำ `traveloptics:aqua_vortex_active` และเสียงลมกระโชก
- **Impact Sound**: เสียงทุบกระแทกพื้นเมื่อเป้าหมายตกถึงพื้น

## สถานะความสอดคล้อง (Synchronization Status)

- [x] จัดทำเอกสาร `docs/spells/fire/gale_drive.md`
- [x] อัปเดตรายการสารบัญใน `docs/spells/README.md`
- [x] ลงทะเบียน `GALE_DRIVE_SPELL` ใน `SpellRegistry`
- [x] ลงทะเบียน `GALE_DRIVE_VORTEX` ใน `EntityRegistry`
- [x] ลงทะเบียน `GALE_DRIVE_DASH` ใน `MobEffectsRegistry`
- [x] เชื่อมโยงโมเดลและ Client Renderer สำหรับ `GaleDriveVortexEntity`
- [x] เพิ่ม Language Keys ใน `en_us.json`
