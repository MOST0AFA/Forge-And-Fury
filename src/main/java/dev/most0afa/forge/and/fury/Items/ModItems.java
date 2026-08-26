package dev.most0afa.forge.and.fury.Items;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

import java.util.function.Function;

public class ModItems {
    public static final Item FIRE_AXE = registerItem("fire_axe", props -> new FireAxe(ToolMaterial.NETHERITE, props));
    public static final Item INFERNAL_BOW = registerItem("infernal_bow", InfernalBow::new);
    public static final Item FROZEN_FANG = registerItem("frozen_fang", props -> new FrozenFang(ToolMaterial.IRON, props));
    public static final Item RUINER = registerItem("ruiner", props -> new Ruiner(ToolMaterial.NETHERITE, props));
    public static final Item STORMFANG = registerItem("stormfang", props -> new StormFang(ToolMaterial.IRON, props));
    public static final Item DUSKREND = registerItem("duskrend", props -> new Duskrend(ToolMaterial.IRON, props));
    public static final Item TITANS_HAMMER = registerItem("titans_hammer", props -> new TitansHammer(ToolMaterial.DIAMOND, props));
    public static final Item HEALING_STAFF = registerItem("healing_staff", props -> new HealingStaff(props.stacksTo(1)));
    public static final Item STORMCALLER_BOW = registerItem("stormcaller_bow", StormCallerBow::new);
    public static final Item STORMSHAPER_STAFF = registerItem("stormshaper_staff", props -> new StormShaperStaff(props.stacksTo(1)));
    public static final Item GRAVITY_INVERTER = registerItem("gravity_inverter", GravityInverter::new);

    private static Item registerItem(String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("forgeandfury", name));
        Item item = factory.apply(new Item.Properties().setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static void registerItems() {
    }
}
