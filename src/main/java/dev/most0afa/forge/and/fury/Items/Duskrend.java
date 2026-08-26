package dev.most0afa.forge.and.fury.Items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

public class Duskrend extends Item {
    public Duskrend(ToolMaterial material, Item.Properties properties) {
        super(properties.sword(material, 5.0F, -2.4F));
    }
}
