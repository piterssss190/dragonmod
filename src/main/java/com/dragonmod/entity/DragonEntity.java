package com.dragonmod.entity;

import com.dragonmod.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.syncher.EntityDataAccessor;
import net.minecraft.world.entity.syncher.EntityDataSerializers;
import net.minecraft.world.entity.syncher.SynchedEntityData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * DragonEntity - serce modyfikacji, przepisane na oficjalne mappingi Mojang
 * (Minecraft 26.2 "Chaos Cubed"; Fabric od 26.1 nie wspiera już Yarn).
 *
 * Dziedziczymy po TamableAnimal (odpowiednik Yarn "TameableEntity"), który
 * sam dziedziczy po Animal (odpowiednik Yarn "AnimalEntity"), dzięki czemu
 * "za darmo" dostajemy: system własności, siadanie, wiek/wzrost (getAge()),
 * oraz tryb miłosny do hodowli.
 *
 * Cała logika w tej klasie wykonuje się identycznie na serwerze dedykowanym
 * i w trybie integrated-server. Klient tylko odczytuje SynchedEntityData.
 */
public class DragonEntity extends TamableAnimal {

    // ================= DANE SYNCHRONIZOWANE (server -> wszyscy klienci) =================
    private static final EntityDataAccessor<Boolean> SADDLED =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);

    /** Liczba ticków potrzebna do dorośnięcia od wyklucia (baby -> adult). 3 dni MC. */
    public static final int TICKS_TO_ADULT = 24000 * 3;

    /** Pola sterowania pionowego (Space/Shift) - ustawiane wyłącznie przez ModNetworking na serwerze. */
    private boolean ascendInput;
    private boolean descendInput;

    public DragonEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.moveControl = new MoveControl(this); // ruch naziemny/hover, gdy bez pasażera
        this.setTame(false, false);
    }

    // =====================================================================================
    // createDragonAttributes() - definiuje BAZOWE statystyki. Wywoływane raz przy rejestracji
    // EntityType (patrz ModEntities#register -> FabricDefaultAttributeRegistry).
    // UWAGA MAPPINGI: stałe atrybutów zmieniły nazwy z GENERIC_* (Yarn) na krótsze
    // odpowiedniki w klasie Attributes (Mojang), np. GENERIC_MAX_HEALTH -> MAX_HEALTH.
    // =====================================================================================
    public static AttributeSupplier.Builder createDragonAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)     // 100 HP wymagane w specyfikacji
                .add(Attributes.ATTACK_DAMAGE, 8.0D)    // 8 ATK
                .add(Attributes.MOVEMENT_SPEED, 0.25D)  // prędkość naziemna
                .add(Attributes.FLYING_SPEED, 0.9D)     // prędkość lotu (używana w travel())
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ARMOR, 4.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SADDLED, false);
    }

    // =====================================================================================
    // GOALS (AI) - wykonują się WYŁĄCZNIE po stronie logicznej serwera.
    // Nazwy klas Goal w Mojang mappings różnią się od Yarn, np.:
    //   SwimGoal -> FloatGoal, EscapeDangerGoal -> PanicGoal, SitGoal -> SitWhenOrderedToGoal,
    //   WanderAroundFarGoal -> WaterAvoidingRandomStrollGoal, LookAtEntityGoal -> LookAtPlayerGoal,
    //   LookAroundGoal -> RandomLookAroundGoal.
    // =====================================================================================
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.5D));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        // TemptGoal - dzikie/nieoswojone smoki podążają za graczem trzymającym surowe mięso
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.1D, stack -> isTamingFood(stack), false));
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.2D, 8.0F, 2.0F, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    // =====================================================================================
    // STADIA WZROSTU - dynamiczny rozmiar hitboxa zależny od wieku (baby <-> adult).
    // =====================================================================================
    @Override
    public EntityDimensions getDimensions(Pose pose) {
        EntityDimensions adult = super.getDimensions(pose);
        if (this.isBaby()) {
            return adult.scale(0.45f);
        }
        return adult;
    }

    /** Współczynnik skali używany także przez DragonRenderer do skalowania modelu. */
    public float getScaleFactor() {
        return this.isBaby() ? 0.45f + 0.55f * getGrowthProgress() : 1.0f;
    }

    /** Zwraca postęp wzrostu (0.0 = świeżo wyklute jajo, 1.0 = dorosły). */
    private float getGrowthProgress() {
        if (!this.isBaby()) return 1.0f;
        return 1.0f - (Math.abs((float) this.getAge()) / (float) TICKS_TO_ADULT);
    }

    // =====================================================================================
    // mobInteract() - CAŁA logika oswajania, siodłania, karmienia, wsiadania.
    // Wywoływane po prawoklikcie gracza na encję. Modyfikacja stanu (setTame, setSaddled,
    // decrement itemu) wykonujemy TYLKO gdy !level.isClientSide, aby serwer pozostał
    // jedynym źródłem prawdy i nie doszło do desynchronizacji.
    // =====================================================================================
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Level level = this.level();

        // --- 1) Karmienie surowym mięsem/rybą przyspiesza wzrost MAŁEGO smoka ---
        if (this.isBaby() && isTamingFood(stack)) {
            if (!player.getAbilities().instabuild) stack.shrink(1);
            if (!level.isClientSide) {
                this.ageUp(1200); // przyspiesza dorastanie o 1200 ticków (metoda z Animal)
                level.playSound(null, blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 1f, 1f);
            }
            return InteractionResult.SUCCESS;
        }

        // --- 2) OSWAJANIE: dziki, dorosły smok + surowe mięso/ryba = 33% szansy na sukces ---
        if (!this.isTame() && !this.isBaby() && isTamingFood(stack)) {
            if (!player.getAbilities().instabuild) stack.shrink(1);
            if (!level.isClientSide) {
                if (this.random.nextInt(3) == 0) { // 1/3 = 33% szans
                    this.tame(player);              // ustawia właściciela i flagę oswojenia
                    this.navigation.stop();
                    this.setTarget(null);
                    this.setOrderedToSit(true);
                    spawnParticlesServer(ParticleTypes.HEART, 7);
                    level.playSound(null, blockPosition(), SoundEvents.FOX_SCREECH, SoundSource.NEUTRAL, 1f, 1.2f);
                } else {
                    spawnParticlesServer(ParticleTypes.SMOKE, 5);
                }
            }
            return InteractionResult.SUCCESS;
        }

        // --- 3) ZAKŁADANIE SIODŁA (wymagane do ujeżdżania) ---
        if (this.isTame() && this.isOwnedBy(player) && !this.isBaby()
                && !isSaddled() && stack.is(Items.SADDLE)) {
            if (!level.isClientSide) {
                this.setSaddled(true);
                if (!player.getAbilities().instabuild) stack.shrink(1);
                level.playSound(null, blockPosition(), SoundEvents.HORSE_SADDLE, SoundSource.NEUTRAL, 1f, 1f);
            }
            return InteractionResult.SUCCESS;
        }

        // --- 4) WSIADANIE: prawoklik pustą ręką na osiodłanego, dorosłego, oswojonego smoka ---
        if (this.isTame() && this.isOwnedBy(player) && isSaddled() && !this.isBaby()
                && stack.isEmpty() && !player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                player.startRiding(this);
            }
            return InteractionResult.SUCCESS;
        }

        // Pozostałe przypadki (m.in. karmienie Złotym Jabłkiem -> tryb miłosny do hodowli)
        // obsługuje domyślna logika Animal poprzez isFood() poniżej.
        return super.mobInteract(player, hand);
    }

    /** Item wywołujący tryb miłosny (hodowlę). Odpowiednik Yarn "isBreedingItem". */
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

    // =====================================================================================
    // HODOWLA - nadpisany finalizeSpawnChildFromBreeding() zamiast tworzyć od razu potomka,
    // dwa dorosłe, oswojone smoki w trybie miłosnym (nakarmione Złotym Jabłkiem) SKŁADAJĄ
    // Jajo Smoka w świecie. Wywoływane WYŁĄCZNIE po stronie serwera przez wewnętrzny
    // mechanizm hodowli Animal (BreedGoal).
    // =====================================================================================
    @Override
    public void finalizeSpawnChildFromBreeding(ServerLevel level, Animal mate, @Nullable net.minecraft.world.entity.AgeableMob child) {
        if (!(mate instanceof DragonEntity dragonMate)) return;

        this.setAge(6000);       // standardowe "odświeżenie" - blokada ponownej hodowli
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
        level.playSound(null, blockPosition(), SoundEvents.TURTLE_LAY_EGG, SoundSource.NEUTRAL, 0.8f, 1f);

        if (this.random.nextInt(7) == 0 && level.getGameRules().getBoolean(GameRules.RULE_MOBLOOT)) {
            level.addFreshEntity(new ExperienceOrb(level, this.getX(), this.getY(), this.getZ(), this.random.nextInt(7) + 1));
        }
    }

    /** Wymagane przez AgeableMob (kontrakt klasy bazowej) - używane tylko jako fallback. */
    @Nullable
    @Override
    public DragonEntity getBreedOffspring(ServerLevel level, net.minecraft.world.entity.AgeableMob mate) {
        DragonEntity baby = ModEntities.DRAGON.create(level);
        if (baby != null) baby.setAge(-TICKS_TO_ADULT);
        return baby;
    }

    // =====================================================================================
    // SIODŁO
    // =====================================================================================
    public boolean isSaddled() {
        return this.entityData.get(SADDLED);
    }

    public void setSaddled(boolean saddled) {
        this.entityData.set(SADDLED, saddled);
    }

    /** Wywoływane przez ModNetworking po odebraniu pakietu C2S od kontrolującego gracza. */
    public void setFlightInput(boolean ascend, boolean descend) {
        this.ascendInput = ascend;
        this.descendInput = descend;
    }

    // =====================================================================================
    // UJEŻDŻANIE - kontroler pasażera
    // =====================================================================================
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

    // =====================================================================================
    // travel() - KLUCZOWA metoda ruchu, wywoływana co tick (server-authoritative).
    //
    // Ruch przód/tył/boki: silnik gry AUTOMATYCZNIE synchronizuje wejście gracza
    // kontrolującego wierzchowca (pola LivingEntity#xxa (strafe) i #zza (forward) na
    // instancji gracza są ustawiane przez serwer po odebraniu inputu od klienta,
    // dokładnie tak samo jak przy koniu).
    //
    // Ruch pionowy (Space = wznoszenie, Shift = opadanie) nie ma odpowiednika w wanilii
    // dla latających wierzchowców, dlatego jest zsynchronizowany własnym pakietem C2S
    // (patrz ModNetworking.DragonFlightInputPayload) ustawiającym ascendInput/descendInput.
    //
    // UWAGA MAPPINGI: pola "sidewaysSpeed"/"forwardSpeed" z Yarn to w Mojang mappings
    // odpowiednio "xxa" i "zza" (LivingEntity) - historycznie krótkie, mało czytelne nazwy
    // pozostawione przez Mojang bez zmian od lat. Jeśli Twoje IDE ich nie znajdzie,
    // sprawdź aktualne pola w dekompilowanym LivingEntity dla 26.2.
    // =====================================================================================
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
                if (forward <= 0f) forward *= 0.25f; // wolniejszy lot wsteczny

                double vertical = 0.0D;
                if (this.ascendInput) vertical = 1.0D;       // Space
                else if (this.descendInput) vertical = -1.0D; // Shift

                float flySpeed = (float) this.getAttributeValue(Attributes.FLYING_SPEED);
                this.setSpeed(flySpeed);
                super.travel(new Vec3(strafe, vertical, forward));
                return;
            }

            // Brak pasażera-kontrolera -> normalny ruch sterowany przez AI/goals.
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
        // Uproszczenie fizyki lotu: smok nie podlega grawitacji jak wanilijne moby latające
        // (analogicznie do Enderdmoka/Ghasta). Realistyczne "opadanie" po zejściu z siodła
        // można dodać przez własny StopFlyingGoal - pominięte tu dla zwięzłości.
        return true;
    }

    @Override
    public boolean isPushable() {
        return !this.isVehicle();
    }

    // =====================================================================================
    // ZAPIS/ODCZYT NBT (persystencja świata).
    // UWAGA MAPPINGI: nazwy metod persystencji zmieniały się między wersjami MC
    // (addAdditionalSaveData/readAdditionalSaveData to nazwy stabilne od wielu lat
    // w mappingach Mojang, ale sprawdź dokumentację portowania 26.2 na
    // docs.fabricmc.net/develop/porting - część gier 26.x eksperymentuje z nowym
    // API ValueInput/ValueOutput zamiast surowego CompoundTag).
    // =====================================================================================
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Saddled", this.isSaddled());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setSaddled(tag.getBoolean("Saddled"));
    }
}
