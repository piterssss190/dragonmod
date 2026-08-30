package com.dragonmod.entity;

import com.dragonmod.ModMain;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Rejestracja EntityType (kod wspólny server+client - NIGDY nie odwołuje się
 * do klas z pakietu com.dragonmod.client ani z net.minecraft.client.*).
 *
 * UWAGA MAPPINGI: w Mojang mappings rejestr faktycznych obiektów to
 * BuiltInRegistries.ENTITY_TYPE, a Registries.ENTITY_TYPE to jedynie
 * ResourceKey<Registry<EntityType<?>>> używany np. do budowy EntityType.Builder.
 */
public class ModEntities {

    private static final ResourceKey<EntityType<?>> DRAGON_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, ModMain.id("dragon"));

    public static final EntityType<DragonEntity> DRAGON = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ModMain.id("dragon"),
            EntityType.Builder
                    .of(DragonEntity::new, MobCategory.CREATURE)
                    // Rozmiar bazowy (dorosły) - zwiększony, żeby pasował do
                    // powiększonej geometrii modelu (poprzedni 2.2x2.6 wyglądał
                    // niewspółmiernie małe jak na "smoka").
                    .sized(3.2f, 3.2f)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .build(DRAGON_KEY)
    );

    public static void register() {
        // Rejestracja bazowych atrybutów (HP, obrażenia, prędkość) - klasa kompatybilności
        // Fabric API (nie wanilijna) - MUSI wykonać się przed pierwszym spawnem encji.
        FabricDefaultAttributeRegistry.register(DRAGON, DragonEntity.createDragonAttributes());
    }
}
