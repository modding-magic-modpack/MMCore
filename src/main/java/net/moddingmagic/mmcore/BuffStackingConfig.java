package net.moddingmagic.mmcore;

import com.electronwill.nightconfig.core.Config;
import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.List;

public class BuffStackingConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.ConfigValue<List<? extends Config>> RULES;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment(
                "Buff Stacking Prevention Rules",
                "",
                "Add entries under [[mmcore-buff-stacking.rules]] in the config file.",
                "Each entry has three fields:",
                " replace = true/false",
                "    true  -> remove the active conflicting effect, then apply the new one",
                "    false -> cancel the incoming effect entirely",
                " active_effects = [\"namespace:effect\", ...]  -- effects already on the entity",
                " incoming_effects = [\"namespace:effect\", ...]  -- effects about to be applied"
        ).push("rules");

        RULES = builder.defineList(
                "entries",
                // Default rules shown to the user as initial config content
                List.of(
                        makeRule(
                                List.of("minecraft:speed", "minecraft:haste"),
                                List.of("minecraft:strength"),
                                false
                        ),
                        makeRule(
                                List.of("minecraft:regeneration"),
                                List.of("minecraft:poison"),
                                true
                        )
                ),
                // Validator: each element must be a Config with the required keys
                entry -> entry instanceof Config cfg
                        && cfg.contains("active_effects")
                        && cfg.contains("incoming_effects")
                        && cfg.contains("replace")
        );

        builder.pop();
        SPEC = builder.build();
    }

    /** Read the current list of raw rules from the config. */
    public static List<RawRule> getRules() {
        return RULES.get().stream()
                .map(BuffStackingConfig::parseEntry)
                .filter(r -> r != null)
                .toList();
    }

    // Internal helpers

    private static RawRule parseEntry(Config cfg) {
        try {
            List<String> activeEffects = cfg.get("active_effects");
            List<String> incomingEffects = cfg.get("incoming_effects");
            boolean replace = cfg.get("replace");

            if (activeEffects == null || activeEffects.isEmpty()) {
                MMCore.LOGGER.warn("[BuffStacking] Skipping rule with empty 'active_effects'.");
                return null;
            }
            if (incomingEffects == null || incomingEffects.isEmpty()) {
                MMCore.LOGGER.warn("[BuffStacking] Skipping rule with empty 'incoming_effects'.");
                return null;
            }

            return new RawRule(activeEffects, incomingEffects, replace);

        } catch (Exception e) {
            MMCore.LOGGER.warn("[BuffStacking] Skipping malformed rule: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Build a default rule as a night-config Config object.
     */
    private static Config makeRule(
            List<String> activeEffects,
            List<String> incomingEffects,
            boolean replace
    ) {
        Config cfg = Config.inMemory();
        cfg.set("active_effects",   activeEffects);
        cfg.set("incoming_effects", incomingEffects);
        cfg.set("replace",          replace);
        return cfg;
    }

    // Raw rule — effect IDs as strings, resolved later by BuffStackingRule

    public record RawRule(
            List<String> activeEffects,
            List<String> incomingEffects,
            boolean replace
    ) {}
}
