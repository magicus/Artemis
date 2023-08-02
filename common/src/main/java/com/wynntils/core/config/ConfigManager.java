/*
 * Copyright © Wynntils 2022-2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Manager;
import com.wynntils.core.components.Managers;
import com.wynntils.core.config.upfixers.ConfigUpfixerManager;
import com.wynntils.core.consumers.Translatable;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.features.FeatureManager;
import com.wynntils.core.consumers.overlays.DynamicOverlay;
import com.wynntils.core.consumers.overlays.Overlay;
import com.wynntils.core.consumers.overlays.OverlayManager;
import com.wynntils.core.json.JsonManager;
import com.wynntils.utils.JsonUtils;
import com.wynntils.utils.mc.McUtils;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.apache.commons.lang3.reflect.FieldUtils;

public final class ConfigManager extends Manager {
    private static final File CONFIG_DIR = WynntilsMod.getModStorageDir("config");
    private static final String FILE_SUFFIX = ".conf.json";
    private static final File DEFAULT_CONFIG = new File(CONFIG_DIR, "default" + FILE_SUFFIX);
    private static final String OVERLAY_GROUPS_JSON_KEY = "overlayGroups";
    private static final Set<Config<?>> CONFIGS = new TreeSet<>();

    private final File userConfig;
    private JsonObject configObject;

    public ConfigManager(
            ConfigUpfixerManager configUpfixerManager,
            JsonManager jsonManager,
            FeatureManager feature,
            OverlayManager overlay) {
        super(List.of(configUpfixerManager, jsonManager, feature, overlay));

        userConfig = new File(CONFIG_DIR, McUtils.mc().getUser().getUuid() + FILE_SUFFIX);
    }

    public void init() {
        // First, we load the config file
        configObject = Managers.Json.loadPreciousJson(userConfig);

        // Register all features and overlays
        Managers.Feature.getFeatures().forEach(this::registerFeature);

        // Now, we have to apply upfixers, before any config loading happens
        if (Managers.ConfigUpfixer.runUpfixers(configObject, CONFIGS)) {
            Managers.Json.savePreciousJson(userConfig, configObject);
        }

        // Finish off the config init process

        // Load configs for all features
        Managers.Config.reloadConfiguration();

        // Save config file after loading all configurables' options
        Managers.Config.saveConfig();

        // Create default config file containing all configurables' options
        Managers.Config.saveDefaultConfig();
    }

    private void registerFeature(Feature feature) {
        registerConfigOptions(feature);

        for (Overlay overlay : Managers.Overlay.getFeatureOverlays(feature).stream()
                .filter(overlay -> Managers.Overlay.getFeatureOverlayGroups(feature).stream()
                        .noneMatch(overlayGroupHolder ->
                                overlayGroupHolder.getOverlays().contains(overlay)))
                .toList()) {
            registerConfigOptions(overlay);
        }
    }

    private <P extends Configurable & Translatable> void registerConfigOptions(P configurable) {
        List<Config<?>> configs = getConfigOptions(configurable);

        configurable.addConfigOptions(configs);
        CONFIGS.addAll(configs);
    }

    public void reloadConfiguration() {
        configObject = Managers.Json.loadPreciousJson(userConfig);
        loadConfigOptions(true, true);
    }

    // Info: The purpose of initOverlayGroups is to use the config system in a way that is really "hacky".
    //       Overlay group initialization needs:
    //          1, Overlay instances to be loaded (at init, the default number of instances, then the number defined in
    //             configObject)
    //          2, We need to handle dynamic overlays' configs as regular configs, so that they can be loaded from the
    //             config file
    //       The problem is that the config system "save" is used to remove unused configs, and that "load" is used to
    //       init dynamic overlay instances.
    //
    //       This really becomes a problem when modifying overlay group sizes at runtime.
    //       We want to do 4 things: Save new overlay group size, init overlay instances, load configs, and remove
    //       unused configs.
    //       This means we need to save - load - save, which we should not do. initOverlayGroups is the solution to
    //       this, for now.
    public void loadConfigOptions(boolean resetIfNotFound, boolean initOverlayGroups) {
        // We have to set up the overlay groups first, so that the overlays' configs can be loaded
        JsonObject overlayGroups = JsonUtils.getNullableJsonObject(configObject, OVERLAY_GROUPS_JSON_KEY);

        for (OverlayGroupHolder holder : Managers.Overlay.getOverlayGroups()) {
            if (initOverlayGroups) {
                if (overlayGroups.has(holder.getConfigKey())) {
                    JsonArray ids = JsonUtils.getNullableJsonArray(overlayGroups, holder.getConfigKey());

                    List<Integer> idList =
                            ids.asList().stream().map(JsonElement::getAsInt).toList();

                    Managers.Overlay.createOverlayGroupWithIds(holder, idList);
                } else {
                    Managers.Overlay.createOverlayGroupWithDefaults(holder);
                }
            }

            holder.getOverlays().forEach(overlay -> overlay.addConfigOptions(this.getConfigOptions(overlay)));
        }

        for (ConfigHolder<?> holder : getConfigHolderList()) {
            // option hasn't been saved to config
            if (!configObject.has(holder.getJsonName())) {
                if (resetIfNotFound) {
                    holder.reset();
                }
                continue;
            }

            // read value and update option
            JsonElement holderJson = configObject.get(holder.getJsonName());
            Object value = Managers.Json.GSON.fromJson(holderJson, holder.getType());
            holder.restoreValue(value);
        }

        // Newly created group overlays need to be enabled
        for (OverlayGroupHolder holder : Managers.Overlay.getOverlayGroups()) {
            Managers.Overlay.enableOverlays(holder.getParent());
        }
    }

    private static List<ConfigHolder<?>> getConfigHolderList() {
        // This breaks the concept of "manager holds all config holders at all times". Instead we get the group
        // overlays' configs from the overlay instance itself, to save us some trouble.

        return Stream.concat(
                        CONFIGS.stream().map(Config::getConfigHolder),
                        Managers.Overlay.getOverlayGroups().stream()
                                .map(OverlayGroupHolder::getOverlays)
                                .flatMap(List::stream)
                                .map(Overlay::getConfigOptions)
                                .flatMap(List::stream)
                                .map(Config::getConfigHolder))
                .toList();
    }

    public void saveConfig() {
        // create json object, with entry for each option of each container
        JsonObject holderJson = new JsonObject();
        for (ConfigHolder<?> holder : getConfigHolderList()) {
            if (!holder.valueChanged()) continue; // only save options that have been set by the user
            Object value = holder.getValue();

            JsonElement holderElement = Managers.Json.GSON.toJsonTree(value);
            holderJson.add(holder.getJsonName(), holderElement);
        }

        // Also save upfixer data
        holderJson.add(
                Managers.ConfigUpfixer.UPFIXER_JSON_MEMBER_NAME,
                configObject.get(Managers.ConfigUpfixer.UPFIXER_JSON_MEMBER_NAME));

        // Save overlay groups
        JsonObject overlayGroups = new JsonObject();
        for (OverlayGroupHolder holder : Managers.Overlay.getOverlayGroups()) {
            JsonArray ids = new JsonArray();

            holder.getOverlays().stream()
                    .map(overlay -> ((DynamicOverlay) overlay).getId())
                    .forEach(ids::add);

            overlayGroups.add(holder.getConfigKey(), ids);
        }

        holderJson.add(OVERLAY_GROUPS_JSON_KEY, overlayGroups);

        Managers.Json.savePreciousJson(userConfig, holderJson);
    }

    private void saveDefaultConfig() {
        // create json object, with entry for each option of each container
        JsonObject holderJson = new JsonObject();
        for (ConfigHolder<?> holder : getConfigHolderList()) {
            Object value = holder.getDefaultValue();

            JsonElement holderElement = Managers.Json.GSON.toJsonTree(value);
            holderJson.add(holder.getJsonName(), holderElement);
        }

        WynntilsMod.info("Creating default config file with " + holderJson.size() + " config values.");
        Managers.Json.savePreciousJson(DEFAULT_CONFIG, holderJson);
    }

    private <P extends Configurable & Translatable> List<Config<?>> getConfigOptions(P parent) {
        List<Config<?>> options = new ArrayList<>();

        Field[] annotatedConfigs = FieldUtils.getFieldsWithAnnotation(parent.getClass(), RegisterConfig.class);
        for (Field field : annotatedConfigs) {
            try {
                Object fieldValue = FieldUtils.readField(field, parent, true);
                if (!(fieldValue instanceof Config)) {
                    throw new RuntimeException(
                            "A non-Config class was marked with @RegisterConfig annotation: " + field);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to read @RegisterConfig annotated field: " + field, e);
            }
        }

        List<Field> fields = FieldUtils.getAllFieldsList(parent.getClass());
        List<Field> configFields = fields.stream()
                .filter(f -> f.getType().equals(Config.class) || f.getType().equals(HiddenConfig.class))
                .toList();

        for (Field configField : configFields) {
            RegisterConfig configInfo = Arrays.stream(annotatedConfigs)
                    .filter(f -> f.equals(configField))
                    .findFirst()
                    .map(f -> f.getAnnotation(RegisterConfig.class))
                    .orElse(null);
            if (configInfo == null) {
                throw new RuntimeException("A Config is missing @RegisterConfig annotation:" + configField);
            }
            Config<?> configObj;
            try {
                configObj = (Config<?>) FieldUtils.readField(configField, parent, true);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Cannot read Config field: " + configField, e);
            }

            configObj.createConfigHolder(parent, configField, configInfo);

            if (WynntilsMod.isDevelopmentEnvironment()) {
                ConfigHolder<?> configHolder = configObj.getConfigHolder();

                if (configHolder.isVisible()) {
                    if (configHolder.getDisplayName().startsWith("feature.wynntils.")) {
                        WynntilsMod.error("Config displayName i18n is missing for " + configHolder.getDisplayName());
                        throw new AssertionError("Missing i18n for " + configHolder.getDisplayName());
                    }
                    if (configHolder.getDescription().startsWith("feature.wynntils.")) {
                        WynntilsMod.error("Config description i18n is missing for " + configHolder.getDescription());
                        throw new AssertionError("Missing i18n for " + configHolder.getDescription());
                    }
                    if (configHolder.getDescription().isEmpty()) {
                        WynntilsMod.error("Config description is empty for " + configHolder.getDisplayName());
                        throw new AssertionError("Missing i18n for " + configHolder.getDisplayName());
                    }
                }
            }
            options.add(configObj);
        }
        return options;
    }

    public Stream<ConfigHolder<?>> getConfigHolders() {
        return getConfigHolderList().stream();
    }
}
