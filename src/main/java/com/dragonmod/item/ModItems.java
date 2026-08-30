package com.dragonmod.item;

import com.dragonmod.ModMain;
import com.dragonmod.block.ModBlocks;
import com.dragonmod.entity.ModEntities;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

/**
 * Rejestracja itemów. Mappingi Mojang: Yarn "Registries.ITEM" (rejestr) ->
 * Mojang "BuiltInRegistries.ITEM".
 */
public class ModItems {

    /** Item odpowiadający blokowi jaja - pozwala go trzymać w ekwipunku i stawiać. */
    public static final Item DRAGON_EGG_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            ModMain.id("dragon_egg"),
            new BlockItem(ModBlocks.DRAGON_EGG, new Item.Properties())
    );

    /** Jajko do kreatywnego spawnowania smoka (narzędzie testowe/administracyjne). */
    public static final Item DRAGON_SPAWN_EGG = Registry.register(
            BuiltInRegistries.ITEM,
            ModMain.id("dragon_spawn_egg"),
            new SpawnEggItem(ModEntities.DRAGON, new Item.Properties())
    );

    public static void register() {
        // Rejestracja w karcie kreatywnej - opcjonalne, ułatwia testy.
    }
}
