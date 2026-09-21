# Glacial Firmament (เหมันต์ผนึกฟ้า)

## ข้อมูลพื้นฐาน (Basic Information)
- **Display Name:** Glacial Firmament
- **Registry ID:** `ironspell_more:glacial_firmament`
- **School:** Aqua (`TravelopticsSchools.AQUA_RESOURCE`)
- **Rarity:** EPIC
- **Max Level:** 5
- **Cast Type:** INSTANT
- **Cast Time:** 0 ticks
- **Base Mana Cost:** 60 (+15 ต่อเลเวล)
- **Cooldown:** 25 วินาที
- **Animation:** `SpellAnimations.TOUCH_GROUND_ANIMATION`
- **Sounds:** `SoundRegistry.FROSTWAVE_PREPARE`, `SoundRegistry.ICE_SPIKE_EMERGE`

## กลไกการทำงาน (Spell Mechanics)
1. **Ice Domain & Radial Particle Blast (อาณาเขตน้ำแข็งและละอองน้ำแข็งพุ่งกระจายรอบตัว):**
   - เมื่อกดใช้ ผู้ร่ายจะปลดปล่อยพลังน้ำแข็งมหาศาลระเบิดออกมารอบตัว สร้างอาณาเขตน้ำแข็งในรัศมี 15 บล็อก
   - มีการแสดงผลของคลื่นวงแหวน Shockwave และเกล็ดหิมะกระจายออกรอบทิศทาง (`BlastwaveParticleOptions` + `ShockwaveParticlesPacket`)
   - **Radial Particle Blast:** ปลดปล่อยละอองน้ำแข็งและเกล็ดหิมะพุ่งกระจายพวยพุ่งระเบิดออกจากตัวผู้ร่ายเป็นแฉกรัศมี 48 ทิศทาง (`ParticleHelper.SNOWFLAKE`, `ParticleHelper.SNOW_DUST`, และ vanilla `SNOWFLAKE`) ด้วยความเร็วสูง แผ่ปกคลุมทั่วอาณาเขต 15 บล็อกอย่างตระการตา
2. **Speed II Buff for Caster:**
   - ผู้ร่ายจะได้รับผลของบัฟ Speed II (`MobEffects.MOVEMENT_SPEED`, Amplifier 1) เป็นเวลา 2 วินาที (40 Ticks) เพื่อเพิ่มความคล่องตัวในการเคลื่อนที่และใช้ประโยชน์จากอาณาเขต
3. **Domain Damage & Slowness II to Enemies:**
   - ศัตรูทุกตัวที่อยู่ภายในรัศมี 15 บล็อก (โดยตรวจสอบ Line of Sight และป้องกัน Friendly Fire) จะได้รับความเสียหาย 35 หน่วย (คำนวณสเกลตาม Aqua Spell Power ของผู้ร่าย)
   - ศัตรูที่ถูกโจมตีจะติดสถานะ Slowness II (`MobEffects.MOVEMENT_SLOWDOWN`, Amplifier 1) เป็นเวลา 3 วินาที (60 Ticks) ทำให้การเคลื่อนไหวชะลอลงอย่างรุนแรง
