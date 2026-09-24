# Rapturous Bloom

## Identity and configuration

- Registry ID: `ironspell_more:rapturous_bloom`
- School: Nature
- Rarity: Rare
- Maximum level: 5
- Cast type: `INSTANT`
- Mana cost: 45 base (+5 per level)
- Base cooldown: 16 seconds
- Range: 28 blocks
- Target requirement: LivingEntity (enemy/non-friendly, line of sight required)
- Animation: `SpellAnimations.ANIMATION_INSTANT_CAST`
- Sounds:
  - Cast start sound: `SoundEvents.ENCHANTMENT_TABLE_USE`
  - Cast finish sound: `SoundEvents.AMETHYST_BLOCK_CHIME`
  - Bloom sound (at 2s): `SoundEvents.FLOWERING_AZALEA_PLACE` + `SoundEvents.CHERRY_SAPLING_PLACE`
  - Burst sound (at 6s): `SoundEvents.GENERIC_EXPLODE` + `SoundEvents.CHERRY_LEAVES_BREAK`

## Targeting and ground placement

The caster aims at an enemy living entity within 28 blocks. The spell validates:
1. Target is a valid `LivingEntity`, is alive, is not the caster, is not allied to the caster, and does not have friendly-fire immunity with the caster (`!DamageSources.isFriendlyFireBetween(caster, target)`).
2. The caster has line of sight to the target (`Utils.hasLineOfSight`).
3. Ground placement: The server traces straight down from the target's bounding box feet up to 16 blocks to find the highest solid collision surface. If no solid surface is found within 16 blocks, the cast fails and notifies the caster via action bar (`ui.ironspell_more.rapturous_bloom_no_ground`) without consuming mana or cooldown.
4. Active bloom limits and anti-stacking:
   - Each caster can maintain at most 3 active blooms simultaneously. Casting a 4th bloom is rejected with action bar message `ui.ironspell_more.rapturous_bloom_max_active`.
   - Blooms cannot stack on the same target or overlap horizontally. Because each bloom has a 3.0-block radius, two blooms must be at least 6.0 blocks apart (center-to-center distance) to prevent overlapping areas. Attempting to cast within 6.0 blocks of an existing active bloom is rejected with action bar message `ui.ironspell_more.rapturous_bloom_already_blooming`.

## Entity lifecycle and phases

The spawned entity is `RapturousBloomEntity` (`io.redspace.ironspell_more.entity.spells.rapturous_bloom.RapturousBloomEntity`, registered as `ironspell_more:rapturous_bloom`), extending `AoeEntity` and implementing `AntiMagicSusceptible`. It is stationary, circular, and server-authoritative with a 3.0-block radius.

The entity lifecycle lasts 136 ticks (6.8 seconds total):

### Phase 1: Closed Bud & Water Ripple (`PHASE_RIPPLE`, Ticks 0–40 / 0–2s)
- Concentric aqua water ripple rings appear on the ground at the anchor point (`y = 0.025`).
- Ripple waves expand slowly towards the 3.0-block perimeter and continuously lose opacity as their radius grows, fading completely before each cycle restarts.
- **หุบเป็นตุ่มก่อน (Closed Bud):** ดอกไม้เริ่มต้นจากการหุบเป็นตุ่มตูมแน่นตรงใจกลางวงน้ำ:
  - กลีบทุกชั้นตั้งตรงและโอบรอบศูนย์กลางอย่างแน่นหนา (`OUTER_BUD_ANGLE ~14°`, `MIDDLE_BUD_ANGLE ~11°`, `INNER_BUD_ANGLE ~8°`).
  - กลีบโค้งนูนป่องตรงกลางและงุ้มเข้าหากันที่ปลายยอดเพื่อห่อหุ้มเกสรไว้ด้านในทั้งหมดอย่างมิดชิด.
  - ตุ่มดอกไม้เริ่มจากขนาดเล็กมากและค่อยๆ โผล่พ้นผิวน้ำตลอดช่วง 2 วินาทีแรกด้วย `smootherstep` (สเกล 0.08 ถึง 0.52) พร้อมการลอยเบาๆ ที่ไม่รบกวน motion หลัก.
