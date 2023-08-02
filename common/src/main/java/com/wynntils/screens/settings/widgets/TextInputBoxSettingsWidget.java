/*
 * Copyright © Wynntils 2022-2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.settings.widgets;

import com.wynntils.core.config.Config;
import com.wynntils.screens.base.TextboxScreen;
import com.wynntils.screens.base.widgets.TextInputBoxWidget;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.render.FontRenderer;

public class TextInputBoxSettingsWidget<T> extends TextInputBoxWidget {
    protected final Config<T> configHolder;

    protected TextInputBoxSettingsWidget(Config<T> configHolder, TextboxScreen textboxScreen, int width) {
        super(0, 6, width, FontRenderer.getInstance().getFont().lineHeight + 8, null, textboxScreen);
        this.configHolder = configHolder;
        setTextBoxInput(configHolder.getConfigHolder().getValue().toString());
    }

    public TextInputBoxSettingsWidget(Config<T> configHolder, TextboxScreen textboxScreen) {
        this(configHolder, textboxScreen, 100);
    }

    @Override
    protected void onUpdate(String text) {
        T parsedValue = configHolder.getConfigHolder().tryParseStringValue(text);
        if (parsedValue != null) {
            if (!parsedValue.equals(configHolder.getConfigHolder().getValue())) {
                configHolder.getConfigHolder().setValue(parsedValue);
            }

            setRenderColor(CommonColors.GREEN);
        } else {
            setRenderColor(CommonColors.RED);
        }
    }
}
