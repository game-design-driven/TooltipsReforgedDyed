package com.yardenzamir.tooltipsreforgeddyed.component;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class WaresTradeComponent implements TooltipComponent {
    private static final int ITEM_SIZE = 16;
    private static final int ITEM_GAP = 2;
    private static final int SECTION_GAP = 6;
    private static final int ARROW_WIDTH = 12;
    private static final int VERTICAL_PADDING = 2;

    private final List<ItemStack> requestedItems;
    private final List<ItemStack> paymentItems;

    public WaresTradeComponent(List<ItemStack> requestedItems, List<ItemStack> paymentItems) {
        this.requestedItems = requestedItems;
        this.paymentItems = paymentItems;
    }

    public static WaresTradeComponent fromNbt(NbtCompound nbt) {
        List<ItemStack> requested = parseItemList(nbt.getList("requestedItems", NbtElement.COMPOUND_TYPE));
        List<ItemStack> payment = parseItemList(nbt.getList("paymentItems", NbtElement.COMPOUND_TYPE));
        return new WaresTradeComponent(requested, payment);
    }

    private static List<ItemStack> parseItemList(NbtList list) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            NbtCompound itemNbt = list.getCompound(i);
            ItemStack stack = parseItemStack(itemNbt);
            if (!stack.isEmpty()) {
                mergeOrAdd(items, stack);
            }
        }
        return items;
    }

    private static ItemStack parseItemStack(NbtCompound nbt) {
        String id = nbt.getString("id");
        int count = nbt.contains("Count") ? nbt.getInt("Count") : 1;

        Identifier itemId = Identifier.tryParse(id);
        if (itemId == null) return ItemStack.EMPTY;

        var item = Registries.ITEM.getOrEmpty(itemId);
        if (item.isEmpty()) return ItemStack.EMPTY;

        ItemStack stack = new ItemStack(item.get(), count);

        if (nbt.contains("tag", NbtElement.COMPOUND_TYPE)) {
            stack.setNbt(nbt.getCompound("tag"));
        }

        return stack;
    }

    private static void mergeOrAdd(List<ItemStack> items, ItemStack newStack) {
        for (ItemStack existing : items) {
            if (ItemStack.canCombine(existing, newStack)) {
                existing.setCount(existing.getCount() + newStack.getCount());
                return;
            }
        }
        items.add(newStack.copy());
    }

    @Override
    public int getHeight() {
        return ITEM_SIZE + VERTICAL_PADDING * 2;
    }

    @Override
    public int getWidth(TextRenderer textRenderer) {
        int requestedWidth = requestedItems.size() * ITEM_SIZE + Math.max(0, requestedItems.size() - 1) * ITEM_GAP;
        int paymentWidth = paymentItems.size() * ITEM_SIZE + Math.max(0, paymentItems.size() - 1) * ITEM_GAP;
        return requestedWidth + SECTION_GAP + ARROW_WIDTH + SECTION_GAP + paymentWidth;
    }

    @Override
    public void drawText(TextRenderer textRenderer, int x, int y, Matrix4f matrix, VertexConsumerProvider.Immediate vertexConsumers) {
    }

    @Override
    public void drawItems(TextRenderer textRenderer, int x, int y, DrawContext context) {
        int currentX = x;
        int itemY = y + VERTICAL_PADDING;

        // Draw requested items (what player gives)
        for (ItemStack stack : requestedItems) {
            drawItemWithCount(context, textRenderer, stack, currentX, itemY);
            currentX += ITEM_SIZE + ITEM_GAP;
        }

        if (!requestedItems.isEmpty()) {
            currentX -= ITEM_GAP;
        }
        currentX += SECTION_GAP;

        // Draw arrow
        int arrowY = itemY + ITEM_SIZE / 2;
        drawArrow(context, currentX, arrowY, ARROW_WIDTH);
        currentX += ARROW_WIDTH + SECTION_GAP;

        // Draw payment items (what player receives)
        for (ItemStack stack : paymentItems) {
            drawItemWithCount(context, textRenderer, stack, currentX, itemY);
            currentX += ITEM_SIZE + ITEM_GAP;
        }
    }

    private void drawItemWithCount(DrawContext context, TextRenderer textRenderer, ItemStack stack, int x, int y) {
        context.drawItem(stack, x, y);
        context.drawItemInSlot(textRenderer, stack, x, y);
    }

    private void drawArrow(DrawContext context, int x, int y, int width) {
        int color = 0xFFAAAAAA;
        int arrowHeadSize = 3;

        // Arrow shaft
        context.fill(x, y - 1, x + width - arrowHeadSize, y + 1, color);

        // Arrow head
        for (int i = 0; i < arrowHeadSize; i++) {
            int headX = x + width - arrowHeadSize + i;
            context.fill(headX, y - (arrowHeadSize - i), headX + 1, y + (arrowHeadSize - i) + 1, color);
        }
    }
}
