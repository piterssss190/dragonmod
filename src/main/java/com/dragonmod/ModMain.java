package com.dragonmod;

import com.dragonmod.block.ModBlocks;
import com.dragonmod.entity.ModEntities;
import com.dragonmod.item.ModItems;
import com.dragonmod.network.ModNetworking;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Punkt wejścia modu wykonywany na obu stronach: SERWER i KLIENT.
 *
 * UWAGA MAPPINGI (potwierdzone realnym błędem kompilacji): oficjalna nazwa
 * Mojang dla identyfikatora zasobu to "Identifier", NIE "ResourceLocation".
 * "ResourceLocation" była nazwą zwyczajową, jaką społeczności moddingowe
 * (Forge/NeoForge/Yarn) nadały temu typowi przez lata pracy na obfuskowanym
 * kodzie - teraz, gdy Minecraft jest nieobfuskowany, widzimy prawdziwą nazwę
 * użytą przez samego Mojanga w ich źródle.
 */
public class ModMain implements ModInitializer {

    public static final String MOD_ID = "dragonmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
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
