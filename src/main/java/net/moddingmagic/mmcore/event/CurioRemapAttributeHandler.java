package net.moddingmagic.mmcore.event;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.moddingmagic.mmcore.MMCore;
import net.moddingmagic.mmcore.config.CurioRemapConfig;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.event.CurioAttributeModifierEvent;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import com.google.common.collect.Multimap;

@EventBusSubscriber(modid = MMCore.MODID)
public class CurioRemapAttributeHandler {

    @SubscribeEvent
    public static void onCurioAttributeModifiers(CurioAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        SlotContext slotContext = event.getSlotContext();
        String currentSlot = slotContext.identifier();

        ResourceLocation itemId = stack.getItemHolder().unwrapKey()
                .map(k -> k.location())
                .orElse(null);
        if (itemId == null) return;

        CurioRemapConfig.getRemap(itemId.toString()).ifPresent(remap -> {
            if (currentSlot.equals(remap.originalSlot())) return;
            if (!remap.targetSlots().contains(currentSlot)) return;
            if (!(stack.getItem() instanceof ICurioItem curioItem)) return;

            SlotContext spoofedContext = new SlotContext(
                    remap.originalSlot(),
                    slotContext.entity(),
                    slotContext.index(),
                    slotContext.visible(),
                    slotContext.cosmetic()
            );

            ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath(
                    MMCore.MODID,
                    currentSlot + "_" + slotContext.index()
            );

            Multimap<Holder<Attribute>, AttributeModifier> modifiers =
                    curioItem.getAttributeModifiers(spoofedContext, modifierId, stack);

            modifiers.forEach((attribute, modifier) -> {
                event.addModifier(
                        attribute,
                        new AttributeModifier(
                                modifierId,
                                modifier.amount(),
                                modifier.operation()
                        )
                );
            });
        });
    }
}
