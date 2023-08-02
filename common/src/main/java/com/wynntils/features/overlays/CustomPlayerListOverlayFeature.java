/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.overlays;

import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.overlays.Overlay;
import com.wynntils.core.consumers.overlays.annotations.OverlayInfo;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.mc.event.RenderEvent;
import com.wynntils.overlays.CustomPlayerListOverlay;

@ConfigCategory(Category.OVERLAYS)
public class CustomPlayerListOverlayFeature extends Feature {
    @OverlayInfo(renderType = RenderEvent.ElementType.GUI)
    private final Overlay customPlayerListOverlay = new CustomPlayerListOverlay();
}
