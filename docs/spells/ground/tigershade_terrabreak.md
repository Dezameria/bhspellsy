# Tigershade Terrabreak

## ข้อมูลหลัก

| รายการ | ค่า |
| --- | --- |
| ตัวละคร | มู่ หลิงเยว่ (Mu Lingyue) |
| Registry ID | `ironspell_more:tigershade_terrabreak` |
| Class | `spells/ground/TigershadeTerrabreakSpell.java` |
| School | `BHSchoolRegistry.GROUND_RESOURCE` (`bhspells:ground`); fallback เป็น Evocation เมื่อไม่มี BHSpells |
| Rarity | Epic |
| Max level | 1 |
| Cast type / time | Instant / 0 ticks |
| Mana cost | 40 เฉพาะการกดมาร์คครั้งแรก; การกดทุบเป็น recast |
| Acquisition range | 20 บล็อกและต้องเป็นศัตรูที่อยู่ในแนวสายตา |
| Mark / recast window | 1,200 ticks (60 วินาที) |
| Slam range | ไม่เกิน 8 บล็อก (`distanceToSqr <= 64`) |
| Execute threshold | ไม่มี (สามารถพุ่งทุบได้ที่ทุกระดับ HP ของเป้าหมาย ไม่ต้องรอต่ำกว่า 10%) |
| Slam damage | ค่า config `base_damage + (level - 1) × damage_per_level` แล้วคูณ spell power multiplier (ดีฟอลต์เลเวล 1 คือ 30 damage) |
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
4. ระยะไม่เกิน 8 บล็อก
*(ไม่ต้องเช็ค HP ต่ำกว่า 10% สามารถทุบใส่เป้าหมายที่มี HP เท่าใดก็ได้)*

เมื่อผ่านเงื่อนไข:
- Server เริ่ม dash แบบ homing เข้าหาเป้าหมายด้วยความเร็วรวมไม่เกิน 1.35 บล็อกต่อ tick โดยไม่ใช้ teleport และ sync แรงเริ่มต้นไป client ผ่าน `ImpulseCastData`
- Dash ขับเคลื่อนจาก Forge `PlayerTickEvent` ฝั่ง server ที่ phase `END` หลังการเคลื่อนที่จริงของแต่ละ tick ทำงานสูงสุด 10 movement ticks (นับ initial impulse เป็น tick แรก) และนับระยะเดินทางจริงสะสมได้ไม่เกิน 9.35 บล็อก; ทุก tick จะปรับทิศเข้าหาเป้าหมาย ตรวจ block collision แบบ sample ตามเส้นทาง, world border, สถานะผู้ร่าย/เป้าหมาย และหยุดเมื่อชนกำแพงหรือข้อมูลไม่ valid
- Recast, Mark และ Stance ถูกใช้ทันทีเมื่อเริ่ม dash ทำให้ cooldown เริ่มตามปกติแม้ dash ถูกขวางหรือหมดเวลา โดยกรณีล้มเหลวจะไม่มี damage, fracture, flash, heal หรือ impact animation
- ตอนเริ่ม dash ส่ง animation `SpellAnimations.OVERHEAD_MELEE_SWING_ANIMATION` ของ Iron's Spellbooks และเมื่อ hitbox ของผู้ร่ายเข้าถึงเป้าหมายจริงจึงส่ง `SpellAnimations.TOUCH_GROUND_ANIMATION`
- ดาเมจและเอฟเฟกต์กระแทกเกิดจากเส้นทาง impact เพียงจุดเดียวหลัง AABB จริงของผู้ร่ายและเป้าหมาย intersect กันเท่านั้น จึงไม่มี remote slam ตอนเริ่มพุ่ง
- เป้าหมายถูกกระแทกลงพื้น (`y = -1.2D`)
- สร้างความเสียหาย spell damage จาก `SpellConfig.TigershadeTerrabreak` (ดีฟอลต์เลเวล 1 คือ 30 damage) และคูณด้วย spell power multiplier ของผู้ร่าย
- หากติดตั้ง Epic Fight จะสร้าง fracture ของพื้นรัศมี 3 บล็อกที่ตำแหน่งกระแทกผ่าน optional compat facade
- แสดงเอฟเฟกต์การทุบทุกครั้งที่ทำสำเร็จแม้ไม่มี Epic Fight: เสียงระเบิด/ทั่งตก, custom shockwave, เศษดิน Coarse Dirt, dust สีส้มทอง, flame, lava, แสงวาบ `FLASH` และ explosion emitter
- หากการโจมตีทำให้เป้าหมายตาย (`!target.isAlive()`) ผู้ร่ายจะได้รับการฟื้นฟูเลือด 10 HP
- ล้าง Stance, Mark, UUID link และ client glow
- เมื่อ commit การ dash จะเข้าสู่ cooldown 30 วินาทีจาก config ของ Iron's Spellbooks ไม่ว่าจะ impact สำเร็จหรือถูกยกเลิกระหว่างทาง
- หากเป้าหมายไม่ตายจากการทุบ หรือ damage event ถูกยกเลิกโดย mod อื่น จะไม่ได้รับการฮีล แต่การทุบยังคงล้าง Mark และเข้าสู่ cooldown ตามปกติ

