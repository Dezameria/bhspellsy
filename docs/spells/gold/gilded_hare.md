# Gilded Hare (จินตู้หลิวหลิง / กระต่ายทองคำ)

## ข้อมูลหลัก

| รายการ | ค่าปัจจุบัน |
| --- | --- |
| Registry ID | `ironspell_more:gilded_hare` |
| Class | `spells/gold/GildedHareSpell.java` |
| School | `bhspells:gold`; fallback เป็น Ender เมื่อหา Gold school ไม่พบ |
| Theme | สายพลังเน้นการบัฟและป้องกันตนเองแบบเรียบง่ายและยืดหยุ่น (จินตู้หลิวหลิง / Golden Hare Flowing Spirit), อาศัยความพลิ้วไหวของร่างกายร่วมกับอาวุธริบบิ้นพันข้อเท้า |
| Rarity | Rare |
| Max level | 1 |
| Cast type | Instant (กดคลิกครั้งเดียว) |
| Cast time | 0 ticks (ร่ายทันที) |
| Cooldown | 60 วินาที |
| Mana | 80 mana |
| Spell power | base 1, เพิ่ม 1 ต่อเลเวล |
| Caster Status Effects | Speed I, Jump Boost I, Attack Speed I (+10%), Absorption, Strength I, และ `ironspell_more:gilded_hare` ทั้งหมดคงอยู่ 2 นาที (2400 ticks) |
| Target Debuffs | `ironspell_more:gilded_hare_mark` (ริบบิ้นพันธนาการเพิ่มทีละเส้น 1-4 เส้นตามคอมโบ), Slowness I (20 ticks / 1 วินาทีต่อการเตะโดน 1 ครั้ง) |
| 5-Hit Finisher | Stun 1 วินาที (20 ticks) สมบูรณ์, Blindness 3 วินาที (60 ticks), พันธนาการดักแด้ริบบิ้นมิดตัวพร้อมหูกระต่ายริบบิ้น (Full Cocoon, amplifier 4) และเมื่อครบ 1 วินาที ดักแด้จะแตกกระจายออกเป็นเศษริบบิ้นคริสตัลทองคำ (Cocoon Shatter) |
| Language keys | `spell.ironspell_more.gilded_hare`, `.guide`, `effect.ironspell_more.gilded_hare`, และ `effect.ironspell_more.gilded_hare_mark` |

## กลไกการทำงานและผลลัพธ์

### 1. การบัฟตนเอง (Self-Buff Phase)
- เมื่อร่ายคาถา จะงอกหูกระต่ายริบบิ้นออกมาบนศีรษะ พร้อมออร่าสีทองเล็กจางๆ ที่ริบบิ้นทั้งตัวและข้อเท้า
- กระจายละอองทองคำเบาบางรอบตัว ช่วยเพิ่มความเร็วและความยืดหยุ่นให้ร่างกาย และสลายแรงปะทะของศัตรูออกไปได้อย่างนุ่มนวล
- ผู้ร่ายจะได้รับบัฟคงอยู่นาน **2 นาที (2400 ticks)**:
  - **Speed I** (`MobEffects.MOVEMENT_SPEED`, Amplifier 0)
  - **Jump Boost I** (`MobEffects.JUMP`, Amplifier 0)
  - **Attack Speed I** (เพิ่มความเร็วโจมตี +10% ผ่าน `Attributes.ATTACK_SPEED` บน `GildedHareEffect`)
  - **Absorption** (`MobEffects.ABSORPTION`, Amplifier 0 มอบเกราะหัวใจสีทอง)
  - **Strength I** (`MobEffects.DAMAGE_BOOST`, Amplifier 0)
  - **Gilded Hare Stance** (`ironspell_more:gilded_hare`) ควบคุมออร่าละอองทองคำ, การแสดงผลเลเยอร์ริบบิ้น, และตรวจจับการเตะ/โจมตี

