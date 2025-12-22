package com.kcw.tooltipsreforgeddyed.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kcw.tooltipsreforgeddyed.TooltipsReforgedDyed;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class CustomTagsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ConfigData config = new ConfigData();
    private static Path configPath;
    private static long lastModified = 0;

    public static void load(Path path) {
        configPath = path;
        reload();
    }

    private static String configError = null;

    private static void reload() {
        if (!Files.exists(configPath)) {
            createDefaultConfig(configPath);
            configError = null;
            return;
        }
        try {
            lastModified = Files.getLastModifiedTime(configPath).toMillis();
            try (Reader reader = Files.newBufferedReader(configPath)) {
                config = GSON.fromJson(reader, ConfigData.class);
                if (config == null) config = new ConfigData();
                configError = null;
            }
        } catch (Exception e) {
            TooltipsReforgedDyed.LOGGER.error("Failed to load custom tags config", e);
            configError = "TooltipsReforgedDyed config error: " + e.getMessage();
            config = new ConfigData(); // Use empty config instead of crashing
        }
    }

    public static String getAndClearError() {
        String error = configError;
        configError = null;
        return error;
    }

    private static void checkReload() {
        try {
            long current = Files.getLastModifiedTime(configPath).toMillis();
            if (current != lastModified) {
                TooltipsReforgedDyed.LOGGER.info("Config changed, reloading...");
                reload();
            }
        } catch (IOException ignored) {}
    }

    private static void createDefaultConfig(Path path) {
        config = new ConfigData();

        // false = whitelist mode (only show tags in tagList)
        // true = blacklist mode (show all tags EXCEPT those in tagList, but use tagList for display overrides)
        config.useBlacklist = false;

        // Example item-specific tags
        config.itemTags.put("minecraft:diamond_sword", List.of(
            new TagEntry("Favorite", 0x55FF55, null),
            new TagEntry("PvP", 0x5555FF, null)
        ));

        // Tag list - behavior depends on useBlacklist:
        // Whitelist mode: only tags in list are shown (hidden field ignored)
        // Blacklist mode: hidden=true hides tag, hidden=false/null shows with custom display
        // text and color are optional - derived from tag name if omitted
        // Wildcards supported: "minecraft:mineable/*" matches all mineable tags
        // When using wildcards, text/color are ignored (always derived from actual tag)
        config.tagList.put("forge:tools/pickaxes", new TagEntry(null, null, null));
        config.tagList.put("forge:tools/swords", new TagEntry("Blade", 0xFF5555, null));
        config.tagList.put("minecraft:mineable/*", new TagEntry(null, null, true));
        config.tagList.put("minecraft:planks", new TagEntry("Wood", 0x8B4513, null));

        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            TooltipsReforgedDyed.LOGGER.error("Failed to create default config", e);
        }
    }

    public static List<ResolvedTag> getTagsForItem(ItemStack stack) {
        checkReload();
        List<ResolvedTag> result = new ArrayList<>();

        // Direct item tags
        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        List<TagEntry> direct = config.itemTags.get(itemId.toString());
        if (direct != null) {
            for (TagEntry entry : direct) {
                result.add(resolveTag("custom", entry, false));
            }
        }

        // Collect all item's tags
        Set<String> itemTags = new HashSet<>();
        stack.streamTags().forEach(tag -> itemTags.add(tag.id().toString()));

        // Block tags for BlockItems
        if (stack.getItem() instanceof BlockItem blockItem) {
            blockItem.getBlock().getDefaultState().streamTags()
                .forEach(tag -> itemTags.add(tag.id().toString()));
        }

        if (config.useBlacklist) {
            // Blacklist mode: show all tags except those with hidden=true
            for (String tagId : itemTags) {
                MatchResult match = findMatchingEntry(tagId);
                if (match == null) {
                    // Not in list = show with auto-derived display
                    result.add(resolveTag(tagId, new TagEntry(null, null, null), false));
                } else if (!Boolean.TRUE.equals(match.entry.hidden())) {
                    // In list but not hidden = show with custom display (or derived if wildcard)
                    result.add(resolveTag(tagId, match.entry, match.isWildcard));
                }
                // hidden=true = blacklisted, don't show
            }
        } else {
            // Whitelist mode: only show tags matching the list
            for (String tagId : itemTags) {
                MatchResult match = findMatchingEntry(tagId);
                if (match != null) {
                    result.add(resolveTag(tagId, match.entry, match.isWildcard));
                }
            }
        }

        return result;
    }

    private record MatchResult(TagEntry entry, boolean isWildcard) {}

    private static MatchResult findMatchingEntry(String tagId) {
        // Check exact match first
        TagEntry exact = config.tagList.get(tagId);
        if (exact != null) {
            return new MatchResult(exact, false);
        }

        // Check wildcard patterns
        for (Map.Entry<String, TagEntry> entry : config.tagList.entrySet()) {
            String pattern = entry.getKey();
            if (pattern.contains("*") && matchesWildcard(pattern, tagId)) {
                return new MatchResult(entry.getValue(), true);
            }
        }

        return null;
    }

    private static boolean matchesWildcard(String pattern, String tagId) {
        // Convert glob pattern to regex: * matches anything
        String regex = pattern
            .replace(".", "\\.")
            .replace("*", ".*");
        return tagId.matches(regex);
    }

    private static ResolvedTag resolveTag(String tagId, TagEntry entry, boolean ignoreEntryDisplay) {
        // If wildcard match, ignore entry's text/color and derive from actual tag
        String text = (!ignoreEntryDisplay && entry.text() != null) ? entry.text() : deriveDisplayName(tagId);
        int color = (!ignoreEntryDisplay && entry.color() != null) ? entry.color() : colorFromName(text);
        return new ResolvedTag(text, 0xFF000000 | color);
    }

    private static String deriveDisplayName(String tagId) {
        // Get last segment after / or : and convert to title case
        String path = tagId;
        int slashIdx = path.lastIndexOf('/');
        int colonIdx = path.lastIndexOf(':');
        int idx = Math.max(slashIdx, colonIdx);
        if (idx >= 0 && idx < path.length() - 1) {
            path = path.substring(idx + 1);
        }
        return toTitleCase(path);
    }

    private static String toTitleCase(String input) {
        if (input == null || input.isEmpty()) return input;
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : input.toCharArray()) {
            if (c == '_' || c == '-') {
                result.append(' ');
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }

    public static int getTagCount() {
        return config.itemTags.size() + config.tagList.size();
    }

    private static int colorFromName(String name) {
        int hash = name.hashCode();
        int r = (hash >> 16) & 0xFF;
        int g = (hash >> 8) & 0xFF;
        int b = hash & 0xFF;
        r = Math.max(r, 64);
        g = Math.max(g, 64);
        b = Math.max(b, 64);
        return (r << 16) | (g << 8) | b;
    }

    public record TagEntry(String text, Integer color, Boolean hidden) {}

    public record ResolvedTag(String text, int argbColor) {}

    private static class ConfigData {
        boolean useBlacklist = false;
        Map<String, List<TagEntry>> itemTags = new LinkedHashMap<>();
        Map<String, TagEntry> tagList = new LinkedHashMap<>();
    }
}