- No damage or status effects are applied during this phase.

### Phase 2: Progressive Bloom & Full Bloom (`PHASE_BLOOM`, Ticks 40–120 / 2–6s)
- **ค่อยๆ ผลิบาน (Ticks 40–102 / 2–5.1s):** เมื่อครบ 2 วินาที กลีบดอกไม้จะค่อยๆ คลี่บานออกทีละชั้นด้วย `smootherstep` เพื่อให้ช่วงเริ่มและหยุดของแต่ละชั้นนุ่มนวล:
  - กลีบชั้นนอก (Outer Layer, 10 petals): เริ่มคลี่ออกเป็นชั้นแรก โดยกางแผ่กว้างลงสู่พื้นดิน (~76° จากแนวดิ่ง).
  - กลีบชั้นกลาง (Middle Layer, 8 petals): คลี่บานตามในองศาปานกลาง (~54° จากแนวดิ่ง).
  - กลีบชั้นใน (Inner Layer, 6 petals): คลี่บานตามเป็นชั้นสุดท้ายในลักษณะรูปถ้วยรองรับใจกลางดอก (~32° จากแนวดิ่ง).
  - เกสรสีทองตรงกลาง (Center Stamen, 12 filaments): คงรูปทรงและความสูงเดิม ไม่ยืดตัวระหว่างการบาน แต่ค่อยๆ ปรากฏด้วย opacity เมื่อกลีบดอกเปิดออก.
  - สเกลดอกไม้ค่อยๆ ขยายจาก 0.52 สู่ขนาดเต็ม 1.0 ใน 58 ticks แรกของเฟสนี้.
  - `dungeons_and_combat:blessed_sparkle` ค่อยๆ ถูกปล่อยจากจุดกึ่งกลางด้วยแรงส่งขึ้นด้านบนและ drift แนวรัศมีเล็กน้อย เพื่อให้มีอารมณ์เหมือนละอองเกสรลอยขึ้น.
- **บานเต็มที่ (Full Bloom, Ticks 102–120 / 5.1–6s):**
  - ดอกบานสะพรั่งเต็มที่ แผ่รัศมีพลังงานพฤกษาลงสู่วงเวทสีบานเย็นบนพื้น.
  - ดอกไม้หายใจและลอยพลิ้วไหวเบาๆ ($1.0 \pm 0.018$) โดย motion นี้ค่อยๆ fade in หลังดอกใกล้บานเต็มที่ พร้อมหมุนช้าๆ ($0.22^\circ/\text{tick}$).
  - ปล่อยละอองเกสร กลิ่นหอม และละอองดอกซากุระ/สปอร์ พร้อมกระหน่ำสถานะผิดปกติทุกๆ 20 ticks (1.0s):
    - **Poison I** (duration: 160 ticks / 8 seconds)
    - **Wither I** (duration: 160 ticks / 8 seconds)
    - **Slowness III** (amplifier 2, duration: 100 ticks / 5 seconds)

### Phase 3: Petal Burst & Shatter (`PHASE_BURST`, Ticks 120–136 / 6–6.8s)
- **แตกออก (At Tick 120 / 6s):**
  - ดอกไม้ระเบิดแตกออกเป็นกลีบ ("โพล๊ะ"):
    - กลีบดอกทุกชิ้นหลุดออกจากฐานและพุ่งกระจายออกรอบทิศทางในแนวรัศมีด้วยความเร็วสูง พร้อมหมุนเคว้งกลางอากาศ.
    - กลีบแตกกระจายเป็นชิ้นเล็กๆ (Shards) ลอยหมุนและค่อยๆ จางหายไปใน 0.5 วินาที ($(1 - p)^2$).
    - วงคลื่นกระแทกสีแดงเข้มเป็นเส้นบางแบบระลอกน้ำ (แกนเส้นกว้าง 0.04 บล็อก) ขยายตัวบนพื้นจาก 0.6 ถึง 4.4 บล็อกตลอด 16 ticks พร้อม halo เรืองแสงเฉพาะตัววงที่กำลังกระเพื่อม และค่อยๆ จางจนหายเมื่อขยายออก.
    - เกสรสีทองตรงกลางคงตำแหน่งและรูปทรงเดิม แล้วค่อยๆ สลายด้วย opacity โดยไม่ยืดหรือพุ่งกระจายออกด้านข้าง.
  - Direct burst damage:
    - Formula: `(24.0F + 4.0F * (spellLevel - 1)) * getEntityPowerMultiplier(caster)`.
    - Applied once to all valid living enemies within the 3.0-block radius cylinder.
  - Audio: Explosion sound + cherry leaf break.
  - VFX: Procedural thin glowing ground ripple + `ironspell_more:red_plum` particles scattering and spinning outwards with exact directional velocity. The separate textured `ShockwaveParticleCustom` is not spawned for this spell, preventing a thick duplicate ring.
  - The entity is marked inactive in the active bloom registry so it no longer counts against the caster's 3-bloom limit or area exclusion.
