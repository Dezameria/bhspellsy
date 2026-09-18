# Crimson Rain Bathes Moon (ฝนโลหิตอาบจันทรา)

## ข้อมูลหลัก

| รายการ | ค่าปัจจุบัน |
| --- | --- |
| Registry ID | `ironspell_more:crimson_rain_bathes_moon` |
| `getSpellResource()` | `ironspell_more:crimson_rain_bathes_moon` |
| Class | `spells/aqua/CrimsonRainBathesMoonSpell.java` |
| School | Aqua (`TravelopticsSchools.AQUA_RESOURCE`) |
| Rarity | Epic |
| Max level | 3 |
| Cast type | Continuous (ร่ายค้างต่อเนื่อง) |
| Cast time | 200 ticks (10 วินาที) |
| Cooldown | 60 วินาที |
| Mana cost | base 5, เพิ่ม 3 ต่อเลเวล |
| Spell power | base 1, เพิ่ม 1 ต่อเลเวล |
| Range | 32 บล็อก |
| Storm Radius | `9 + spellLevel` บล็อก (10 – 12 บล็อก) |
| Damage Formula | `5.0 + (spellPower × 5.0)` ต่อหอก |
| Spear Count | 3 เล่มต่อรอบการยิง (ยิงทุกๆ 4 ticks หลังจาก tick ที่ 70) |
| Spawned Entity | `ironspell_more:crimson_spear` (`CrimsonSpearEntity.java`) |
| Language keys | `spell.ironspell_more.crimson_rain_bathes_moon` และ `.guide`, `entity.ironspell_more.crimson_spear` |

## พฤติกรรมการทำงาน (Gameplay & Mechanics)

การร่ายแบบต่อเนื่อง (Continuous Channeling) 200 ticks แบ่งออกเป็น 4 เฟสหลักตามระยะเวลา:

1. **จังหวะเริ่มต้น - ชาร์จสายฟ้าคราม (Ticks 15 – 39)**:
   - สปอว์นวงแหวนสายฟ้าสีฟ้าคราม (`CircleLightningParticle.CircleData(99, 194, 224)`) หมุนวน 4 ทิศทางรอบตัวผู้ร่ายที่ความสูง 16 บล็อก
   - มีการสั่นสะเทือนของหน้าจอระดับเบาที่ tick 25 (`TOFollowingScreenShakeEntity`)

2. **จังหวะพายุหมุนขยายวงกว้างและกระแสลมผลักศัตรู (Ticks 40 – 200)**:
   - ปลดปล่อยพายุหมุนขนาดใหญ่ (`spawnExpandingStormWind`) โดยมีตัวผู้ร่ายเป็นศูนย์กลาง
   - พายุ `StormParticle` สีแดงเลือดจะขยายรัศมีการหมุนวนออกเป็นชั้นๆ (แกนใน, ชั้นกลาง, และขอบนอกกว้างสุด 10–12 บล็อก)
   - ปลดปล่อยกระแสลมหมุนวนและพัดแผ่ขยายออกสู่ภายนอกเป็นวงกว้าง (`ParticleTypes.CLOUD` & `POOF`)
   - มีแรงกระแทกกระแสลมผลักศัตรูรอบตัวให้กระเด็นลอยออกจากรัศมีพายุอย่างต่อเนื่อง (`applyStormKnockback`) โดยปลอดภัยต่อเพื่อนร่วมทีม (Friendly Fire Protection)
   - สปอว์นเมฆฝนดำทมิฬ (`ModParticle.RAIN_CLOUD`) และประกายสายฟ้าสีฟ้าอ่อน (`CircleLightningParticle(143, 241, 215)`) ทั่วบริเวณพายุ
   - มีการสั่นสะเทือนของหน้าจอเพิ่มระดับความแรงที่ tick 40 และ 55

