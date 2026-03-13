package net.moddingmagic.mmcore;

import net.moddingmagic.mmcore.buffstacking.BuffStackingManager;
import net.moddingmagic.mmcore.buffstacking.BuffStackingRulesLoader;
import net.moddingmagic.mmcore.config.FormulaConfig;
import net.moddingmagic.mmcore.effect_categories.EffectCategoryManager;
import net.moddingmagic.mmcore.network.SyncEffectCategoriesPayload;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(MMCore.MODID)
public class MMCore {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "mmcore";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public MMCore(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, FormulaConfig.SPEC, "mmcore-formulas.toml");
    }

    private void commonSetup(FMLCommonSetupEvent event) {

    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {

    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(EffectCategoryManager.INSTANCE);
        event.addListener(BuffStackingRulesLoader.INSTANCE);
    }

    @SubscribeEvent
    public void onDatapackSync(OnDatapackSyncEvent event) {
        SyncEffectCategoriesPayload payload =
                new SyncEffectCategoriesPayload(EffectCategoryManager.INSTANCE.getCategories());

        if (event.getPlayer() != null) {
            // Single player just logged in
            PacketDistributor.sendToPlayer(event.getPlayer(), payload);
        } else {
            // Full /reload — send to all online players
            PacketDistributor.sendToAllPlayers(payload);
        }
    }
}
