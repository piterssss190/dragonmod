package com.dragonmod.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * Klasa stanu renderowania - NOWY wzorzec w Minecraft 26.x (potwierdzony
 * realną kompilacją oraz oficjalnym tutorialem docs.fabricmc.net/develop/
 * entities/first-entity). Zamiast przekazywać samą encję bezpośrednio do
 * modelu/renderera co klatkę, silnik najpierw "wyciąga" (extract) potrzebne
 * dane z encji do lekkiego obiektu stanu (na wątku logiki gry), a dopiero
 * ten obiekt trafia do modelu/renderera (na wątku renderowania). To m.in.
 * pozwala bezpieczniej dzielić pracę między wątki.
 *
 * Przechowujemy tu tylko WŁASNE, dodatkowe pola potrzebne do animacji smoka -
 * podstawowe rzeczy (pozycja, obrót, wiek/skala) są już dziedziczone z
 * LivingEntityRenderState.
 */
public class DragonRenderState extends LivingEntityRenderState {
    public boolean saddled;
    public boolean flying;
    public float scaleFactor = 1.0f;
}
