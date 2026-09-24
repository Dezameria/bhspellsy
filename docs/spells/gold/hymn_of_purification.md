# Hymn of Purification (ทำนองชำระล้าง)

## ข้อมูลหลัก

| รายการ | ค่าปัจจุบัน |
| --- | --- |
| Registry ID | `ironspell_more:hymn_of_purification` |
| Class | `spells/gold/HymnofPurificationSpell.java` |
| School | `bhspells:gold`; fallback เป็น Ender เมื่อหา Gold school ไม่พบ |
| Theme | พลังธาตุหลัก: ทองคำ, พลังธาตุ: เสียง (ควบคุมทองคำให้ก่อรูปเป็นสายบาง ๆ นำพลังผ่านเสียงฉิน) |
| Rarity | Rare |
| Max level | 1 |
| Cast type | Instant (กดคลิกครั้งเดียว ไม่ต้องกดค้าง) |
| Cast time | 0 ticks (ร่ายทันทีแล้วเข้าสู่สถานะบรรเลงกู่ฉิน 20 วินาที) |
| Cooldown | 60 วินาที |
| Mana | 80 mana |
| Spell power | base 1, เพิ่ม 1 ต่อเลเวล |
| Status effect | `ironspell_more:hymn_of_purification` (20 วินาที / 400 ticks) |
| Radius | 20 blocks (ทรงกลมรอบตัวผู้ร่าย) |
| Healing pulse | 1.0 HP × Spell Power ทุก 20 ticks (วินาทีที่ 1–19 รวม 19 ครั้ง) |
| Cleanse effect | ล้าง harmful debuffs ทั้งหมดทันทีเมื่อบรรเลงครบ 20 วินาที (tick 400) |
| Interruption penalty | ดีบัฟเวียนหัว Nausea (Confusion) 40 ticks (2 วินาที) แก่ทุกคนในวง 20 blocks |
| Language keys | `spell.ironspell_more.hymn_of_purification`, `.guide`, และ `effect.ironspell_more.hymn_of_purification` |

## กลไกการทำงานและผลลัพธ์

- ผู้ร่ายเข้าสู่สมาธินั่งบรรเลงกู่ฉินเป็นเวลา 20 วินาที (400 ticks) โดยต้องอยู่กับที่
- **วินาทีที่ 1–19 (ticks 20–380)**: ปล่อยคลื่นเสียงทำนองฟื้นฟูพลังชีวิตเล็กน้อย (1.0 HP × Spell Power) ทุก ๆ 1 วินาที (20 ticks) แก่ผู้ร่ายและพันธมิตรในรัศมี 20 บล็อกรอบตัว
- **วินาทีที่ 20 (tick 400)**: ทำนองช่วงสุดท้ายบรรเลงสมบูรณ์ ปลดปล่อยคลื่นพลังประกายทองคำชำระล้างสถานะผิดปกติ (ลบเฉพาะ Harmful Effects ทั้งหมด โดยคงสถานะบัฟที่เป็นประโยชน์ไว้) แก่ผู้ร่ายและพันธมิตรทุกคนในรัศมี 20 บล็อก

## เงื่อนไขการขัดจังหวะและบทลงโทษ

- หากผู้ร่ายถูกขัดจังหวะก่อนบรรเลงครบ 20 วินาที สกิลจะยุติลงทันที:
  - **การขัดจังหวะเกิดจาก**:
    1. ผู้ร่ายได้รับความเสียหาย (Damage > 0)
    2. ผู้ร่ายเคลื่อนที่ (Displacement เกิน 0.2 บล็อก)
    3. ผู้ร่ายหันหน้าจอ / ขยับมุมมอง (Yaw/Pitch เกิน 3 องศา)
    4. มี Entity อื่นเดินเข้ามาชนกล่องฮิตบ็อกซ์ของผู้ร่าย
    5. ผู้ร่ายปล่อยปุ่มร่ายก่อนเวลา หรือมานาหมดระหว่างร่าย
  - **ผลของบทลงโทษ**:
    1. สกิลถูกยกเลิกทันที (Cancel Cast)
    2. เสียงเพลงทำนองชำระล้างหยุดเล่นทันที
    3. เล่นเสียงโน้ตดีดพลาด (Discordant Bass Note / Item Break Snap)
    4. การฟื้นฟูหยุดลงทันที และ**ไม่เกิดผลล้างดีบัฟ**
    5. ผู้ร่ายและพันธมิตรทุกคนในรัศมี 20 บล็อกได้รับดีบัฟเวียนหัว (Nausea / Confusion) เป็นเวลา 2 วินาที (40 ticks) อันเนื่องมาจากลมปราณไม่คงที่

