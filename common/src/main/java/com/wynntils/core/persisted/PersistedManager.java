/*
 * Copyright © Wynntils 2023.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.persisted;

import com.google.common.base.CaseFormat;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Manager;
import com.wynntils.core.components.Managers;
import com.wynntils.core.consumers.overlays.Overlay;
import com.wynntils.core.json.JsonManager;
import com.wynntils.core.mod.event.WynncraftConnectionEvent;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.NullableConfig;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.type.Pair;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.commons.lang3.reflect.FieldUtils;

public final class PersistedManager extends Manager {
    private static final long SAVE_INTERVAL = 10_000;

    private static final File STORAGE_DIR = WynntilsMod.getModStorageDir("persisted");
    private static final String FILE_SUFFIX = ".data.json";
    private final File userPersistedFile;

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private final Map<PersistedValue<?>, PersistedMetadata<?>> metadatas = new HashMap<>();
    private final Set<PersistedValue<?>> persisteds = new TreeSet<>();

    private long lastPersisted;
    private boolean scheduledPersist;

    private boolean persistedInitialized = false;

    public PersistedManager(JsonManager jsonManager) {
        super(List.of(jsonManager));
        userPersistedFile = new File(STORAGE_DIR, McUtils.mc().getUser().getUuid() + FILE_SUFFIX);
    }

    public void setRaw(PersistedValue<?> persisted, Object value) {
        // Hack to allow Config/Storage manager to get around package limitations
        // Will be removed when refactoring is done
        persisted.setRaw(value);
    }

    public void registerOwner(PersistedOwner owner) {
        verifyAnnotations(owner);

        Map<PersistedValue<?>, PersistedMetadata<?>> newMetadatas = new HashMap<>();

        getPersisted(owner, Config.class).forEach(p -> {
            Field configField = p.a();
            Config<?> configObj;
            try {
                configObj = (Config<?>) FieldUtils.readField(configField, owner, true);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Cannot read persisted field: " + configField, e);
            }

            PersistedMetadata<?> metadata = createMetadata((PersistedValue<?>) configObj, owner, configField, p.b());
            newMetadatas.put(configObj, metadata);
        });

        metadatas.putAll(newMetadatas);
        persisteds.addAll(newMetadatas.keySet());
    }

    public List<Pair<Field, Persisted>> getPersisted(PersistedOwner owner, Class<? extends PersistedValue> clazzType) {
        // Get pairs of field and annotation for all persisted values of the requested type
        return Arrays.stream(FieldUtils.getFieldsWithAnnotation(owner.getClass(), Persisted.class))
                .filter(field -> clazzType.isAssignableFrom(field.getType()))
                .map(field -> Pair.of(field, field.getAnnotation(Persisted.class)))
                .toList();
    }

    public void verifyAnnotations(PersistedOwner owner) {
        // Verify that only persistable fields are annotated
        Arrays.stream(FieldUtils.getFieldsWithAnnotation(owner.getClass(), Persisted.class))
                .forEach(field -> {
                    if (!PersistedValue.class.isAssignableFrom(field.getType())) {
                        throw new RuntimeException(
                                "A non-persistable class was marked with @Persisted annotation: " + field);
                    }
                });

        // Verify that we have not missed to annotate a persistable field
        FieldUtils.getAllFieldsList(owner.getClass()).stream()
                .filter(field -> PersistedValue.class.isAssignableFrom(field.getType()))
                .forEach(field -> {
                    Persisted annotation = field.getAnnotation(Persisted.class);
                    if (annotation == null) {
                        throw new RuntimeException("A persisted datatype is missing @Persisted annotation:" + field);
                    }
                });
    }

    public <T> PersistedMetadata<T> getMetadata(PersistedValue<T> persisted) {
        return (PersistedMetadata<T>) metadatas.get(persisted);
    }

    private <T> PersistedMetadata<T> createMetadata(
            PersistedValue<T> persisted, PersistedOwner owner, Field configField, Persisted annotation) {
        Type valueType = Managers.Json.getJsonValueType(configField);
        String fieldName = configField.getName();

        String i18nKeyOverride = annotation.i18nKey();

        // save default value to enable easy resetting
        // We have to deep copy the value, so it is guaranteed that we detect changes
        T defaultValue = Managers.Json.deepCopy(persisted.get(), valueType);

        boolean allowNull = valueType instanceof Class<?> clazz && NullableConfig.class.isAssignableFrom(clazz);
        if (defaultValue == null && !allowNull) {
            throw new RuntimeException("Default config value is null in " + owner.getJsonName() + "." + fieldName);
        }

        String jsonName = getPrefix(owner) + owner.getJsonName() + "." + fieldName;

        return new PersistedMetadata<T>(
                owner, fieldName, valueType, defaultValue, i18nKeyOverride, allowNull, jsonName);
    }

    private String getPrefix(PersistedOwner owner) {
        // "featureName.overlayName.settingName" vs "featureName.settingName"
        if (!(owner instanceof Overlay overlay)) return "";

        String name = overlay.getDeclaringClassName();
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, name) + ".";
    }

    /// ============================================================================================

    public void initFeatures() {
        readFromJson();

        persistedInitialized = true;

        // We might have missed a persist call in between feature init and persisted manager init
        persist();
    }

    @SubscribeEvent
    public void onWynncraftDisconnect(WynncraftConnectionEvent.Disconnected event) {
        // Always save when disconnecting
        writeToJson();
    }

    void persist() {
        // We cannot persist before the persisted is initialized, or we will overwrite our persisted
        if (!persistedInitialized || scheduledPersist) return;

        long now = System.currentTimeMillis();
        long delay = Math.max((lastPersisted + SAVE_INTERVAL) - now, 0);

        executor.schedule(
                () -> {
                    scheduledPersist = false;
                    lastPersisted = System.currentTimeMillis();
                    writeToJson();
                },
                delay,
                TimeUnit.MILLISECONDS);
        scheduledPersist = true;
    }

    private void readFromJson() {
        JsonObject persistedJson = Managers.Json.loadPreciousJson(userPersistedFile);
        metadatas.forEach((persisted, metadata) -> {
            String jsonName = metadata.getJsonName();
            if (!persistedJson.has(jsonName)) return;

            // read value and update option
            JsonElement jsonElem = persistedJson.get(jsonName);
            Type valueType = metadata.getValueType();
            Object value = Managers.Json.GSON.fromJson(jsonElem, valueType);
            setRaw(persisted, value);

            PersistedOwner owner = metadata.getOwner();
            // FIXME
            // owner.onPersistedLoad();
        });
    }

    private void writeToJson() {
        JsonObject persistedJson = new JsonObject();

        metadatas.forEach((persisted, metadata) -> {
            String jsonName = metadata.getJsonName();
            Type valueType = metadata.getValueType();
            JsonElement jsonElem = Managers.Json.GSON.toJsonTree(persisted.get(), valueType);
            persistedJson.add(jsonName, jsonElem);
        });

        Managers.Json.savePreciousJson(userPersistedFile, persistedJson);
    }
}
