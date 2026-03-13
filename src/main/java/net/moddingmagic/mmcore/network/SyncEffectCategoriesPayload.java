package net.moddingmagic.mmcore.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.moddingmagic.mmcore.MMCore;
import net.moddingmagic.mmcore.effect_categories.EffectCategory;

import java.util.ArrayList;
import java.util.List;

/**
 * Sent from server -> client to sync the effect_categories.json data.
 */
public record SyncEffectCategoriesPayload(List<EffectCategory> categories) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncEffectCategoriesPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MMCore.MODID, "sync_effect_categories"));

    public static final StreamCodec<FriendlyByteBuf, SyncEffectCategoriesPayload> STREAM_CODEC =
            StreamCodec.of(SyncEffectCategoriesPayload::encode, SyncEffectCategoriesPayload::decode);

    private static void encode(FriendlyByteBuf buf, SyncEffectCategoriesPayload payload) {
        buf.writeVarInt(payload.categories.size());
        for (EffectCategory cat : payload.categories) {
            buf.writeUtf(cat.getTag());
            buf.writeUtf(cat.getName());
            buf.writeInt(cat.getColor());
        }
    }

    private static SyncEffectCategoriesPayload decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<EffectCategory> categories = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String tag   = buf.readUtf();
            String name  = buf.readUtf();
            int color    = buf.readInt();
            categories.add(new EffectCategory(tag, name, color));
        }
        return new SyncEffectCategoriesPayload(categories);
    }

    @Override
    public CustomPacketPayload.Type<SyncEffectCategoriesPayload> type() {
        return TYPE;
    }
}
