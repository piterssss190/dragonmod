package com.dragonmod.entity;

import com.dragonmod.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * DragonEntity - serce modyfikacji.
 *
 * UWAGA MAPPINGI (potwierdzone realną kompilacją przeciwko 26.2):
 * - "EntityDataAccessor"/"SynchedEntityData"/"EntityDataSerializers" żyją
 *   w pakiecie net.minecraft.network.syncher (NIE world.entity.syncher,
 *   jak błędnie zakładałem wcześniej).
 * - Level#isClientSide to teraz METODA (isClientSide()), nie pole.
 * - Stałe SoundEvents (np. SoundEvents.GENERIC_EAT) są typu Holder<SoundEvent>,
 *   trzeba wywołać .value() żeby uzyskać surowy SoundEvent do playSound().
 * - EntityType#create() wymaga dodatkowo EntitySpawnReason.
 * - Baby-scaling hitboxa (getDimensions) jest teraz FINALNE w LivingEntity -
 *   nie da się go nadpisać; polegamy na wbudowanej skali Animal (isBaby()).
 * - mobInteract() w Animal jest PUBLIC, nie protected.
 * - Zrezygnowano tu z persystencji NBT (addAdditionalSaveData/read...) - w
 *   26.2 sygnatury zmieniły się na ValueOutput/ValueInput zamiast CompoundTag,
 *   a dokładny kształt tego API nie został jeszcze przeze mnie zweryfikowany.
 *   Flaga "Saddled" nie przetrwa więc zapisu/wczytania świata - do uzupełnienia.
 */
public class DragonEntity extends TamableAnimal {

