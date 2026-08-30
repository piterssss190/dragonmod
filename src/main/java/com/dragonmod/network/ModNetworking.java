package com.dragonmod.network;

import com.dragonmod.ModMain;
import com.dragonmod.entity.DragonEntity;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Centralny punkt rejestracji sieci (server-side). Przepisane na mappingi
 * Mojang: Yarn "CustomPayload" -> Mojang "CustomPacketPayload",
 * Yarn "PacketCodec"/"PacketByteBuf" -> Mojang "StreamCodec"/"FriendlyByteBuf".
 *
 * WAŻNE dot. kompatybilności z serwerem dedykowanym:
 * Ruch przód/tył/boki NIE wymaga własnego pakietu - silnik gry synchronizuje
 * go natywnie, gdy gracz jest "controlling passenger" osiodłanej encji
 * (patrz DragonEntity#travel). Potrzebujemy WŁASNEGO, lekkiego kanału tylko
 * dla wejścia pionowego (Space/Shift), którego wanilia nie przewiduje dla
 * latających wierzchowców.
 */
public class ModNetworking {

    /** Payload C2S (Client -> Server) niosący stan klawiszy Space/Shift. */
    public record DragonFlightInputPayload(boolean ascend, boolean descend) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<DragonFlightInputPayload> TYPE =
                new CustomPacketPayload.Type<>(ModMain.id("dragon_flight_input"));

        public static final StreamCodec<FriendlyByteBuf, DragonFlightInputPayload> STREAM_CODEC = StreamCodec.composite(
                StreamCodec.of((buf, val) -> buf.writeBoolean(val), FriendlyByteBuf::readBoolean),
                DragonFlightInputPayload::ascend,
                StreamCodec.of((buf, val) -> buf.writeBoolean(val), FriendlyByteBuf::readBoolean),
                DragonFlightInputPayload::descend,
                DragonFlightInputPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Rejestracja typu pakietu - musi się wykonać identycznie na serwerze i kliencie. */
    public static void registerPayloadType() {
        // UWAGA MAPPINGI: metoda "playC2S()" została przemianowana na
        // "serverboundPlay()" (potwierdzone aktualną dokumentacją Fabric).
        PayloadTypeRegistry.serverboundPlay().register(DragonFlightInputPayload.TYPE, DragonFlightInputPayload.STREAM_CODEC);
    }

    /**
     * Odbiornik pakietu po stronie serwera. Weryfikujemy, że gracz faktycznie
     * JEDZIE na smoku (a nie próbuje sterować cudzą encją), i wykonujemy zmianę
     * stanu na wątku głównym serwera, aby uniknąć race-condition z tickiem świata.
     */
    public static void registerServerReceivers() {
        registerPayloadType();

        ServerPlayNetworking.registerGlobalReceiver(DragonFlightInputPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                boolean isOnDragon = player.getVehicle() instanceof DragonEntity;
                com.dragonmod.ModMain.LOGGER.info("[DragonMod-SERWER] odebrano pakiet ascend={} descend={} isOnDragon={}",
                        payload.ascend(), payload.descend(), isOnDragon);
                if (player.getVehicle() instanceof DragonEntity dragon && dragon.isOwnedBy(player)) {
                    dragon.setFlightInput(payload.ascend(), payload.descend());
                }
            });
        });
    }
}
