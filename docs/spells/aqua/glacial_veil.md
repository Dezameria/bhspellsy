# Glacial Veil (ม่านธารน้ำแข็ง)

## ข้อมูลพื้นฐาน (Basic Information)
- **Display Name:** Glacial Veil
- **Registry ID:** `ironspell_more:glacial_veil`
- **School:** Aqua (`TravelopticsSchools.AQUA_RESOURCE`)
- **Rarity:** RARE
- **Max Level:** 1 (ทุก scroll มี 1 เลเวล; scale ต่อได้ด้วยคำสั่ง /cast และปรับแต่งได้ผ่าน ironspell_more-server.toml)
- **Cast Type:** INSTANT
- **Cast Time:** 0 ticks
- **Base Mana Cost:** 50 (+10 ต่อเลเวล)
- **Cooldown:** 20 วินาที
- **Animation:** `SpellAnimations.TOUCH_GROUND_ANIMATION`
- **Sounds:** `SoundRegistry.FROSTWAVE_PREPARE` (เล่นผ่าน `getCastStartSound()` เพียง 1 ครั้งอย่างสะอาดตา ปราศจากเสียงซ้อนทับ)

## กลไกการทำงาน (Spell Mechanics)
1. **Frostwave Shockwave Visual & Effect (รัศมี 10 บล็อก):**
   - ปลดปล่อยคลื่นวงแหวนน้ำแข็งและละอองหิมะพุ่งกระจายรอบตัวผู้ร่ายเป็นวงกลมรัศมี 10 บล็อก (`BlastwaveParticleOptions` + `ShockwaveParticlesPacket`)
   - ศัตรูทุกตัวในระยะรัศมี 10 บล็อกที่มี Line of Sight จะติดดีบัฟ `dungeons_and_combat:frostbite` เป็นเวลา 10 วินาที (200 Ticks)
2. **Ice Spikes Eruption (8 ทิศทาง ระยะ 10 บล็อก):**
   - ปลดปล่อยเสาหนามน้ำแข็ง (`IceSpikeEntity`) พุ่งระเบิดแทงทะลุพื้นดินออกไปรอบทิศทาง 8 แฉก (ทำมุมแฉกละ 45 องศา) ออกไปไกลสูงสุด 10 บล็อก โดยหมุนตามทิศทางที่ผู้ร่ายหันหน้า (ทิศทางแรกจะพุ่งตรงไปข้างหน้าตามทิศทางการมอง/Crosshair ของผู้ร่ายเสมอ)
   - หนามน้ำแข็งในแต่ละแฉกจะหันแกนหน้าและเฉียงพุ่งไปข้างหน้าตามทิศทางการกระจายตัวออกจากจุดศูนย์กลาง (`dirYaw - 45°`)
   - หนามน้ำแข็งจะขยายขนาดขึ้นตามระยะทาง และพุ่งโผล่เป็นระลอกคลื่นไล่ระดับตามความล่าช้า (Wait time) โดยตัวเสาหนามทั้งหมดถูกตั้งเป็น Silent (`setSilent(true)`) เพื่อป้องกันเสียงแทงหนาม 56 เสาซ้อนทับกันจนระเบิดหู
   - ศัตรูที่ถูกหนามน้ำแข็งแทงจะได้รับดาเมจ 20 หน่วย (คำนวณตามพลังเวท Aqua Spell Power) ควบคู่กับดีบัฟ Frostbite

## ค่าตัวเลขและการคำนวณ (Formulas & Scaling)
- **Damage:** 20 Base Damage (+3 ต่อเลเวลเวทมนตร์) สเกลตาม Aqua Spell Power ของผู้ร่าย
- **Debuff:** `dungeons_and_combat:frostbite` นาน 10 วินาที (Amplifier 0)
- **Range / Radius:** 10 บล็อกรอบตัวผู้ร่าย

## การทำงานฝั่ง Server / Client
- **Server:** ตรวจสอบเป้าหมาย, ใส่ MobEffect `frostbite`, เสก `IceSpikeEntity` ทั้ง 8 ทิศทาง และสร้างความเสียหาย
- **Client:** แสดงผลคลื่น Shockwave, ละอองหิมะ และอนิเมชั่นทุบพื้น
