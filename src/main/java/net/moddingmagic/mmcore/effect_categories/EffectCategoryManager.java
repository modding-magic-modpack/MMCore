package net.moddingmagic.mmcore.effect_categories;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.moddingmagic.mmcore.MMCore;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Loads the file data/mmcore/effect_categories.json.
 */
public class EffectCategoryManager implements PreparableReloadListener {

    public static final EffectCategoryManager INSTANCE = new EffectCategoryManager();

    private static final ResourceLocation DATA_FILE =
            ResourceLocation.fromNamespaceAndPath(MMCore.MODID, "effect_categories.json");

    private List<EffectCategory> categories = Collections.emptyList();

    private Map<Holder<MobEffect>, List<EffectCategory>> effectToCategoriesCache = new HashMap<>();
    private boolean cacheBuilt = false;

    private EffectCategoryManager() {}

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier pBarrier,
                                          ResourceManager pResourceManager,
                                          ProfilerFiller pPreparationsProfiler,
                                          ProfilerFiller pReloadProfiler,
                                          Executor pBackgroundExecutor,
                                          Executor pGameExecutor) {

        CompletableFuture<List<EffectCategory>> prepareFuture =
                CompletableFuture.supplyAsync(() -> loadFromDisk(pResourceManager), pBackgroundExecutor);

        return prepareFuture
                .thenCompose(pBarrier::wait)
                .thenAcceptAsync(loaded -> {
                    this.categories = Collections.unmodifiableList(loaded);
                    this.effectToCategoriesCache = new HashMap<>();
                    this.cacheBuilt = false;
                    MMCore.LOGGER.debug("Applied {} effect categories.", loaded.size());
                }, pGameExecutor);
    }

    private List<EffectCategory> loadFromDisk(ResourceManager resourceManager) {
        List<EffectCategory> loaded = new ArrayList<>();

        Optional<Resource> resource = resourceManager.getResource(DATA_FILE);
        if (resource.isEmpty()) {
            MMCore.LOGGER.warn("MMCore effect_categories.json not found at '{}'.", DATA_FILE);
            return loaded;
        }

        try (Reader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray entries = root.getAsJsonArray("entries");

            if (entries == null) {
                MMCore.LOGGER.warn("MMCore effect_categories.json has no 'entries' array.");
                return loaded;
            }

            for (JsonElement el : entries) {
                JsonObject obj = el.getAsJsonObject();
                String tag   = obj.get("tag").getAsString();
                String name  = obj.get("name").getAsString();
                String color = obj.has("color") ? obj.get("color").getAsString() : "#FFFFFF";
                loaded.add(new EffectCategory(tag, name, EffectCategory.parseColor(color)));
            }

            MMCore.LOGGER.debug("Loaded {} entries from effect_categories.json.", loaded.size());
        } catch (Exception e) {
            MMCore.LOGGER.error("Failed to parse effect_categories.json: {}", e.getMessage(), e);
        }

        return loaded;
    }

    public List<EffectCategory> getCategoriesForEffect(Holder<MobEffect> effectHolder) {
        if (!cacheBuilt) {
            buildCache();
        }
        return effectToCategoriesCache.getOrDefault(effectHolder, Collections.emptyList());
    }

    public List<EffectCategory> getCategories() {
        return categories;
    }

    public void applyFromNetwork(List<EffectCategory> received) {
        applyCategories(received);
        MMCore.LOGGER.debug("Received {} effect categories from server.", received.size());
    }

    private void applyCategories(List<EffectCategory> loaded) {
        this.categories = Collections.unmodifiableList(loaded);
        this.effectToCategoriesCache = new HashMap<>();
        this.cacheBuilt = false;
    }

    private void buildCache() {
        Map<Holder<MobEffect>, List<EffectCategory>> cache = new HashMap<>();

        for (EffectCategory category : categories) {
            ResourceLocation tagId = ResourceLocation.tryParse(category.getTag());
            if (tagId == null) {
                MMCore.LOGGER.warn("Invalid tag '{}' in category '{}', skipping.",
                        category.getTag(), category.getName());
                continue;
            }

            TagKey<MobEffect> tagKey = TagKey.create(Registries.MOB_EFFECT, tagId);

            for (Holder<MobEffect> holder : BuiltInRegistries.MOB_EFFECT.getTagOrEmpty(tagKey)) {
                cache.computeIfAbsent(holder, k -> new ArrayList<>()).add(category);
            }
        }

        cache.replaceAll((k, v) -> Collections.unmodifiableList(v));
        this.effectToCategoriesCache = Collections.unmodifiableMap(cache);
        this.cacheBuilt = true;
    }
}
