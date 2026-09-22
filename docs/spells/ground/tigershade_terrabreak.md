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
| Mana cost | 40 เฉพาะการกดมาร์คครั้งแรก; การกดปิดฉากเป็น recast |
| Acquisition range | 20 บล็อกและต้องเป็นศัตรูที่อยู่ในแนวสายตา |
| Mark / recast window | 1,200 ticks (60 วินาที) |
| Execute range | ไม่เกิน 5 บล็อก (`distanceToSqr <= 25`) |
| Execute threshold | HP ปัจจุบันไม่เกิน 10% ของ Max HP |
| Execute cooldown | 600 ticks (30 วินาที) หลังใช้จังหวะปิดฉาก |
| Heal | 50 HP หลังยืนยันว่าเป้าหมายตายจากท่าปิดฉากแล้วเท่านั้น |

## ลำดับการทำงาน

### จังหวะที่ 1 — Mark และ Tigershade Stance

เมื่อกดใช้โดยยังไม่มี hunt ที่ทำงานอยู่ server จะหา `LivingEntity` ศัตรูในระยะ 20 บล็อกตาม crosshair หรือกรวยสายตาแคบและต้องมี line of sight

- เป้าหมายได้รับ `TigershadeMarkEffect` 60 วินาที
- ผู้ร่ายได้รับ `TigershadeStanceEffect` 60 วินาที
- Stance ให้ค่าทัดเทียม Speed II (`+40% MOVEMENT_SPEED`) และ Strength I (`+3 ATTACK_DAMAGE`)
- Stance ปล่อยละอองพลังสีเหลืองส้มและเปลวไฟเป็นระยะ
- ระบบสร้าง Iron's Spellbooks recast จำนวนหนึ่งครั้ง อายุ 60 วินาที จึงยังไม่เริ่ม cooldown หลังจังหวะ Mark
- หนึ่งเป้าหมายมีเจ้าของ Mark ได้หนึ่งคนในเวลาเดียวกัน ผู้ร่ายอื่นไม่สามารถขโมยหรือใช้ Mark นั้นปิดฉากได้

ความสัมพันธ์ถูกบันทึกทั้งสองฝั่งด้วย UUID และ dimension:

- ผู้ร่ายเก็บ UUID/dimension ของเป้าหมาย
- เป้าหมายเก็บ UUID/dimension ของเจ้าของ Mark
- ทุกครั้งที่ resolve หรือ execute ต้องตรวจว่าข้อมูลทั้งสองฝั่งตรงกัน รวมทั้ง effect และมิติที่อยู่จริง
- ไม่มี fallback ที่สามารถเลือก Mark ของผู้เล่นคนอื่นได้

### การมองเห็นเป้าหมายเฉพาะผู้ร่าย

Server ส่ง `SyncTigershadeTargetPacket` ไปยังผู้ร่ายเพียงคนเดียวหลัง Mark สำเร็จ Client ของผู้ร่ายจึงเปิด Glowing ให้ entity ที่มี UUID ตรงกับ packet เท่านั้น

- ผู้เล่นอื่นไม่เห็น outline จาก Tigershade
- ไม่สแกนหรือแสดงทุก entity ที่มี `TigershadeMarkEffect`
- ถ้า entity มี Glowing อยู่ก่อนแล้ว Tigershade จะไม่ถือว่าตัวเองเป็นเจ้าของ flag และจะไม่ปิด flag นั้นตอน cleanup
- Glowing แสดงได้เฉพาะตอน entity ถูกโหลด/track อยู่ใน client ตามข้อจำกัดของ Minecraft

### จังหวะที่ 2 — Takedown Slam Execute

เมื่อกดอีกครั้งระหว่าง recast window server ตรวจเงื่อนไขทั้งหมดดังนี้:

