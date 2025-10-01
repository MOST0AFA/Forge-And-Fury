package dev.most0afa.forge.and.fury.Items;

import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registry;

public class ModItems {
    public static final Item FIRE_AXE = registerItem("fire_axe", new FireAxe(ToolMaterials.NETHERITE, new Item.Settings()));
    public static final Item INFERNAL_BOW = registerItem("infernal_bow", new InfernalBow());
    public static final Item FROZEN_FANG = registerItem("frozen_fang", new FrozenFang(ToolMaterials.IRON, new Item.Settings()));
    public static final Item RUINER = registerItem("ruiner", new Ruiner(ToolMaterials.NETHERITE, new Item.Settings()));
    public static final Item STORMFANG = registerItem("stormfang", new StormFang(ToolMaterials.IRON, new Item.Settings()));
    public static final Item DUSKREND = registerItem("duskrend", new Duskrend(ToolMaterials.IRON, new Item.Settings()));
    public static final Item TITANS_HAMMER = registerItem("titans_hammer", new TitansHammer(ToolMaterials.DIAMOND, new Item.Settings()));
    public static final Item HEALING_STAFF = registerItem("healing_staff", new HealingStaff(new Item.Settings().maxCount(1)));
    public static final Item STORMCALLER_BOW = registerItem("stormcaller_bow", new StormCallerBow(new Item.Settings().maxDamage(512)));
    public static final Item STORMSHAPER_STAFF = registerItem("stormshaper_staff", new StormShaperStaff(new Item.Settings().maxCount(1)));
    public static final Item GRAVITY_INVERTER = registerItem("gravity_inverter", new GravityInverter(new Item.Settings()));

    private static Item registerItem(String name, Item item) {
        System.out.println("Registering item: " + name);
        return Registry.register(Registries.ITEM, Identifier.of("forgeandfury", name), item);
    }

    public static void registerItems() {
        System.out.println("ModItems.registerItems() called - but items are already registered via static initialization");
    }
}
