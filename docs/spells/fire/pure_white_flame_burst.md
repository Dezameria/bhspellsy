# Pure White Flame Burst

## ข้อมูลหลัก

| รายการ | ค่าปัจจุบัน |
| --- | --- |
| Registry ID | `ironspell_more:pure_white_flame_burst` |
| Class | `spells/fire/PureWhiteFlameBurstSpell.java` |
| School | Fire |
| Rarity | Rare |
| Max level | 5 |
| Cast type | Long |
| Cast time | 25 ticks (1.25 วินาที) |
| Cooldown | 15 วินาที |
| Mana | base 50, เพิ่ม 10 ต่อเลเวล |
| Spell power/damage | base 80, เพิ่ม 10 ต่อเลเวล; primary damage เท่ากับ spell power |
| Language keys | `spell.ironspell_more.pure_white_flame_burst` และ `.guide` |

## การหาเป้าหมายและช่วงชาร์จ

- สร้างวงหาเป้าหมายรัศมี 1 block ที่ตำแหน่ง 1.2 blocks ด้านหน้าผู้ร่าย
- รับเฉพาะ LivingEntity ที่มีชีวิต ไม่ใช่ spectator ไม่ใช่ผู้ร่ายหรือฝ่ายเดียวกัน และต่างระดับแนวตั้งไม่เกิน 2 blocks
- วงเป้าหมายเคลื่อนตามทิศที่ผู้ร่ายมองระหว่างชาร์จ และเลือกเป้าหมายใหม่ได้หากเป้าหมายเดิมหายหรือตาย
- ผู้ร่ายได้รับ Slowness IV แบบต่อเนื่องและความเร็วแนวราบถูกลดเหลือ 5% ระหว่างชาร์จ
- เป้าหมายที่ล็อกไว้ถูกดูดมาที่ระยะ 1.2 blocks ด้านหน้าและได้รับ Slowness III ระหว่างช่วงชาร์จ

## ผลเมื่อร่ายสำเร็จ

### เมื่อพลาดเป้าหมาย

- ผู้ร่ายถูกตรึง 100 ticks ด้วย Slowness ระดับสูงสุด, Mining Fatigue ระดับสูงสุด และ Jump amplifier 200
- ความเร็วแนวราบถูกล้าง พร้อมเล่นควันและเสียง `FIRE_EXTINGUISH`

### เมื่อโดนเป้าหมาย

- ย้ายเป้าหมายมาด้านหน้าผู้ร่ายแล้วเหวี่ยงไปตามทิศที่มองด้วยความเร็ว 1.8 และแรงยก 0.35
- Phase 1 สร้าง damage เท่ากับ spell power
- เป้าหมายหลักได้รับ White Flame Burn 100 ticks และ Slowness III 200 ticks
- หลัง 8 ticks เกิด Phase 2 เป็นพื้นที่ทรงกล่องยาว 20, กว้าง 7 และสูง 7 blocks จากจุดกระแทก
- Phase 2 สร้าง damage 80% ของ primary damage, ใส่ White Flame Burn 100 ticks และผลักเป้าหมายไปข้างหน้าด้วยความเร็ว 1.8 พร้อมแรงยก 0.45
- เป้าหมายใน Phase 2 ไม่รวมผู้ร่าย, spectator, entity ที่ตาย และฝ่ายเดียวกัน

## ผลย้อนกลับต่อผู้ร่าย

- เสียพลังชีวิต 20% ของพลังชีวิตปัจจุบัน แต่จะไม่ต่ำกว่า 1 HP
- ได้รับ Slowness III 10 วินาที, Wither I 5 วินาที และ Weakness I 30 วินาที

## Effect, VFX และ dependency

- `ironspell_more:white_flame_burn` ล้างไฟ vanilla และทำ fire damage `2 + amplifier` ทุก 20 ticks
- ใช้ particles `white_fire`, `white_fire_emitter`, `white_ember`, white sparks, smoke และ explosion
- ใช้ Effekseer effect `ironspell_more:pure_white_flame` ผ่าน AAA Particles
- ใช้ `compat/epicfight/EpicFightFractureHelper` เป็น optional integration boundary ก่อนเรียก
  `LevelUtil.circleSlamFracture` สำหรับพื้นแตกร้าวที่จุดกระแทกและคลื่น 5 จุด หากไม่มี Epic Fight
  ส่วน fracture จะเป็น no-op โดยไม่กระทบ damage หรือเอฟเฟกต์หลักของสกิล
- ใช้เสียง Blaze, explosion, dragon fireball และ wither ตามแต่ละช่วง

## การตรวจสอบ

- ทดสอบทั้งกรณีโดนและพลาดเป้าหมาย รวมถึงเป้าหมายที่ตายระหว่างชาร์จ
- ทดสอบ Phase 2 กับทิศมองขึ้น/ลง, พื้นต่างระดับ และพื้นที่ว่าง
- ตรวจ friendly fire, HP ขั้นต่ำ 1 และ duration ของ debuff ทุกชนิด
