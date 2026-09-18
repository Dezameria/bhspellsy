# Lightning Strike

## ข้อมูลหลัก

| รายการ | ค่าปัจจุบัน |
| --- | --- |
| Registry ID | `ironspell_more:lightning_strike` |
| Class | `spells/lightning/LightningStrikeSpell.java` |
| School | Lightning |
| Rarity | Common |
| Max level | 1 |
| Cast type | Instant |
| Cast time | 0 ticks |
| Cooldown | 15 วินาที |
| Mana | base 50, เพิ่ม 5 ต่อเลเวล |
| Spell power | base 8, เพิ่ม 1 ต่อเลเวล |
| Range | `12 + (spellLevel × 2)` blocks |
| Damage | `4 + (spellPower × 0.75)` |
| Language keys | `spell.ironspell_more.lightning_strike` และ `.guide` |

## พฤติกรรม

- Raycast ไปตามทิศที่ผู้ร่ายมองภายในระยะของ spell โดยตรวจทั้ง entity และ block
- เมื่อโดน entity ผู้ร่ายจะวาร์ปไปที่ตำแหน่งห่างจากเป้าหมาย 1.5 blocks ในแนวราบ
- สร้าง damage เฉพาะ entity ที่มีชีวิต, pickable, ไม่ใช่ spectator และไม่ใช่ผู้ร่าย
- เมื่อโดน block จะวาร์ปไปยังจุดกระทบ; เมื่อไม่โดนอะไรจะวาร์ปไปยังปลายระยะจากระดับสายตา
- รีเซ็ต fall distance หลังวาร์ป และหันลำตัวผู้ร่ายเข้าหาเป้าหมายเมื่อโดน entity

## VFX และเสียง

- ส่งลำแสง `zap_custom` จากตำแหน่งเริ่มต้นไปยังเป้าหมายหรือปลายทาง
- เมื่อโจมตี LivingEntity จะสร้าง shockwave สีชมพูรัศมี 3.5, electricity 50 จุด และสายฟ้าแตกแขนง 35 เส้น
- สร้าง camera shake 10 ticks ในรัศมี 7 blocks
- ใช้เสียง Lightning Cast ตอนเริ่ม/จบ, thunder และ Lightning Lance ที่จุดกระทบ
- VFX ถูกส่งจาก server ไปยังผู้เล่นทุกคนใน ServerLevel

## ข้อจำกัดปัจจุบัน

- การตรวจ `canHit` ไม่ได้เรียกตัวช่วย friendly-fire ของ Iron's Spells จึงควรทดสอบกับสมาชิกทีมและสัตว์เลี้ยง
- Language guide ปัจจุบันระบุว่าเรียกสายฟ้าลงพื้นที่ แต่ implementation จริงเป็นการโจมตีพร้อม teleport

## การตรวจสอบ

- ทดสอบปลายทางเมื่อ raycast โดน entity, block และไม่โดนสิ่งใด
- ทดสอบความปลอดภัยของตำแหน่ง teleport และการชน block
- ทดสอบเป้าหมายฝ่ายเดียวกัน, multipart entity และเป้าหมายที่ไม่ใช่ LivingEntity

