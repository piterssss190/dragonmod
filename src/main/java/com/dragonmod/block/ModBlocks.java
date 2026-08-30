package com.dragonmod.block;

import com.dragonmod.ModMain;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * Rejestracja bloków.
 *
 * UWAGA MAPPINGI (potwierdzone realnym crashem w grze - "Block id not set"):
 * od 26.2 obiekt Properties MUSI mieć jawnie ustawiony identyfikator przez
 * .setId(ResourceKey<Block>) PRZED skonstruowaniem bloku - silnik używa go
 * m.in. do wyznaczenia domyślnej tabeli dropów (effectiveDrops). Ten klucz
 * musi być identyczny z tym, którego użyjemy w Registry.register().
 */
public class ModBlocks {

    private static final ResourceKey<Block> DRAGON_EGG_KEY =
            ResourceKey.create(Registries.BLOCK, ModMain.id("dragon_egg"));

    public static final Block DRAGON_EGG = Registry.register(
            BuiltInRegistries.BLOCK,
            DRAGON_EGG_KEY,
            new DragonEggBlock(BlockBehaviour.Properties.of()
                    .setId(DRAGON_EGG_KEY)
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(0.5f)
                    .lightLevel(state -> 1)
                    .noOcclusion())
    );

    public static void register() {
        // Wywołanie triggeruje statyczną inicjalizację powyższych pól.
    }
}
