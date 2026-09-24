# รายงานวิเคราะห์ความเสี่ยงการ Crash และปัญหาในระบบ Multiplayer
**โครงการ:** IronSpell More (`ironspell_more`)  
**Minecraft Version:** 1.20.1 | **Forge:** 47.4.20 | **Iron's Spells:** 1.20.1-3.16.1  
**วันที่วิเคราะห์:** 24 กันยายน 2026  

---

## สารบัญ
1. [ภาพรวม (Executive Summary)](#1-ภาพรวม-executive-summary)
2. [ตารางสรุปความรุนแรงของปัญหา (Risk & Severity Matrix)](#2-ตารางสรุปความรุนแรงของปัญหา-risk--severity-matrix)
3. [หมวดที่ 1: จุดเสี่ยงเกิดการ Crash รุนแรง (Hard Crash Risks)](#3-หมวดที่-1-จุดเสี่ยงเกิดการ-crash-รุนแรง-hard-crash-risks)
   - [1.1 BlazingChakraSpell เรียกใช้คลาส Epic Fight โดยตรง](#11-blazingchakraspell-เรียกใช้คลาส-epic-fight-โดยตรง)
   - [1.2 GlacialVeilSpell และ CrimsonSpearEntity ผูกกับม็อดภายนอกโดยไม่มีการประกาศ Dependency](#12-glacialveilspell-และ-crimsonspearentity-ผูกกับม็อดภายนอกโดยไม่มีการประกาศ-dependency)
   - [1.3 โค้ดฝั่ง Server ทำงานใน Package `client.*`](#13-โค้ดฝั่ง-server-ทำงานใน-package-client)
4. [หมวดที่ 2: บั๊กระบบตรรกะใน Multiplayer (Logic Inversion: บัฟศัตรู)](#4-หมวดที่-2-บั๊กระบบตรรกะใน-multiplayer-logic-inversion-บัฟศัตรู)
   - [2.1 บั๊กกลับค่าการตรวจสอบพันธมิตรใน Hymn of Purification และ Resonant Knell](#21-บั๊กกลับค่าการตรวจสอบพันธมิตรใน-hymn-of-purification-และ-resonant-knell)
5. [หมวดที่ 3: ปัญหา Network, Packet Flooding และ Rubberbanding](#5-หมวดที่-3-ปัญหา-network-packet-flooding-และ-rubberbanding)
   - [3.1 Packet Explosion ใน LightningStrikeSpell](#31-packet-explosion-ใน-lightningstrikespell)
   - [3.2 Force Teleport ผู้เล่นทุก Tick ใน GaleDriveVortexEntity](#32-force-teleport-ผู้เล่นทุก-tick-ใน-galedrivevortexentity)
   - [3.3 ขาดการสั่ง `stopRiding()` ก่อน Teleport ใน LightningStrikeSpell](#33-ขาดการสั่ง-stopriding-ก่อน-teleport-ใน-lightningstrikespell)
6. [หมวดที่ 4: ปัญหา State Sync, Desync และ Race Condition](#6-หมวดที่-4-ปัญหา-state-sync-desync-และ-race-condition)
   - [4.1 สายโซ่หลุดการเชื่อมต่อและลอยขึ้นฟ้าบน Client (GoldChain)](#41-สายโซ่หลุดการเชื่อมต่อและลอยขึ้นฟ้าบน-client-goldchain)
   - [4.2 Race Condition การแย่ง Combo ใน Gilded Hare เมื่อรุมตีเป้าหมายเดียวกัน](#42-race-condition-การแย่ง-combo-ใน-gilded-hare-เมื่อรุมตีเป้าหมายเดียวกัน)
   - [4.3 กล้องหลุดการร่ายง่ายเกินไปจาก Network Latency (Hymn of Purification)](#43-กล้องหลุดการร่ายง่ายเกินไปจาก-network-latency-hymn-of-purification)
   - [4.4 การปะปนของ Entity Client-Server ใน Integrated Server (ResonantKnellDomeAoe)](#44-การปะปนของ-entity-client-server-ใน-integrated-server-resonantknelldomeaoe)
7. [แนวทางและตัวอย่างโค้ดแก้ไข (Actionable Fixes)](#7-แนวทางและตัวอย่างโค้ดแก้ไข-actionable-fixes)

---

## 1. ภาพรวม (Executive Summary)

จากการตรวจสอบ Source Code เชิงลึกทุกโมดูล พบว่าระบบภาพรวมของ IronSpell More มีการออกแบบ Effect และ Visuals ที่ละเอียดและมีเอกลักษณ์สูง แต่ยังมีจุดบกพร่องสำคัญแบ่งออกเป็น **4 มิติหลัก** ที่ส่งผลกระทบต่อความเสถียรของเกม:
1. **ความเสถียรของ Class Loading:** มีการ Hard-link คลาสจาก Optional Dependencies ส่งผลให้เกม **แครชทันทีตอนเปิดเซิร์ฟเวอร์ (Startup Crash)** หากไม่มีม็อดเสริมนั้นๆ ติดตั้งอยู่
2. **ความถูกต้องของตรรกะ Multiplayer (PvP/PvE):** มีการใช้ Boolean Negation (`!`) ผิดพลาด ทำให้เวทประเภทช่วยเหลือ/ป้องกัน กลายเป็นการ **ช่วยเหลือศัตรูและปฏิเสธเพื่อนร่วมทีม**
3. **ประสิทธิภาพของเครือข่าย (Networking):** มีการยิง Packet แบบ Unbounded Loop หาผู้เล่นทั้งมิติ และการใช้คำสั่ง `teleportTo` บน Server ทุก Tick ซึ่งเสี่ยงต่อการโดนระบบ Anti-Cheat เตะ
4. **ความแม่นยำในการ Sync สถานะ:** มีปัญหาด้าน Entity Lifecycle และ Client-Server State Bleeding

---

## 2. ตารางสรุปความรุนแรงของปัญหา (Risk & Severity Matrix)

| รหัส | หัวข้อปัญหา | ระดับความรุนแรง | สถานะ | สถานการณ์ที่เกิด | ผลกระทบ |
| :--- | :--- | :---: | :---: | :--- | :--- |
| **CR-01** | `BlazingChakraSpell` ผูกคลาส Epic Fight โดยตรง | **CRITICAL (แครช)** | ⏳ รอการแก้ | เปิดเกม/เซิร์ฟเวอร์โดยไม่มี Epic Fight | `NoClassDefFoundError` เกมแครชทันทีตอน Boot |
| **CR-02** | `GlacialVeilSpell` / `CrimsonSpear` พึ่งพา Traveloptics / Cataclysm | **CRITICAL (แครช)** | ⏳ รอการแก้ | รันบนเซิร์ฟเวอร์ที่ไม่มีม็อด Traveloptics / Cataclysm | `NoClassDefFoundError` เกมแครชทันทีตอน Boot |
| **LG-01** | ตรรกะตรวจ Ally กลับด้านใน `Hymn of Purification` | **HIGH (บั๊กร้ายแรง)** | ✅ **แก้ไขแล้ว** | เซิร์ฟเวอร์ Multiplayer ที่เปิดระบบ PvP | ฮีลและล้างดีบัฟให้ศัตรู แต่เพื่อนร่วมทีมไม่ได้รับผล |
| **LG-02** | ตรรกะตรวจ Ally กลับด้านใน `Resonant Knell` | **HIGH (บั๊กร้ายแรง)** | ✅ **แก้ไขแล้ว** | เซิร์ฟเวอร์ Multiplayer ที่เปิดระบบ PvP | มอบบาเรียและดาเมจสะท้อนให้ศัตรู แทนที่จะให้เพื่อน |
| **NET-01** | Packet Explosion ใน `LightningStrikeSpell` | **HIGH (Lag/เตะ)** | ⏳ รอการแก้ | เซิร์ฟเวอร์ที่มีผู้เล่นออนไลน์หลายคน | เกิด Network Spike ส่งแพ็กเก็ตหลายร้อยถึงพันชุดต่อการร่าย 1 ครั้ง |
| **NET-02** | Force Teleport ทุก Tick ใน `GaleDriveVortexEntity` | **HIGH (Lag/เตะ)** | ✅ **แก้ไขแล้ว** | ดูดผู้เล่นคนอื่นใน Multiplayer | Rubberbanding รุนแรง, โดนเตะข้อหา Flying / Moved Wrongly |
| **SYNC-01** | โซ่ลอยชี้ฟ้าใน Client (`GoldChain`) | **MEDIUM (การแสดงผล)** | ⏳ รอการแก้ | เซิร์ฟเวอร์ Multiplayer ที่มี Packet Latency | Client ไม่สามารถค้นหา Target Entity ได้ โซ่จึงไม่เชื่อมต่อกับตัวเป้าหมาย |
| **SYNC-02** | แย่ง Combo ใน `Gilded Hare` | **MEDIUM (เกมเพลย์)** | ⏳ รอการแก้ | ผู้เล่นมากกว่า 1 คนรุมตีมอนสเตอร์/บอสตัวเดียวกัน | รีเซ็ตคอมโบของกันและกัน ทำให้ไม่มีใครสามารถทำครบ 5 ฮิตได้ |
| **NET-03** | ขาด `stopRiding()` ใน `LightningStrikeSpell` | **LOW (Desync)** | ⏳ รอการแก้ | ร่ายขณะนั่งเรือ ขี่ม้า หรือถูกจับยึด | ตำแหน่งผู้เล่นและยานพาหนะเกิดการ Desync / ติดบั๊กขยับไม่ได้ |
| **GAME-01**| มุมกล้องหลุดการร่ายง่ายเกินไปใน `Hymn of Purification` | **LOW (เกมเพลย์)** | ⏳ รอการแก้ | เซิร์ฟเวอร์ที่มีความหน่วง (Ping) หรือเมาส์สั่น | หลุดร่ายโดยไม่ตั้งใจและโดนดีบัฟเวียนหัว (Nausea) ทันที |

---

## 3. หมวดที่ 1: จุดเสี่ยงเกิดการ Crash รุนแรง (Hard Crash Risks)

### 1.1 BlazingChakraSpell เรียกใช้คลาส Epic Fight โดยตรง
* **ไฟล์:** [`BlazingChakraSpell.java`](file:///d:/Minecraft/Dev/ironspell_more/ironspell_more/src/main/java/io/redspace/ironspell_more/spells/fire/BlazingChakraSpell.java#L6)
* **บรรทัดที่เกิดปัญหา:**
  ```java
  import io.redspace.ironspell_more.compat.epicfight.skills.blazing_chakra.BlazingChakraVfx;
  ...
  public float getRadius(int spellLevel, LivingEntity caster) {
      return BlazingChakraVfx.MAX_RANGE; // บรรทัดที่ 87
  }
  ...
  float closeRadius = BlazingChakraVfx.CLOSE_RANGE; // บรรทัดที่ 115
  ```
* **สาเหตุ:** แม้ว่าใน [`mods.toml`](file:///d:/Minecraft/Dev/ironspell_more/ironspell_more/src/main/resources/META-INF/mods.toml#L73) จะตั้งค่า `epicfight` เป็น `mandatory = false` และมี [`CompatBootstrap`](file:///d:/Minecraft/Dev/ironspell_more/ironspell_more/src/main/java/io/redspace/ironspell_more/compat/CompatBootstrap.java) รองรับแบบ Safe Reflection/Facade แต่ในตัว `BlazingChakraSpell` ดันไปเรียกค่าคงที่จาก `BlazingChakraVfx` โดยตรง ซึ่งคลาส `BlazingChakraVfx` มีการ `import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch`
* **ผลลัพธ์:** เมื่อ JVM พยายาม Load คลาส `BlazingChakraSpell` ระหว่าง Register เวทมนตร์ หากเครื่องนั้นไม่มีม็อด Epic Fight ติดตั้งอยู่ ClassLoader จะพ่น `NoClassDefFoundError: yesman/epicfight/...` ส่งผลให้ **เกมแครชทันทีตั้งแต่เริ่มเปิด (Startup Crash)**

### 1.2 GlacialVeilSpell และ CrimsonSpearEntity ผูกกับม็อดภายนอกโดยไม่มีการประกาศ Dependency
* **ไฟล์:** 
  - [`GlacialVeilSpell.java`](file:///d:/Minecraft/Dev/ironspell_more/ironspell_more/src/main/java/io/redspace/ironspell_more/spells/aqua/GlacialVeilSpell.java#L7)
  - [`CrimsonSpearEntity.java`](file:///d:/Minecraft/Dev/ironspell_more/ironspell_more/src/main/java/io/redspace/ironspell_more/entity/spells/crimson_rain_bathes_moon/CrimsonSpearEntity.java#L3-L6)
* **บรรทัดที่เกิดปัญหา:**
  ```java
  // GlacialVeilSpell.java
  import com.gametechbc.traveloptics.api.init.TravelopticsSchools;
  ...
  .setSchoolResource(TravelopticsSchools.AQUA_RESOURCE)

  // CrimsonSpearEntity.java
  import com.gametechbc.traveloptics.entity.extended_projectiles.ExtendedWaterSpearEntity;
  import com.github.L_Ender.cataclysm.client.particle.StormParticle;
  ```
* **สาเหตุ:** มีการ Hard-import คลาสจากม็อด `traveloptics` และ `lendercataclysm` โดยที่ใน [`mods.toml`](file:///d:/Minecraft/Dev/ironspell_more/ironspell_more/src/main/resources/META-INF/mods.toml) ไม่ได้ระบุ Dependencies สองม็อดนี้ไว้เลย
* **ผลลัพธ์:** หากนำไฟล์ `.jar` ของม็อดไปลงใน Modpack หรือ Dedicated Server ทั่วไปที่ไม่มี Traveloptics หรือ Cataclysm เซิร์ฟเวอร์จะแครชขณะ Initialize ม็อดทันที

### 1.3 โค้ดฝั่ง Server ทำงานใน Package `client.*`
* **ไฟล์:** [`GildedHareVfx.java`](file:///d:/Minecraft/Dev/ironspell_more/ironspell_more/src/main/java/io/redspace/ironspell_more/client/particle/GildedHareVfx.java) และ [`JadeAuraVfx.java`](file:///d:/Minecraft/Dev/ironspell_more/ironspell_more/src/main/java/io/redspace/ironspell_more/client/particle/JadeAuraVfx.java)
* **สาเหตุ:** คลาสทั้งสองอยู่ใน `io.redspace.ironspell_more.client.particle` แต่มีเมธอดฝั่ง Server เช่น `spawnKickImpactVfx(ServerLevel, ...)` และ `spawnCastBurst(ServerLevel, ...)` ซึ่งถูกเรียกใช้จาก Server Event Listener
* **ผลลัพธ์:** มีความเสี่ยงสูงมากที่หากในอนาคตมีผู้พัฒนาเพิ่มคลาสเฉพาะ Client เช่น `Minecraft.getInstance()` เข้าไปในไฟล์เหล่านี้ จะทำให้ Dedicated Server แครชทันทีเนื่องจาก ClassNotFound ใน Dedicated Server Environment

---

## 4. หมวดที่ 2: บั๊กระบบตรรกะใน Multiplayer (Logic Inversion: บัฟศัตรู)

### 2.1 บั๊กกลับค่าการตรวจสอบพันธมิตรใน Hymn of Purification และ Resonant Knell
* **ไฟล์:** 
  - [`HymnofPurificationSpell.java`](file:///d:/Minecraft/Dev/ironspell_more/ironspell_more/src/main/java/io/redspace/ironspell_more/spells/gold/HymnofPurificationSpell.java#L130)
  - [`ResonantKnellSpell.java`](file:///d:/Minecraft/Dev/ironspell_more/ironspell_more/src/main/java/io/redspace/ironspell_more/spells/fire/ResonantKnellSpell.java#L250)
* **โค้ดที่เป็นปัญหา:**
  ```java
  public static boolean isAlly(LivingEntity caster, LivingEntity target) {
      if (target == caster) return true;
      if (!target.isAlive() || target.isSpectator()) return false;
      if (target.isAlliedTo(caster)) return true;
      if (caster instanceof Player && target instanceof Player) {
          return !DamageSources.isFriendlyFireBetween(caster, target); // <--- ผิดพลาด! มีเครื่องหมาย !
      }
      ...
  }
  ```
* **การทำงานของ Iron's Spells:** 
  ฟังก์ชัน `DamageSources.isFriendlyFireBetween(a, b)` จะคืนค่า `true` เมื่อ **ห้ามตีกันเอง (เป็นพันธมิตรกัน/อยู่ทีมเดียวกันและปิด PvP)** และจะคืนค่า `false` เมื่อ **ตีกันได้ (เป็นศัตรูกัน/เปิด PvP)**
* **ผลกระทบใน Multiplayer เมื่อมีเครื่องหมาย `!`:**
  1. **กรณีเป็นศัตรูกัน (เปิด PvP):** ฟังก์ชันคืนค่า `false` แต่พอเจอ `!` กลายเป็น `true` ทำให้ **เวทเข้าใจว่าศัตรูคือเพื่อน** ส่งผลให้เวท `Hymn of Purification` ทำการฮีลเลือดและล้างดีบัฟให้ศัตรู! และ `Resonant Knell` จะมอบบาเรียไฟป้องกันดาเมจให้ศัตรู!
  2. **กรณีเป็นเพื่อนร่วมทีม (ปิด Friendly Fire):** ฟังก์ชันคืนค่า `true` แต่พอเจอ `!` กลายเป็น `false` ทำให้ **เพื่อนในทีมไม่ได้รับฮีล ไม่ได้รับการล้างดีบัฟ และไม่ได้รับบาเรีย**
  *(หมายเหตุ: ใน [`JadeAuraSpell.java#L151`](file:///d:/Minecraft/Dev/ironspell_more/ironspell_more/src/main/java/io/redspace/ironspell_more/spells/ground/JadeAuraSpell.java#L151) เขียนไว้ถูกต้องแล้วคือ `return DamageSources.isFriendlyFireBetween(caster, target);` โดยไม่มี `!`)*

> [!NOTE]
> **สถานะการแก้ไข (RESOLVED):** ได้ทำการตัดเครื่องหมาย `!` ออกจาก `HymnofPurificationSpell.java` และ `ResonantKnellSpell.java` เรียบร้อยแล้ว ทำให้ระบบตรวจสอบพันธมิตรและศัตรูทำงานอย่างถูกต้องสมบูรณ์ในเซิร์ฟเวอร์ Multiplayer

---

## 5. หมวดที่ 3: ปัญหา Network, Packet Flooding และ Rubberbanding

### 3.1 Packet Explosion ใน LightningStrikeSpell
* **ไฟล์:** [`LightningStrikeSpell.java`](file:///d:/Minecraft/Dev/ironspell_more/ironspell_more/src/main/java/io/redspace/ironspell_more/spells/lightning/LightningStrikeSpell.java#L155-L171)
* **โค้ดที่เป็นปัญหา:**
  ```java
  int particleCount = 35;
  for (int i = 0; i < particleCount; i++) {
      ...
      for (ServerPlayer otherPlayer : serverLevel.players()) {
          serverLevel.sendParticles(otherPlayer, new ZapParticleOptionCustom(sparkDest, zapLength), true,
                  targetX, targetY, targetZ, 1, 0, 0, 0, 0);
      }
  }
  ```
* **สาเหตุ:** การครอบ Loop 35 รอบ ด้วย Loop ผู้เล่นทุกคนในมิติ (`serverLevel.players()`)
* **ผลกระทบใน Multiplayer:** 
  - หากในเซิร์ฟเวอร์มีผู้เล่น 20 คนในมิตินั้น การร่ายเพียง 1 ครั้งจะส่ง Custom Packet ออกไป **700 แพ็กเก็ต** พร้อมกันใน 1 Tick
  - แพ็กเก็ตจะถูกส่งไปหาผู้เล่นที่อยู่ห่างออกไปนับหมื่นบล็อกที่ไม่ได้มองเห็นเอฟเฟกต์ด้วย
  - ทำให้เกิดอาการ Network Spike, TPS ตก และผู้เล่นที่เชื่อมต่อด้วยอินเทอร์เน็ตความเร็วต่ำอาจหลุดออกจากเซิร์ฟเวอร์ทันที (Timed Out)

### 3.2 Force Teleport ผู้เล่นทุก Tick ใน GaleDriveVortexEntity
* **ไฟล์:** [`GaleDriveVortexEntity.java`](file:///d:/Minecraft/Dev/ironspell_more/ironspell_more/src/main/java/io/redspace/ironspell_more/entity/spells/gale_drive/GaleDriveVortexEntity.java#L82)
* **โค้ดที่เป็นปัญหา:**
  ```java
  if (this.capturedTarget != null && this.capturedTarget.isAlive()) {
      double targetY = this.groundY + 6.0;
      this.capturedTarget.teleportTo(this.getX(), targetY, this.getZ());
      this.capturedTarget.setDeltaMovement(0, 0, 0);
      ...
  ```
* **สาเหตุ:** การสั่ง `teleportTo(...)` บน Server ทุก Tick ตลอด 140 Ticks (7 วินาที)
* **ผลกระทบใน Multiplayer:**
  1. การ Teleport บน Server จะส่ง Position Correction Packet ไปยัง Client 20 ครั้ง/วินาที ทำให้เกิดการต่อสู้ระหว่าง Client Prediction กับ Server State เกิดอาการ **Rubberbanding (กระตุกสั่นอย่างรุนแรง)**
  2. เสี่ยงที่จะถูกเซิร์ฟเวอร์เตะด้วยข้อหาของระบบ Vanilla: *"Player moved wrongly!"* หรือ *"Flying is not enabled on this server"*
  3. หากผู้เล่นเป้าหมาย **Disconnect ออกจากเกมระหว่างโดนดูด** ตัวแปร `isAlive()` จะยังคงเป็น `true` แต่ Entity ถูก Remove ออกจาก World ไปแล้ว ทำให้เซิร์ฟเวอร์ยังคงรันคำสั่ง `teleportTo` บน Removed Entity ต่อไปจนครบเวลา

> [!NOTE]
> **สถานะการแก้ไข (RESOLVED):** ได้ทำการปรับปรุงโค้ดใน `GaleDriveVortexEntity.java` เรียบร้อยแล้ว:
> 1. เพิ่มการตรวจเช็ค `!this.capturedTarget.isRemoved()` ป้องกันกรณีเป้าหมายถูกลบออกจาก World
> 2. สลับมาใช้แรงส่ง Motion (`setDeltaMovement` สเกล 0.35 เข้าหาจุดกึ่งกลาง) ร่วมกับ `hurtMarked = true` สำหรับ `ServerPlayer` แทนการสั่ง `teleportTo` ทุก Tick แก้ไขปัญหา Rubberbanding และป้องกันระบบ Anti-Cheat เตะได้อย่างสมบูรณ์

### 3.3 ขาดการสั่ง `stopRiding()` ก่อน Teleport ใน LightningStrikeSpell
* **ไฟล์:** [`LightningStrikeSpell.java`](file:///d:/Minecraft/Dev/ironspell_more/ironspell_more/src/main/java/io/redspace/ironspell_more/spells/lightning/LightningStrikeSpell.java#L198)
* **สาเหตุ:** เรียก `entity.teleportTo(destPos.x, destPos.y, destPos.z);` โดยไม่ได้ตรวจสอบว่าผู้เล่นกำลังขี่ Entity อื่นอยู่หรือไม่ (เช่น นั่งเรือ ขี่ม้า หรือถูกจับด้วยสกิลอื่น) ต่างจาก [`ThunderStepSpell.java#L112`](file:///d:/Minecraft/Dev/ironspell_more/ironspell_more/src/main/java/io/redspace/ironspell_more/spells/lightning/ThunderStepSpell.java#L112) ที่มี `if (entity.isPassenger()) entity.stopRiding();`
* **ผลกระทบ:** ยานพาหนะหรือผู้เล่นอาจเกิดการ Desync ขยับตัวไม่ได้ หรือหลุดจากแผนที่

---

## 6. หมวดที่ 4: ปัญหา State Sync, Desync และ Race Condition

### 4.1 สายโซ่หลุดการเชื่อมต่อและลอยขึ้นฟ้าบน Client (GoldChain)
* **ไฟล์:** [`GoldChain.java`](file:///d:/Minecraft/Dev/ironspell_more/ironspell_more/src/main/java/io/redspace/ironspell_more/entity/spells/gold_chain/GoldChain.java#L108-L115) และ [`readSpawnData`](file:///d:/Minecraft/Dev/ironspell_more/ironspell_more/src/main/java/io/redspace/ironspell_more/entity/spells/gold_chain/GoldChain.java#L322-L326)
* **สาเหตุ:**
  1. ใน `readSpawnData` ฝั่ง Client อ่านเฉพาะ `Entity ID` (Integer) จาก Packet หากจังหวะนั้น Client ยังโหลด Entity ของเหยื่อเข้าสู่ World ไม่ทัน `level().getEntity(id)` จะได้ `null`
  2. ในเมธอด `getVictim()` โค้ดตรวจสอบเฉพาะ:
     ```java
     else if (this.victimUUID != null && this.level() instanceof ServerLevel serverLevel)
     ```
     ซึ่งฝั่ง Client มีระดับเลเวลเป็น `ClientLevel` เสมอ ทำให้ไม่สามารถค้นหาเหยื่อด้วย UUID บน Client ได้อีกเลย
* **ผลกระทบ:** บนหน้าจอ Client สายโซ่จะไม่ยอมเชื่อมต่อกับตัวเหยื่อ และจะแสดงผลชี้ขึ้นฟ้าตรงๆ ที่พิกัด `(0, 1, 0)` ไปจนกว่าโซ่จะหมดเวลา

### 4.2 Race Condition การแย่ง Combo ใน Gilded Hare เมื่อรุมตีเป้าหมายเดียวกัน
* **ไฟล์:** [`GildedHareCombatEvents.java`](file:///d:/Minecraft/Dev/ironspell_more/ironspell_more/src/main/java/io/redspace/ironspell_more/event/GildedHareCombatEvents.java#L70-L80)
* **สาเหตุ:** สถานะ Combo ผูกไว้กับตัวเหยื่อผ่าน NBT Tag เพียงชุดเดียว (`OWNER_UUID_TAG`)
* **ผลกระทบ:**
  - หากผู้เล่น A กำลังทำคอมโบถึงฮิตที่ 3 แล้วผู้เล่น B (ที่มีบัฟ Gilded Hare เช่นกัน) เข้ามาช่วยตีมอนสเตอร์ตัวนั้น
  - ระบบจะตรวจพบว่า `storedOwner` (ผู้เล่น A) ไม่ตรงกับผู้เล่น B จึงทำการ **รีเซ็ตคอมโบกลับเป็น 1** และบันทึกผู้เล่น B เป็นเจ้าของแทน
  - เมื่อผู้เล่น A ตีซ้ำอีกครั้ง ก็จะถูกรีเซ็ตคอมโบกลับเป็น 1 เช่นกัน
  - ผลคือ **ไม่มีผู้เล่นคนใดสามารถทำคอมโบครบ 5 ฮิตเพื่อระเบิด Cocoon Stun ได้เลยเมื่อเล่นด้วยกันใน Multiplayer**

### 4.3 กล้องหลุดการร่ายง่ายเกินไปจาก Network Latency (Hymn of Purification)
* **ไฟล์:** [`HymnofPurificationEffect.java`](file:///d:/Minecraft/Dev/ironspell_more/ironspell_more/src/main/java/io/redspace/ironspell_more/effect/HymnofPurificationEffect.java#L82-L87)
* **สาเหตุ:** โค้ดบน Server ตรวจจับการขยับมุมกล้องของผู้เล่นอย่างเข้มงวด:
  ```java
  float yawDiff = Math.abs(Mth.wrapDegrees(entity.getYRot() - entity.getPersistentData().getFloat(START_YAW)));
  float pitchDiff = Math.abs(Mth.wrapDegrees(entity.getXRot() - entity.getPersistentData().getFloat(START_PITCH)));
  if (yawDiff > 3.0F || pitchDiff > 3.0F) {
      interrupt(entity);
      return;
  }
  ```
* **ผลกระทบ:** ในสภาพแวดล้อม Multiplayer ที่มี Latency หรือ Packet Jitter หรือผู้เล่นขยับเมาส์เพียงเล็กน้อย (3 องศา) การร่ายจะถูกขัดจังหวะทันที เกิดเสียงโน้ตผิดเพี้ยน และผู้ร่ายรวมถึงเพื่อนรอบข้างจะติดสถานะ Nausea (มึนเมา) ทันทีโดยไม่เจตนา

### 4.4 การปะปนของ Entity Client-Server ใน Integrated Server (ResonantKnellDomeAoe)
* **ไฟล์:** [`ResonantKnellDomeAoe.java`](file:///d:/Minecraft/Dev/ironspell_more/ironspell_more/src/main/java/io/redspace/ironspell_more/entity/spells/resonant_knell/ResonantKnellDomeAoe.java#L198-L200)
* **สาเหตุ:** ใน `onAddedToWorld()` มีการเพิ่ม Entity เข้าสู่ static set:
  ```java
  ACTIVE_DOMES.add(this);
  ```
  โดยไม่ได้เช็ค `if (!this.level().isClientSide)`
* **ผลกระทบ:** ใน Singleplayer หรือ Open to LAN (ซึ่ง Client และ Server แชร์ JVM Memory เดียวกัน) เซ็ตนี้จะมีทั้ง Entity ฝั่ง Client และ Server ปะปนกัน แม้ว่าใน Dedicated Server จะไม่เกิดปัญหานี้ แต่เป็น Bad Practice ใน Minecraft Modding

---

## 7. แนวทางและตัวอย่างโค้ดแก้ไข (Actionable Fixes)

### แก้ไขที่ 1: ปรับแก้ตรรกะตรวจสอบพันธมิตร (สถานะ: ✅ ดำเนินการแก้ไขแล้ว)
ใน [`HymnofPurificationSpell.java#L130`](file:///d:/Minecraft/Dev/ironspell_more/ironspell_more/src/main/java/io/redspace/ironspell_more/spells/gold/HymnofPurificationSpell.java#L130) และ [`ResonantKnellSpell.java#L250`](file:///d:/Minecraft/Dev/ironspell_more/ironspell_more/src/main/java/io/redspace/ironspell_more/spells/fire/ResonantKnellSpell.java#L250) ได้ตัดเครื่องหมาย `!` ออกเรียบร้อยแล้ว:

```diff
- return !DamageSources.isFriendlyFireBetween(caster, target);
+ return DamageSources.isFriendlyFireBetween(caster, target);
```

---

### แก้ไขที่ 2: ป้องกัน Crash จาก Optional Dependencies
1. ใน [`BlazingChakraSpell.java`](file:///d:/Minecraft/Dev/ironspell_more/ironspell_more/src/main/java/io/redspace/ironspell_more/spells/fire/BlazingChakraSpell.java) ให้นำค่าคงที่มาประกาศไว้ในตัว Spell เองแทนการดึงจาก `BlazingChakraVfx`:
   ```java
   public static final float MAX_RANGE = 12.0F;
   public static final float CLOSE_RANGE = 3.0F;
   ```
2. ประกาศ Dependencies ที่จำเป็นใน [`mods.toml`](file:///d:/Minecraft/Dev/ironspell_more/ironspell_more/src/main/resources/META-INF/mods.toml):
   ```toml
   [[dependencies.ironspell_more]]
       modId="traveloptics"
       mandatory=false
       versionRange="[6.0.0,)"
       ordering="AFTER"
       side="BOTH"

   [[dependencies.ironspell_more]]
       modId="cataclysm"
       mandatory=false
       versionRange="[1.0.0,)"
       ordering="AFTER"
       side="BOTH"
   ```
3. ทำ Soft-reference สำหรับ `TravelopticsSchools.AQUA_RESOURCE` แบบเดียวกับที่ทำใน `TigershadeTerrabreakSpell` โดยใช้ `ResourceLocation.fromNamespaceAndPath("traveloptics", "aqua")`

---

### แก้ไขที่ 3: ลดภาระ Network Packet ใน LightningStrikeSpell
ใน [`LightningStrikeSpell.java#L167`](file:///d:/Minecraft/Dev/ironspell_more/ironspell_more/src/main/java/io/redspace/ironspell_more/spells/lightning/LightningStrikeSpell.java#L167) แทนที่จะวนลูปส่งหาผู้เล่นทุกคน ให้ใช้คำสั่งของ Forge/Vanilla:

```diff
- for (ServerPlayer otherPlayer : serverLevel.players()) {
-     serverLevel.sendParticles(otherPlayer, new ZapParticleOptionCustom(sparkDest, zapLength), true,
-             targetX, targetY, targetZ, 1, 0, 0, 0, 0);
- }
+ serverLevel.sendParticles(new ZapParticleOptionCustom(sparkDest, zapLength),
+         targetX, targetY, targetZ, 1, 0, 0, 0, 0);
```
*(ระบบของ Minecraft จะคัดกรองส่งเฉพาะผู้เล่นที่อยู่ใน Tracking Range ของพิกัดนั้นโดยอัตโนมัติ)*

---

### แก้ไขที่ 4: ปรับปรุงการดูดเป้าหมายใน GaleDriveVortexEntity (สถานะ: ✅ ดำเนินการแก้ไขแล้ว)
ใน [`GaleDriveVortexEntity.java#L82`](file:///d:/Minecraft/Dev/ironspell_more/ironspell_more/src/main/java/io/redspace/ironspell_more/entity/spells/gale_drive/GaleDriveVortexEntity.java#L82) ได้เพิ่มการตรวจสอบสถานะ Removed และเปลี่ยนจากการ Force Teleport มาเป็นการปรับ Velocity ควบคุมผู้เล่นด้วย Motion อย่างปลอดภัยเรียบร้อยแล้ว:

```java
if (this.capturedTarget != null && this.capturedTarget.isAlive() && !this.capturedTarget.isRemoved()) {
    double targetY = this.groundY + 6.0;
    if (this.capturedTarget instanceof ServerPlayer player) {
        // ดึงเข้าศูนย์กลางผ่าน Motion เพื่อป้องกัน Rubberbanding และ Anti-cheat
        Vec3 pull = new Vec3(this.getX() - player.getX(), targetY - player.getY(), this.getZ() - player.getZ());
        player.setDeltaMovement(pull.scale(0.35));
        player.hurtMarked = true;
    } else {
        this.capturedTarget.teleportTo(this.getX(), targetY, this.getZ());
        this.capturedTarget.setDeltaMovement(0, 0, 0);
    }
    this.capturedTarget.fallDistance = 6.0f;
    ...
```

---

### แก้ไขที่ 5: ปรับปรุง Client Entity Resolution ใน GoldChain
1. ใน `writeSpawnData` ให้ส่งทั้ง `Entity ID` และ `UUID` (ผ่าน `buffer.writeUUID(...)`)
2. ใน `getVictim()` ให้รองรับการค้นหาบน `ClientLevel` ด้วย:
   ```java
   @Nullable
   public Entity getVictim() {
       if (this.cachedVictim != null && !this.cachedVictim.isRemoved()) {
           return this.cachedVictim;
       }
       if (this.victimUUID != null) {
           if (this.level() instanceof ServerLevel serverLevel) {
               this.cachedVictim = serverLevel.getEntity(this.victimUUID);
           } else if (this.level() instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel) {
               for (Entity entity : clientLevel.entitiesForRendering()) {
                   if (entity.getUUID().equals(this.victimUUID)) {
                       this.cachedVictim = entity;
                       break;
                   }
               }
           }
       }
       return this.cachedVictim;
   }
   ```