1. ผู้ร่ายยังมี Stance และ recast ของเวทนี้
2. เป้าหมายยังมีชีวิตและอยู่มิติเดียวกัน
3. Mark เป็นของผู้ร่ายคนนี้จริงจาก UUID/dimension ทั้งสองฝั่ง
4. ระยะไม่เกิน 5 บล็อก
5. HP เป้าหมายไม่เกิน 10% ของ Max HP

เมื่อผ่านเงื่อนไข ผู้ร่ายได้รับแรงพุ่งเข้าหาเป้าหมาย เป้าหมายถูกกระแทกลง และรับ lethal spell damage หาก damage event ยอมรับการโจมตีแต่เป้าหมายยังรอด ระบบจะตั้ง HP เป็น 0 และเรียก death ด้วย damage source ของเวทเพื่อปิดฉาก

- ฮีลผู้ร่าย 50 HP เฉพาะเมื่อ `target.isAlive()` เป็น false หลังการโจมตี
- แสดงเสียงระเบิด/ทั่งตก, custom shockwave, เศษดิน, dust, flame, lava และ explosion emitter เมื่อสังหารสำเร็จ
- ล้าง Stance, Mark, UUID link และ client glow
- จังหวะปิดฉากใช้ cooldown 30 วินาทีจาก config ของ Iron's Spellbooks
- หาก damage event ถูกยกเลิกหรือเป้าหมายไม่ตาย จะไม่ได้รับฮีล แต่การพยายามปิดฉากยังใช้ Mark และเข้าสู่ cooldown

## Lifecycle และ cleanup

- Effect ฝั่งใดฝั่งหนึ่งหมดอายุหรือถูกลบ จะล้าง effect/link อีกฝั่งและยกเลิก recast
- เมื่อผู้ร่ายหรือเป้าหมายตาย จะล้าง hunt ทันที
- เมื่อผู้ร่ายออกจากเกมหรือเปลี่ยนมิติ จะล้าง hunt เพื่อไม่ทิ้ง Mark ที่ไม่มีเจ้าของ
- เมื่อ login จะ sync target ที่ยัง valid กลับไปยัง client; target ที่ invalid จะไม่ถูกแสดง
- ข้อมูลเก่าหรือ UUID/dimension ที่อ่านไม่ได้ถือเป็น link ที่ไม่ valid และไม่สามารถ execute ได้

## Server / Client responsibility

- Server เป็นผู้เลือกเป้าหมาย ตรวจ ownership/range/HP, จัดการ effect/recast/cooldown, ทำ damage, heal, movement และ spawn particle/sound
- Client รับเฉพาะ UUID ของ target สำหรับแสดง Glowing ในมุมมองของผู้ร่าย
- `persistentData` ใช้เป็น authoritative link ฝั่ง server และไม่ถูกสมมติว่าจะ sync ไป client

## Dependencies และข้อจำกัด

- School resource คือ `bhspells:ground` ซึ่งมีค่าเดียวกับ `BHSchoolRegistry.GROUND_RESOURCE`
- โค้ดไม่ hard-load class ของ BHSpells เพราะ dependency ถูกประกาศเป็น optional; หาก school ไม่มีใน registry จะ fallback เป็น `irons_spellbooks:evocation`
- Lethal damage เคารพการยกเลิก `SpellDamageEvent`/damage event จาก mod อื่น การยกเลิกดังกล่าวทำให้ไม่สังหารและไม่ฮีล

## การตรวจสอบ

- `./gradlew build`
- ทดสอบในเกมอย่างน้อย: Mark/buff 60 วินาที, selective glow ใน multiplayer, ปฏิเสธ execute เมื่อ HP >10% หรือไกลกว่า 5 บล็อก, execute+heal เมื่อผ่านเงื่อนไข, ไม่ฮีลเมื่อ damage ถูกยกเลิก, cleanup เมื่อหมดเวลา/ตาย/ออกเกม/เปลี่ยนมิติ และ cooldown 30 วินาทีหลังจังหวะปิดฉาก
