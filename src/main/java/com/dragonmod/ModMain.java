package com.dragonmod;

import com.dragonmod.block.ModBlocks;
import com.dragonmod.entity.ModEntities;
import com.dragonmod.item.ModItems;
import com.dragonmod.network.ModNetworking;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Punkt wejścia modu wykonywany na obu stronach: SERWER i KLIENT.
 * Kod przeniesiony na oficjalne mappingi Mojang (Minecraft 26.2 "Chaos Cubed"),
 * ponieważ Fabric od wersji 26.1 nie wspiera już Yarn mappings.
 */
public class ModMain implements ModInitializer {

    public static final String MOD_ID = "dragonmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        LOGGER.info("[DragonMod] Inicjalizacja logiki serwerowej...");

        ModEntities.register();
        ModItems.register();
        ModBlocks.register();
        ModNetworking.registerServerReceivers();

        LOGGER.info("[DragonMod] Gotowe.");
    }
}
