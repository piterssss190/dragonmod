# Dragon Mod (Fabric) — Minecraft 26.2 "Chaos Cubed"

## Zmiana mappingów: Yarn -> Mojang (ważne!)
Od Minecrafta 26.1 gra jest dystrybuowana **nieobfuskowana**, z oficjalnymi
nazwami Mojang (parametry, klasy, metody) - w efekcie Fabric **przestał
oficjalnie wspierać Yarn mappings**. Ten projekt został w całości przepisany
z Yarn na Mojang mappings. Najważniejsze zmiany nazewnictwa, na wypadek gdy
porównujesz to z wcześniejszą wersją kodu (pod 1.21.x/Yarn):

| Yarn (stare)         | Mojang (aktualne)      |
|----------------------|------------------------|
| `PlayerEntity`        | `Player`               |
| `World` / `ServerWorld` | `Level` / `ServerLevel` |
| `TameableEntity`      | `TamableAnimal`        |
| `AnimalEntity`        | `Animal`               |
| `PassiveEntity`       | `AgeableMob`           |
| `ActionResult`        | `InteractionResult`    |
| `Hand`                | `InteractionHand`      |
| `Identifier`          | `ResourceLocation`     |
| `NbtCompound`         | `CompoundTag`          |
| `DataTracker`         | `SynchedEntityData`    |
| `TrackedData`         | `EntityDataAccessor`   |
| `Vec3d`               | `Vec3`                 |
| `MatrixStack`         | `PoseStack`            |
| `MathHelper`          | `Mth`                  |
| `MobEntityRenderer`   | `MobRenderer`          |
| `EntityRendererFactory` | `EntityRendererProvider` |
| `CustomPayload`       | `CustomPacketPayload`  |
| `PacketCodec`/`PacketByteBuf` | `StreamCodec`/`FriendlyByteBuf` |
| `interactMob()`       | `mobInteract()`        |
| `isTamed()`/`setTamed()` | `isTame()`/`setTame()`/`tame(Player)` |
| `getBreedingAge()`/`setBreedingAge()` | `getAge()`/`setAge()` |
| `isBreedingItem()`    | `isFood()`             |

## Build toolchain
- Loom **1.15+** (nie remapuje już kodu - stąd `implementation` zamiast
  `modImplementation`, i zwykły `jar` zamiast `remapJar`)
- Gradle **9.4.0+**
- Fabric Loader **0.18.4+**
- Fabric API **0.158.0+26.2**
- **Brak bloku `mappings` w `build.gradle`** - POTWIERDZONE oficjalnie przez
  Fabric (ogłoszenie dla 26.1, fabricmc.net/2026/03/14/261.html): "mappings
  are not provided for 26.1" - dla wersji nieobfuskowanych (26.1+) w ogóle
  nie deklaruje się mappingów. (Wcześniejsza wersja tego README błędnie
  sugerowała `loom.officialMojangMappings()` - to dotyczy tylko starszych,
  obfuskowanych wersji i celowo zawodzi dla 26.2.)

Sprawdź zawsze aktualne, dokładne numery na https://fabricmc.net/develop
przed buildem - mogły wyjść nowsze hotfixy niż te wpisane w `gradle.properties`.

## Status kodu - co jest pewne, a co wymaga weryfikacji
Nie miałem możliwości skompilowania tego projektu w tym środowisku (brak
internetu/Gradle/zależności Minecrafta w tej piaskownicy) ani dostępu do
dekompilowanego źródła 26.2 - konwersja Yarn->Mojang została wykonana na
podstawie wieloletnio stabilnych nazw w mappingach Mojang. Obszary o
**najwyższym ryzyku rozjazdu** względem realnego 26.2 (oznaczone też
komentarzami w kodzie):

1. **Renderowanie klienckie** (`DragonModel`, `DragonRenderer`) - to
   historycznie najbardziej zmienna część API. Nowsze wersje mogą wymagać
   wzorca `EntityRenderState` (oddzielny obiekt stanu zamiast przekazywania
   samej encji do modelu).
2. **Zapis/odczyt NBT** (`addAdditionalSaveData`/`readAdditionalSaveData`) -
   część gałęzi 26.x eksperymentuje z nowym API `ValueInput`/`ValueOutput`.
3. **Pola ruchu gracza** (`controller.xxa`/`controller.zza` w `travel()`) -
   to historycznie nieprzejrzyste, krótkie nazwy pól Mojang; zweryfikuj je
   w dekompilowanym `LivingEntity`.
4. ~~Dokładna wersja wymaganego JDK dla 26.2~~ **POTWIERDZONE przez realny build:
   Minecraft 26.2 wymaga Java 25** (Fabric Loom zgłosił to wprost przy
   konfigurowaniu Minecrafta - `sourceCompatibility`/`targetCompatibility`
   w `build.gradle` są już ustawione na `VERSION_25`).

Jeśli kompilator zgłosi błędy w tych miejscach, sprawdź aktualny kształt
klas przez dekompilację (IntelliJ "Go to Source" po podłączeniu Loom, lub
wtyczkę Ravel) i popraw nazwy pól/metod.

## Struktura projektu

```
dragonmod/
├── build.gradle
├── gradle.properties
└── src/main/
    ├── java/com/dragonmod/
    │   ├── ModMain.java              # wspólny entrypoint (server+client)
    │   ├── entity/
    │   │   ├── ModEntities.java      # rejestracja EntityType + atrybutów
    │   │   └── DragonEntity.java     # cała logika smoka
    │   ├── block/
    │   │   ├── ModBlocks.java
    │   │   └── DragonEggBlock.java   # inkubacja jaja
    │   ├── item/
    │   │   └── ModItems.java
    │   ├── network/
    │   │   └── ModNetworking.java    # pakiet C2S sterowania pionowego
    │   └── client/                   # <- WYŁĄCZNIE strona klienta
    │       ├── ModMainClient.java
    │       ├── DragonModel.java
    │       └── DragonRenderer.java
    └── resources/
        └── fabric.mod.json
```

## Dlaczego mod działa na serwerze dedykowanym
1. Cała logika gry leży w `ModMain`, `entity/`, `block/`, `network/` -
   żadna z tych klas nie importuje niczego z pakietu `client` ani z
   `net.minecraft.client.*`.
2. `DragonRenderer`/`DragonModel` są rejestrowane wyłącznie w
   `ModMainClient`, wskazanym w `fabric.mod.json` jako entrypoint
   `"client"` - Fabric Loader ładuje go tylko na kliencie.
3. Zmiany stanu (`tame()`, `setSaddled()`, `ageUp()`) wykonywane są tylko
   gdy `!level.isClientSide`, więc serwer jest jedynym "źródłem prawdy".
4. Ruch przód/tył/boki wykorzystuje natywny mechanizm silnika (jak przy
   koniu); tylko oś pionowa (Space/Shift) używa własnego pakietu
   `DragonFlightInputPayload`.

## Dalszy rozwój (poza zakresem tego przykładu)
- Podmiana `DragonModel`/`DragonRenderer` na GeckoLib dla pełnej animacji
  szkieletowej (komentarz na końcu `DragonModel.java`).
- Prawdziwa tekstura `assets/dragonmod/textures/entity/dragon.png`.
- Lang file `assets/dragonmod/lang/pl_pl.json`.
- Model itemu jaja/spawn egga.
