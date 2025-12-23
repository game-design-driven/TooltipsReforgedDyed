package com.yardenzamir.tooltipsreforgeddyed.component;

import com.yardenzamir.tooltipsreforgeddyed.config.CustomTagsConfig.ResolvedTag;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class CustomTagComponent implements TooltipComponent {
    private static final float SCALE = 0.75f;
    private static final int TAG_PADDING = 2;
    private static final int TAG_GAP = 3;
    private static final int ROW_GAP = 2;
    private static final int SCREEN_EDGE_MARGIN = 8;

    private final List<ResolvedTag> tags;

    // Layout cache
    private List<List<ResolvedTag>> rows;
    private int layoutMaxWidth = -1;
    private int cachedHeight;
    private int cachedWidth;

    public CustomTagComponent(List<ResolvedTag> tags) {
        this.tags = tags;
    }

    private void calculateLayout(TextRenderer textRenderer, int maxWidth) {
        if (rows != null && layoutMaxWidth == maxWidth) return;

        layoutMaxWidth = maxWidth;
        rows = new ArrayList<>();
        List<ResolvedTag> currentRow = new ArrayList<>();
        int currentRowWidth = 0;
        int maxRowWidth = 0;

        for (ResolvedTag tag : tags) {
            int tagWidth = getScaledTagWidth(textRenderer, tag);

            if (!currentRow.isEmpty() && currentRowWidth + TAG_GAP + tagWidth > maxWidth) {
                rows.add(currentRow);
                maxRowWidth = Math.max(maxRowWidth, currentRowWidth);
                currentRow = new ArrayList<>();
                currentRowWidth = 0;
            }

            if (!currentRow.isEmpty()) {
                currentRowWidth += TAG_GAP;
            }
            currentRow.add(tag);
            currentRowWidth += tagWidth;
        }

        if (!currentRow.isEmpty()) {
            rows.add(currentRow);
            maxRowWidth = Math.max(maxRowWidth, currentRowWidth);
        }

        int rowHeight = getScaledRowHeight(textRenderer);
        cachedHeight = rows.isEmpty() ? 0 : rows.size() * rowHeight + (rows.size() - 1) * ROW_GAP + 2;
        cachedWidth = maxRowWidth;
    }

    private int getDefaultMaxWidth() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.getWindow().getScaledWidth() / 2;
    }

    private int getScaledTagWidth(TextRenderer textRenderer, ResolvedTag tag) {
        return (int) (textRenderer.getWidth(tag.text()) * SCALE) + TAG_PADDING * 2;
    }

    private int getScaledRowHeight(TextRenderer textRenderer) {
        return (int) (textRenderer.fontHeight * SCALE) + 2;
    }

    @Override
    public int getHeight() {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        calculateLayout(textRenderer, getDefaultMaxWidth());
        return cachedHeight;
    }

    @Override
    public int getWidth(TextRenderer textRenderer) {
        calculateLayout(textRenderer, getDefaultMaxWidth());
        return cachedWidth;
    }

    @Override
    public void drawText(TextRenderer textRenderer, int x, int y, Matrix4f matrix, VertexConsumerProvider.Immediate vertexConsumers) {
        // Text drawn in drawItems for proper layering with scale
    }

    @Override
    public void drawItems(TextRenderer textRenderer, int x, int y, DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();

        // Calculate available width considering screen edge
        int availableWidth = screenWidth - x - SCREEN_EDGE_MARGIN;
        int maxWidth = Math.min(getDefaultMaxWidth(), availableWidth);

        // Recalculate layout if width constraint changed
        calculateLayout(textRenderer, maxWidth);

        int rowHeight = getScaledRowHeight(textRenderer);
        int currentY = y + 1;

        for (List<ResolvedTag> row : rows) {
            int currentX = x;

            for (ResolvedTag tag : row) {
                int scaledTextWidth = (int) (textRenderer.getWidth(tag.text()) * SCALE);
                int tagWidth = scaledTextWidth + TAG_PADDING * 2;
                int tagHeight = rowHeight;

                int bgColor = darkenColor(tag.argbColor(), 0.85f);
                int frameColor = darkenColor(tag.argbColor(), 0.7f);

                // Background
                context.fill(currentX, currentY, currentX + tagWidth, currentY + tagHeight, bgColor);

                // Frame
                drawFrame(context, currentX, currentY, tagWidth, tagHeight, frameColor);

                // Scaled text
                context.getMatrices().push();
                context.getMatrices().translate(currentX + TAG_PADDING, currentY + 1, 0);
                context.getMatrices().scale(SCALE, SCALE, 1.0f);
                context.drawText(textRenderer, tag.text(), 0, 0, 0xFFFFFFFF, true);
                context.getMatrices().pop();

                currentX += tagWidth + TAG_GAP;
            }

            currentY += rowHeight + ROW_GAP;
        }
    }

    private static int darkenColor(int color, float factor) {
        int alpha = (color >> 24) & 0xFF;
        int red = (int) (((color >> 16) & 0xFF) * factor);
        int green = (int) (((color >> 8) & 0xFF) * factor);
        int blue = (int) ((color & 0xFF) * factor);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static void drawFrame(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }
}
