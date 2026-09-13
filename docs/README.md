# 📚 เอกสารคู่มือระบบ IronSpell More (Documentation)

ยินดีต้อนรับสู่เอกสารโครงสร้างระบบและการทำงานของ Mod **IronSpell More** (Minecraft Forge 1.20.1)

เอกสารฉบับนี้ถูกจัดแบ่งออกเป็น 3 ส่วนหลักเพื่อให้ง่ายต่อการศึกษาและพัฒนาต่อ:

---

## 📑 สารบัญเอกสาร

### 1. [รายการเวทมนตร์ทั้งหมด (All Spells Overview)](all_spells.md)
* รวบรวมเวทมนตร์ทั้งหมด 5 สกิลใน Mod:
  - **Pure White Flame Burst** (เพลิงขาวบริสุทธิ์)
  - **Spin Strike** (หมุนตัวพุ่งทะลวงเพลิง)
  - **Lightning Strike** (อัสนีบาตทะลวงเงา)
  - **Thunder Step** (ก้าวย่างอัสนี)
  - **Shackle of Fear** (โซ่ตรวนแห่งความกลัว)
* รายละเอียดสายเวท (School), ระดับความหายาก (Rarity), ค่ามานา, คูลดาวน์ และความสามารถของแต่ละสกิล

---

### 2. [โครงสร้างการทำงานและขั้นตอนการร่ายเวท (Spell Mechanics & Execution Flow)](spell_mechanics.md)
* แผนผัง Flowchart การทำงานของแต่ละ Spell ตั้งแต่เริ่มร่ายจนจบ
* วงจรชีวิตของการร่าย (Cast Lifecycle): `checkCanCast` -> `onServerCastTick` -> `onCast`
* สิ่งที่แต่ละ Spell เรียกใช้:
  - การคำนวณเวกเตอร์และระยะทาง (Raycasting, Oriented Bounding Box)
  - การเรียกใช้ **Effekseer VFX** (`pure_white_flame.efkefc`) ผ่าน AAA Particles
  - การเรียกใช้พื้นแตกร้าวผ่าน **Epic Fight API** (`LevelUtil.circleSlamFracture`)
  - กลไกสถานะ **White Flame Burn** และ **Spin Strike Dash**

---

### 3. [โครงสร้างระบบทั้งหมด (Overall System Architecture)](system_architecture.md)
* สถาปัตยกรรมระดับภาพรวมของ Mod (System Architecture Diagram)
* ผังโฟลเดอร์และแพ็กเกจของโปรเจกต์ (`io.redspace.ironspell_more`)
* ระบบ DeferredRegister (Spells, MobEffects, Particles, Entities, Items)
* ระบบ Client Rendering (Particle Providers, Entity Renderers)
* รายละเอียดระบบนิเวศอนุภาคเพลิงขาว (White Fire, White Fire Emitter, White Ember)
* การเชื่อมต่อและผสานรวมกับ Mod ภายนอก (Iron's Spells, Epic Fight, AAA Particles, GeckoLib)
