# Tigershade Terrabreak

## ข้อมูลหลัก

| รายการ | ค่า |
| --- | --- |
| Registry ID | `ironspell_more:tigershade_terrabreak` |
| Class | `spells/ground/TigershadeTerrabreakSpell.java` |
| School | `BHSchoolRegistry.GROUND_RESOURCE` (`bhspells:ground`); fallback เป็น Evocation เมื่อไม่มี BHSpells |
| Rarity | Epic |
| Max level | 1 |
| Cast type / time | Instant / 0 ticks |
| Mana cost | 40 เฉพาะการกดมาร์คครั้งแรก; การกดทุบเป็น recast |
| Acquisition range | 20 บล็อกและต้องเป็นศัตรูที่อยู่ในแนวสายตา |
| Mark / recast window | 1,200 ticks (60 วินาที) |
| Slam range | ไม่เกิน 5 บล็อก (`distanceToSqr <= 25`) |
| Execute threshold | ไม่มี (สามารถพุ่งทุบได้ที่ทุกระดับ HP ของเป้าหมาย ไม่ต้องรอต่ำกว่า 10%) |
| Slam damage | 20 base + 1.0 × Spell Power (ดีฟอลต์ 30 damage ที่ spell power 10) |
| Cooldown | 600 ticks (30 วินาที) หลังใช้จังหวะพุ่งทุบ |
| Heal | 10 HP หลังยืนยันว่าเป้าหมายตายจากการทุบเท่านั้น |

## ลำดับการทำงาน

### จังหวะที่ 1 — Mark และ Tigershade Stance

เมื่อกดใช้โดยยังไม่มี hunt ที่ทำงานอยู่ server จะหา `LivingEntity` ศัตรูในระยะ 20 บล็อกตาม crosshair หรือกรวยสายตาแคบและต้องมี line of sight

- เป้าหมายได้รับ `TigershadeMarkEffect` 60 วินาที
- ผู้ร่ายได้รับ `TigershadeStanceEffect` 60 วินาที
- Stance ให้ค่าทัดเทียม Speed II (`+40% MOVEMENT_SPEED`) และ Strength I (`+3 ATTACK_DAMAGE`)
- Stance ปล่อยละอองพลังสีเหลืองส้มและเปลวไฟเป็นระยะ
- ระบบสร้าง Iron's Spellbooks recast จำนวนหนึ่งครั้ง อายุ 60 วินาที จึงยังไม่เริ่ม cooldown หลังจังหวะ Mark
- หนึ่งเป้าหมายมีเจ้าของ Mark ได้หนึ่งคนในเวลาเดียวกัน ผู้ร่ายอื่นไม่สามารถขโมยหรือใช้ Mark นั้นทุบได้

ความสัมพันธ์ถูกบันทึกทั้งสองฝั่งด้วย UUID และ dimension:

- ผู้ร่ายเก็บ UUID/dimension ของเป้าหมาย
- เป้าหมายเก็บ UUID/dimension ของเจ้าของ Mark
- ทุกครั้งที่ resolve หรือ slam ต้องตรวจว่าข้อมูลทั้งสองฝั่งตรงกัน รวมทั้ง effect และมิติที่อยู่จริง
- ไม่มี fallback ที่สามารถเลือก Mark ของผู้เล่นคนอื่นได้

### การมองเห็นเป้าหมายเฉพาะผู้ร่าย

Server ส่ง `SyncTigershadeTargetPacket` ไปยังผู้ร่ายเพียงคนเดียวหลัง Mark สำเร็จ Client ของผู้ร่ายจึงเปิด Glowing ให้ entity ที่มี UUID ตรงกับ packet เท่านั้น

- ผู้เล่นอื่นไม่เห็น outline จาก Tigershade
- ไม่สแกนหรือแสดงทุก entity ที่มี `TigershadeMarkEffect`
- ถ้า entity มี Glowing อยู่ก่อนแล้ว Tigershade จะไม่ถือว่าตัวเองเป็นเจ้าของ flag และจะไม่ปิด flag นั้นตอน cleanup
- Glowing แสดงได้เฉพาะตอน entity ถูกโหลด/track อยู่ใน client ตามข้อจำกัดของ Minecraft

### จังหวะที่ 2 — Takedown Slam

เมื่อกดอีกครั้งระหว่าง recast window server ตรวจเงื่อนไขทั้งหมดดังนี้:

1. ผู้ร่ายยังมี Stance และ recast ของเวทนี้
2. เป้าหมายยังมีชีวิตและอยู่มิติเดียวกัน
3. Mark เป็นของผู้ร่ายคนนี้จริงจาก UUID/dimension ทั้งสองฝั่ง
4. ระยะไม่เกิน 5 บล็อก
*(ไม่ต้องเช็ค HP ต่ำกว่า 10% สามารถทุบใส่เป้าหมายที่มี HP เท่าใดก็ได้)*

