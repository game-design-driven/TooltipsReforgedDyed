package com.yardenzamir.tooltipsreforgeddyed;

import com.iafenvoy.integration.entrypoint.EntryPointProvider;
import com.iafenvoy.tooltipsreforged.api.TooltipsReforgeEntrypoint;
import com.yardenzamir.tooltipsreforgeddyed.component.CustomTagComponent;
import com.yardenzamir.tooltipsreforgeddyed.config.CustomTagsConfig;
import com.yardenzamir.tooltipsreforgeddyed.config.CustomTagsConfig.ResolvedTag;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

@EntryPointProvider(slug = "tooltips_reforged")
public class CustomTagsEntrypoint implements TooltipsReforgeEntrypoint {

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

        List<ResolvedTag> tags = CustomTagsConfig.getTagsForItem(stack);

        if (!tags.isEmpty()) {
            // Insert after header (index 1) or at beginning if empty
            int insertIndex = Math.min(1, components.size());
            components.add(insertIndex, new CustomTagComponent(tags));
        }
    }
}
