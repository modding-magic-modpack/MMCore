package net.moddingmagic.mmcore.buffstacking;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.effect.MobEffect;
import net.moddingmagic.mmcore.MMCore;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class BuffStackingRulesLoader implements PreparableReloadListener {

    public static final BuffStackingRulesLoader INSTANCE = new BuffStackingRulesLoader();

    private static final ResourceLocation DATA_FILE =
            ResourceLocation.fromNamespaceAndPath(MMCore.MODID, "buff_stacking_rules.json");

    private BuffStackingRulesLoader() {}

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier barrier,
                                          ResourceManager resourceManager,
                                          ProfilerFiller preparationsProfiler,
                                          ProfilerFiller reloadProfiler,
                                          Executor backgroundExecutor,
                                          Executor gameExecutor) {
        CompletableFuture<ParsedRules> parseFuture =
                CompletableFuture.supplyAsync(() -> parseFromDisk(resourceManager), backgroundExecutor);

        return parseFuture
                .thenCompose(barrier::wait)
                .thenAcceptAsync(parsed -> {
                    // Resolve registries on the main thread
                    List<BuffStackingRule> effectRules = resolveEffectRules(parsed.rawEffectRules());
                    List<SpellStackingRule> spellRules = resolveSpellRules(parsed.rawSpellRules());
                    BuffStackingManager.applyRules(effectRules, spellRules);
                    MMCore.LOGGER.debug("Applied {} effect rule(s) and {} spell rule(s).",
                            effectRules.size(), spellRules.size());
                }, gameExecutor);
    }

    private ParsedRules parseFromDisk(ResourceManager resourceManager) {
        List<RawEffectRule> effectRules = new ArrayList<>();
        List<RawSpellRule> spellRules   = new ArrayList<>();

        Optional<Resource> resource = resourceManager.getResource(DATA_FILE);
        if (resource.isEmpty()) {
            MMCore.LOGGER.warn("MMCore buff_stacking_rules.json not found at '{}'.", DATA_FILE);
            return new ParsedRules(effectRules, spellRules);
        }

        try (Reader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            if (root.has("effect_rules")) {
                for (JsonElement el : root.getAsJsonArray("effect_rules")) {
                    JsonObject obj = el.getAsJsonObject();
                    List<String> active   = jsonArrayToStrings(obj.getAsJsonArray("active_effects"));
                    List<String> incoming = jsonArrayToStrings(obj.getAsJsonArray("incoming_effects"));
                    boolean replace       = obj.get("replace").getAsBoolean();
                    effectRules.add(new RawEffectRule(active, incoming, replace));
                }
            }

            if (root.has("spell_rules")) {
                for (JsonElement el : root.getAsJsonArray("spell_rules")) {
                    JsonObject obj = el.getAsJsonObject();
                    List<String> active  = jsonArrayToStrings(obj.getAsJsonArray("active_effects"));
                    List<String> spells  = jsonArrayToStrings(obj.getAsJsonArray("blocked_spells"));
                    boolean replace      = obj.get("replace").getAsBoolean();
                    spellRules.add(new RawSpellRule(active, spells, replace));
                }
            }

        } catch (Exception e) {
            MMCore.LOGGER.error("Failed to parse buff_stacking_rules.json: {}", e.getMessage(), e);
        }

        return new ParsedRules(effectRules, spellRules);
    }

    private List<BuffStackingRule> resolveEffectRules(List<RawEffectRule> raws) {
        List<BuffStackingRule> resolved = new ArrayList<>();
        for (RawEffectRule raw : raws) {
            List<MobEffect> active   = resolveEffects(raw.activeEffects(),   "active_effects");
            List<MobEffect> incoming = resolveEffects(raw.incomingEffects(), "incoming_effects");
            if (active.isEmpty() || incoming.isEmpty()) {
                MMCore.LOGGER.warn("Skipping effect rule with no resolved effects: {}", raw);
                continue;
            }
            resolved.add(new BuffStackingRule(active, incoming, raw.replace()));
        }
        MMCore.LOGGER.debug("Resolved {} effect rule(s).", resolved.size());
        return resolved;
    }

    private List<SpellStackingRule> resolveSpellRules(List<RawSpellRule> raws) {
        List<SpellStackingRule> resolved = new ArrayList<>();
        for (RawSpellRule raw : raws) {
            List<MobEffect> active      = resolveEffects(raw.activeEffects(), "active_effects");
            List<AbstractSpell> spells  = resolveSpells(raw.blockedSpells());
            if (active.isEmpty() || spells.isEmpty()) {
                MMCore.LOGGER.warn("Skipping spell rule with no resolved entries: {}", raw);
                continue;
            }
            resolved.add(new SpellStackingRule(active, spells, raw.replace()));
        }
        MMCore.LOGGER.debug("Resolved {} spell rule(s).", resolved.size());
        return resolved;
    }

    private List<MobEffect> resolveEffects(List<String> ids, String fieldName) {
        List<MobEffect> effects = new ArrayList<>();
        for (String id : ids) {
            ResourceLocation rl = ResourceLocation.tryParse(id.trim());
            if (rl == null) {
                MMCore.LOGGER.warn("'{}' in {} is not a valid ResourceLocation.", id, fieldName);
                continue;
            }
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(rl);
            if (effect == null) {
                MMCore.LOGGER.warn("Unknown effect '{}' in {}", id, fieldName);
            } else {
                effects.add(effect);
            }
        }
        return effects;
    }

    private List<AbstractSpell> resolveSpells(List<String> ids) {
        List<AbstractSpell> spells = new ArrayList<>();
        for (String id : ids) {
            ResourceLocation rl = ResourceLocation.tryParse(id.trim());
            if (rl == null) {
                MMCore.LOGGER.warn("'{}' in blocked_spells is not a valid ResourceLocation.", id);
                continue;
            }
            AbstractSpell spell = SpellRegistry.getSpell(rl);
            if (spell == null || spell == SpellRegistry.none()) {
                MMCore.LOGGER.warn("Unknown spell '{}' in blocked_spells", id);
            } else {
                spells.add(spell);
            }
        }
        return spells;
    }

    private static List<String> jsonArrayToStrings(JsonArray array) {
        List<String> list = new ArrayList<>();
        for (JsonElement el : array) list.add(el.getAsString());
        return list;
    }

    private record RawEffectRule(List<String> activeEffects, List<String> incomingEffects, boolean replace) {}
    private record RawSpellRule(List<String> activeEffects, List<String> blockedSpells, boolean replace) {}
    private record ParsedRules(List<RawEffectRule> rawEffectRules, List<RawSpellRule> rawSpellRules) {}
}
