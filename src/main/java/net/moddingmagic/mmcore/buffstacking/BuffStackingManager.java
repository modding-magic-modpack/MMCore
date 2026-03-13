package net.moddingmagic.mmcore.buffstacking;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class BuffStackingManager {

    private static List<BuffStackingRule>  effectRules = List.of();
    private static List<SpellStackingRule> spellRules  = List.of();

    public static void applyRules(List<BuffStackingRule> newEffectRules,
                                  List<SpellStackingRule> newSpellRules) {
        effectRules = List.copyOf(newEffectRules);
        spellRules  = List.copyOf(newSpellRules);
    }

    // Effect evaluation

    public static RuleResult evaluateEffect(LivingEntity entity, MobEffectInstance incoming) {
        MobEffect incomingType = incoming.getEffect().value();

        for (MobEffectInstance activeInstance : entity.getActiveEffects()) {
            MobEffect activeType = activeInstance.getEffect().value();

            if (activeType == incomingType) continue;

            for (BuffStackingRule rule : effectRules) {
                if (rule.matches(activeType, incomingType)) {
                    return rule.replace()
                            ? RuleResult.replace(activeInstance)
                            : RuleResult.cancel();
                }
            }
        }
        return RuleResult.allow();
    }

    // Spell evaluation

    public static RuleResult evaluateSpell(LivingEntity caster, ResourceLocation spellId) {
        AbstractSpell spell = SpellRegistry.getSpell(spellId);
        for (MobEffectInstance activeInstance : caster.getActiveEffects()) {
            MobEffect activeType = activeInstance.getEffect().value();

            for (SpellStackingRule rule : spellRules) {
                if (rule.matches(activeType, spell)) {
                    return rule.replace()
                            ? RuleResult.replace(activeInstance)
                            : RuleResult.cancel();
                }
            }
        }
        return RuleResult.allow();
    }

    // Result type

    public enum Action { ALLOW, CANCEL, REPLACE }

    public record RuleResult(Action action, MobEffectInstance effectToRemove) {

        public static RuleResult allow()   { return new RuleResult(Action.ALLOW, null); }
        public static RuleResult cancel()  { return new RuleResult(Action.CANCEL, null); }
        public static RuleResult replace(MobEffectInstance toRemove) {
            return new RuleResult(Action.REPLACE, toRemove);
        }
    }
}
