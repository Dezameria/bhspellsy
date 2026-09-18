# Spin Strike

## ข้อมูลหลัก

| รายการ | ค่าปัจจุบัน |
| --- | --- |
| Registry ID | `ironspell_more:spin_strike` |
| Class | `spells/fire/SpinStrikeSpell.java` |
| School | Fire |
| Rarity | Common |
| Max level | 1 |
| Cast type | Instant |
| Cast time | 0 ticks |
| Cooldown | 10 วินาที |
| Mana | base 30, เพิ่ม 5 ต่อเลเวล |
| Spell power | base 10, เพิ่ม 1 ต่อเลเวล |
| Language keys | `spell.ironspell_more.spin_strike` และ `.guide` |

## พฤติกรรม

- ผลักผู้ร่ายไปตามทิศที่มองด้วยแรง `2.5 × ((15 + spellPower) / 20)` และเพิ่มแรงยก 0.2 เมื่อเริ่มจากพื้น
- ปรับแรงในแกน Y ลงเมื่อมองขึ้น เพื่อควบคุมความสูงของการพุ่ง
- เริ่มท่า auto-spin 10 ticks และตั้งชนิด spin เป็น Fire
- ใส่ effect `ironspell_more:spin_strike` ให้ผู้ร่าย 12 ticks โดยใช้ค่า damage ที่ตัดเป็นจำนวนเต็มเป็น amplifier
- ระหว่าง effect ทำงาน จะรักษาความเร็วพุ่งประมาณ 1.4 blocks/tick, สร้างอนุภาค Flame/Electric Spark และตรวจเป้าหมายทุก tick ใน hitbox ที่ขยาย 1.2/0.6/1.2 blocks
- เป้าหมายที่ไม่ใช่ฝ่ายเดียวกันได้รับความเสียหายตาม amplifier และถูกผลักออกด้านข้าง โดยมีช่วงป้องกันการโดนซ้ำ 20 ticks
- การพุ่งหยุดเมื่อชนกำแพง และรีเซ็ต fall distance ระหว่าง effect

## Client/server และทรัพยากร

- Server คำนวณ impulse, damage และ effect แล้วส่ง `ImpulseCastData` เพื่อซิงก์การเคลื่อนที่
- Client นำ impulse ไปใช้และเริ่ม auto-spin animation
- ใช้เสียง `TRIDENT_RIPTIDE_3`
- ใช้ mob effect `ironspell_more:spin_strike`

## การตรวจสอบ

- ตรวจการซิงก์ตำแหน่งเมื่อมี latency สูง
- ตรวจว่าการชนเป้าหมายหลายตัวสร้าง damage เพียงครั้งเดียวต่อเป้าหมายต่อการร่าย
- ตรวจการหยุดเมื่อชนกำแพงและการไม่รับ fall damage

