/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.attributes.resolving;

import com.wynntils.services.mapdata.attributes.type.MapAttributes;
import com.wynntils.services.mapdata.attributes.type.MapDecoration;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.type.TextShadow;
import java.util.Optional;
import java.util.function.Function;

public abstract class DerivedMapAttributes implements MapAttributes {
    protected abstract <T> Optional<T> getAttribute(Function<MapAttributes, Optional<T>> getter);

    @Override
    public Optional<String> getLabel() {
        return getAttribute(MapAttributes::getLabel);
    }

    @Override
    public Optional<String> getIconId() {
        return getAttribute(MapAttributes::getIconId);
    }

    @Override
    public Optional<Integer> getPriority() {
        return getAttribute(MapAttributes::getPriority);
    }

    @Override
    public Optional<Integer> getLevel() {
        return getAttribute(MapAttributes::getLevel);
    }

    @Override
    public Optional<CustomColor> getLabelColor() {
        return getAttribute(MapAttributes::getLabelColor);
    }

    @Override
    public Optional<TextShadow> getLabelShadow() {
        return getAttribute(MapAttributes::getLabelShadow);
    }

    @Override
    public Optional<CustomColor> getIconColor() {
        return getAttribute(MapAttributes::getIconColor);
    }

    @Override
    public Optional<MapDecoration> getIconDecoration() {
        return getAttribute(MapAttributes::getIconDecoration);
    }
}
