# Wind Arrow (ศรวายุ / ศรวายุเพลิง)

## ข้อมูลหลัก

| รายการ | ค่าปัจจุบัน |
| --- | --- |
| Registry ID | `ironspell_more:wind_arrow` |
| `getSpellResource()` | `ironspell_more:wind_arrow` |
| Class | `spells/nature/WindArrowSpell.java` |
| School | Nature |
| Rarity | Uncommon |
| Max level | 5 |
| Cast type | Long |
| Max charge time | 200 ticks (10.0 วินาที) สำหรับ Full Charge (ศรวายุเพลิง) |
| Native hold time | 72,000 ticks (สามารถกดค้างรอปล่อยได้ตามต้องการ) |
| Cooldown | 12.0 วินาที |
| Mana | Base 35, เพิ่ม 10 ต่อเลเวล |
| Spell power | Base 10, เพิ่ม 3 ต่อเลเวล |
| Targeting | Crosshair LOS targeting สูงสุด 48 บล็อก (หากไม่มีเป้าหมายจะยิงตรงตามทิศทางที่หันหน้า) |
| Spawned Entity | `ironspell_more:wind_arrow` (`entity/spells/wind_arrow/WindArrowEntity.java`) |
| Renderer | `entity/spells/wind_arrow/WindArrowRenderer.java` |
| Language keys | `spell.ironspell_more.wind_arrow`, `spell.ironspell_more.wind_arrow.guide`, `entity.ironspell_more.wind_arrow`, `ui.ironspell_more.wind_arrow_full_charge`, `ui.ironspell_more.wind_arrow_normal_damage`, `ui.ironspell_more.wind_arrow_full_damage`, `ui.ironspell_more.wind_arrow_charge_time` |

---

## รูปแบบการทำงานและโหมดการยิง (Firing Modes)

ผู้ใช้หล่อหลอมพลังธาตุลมเข้าสู่คันธนู ก่อนยกธนูขึ้นเพื่อทำการล็อกเป้าหมายที่อยู่ในระยะสายตา (Line of Sight) ผ่านเป้าเล็ง Crosshair โดยสามารถกดปล่อยเพื่อยิงได้ทุกเมื่อ:

### 1. การยิงปกติ / ปล่อยก่อนชาร์จเต็ม (Normal Wind Arrow)
- **เงื่อนไข**: กดแตะยิงทันที หรือปล่อยมือออกก่อนครบ 200 ticks (10 วินาที)
- **คุณสมบัติลูกศร**: ลูกศรสายลมสีเขียวครามโปร่งแสง เคลื่อนที่เร็วขึ้นด้วยแรงลม (Speed 2.2 บล็อก/tick) มีสายลมบางๆ พัดตามหลัง
- **ระบบนำวิถี (Homing)**: ติดตามเป้าหมายทันทีที่ยิงด้วยความเร็วเลี้ยวตามธรรมชาติ (Turn rate 0.13) แต่หากเป้าหมายเคลื่อนที่เร็ว หลบหลีก หรือกระโดดฉากหลบ ลูกศรจะเลี้ยวตามไม่ทันและพุ่งเลยเป้าหมายไป (Overshoot) เมื่อลูกศรเลยเป้าหมาย (`motion.dot(toTarget) <= 0`) ระบบนำวิถีจะหยุดลงทันที ทำให้ลูกศรพุ่งตรงไปปักลงพื้นหรือชนผนังด้านหลัง
- **ความเสียหาย**: Base 8.0 + 2.0 ต่อเลเวล (คูณด้วย Nature Spell Power)
- **แรงผลัก (Knockback)**: แรงผลักเบาตามทิศทางลูกศร (Impulse 0.45)

### 2. ชาร์จเต็ม 10 วินาที (Gale Fire Arrow / ศรวายุเพลิง)
- **เงื่อนไข**: กดชาร์จจนครบ 200 ticks (10.0 วินาที) จะมีเสียงกระดิ่งเลเวลและข้อความแจ้งเตือนที่ Action Bar สามารถกดค้างเล็งต่อได้โดยไม่ถูกบังคับปล่อย
- **คุณสมบัติลูกศร**: เปลี่ยนเป็น "ศรวายุเพลิง" เปล่งแสงเพลิงสีส้ม-แดงและทองสว่างจ้า พุ่งทะยานด้วยความเร็วสูงมาก (Speed 3.8 บล็อก/tick) พร้อมสายลมหมุนวน (Spiraling Vortex) และสะเก็ดประกายไฟสีส้มแดงแทรกตามกระแสลม
- **ระบบนำวิถี (Homing)**: เข้าเป้า 100% แน่นอน (Guaranteed Hit) ด้วยระบบคำนวณทิศทางล่วงหน้าตามความเร็วเป้าหมาย (Predictive Lead Aiming), อัตราการเลี้ยวเฉียบคมสูง (Turn rate 0.85), และระบบล็อกพุ่งตรงเข้าจุดศูนย์กลางเป้าหมายในระยะประชิด (Distance <= 4 บล็อก) พร้อม Hit Detection Inflation 1.25 บล็อก ไม่สามารถหลบหลีกได้ในที่โล่ง (หากเป้าหมายหลบหลังสิ่งกีดขวาง/กำแพง ลูกศรจะชนระเบิดที่สิ่งกีดขวาง)
- **ความเสียหาย**: Base 22.0 + 5.0 ต่อเลเวล (คูณด้วย Nature Spell Power)
- **แรงผลัก (Knockback)**: แรงกระแทกจากลมรุนแรง (Impulse 1.25) ทำให้เป้าหมายกระเด็นถอยหลังอย่างชัดเจน
- **สถานะผิดปกติเพิ่มเติม (Debuffs)**:
  - ติดไฟ (Fire): แผดเผาต่อเนื่องเป็นเวลา 4 วินาที (80 ticks)
  - Slowness I: สโลว์เป้าหมายเป็นเวลา 4 วินาที (80 ticks)