### 2. การเตะและดีบัฟริบบิ้นพันธนาการสะสม (Progressive Ribbon Wrap & Combo Phase)
- ขณะมีผลของ `ironspell_more:gilded_hare` เมื่อผู้ร่ายเตะหรือโจมตีระยะประชิด (Direct Melee Hit) โดนศัตรู:
  - มีเอฟเฟกลอยลางๆ และละอองทองคำกระจายทุกครั้งที่เตะโดน
  - เป้าหมายจะได้รับดีบัฟ **ริบบิ้นพันธนาการ** (`ironspell_more:gilded_hare_mark`) โดยริบบิ้นจะค่อยๆ เพิ่มขึ้นทีละเส้นตามจำนวนครั้งที่เตะโดน:
    - **เตะครั้งที่ 1 (Hit 1)**: เกิดริบบิ้น 1 เส้น พันช่วงล่างของเป้าหมาย (Amplifier 0)
    - **เตะครั้งที่ 2 (Hit 2)**: ริบบิ้นเพิ่มเป็น 2 เส้น พันสูงขึ้นมาถึงช่วงเอว (Amplifier 1)
    - **เตะครั้งที่ 3 (Hit 3)**: ริบบิ้นเพิ่มเป็น 3 เส้น พันขึ้นมาถึงช่วงลำตัว (Amplifier 2)
    - **เตะครั้งที่ 4 (Hit 4)**: ริบบิ้นเพิ่มเป็น 4 เส้น พันขึ้นมาถึงช่วงอกและไหล่ (Amplifier 3)
  - เป้าหมายติด **Slowness I** เป็นเวลา **1 วินาที (20 ticks)** ทุกครั้งที่เตะโดน
  - ริบบิ้นเป็นสีทองโปร่งแสงคล้ายผ้ากระจกคริสตอลประกายวิ้งๆ ขนาดจะปรับตามความกว้างและสูงของเป้าหมายโดยอัตโนมัติ
  - ระบบจะบันทึกคอมโบการเตะต่อเนื่อง (Combo Window 100 ticks หรือ 5 วินาที) ช่วยให้ผู้เล่นมีเวลาออกท่าต่อเนื่องได้ทันโดยสแตกไม่หายไวก่อนกำหนด หากเปลี่ยนเป้าหมายหรือเกินเวลา คอมโบจะรีเซ็ต

### 3. ฟินิชเชอร์เตะครบ 5 ครั้งและดักแด้แตกออก (5-Hit Ribbon Cocoon Stun & Shatter)
- หากผู้ร่ายเตะโดนเป้าหมายเดิมต่อเนื่องครบ **5 ครั้งโดยไม่หลุดคอมโบ**:
  - เป้าหมายจะติดสถานะ **Stun สมบูรณ์เป็นเวลา 1 วินาที (20 ticks)** (เคลื่อนที่ไม่ได้, ขุดไม่ได้, กระโดดไม่ได้, ยกเลิก knockback, ความเร็วแกน x/y/z ถูกล็อกเป็น 0)
  - เป้าหมายจะติดสถานะ **Blindness เป็นเวลา 3 วินาที (60 ticks)** (ริบบิ้นพันมิดตัวและทำให้เสียการมองเห็น)
  - การแสดงผลริบบิ้นจะเปลี่ยนเป็น **ดักแด้ริบบิ้นสีทองพันมิดทั้งตัว (Full Cocoon, Amplifier 4)** พร้อมมีหูกระต่ายริบบิ้นโผล่ขึ้นมาด้านบนศีรษะของเป้าหมาย
  - **การแตกสลายของดักแด้ (Cocoon Shatter)**: เมื่อครบระยะเวลา Stun 1 วินาที (20 ticks) ดักแด้ริบบิ้นจะแตกสลายออกทันที เกิดเสียงกระจก/คริสตัลริบบิ้นแตก (`SoundEvents.AMETHYST_CLUSTER_BREAK`, `SoundEvents.GLASS_BREAK`, `SoundEvents.WOOL_BREAK`) พร้อมสะเก็ดริบบิ้นสีทอง 36 ชิ้นและประกายคริสตัลระเบิดพุ่งกระจายรอบทิศทาง
  - **คูลดาวน์กันปั๊มสแตกต่อเนื่อง (Finisher Stack Lockout 10 วินาที / 200 ticks)**: เมื่อคอมโบครบ 5 ครั้งและดักแด้ทำงาน เป้าหมายจะติดสถานะคูลดาวน์ 10 วินาที ทำให้การโจมตีใส่เป้าหมายนี้จะไม่เริ่มนับหรือขึ้นสแตกใหม่จนกว่าจะพ้นระยะเวลา 10 วินาที
  - รีเซ็ตคอมโบการเตะ และเคลียร์มาร์กหลังจบสถานะดักแด้

## Audio, VFX และ Animation

