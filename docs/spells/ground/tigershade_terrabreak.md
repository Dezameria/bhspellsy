# Tigershade Terrabreak

## ข้อมูลหลัก

| รายการ | ค่าปัจจุบัน |
| --- | --- |
| Registry ID | `ironspell_more:tigershade_terrabreak` |
| Class | `spells/ground/TigershadeTerrabreakSpell.java` |
| School | `bhspells:ground`; fallback เป็น `irons_spellbooks:evocation` เมื่อหา Ground school ไม่พบ |
| Rarity | Epic |
| Max level | 1 |
| Cast type | Instant |
| Cast time | 0 ticks (ทันที) |
| Framework Cooldown | 0.0 วินาที (เพื่อรองรับการร่าย Phase 2 Execute; คูลดาวน์จริง 30 วินาที ถูกนำมาใช้เมื่อทุ่มปิดฉากสำเร็จ) |
| Execute Cooldown | 600 ticks (30 วินาที) |
| Mana cost | Base 40 |
| Spell power | Base 10 |
| Acquisition Range | 20 blocks (Phase 1 Raycast ล็อคเป้าหมาย) |
| Execute Max Distance | 5 blocks (ระยะทำการจับทุ่มปิดฉาก) |
| Execute Health Threshold | <= 10% ของ Max Health ของเป้าหมาย |
| HP Restored | 50 HP (25 หัวใจ) ฟื้นฟูให้ผู้ใช้หลังการทุ่มสำเร็จ |
| Language keys | `spell.ironspell_more.tigershade_terrabreak`, `.guide`, `effect.ironspell_more.tigershade_stance`, `effect.ironspell_more.tigershade_mark`, `ui.ironspell_more.tigershade_*` |

---

## กลไกการทำงาน (Two-Phase Hunt & Execute)

### Phase 1: การมาร์คเป้าหมาย (Mark Target & Tigershade Stance)
- เมื่อกดใช้สกิลขณะมองไปที่เป้าหมาย (LivingEntity ที่มีชีวิตและไม่ใช่ฝ่ายเดียวกัน) ภายในระยะ 20 บล็อก:
  - **เป้าหมาย:** ได้รับเอฟเฟกต์ `TigershadeMarkEffect` เป็นเวลา 60 วินาที (1200 ticks)
    - เป้าหมายจะแสดงเส้นขอบเรืองแสง (**Glowing Outline**) โดยมีเฉพาะผู้ใช้ที่กำลังอยู่ใน Tigershade Stance เท่านั้นที่มองเห็น (Client-side selective glowing)
  - **ผู้ใช้:** ได้รับเอฟเฟกต์ `TigershadeStanceEffect` เป็นเวลา 60 วินาที (1200 ticks)
    - บัฟความเร็วเทียบเท่า **Speed II** (+40% Movement Speed) ผ่าน AttributeModifier
    - บัฟพลังโจมตีเทียบเท่า **Strength I** (+3.0 Attack Damage) ผ่าน AttributeModifier
    - ปล่อย Particle ไอคลื่นพลังสีเหลือง-ส้มรอบตัวอย่างต่อเนื่อง (`DustParticleOptions` สีเหลือง-ส้มอำพัน และ `FLAME`)
  - มีเสียง Heartbeat และ Roar พร้อมละอองพลังสีส้ม-เหลืองระเบิดที่เป้าหมายเมื่อมาร์คสำเร็จ
  - หากไม่มีเป้าหมายที่ถูกต้องอยู่ในระยะสายตา จะไม่ร่ายสกิลและแสดงข้อความแจ้งเตือนที่ Action Bar

### Phase 2: การจับทุ่มปิดฉาก (Takedown Slam Execute)
- เมื่อผู้ใช้อยู่ในสถานะ Tigershade Stance และกดใช้สกิลอีกครั้ง:
  - **เงื่อนไขในการจับทุ่ม:**
    1. เป้าหมายที่ถูกมาร์คยังคงมีชีวิตอยู่
    2. เป้าหมายอยู่ในระยะ **ไม่เกิน 5 บล็อก** (`distanceToSqr <= 25.0`)
    3. พลังชีวิตของเป้าหมายเหลือ **ต่ำกว่าหรือเท่ากับ 10% ของ Max Health** (`health <= maxHealth * 0.10F`)
  - **ผลลัพธ์เมื่อเข้าเงื่อนไขครบถ้วน:**
    - ผู้ใช้จะพุ่งตัวระยะประชิด (Lunge impulse) เข้าหาเป้าหมาย
    - จับเป้าหมายทุ่มกระแทกลงพื้นอย่างรุนแรง
    - สร้างความเสียหายเวทสังหารเด็ดขาด (Lethal damage) ทำให้พลังชีวิตของเป้าหมายเหลือ 0 ทันที จบการต่อสู้โดยไม่สนเลือดที่เหลือภายใต้เกณฑ์ 10%
    - ผู้ใช้ได้รับการฟื้นฟูพลังชีวิต **50 HP** ทันที
    - เกิดคลื่นกระแทกแผ่นดินไหว (Earth shatter impact) พร้อมวงแหวน Shockwave กระจายตัวสีเหลือง-ส้ม (`ShockwaveParticleOptionCustom`), ละอองฝุ่นและประกายไฟระเบิดสีส้ม-เหลือง (`DustParticleOptions`, `FLAME`, `LAVA`), เศษดินระเบิด (`COARSE_DIRT`), Explosion Emitter, และเสียงระเบิดกัมปนาท
    - ลบล้างสถานะ Mark จากเป้าหมาย และลบล้างสถานะ Stance จากผู้ใช้
    - ติดคูลดาวน์สกิลทันที **30 วินาที** (600 ticks)
  - **กรณีที่ไม่เข้าเงื่อนไข:**
    - หากเป้าหมายอยู่ไกลเกิน 5 บล็อก: แสดงข้อความแจ้งเตือนที่ Action Bar โดยไม่เสียสถานะ Stance และไม่ติดคูลดาวน์ใหญ่
    - หากพลังชีวิตของเป้าหมายยังสูงกว่า 10%: แสดงข้อความแจ้งเตือนที่ Action Bar โดยไม่เสียสถานะ Stance และไม่ติดคูลดาวน์ใหญ่

---

## Client-side & Rendering

- `TigershadeClientEvents` ทำหน้าที่ตรวจจับ Tick ทางฝั่ง Client:
  - หาก Local Player มีเอฟเฟกต์ `TigershadeStanceEffect` จะค้นหา Entity ในระยะที่มี `TigershadeMarkEffect` และสั่ง `setGlowingTag(true)` เฉพาะบน Client เครื่องนั้น
  - เมื่อหมดสถานะ Stance หรือเป้าหมายหลุดจาก Mark จะสั่ง `setGlowingTag(false)` คืนค่าทันที ทำให้ไม่มีผลกระทบต่อผู้เล่นอื่นใน Server

---

## Dependency และการตรวจสอบ

- Ground school เชื่อมต่อกับ `bhspells` ผ่าน `BHSchoolRegistry.GROUND_RESOURCE`
- หากรันในสภาพแวดล้อมที่ไม่มี mod `bhspells` จะ Fallback ไปใช้ `SchoolRegistry.EVOCATION` โดยอัตโนมัติ ไม่ทำให้เกม Crash หรือ ClassNotFound
