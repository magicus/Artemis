/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wynn.handleditems;

import com.wynntils.handlers.item.ItemAnnotation;
import java.util.HashMap;
import java.util.Map;

public class WynnItem implements ItemAnnotation {
    private Map<Class<?>, Object> cache = new HashMap<>();
    private boolean searched = false;

    public <T> T getCached(Class<T> clazz) {
        return (T) cache.get(clazz);
    }

    public <T> void storeInCache(T obj) {
        cache.put(obj.getClass(), obj);
    }

    public void setSearched(boolean searched) {
        this.searched = searched;
    }

    public boolean isSearched() {
        return searched;
    }
}