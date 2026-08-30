package com.dragonmod.block;

import com.dragonmod.ModMain;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * Rejestracja bloków. Mappingi Mojang: Yarn "AbstractBlock.Settings" ->
 * Mojang "BlockBehaviour.Properties", Yarn "Registries.BLOCK" (rejestr) ->
 * Mojang "BuiltInRegistries.BLOCK".
 */
public class ModBlocks {

    public static final Block DRAGON_EGG = Registry.register(
            BuiltInRegistries.BLOCK,
            ModMain.id("dragon_egg"),
            new DragonEggBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(0.5f)
                    .lightLevel(state -> 1) // delikatna poświata jaja
                    .noOcclusion())
    );

    public static void register() {
        // Wywołanie triggeruje statyczną inicjalizację powyższych pól.
    }
}
