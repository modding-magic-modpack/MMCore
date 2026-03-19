package net.moddingmagic.mmcore.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.moddingmagic.mmcore.MMCore;

public class MMCoreTags {
    public static final TagKey<Item> EQUIP_TO_CURIO = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MMCore.MODID, "equip_to_curio")
    );
}
