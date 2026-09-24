# Wings of Tempest

## ข้อมูลหลัก

| รายการ | ค่าปัจจุบัน |
| --- | --- |
| Registry ID | `ironspell_more:wings_of_tempest` |
| `getSpellResource()` | `ironspell_more:wings_of_tempest` |
| Class | `spells/nature/WingsofTempestSpell.java` |
| School | Nature |
| Rarity | Rare |
| Max level | 1 (ทุก scroll มี 1 เลเวล; scale ต่อได้ด้วยคำสั่ง /cast และปรับแต่งได้ผ่าน ironspell_more-server.toml) |
| Cast type | Long |
| Cast time | 25 ticks (1.25 วินาที) |
| Cooldown | 22 วินาที |
| Mana | base 40, เพิ่ม 10 ต่อเลเวล |
| Spell power | base 12, เพิ่ม 3 ต่อเลเวล |
| Target type | Self-centered AoE (รอบตัวผู้ร่าย) |
| Radius | `8 + (4 × entityPowerMultiplier)` blocks |
| Duration | `20 × (12 + 2.25 × spellLevel)` ticks (14.25 – 30 วินาที สำหรับเลเวล 1–8) |
| Spawned Entity | `ironspell_more:wing_of_tempest_aoe` (`entity/spells/wings_of_tempest/WingofTempestAoe.java`) |
| Language keys | `spell.ironspell_more.wings_of_tempest` และ `.guide` |

## พฤติกรรมการทำงาน

- เมื่อร่ายเสร็จ สปอว์น `ironspell_more:wing_of_tempest_aoe` ที่ตำแหน่งของผู้ร่าย
- ตั้งค่า owner, radius และ duration ให้ตรงกับค่าที่คำนวณจาก spell level และ spell power
- `WingofTempestAoe` จะติดตามตำแหน่งของผู้ร่ายอย่างต่อเนื่องตลอดระยะเวลาการทำงาน และจะหายไปทันทีหากผู้ร่ายเสียชีวิต
- มีการตรวจสอบ Friendly Fire (`DamageSources.isFriendlyFireBetween`) เพื่อป้องกันไม่ให้ส่งผลกระทบต่อเพื่อนร่วมทีมหรือสัตว์เลี้ยง
- ศัตรูในระยะรัศมีจะถูกแรงลมหมุนเหวี่ยงวนเป็นวงรอบตัวผู้ร่าย (Tangential vortex velocity) ด้วยความเร็วเป้าหมาย 0.35 และ interpolation 0.25 (มีผลกับเป้าหมายที่ต่างระดับแนวตั้งไม่เกิน 6 blocks)
- ศัตรูในระยะจะได้รับผลของ Debuff ทุกๆ 20 ticks:
  - Nausea II (ระยะเวลา 6 วินาที)
  - Blight V (ระยะเวลา 20 วินาที)
  - Mining Fatigue V (ระยะเวลา 20 วินาที)
  - Slowness III (ระยะเวลา 20 วินาที)
- สามารถถูกลบล้างได้ด้วยผลของ Anti-magic (`AntiMagicSusceptible`)

## เสียงและเอฟเฟกต์ (Audio & Visuals)

- **Cast Start**: เสียง `SoundEvents.ELYTRA_FLYING` พร้อมแอนิเมชันร่ายแบบยาว (`ANIMATION_LONG_CAST`)
- **Cast Finish**: เสียง `SoundEvents.PHANTOM_SWOOP`
- **Ambient Loop**: เล่นเสียง `SoundEvents.ELYTRA_FLYING` ทุก 40 ticks ที่ตำแหน่งศูนย์กลางของพายุ
- **Particles**:
  - **Cataclysm Storm Trail**: ใช้อนุภาค `com.github.L_Ender.cataclysm.client.particle.StormParticle$OrbData` สีขาวบริสุทธิ์ (RGB 1.0, 1.0, 1.0) หมุนวนรอบตัวผู้ร่าย:
    - ระหว่างชาร์จร่าย (`onServerCastTick`): ปลดปล่อย Storm Trail หมุนวนกระชับรอบตัวผู้ร่าย
    - เมื่อร่ายเสร็จ (`onCast`): ปลดปล่อย Storm Trail ระเบิดขยายวงรอบตัวผู้ร่าย
    - ระหว่างพายุทำงาน (`WingofTempestAoe.ambientParticles`): สปอว์น Storm Trail โคจรรอบตัวผู้ร่ายตามระดับความสูงและรัศมีต่างๆ อย่างต่อเนื่อง
  - **Ambient Dust/Cloud**: สปอว์นอนุภาค `ParticleTypes.CLOUD` (70%) และ `ParticleTypes.POOF` (30%) หมุนวนรอบศูนย์กลางพายุตามแนวสัมผัส (ฝั่งไคลเอนต์)

## สถานะความสอดคล้อง (Synchronization Status)

- [x] `getSpellResource()` ใช้ namespace `ironspell_more` ตรงกับ Registry และ Language keys
- [x] เพิ่ม Annotation `@AutoSpellConfig` ให้กับ `WingsofTempestSpell`
- [x] `WingsofTempestSpell` สปอว์น `WingofTempestAoe` ของ mod นี้โดยตรง ไม่พึ่งพา BlizzardAoe อีกต่อไป
- [x] ลบไฟล์ส่วนเกิน `entity/spells/BlizzardAoe.java` ออกจากโปรเจกต์
- [x] ปรับ Language guide ให้สอดคล้องกับพฤติกรรมจริงและระยะเวลาที่สเกลตามเลเวล
- [x] เพิ่มระบบ Friendly Fire ป้องกันเพื่อนร่วมทีม
