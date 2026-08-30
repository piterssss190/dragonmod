package com.dragonmod.client;

import com.dragonmod.ModMain;
import com.dragonmod.entity.DragonEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;

/**
 * Uproszczony, ręcznie zdefiniowany model smoka. Przepisany na mappingi
 * Mojang: Yarn "ModelData"->"MeshDefinition", "ModelPartData"->"PartDefinition",
 * "ModelPartBuilder"->"CubeListBuilder", "ModelTransform"->"PartPose",
 * "TexturedModelData"->"LayerDefinition", "EntityModelLayer"->"ModelLayerLocation",
 * "MatrixStack"->"PoseStack" (com.mojang.blaze3d.vertex), "MathHelper"->"Mth".
 *
 * To jest odpowiednik "domyślnego EnderDragonModel wyskalowanego jako baza"
 * z wymagań - własny, prosty szkielet ModelPart, łatwy do podmiany na
 * GeckoLib bez zmiany reszty kodu (renderer/encja się nie zmieniają).
 */
public class DragonModel extends EntityModel<DragonEntity> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ModMain.id("dragon"), "main");

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart tail;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart[] legs;

    public DragonModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.tail = this.body.getChild("tail");
        this.leftWing = this.body.getChild("left_wing");
        this.rightWing = this.body.getChild("right_wing");
        this.legs = new ModelPart[]{
                this.body.getChild("leg_front_left"), this.body.getChild("leg_front_right"),
                this.body.getChild("leg_back_left"), this.body.getChild("leg_back_right")
        };
    }

    /** Definicja geometrii - wywoływana raz przy starcie klienta (EntityModelLayerRegistry). */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-6f, -6f, -14f, 12f, 12f, 28f),
                PartPose.offset(0f, 12f, 0f));

        body.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 40).addBox(-4f, -4f, -10f, 8f, 8f, 10f),
                PartPose.offset(0f, -2f, -14f));

        body.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(60, 0).addBox(-2f, -2f, 0f, 4f, 4f, 20f),
                PartPose.offset(0f, 0f, 14f));

        body.addOrReplaceChild("left_wing",
                CubeListBuilder.create().texOffs(0, 80).addBox(0f, 0f, -2f, 24f, 1f, 16f),
                PartPose.offset(6f, -4f, -2f));

        body.addOrReplaceChild("right_wing",
                CubeListBuilder.create().texOffs(0, 100).addBox(-24f, 0f, -2f, 24f, 1f, 16f),
                PartPose.offset(-6f, -4f, -2f));

        body.addOrReplaceChild("leg_front_left",
                CubeListBuilder.create().texOffs(90, 0).addBox(-2f, 0f, -2f, 4f, 10f, 4f),
                PartPose.offset(5f, 6f, -8f));
        body.addOrReplaceChild("leg_front_right",
                CubeListBuilder.create().texOffs(90, 20).addBox(-2f, 0f, -2f, 4f, 10f, 4f),
                PartPose.offset(-5f, 6f, -8f));
        body.addOrReplaceChild("leg_back_left",
                CubeListBuilder.create().texOffs(90, 40).addBox(-2f, 0f, -2f, 4f, 10f, 4f),
                PartPose.offset(5f, 6f, 8f));
        body.addOrReplaceChild("leg_back_right",
                CubeListBuilder.create().texOffs(90, 60).addBox(-2f, 0f, -2f, 4f, 10f, 4f),
                PartPose.offset(-5f, 6f, 8f));

        return LayerDefinition.create(meshDefinition, 256, 128);
    }

    /**
     * Wywoływane co klatkę na kliencie. Ustawia kąty poszczególnych części ciała
     * na podstawie stanu encji (lot/chodzenie, kierunek spojrzenia).
     *
     * UWAGA MAPPINGI/API: od pewnej wersji Mojang wprowadził rozdzielenie modelu
     * od "renderState" (EntityRenderState) - jeśli 26.2 wymaga innej sygnatury
     * niż ta klasyczna (entity, limbAngle, limbDistance, animationProgress,
     * headYaw, headPitch), zaktualizuj sygnaturę wg aktualnego EntityModel<T>
     * w dekompilowanym źródle.
     */
    @Override
    public void setupAnim(DragonEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.head.yRot = headYaw * Mth.DEG_TO_RAD;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD;

        boolean flying = entity.isVehicle() || !entity.onGround();
        float flapSpeed = flying ? 1.4f : 0.35f;
        float flapAmount = flying ? 0.9f : 0.25f;
        float flap = Mth.sin(animationProgress * flapSpeed) * flapAmount;
        this.leftWing.zRot = flap;
        this.rightWing.zRot = -flap;

        // Naprzemienny chód nóg podczas poruszania się po ziemi
        this.legs[0].xRot = Mth.cos(limbAngle * 0.6662f) * 1.4f * limbDistance;
        this.legs[1].xRot = Mth.cos(limbAngle * 0.6662f + (float) Math.PI) * 1.4f * limbDistance;
        this.legs[2].xRot = Mth.cos(limbAngle * 0.6662f + (float) Math.PI) * 1.4f * limbDistance;
        this.legs[3].xRot = Mth.cos(limbAngle * 0.6662f) * 1.4f * limbDistance;

        this.tail.yRot = Mth.sin(animationProgress * 0.3f) * 0.25f;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int light, int overlay, int color) {
        body.render(poseStack, vertexConsumer, light, overlay, color);
    }

    // ---------------------------------------------------------------------------------
    // ROZSZERZENIE O GECKOLIB (opcjonalne):
    // Jeśli chcesz pełnej animacji szkieletowej zamiast ręcznych obrotów ModelPart,
    // zamień dziedziczenie na GeoModel<DragonEntity> (biblioteka GeckoLib), wskaż pliki
    // .geo.json (geometria z Blockbench), .animation.json (animacje) i .png (tekstura),
    // a DragonRenderer zamień na GeoEntityRenderer<DragonEntity>. Reszta kodu (DragonEntity,
    // sieć, AI) pozostaje bez zmian, ponieważ logika serwera nie zależy od warstwy renderowania.
    // ---------------------------------------------------------------------------------
}
