package net.moddingmagic.mmcore.event;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.effect.MobEffect;
import net.moddingmagic.mmcore.effect_categories.EffectCategory;
import net.moddingmagic.mmcore.effect_categories.EffectCategoryManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.GatherEffectScreenTooltipsEvent;

import java.util.List;

@EventBusSubscriber(value = Dist.CLIENT)
public class EffectTooltipHandler {

    @SubscribeEvent
    public static void onGatherEffectTooltips(GatherEffectScreenTooltipsEvent event) {
        Holder<MobEffect> effectHolder = event.getEffectInstance().getEffect();

        List<EffectCategory> categories = EffectCategoryManager.INSTANCE.getCategoriesForEffect(effectHolder);
        if (categories.isEmpty()) {
            return;
        }

        for (EffectCategory cat : categories) {
            int rgb = cat.getColor() & 0x00FFFFFF;
            event.getTooltip().add(
                    Component.literal(cat.getName())
                            .withStyle(Style.EMPTY.withColor(rgb))
            );
        }
    }
}
