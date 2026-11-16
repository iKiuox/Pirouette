package io.github.ikiuox.pirouette.gui;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Adds the "Configure" button for Pirouette in Mod Menu (optional dependency). */
public final class PirouetteModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new PirouetteConfigScreen(parent);
    }
}
