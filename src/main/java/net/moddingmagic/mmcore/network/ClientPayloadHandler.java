package net.moddingmagic.mmcore.network;

import net.moddingmagic.mmcore.config.CurioRemapConfig;
import net.moddingmagic.mmcore.effect_categories.EffectCategoryManager;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Handles the SyncEffectCategoriesPayload on the client.
 * Receives the server's loaded categories and applies them to the client-side EffectCategoryManager instance.
 */
public class ClientPayloadHandler {

    public static void handleSyncEffectCategories(SyncEffectCategoriesPayload payload, IPayloadContext context) {
        context.enqueueWork(() ->
                EffectCategoryManager.INSTANCE.applyFromNetwork(payload.categories())
        );
    }

    public static void handleCurioRemapSync(SyncCurioRemapsPayload payload, IPayloadContext context) {
        context.enqueueWork(() ->
                CurioRemapConfig.applyServerRemaps(payload.remaps())
        );
    }
}
