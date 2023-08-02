/*
 * Copyright © Wynntils 2022-2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.settings.widgets;

import com.wynntils.core.config.Config;
import com.wynntils.utils.EnumUtils;
import com.wynntils.utils.mc.ComponentUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import java.lang.reflect.Type;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class EnumSettingsButton<E extends Enum<E>> extends GeneralSettingsButton {
    private final Config<E> config;
    private final List<E> enumConstants;

    public EnumSettingsButton(Config<E> config) {
        super(
                0,
                7,
                getWidth(config.getType()),
                FontRenderer.getInstance().getFont().lineHeight + 8,
                Component.literal(config.getValueString()),
                ComponentUtils.wrapTooltips(List.of(Component.literal(config.getDescription())), 150));
        this.config = config;
        enumConstants = EnumSet.allOf((Class<E>) config.getType()).stream().toList();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!clicked(mouseX, mouseY)) return false;

        int addToIndex;

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            addToIndex = 1;
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            addToIndex = -1;
        } else {
            return false;
        }

        E value = config.getValue();
        int nextIndex = (enumConstants.indexOf(value) + addToIndex + enumConstants.size()) % enumConstants.size();
        E nextValue = enumConstants.get(nextIndex);
        config.setValue(nextValue);
        setMessage(Component.literal(config.getValueString()));

        playDownSound(McUtils.mc().getSoundManager());

        return true;
    }

    @Override
    public void onPress() {
        // We use instead AbstractWidget#mouseClicked, because we also want to have an action on the right mouse button
    }

    private static <E extends Enum<E>> int getWidth(Type type) {
        Font font = FontRenderer.getInstance().getFont();
        int maxWidth = EnumSet.allOf((Class<E>) type).stream()
                .mapToInt(enumValue -> font.width(EnumUtils.toNiceString(enumValue)))
                .max()
                .orElse(0);
        return maxWidth + 8;
    }
}
