package com.yardenzamir.tooltipsreforgeddyed;

import com.iafenvoy.integration.entrypoint.EntryPointProvider;
import com.iafenvoy.tooltipsreforged.api.TooltipsReforgeEntrypoint;
import com.yardenzamir.tooltipsreforgeddyed.component.CustomTagComponent;
import com.yardenzamir.tooltipsreforgeddyed.component.WaresTradeComponent;
import com.yardenzamir.tooltipsreforgeddyed.config.CustomTagsConfig;
import com.yardenzamir.tooltipsreforgeddyed.config.CustomTagsConfig.ResolvedTag;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

@EntryPointProvider(slug = "tooltips_reforged")
public class CustomTagsEntrypoint implements TooltipsReforgeEntrypoint {

    private static final String WARES_NAMESPACE = "wares";

    @Override
    public void appendTooltip(ItemStack stack, List<TooltipComponent> components) {
        // Check for config errors and notify player
        String error = CustomTagsConfig.getAndClearError();
        if (error != null) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(
                    Text.literal(error).formatted(Formatting.RED),
                    false
                );
            }
        }

        int insertIndex = Math.min(1, components.size());

        List<ResolvedTag> tags = CustomTagsConfig.getTagsForItem(stack);
        if (!tags.isEmpty()) {
            components.add(insertIndex, new CustomTagComponent(tags));
            insertIndex++;
        }

        // Handle Wares mod trade items (below tags)
        NbtCompound tradeNbt = extractTradeNbt(stack);
        if (tradeNbt != null) {
            components.add(insertIndex, WaresTradeComponent.fromNbt(tradeNbt));
        }
    }

    private NbtCompound extractTradeNbt(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null) return null;

        var itemId = Registries.ITEM.getId(stack.getItem());
        String namespace = itemId.getNamespace();
        String path = itemId.getPath();

        // Direct wares items: delivery_agreement, sealed_delivery_agreement
        if (WARES_NAMESPACE.equals(namespace) &&
            (path.equals("delivery_agreement") || path.equals("sealed_delivery_agreement"))) {
            if (nbt.contains("requestedItems") && nbt.contains("paymentItems")) {
                return nbt;
            }
        }

        // ptdye trading_transceiver: trade data nested in StoredAgreement.tag
        if ("ptdye".equals(namespace) && path.equals("trading_transceiver")) {
            if (nbt.contains("StoredAgreement", NbtElement.COMPOUND_TYPE)) {
                NbtCompound stored = nbt.getCompound("StoredAgreement");
                if (stored.contains("tag", NbtElement.COMPOUND_TYPE)) {
                    NbtCompound tag = stored.getCompound("tag");
                    if (tag.contains("requestedItems") && tag.contains("paymentItems")) {
                        return tag;
                    }
                }
            }
        }

        return null;
    }
}
