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
            List<String> slotsToTry = new ArrayList<>();

            CurioRemapConfig.getRemap(itemId).ifPresent(remap ->
                    slotsToTry.addAll(remap.targetSlots())
            );

            CuriosApi.getItemStackSlots(stack, player).keySet().forEach(slotId -> {
                if (!slotsToTry.contains(slotId)) {
                    slotsToTry.add(slotId);
                }
            });

            var curiosMap = inv.getCurios();

            // Track the first valid slot we find for swapping, in case no empty slot exists
            String swapSlotId = null;
            int swapIndex = -1;

            for (String slotId : slotsToTry) {
                var stacksHandler = curiosMap.get(slotId);
                if (stacksHandler == null) continue;

                var stacks = stacksHandler.getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    SlotContext ctx = new SlotContext(slotId, player, i, false, false);
                    if (!curioItem.canEquip(ctx, stack)) continue;

                    if (stacks.getStackInSlot(i).isEmpty()) {
                        // Found an empty slot — equip directly, no need to swap
                        ItemStack toEquip = stack.copyWithCount(1);
                        stacks.setStackInSlot(i, toEquip);
                        stack.shrink(1);
                        playEquipSound(curioItem, ctx, toEquip, player);
                        event.setCanceled(true);
                        event.setCancellationResult(InteractionResult.sidedSuccess(false));
                        return;
                    }

                    // Remember the first occupied valid slot for swapping
                    if (swapSlotId == null) {
                        swapSlotId = slotId;
                        swapIndex = i;
                    }
                }
            }

            // No empty slot found — swap with the first valid occupied slot
            if (swapSlotId != null) {
                var stacksHandler = curiosMap.get(swapSlotId);
                if (stacksHandler == null) return;

                var stacks = stacksHandler.getStacks();
                SlotContext ctx = new SlotContext(swapSlotId, player, swapIndex, false, false);

                ItemStack existing = stacks.getStackInSlot(swapIndex).copy();
                ItemStack toEquip = stack.copyWithCount(1);

                // Unequip the existing item first so its attributes are removed cleanly
                if (stacks.getStackInSlot(swapIndex).getItem() instanceof ICurioItem existingCurio) {
                    existingCurio.onUnequip(ctx, existing, stacks.getStackInSlot(swapIndex));
                }

                stacks.setStackInSlot(swapIndex, toEquip);
                stack.shrink(1);

                // Give the unequipped item back to the player's inventory, or drop it if inventory is full
                if (!player.getInventory().add(existing)) {
                    player.drop(existing, false);
                }

                playEquipSound(curioItem, ctx, toEquip, player);
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.sidedSuccess(false));
            }
        });
    }

    private static void playEquipSound(ICurioItem curioItem, SlotContext ctx,
                                       ItemStack stack, Player player) {
        ICurio.SoundInfo sound = curioItem.getEquipSound(ctx, stack);
        player.level().playSound(
                null,
                player.blockPosition(),
                sound.getSoundEvent(),
                SoundSource.PLAYERS,
                sound.getVolume(),
                sound.getPitch()
        );
    }
}
