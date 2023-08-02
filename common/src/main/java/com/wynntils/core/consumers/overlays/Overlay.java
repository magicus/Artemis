/*
 * Copyright © Wynntils 2022-2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.consumers.overlays;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ComparisonChain;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.config.AbstractConfigurable;
import com.wynntils.core.config.Config;
import com.wynntils.core.config.ConfigHolder;
import com.wynntils.core.config.HiddenConfig;
import com.wynntils.core.config.RegisterConfig;
import com.wynntils.core.consumers.Translatable;
import com.wynntils.core.mod.type.CrashType;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.VerticalAlignment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.phys.Vec2;

public abstract class Overlay extends AbstractConfigurable implements Translatable, Comparable<Overlay> {
    @RegisterConfig(i18nKey = "overlay.wynntils.overlay.position")
    protected final HiddenConfig<OverlayPosition> position = new HiddenConfig<>(null);

    @RegisterConfig(i18nKey = "overlay.wynntils.overlay.size")
    protected final HiddenConfig<OverlaySize> size = new HiddenConfig<>(null);

    @RegisterConfig(i18nKey = "overlay.wynntils.overlay.userEnabled")
    protected final Config<Boolean> userEnabled = new Config<>(true);

    // This is used in rendering.
    // Initially we use the overlay position horizontal alignment
    // but the user can modify this config field to use an override.
    // Example use case: Overlay is aligned to the left in the TopRight section,
    //                   but the user wants to use right text alignment
    @RegisterConfig(i18nKey = "overlay.wynntils.overlay.horizontalAlignmentOverride")
    protected final HiddenConfig<HorizontalAlignment> horizontalAlignmentOverride = new HiddenConfig<>(null);

    @RegisterConfig(i18nKey = "overlay.wynntils.overlay.verticalAlignmentOverride")
    protected final HiddenConfig<VerticalAlignment> verticalAlignmentOverride = new HiddenConfig<>(null);

    protected Overlay(OverlayPosition position, float width, float height) {
        this.position.updateConfig(position);
        this.size.updateConfig(new OverlaySize(width, height));
    }

    protected Overlay(OverlayPosition position, OverlaySize size) {
        this.position.updateConfig(position);
        this.size.updateConfig(size);
    }

    protected Overlay(
            OverlayPosition position,
            OverlaySize size,
            HorizontalAlignment horizontalAlignmentOverride,
            VerticalAlignment verticalAlignmentOverride) {
        this.position.updateConfig(position);
        this.size.updateConfig(size);
        this.horizontalAlignmentOverride.updateConfig(horizontalAlignmentOverride);
        this.verticalAlignmentOverride.updateConfig(verticalAlignmentOverride);
    }

    public abstract void render(PoseStack poseStack, MultiBufferSource bufferSource, float partialTicks, Window window);

    public void renderPreview(PoseStack poseStack, MultiBufferSource bufferSource, float partialTicks, Window window) {
        this.render(poseStack, bufferSource, partialTicks, window);
    }

    @Override
    public final void updateConfigOption(ConfigHolder<?> configHolder) {
        // if user toggle was changed, enable/disable overlay accordingly
        if (configHolder.getFieldName().equals("userEnabled")) {
            if (configHolder.getValue() == Boolean.FALSE) {
                Managers.Overlay.disableOverlay(this);
            } else {
                // If new state is TRUE or null, try to enable overlay
                // (worst case overlay.shouldBeEnabled() will return false)
                Managers.Overlay.enableOverlay(this);
            }
        }

        callOnConfigUpdate(configHolder);
    }

    protected abstract void onConfigUpdate(ConfigHolder<?> configHolder);

    protected void callOnConfigUpdate(ConfigHolder<?> configHolder) {
        try {
            onConfigUpdate(configHolder);
        } catch (Throwable t) {
            // We can't stop disabled overlays from getting config updates, so if it crashes again,
            // just ignore it
            if (!Managers.Overlay.isEnabled(this)) return;

            Managers.Overlay.disableOverlay(this);
            WynntilsMod.reportCrash(
                    CrashType.OVERLAY, getTranslatedName(), getClass().getName(), "config update", t);
        }
    }

    /** Gets the name of a feature */
    @Override
    public String getTranslatedName() {
        return getTranslation("name");
    }

    @Override
    public String getTranslation(String keySuffix) {
        return I18n.get("feature.wynntils." + getDeclaringFeatureNameCamelCase() + ".overlay." + getNameCamelCase()
                + "." + keySuffix);
    }

    public String getShortName() {
        return this.getClass().getSimpleName();
    }

    public String getDeclaringClassName() {
        return Managers.Overlay.getOverlayParent(this).getClass().getSimpleName();
    }

    protected String getNameCamelCase() {
        return CaseFormat.UPPER_CAMEL.to(
                CaseFormat.LOWER_CAMEL, this.getClass().getSimpleName().replace("Overlay", ""));
    }

    protected String getDeclaringFeatureNameCamelCase() {
        String name = getDeclaringClassName().replace("Feature", "");
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, name);
    }

    protected Boolean isUserEnabled() {
        return userEnabled.get();
    }

    public boolean shouldBeEnabled() {
        if (!isParentEnabled()) {
            return false;
        }

        if (this.isUserEnabled() != null) {
            return this.isUserEnabled();
        }

        return Managers.Overlay.isEnabledByDefault(this);
    }

    public final boolean isParentEnabled() {
        return Managers.Overlay.getOverlayParent(this).isEnabled();
    }

    public float getWidth() {
        return this.size.get().getWidth();
    }

    public float getHeight() {
        return this.size.get().getHeight();
    }

    public OverlaySize getSize() {
        return size.get();
    }

    public OverlayPosition getPosition() {
        return position.get();
    }

    public void setPosition(OverlayPosition position) {
        this.position.updateConfig(position);
    }

    // Return the X where the overlay should be rendered
    public float getRenderX() {
        return getRenderX(this.position.get());
    }

    // Return the Y where the overlay should be rendered
    public float getRenderY() {
        return getRenderY(this.position.get());
    }

    public float getRenderX(OverlayPosition position) {
        final SectionCoordinates section = Managers.Overlay.getSection(position.getAnchorSection());
        return switch (position.getHorizontalAlignment()) {
            case LEFT -> section.x1() + position.getHorizontalOffset();
            case CENTER -> (section.x1() + section.x2() - this.getWidth()) / 2 + position.getHorizontalOffset();
            case RIGHT -> section.x2() + position.getHorizontalOffset() - this.getWidth();
        };
    }

    public float getRenderY(OverlayPosition position) {
        final SectionCoordinates section = Managers.Overlay.getSection(position.getAnchorSection());
        return switch (position.getVerticalAlignment()) {
            case TOP -> section.y1() + position.getVerticalOffset();
            case MIDDLE -> (section.y1() + section.y2() - this.getHeight()) / 2 + position.getVerticalOffset();
            case BOTTOM -> section.y2() + position.getVerticalOffset() - this.getHeight();
        };
    }

    public HorizontalAlignment getRenderHorizontalAlignment() {
        return horizontalAlignmentOverride.get() == null
                ? position.get().getHorizontalAlignment()
                : horizontalAlignmentOverride.get();
    }

    public VerticalAlignment getRenderVerticalAlignment() {
        return verticalAlignmentOverride.get() == null
                ? position.get().getVerticalAlignment()
                : verticalAlignmentOverride.get();
    }

    public Vec2 getCornerPoints(Corner corner) {
        return switch (corner) {
            case TOP_LEFT -> new Vec2(getRenderX(), getRenderY());
            case TOP_RIGHT -> new Vec2(getRenderX() + getWidth(), getRenderY());
            case BOTTOM_LEFT -> new Vec2(getRenderX(), getRenderY() + getHeight());
            case BOTTOM_RIGHT -> new Vec2(getRenderX() + getWidth(), getRenderY() + getHeight());
        };
    }

    @Override
    public int compareTo(Overlay other) {
        return ComparisonChain.start()
                .compareTrueFirst(this.isParentEnabled(), other.isParentEnabled())
                .compare(this.getDeclaringClassName(), other.getDeclaringClassName())
                .compare(
                        (this instanceof DynamicOverlay dynamicThis ? dynamicThis.getId() : 0),
                        (other instanceof DynamicOverlay dynamicOther ? dynamicOther.getId() : 0))
                .compare(this.getTranslatedName(), other.getTranslatedName())
                .result();
    }

    public void setHeight(float height) {
        getSize().setHeight(height);
    }

    public void setWidth(float width) {
        getSize().setWidth(width);
    }
}
