package com.dragonmod.client;

import com.dragonmod.ModMain;
import com.dragonmod.entity.DragonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * DragonRenderer istnieje WYŁĄCZNIE po stronie klienta (rejestrowany w
 * ModMainClient). Przepisane na mappingi Mojang: Yarn "EntityRendererFactory"
 * -> Mojang "EntityRendererProvider", Yarn "MobEntityRenderer" -> Mojang
 * "MobRenderer", Yarn "MatrixStack" -> Mojang "PoseStack" (com.mojang.blaze3d.vertex),
 * Yarn "Identifier" -> Mojang "ResourceLocation".
 *
 * UWAGA API: renderery klienckie to obszar najbardziej podatny na zmiany
 * między wersjami - w niektórych gałęziach 1.21.x/26.x Mojang wprowadził
 * wzorzec oparty o "EntityRenderState" (oddzielny obiekt stanu przekazywany
 * do modelu zamiast bezpośrednio encji). Jeśli kompilator w Twoim projekcie
 * zgłosi niezgodność sygnatur w MobRenderer/EntityModel, zweryfikuj aktualny
 * kształt tych klas w dekompilowanym źródle 26.2 (np. przez Ravel/IDE "Go to
 * Source") i dostosuj DragonModel#setupAnim oraz getTexture odpowiednio.
 */
public class DragonRenderer extends MobRenderer<DragonEntity, DragonModel> {

    private static final ResourceLocation TEXTURE = ModMain.id("textures/entity/dragon.png");

    public DragonRenderer(EntityRendererProvider.Context context) {
        super(context, new DragonModel(context.bakeLayer(DragonModel.LAYER)), 1.2f);
    }

    @Override
    public ResourceLocation getTextureLocation(DragonEntity entity) {
        return TEXTURE;
    }

    /** Skalowanie modelu zależnie od stadium wzrostu (Baby Dragon -> Adult Dragon). */
    @Override
    protected void scale(DragonEntity entity, PoseStack poseStack, float amount) {
        float scale = entity.getScaleFactor();
        poseStack.scale(scale, scale, scale);
    }
}
