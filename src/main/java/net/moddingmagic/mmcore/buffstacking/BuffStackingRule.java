package net.moddingmagic.mmcore.buffstacking;

import net.minecraft.world.effect.MobEffect;
import java.util.List;

public record BuffStackingRule(
        List<MobEffect> activeEffects,
        List<MobEffect> incomingEffects,
        boolean replace
) {
    public boolean matches(MobEffect active, MobEffect incoming) {
        return activeEffects.contains(active) && incomingEffects.contains(incoming);
    }
}
