package mike.autototem.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

@Mixin(MinecraftClient.class)
public class OnTotemPopMixin {
    private final KeyBinding totemKey = new KeyBinding("key.autototem.swap", GLFW.GLFW_KEY_T, "key.categories.gameplay");

    @Inject(at = @At("TAIL"), method = "tick")
    private void onTick(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.currentScreen != null) return;

        if (totemKey.wasPressed()) {
            swapTotemToOffhand(client.player);
        }
    }

    private void swapTotemToOffhand(PlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        ClientPlayNetworkHandler networkHandler = MinecraftClient.getInstance().getNetworkHandler();
        if (networkHandler == null) return;

        int totemSlot = getTotemSlot(inventory);
        if (totemSlot == -1) {
            System.out.println("No totem found in inventory!");
            return;
        }

        if (totemSlot < 9) {
            // Hotbar to Offhand
            networkHandler.sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                BlockPos.ORIGIN, Direction.DOWN
            ));
        } else {
            // Move to selected slot first
            ScreenHandler screenHandler = player.currentScreenHandler;
            int selectedSlot = inventory.selectedSlot;

            networkHandler.sendPacket(new ClickSlotC2SPacket(
                screenHandler.syncId,
                screenHandler.getRevision(),
                totemSlot,
                selectedSlot,
                SlotActionType.SWAP,
                inventory.getStack(selectedSlot).copy(),
                new Int2ObjectOpenHashMap<>()
            ));

            // Swap from hotbar to offhand
            networkHandler.sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                BlockPos.ORIGIN, Direction.DOWN
            ));
        }

        System.out.println("Totem swapped to offhand!");
    }

    private int getTotemSlot(PlayerInventory inventory) {
        for (int i = 0; i < inventory.main.size(); i++) {
            if (!inventory.main.get(i).isEmpty() && inventory.main.get(i).isOf(Items.TOTEM_OF_UNDYING)) {
                return i;
            }
        }
        return -1;
    }
            }
