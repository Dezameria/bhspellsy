# รายการเวททั้งหมด

รายการนี้ซิงก์กับ `src/main/java/io/redspace/ironspell_more/registry/SpellRegistry.java` ปัจจุบันมี **20 เวทใน 6 สายเวท** รายละเอียดค่าพลัง ระยะเวลา damage, cooldown, entity, asset และ edge case ให้ยึดเอกสารรายสกิลที่ลิงก์ไว้

> **หมายเหตุระบบ Scroll และ Config:**
> - สกิลทุกสกิลในม็อดถูกกำหนดให้มี Scroll เพียงระดับเดียวคือ **Level 1** (`maxLevel = 1`) ทำให้ในเมนูสร้างสรรค์ (Creative Tab), JEI, ดรอปจากมอนสเตอร์ หรือกล่องสมบัติ จะพบเพียงคัมภีร์ระดับ 1 เท่านั้น
> - ตัวคูณความสามารถตามเลเวล (Level Scaling) ยังคงทำงานได้เต็มรูปแบบผ่านคำสั่งร่ายเวท: `/cast <ผู้เล่น> <ชื่อเวท> <เลเวล>`
> - ค่าสเตตัสทั้งหมด (Base Damage, Damage Per Level, Base Mana, Mana Per Level, Cooldown) สามารถปรับแต่งได้อย่างสะดวกรวดเร็วผ่านไฟล์คอนฟิกเซิร์ฟเวอร์ `config/ironspell_more-server.toml` หรือผ่านค่าคงที่ `SPELL TUNING CONSTANTS` ด้านบนสุดของแต่ละคลาสเวท

## Fire

| เวท | Registry ID | บทบาทหลัก | Specification |
| --- | --- | --- | --- |
| Blazing Chakra | `ironspell_more:blazing_chakra` | กระแทกพื้นและปล่อยคลื่นเพลิงหลายชั้นพร้อม damage falloff | [เปิดเอกสาร](spells/fire/blazing_chakra.md) |
| Spin Strike | `ironspell_more:spin_strike` | พุ่งหมุนไปข้างหน้าและโจมตีเป้าหมายตามเส้นทาง | [เปิดเอกสาร](spells/fire/spin_strike.md) |
| Pure White Flame Burst | `ironspell_more:pure_white_flame_burst` | จับเป้าหมายระยะประชิด ทุ่ม และสร้างระเบิด/คลื่นเพลิงขาว | [เปิดเอกสาร](spells/fire/pure_white_flame_burst.md) |
| Gale Drive | `ironspell_more:gale_drive` | พุ่งทะลวงและสร้าง vortex เมื่อชนเป้าหมาย | [เปิดเอกสาร](spells/fire/gale_drive.md) |
| Resonant Knell | `ironspell_more:resonant_knell` | Recast 6 ครั้งเพื่อสลับโดมป้องกันกับ shockwave รัศมี 15/20/30 | [เปิดเอกสาร](spells/fire/resonant_knell.md) |
| Crimson Thornbind | `ironspell_more:crimson_thornbind` | ปล่อยเถาวัลย์แนวนอนจากมือ จับเป้าหมายแรกเพียงตัวเดียวด้วยรากแนวตั้ง และเสริม Rend Damage | [เปิดเอกสาร](spells/fire/crimson_thornbind.md) |

## Lightning

| เวท | Registry ID | บทบาทหลัก | Specification |
| --- | --- | --- | --- |
| Lightning Strike | `ironspell_more:lightning_strike` | พุ่ง/วาร์ปตาม raycast และโจมตีเป้าหมายด้วยสายฟ้า | [เปิดเอกสาร](spells/lightning/lightning_strike.md) |
| Thunder Step | `ironspell_more:thunder_step` | Teleport ไปข้างหน้าและทำ damage ตลอดเส้นทาง | [เปิดเอกสาร](spells/lightning/thunder_step.md) |

## Nature