## Lifecycle และ cleanup

- Effect ฝั่งใดฝั่งหนึ่งหมดอายุหรือถูกลบ จะล้าง effect/link อีกฝั่งและยกเลิก recast
- เมื่อผู้ร่ายหรือเป้าหมายตาย จะล้าง hunt ทันที
- เมื่อผู้ร่ายตาย ออกจากเกม หรือเปลี่ยนมิติ จะยกเลิก active dash และล้าง hunt เพื่อไม่ทิ้ง callback/state หรือ Mark ที่ไม่มีเจ้าของ
- เมื่อ login จะ sync target ที่ยัง valid กลับไปยัง client; target ที่ invalid จะไม่ถูกแสดง
- ข้อมูลเก่าหรือ UUID/dimension ที่อ่านไม่ได้ถือเป็น link ที่ไม่ valid และไม่สามารถ slam ได้

## Server / Client responsibility

- Server เป็นผู้เลือกเป้าหมาย ตรวจ ownership/range, ขับเคลื่อน homing dash และตรวจ collision/arrival/timeout, จัดการ effect/recast/cooldown, ทำ damage, heal และ spawn particle/sound/fracture
- Client รับ initial dash impulse ผ่าน `ImpulseCastData`; การปรับทิศทาง tick ถัดไปและการตัดสิน impact เป็น authoritative ฝั่ง server
- Client รับเฉพาะ UUID ของ target สำหรับแสดง Glowing ในมุมมองของผู้ร่าย
- `persistentData` ใช้เป็น authoritative link ฝั่ง server และไม่ถูกสมมติว่าจะ sync ไป client

## Dependencies และข้อจำกัด

- School resource คือ `bhspells:ground` ซึ่งมีค่าเดียวกับ `BHSchoolRegistry.GROUND_RESOURCE`
- โค้ดไม่ hard-load class ของ BHSpells เพราะ dependency ถูกประกาศเป็น optional; หาก school ไม่มีใน registry จะ fallback เป็น `irons_spellbooks:evocation`
- Animation dash/impact ใช้ native animation และ packet ของ Iron's Spellbooks โดยตรง ไม่ขึ้นกับ Epic Fight
- Epic Fight เป็น optional dependency สำหรับ fracture เท่านั้น และมีการกัน runtime failure ไม่ให้หยุด damage, particle ปกติ, heal หรือ cleanup
- Spell damage เคารพการยกเลิก `SpellDamageEvent`/damage event จาก mod อื่น

## การตรวจสอบ

- `./gradlew compileJava` หรือ `./gradlew build`
- ทดสอบในเกมอย่างน้อย:
  - Mark และรับ buff Stance (Speed II + Strength I) ระยะ 20 บล็อก
  - Selective glow แสดงเฉพาะผู้ร่าย
  - กด recast ที่ระยะ 8 บล็อกพอดีเพื่อพุ่งด้วยความเร็วเข้าถึงและทุบเป้าหมายที่ HP ใดก็ได้ (ทั้งสูงกว่าและต่ำกว่า 10%)
  - ปฏิเสธการ recast หากเป้าหมายอยู่ไกลกว่า 8 บล็อก โดยไม่สูญเสียสถานะ Mark/recast
  - พื้นที่โล่งต้องเห็นผู้ร่ายเคลื่อนที่จริงโดยไม่มี teleport และไม่มี damage ก่อน hitbox ถึงตัวเป้าหมาย
  - ทดสอบกำแพง มุม พื้นต่างระดับ entity เบียดกัน และขอบ world border: ต้องหยุด dash โดยไม่มี remote damage หรือ impact effect
  - เป้าหมายเคลื่อนที่ต้องถูก homing ตามได้ภายใน timeout/ระยะสูงสุด หรือยกเลิกอย่างปลอดภัย
  - ผู้ร่ายหันเข้าหาเป้าหมายและไม่มีความเร็วหรือ fall distance คงค้างหลัง impact/abort
  - สร้างความเสียหายพื้นฐาน 30 damage และสเกลตาม Spell Power
  - ฟื้นฟู 10 HP เฉพาะเมื่อเป้าหมายตายจากการทุบ
  - ต้องเห็น animation ยกสองมือทุบตอนเริ่ม dash และ animation กระแทกพื้นตอนถึงเป้าหมาย; เอฟเฟกต์แสงวาบและ particle ปกติแสดงเฉพาะเมื่อ impact สำเร็จ
  - เมื่อติดตั้ง Epic Fight ต้องเพิ่ม fracture ของพื้นเท่านั้น โดย animation ยังคงมาจาก Iron's Spellbooks
  - เมื่อไม่มี Epic Fight การทุบ damage/heal/cleanup และ particle ปกติยังทำงานครบ
  - Cleanup ทำงานถูกต้องเมื่อหมดเวลา 60 วินาที, ผู้ร่ายหรือเป้าหมายตาย, disconnect หรือข้ามมิติ
  - Cooldown 30 วินาทีเริ่มนับเมื่อ commit การ dash แม้เส้นทางถูกขวางหรือหมดเวลาโดยไม่ impact