4. **Glacial Spike & Glacial Tomb Eruption (การปะทุของผลึกและหนามน้ำแข็งที่กระจายตัวสวยงาม):**
   - เสกแท่งหนามน้ำแข็ง (`GlacialSpikeEntity`) และเสาผลึกสุสานน้ำแข็ง (`GlacialTombEntity`) กระจายออกเป็น 3 วงแหวนที่เว้นระยะห่างอย่างลงตัว (Concentric Rings: รัศมี 4.0, 8.5, และ 14.0 บล็อก รวม 25 ต้น)
   - ตัวหนามและเสาผลึกสุสานถูกวางสลับและกระจายตัวไม่กระจุกตัวกัน เกิดเป็นลานประลองน้ำแข็งรอบตัวผู้ร่าย
   - **การเอียงพุ่งแทงออกตามระยะทาง (Outward Angled Thrust):**
     - **วงใน (ระยะใกล้ ~4.0 บล็อก):** เอียงเพียงเล็กน้อย (8° - 14°) เพื่อคงรูปเป็นเสาน้ำแข็งที่ตั้งตรงรอบผู้ร่าย
     - **วงกลาง (ระยะกลาง ~8.5 บล็อก):** เอียงเฉียงออกจากตัวปานกลาง (22° - 30°)
     - **วงนอก (ระยะไกล ~14.0 บล็อก):** พุ่งแทงเฉียงออกอย่างดุดันเป็นมุม 46° ถึง 56° ดุจดั่งคมดาบน้ำแข็งที่พุ่งแทงทะยานออกจากพื้นดินรอบอาณาเขต
   - มีการหน่วงเวลาการโผล่ (Staggered Wave: 2, 5, 8 ticks) เพื่อให้ผลึกน้ำแข็งพุ่งแทงทะลุพื้นดินขึ้นมาเป็นระลอกคลื่นไล่ระดับจากวงในสู่วงนอก
5. **Shatter on Expiry (การแตกสลายตัวเมื่อหมดเวลาสกิล):**
   - เมื่อครบกำหนดเวลาคงอยู่ (~2.75 - 3 วินาที) หนามน้ำแข็งและเสาผลึกสุสานแต่ละต้นจะแตกสลายตัว (`shatter()`) ระเบิดออกเป็นเศษเสี้ยวน้ำแข็ง (`Blocks.ICE`, `Blocks.PACKED_ICE`) พร้อมละอองหิมะพุ่งกระจายรอบทิศทาง ควบคู่กับเสียงผลึกน้ำแข็งแตกกัมปนาท (`SoundEvents.GLASS_BREAK` และ `SoundRegistry.ICE_IMPACT`) ก่อนจะสลายหายไปในพริบตา

## ค่าตัวเลขและการคำนวณ (Formulas & Scaling)
- **Damage:** 35 Base Damage (+5 ต่อเลเวลเวทมนตร์) สเกลตาม Aqua Spell Power ของผู้ร่าย
- **Caster Buff:** Speed II นาน 2 วินาที (Amplifier 1, 40 Ticks)
- **Enemy Debuff:** Slowness II นาน 3 วินาที (Amplifier 1, 60 Ticks)
- **Range / Radius:** 15 บล็อกรอบตัวผู้ร่าย
- **Mana Cost:** 60 Base Mana (+15 ต่อเลเวล)
- **Cooldown:** 25 วินาที

## เอนทิตีและเอฟเฟกต์ (Entities & Visuals)
- `GlacialSpikeEntity`: หนามน้ำแข็งขนาดยักษ์ สเกลขนาดใหญ่พิเศษตามระยะห่าง (2.6x - 5.5x) พร้อมเอียงพุ่งแทงออกจากผู้ร่าย
- `GlacialTombEntity`: เสาผลึกสุสานน้ำแข็งมหึมาโปร่งแสง (2.0x - 3.8x) ที่ผุดและเอียงสลับกับหนามน้ำแข็งอย่างสง่างาม
- `Textures`: ใช้เทกเจอร์น้ำแข็งมาตรฐานของ Iron's Spells 'n Spellbooks (`textures/entity/ice_spike.png` และ `textures/entity/ice_tomb/...`)

## การทำงานฝั่ง Server / Client
- **Server:** ค้นหาเป้าหมายศัตรูในระยะ 15 บล็อก, สร้างดาเมจ, มอบ Speed II แก่ผู้ร่าย, ใส่ Slowness II แก่ศัตรู, และเสก `GlacialSpikeEntity` กับ `GlacialTombEntity` ลงในโลก
- **Client:** แสดงผลคลื่นกระแทก Shockwave, ละอองหิมะ, อนิเมชั่นผู้ร่ายทุบพื้น, และเรนเดอร์โมเดลหนามน้ำแข็งกับเสาผลึก
