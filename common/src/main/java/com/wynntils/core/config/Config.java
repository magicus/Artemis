/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.config;

import com.wynntils.core.components.Managers;
import com.wynntils.core.json.PersistedValue;

public class Config<T> extends PersistedValue<T> {
    public Config(T value) {
        super(value);
    }

    @Override
    public void touched() {
        Managers.Config.saveConfig();
    }

    public void updateConfig(T value) {
        this.value = value;
    }
}
