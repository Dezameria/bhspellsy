# Shackle of Fear

## ข้อมูลหลัก

| รายการ | ค่าปัจจุบัน |
| --- | --- |
| ตัวละคร | อ็อตโต (Otto) |
| Registry ID | `ironspell_more:shackle_of_fear` |
| Class | `spells/gold/ShackleofFearSpell.java` |
| School | `bhspells:gold`; fallback เป็น Ender เมื่อหา Gold school ไม่พบ |
| Rarity | Rare |
| Max level | 1 |
| Cast type | Long |
| Cast time | 10 ticks (0.5 วินาที) |
| Cooldown | 0 วินาที |
| Mana | base 40, เพิ่ม 8 ต่อเลเวล |
| Spell power | base 6, เพิ่ม 1 ต่อเลเวล |
| Chain health | `15 × entityPowerMultiplier` |
| Chain duration | 400 ticks (20 วินาที) |
| Lash radius | 5 blocks |
| Language keys | `spell.ironspell_more.shackle_of_fear` และ `.guide` |

## Projectile และการสร้างโซ่

- ร่าย projectile `ironspell_more:arcane_shackle` จากระดับตาของผู้ร่ายด้วยความเร็ว 1.2 และ gravity 0.06
- เมื่อโดน LivingEntity หรือ parent ของ multipart entity จะสร้างโซ่ 3 เส้นล้อมเป้าหมาย
- เมื่อโดน block จะค้นหา LivingEntity ในรัศมี 5 blocks เรียงจากใกล้ไปไกล และสร้างโซ่ให้สูงสุด 3 เป้าหมาย
- แต่ละโซ่ใช้ entity `ironspell_more:gold_chain`, มี restraint strength 0.015 และใส่ Slowness VI ตลอด 400 ticks

## วงจรชีวิตของโซ่

- Gold chain เป็น multipart entity 8 ส่วน ส่วนละ 0.5 blocks และสามารถถูกโจมตีได้
- โซ่ดึงเป้าหมายกลับเข้าหาจุดยึดทุก server tick
- โซ่แตกเมื่อเป้าหมายหาย, ระยะจากจุดยึดเกิน 12 blocks, อายุเกิน lifetime, health หมด หรือถูก anti-magic
- Damage จากฝ่ายเดียวกับเจ้าของโซ่ไม่ลด health ของโซ่ และมี invulnerability 10 ticks หลังถูกโจมตี
- ข้อมูล owner, victim, health, lifetime, age และ restraint strength ถูกบันทึกลง NBT

## VFX, renderer และเสียง

- Projectile ใช้ ember trail และ impact particles; Gold chain ใช้ wisp/wax particles
- Renderer ที่เกี่ยวข้องคือ `ArcaneShackleRenderer` และ `GoldChainRenderer`
- ใช้เสียง chain step/break และ trident throw
- เริ่มร่ายด้วย one-handed horizontal swing; finish animation เป็น pass-through

## Dependency และการตรวจสอบ

- Gold school อ้างถึง `bhspells`; ต้องตรวจทั้งสภาพแวดล้อมที่มีและไม่มี mod นี้
- ทดสอบ projectile ชน entity, multipart entity และ block
- ทดสอบการทำลายโซ่, anti-magic, save/load และการซิงก์ multipart entity ใน multiplayer

