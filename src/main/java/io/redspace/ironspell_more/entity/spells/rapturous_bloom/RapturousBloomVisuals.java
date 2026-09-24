package io.redspace.ironspell_more.entity.spells.rapturous_bloom;

/**
 * Visual constants and geometric configurations for RapturousBloomRenderer.
 * Theme: Ethereal Glowing Lotus / Plum Blossom with layered petals, radiant stamen,
 * concentric water ripples, and radial shatter dispersion.
 *
 * Sequence:
 * 1. หุบเป็นตุ่ม (Closed Bud): Tightly closed bud at center during water ripple (0-2s)
 * 2. ค่อยผลิบาน (Unfolding): Outer -> Middle -> Inner petals peel back smoothly (2-5.1s)
 * 3. บานเต็มที่ (Full Bloom): Fully open blossom with gentle breathing and rotation (5.1-6s)
 * 4. แตกออก (Shatter): Petals disperse from 6-6.5s; the final ripple fades by 6.8s
 */
public final class RapturousBloomVisuals {
    private RapturousBloomVisuals() {}

    // --- Geometry Resolution ---
    public static final int GROUND_SEGMENTS = 64;
    /**
     * Lightweight surface grid used by every independently transformed petal.
     * Nine rows give the silhouette extra shoulder/cap control, while seven
     * columns resolve the rounded dome and its shallow organic edge wave.
     */
    public static final int PETAL_LENGTH_ROWS = 9;
    public static final int PETAL_WIDTH_COLUMNS = 7;
    public static final float BASE_RADIUS = 3.0F;

    // --- Bloom Motion ---
    // Keep the first phase as a genuinely small bud, then unfold each layer
    // over most of the bloom phase with smooth acceleration and deceleration.
    public static final float BUD_START_SCALE = 0.08F;
    public static final float BUD_END_SCALE = 0.52F;
    public static final float OUTER_BLOOM_TICKS = 54.0F;
    public static final float MIDDLE_BLOOM_DELAY_TICKS = 7.0F;
    public static final float MIDDLE_BLOOM_TICKS = 52.0F;
    public static final float INNER_BLOOM_DELAY_TICKS = 14.0F;
    public static final float INNER_BLOOM_TICKS = 48.0F;
    public static final float FLOWER_GROWTH_TICKS = 58.0F;

    // --- Colors: Vivid Scarlet Petal Palette ---
    public static final float PETAL_SHADOW_R = 0.471F;
    public static final float PETAL_SHADOW_G = 0.000F;
    public static final float PETAL_SHADOW_B = 0.059F;

    public static final float PETAL_BASE_R = 0.702F;
    public static final float PETAL_BASE_G = 0.000F;
    public static final float PETAL_BASE_B = 0.098F;

    public static final float PETAL_BODY_R = 0.851F;
    public static final float PETAL_BODY_G = 0.082F;
    public static final float PETAL_BODY_B = 0.184F;

    public static final float PETAL_BRIGHT_R = 0.941F;
    public static final float PETAL_BRIGHT_G = 0.165F;
    public static final float PETAL_BRIGHT_B = 0.259F;

    public static final float PETAL_RIM_R = 1.000F;
    public static final float PETAL_RIM_G = 0.251F;
    public static final float PETAL_RIM_B = 0.341F;

    public static final float PETAL_HIGHLIGHT_R = 1.000F;
    public static final float PETAL_HIGHLIGHT_G = 0.420F;
    public static final float PETAL_HIGHLIGHT_B = 0.471F;

    // --- Colors: Center Stamen (เกสรสีทองเรืองแสง) ---
    public static final float STAMEN_R = 1.00F;
    public static final float STAMEN_G = 0.86F;
    public static final float STAMEN_B = 0.25F;

    public static final float STAMEN_TIP_R = 1.00F;
    public static final float STAMEN_TIP_G = 0.98F;
    public static final float STAMEN_TIP_B = 0.65F;

    // --- Colors: Ground Water Ripples (Phase 1 Aqua/Cyan) ---
    public static final float RIPPLE_R = 0.22F;
    public static final float RIPPLE_G = 0.85F;
    public static final float RIPPLE_B = 0.98F;

    public static final float RIPPLE_CORE_R = 0.65F;
    public static final float RIPPLE_CORE_G = 0.95F;
    public static final float RIPPLE_CORE_B = 1.00F;

    // --- Colors: Ground Floral Sigil (Phase 2 Magenta/Crimson) ---
    public static final float GROUND_SIGIL_R = 0.478F;
    public static final float GROUND_SIGIL_G = 0.000F;
    public static final float GROUND_SIGIL_B = 0.118F;

    // --- Colors: Burst Shockwave (Phase 3 Shatter) ---
    public static final float BURST_R = 0.718F;
    public static final float BURST_G = 0.039F;
    public static final float BURST_B = 0.165F;

    // --- Petal Layer Structure ---
    // Outer Layer (กลีบชั้นนอก - 10 Petals)
    public static final int OUTER_PETAL_COUNT = 10;
    public static final float OUTER_PETAL_LENGTH = 1.48F;
    public static final float OUTER_PETAL_WIDTH = 0.96F;
    public static final float OUTER_PETAL_CURVE = 0.20F;
    public static final float OUTER_BUD_ANGLE = 0.24F;    // ~14° from vertical (tight closed bud)
    public static final float OUTER_BLOOM_ANGLE = 1.32F;  // ~76° from vertical (unfolded wide)
    public static final float OUTER_PIVOT_Y = 0.06F;

    // Middle Layer (กลีบชั้นกลาง - 8 Petals)
    public static final int MIDDLE_PETAL_COUNT = 8;
    public static final float MIDDLE_PETAL_LENGTH = 1.22F;
    public static final float MIDDLE_PETAL_WIDTH = 0.78F;
    public static final float MIDDLE_PETAL_CURVE = 0.18F;
    public static final float MIDDLE_BUD_ANGLE = 0.19F;   // ~11° from vertical
    public static final float MIDDLE_BLOOM_ANGLE = 0.95F; // ~54° from vertical
    public static final float MIDDLE_PIVOT_Y = 0.10F;

    // Inner Layer (กลีบชั้นใน - 6 Petals)
    public static final int INNER_PETAL_COUNT = 6;
    public static final float INNER_PETAL_LENGTH = 0.92F;
    public static final float INNER_PETAL_WIDTH = 0.60F;
    public static final float INNER_PETAL_CURVE = 0.15F;
    public static final float INNER_BUD_ANGLE = 0.14F;    // ~8° from vertical
    public static final float INNER_BLOOM_ANGLE = 0.56F;  // ~32° from vertical
    public static final float INNER_PIVOT_Y = 0.14F;

    // Stamen Filaments (เกสรตัวผู้)
    public static final int STAMEN_COUNT = 12;
    public static final float STAMEN_HEIGHT = 0.62F;
    public static final float STAMEN_RADIUS = 0.13F;
    public static final float STAMEN_TIP_SIZE = 0.07F;

    // Ground Ring Rendering
    public static final float GROUND_Y = 0.025F;
    public static final float RING_WIDTH = 0.08F;
    public static final float RIPPLE_LINE_WIDTH = 0.025F;
    public static final float BURST_RIPPLE_LINE_WIDTH = 0.040F;
    public static final int RIPPLE_COUNT = 3;
    public static final float RIPPLE_CYCLE_TICKS = 52.0F;

    // Render Distance
    public static final double MAX_RENDER_DISTANCE = 64.0;
    public static final double MAX_RENDER_DISTANCE_SQ = MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE;
}
