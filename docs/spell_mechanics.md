# กลไกการร่ายและวงจรชีวิตของเวท

เอกสารนี้อธิบายรูปแบบร่วมของระบบเวททั้งหมด รายละเอียดเฉพาะเวทให้ดูจาก [สารบัญเอกสารรายสกิล](spells/README.md)

## ภาพรวมการลงทะเบียน

`SpellRegistry` ใช้ Forge `DeferredRegister<AbstractSpell>` ลงทะเบียนเวท 15 รายการ เวทแต่ละคลาสกำหนด registry ID, school, rarity, level, mana, cooldown, cast type และ callback ตาม lifecycle ของ Iron's Spells 'n Spellbooks

```text
SpellRegistry
  → AbstractSpell configuration
  → cast validation
  → cast tick/release/recast callback
  → server gameplay result
  → synchronized entity/data/packet
  → client renderer, particles, sound, and animation
```

## รูปแบบการร่าย

### Instant

เวท Instant ทำงานทันทีหลังผ่านเงื่อนไขการร่าย โดย server เป็นผู้คำนวณ target, damage, effect, movement และการ spawn entity ตัวอย่างได้แก่ Blazing Chakra, Spin Strike, Lightning Strike, Thunder Step, Gale Drive, Glacial Veil และ Glacial Firmament

### Long และ Continuous

เวทกลุ่มนี้มีช่วงเตรียม ชาร์จ หรือ channel ก่อน resolve:

- Pure White Flame Burst ใช้ช่วง channel และลำดับ impact หลายเฟส
- Wings of Tempest ร่ายก่อนสร้าง AoE ที่ติดตามผู้ร่าย
- Crimson Rain Bathes Moon ทำงานเป็น channel หลายเฟสและสร้างหอกตามเวลา
- Venomous Blossomfall และ Gale Piercer ใช้ระยะเวลาชาร์จเพื่อเลือก projectile/ความแรงเมื่อปล่อย
- Shackle of Fear มีช่วง cast สั้นก่อนยิง projectile สร้างโซ่

### Recast และ multi-stage

เวทที่ต้องคงสถานะข้ามการกดหลายครั้งใช้ recast data, effect หรือ entity ที่ sync แล้วแต่ implementation:

- Resonant Knell ใช้ 6 casts แบ่งเป็น Open/Blast สามรอบ
- Tigershade Terrabreak ใช้รอบแรก mark เป้าหมายและรอบถัดไป execute เมื่อระยะและพลังชีวิตผ่านเงื่อนไข

## Server และ client

### Server authoritative

Server ต้องเป็นเจ้าของการตัดสินใจที่มีผลต่อ gameplay ได้แก่:

- ตรวจ mana, cooldown, target และ friendly fire
- คำนวณ damage, knockback, teleport, status effect และ duration
- สร้าง/ลบ entity และเปลี่ยน state ที่ต้อง sync
- จัดการ recast window และผลเมื่อ sequence จบหรือหมดเวลา

### Client presentation

Client รับข้อมูลที่ sync แล้วเพื่อแสดง:

- entity renderer, model, texture และ full-bright/emissive layer
- particle, sound, camera shake และ animation
- interpolation และเวลาเริ่ม VFX ในเครื่อง client

ภาพบน client ไม่ควรเป็นผู้ตัดสิน damage หรือผลการต่อสู้

## Entity-backed spell patterns

| Pattern | ตัวอย่าง | หน้าที่ของ entity |
| --- | --- | --- |
| Following AoE | Wings of Tempest, Resonant Knell | ติดตาม owner, เก็บ state/radius/duration และเป็น anchor ของ VFX |
| Projectile | Shackle of Fear, Venomous Blossomfall, Gale Piercer | เคลื่อนที่ ตรวจ hit และ resolve ผลเมื่อชนหรือหมดระยะ |
| Persistent control | Gold Chain, Gale Drive Vortex | ตรึง/ดูด/ควบคุมเป้าหมายตาม tick และ lifecycle |
| Terrain eruption | Glacial Veil, Glacial Firmament | วาง entity น้ำแข็งตามทิศหรือวงแหวนและแสดง renderer เฉพาะ |

## Resonant Knell lifecycle

```text
Cast 1: Open stage 1
Cast 2: Blast radius 15 → Inactive
Cast 3: Open stage 2
Cast 4: Blast radius 20 → Inactive
Cast 5: Open stage 3
Cast 6: Blast radius 30 → Discard + cooldown
```

`ResonantKnellDomeAoe` sync `state`, `stage`, `shockwave radius` และสัญญาณเริ่ม shockwave มายัง client:

- `STATE_OPEN`: entity ติดตามเท้าผู้ร่าย ตัว renderer เล่น motion กางโดมใหม่ทุก stage จาก scale 0.22 และต่ำกว่าจุดกำเนิด 1.4 บล็อก ใช้เวลา 12 ticks พร้อม fade 6 ticks และ overshoot เบา ๆ
- ระหว่าง Open: วงพื้นสี่วงวนหดจากรัศมี 7.6 เข้าสู่ 0.45 ใต้เท้าผู้ร่าย
- `STATE_EXPLODING`: gameplay damage/launch เกิดทันทีบน server ส่วน client แสดง shell และวงพื้นขยายไปถึงรัศมี 15, 20 หรือ 30 ภายใน 24 ticks พร้อม echo rings สามวง
- `STATE_INACTIVE`: entity ยังติด owner แต่ไม่ render เพื่อรอการเปิด stage ถัดไป
- หลัง blast ของ stage 3 entity ถูก discard

รายละเอียดค่าทั้งหมดอยู่ที่ [Resonant Knell](spells/fire/resonant_knell.md)

## การตรวจสอบเมื่อแก้ระบบเวท

1. เทียบ registry ID, language key, icon, entity และ renderer ให้ตรงกัน
2. ตรวจ server/client responsibility และ synchronized data
3. ตรวจ lifecycle ตอน owner ตาย, recast หมดเวลา, entity ถูก unload และ sequence จบ
4. รัน build/test ที่เกี่ยวข้อง
5. ตรวจ VFX, sound, timing และตำแหน่งจริงในเกม
6. อัปเดตเอกสารรายสกิลและเอกสารภาพรวมที่ได้รับผลกระทบ