เมื่อผ่านเงื่อนไข:
- ผู้ร่ายได้รับแรงพุ่งเข้าหาเป้าหมาย (`scale 1.25D`)
- เป้าหมายถูกกระแทกลงพื้น (`y = -1.2D`)
- สร้างความเสียหาย spell damage: `20.0 + getSpellPower(spellLevel, caster)` (ที่ spell power พื้นฐาน 10 ทำ 30 damage)
- แสดงเอฟเฟกต์การทุบทุกครั้งที่ทำสำเร็จ: เสียงระเบิด/ทั่งตก, custom shockwave, เศษดิน Coarse Dirt, dust สีส้มทอง, flame, lava และ explosion emitter
- หากการโจมตีทำให้เป้าหมายตาย (`!target.isAlive()`) ผู้ร่ายจะได้รับการฟื้นฟูเลือด 10 HP
- ล้าง Stance, Mark, UUID link และ client glow
- จังหวะทุบเข้าสู่ cooldown 30 วินาทีจาก config ของ Iron's Spellbooks
- หากเป้าหมายไม่ตายจากการทุบ หรือ damage event ถูกยกเลิกโดย mod อื่น จะไม่ได้รับการฮีล แต่การทุบยังคงล้าง Mark และเข้าสู่ cooldown ตามปกติ

## Lifecycle และ cleanup

- Effect ฝั่งใดฝั่งหนึ่งหมดอายุหรือถูกลบ จะล้าง effect/link อีกฝั่งและยกเลิก recast
- เมื่อผู้ร่ายหรือเป้าหมายตาย จะล้าง hunt ทันที
- เมื่อผู้ร่ายออกจากเกมหรือเปลี่ยนมิติ จะล้าง hunt เพื่อไม่ทิ้ง Mark ที่ไม่มีเจ้าของ
- เมื่อ login จะ sync target ที่ยัง valid กลับไปยัง client; target ที่ invalid จะไม่ถูกแสดง
- ข้อมูลเก่าหรือ UUID/dimension ที่อ่านไม่ได้ถือเป็น link ที่ไม่ valid และไม่สามารถ slam ได้

## Server / Client responsibility

- Server เป็นผู้เลือกเป้าหมาย ตรวจ ownership/range, จัดการ effect/recast/cooldown, ทำ damage, heal, movement และ spawn particle/sound
- Client รับเฉพาะ UUID ของ target สำหรับแสดง Glowing ในมุมมองของผู้ร่าย
- `persistentData` ใช้เป็น authoritative link ฝั่ง server และไม่ถูกสมมติว่าจะ sync ไป client

## Dependencies และข้อจำกัด

- School resource คือ `bhspells:ground` ซึ่งมีค่าเดียวกับ `BHSchoolRegistry.GROUND_RESOURCE`
- โค้ดไม่ hard-load class ของ BHSpells เพราะ dependency ถูกประกาศเป็น optional; หาก school ไม่มีใน registry จะ fallback เป็น `irons_spellbooks:evocation`
- Spell damage เคารพการยกเลิก `SpellDamageEvent`/damage event จาก mod อื่น

## การตรวจสอบ

- `./gradlew compileJava` หรือ `./gradlew build`
- ทดสอบในเกมอย่างน้อย:
  - Mark และรับ buff Stance (Speed II + Strength I) ระยะ 20 บล็อก
  - Selective glow แสดงเฉพาะผู้ร่าย
  - กด recast ภายในระยะ 5 บล็อกเพื่อพุ่งทุบเป้าหมายที่ HP ใดก็ได้ (ทั้งสูงกว่าและต่ำกว่า 10%)
  - ปฏิเสธการ recast หากเป้าหมายอยู่ไกลกว่า 5 บล็อก โดยไม่สูญเสียสถานะ Mark/recast
  - สร้างความเสียหายพื้นฐาน 30 damage และสเกลตาม Spell Power
  - ฟื้นฟู 10 HP เฉพาะเมื่อเป้าหมายตายจากการทุบ
  - เอฟเฟกต์การทุบแสดงผลทุกครั้งที่มีการทุบ
  - Cleanup ทำงานถูกต้องเมื่อหมดเวลา 60 วินาที, ผู้ร่ายหรือเป้าหมายตาย, disconnect หรือข้ามมิติ
  - Cooldown 30 วินาทีเริ่มนับหลังการทุบเสร็จสิ้น
