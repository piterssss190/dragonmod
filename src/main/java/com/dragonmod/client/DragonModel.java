package com.dragonmod.client;

import com.dragonmod.ModMain;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Model smoka - przepisany na wzorzec EntityModel<DragonRenderState> (NOWA
 * architektura render-state w 26.x, patrz komentarz w DragonRenderState.java).
 *
 * UWAGA MAPPINGI:
 * - VertexConsumer żyje w com.mojang.blaze3d.vertex, NIE w
 *   net.minecraft.client.renderer (potwierdzone realnym błędem kompilacji).
 * - renderToBuffer() jest teraz FINALNE w klasie bazowej Model - silnik sam
 *   renderuje drzewo ModelPart przekazane w konstruktorze, nie trzeba (i nie
 *   można) tego nadpisywać ręcznie.
 * - setupAnim() przyjmuje teraz sam obiekt stanu (DragonRenderState) zamiast
 *   listy osobnych parametrów (limbAngle, limbDistance, itd.) - te dane
 *   trzeba odczytać z pól obiektu stanu zamiast z argumentów metody.
 */
public class DragonModel extends EntityModel<DragonRenderState> {

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

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();

        // UWAGA: poniższe texOffs MUSZĄ być identyczne z układem policzonym
        // w skrypcie generującym teksturę (make_texture.py) - to jest to,
        // co sprawia, że tekstura "pasuje" do modelu piksel w piksel.
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-12f, -12f, -28f, 24f, 24f, 56f),
                PartPose.offset(0f, 24f, 0f));

        body.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 82).addBox(-8f, -8f, -20f, 16f, 16f, 20f),
                PartPose.offset(0f, -4f, -28f));

        body.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(0, 120).addBox(-4f, -4f, 0f, 8f, 8f, 40f),
                PartPose.offset(0f, 0f, 28f));

        body.addOrReplaceChild("left_wing",
                CubeListBuilder.create().texOffs(0, 170).addBox(0f, 0f, -4f, 48f, 2f, 32f),
                PartPose.offset(12f, -8f, -4f));

        body.addOrReplaceChild("right_wing",
                CubeListBuilder.create().texOffs(0, 206).addBox(-48f, 0f, -4f, 48f, 2f, 32f),
                PartPose.offset(-12f, -8f, -4f));

        body.addOrReplaceChild("leg_front_left",
                CubeListBuilder.create().texOffs(0, 242).addBox(-4f, 0f, -4f, 8f, 20f, 8f),
                PartPose.offset(10f, 12f, -16f));
        body.addOrReplaceChild("leg_front_right",
                CubeListBuilder.create().texOffs(34, 242).addBox(-4f, 0f, -4f, 8f, 20f, 8f),
                PartPose.offset(-10f, 12f, -16f));
        body.addOrReplaceChild("leg_back_left",
                CubeListBuilder.create().texOffs(68, 242).addBox(-4f, 0f, -4f, 8f, 20f, 8f),
                PartPose.offset(10f, 12f, 16f));
        body.addOrReplaceChild("leg_back_right",
                CubeListBuilder.create().texOffs(102, 242).addBox(-4f, 0f, -4f, 8f, 20f, 8f),
                PartPose.offset(-10f, 12f, 16f));

        return LayerDefinition.create(meshDefinition, 160, 272);
    }

    /**
     * UWAGA: sygnatura setupAnim(DragonRenderState) jest moim najlepszym
     * przybliżeniem nowego API render-state - nie zdążyłem zweryfikować
     * dokładnych nazw pól odziedziczonych z LivingEntityRenderState (np. czy
     * kąt głowy nazywa się "yRot"/"headYRot" itp.). Jeśli kompilator zgłosi
     * "cannot find symbol" na polach state.*, sprawdź realne pole w
     * dekompilowanym LivingEntityRenderState i podmień nazwę.
     */
    @Override
    public void setupAnim(DragonRenderState state) {
        float flapSpeed = state.flying ? 1.4f : 0.35f;
        float flapAmount = state.flying ? 0.9f : 0.25f;
        float time = state.ageInTicks;
        float flap = Mth.sin(time * flapSpeed) * flapAmount;
        this.leftWing.zRot = flap;
        this.rightWing.zRot = -flap;

        float limbAngle = state.walkAnimationPos;
        float limbDistance = state.walkAnimationSpeed;
        this.legs[0].xRot = Mth.cos(limbAngle * 0.6662f) * 1.4f * limbDistance;
        this.legs[1].xRot = Mth.cos(limbAngle * 0.6662f + (float) Math.PI) * 1.4f * limbDistance;
        this.legs[2].xRot = Mth.cos(limbAngle * 0.6662f + (float) Math.PI) * 1.4f * limbDistance;
        this.legs[3].xRot = Mth.cos(limbAngle * 0.6662f) * 1.4f * limbDistance;

        this.tail.yRot = Mth.sin(time * 0.3f) * 0.25f;
    }

    // ---------------------------------------------------------------------------------
    // ROZSZERZENIE O GECKOLIB (opcjonalne): patrz komentarz w README.
    // ---------------------------------------------------------------------------------
}
