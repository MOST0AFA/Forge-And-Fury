package dev.most0afa.forge.and.fury;

import net.fabricmc.api.ModInitializer;

import dev.most0afa.forge.and.fury.Items.ModItems;

public class ForgeAndFuryMain implements ModInitializer {
    public static final String MOD_ID = "forgeandfury";

    @Override
    public void onInitialize() {
        ModItems.registerItems();
        ModCreativeTabs.registerModCreativeTabs();
    }
}
