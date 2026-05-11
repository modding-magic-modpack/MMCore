package net.moddingmagic.mmcore.formulas;

import io.redspace.ironsspellbooks.api.util.Utils;
import net.moddingmagic.mmcore.config.FormulaConfig;

public final class FormulaEvaluator {

    private FormulaEvaluator() {}

    /**
     * Returns the cast time multiplier for the given raw CAST_TIME_REDUCTION attribute value.
     *
     * A value < 1.0 shortens cast time; > 1.0 lengthens it.
     * Iron's default equivalent: {@code 2 - softCap(x)}.
     *
     * @param attributeValue  raw value of the CAST_TIME_REDUCTION attribute (baseline 1.0)
     * @return multiplier applied to the base spell cast time ticks
     */
    public static double castTimeMultiplier(double attributeValue) {
        FormulaConfig.FormulaType type = FormulaConfig.CAST_TIME_FORMULA.get();

        // SOFTCAP keeps Iron's own inversion (2 - softCap).
        if (type == FormulaConfig.FormulaType.SOFTCAP) {
            return 2.0 - Utils.softCapFormula(attributeValue);
        }

        // CUSTOM: compute a raw value then turn it into a multiplier.
        return 2.0 - evaluate(
                attributeValue,
                type,
                FormulaConfig.CAST_TIME_DIMINISH_START.get(),
                FormulaConfig.CAST_TIME_INTENSITY.get(),
                FormulaConfig.CAST_TIME_OFFSET.get(),
                FormulaConfig.CAST_TIME_CAP.get()
        );
    }

    /**
     * Returns the cooldown multiplier for the given raw COOLDOWN_REDUCTION attribute value.
     *
     * A value < 1.0 shortens cooldowns; > 1.0 lengthens them.
     * Iron's default equivalent: {@code 2 - softCap(x)}.
     *
     * @param attributeValue  raw value of the COOLDOWN_REDUCTION attribute (baseline 1.0)
     * @return multiplier applied to the base spell cooldown ticks
     */
    public static double cooldownMultiplier(double attributeValue) {
        FormulaConfig.FormulaType type = FormulaConfig.COOLDOWN_FORMULA.get();

        // SOFTCAP keeps Iron's own inversion (2 - softCap).
        if (type == FormulaConfig.FormulaType.SOFTCAP) {
            return 2.0 - Utils.softCapFormula(attributeValue);
        }

        // CUSTOM: compute a raw value then turn it into a multiplier.
        return 2.0 - evaluate(
                attributeValue,
                type,
                FormulaConfig.COOLDOWN_DIMINISH_START.get(),
                FormulaConfig.COOLDOWN_INTENSITY.get(),
                FormulaConfig.COOLDOWN_OFFSET.get(),
                FormulaConfig.COOLDOWN_CAP.get()
        );
    }

    /**
     * Returns the spell-resist damage multiplier for the given combined resist value.
     *
     * A value < 1.0 means the entity takes less damage (resistance).
     * A value > 1.0 means more damage (vulnerability).
     * Iron's default equivalent: {@code 2 - softCap(combinedResist)}.
     *
     * @param combinedResist  schoolResist * baseResist  (baseline 1.0 = no resistance)
     * @return damage multiplier applied to incoming spell damage
     */
    public static double resistMultiplier(double combinedResist) {
        FormulaConfig.FormulaType type = FormulaConfig.RESIST_FORMULA.get();

        if (type == FormulaConfig.FormulaType.SOFTCAP) {
            return 2.0 - Utils.softCapFormula(combinedResist);
        }

        return 2.0 - evaluate(
                combinedResist,
                type,
                FormulaConfig.RESIST_DIMINISH_START.get(),
                FormulaConfig.RESIST_INTENSITY.get(),
                FormulaConfig.RESIST_OFFSET.get(),
                FormulaConfig.RESIST_CAP.get()
        );
    }


    /**
     * Generic formula dispatcher.
     */
    private static double evaluate(
            double x,
            FormulaConfig.FormulaType type,
            double dStart,
            double intensity,
            double offset,
            double cap
    ) {
        return switch (type) {
            case SOFTCAP  -> Utils.softCapFormula(x);
            case CUSTOM   -> custom(x, dStart, intensity, offset, cap);
        };
    }

    /**
     * Smooth diminishing-returns hyperbolic curve that begins after dStart.
     * x <= dStart  →  x
     * x >  dStart  →  -intensity / (x - offset) + cap
     * Asymptote at cap.
     */
    private static double custom(double x, double dStart, double intensity, double offset, double cap) {
        if (x <= 0.0) return 0.0;
        return x <= dStart ? x : -intensity * (1 / (x - offset)) + cap;
    }
}
