package net.moddingmagic.mmcore.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.moddingmagic.mmcore.MMCore;
import net.moddingmagic.mmcore.config.CurioRemapConfig.SlotRemap;

import java.util.*;

public record SyncCurioRemapsPayload(Map<String, SlotRemap> remaps) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncCurioRemapsPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MMCore.MODID, "sync_curio_remaps"));

    public static final StreamCodec<FriendlyByteBuf, SyncCurioRemapsPayload> STREAM_CODEC =
            StreamCodec.of(SyncCurioRemapsPayload::encode, SyncCurioRemapsPayload::decode);

    private static void encode(FriendlyByteBuf buf, SyncCurioRemapsPayload payload) {
        buf.writeVarInt(payload.remaps().size());
        payload.remaps().forEach((itemId, remap) -> {
            buf.writeUtf(itemId);
            buf.writeUtf(remap.originalSlot());
            buf.writeVarInt(remap.targetSlots().size());
            remap.targetSlots().forEach(buf::writeUtf);
        });
    }

    private static SyncCurioRemapsPayload decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        Map<String, SlotRemap> remaps = new LinkedHashMap<>(count);
        for (int i = 0; i < count; i++) {
            String itemId = buf.readUtf();
            String originalSlot = buf.readUtf();
            int slotCount = buf.readVarInt();
            Set<String> targets = new LinkedHashSet<>(slotCount);
            for (int j = 0; j < slotCount; j++) {
                targets.add(buf.readUtf());
            }
            remaps.put(itemId, new SlotRemap(originalSlot, Collections.unmodifiableSet(targets)));
        }
        return new SyncCurioRemapsPayload(Collections.unmodifiableMap(remaps));
    }

    @Override
    public CustomPacketPayload.Type<SyncCurioRemapsPayload> type() {
        return TYPE;
    }
}
