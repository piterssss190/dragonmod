package com.dragonmod.client;

import com.dragonmod.ModMain;
import com.dragonmod.entity.DragonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/**
 * DragonRenderer istnieje WYŁĄCZNIE po stronie klienta.
 *
 * UWAGA MAPPINGI: MobRenderer przyjmuje teraz TRZY parametry generyczne
 * (Entity, RenderState, Model) zamiast dwóch (Entity, Model) - stąd nowa
 * klasa DragonRenderState. getTextureLocation() i scale() operują teraz na
 * obiekcie stanu, nie bezpośrednio na encji.
 */
public class DragonRenderer extends MobRenderer<DragonEntity, DragonRenderState, DragonModel> {

    // Własna, wygenerowana tekstura dopasowana piksel-w-piksel do UV naszego
    // modelu (patrz DragonModel.java - texOffs muszą być z nią spójne).
    private static final Identifier TEXTURE = ModMain.id("textures/entity/dragon.png");

    public DragonRenderer(EntityRendererProvider.Context context) {
        super(context, new DragonModel(context.bakeLayer(DragonModel.LAYER)), 1.8f);
    }

    @Override
    public DragonRenderState createRenderState() {
        return new DragonRenderState();
    }

    /** Kopiuje dane z encji (wątek logiki) do obiektu stanu (wątek renderowania). */
    @Override
    public void extractRenderState(DragonEntity entity, DragonRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.saddled = entity.isSaddled();
        state.flying = entity.isVehicle() || !entity.onGround();
        state.scaleFactor = entity.getScaleFactor();
    }

    @Override
    public Identifier getTextureLocation(DragonRenderState state) {
        return TEXTURE;
    }

    /**
     * UWAGA: sygnatura scale(RenderState, PoseStack) jest moim przybliżeniem
     * nowego API - w razie błędu kompilacji sprawdź dekompilowany
     * LivingEntityRenderer#scale dla dokładnej sygnatury w 26.2.
     */
    @Override
    protected void scale(DragonRenderState state, PoseStack poseStack) {
        float scale = state.scaleFactor;
        poseStack.scale(scale, scale, scale);
    }
}
