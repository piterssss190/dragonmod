package com.dragonmod.item;

import com.dragonmod.ModMain;
import com.dragonmod.block.ModBlocks;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

/**
 * Rejestracja itemów.
 *
 * UWAGA MAPPINGI: tak samo jak bloki (patrz ModBlocks.java), Item.Properties
 * w 26.2 wymaga jawnego .setId(ResourceKey<Item>) przed konstrukcją itemu -
 * naprawione prewencyjnie po tym, jak dokładnie ten sam wzorzec spowodował
 * crash "Block id not set" dla DragonEggBlock.
 */
public class ModItems {

    private static final ResourceKey<Item> DRAGON_EGG_ITEM_KEY =
            ResourceKey.create(Registries.ITEM, ModMain.id("dragon_egg"));

    public static final Item DRAGON_EGG_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            DRAGON_EGG_ITEM_KEY,
            new BlockItem(ModBlocks.DRAGON_EGG, new Item.Properties().setId(DRAGON_EGG_ITEM_KEY))
    );

    public static void register() {
        // Wywołanie triggeruje statyczną inicjalizację powyższych pól.
    }
}