---

## ระบบเล็งเป้าหมายและไฮไลต์ (Targeting & Highlight Rules)

- **Iron's Spells Target Highlight**: ขณะเล็งง้างธนู ตัวเกมจะแสดงกรอบไฮไลต์เป้าหมาย (Target Reticle/Highlight Layer) ของ Iron's Spells บนตัวเป้าหมายที่ล็อกได้อย่างชัดเจน ทั้งฝั่งไคลเอนต์ (`WindArrowClientEvents`) และเซิร์ฟเวอร์ (`SyncTargetingDataPacket`)
- **Line of Sight (LOS)**: ระบบจะตรวจจับเป้าหมายสิ่งมีชีวิตในระยะ 48 บล็อกผ่าน `Utils.raycastForEntity` โดยต้องมองเห็นตัวเป้าหมายโดยตรง
- **ไม่สามารถล็อกผ่านกำแพงได้**: หากเป้าหมายหลบหลังกำแพง บล็อกทึบ หรือหลุดจากระยะสายตา กรอบไฮไลต์และตัวล็อกจะหลุดทันที
- **Untargeted Fallback**: หากไม่ได้เล็งเป้าหมายใดๆ หรือไม่มีเป้าหมายในสายตา ลูกศรจะถูกยิงออกไปตรงๆ ตามแนวสายตาของผู้ร่าย (Look angle)
- **Friendly Fire**: มีการตรวจสอบ `DamageSources.isFriendlyFireBetween` เพื่อไม่ให้ทำความเสียหายหรือผลักเพื่อนร่วมทีมและสัตว์เลี้ยง

---

## เสียงและเอฟเฟกต์ (Audio & Visuals)

- **Cast Start**: เสียงชาร์จคันธนู `SoundRegistry.FIRE_ARROW_CHARGE` พร้อมแอนิเมชันง้างธนู `SpellAnimations.BOW_CHARGE_ANIMATION`
- **Full Charge Ready**: เสียง `SoundEvents.PLAYER_LEVELUP` (Pitch 2.0) พร้อมแสดงข้อความ Action Bar "Gale Fire Arrow: Ready! Release to fire"
- **Cast Finish / Release**:
  - โหมดปกติ: เสียงยิงธนู `SoundEvents.ARROW_SHOOT`
  - โหมดชาร์จเต็ม: เสียงเพลิงคำราม `SoundEvents.FIRECHARGE_USE` ผสมกับเสียงสายลมฉวัดเฉวียน `SoundEvents.PHANTOM_SWOOP`
- **Particles**:
  - **โหมดปกติ (Normal Mode)**: ละอองลม `ParticleTypes.POOF` และ `ParticleTypes.CLOUD` สีขาวครามเป็นสายตามหลังลูกศร
  - **โหมดชาร์จเต็ม (Full Charge Mode)**: วงเกลียวลมหมุนวนคู่ (Double Spiral Vortex) รอบแกนการบิน ปลดปล่อยประกายไฟ `ParticleTypes.SMALL_FLAME`, `ParticleTypes.FLAME`, และ `ParticleTypes.CLOUD` ตลอดเส้นทาง
  - **Impact**:
    - โหมดปกติ: ละอองลมกระจายตัว `ParticleTypes.POOF`
    - โหมดชาร์จเต็ม: ระเบิดเปลวไฟและลาวา `ParticleTypes.FLAME`, `ParticleTypes.LAVA`, `ParticleTypes.EXPLOSION`

---

## ความรับผิดชอบฝั่งเซิร์ฟเวอร์และไคลเอนต์ (Server & Client Responsibilities)

- **Server-Side**:
  - คำนวณเวลาการชาร์จ authoritative charge time และการปล่อยลูกศรผ่าน `onServerCastComplete` / `onCast`
  - ตรวจสอบ LOS และคำนวณการเลี้ยวติดตามเป้าหมาย (authoritative homing & movement)
  - ตรวจสอบความเสียหาย แรงผลัก การติดไฟ และ Slowness
  - รักษาสถานะการง้างธนูค้างไว้ไม่ให้หมดเวลาผ่าน `LivingEntityUseItemEvent.Tick`
- **Client-Side**:
  - เรนเดอร์ตัวลูกศร 3 มิติ (`WindArrowRenderer`) พร้อมการหมุนตามแนวการเคลื่อนที่ (Roll & Pitch/Yaw orientation)
  - เรนเดอร์สีแบบ Translucent สำหรับลูกศรลมปกติ และ Emissive Glowing สีส้มแดงสำหรับศรวายุเพลิง
  - สปอว์นอนุภาค Trail Particles และ Impact Particles

---

## สถานะความสอดคล้อง (Synchronization Status)

- [x] Registry ID `ironspell_more:wind_arrow` ตรงกับ Spell, Entity, Translations, และ Documentation
- [x] สายเวท Nature (`SchoolRegistry.NATURE_RESOURCE`)
- [x] รองรับการปล่อยยิงได้ทุกเมื่อ (Quick tap / Early release = Normal Wind Arrow, >= 200 ticks = Gale Fire Arrow)
- [x] รองรับการค้างสถานะชาร์จเต็มไว้ได้จนกว่าจะปล่อยมือ
- [x] ล็อกเป้าหมายเฉพาะที่อยู่ในระยะสายตา (LOS) และมี Untargeted Fallback เมื่อไม่มีเป้าหมาย
- [x] โค้ดคอมไพล์ผ่านสมบูรณ์ (`compileJava`)
