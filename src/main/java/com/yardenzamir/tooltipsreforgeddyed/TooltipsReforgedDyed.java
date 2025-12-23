package com.yardenzamir.tooltipsreforgeddyed;

import com.yardenzamir.tooltipsreforgeddyed.config.CustomTagsConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(TooltipsReforgedDyed.MOD_ID)
public class TooltipsReforgedDyed {
    public static final String MOD_ID = "tooltips_reforged_dyed";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public TooltipsReforgedDyed() {
        CustomTagsConfig.load(FMLPaths.CONFIGDIR.get().resolve("tooltips_reforged_dyed.json"));
        LOGGER.info("TooltipsReforgedDyed loaded with {} tag definitions", CustomTagsConfig.getTagCount());
    }
}
