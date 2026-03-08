package net.moddingmagic.mmcore.buffstacking;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class BuffStackingManager {

    private static List<BuffStackingRule> rules = List.of();

    /** Re-resolve rules from the current config values. */
    public static void reload() {
        rules = BuffStackingRule.resolveAll();
    }

    public static RuleResult evaluate(LivingEntity entity, MobEffectInstance incoming) {
        MobEffect incomingType = incoming.getEffect().value();

        for (MobEffectInstance activeInstance : entity.getActiveEffects()) {
            MobEffect activeType = activeInstance.getEffect().value();

            for (BuffStackingRule rule : rules) {
                if (rule.matches(activeType, incomingType)) {
                    return rule.replace()
                            ? RuleResult.replace(activeInstance)
                            : RuleResult.cancel();
                }
            }
        }

        return RuleResult.allow();
    }

    public enum Action { ALLOW, CANCEL, REPLACE }

    public record RuleResult(Action action, MobEffectInstance effectToRemove) {

        public static RuleResult allow()   { return new RuleResult(Action.ALLOW, null); }
        public static RuleResult cancel()  { return new RuleResult(Action.CANCEL, null); }
        public static RuleResult replace(MobEffectInstance toRemove) {
            return new RuleResult(Action.REPLACE, toRemove);
        }
    }
}
