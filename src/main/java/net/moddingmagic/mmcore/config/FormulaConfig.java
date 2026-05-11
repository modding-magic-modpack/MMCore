package net.moddingmagic.mmcore.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class FormulaConfig {
    public static final ModConfigSpec SPEC;

    // Cast Time Reduction

    /**
     * Which formula to use for effective cast time.
     * The attribute value is passed in; the result replaces  2 - softCap(attr).
     * A result of 1.0 means no change; 0.5 means half cast time; 2.0 means double.
     */
    public static final ModConfigSpec.EnumValue<FormulaType> CAST_TIME_FORMULA;
    public static final ModConfigSpec.DoubleValue CAST_TIME_DIMINISH_START;
    public static final ModConfigSpec.DoubleValue CAST_TIME_INTENSITY;
    public static final ModConfigSpec.DoubleValue CAST_TIME_OFFSET;
    public static final ModConfigSpec.DoubleValue CAST_TIME_CAP;

    // Cooldown Reduction

    /**
     * Which formula to use for effective cooldown.
     * The attribute value is passed in; the result replaces  2 - softCap(attr).
     * A result of 1.0 means no change; 0.5 means half cooldown; 2.0 means double.
     */
    public static final ModConfigSpec.EnumValue<FormulaType> COOLDOWN_FORMULA;
    public static final ModConfigSpec.DoubleValue COOLDOWN_DIMINISH_START;
    public static final ModConfigSpec.DoubleValue COOLDOWN_INTENSITY;
    public static final ModConfigSpec.DoubleValue COOLDOWN_OFFSET;
    public static final ModConfigSpec.DoubleValue COOLDOWN_CAP;


    // Spell Resist

    /**
     * Which formula to use for the resist multiplier.
     * The combined resist value (school * base) is passed in.
     * The result replaces  2 - softCap(combined).
     * A result < 1.0 means the entity takes less damage; > 1.0 means more.
     */
    public static final ModConfigSpec.EnumValue<FormulaType> RESIST_FORMULA;
    public static final ModConfigSpec.DoubleValue RESIST_DIMINISH_START;
    public static final ModConfigSpec.DoubleValue RESIST_INTENSITY;
    public static final ModConfigSpec.DoubleValue RESIST_OFFSET;
    public static final ModConfigSpec.DoubleValue RESIST_CAP;


    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        // ---- Cast Time Reduction ----
        b.comment(
                "Cast Time Reduction formula",
                "Controls how the irons_spellbooks:cast_time_reduction attribute shortens casting time.",
                "The result is used as a multiplier on the base cast time ticks.",
                "A result of 1.0 = no change, 0.5 = half duration, 2.0 = double duration.",
                "Multiplier = 2 - Formula.",
                "Formula types:",
                "  SOFTCAP  – Iron's default: multiplier = softCap(x), asymptote at 2.0",
                "  CUSTOM   – If x <= dStart  →  x, if x >  dStart  →  -intensity / (x - offset) + cap"
        ).push("cast_time_reduction");

        CAST_TIME_FORMULA    = b.comment("Formula type for Cast Time Reduction. Default: SOFTCAP")
                .defineEnum("formula", FormulaType.SOFTCAP);

        CAST_TIME_DIMINISH_START   = b.comment("Value where diminishing returns begin")
                .defineInRange("dStart",   1.5, 0.0, 1000.0);

        CAST_TIME_INTENSITY = b.comment("Steepness of the curve.")
                .defineInRange("intensity", 0.25, 0.001, 1.0);

        CAST_TIME_OFFSET = b.comment("Curve offset.")
                .defineInRange("offset", 1.0, -1000.0, 1000.0);

        CAST_TIME_CAP = b.comment("Cap which the formula curves towards but never quite reaches.")
                .defineInRange("cap", 2.0, 0.0, 2.0);

        b.pop();

        // ---- Cooldown Reduction ----
        b.comment(
                "Cooldown Reduction formula",
                "Controls how the irons_spellbooks:cooldown_reduction attribute shortens spell cooldowns.",
                "The result is used as a multiplier on the base cooldown ticks.",
                "A result of 1.0 = no change, 0.5 = half duration, 2.0 = double duration.",
                "Multiplier = 2 - Formula.",
                "Formula types:",
                "  SOFTCAP  – Iron's default: multiplier = softCap(x), asymptote at 2.0",
                "  CUSTOM   – If x <= dStart  →  x, if x >  dStart  →  -intensity / (x - offset) + cap"
        ).push("cooldown_reduction");

        COOLDOWN_FORMULA = b.comment("Formula type for Cooldown Reduction. Default: SOFTCAP")
                .defineEnum("formula", FormulaType.SOFTCAP);

        COOLDOWN_DIMINISH_START   = b.comment("Value where diminishing returns begin")
                .defineInRange("dStart",   1.5, 0.0, 1000.0);

        COOLDOWN_INTENSITY = b.comment("Steepness of the curve.")
                .defineInRange("intensity", 0.25, 0.001, 1.0);

        COOLDOWN_OFFSET = b.comment("Curve offset.")
                .defineInRange("offset", 1.0, -1000.0, 1000.0);

        COOLDOWN_CAP = b.comment("Cap which the formula curves towards but never quite reaches.")
                .defineInRange("cap", 2.0, 0.0, 2.0);

        b.pop();

        // ---- Spell Resist ----
        b.comment(
                "Spell Resist formula",
                "Controls how the irons_spellbooks:spell_resist (and school-specific magic resist) attribute reduces incoming spell damage.",
                "The combined resist value (schoolResist * baseResist) is fed into the formula.",
                "The output is used directly as the damage multiplier applied to incoming spell damage.",
                "A result < 1.0 means the entity takes less damage; > 1.0 means more (vulnerability).",
                "Multiplier = 2 - Formula.",
                "Formula types:",
                "  SOFTCAP  – Iron's default: multiplier = softCap(x), asymptote at 2.0",
                "  CUSTOM   – If x <= dStart  →  x, if x >  dStart  →  -intensity / (x - offset) + cap"
        ).push("spell_resist");

        RESIST_FORMULA = b.comment("Formula type for Spell Resist. Default: SOFTCAP")
                .defineEnum("formula", FormulaType.SOFTCAP);

        RESIST_DIMINISH_START   = b.comment("Value where diminishing returns begin")
                .defineInRange("dStart",   1.5, 0.0, 1000.0);

        RESIST_INTENSITY = b.comment("Steepness of the curve.")
                .defineInRange("intensity", 0.25, 0.001, 1.0);

        RESIST_OFFSET = b.comment("Curve offset.")
                .defineInRange("offset", 1.0, -1000.0, 1000.0);

        RESIST_CAP = b.comment("Cap which the formula curves towards but never quite reaches.")
                .defineInRange("cap", 2.0, 0.0, 2.0);

        b.pop();

        SPEC = b.build();
    }


    // Formula type enum

    public enum FormulaType {

        /**
         * Iron's built-in soft-cap:
         *   x <= 1.5  →  x
         *   x >  1.5  →  -0.25 / (x - 1) + 2
         * Asymptote at 2.
         */
        SOFTCAP,

        /**
         * Modifiable soft-cap:
         *   x <= dStart  →  x
         *   x >  dStart  →  -intensity / (x - offset) + cap
         * Asymptote at cap.
         */
        CUSTOM
    }
}
