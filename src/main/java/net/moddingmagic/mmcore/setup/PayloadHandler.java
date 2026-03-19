package net.moddingmagic.mmcore.setup;

import net.moddingmagic.mmcore.MMCore;
import net.moddingmagic.mmcore.network.ClientPayloadHandler;
import net.moddingmagic.mmcore.network.SyncCurioRemapsPayload;
import net.moddingmagic.mmcore.network.SyncEffectCategoriesPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, modid = MMCore.MODID)
public class PayloadHandler {
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar payloadRegistrar = event.registrar(MMCore.MODID).versioned("1.0.0").optional();

        payloadRegistrar.playToClient(
                SyncEffectCategoriesPayload.TYPE,
                SyncEffectCategoriesPayload.STREAM_CODEC,
                ClientPayloadHandler::handleSyncEffectCategories
        );

        payloadRegistrar.playToClient(
                SyncCurioRemapsPayload.TYPE,
                SyncCurioRemapsPayload.STREAM_CODEC,
                ClientPayloadHandler::handleCurioRemapSync
        );
    }
}