3. **จังหวะกระหน่ำหอกสายฝนโลหิต (Ticks 70 – 200)**:
   - เล่นเสียงร่ายเวทน้ำคำราม (`TravelopticsSounds.AQUA_CAST_2`) ทุกๆ 5 ticks
   - ปลดปล่อยคลื่นแผ่นดินไหวหน้าจอสั่นสะเทือนระดับสูงสุดที่ tick 70
   - เล็งเป้าหมายด้วยระบบ Raycast หากมีศัตรูในระยะสายตา (32 บล็อก) หอกจะคำนวณทิศทางพุ่งนำวิถีดักหน้าตามความเร็วการเคลื่อนที่ของเป้าหมาย (Predictive Leading)
   - สปอว์นหอกโลหิต **Crimson Spear** จำนวน 3 เล่มตกลงมาจากฟ้าเหนือพายุทุกๆ 4 ticks
   - หอกแต่ละเล่มมีคุณสมบัติกระดอนพื้นได้ `2 + spellLevel` ครั้ง และเร่งความเร็วพุ่งเข้าหาเป้าหมาย

4. **การสร้างความเสียหายและดีบัฟ**:
   - เมื่อหอกพุ่งชนศัตรู จะสร้างความเสียหายเวทสาย Aqua สเกลตามพลังเวท
   - ศัตรูที่โดนโจมตีจะติดสถานะ **Wet** (`TravelopticsEffects.WET`) นาน 2 วินาทีขึ้นไป และมีโอกาสสุ่มเพิ่มระยะเวลาหรือระดับขั้น (Amplifier) สูงสุดถึงระดับ 10

## เสียงและเอฟเฟกต์ (Audio & Visuals)

- **Cast Start Sound**: `SoundEvents.LIGHTNING_BOLT_THUNDER` (ไม่มีแอนิเมชันท่าทางการร่าย: `AnimationHolder.none()`)
- **Barrage Sound**: `TravelopticsSounds.AQUA_CAST_2`
- **Particles**:
  - **Storm Trail Vortex**: ใช้ `StormParticle.OrbData` ปรับเฉดสีแดงโลหิต (`r: 0.95F, g: 0.08F, b: 0.15F` และ `r: 1.0F, g: 0.18F, b: 0.22F`) โคจรรอบตัวผู้ร่ายเป็นวงพายุหมุนขนาดใหญ่หลายชั้น
  - **Expanding Wind Gusts**: อนุภาคกระแสลม `ParticleTypes.CLOUD` และ `POOF` พัดแผ่รัศมีกระจายออกรอบทิศทาง
  - **Circle Lightning**: อนุภาคสายฟ้าสีฟ้าครามดั้งเดิม (`RGB: 99, 194, 224` และ `RGB: 143, 241, 215`)
  - **Spear Trail**: หอก Crimson Spear พ่นละออง `StormParticle` สีแดงเลือดตามวิถีพุ่ง
- **Entity Model & Render**:
  - `CrimsonSpearEntity` แสดงผลผ่าน `CrimsonSpearRenderer` โดยใช้โมเดล `Elemental_Spear_Model` ของ Cataclysm เคลือบฟิลเตอร์สีแดงวิญญาณเรืองแสง (`Red Ghost Tint: 1.0F, 0.12F, 0.18F`)

## สถานะความสอดคล้อง (Synchronization Status)

- [x] ใช้ School `TravelopticsSchools.AQUA_RESOURCE`
- [x] สืบทอดจาก `AbstractUniqueSpell` ของ Traveloptics
- [x] ใช้ namespace `ironspell_more` สำหรับ Registry ID และ Asset Icon
- [x] แปลงชื่อ SRG / Deobf ทั้งหมดสู่ Parchment/Mojmap
- [x] คัสตอมหอกเป็น `CrimsonSpearEntity` พร้อมเรนเดอร์และพาร์ทิเคิลสีแดงเลือด
- [x] ลงทะเบียนใน `SpellRegistry`, `EntityRegistry`, `IronSpellMoreClient` และ `en_us.json`
