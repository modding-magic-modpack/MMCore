package net.moddingmagic.mmcore.buffstacking;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.effect.MobEffect;
import java.util.List;

public record SpellStackingRule(
        List<MobEffect> activeEffects,
        List<AbstractSpell> blockedSpells,
        boolean replace
) {
    public boolean matches(MobEffect activeEffect, AbstractSpell castingSpell) {
        return activeEffects.contains(activeEffect) && blockedSpells.contains(castingSpell);
    }
}
