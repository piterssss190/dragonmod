package com.dragonmod.block;

import com.dragonmod.entity.DragonEntity;
import com.dragonmod.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Blok Jaja Smoka. Przepisany na mappingi Mojang: Yarn "ShapeContext" ->
 * Mojang "CollisionContext", Yarn "BlockView" -> Mojang "BlockGetter",
 * Yarn "IntProperty" -> Mojang "IntegerProperty", Yarn "StateManager" ->
 * Mojang "StateDefinition", Yarn "VoxelShapes" -> Mojang "Shapes",
 * Yarn "Random" (util.math.random) -> Mojang "RandomSource".
 *
 * Umieszczony w promieniu 2 bloków od ognia/lawy/kampfire, co losowy tick
 * (randomTick - naturalny mechanizm serwera) zwiększa postęp inkubacji.
 * Po osiągnięciu maksimum blok znika, a w jego miejscu spawnuje się
 * Mały Smok (Baby Dragon).
 */
public class DragonEggBlock extends Block {

    /** 0 = świeże jajo, 3 = gotowe do wyklucia w następnym ticku. */
    public static final IntegerProperty HATCH_PROGRESS = IntegerProperty.create("hatch_progress", 0, 3);
    private static final VoxelShape SHAPE = Shapes.box(0.2, 0.0, 0.2, 0.8, 0.8, 0.8);

    public DragonEggBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any().setValue(HATCH_PROGRESS, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HATCH_PROGRESS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    /** Wywoływane cyklicznie przez serwer (nigdy po stronie klienta) dla losowych bloków. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!isNearHeatSource(level, pos)) return;

        int progress = state.getValue(HATCH_PROGRESS);
        if (progress < 3) {
            level.setBlock(pos, state.setValue(HATCH_PROGRESS, progress + 1), Block.UPDATE_CLIENTS);
            level.sendParticles(ParticleTypes.CRIT,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.0);
        } else {
            hatch(level, pos);
        }
    }

    private boolean isNearHeatSource(Level level, BlockPos pos) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 2, 2))) {
            BlockState neighbor = level.getBlockState(p);
            if (neighbor.is(Blocks.FIRE) || neighbor.is(Blocks.SOUL_FIRE)
                    || neighbor.is(Blocks.LAVA) || neighbor.is(Blocks.MAGMA_BLOCK)
                    || neighbor.is(Blocks.CAMPFIRE) || neighbor.is(Blocks.SOUL_CAMPFIRE)) {
                return true;
            }
        }
        return false;
    }

    private void hatch(ServerLevel level, BlockPos pos) {
        level.removeBlock(pos, false);

        // UWAGA MAPPINGI: EntityType#create() wymaga teraz dodatkowo EntitySpawnReason.
        DragonEntity baby = ModEntities.DRAGON.create(level, EntitySpawnReason.TRIGGERED);
        if (baby != null) {
            // UWAGA MAPPINGI: "moveTo(double,double,double,float,float)" nie istnieje
            // w tej formie w 26.2 - używamy oddzielnych, stabilnych od dawna metod.
            baby.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            baby.setYRot(0f);
            baby.setXRot(0f);
            baby.setAge(-DragonEntity.TICKS_TO_ADULT); // stadium: Baby Dragon
            level.addFreshEntity(baby);
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                    pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 20, 0.3, 0.3, 0.3, 0.1);
            level.playSound(null, pos, SoundEvents.TURTLE_EGG_HATCH, SoundSource.NEUTRAL, 1f, 1f);
        }
    }
}
