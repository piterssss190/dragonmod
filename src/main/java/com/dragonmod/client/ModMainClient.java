package com.dragonmod.client;

import com.dragonmod.entity.DragonEntity;
import com.dragonmod.entity.ModEntities;
import com.dragonmod.network.ModNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

/**
 * Punkt wejścia inicjalizowany TYLKO na kliencie. Serwer dedykowany nigdy nie
 * ładuje tej klasy - dzięki temu możemy bezpiecznie odwoływać się tu do
 * Minecraft (klienta), renderowania itp. bez ryzyka crasha serwera.
 *
 * UWAGA MAPPINGI: Yarn "GameOptions" -> Mojang "Options"; pola klawiszy
 * to "keyJump"/"keyShift" (nie jumpKey/sneakKey), a metoda sprawdzająca
 * wciśnięcie klawisza w Mojang mappings nazywa się "isDown()" (nie isPressed()).
 */
public class ModMainClient implements ClientModInitializer {

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
        // klawiszy Space (wznoszenie) / Shift (opadanie) do serwera.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.options == null) return;

            if (client.player.getVehicle() instanceof DragonEntity) {
                boolean ascend = client.options.keyJump.isDown();
                boolean descend = client.options.keyShift.isDown();

                if (ClientPlayNetworking.canSend(ModNetworking.DragonFlightInputPayload.TYPE)) {
                    ClientPlayNetworking.send(new ModNetworking.DragonFlightInputPayload(ascend, descend));
                }
            }
        });
    }
}