- **เสียงเมื่อร่าย**: `SoundEvents.ENCHANTMENT_TABLE_USE`, `SoundEvents.AMETHYST_BLOCK_CHIME` (เสียงประกายแก้วคริสตอลกังวานนุ่มนวล)
- **เสียงเมื่อเตะโดน**: `SoundEvents.PLAYER_ATTACK_WEAK`, `SoundEvents.AMETHYST_BLOCK_HIT` (ประกายคริสตัลริบบิ้นกระทบเป้าหมาย)
- **เสียงเมื่อติด Stun ดักแด้**: `SoundEvents.PLAYER_ATTACK_CRIT`, `SoundEvents.BELL_RESONATE`, `SoundEvents.AMETHYST_BLOCK_CHIME`
- **เสียงเมื่อดักแด้แตกออก (Cocoon Shatter)**: `SoundEvents.AMETHYST_CLUSTER_BREAK`, `SoundEvents.GLASS_BREAK`, `SoundEvents.WOOL_BREAK`
- **อนุภาค (Particles)**:
  - รอบตัวผู้ร่าย: ละอองทองคำเบาบางหมุนวนสม่ำเสมอ
  - ทุกครั้งที่เตะโดน: ละอองทองคำและสะเก็ดริบบิ้นสีทองจางๆ พุ่งตามทิศทางการเตะ
  - เมื่อติดดักแด้ 5 ฮิต: วงแหวนประกายคริสตัลระเบิดออกรอบตัวเป้าหมาย
  - เมื่อดักแด้ครบเวลาแล้วแตกออก: สะเก็ดริบบิ้นทองคำ 36 ชิ้น, Crit sparks และ Wax off พุ่งกระจายรอบทิศทาง
- **Render Layer & Visuals**:
  - `GildedHareRibbonGeometry`: เก็บ geometry ริบบิ้นกลางที่ใช้ร่วมกันทั้ง renderer ปกติและ Epic Fight เพื่อให้รูปทรง สี และ animation ตรงกัน
  - `GildedHarePlayerLayer`: เรนเดอร์หูกระต่ายริบบิ้นบนศีรษะ และริบบิ้นพันข้อเท้า/หลังเท้า (Ankle & Foot Ribbon Wraps ที่ระดับ y = 9.2 ถึง 11.8 พร้อมโบว์ข้อเท้าด้านนอกที่ y = 10.2) บน vanilla player renderer
  - `GildedHareBindingRenderEvents`: เรนเดอร์ริบบิ้นพันรอบตัวเป้าหมายตามขนาด Bounding Box บน vanilla living renderer (Amplifier 0-3 = ริบบิ้นค่อยๆ พันทีละ 1 ถึง 4 เส้นตามลำดับ, Amplifier 4 = ดักแด้ริบบิ้นมิดตัวพร้อมหูกระต่าย)
  - `GildedHareEpicFightRenderCompat`: เพิ่ม custom patched layer ให้ Epic Fight living renderers; ริบบิ้นผู้ร่ายยึดกับ head/leg joint matrices ของ animation เฟรมปัจจุบัน (โดย leg joint ใช้ transform scale `(-1, 1, -1)` ชดเชยแกน Y ของ biped armature และ translate `-0.375m` ชดเชย pivot หัวเข่ากลับสู่พิกัดสะโพก เพื่อให้ริบบิ้นพันธนาการข้อเท้าและโบว์อยู่ที่ข้อเท้า/หลังเท้าอย่างแม่นยำ ไม่ลอยขึ้นไปอยู่ที่ระดับเอว) และริบบิ้นเป้าหมายเรนเดอร์จาก entity-root transform (แสดงผล 1-4 เส้น และดักแด้ครบทั้งตัวตาม amplifier)

## สถาปัตยกรรม Server / Client

- **Server**:
  - `GildedHareSpell`: ตรวจสอบและให้สถานะบัฟ 6 รายการแก่ผู้ร่าย
  - `GildedHareEffect`: ให้ AttributeModifier ความเร็วโจมตี +10%, ควบคุม tick อนุภาค
  - `GildedHareMarkEffect`: จัดเก็บสถานะ Stun, คุมการ zero delta movement ในช่วงดักแด้
  - `GildedHareCombatEvents`: ดักจับ `LivingDamageEvent` คัดกรองเฉพาะการโจมตีระยะประชิดโดยตรงจากผู้ที่มีบัฟ `GildedHareEffect`
- **Client**:
  - เรนเดอร์เลเยอร์ริบบิ้นสำหรับผู้เล่น
  - เรนเดอร์ดักแด้ริบบิ้นสำหรับเป้าหมายที่ติดดีบัฟ
  - เมื่อ Epic Fight ใช้ patched renderer จะเปลี่ยนไปใช้ custom `PatchedLayer` โดยตรง จึงแสดงผลได้ใน battle mode และบนเป้าหมายที่ vanilla `RenderLivingEvent.Post` ถูกข้าม
  - รองรับการจัดวางหูและข้อเท้าร่วมกับ Epic Fight `HumanoidArmature` ของ entity นั้นเอง โดยใช้ pose matrices ที่ renderer ส่งมาในเฟรมปัจจุบัน
  - การลงทะเบียน Epic Fight ถูกกั้นด้วยการตรวจม็อดและ physical-client dist เพื่อไม่ให้ dedicated server หรือการเล่นโดยไม่ติดตั้ง Epic Fight โหลดคลาส client compatibility