    private static final EntityDataAccessor<Boolean> SADDLED =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);

    public static final int TICKS_TO_ADULT = 24000 * 3;

    private boolean ascendInput;
    private boolean descendInput;

    public DragonEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.moveControl = new MoveControl(this);
        this.setTame(false, false);
    }

    public static AttributeSupplier.Builder createDragonAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FLYING_SPEED, 0.9D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ARMOR, 4.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SADDLED, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.5D));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.1D, stack -> isTamingFood(stack), false));
        // UWAGA MAPPINGI: FollowOwnerGoal ma teraz 4 parametry (bez flagi "canFly").
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.2D, 8.0F, 2.0F));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    /** Współczynnik skali używany przez DragonRenderer do skalowania modelu (baby -> adult). */
    public float getScaleFactor() {
        return this.isBaby() ? 0.45f + 0.55f * getGrowthProgress() : 1.0f;
    }

    private float getGrowthProgress() {
        if (!this.isBaby()) return 1.0f;
        return 1.0f - (Math.abs((float) this.getAge()) / (float) TICKS_TO_ADULT);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Level level = this.level();

        if (this.isBaby() && isTamingFood(stack)) {
            if (!player.getAbilities().instabuild) stack.shrink(1);
            if (!level.isClientSide()) {
                this.ageUp(1200);
                level.playSound(null, blockPosition(), SoundEvents.GENERIC_EAT.value(), SoundSource.NEUTRAL, 1f, 1f);
            }
            return InteractionResult.SUCCESS;
        }

        if (!this.isTame() && !this.isBaby() && isTamingFood(stack)) {
            if (!player.getAbilities().instabuild) stack.shrink(1);
            if (!level.isClientSide()) {
                if (this.random.nextInt(3) == 0) {
                    this.tame(player);
                    this.navigation.stop();
                    this.setTarget(null);
                    this.setOrderedToSit(true);
                    spawnParticlesServer(ParticleTypes.HEART, 7);
                    level.playSound(null, blockPosition(), SoundEvents.FOX_SCREECH.value(), SoundSource.NEUTRAL, 1f, 1.2f);
                } else {
                    spawnParticlesServer(ParticleTypes.SMOKE, 5);
                }
            }
            return InteractionResult.SUCCESS;
        }

        if (this.isTame() && this.isOwnedBy(player) && !this.isBaby()
                && !isSaddled() && stack.is(Items.SADDLE)) {
            if (!level.isClientSide()) {
                this.setSaddled(true);
                if (!player.getAbilities().instabuild) stack.shrink(1);
                level.playSound(null, blockPosition(), SoundEvents.HORSE_SADDLE.value(), SoundSource.NEUTRAL, 1f, 1f);
            }
            return InteractionResult.SUCCESS;
        }

        if (this.isTame() && this.isOwnedBy(player) && isSaddled() && !this.isBaby()
                && stack.isEmpty() && !player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                player.startRiding(this);
            }
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE);
    }

    private boolean isTamingFood(ItemStack stack) {
        return stack.is(Items.BEEF) || stack.is(Items.COD) || stack.is(Items.SALMON);
    }

    private void spawnParticlesServer(ParticleOptions particle, int count) {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(particle, getX(), getY(0.5), getZ(), count, 0.3, 0.3, 0.3, 0.02);
        }
    }

    @Override
    public void finalizeSpawnChildFromBreeding(ServerLevel level, Animal mate, @Nullable net.minecraft.world.entity.AgeableMob child) {
        if (!(mate instanceof DragonEntity dragonMate)) return;

        this.setAge(6000);
        dragonMate.setAge(6000);
        this.resetLove();
        dragonMate.resetLove();

        BlockPos eggPos = this.blockPosition().above();
        if (level.getBlockState(eggPos).isAir()) {
            level.setBlockAndUpdate(eggPos, ModBlocks.DRAGON_EGG.defaultBlockState());
        } else {
            BlockPos alt = this.blockPosition().offset(1, 0, 0);
            if (level.getBlockState(alt).isAir()) {
                level.setBlockAndUpdate(alt, ModBlocks.DRAGON_EGG.defaultBlockState());
            }
        }

        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY(0.7), getZ(), 12, 0.4, 0.4, 0.4, 0.0);
        level.playSound(null, blockPosition(), SoundEvents.TURTLE_LAY_EGG.value(), SoundSource.NEUTRAL, 0.8f, 1f);
    }

    /**
     * Fallback wymagany przez kontrakt klasy bazowej w niektórych wersjach API.
     * Bez adnotacji @Override celowo - jeśli w 26.2 ta metoda nazywa się/wygląda
     * inaczej, kompilator po prostu zignoruje tę deklarację jako martwy kod
     * zamiast zgłaszać błąd (breeding i tak jest w pełni obsłużone przez
     * finalizeSpawnChildFromBreeding powyżej).
     */
    @Nullable
    public DragonEntity getBreedOffspring(ServerLevel level, net.minecraft.world.entity.AgeableMob mate) {
        DragonEntity baby = ModEntities.DRAGON.create(level, EntitySpawnReason.BREEDING);
        if (baby != null) baby.setAge(-TICKS_TO_ADULT);
        return baby;
    }

    public boolean isSaddled() {
        return this.entityData.get(SADDLED);
    }

    public void setSaddled(boolean saddled) {
        this.entityData.set(SADDLED, saddled);
    }

    public void setFlightInput(boolean ascend, boolean descend) {
        this.ascendInput = ascend;
        this.descendInput = descend;
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        if (this.getFirstPassenger() instanceof Player player && this.isSaddled() && this.isOwnedBy(player)) {
            return player;
        }
        return null;
    }

    @Override
    protected boolean canBeControlledByRider() {
        return this.getControllingPassenger() != null;
    }

    @Override
    public void travel(Vec3 movementInput) {
        if (this.isAlive()) {
            if (this.isVehicle() && canBeControlledByRider()) {
                LivingEntity controller = this.getControllingPassenger();

                this.setYRot(controller.getYRot());
                this.yRotO = this.getYRot();
                this.setXRot(controller.getXRot() * 0.5f);
                this.setRot(this.getYRot(), this.getXRot());
                this.yBodyRot = this.getYRot();
                this.yHeadRot = this.yBodyRot;

                float strafe = controller.xxa * 0.5f;
                float forward = controller.zza;
                if (forward <= 0f) forward *= 0.25f;

                double vertical = 0.0D;
                if (this.ascendInput) vertical = 1.0D;
                else if (this.descendInput) vertical = -1.0D;

                float flySpeed = (float) this.getAttributeValue(Attributes.FLYING_SPEED);
                this.setSpeed(flySpeed);
                super.travel(new Vec3(strafe, vertical, forward));
                return;
            }

            if (this.isBaby()) {
                this.setSpeed((float) this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.6f);
            } else {
                this.setSpeed((float) this.getAttributeValue(Attributes.MOVEMENT_SPEED));
            }
        }
        super.travel(movementInput);
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return !this.isVehicle();
    }
}
