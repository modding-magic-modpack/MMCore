package net.moddingmagic.mmcore.config;

import net.moddingmagic.mmcore.MMCore;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.*;

public class CurioRemapConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.ConfigValue<List<? extends String>> REMAPS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.comment(
                "Curio Slot Remapper",
                "Allows curio items from other mods to work in different curio slots",
                "while still applying their attribute bonuses correctly.",
                "",
                "Each entry follows the format:",
                "  \"modid:item_name | original_slot | target_slot1, target_slot2, ...\"",
                "",
                "  modid:item_name  — the registry ID of the item to remap",
                "  original_slot   — the slot the item was originally coded for (e.g. 'ring')",
                "  target_slots    — one or more slots to also make it work in (comma-separated)",
                "",
                "Examples:",
                "  \"irons_spellbooks:mana_ring | ring | necklace\"",
                "  \"irons_spellbooks:fire_pendant | necklace | ring, bracelet\"",
                "",
                "IMPORTANT: You still need datapack tag files to allow the item to be",
                "physically placed in the target slots. This config only handles",
                "attribute injection and tooltip correction."
        ).push("remaps");

        REMAPS = b.comment("List of curio slot remapping entries.")
                .defineListAllowEmpty(
                        "entries",
                        List.of(
                                "irons_spellbooks:mana_ring | ring | necklace"
                        ),
                        entry -> entry instanceof String s && isValidEntry(s)
                );

        b.pop();

        SPEC = b.build();
    }



    // Parsed remap data

    public record SlotRemap(String originalSlot, Set<String> targetSlots) {}

    private static final Map<String, SlotRemap> REMAP_TABLE = new HashMap<>();

    /**
     * Parses the raw config strings into {@link SlotRemap} records.
     */
    public static void buildRemapTable() {
        REMAP_TABLE.clear();

        for (String raw : REMAPS.get()) {
            String[] parts = raw.split("\\|");
            if (parts.length != 3) {
                MMCore.LOGGER.warn("Skipping malformed entry (expected 3 pipe-separated parts): '{}'", raw);
                continue;
            }

            String itemId = parts[0].trim();
            String originalSlot = parts[1].trim();
            String[] targets = parts[2].split(",");

            if (itemId.isEmpty() || originalSlot.isEmpty() || targets.length == 0) {
                MMCore.LOGGER.warn("Skipping empty entry: '{}'", raw);
                continue;
            }

            Set<String> targetSlots = new LinkedHashSet<>();
            for (String t : targets) {
                String slot = t.trim();
                if (!slot.isEmpty()) targetSlots.add(slot);
            }

            if (targetSlots.isEmpty()) {
                MMCore.LOGGER.warn("Skipping entry with no valid target slots: '{}'", raw);
                continue;
            }

            REMAP_TABLE.put(itemId, new SlotRemap(originalSlot, Collections.unmodifiableSet(targetSlots)));
            MMCore.LOGGER.debug("Registered remap: {} | {} -> {}", itemId, originalSlot, targetSlots);
        }

        MMCore.LOGGER.debug("Loaded {} curio slot remap(s).", REMAP_TABLE.size());
    }

    /** Returns the remap for the given item registry ID, if one is configured. */
    public static Optional<SlotRemap> getRemap(String itemId) {
        return Optional.ofNullable(REMAP_TABLE.get(itemId));
    }

    /** Returns all registered remaps. */
    public static Map<String, SlotRemap> getAllRemaps() {
        return Collections.unmodifiableMap(REMAP_TABLE);
    }

    public static void applyServerRemaps(Map<String, SlotRemap> serverRemaps) {
        REMAP_TABLE.clear();
        REMAP_TABLE.putAll(serverRemaps);
    }

    private static boolean isValidEntry(String s) {
        if (s == null || s.isBlank()) return false;
        String[] parts = s.split("\\|");
        if (parts.length != 3) return false;
        String itemId = parts[0].trim();
        return itemId.contains(":") && !parts[1].isBlank() && !parts[2].isBlank();
    }
}
