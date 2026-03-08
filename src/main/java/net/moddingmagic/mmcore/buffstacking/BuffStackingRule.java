package net.moddingmagic.mmcore.buffstacking;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.moddingmagic.mmcore.BuffStackingConfig;
import net.moddingmagic.mmcore.MMCore;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record BuffStackingRule(
        List<MobEffect> activeEffects,
        List<MobEffect> incomingEffects,
        boolean replace
) {
    public static List<BuffStackingRule> resolveAll() {
        List<BuffStackingRule> resolved = new ArrayList<>();
        for (BuffStackingConfig.RawRule raw : BuffStackingConfig.getRules()) {
            resolve(raw).ifPresent(resolved::add);
        }
        MMCore.LOGGER.debug("[BuffStacking] Resolved {} valid rule(s).", resolved.size());
        return resolved;
    }

    private static Optional<BuffStackingRule> resolve(BuffStackingConfig.RawRule raw) {
        List<MobEffect> active   = resolveEffects(raw.activeEffects(),   "active_effects");
        List<MobEffect> incoming = resolveEffects(raw.incomingEffects(), "incoming_effects");

        if (active.isEmpty() || incoming.isEmpty()) {
            MMCore.LOGGER.warn("[BuffStacking] Skipping rule — no valid effects resolved. Raw: {}", raw);
            return Optional.empty();
        }

        return Optional.of(new BuffStackingRule(active, incoming, raw.replace()));
    }

    private static List<MobEffect> resolveEffects(List<String> ids, String fieldName) {
        List<MobEffect> effects = new ArrayList<>();
        for (String id : ids) {
            ResourceLocation rl = ResourceLocation.tryParse(id.trim());
            if (rl == null) {
                MMCore.LOGGER.warn("[BuffStacking] '{}' in {} is not a valid ResourceLocation.", id, fieldName);
                continue;
            }
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(rl);
            if (effect == null) {
                MMCore.LOGGER.warn("[BuffStacking] Unknown effect '{}' in {}", id, fieldName);
            } else {
                effects.add(effect);
            }
        }
        return effects;
    }

    public boolean matches(MobEffect active, MobEffect incoming) {
        return activeEffects.contains(active) && incomingEffects.contains(incoming);
    }
}
