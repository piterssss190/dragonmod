package com.dragonmod.client;

import com.dragonmod.entity.DragonEntity;
import com.dragonmod.entity.ModEntities;
import com.dragonmod.network.ModNetworking;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Punkt wejścia inicjalizowany TYLKO na kliencie. Serwer dedykowany nigdy nie
 * ładuje tej klasy - dzięki temu możemy bezpiecznie odwoływać się tu do
 * Minecraft (klienta), renderowania itp. bez ryzyka crasha serwera.
 *
 * UWAGA MAPPINGI: Yarn "GameOptions" -> Mojang "Options"; pola klawiszy
 * to "keyJump"/"keyShift" (nie jumpKey/sneakKey), a metoda sprawdzająca
 * wciśnięcie klawisza w Mojang mappings nazywa się "isDown()" (nie isPressed()).
 *
 * UWAGA - POPRAWKA BŁĘDU: Shift ("keyShift") jest w wanilii jednocześnie
 * klawiszem ZSIADANIA z wierzchowca - silnik obsługuje to wewnętrznie,
 * niezależnie od naszego kodu, więc trzymanie Shift do "opadania" powodowało
 * natychmiastowe zsiadanie zamiast lotu w dół. Dlatego rejestrujemy WŁASNY,
 * osobny klawisz (domyślnie Lewy Ctrl) wyłącznie do opadania smokiem -
 * gracz może go zmienić w Opcje -> Sterowanie.
 */
public class ModMainClient implements ClientModInitializer {

    // UWAGA MAPPINGI: od Fabric API 26.1 kategoria klawisza to obiekt
    // KeyMapping.Category (nie zwykły String jak w starszych wersjach).
    private static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath("dragonmod", "dragonmod"));

    private static final KeyMapping DESCEND_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.dragonmod.descend",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_CONTROL,
            CATEGORY
    ));

    @Override
    public void onInitializeClient() {
        // Rejestracja geometrii modelu (musi nastąpić przed rejestracją renderera)
        // UWAGA MAPPINGI: klasa EntityModelLayerRegistry została przemianowana
        // na ModelLayerRegistry (potwierdzone oficjalnym przewodnikiem migracji
        // Fabric API dla 26.1 - docs.fabricmc.net/develop/porting/fabric-api).
        ModelLayerRegistry.registerModelLayer(DragonModel.LAYER, DragonModel::createBodyLayer);

        // Powiązanie typu encji z rendererem - WYŁĄCZNIE tutaj, nigdy w kodzie wspólnym
        EntityRendererRegistry.register(ModEntities.DRAGON, DragonRenderer::new);

        // UWAGA: NIE rejestrujemy tu ponownie typu pakietu (ModNetworking.registerPayloadType())!
        // Kod wspólny (ModMain.onInitialize -> ModNetworking.registerServerReceivers) już to
        // robi i uruchamia się RÓWNIEŻ na kliencie (to zwykła metoda Java, nie coś
        // ograniczonego do serwera) - podwójna rejestracja powodowała crash
        // "Packet type ... is already registered!".

        // Co tick klienta: jeśli gracz aktualnie jedzie na smoku, wysyłamy stan
        // klawiszy Space (wznoszenie) / własny klawisz opadania do serwera.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.options == null) return;

            // LOG DIAGNOSTYCZNY (co ok. 3 sekundy, niezależnie od klawiszy) -
            // potwierdza, czy gra w ogóle rozpoznaje gracza jako siedzącego
            // na smoku z perspektywy klienta.
            if (client.player.tickCount % 60 == 0) {
                com.dragonmod.ModMain.LOGGER.info("[DragonMod-KLIENT-TICK] vehicle={} isDragon={}",
                        client.player.getVehicle(), client.player.getVehicle() instanceof DragonEntity);
            }

            if (client.player.getVehicle() instanceof DragonEntity) {
                boolean ascend = client.options.keyJump.isDown();
                boolean descend = DESCEND_KEY.isDown();

                // LOG DIAGNOSTYCZNY - do usunięcia po znalezieniu przyczyny
                // problemu z wznoszeniem/opadaniem. Wypisuje się tylko gdy
                // faktycznie trzymasz Space lub klawisz opadania, żeby nie
                // zaśmiecać logu.
                if (ascend || descend) {
                    com.dragonmod.ModMain.LOGGER.info("[DragonMod-KLIENT] jadę na smoku, wysyłam ascend={} descend={} canSend={}",
                            ascend, descend, ClientPlayNetworking.canSend(ModNetworking.DragonFlightInputPayload.TYPE));
                }

                if (ClientPlayNetworking.canSend(ModNetworking.DragonFlightInputPayload.TYPE)) {
                    ClientPlayNetworking.send(new ModNetworking.DragonFlightInputPayload(ascend, descend));
                }
            }
        });
    }
}

