package io.redspace.ironspell_more.client.renderer;

/**
 * Visual constants and tuning for ResonantKnellDomeRenderer.
 * Theme: Blazing Sun / Fiery Gold + Red Nuclear Shockwave with Taoist Talismans.
 */
public final class ResonantKnellDomeVisuals {
    private ResonantKnellDomeVisuals() {
    }

    // --- Mesh resolution ---
    public static final int LAT_RINGS = 14;
    public static final int SEGMENTS = 36;
    public static final double LOWER_EXTENT = 0.65;
    public static final float BASE_RADIUS = 8.0f;

    // --- Colors (Fiery Radiant Gold + Blaze Red Rim) ---
    public static final float COLOR_R = 1.0f;
    public static final float COLOR_G = 0.76f;
    public static final float COLOR_B = 0.28f;

    public static final float EDGE_R = 1.0f;
    public static final float EDGE_G = 0.42f;
    public static final float EDGE_B = 0.08f;

    public static final float CORE_R = 1.0f;
    public static final float CORE_G = 0.95f;
    public static final float CORE_B = 0.55f;

    // --- Alpha & Fresnel ---
    public static final float BASE_ALPHA = 0.05f;
    public static final float EDGE_ALPHA = 0.42f;
    public static final float EDGE_POWER = 2.6f;
    public static final float INSIDE_ALPHA = 0.14f;
    public static final float RIM_ALPHA = 0.32f;
    public static final float RIM_HEIGHT = 1.5f;

    // --- Opening motion / Fade-in / Fade-out ---
    public static final int FADE_IN_TICKS = 6;
    public static final float DOME_OPEN_TICKS = 12.0f;
    public static final float DOME_OPEN_START_SCALE = 0.22f;
    public static final float DOME_OPEN_START_Y = -1.4f;
    public static final float DOME_OPEN_OVERSHOOT = 1.15f;
    public static final double MAX_RENDER_DISTANCE = 64.0;
    public static final double MAX_RENDER_DISTANCE_SQ = MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE;

    // --- Swirling Golden Solar Bands on Shell ---
    public static final int STREAK_COUNT = 24;
    public static final int STREAK_SEGMENTS = 20;
    public static final float STREAK_CYCLE_TICKS = 45.0f;
    public static final float STREAK_MIN_HEIGHT = 0.05f;
    public static final float STREAK_MAX_HEIGHT = 0.92f;
    public static final float STREAK_MIN_SPAN_DEG = 40.0f;
    public static final float STREAK_MAX_SPAN_DEG = 125.0f;
    public static final float STREAK_SPEED_DEG = 0.70f;
    public static final float STREAK_WIDTH = 0.32f;
    public static final float STREAK_ALPHA = 0.36f;
    public static final float STREAK_R = 1.0f, STREAK_G = 0.88f, STREAK_B = 0.35f;
    public static final float STREAK_SURFACE_OFFSET = 0.04f;

    // --- Red & Orange Taoist Talisman Amulets (ผืนผ้ายันต์สีแดง-ส้ม) ---
    public static final int TALISMAN_COUNT = 22;
    public static final float TALISMAN_WIDTH = 0.55f;
    public static final float TALISMAN_HEIGHT = 1.25f;
    public static final float TALISMAN_RADIUS_OFFSET = 0.15f;
    public static final float TALISMAN_ORBIT_SPEED_DEG = 0.32f;
    public static final float TALISMAN_ALPHA = 0.95f;

    // --- Spirit Fire Wisps (ลูกไฟวิญญาณสีฟ้าอ่อน) ---
    public static final int SPIRIT_WISP_COUNT = 16;
    public static final float SPIRIT_WISP_SIZE = 0.36f;
    public static final float SPIRIT_R = 0.35f;
    public static final float SPIRIT_G = 0.82f;
    public static final float SPIRIT_B = 1.0f;
    public static final float SPIRIT_ALPHA = 0.65f;

    // --- Nuclear Shockwave Blast ---
    public static final int SHOCKWAVE_TICKS = 24;
    public static final float SHOCKWAVE_R = 1.0f;
    public static final float SHOCKWAVE_G = 0.55f;
    public static final float SHOCKWAVE_B = 0.12f;
    public static final float SHOCKWAVE_CORE_R = 1.0f;
    public static final float SHOCKWAVE_CORE_G = 0.95f;
    public static final float SHOCKWAVE_CORE_B = 0.55f;
    public static final float SHOCKWAVE_WALL_HEIGHT = 2.6f;
    public static final float SHOCKWAVE_RING_WIDTH = 1.5f;

    // --- Ground Energy Flow ---
    // While the dome is open, these rings repeatedly contract toward the caster's feet.
    public static final int CONVERGING_RING_COUNT = 4;
    public static final float CONVERGING_RING_CYCLE_TICKS = 36.0f;
    public static final float CONVERGING_RING_MIN_RADIUS = 0.45f;
    public static final float CONVERGING_RING_MAX_RADIUS = 7.6f;
    public static final float CONVERGING_RING_WIDTH = 0.14f;
    public static final float CONVERGING_RING_ALPHA = 0.62f;

    // During a blast, staggered echoes expand from beneath the caster to the gameplay radius.
    public static final int EXPANDING_RING_COUNT = 3;
    public static final float EXPANDING_RING_STAGGER = 0.10f;
    public static final float EXPANDING_RING_START_RADIUS = 0.35f;
    public static final float EXPANDING_RING_WIDTH = 0.42f;
    public static final float EXPANDING_RING_ALPHA = 0.58f;

    // --- Ground Sigil Ring ---
    public static final int SIGIL_SEGMENTS = 72;
    public static final float SIGIL_Y = 0.03f;
    public static final float SIGIL_LINE_WIDTH = 0.06f;
    public static final float SIGIL_ALPHA = 0.45f;
}