- **Ticks 120–130:** Petals and shards complete their original 10-tick (0.5s) dispersal and fade.
- **Ticks 120–136:** The non-damaging entity shell remains for 16 ticks (0.8s) so the slower final ripple can reach its full radius and fade completely before the entity discards at tick 136.

## Anti-Magic and cancellation

- If `onAntiMagic` is triggered on `RapturousBloomEntity`, the flower withers and discards immediately without dealing burst damage or spawning explosion particles.
- If the caster dies or disconnects, the centralized enemy predicate fails closed against allies and prevents accidental friendly fire.

## Visuals and Geometric Procedural Renderer

The spell visuals are implemented with a custom geometric procedural renderer (`RapturousBloomRenderer` extending `EntityRenderer<RapturousBloomEntity>`) and double-sided vertex quads without GeckoLib dependencies. Stable petals use an opaque, depth-writing `entityCutoutNoCull` layer, while ground effects, the glowing stamen, and shattering/fading petals retain the translucent emissive additive layer (`SRC_ALPHA`, `ONE`):

- **Ground Rings (`renderGroundVisuals`):**
  - เฉพาะวงที่กำลังกระเพื่อมใช้ additive band ซ้อนกัน 3 ชั้น (halo บางๆ, glow ชั้นกลาง และแกนเส้นสว่าง) ส่วน boundary/sigil ที่อยู่นิ่งเป็นเส้นเดี่ยวและไม่ใช้ halo ซ้อน.
  - Phase 1: ระลอกน้ำเส้นบางขนาด 0.025 บล็อกขยายทั่วรัศมี 3.0 บล็อกด้วยรอบละ 52 ticks (ช้าลงจาก 30 ticks) ใช้สี aqua-blue (`R=0.22, G=0.85, B=0.98`) และลด opacity ตามระยะการขยายจนหายสนิทก่อนเริ่มรอบใหม่.
  - Phase 2: วงน้ำถูก cross-fade ไปเป็น floral blood-crimson nature sigil ภายใน 20 ticks; วง sigil ที่อยู่นิ่งไม่ใช้ halo ขณะที่ระลอกน้ำเส้นบางซึ่งยังเคลื่อนอยู่มี glow และ fade-out แบบเดียวกับ Phase 1.
  - Phase 3: วง sigil ค่อยๆ จางพร้อมกับระลอก shockwave `#B70A2A` (`R=0.718, G=0.039, B=0.165`) เส้นบางที่ขยายถึง 4.4 บล็อกใน 16 ticks ด้วย `smootherstep` และลด opacity จนเป็นศูนย์. Glow ถูกใช้เฉพาะระลอกที่กำลังขยาย.
