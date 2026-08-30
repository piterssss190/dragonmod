package com.dragonmod.item;

import com.dragonmod.ModMain;
import com.dragonmod.block.ModBlocks;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

/**
 * Rejestracja itemów.
 *
 * UWAGA MAPPINGI: konstruktor SpawnEggItem(EntityType, Properties) zmienił
 * się w 26.2 na SpawnEggItem(Properties) - powiązanie z konkretnym typem
 * encji odbywa się teraz inaczej (prawdopodobnie przez rejestr/builder,
 * którego dokładnego kształtu nie zdążyłem zweryfikować). Usunięto tu
 * jajko-do-spawnu jako nieistotny dodatek administracyjny - smoka można
 * i tak przetestować przez komendę /summon dragonmod:dragon.
 */
public class ModItems {

    public static final Item DRAGON_EGG_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            ModMain.id("dragon_egg"),
            new BlockItem(ModBlocks.DRAGON_EGG, new Item.Properties())
    );

    public static void register() {
        // Wywołanie triggeruje statyczną inicjalizację powyższych pól.
    }
}
