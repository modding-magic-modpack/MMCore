package net.moddingmagic.mmcore.event;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.moddingmagic.mmcore.MMCore;
import net.moddingmagic.mmcore.config.CurioRemapConfig;
import net.moddingmagic.mmcore.util.MMCoreTags;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = MMCore.MODID)
public class CurioEquipHandler {

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

        if (!stack.is(MMCoreTags.EQUIP_TO_CURIO)) return;
        if (!(stack.getItem() instanceof ICurioItem curioItem)) return;
        if (player.level().isClientSide()) return;

        String itemId = stack.getItemHolder().unwrapKey()
                .map(k -> k.location().toString())
                .orElse(null);
        if (itemId == null) return;

        CuriosApi.getCuriosInventory(player).ifPresent(inv -> {
            // Build slot priority list:
            // 1. Remapped target slots first (if configured)
            // 2. Native slots from Curios tag system
            List<String> slotsToTry = new ArrayList<>();

            CurioRemapConfig.getRemap(itemId).ifPresent(remap ->
                    slotsToTry.addAll(remap.targetSlots())
            );

            // getItemStackSlots returns Map<String, ISlotType>
            CuriosApi.getItemStackSlots(stack, player).keySet().forEach(slotId -> {
                if (!slotsToTry.contains(slotId)) {
                    slotsToTry.add(slotId);
                }
            });

            // inv.getCurios() returns Map<String, ICurioStacksHandler>
            var curiosMap = inv.getCurios();

            for (String slotId : slotsToTry) {
                var stacksHandler = curiosMap.get(slotId);
                if (stacksHandler == null) continue;

                // getStacks() returns IItemHandlerModifiable
                var stacks = stacksHandler.getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    if (!stacks.getStackInSlot(i).isEmpty()) continue;

                    SlotContext ctx = new SlotContext(slotId, player, i, false, false);

                    if (!curioItem.canEquip(ctx, stack)) continue;

                    ItemStack toEquip = stack.copyWithCount(1);
                    stacks.setStackInSlot(i, toEquip);
                    stack.shrink(1);

                    ICurio.SoundInfo sound = curioItem.getEquipSound(ctx, toEquip);
                    player.level().playSound(
                            null,
                            player.blockPosition(),
                            sound.getSoundEvent(),
                            SoundSource.PLAYERS,
                            sound.getVolume(),
                            sound.getPitch()
                    );

                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.sidedSuccess(false));
                    return;
                }
            }
        });
    }
}