- **Layered Flower Geometry (`renderFlowerModel`):**
  - **Center Stamen (เกสร):** Glowing golden core and 12 fixed-height vertical filaments (`STAMEN_R, STAMEN_G, STAMEN_B`) with luminous diamond pollen tips (`STAMEN_TIP_R, STAMEN_TIP_G, STAMEN_TIP_B`). Visibility changes through opacity only; the stamen neither stretches during bloom nor disperses during the burst.
  - **Inner Layer (กลีบชั้นใน):** 6 curved petals, length 0.92, width 0.60, unfolding from 8° to 32° from vertical.
  - **Middle Layer (กลีบชั้นกลาง):** 8 curved petals (offset π/8), length 1.22, width 0.78, unfolding from 11° to 54° from vertical.
  - **Outer Layer (กลีบชั้นนอก):** 10 curved petals, length 1.48, width 0.96, unfolding from 14° to 76° from vertical. The widened layer overlaps neighboring petals from above and closes the flower into a dense circular silhouette.
  - **Individual Petal Structure:** Each independently transformed petal uses a lightweight 9-row by 7-column parametric surface grid (48 quads). The width grows smoothly from a narrow 7-10% base to a broad shoulder at 68-76% of the petal length, then tapers by only 14-26%, so the outer edge remains wide. A layer-specific 12-20% longitudinal cap extension pushes the center of the upper rows beyond the sides to form a shallow rounded half-ellipse rather than a single tip. Three parameter sets provide the broadest/roundest outer petals, balanced middle petals, and slightly narrower inner petals. A 3.5% upper-half edge wave plus deterministic per-petal length, width, and asymmetry variation prevents mechanical duplication while preserving the existing base pivot, bloom rotation, and radial-scatter transforms. Longitudinal arch and raised side edges keep the thin mesh softly cupped.
  - **Petal Palette and Opacity:** Stable petals use a vivid scarlet range: Shadow `#78000F`, Base `#B30019`, Body `#D9152F`, Bright `#F02A42`, Rim `#FF4057`, and Highlight `#FF6B78`. Every stable-petal vertex renders at alpha `1.0` through an opaque depth-writing layer, keeping overlapping layers saturated and clearly separated instead of blending into transparency. During the burst phase, petals and shards switch back to the additive layer so the existing shatter fade remains smooth.
  - **Shatter Dispersal & Shards (`renderPetalShards`):** During Phase 3, individual petals fly outward along radial trajectories with aerodynamic tumbling, while spawning high-velocity spinning shard quads that fade away.
- **Custom Plum Petal Particle:** `ironspell_more:red_plum` uses `textures/particle/red_plum.png` through `particles/red_plum.json` and `RedPlumParticle`. The client particle is translucent and full-bright, mirrors some sprites, rotates continuously, curls its horizontal velocity in a shared direction, flutters vertically, and fades at the beginning/end of its 30-48 tick lifetime. A compact orbit starts during the water-ripple phase; its spawn radius, height, and outward drift grow continuously with total lifecycle progress. The orbit persists through bloom and reaches near the spell perimeter before the burst. Burst particles detect their higher launch speed, retain only a slight curve, and preserve the strong radial explosion.
- **Center Pollen Particle:** `dungeons_and_combat:blessed_sparkle` is resolved from the Forge particle registry on the client and emitted every 2–3 ticks from a tight radius around the flower center. Its upward speed and emission density increase gently as the flower opens. If the external particle ID is unavailable or is not a `SimpleParticleType`, emission is skipped safely.

## Server and client responsibilities

- **Server-Authoritative:**
  - Target acquisition, line-of-sight validation, and ground collision check.
  - Multi-bloom tracking (max 3 per caster) and non-stacking spatial check (6.0 blocks minimum center distance).
  - Entity spawning, phase timing, periodic debuffs, damage calculation, and entity removal.
- **Client-Side:**
  - Procedural mathematical mesh rendering and dynamic bone-free transformations based on synced entity phase and tick count.
  - Custom rotating `red_plum` petals from the initial ripple through full bloom, with progressive orbit expansion; ambient nectar and rising `dungeons_and_combat:blessed_sparkle` pollen join during bloom. Vanilla blossom/cherry visuals are not used.
  - Custom plum-petal burst cloud and a single procedural thin glowing ripple during the burst phase; the shared textured shockwave particle is intentionally omitted for this spell.

## Verification

- Passes `.\gradlew.bat compileJava` with zero errors.
- Passes `.\gradlew.bat build`; the project currently has no Java test sources, so Gradle reports the test task as `NO-SOURCE`.
- All registry keys, language entries, and texture paths resolve without errors.
