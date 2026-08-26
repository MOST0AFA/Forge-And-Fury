package dev.most0afa.forge.and.fury;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import dev.most0afa.forge.and.fury.Items.ModItems;

public class ModCreativeTabs {

    public static final CreativeModeTab FORGE_AND_FURY_TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath("forgeandfury", "forge_and_fury_tab"),
            FabricCreativeModeTab.builder()
                    .title(Component.literal("Forge & Fury"))
                    .icon(() -> new ItemStack(ModItems.FIRE_AXE))
                    .displayItems((displayContext, entries) -> {
                        entries.accept(ModItems.FIRE_AXE);
                        entries.accept(ModItems.DUSKREND);
                        entries.accept(ModItems.FROZEN_FANG);
                        entries.accept(ModItems.RUINER);
                        entries.accept(ModItems.STORMFANG);
                        entries.accept(ModItems.TITANS_HAMMER);
                        entries.accept(ModItems.INFERNAL_BOW);
                        entries.accept(ModItems.STORMCALLER_BOW);
                        entries.accept(ModItems.HEALING_STAFF);
                        entries.accept(ModItems.STORMSHAPER_STAFF);
                        entries.accept(ModItems.GRAVITY_INVERTER);
                    })
                    .build());

    public static void registerModCreativeTabs() {
    }
}
