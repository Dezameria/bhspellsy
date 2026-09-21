# เอกสารระบบ IronSpell More

เอกสารชุดนี้อธิบายโครงสร้างและพฤติกรรมปัจจุบันของ IronSpell More สำหรับ Minecraft Forge 1.20.1

ปัจจุบัน `SpellRegistry` ลงทะเบียนเวททั้งหมด **15 เวทใน 6 สายเวท** ได้แก่ Fire 5, Lightning 2, Nature 3, Aqua 3, Gold 1 และ Ground 1

## แผนที่เอกสาร

- [รายการเวททั้งหมด](all_spells.md) — สรุปเวททั้ง 15 รายการตาม registry พร้อมลิงก์ไปยัง specification ของแต่ละเวท
- [เอกสารรายสกิล](spells/README.md) — สารบัญและกฎการดูแลเอกสารรายเวท
- [กลไกการร่ายและวงจรชีวิต](spell_mechanics.md) — flow ของ Instant, Long, Continuous, Recast และความรับผิดชอบของ server/client
- [สถาปัตยกรรมระบบ](system_architecture.md) — registry, package, entity, renderer, particle และ subsystem หลัก
- [สถาปัตยกรรม compatibility](compat_architecture.md) — การเชื่อมต่อกับม็อดและ API ภายนอก

## Source of truth

เมื่อตรวจสอบค่าหรือพฤติกรรม ให้ใช้ลำดับความน่าเชื่อถือดังนี้:

1. Implementation และ registry ใน `src/main/java`
2. เอกสารรายสกิลใน `docs/spells/<school>/<spell_id>.md`
3. เอกสารสรุประดับระบบในโฟลเดอร์ `docs`

`docs/all_spells.md` เป็นสารบัญภาพรวม ไม่ใช้แทน specification รายสกิล

## การดูแลเอกสาร

- การเพิ่ม ลบ ปรับสมดุล หรือเปลี่ยนพฤติกรรมของเวท ต้องอัปเดตเอกสารรายสกิลใน change เดียวกัน
- การเพิ่มหรือลบเวทต้องซิงก์ `SpellRegistry`, `docs/spells/README.md` และ `docs/all_spells.md`
- การเปลี่ยน entity, renderer, packet หรือเส้นแบ่ง server/client ต้องอัปเดต `spell_mechanics.md` และ `system_architecture.md` เมื่อภาพรวมระบบเปลี่ยน
- ต้องรัน build/test ที่เกี่ยวข้องและระบุ runtime behavior ที่ยังต้องตรวจในเกม

## การอัปเดตล่าสุด: Resonant Knell

ระบบ Resonant Knell ใช้ entity ที่ติดตามผู้ร่ายและสลับระหว่าง Open, Inactive และ Exploding ตลอดสามรอบของ recast ภาพโดมจะยกตัวและกางขึ้นทุกครั้งที่เปิด ขณะที่วงพลังบนพื้นหดเข้าหาเท้าผู้ร่าย เมื่อระเบิด วงนำและวงตามจะขยายออกไปถึงรัศมี gameplay 15, 20 หรือ 30 บล็อก

รายละเอียดทั้งหมดอยู่ที่ [Resonant Knell specification](spells/fire/resonant_knell.md)