| เวท | Registry ID | บทบาทหลัก | Specification |
| --- | --- | --- | --- |
| Wings of Tempest | `ironspell_more:wings_of_tempest` | AoE ติดตามผู้ร่าย หมุนศัตรูรอบพายุและใส่ debuff | [เปิดเอกสาร](spells/nature/wings_of_tempest.md) |
| Venomous Blossomfall | `ironspell_more:venomous_blossomfall` | ชาร์จเข็มพิษสามระดับและยิงไปยัง crosshair | [เปิดเอกสาร](spells/nature/venomous_blossomfall.md) |
| Gale Piercer | `ironspell_more:gale_piercer` | ชาร์จลูกศรลมแบบ homing; full charge ทะลุกำแพงและติดสถานะ | [เปิดเอกสาร](spells/nature/gale_piercer.md) |
| Rapturous Bloom | `ironspell_more:rapturous_bloom` | สร้างดอกไม้พิษใต้เป้าหมาย ใส่สถานะเป็นระยะ และระเบิดกลีบดอกเมื่อครบเวลา | [เปิดเอกสาร](spells/nature/rapturous_bloom.md) |

## Aqua

| เวท | Registry ID | บทบาทหลัก | Specification |
| --- | --- | --- | --- |
| Crimson Rain Bathes Moon | `ironspell_more:crimson_rain_bathes_moon` | Channel พายุสายฟ้าครามและฝนหอกสีชาด | [เปิดเอกสาร](spells/aqua/crimson_rain_bathes_moon.md) |
| Glacial Veil | `ironspell_more:glacial_veil` | คลื่นน้ำแข็งและหนามน้ำแข็งแปดทิศรอบผู้ร่าย | [เปิดเอกสาร](spells/aqua/glacial_veil.md) |
| Glacial Firmament | `ironspell_more:glacial_firmament` | สร้าง ice domain และวงหนาม/สุสานน้ำแข็งหลายชั้น | [เปิดเอกสาร](spells/aqua/glacial_firmament.md) |

## Gold

| เวท | Registry ID | บทบาทหลัก | Specification |
| --- | --- | --- | --- |
| Shackle of Fear | `ironspell_more:shackle_of_fear` | ยิง projectile เพื่อสร้างโซ่หลายเส้นตรึงเป้าหมาย | [เปิดเอกสาร](spells/gold/shackle_of_fear.md) |
| Hymn of Purification | `ironspell_more:hymn_of_purification` | บรรเลงเพลงทองคำ 20 วินาที ฟื้นฟูเลือดและล้างสถานะผิดปกติทั้งหมด | [เปิดเอกสาร](spells/gold/hymn_of_purification.md) |
| Gilded Hare | `ironspell_more:gilded_hare` | บัฟตนเอง 2 นาที (ความเร็ว/กระโดด/ตีเร็ว/เกราะทอง/พลัง) เตะศัตรูสะสมคอมโบติดริบบิ้นสโลว์ และเตะครบ 5 ครั้งตรึงดักแด้สตั๊นพร้อมหูกระต่าย | [เปิดเอกสาร](spells/gold/gilded_hare.md) |

## Ground

| เวท | Registry ID | บทบาทหลัก | Specification |
| --- | --- | --- | --- |
| Tigershade Terrabreak | `ironspell_more:tigershade_terrabreak` | Mark เป้าหมาย รับบัฟ stance และ recast เพื่อพุ่งทุบทำ damage ในระยะ 5 บล็อก | [เปิดเอกสาร](spells/ground/tigershade_terrabreak.md) |
| Jade Aura | `ironspell_more:jade_aura` | บัฟตัวเองหรือเพื่อนร่วมทีม เพิ่มพลังโจมตี ความเร็ว และพลังเวท พร้อมคลื่นออร่าหยก | [เปิดเอกสาร](spells/ground/jade_aura.md) |

## ตรวจความสอดคล้อง

- จำนวนในเอกสารนี้ต้องเท่ากับจำนวน `RegistryObject<AbstractSpell>` ใน `SpellRegistry`
- ทุก registry path ต้องมีเอกสารหนึ่งไฟล์ใน `docs/spells/<school>/`
- ชื่อที่แสดงควรตรงกับ `assets/ironspell_more/lang/en_us.json`
- ค่ารายละเอียดต้องแก้ในเอกสารรายสกิลก่อน แล้วจึงแก้คำสรุปในไฟล์นี้เมื่อจำเป็น
