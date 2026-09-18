# Thunder Step

## ข้อมูลหลัก

| รายการ | ค่าปัจจุบัน |
| --- | --- |
| Registry ID | `ironspell_more:thunder_step` |
| Class | `spells/lightning/ThunderStepSpell.java` |
| School | Lightning |
| Rarity | Uncommon |
| Max level | 1 |
| Cast type | Instant |
| Cast time | 0 ticks |
| Cooldown | 8 วินาที |
| Mana | base 75, เพิ่ม 15 ต่อเลเวล |
| Spell power | base 10, เพิ่ม 2 ต่อเลเวล |
| Range | เท่ากับ spell power |
| Damage | เท่ากับ spell power |
| Language keys | `spell.ironspell_more.thunder_step` และ `.guide` |

## พฤติกรรม

- ใช้ `TeleportSpell.TeleportData` เป็นปลายทางเมื่อมีข้อมูลจากระบบ cast; หากไม่มีจะเรียก `TeleportSpell.findTeleportLocation`
- ตรวจ entity ตลอดเส้นทางจากระดับสายตาต้นทางถึงระดับสายตาปลายทาง รวมทั้งเส้นขนานที่ระดับเท้า
- hit width ของแต่ละเส้นคือ 1 block และ entity ที่ตัดผ่านเส้นได้รับ lightning spell damage
- ก่อน teleport จะลงจากพาหนะถ้าผู้ร่ายกำลังโดยสาร
- Teleport ไปยังปลายทาง, รีเซ็ต fall distance และล้าง additional cast data

## VFX และเสียง

- สร้าง Zap particles 7 คู่ระหว่างต้นทางกับปลายทางจากฝั่ง server
- ไม่มี cast-start sound
- ใช้ `ILLUSIONER_PREPARE_BLINDNESS` เป็นเสียงจบและเล่นที่ระดับเสียง 2.0

## ข้อจำกัดปัจจุบัน

- การเลือกเป้าหมายตามทางใช้ entity query โดยไม่กรอง friendly fire ในเมธอด `zapEntitiesBetween`
- `onCast` cast `Level` เป็น `ServerLevel` โดยตรงขณะสร้าง particle จึงพึ่งสมมติฐานว่าเมธอดนี้ทำงานฝั่ง server เท่านั้น

## การตรวจสอบ

- ทดสอบการเลือกปลายทางเมื่อมีและไม่มี `TeleportData`
- ทดสอบสิ่งกีดขวาง, การโดยสาร, fall distance และ entity ที่อยู่ตลอดแนวทาง
- ทดสอบสมาชิกทีมและ entity ที่ไม่ควรรับ friendly fire