## Audio, VFX และ Animation

- **เสียงเพลงหลัก**: `ironspell_more:hymnofpurification` รันเล่นต่อเนื่องตลอด 20 วินาที จัดการระดับ client instance ผ่าน `HymnofPurificationClientEvents` และตัดเสียงทันทีที่หลุดจากการร่าย
- **เสียงโน้ตพลาด**: `SoundEvents.NOTE_BLOCK_BASS` (pitch 0.5), `NOTE_BLOCK_DIDGERIDOO`, และ `ITEM_BREAK`
- **เสียงเมื่อบรรเลงสำเร็จ**: `SoundEvents.PLAYER_LEVELUP`, `AMETHYST_BLOCK_CHIME`, และ `BEACON_ACTIVATE`
- **อนุภาค (Particles)**:
  - ระหว่างบรรเลง: โน้ตดนตรี (`NOTE`), ประกายทองคำ (`WAX_OFF`), วงแหวนคลื่นเสียงบนพื้น (`GLOW`), และหัวใจฟื้นฟู (`HEART`)
  - เมื่อชำระล้างสำเร็จ: แสงวาบระเบิดประกายทองคำ (`FLASH`, `END_ROD`) รอบตัวพันธมิตรที่ได้รับการชำระล้าง
- **แอนิเมชัน**: `SpellAnimations.ANIMATION_CONTINUOUS_CAST_ONE_HANDED`

## สถาปัตยกรรม Server / Client

- **Server**:
  - `HymnofPurificationSpell`: ร่ายแบบ Instant ใส่เอฟเฟกต์ `HymnofPurificationEffect` นาน 400 ticks (20 วินาที) มีเมธอด `isAlly(LivingEntity caster, LivingEntity target)` คัดกรองเป้าหมายพันธมิตร (ผู้ร่าย, สัตว์เลี้ยง, และผู้เล่นที่มีทีมเดียวกันหรือปิด Friendly Fire ผ่าน `DamageSources.isFriendlyFireBetween`) เพื่อป้องกันไม่ให้ฮีลหรือล้างดีบัฟให้ศัตรูในเซิร์ฟเวอร์ Multiplayer
  - `HymnofPurificationEffect`: ควบคุมลูปทุก server tick, ตรวจสอบการเคลื่อนที่, มุมกล้อง, การชนของ entity, ส่ง healing pulse, ล้างดีบัฟ harmful เมื่อครบ 20 วิ, แจกจ่ายบทลงโทษ Nausea เมื่อถูกขัดจังหวะ และมี inner class `Events` ดักจับ `LivingDamageEvent` เพื่อสั่งขัดจังหวะทันทีเมื่อโดนดาเมจ
- **Client**:
  - `HymnofPurificationClientEvents`: ติดตามสถานะผ่าน `player.hasEffect(MobEffectsRegistry.HYMN_OF_PURIFICATION.get())` เล่นเสียง `hymnofpurification` แนบกับตัวผู้เล่น และตัดเสียงทันทีเมื่อเอฟเฟกต์สิ้นสุดลง

## การตรวจสอบ (Verification)

- ตรวจสอบการคอมไพล์โค้ด Java ด้วย `./gradlew.bat compileJava`
- ตรวจสอบความถูกต้องของทรัพยากรเสียงใน `sounds.json` และข้อความภาษาใน `en_us.json`
- ตรวจสอบ fallback ของ Magic School ในกรณีที่ไม่มี `bhspells`
