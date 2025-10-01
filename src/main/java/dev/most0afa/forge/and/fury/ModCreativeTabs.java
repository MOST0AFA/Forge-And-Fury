package dev.most0afa.forge.and.fury;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import dev.most0afa.forge.and.fury.Items.ModItems;

public class ModCreativeTabs {

    public static final ItemGroup FORGE_AND_FURY_TAB = Registry.register(Registries.ITEM_GROUP,
            Identifier.of("forgeandfury", "forge_and_fury_tab"),
            FabricItemGroup.builder()
                    .displayName(Text.literal("Forge & Fury"))
                    .icon(() -> new ItemStack(ModItems.FIRE_AXE))
                    .entries((displayContext, entries) -> {
                        entries.add(ModItems.FIRE_AXE);
                        entries.add(ModItems.DUSKREND);
                        entries.add(ModItems.FROZEN_FANG);
                        entries.add(ModItems.RUINER);
                        entries.add(ModItems.STORMFANG);
                        entries.add(ModItems.TITANS_HAMMER);
                        entries.add(ModItems.INFERNAL_BOW);
                        entries.add(ModItems.STORMCALLER_BOW);
                        entries.add(ModItems.HEALING_STAFF);
                        entries.add(ModItems.STORMSHAPER_STAFF);
                        entries.add(ModItems.GRAVITY_INVERTER);
                    })
                    .build());

    public static void registerModCreativeTabs() {
    }
}