/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Services;
import com.wynntils.core.text.StyledText;
import com.wynntils.services.map.pois.Poi;
import com.wynntils.services.map.type.DisplayPriority;
import com.wynntils.services.mapdata.attributes.type.MapAttributes;
import com.wynntils.services.mapdata.attributes.type.MapIcon;
import com.wynntils.services.mapdata.attributes.type.MapVisibility;
import com.wynntils.services.mapdata.type.MapFeature;
import com.wynntils.services.mapdata.type.MapLocation;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.type.PoiLocation;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.buffered.BufferedFontRenderer;
import com.wynntils.utils.render.buffered.BufferedRenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import net.minecraft.client.renderer.MultiBufferSource;

public class MapFeaturePoiWrapper implements Poi {
    public static final int SPACING = 2;
    public static final float TEXT_SCALE = 1f;
    private final MapFeature feature;
    private final MapAttributes attributes;

    public MapFeaturePoiWrapper(MapFeature feature) {
        this.feature = feature;
        attributes = Services.MapData.getFullFeatureAttributes(this.feature);
    }

    @Override
    public PoiLocation getLocation() {
        if (feature instanceof MapLocation mapLocation) {
            return PoiLocation.fromLocation(mapLocation.getLocation());
        }

        return null;
    }

    @Override
    public DisplayPriority getDisplayPriority() {
        return DisplayPriority.HIGHEST;
    }

    @Override
    public boolean hasStaticLocation() {
        return false;
    }

    @Override
    public String getName() {
        return "Wrapped MapFeature [" + feature.getFeatureId() + "]";
    }

    @Override
    public int getWidth(float mapZoom, float scale) {
        String iconId = attributes.getIconId();
        String label = attributes.getLabel();

        if (hasIcon(iconId)) {
            MapIcon icon = Services.MapData.getIcon(iconId).get();
            return (int) (icon.getWidth() * scale);
        }

        if (hasLabel(label)) {
            // Use label for measurements
            return (int) (FontRenderer.getInstance().getFont().width(attributes.getLabel()) * scale);
        }

        // No icon or label, return 32 as fallback
        WynntilsMod.warn("No icon or label for feature " + feature.getFeatureId());
        return 32;
    }

    @Override
    public int getHeight(float mapZoom, float scale) {
        String iconId = attributes.getIconId();
        String label = attributes.getLabel();

        if (hasIcon(iconId)) {
            MapIcon icon = Services.MapData.getIcon(iconId).get();
            return (int) (icon.getHeight() * scale);
        }

        if (hasLabel(label)) {
            // Use label for measurements
            return getLabelHeight(scale);
        }

        // No icon or label, return 32 as fallback
        WynntilsMod.warn("No icon or label for feature " + feature.getFeatureId());
        return 32;
    }

    @Override
    public void renderAt(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            float renderX,
            float renderY,
            boolean hovered,
            float scale,
            float zoomRenderScale,
            float zoomLevel) {
        float renderScale = hovered ? scale * 1.05f : scale;
        // this is the default alpha for labels
        float alpha = hovered ? 1f : 0.9f;
        int labelHeight = getLabelHeight(renderScale);

        String iconId = attributes.getIconId();
        String label = attributes.getLabel();
        int level = attributes.getLevel();

        int yOffset = 0;

        poseStack.pushPose();
        // z-index for rendering
        poseStack.translate(renderX, renderY, getDisplayPriority().ordinal());
        poseStack.scale(renderScale, renderScale, renderScale);

        // Draw icon, if applicable
        boolean drawIcon = hasIcon(iconId) && this.getIconAlpha(zoomLevel) > 0.01;
        if (drawIcon) {
            MapIcon icon = Services.MapData.getIcon(iconId).get();

            float iconWidth = icon.getWidth();
            float iconHeight = icon.getHeight();

            CustomColor iconColor = attributes.getIconColor();
            if (iconColor == null) {
                iconColor = CommonColors.WHITE;
            }
            CustomColor color = iconColor.withAlpha(alpha);

            BufferedRenderUtils.drawColoredTexturedRect(
                    poseStack,
                    bufferSource,
                    icon.getResourceLocation(),
                    color,
                    this.getIconAlpha(zoomLevel),
                    0 - iconWidth / 2,
                    yOffset - iconHeight / 2,
                    0,
                    iconWidth,
                    iconHeight);
            yOffset += (iconHeight + labelHeight) / 2 + SPACING;
        }

        // Draw label, if applicable
        boolean drawLabel = hasLabel(label) && this.getLabelAlpha(zoomLevel) > 0.01 || (drawIcon && hovered);
        if (drawLabel) {
            CustomColor labelColor = attributes.getLabelColor();
            if (labelColor == null) {
                labelColor = CommonColors.WHITE;
            }
            TextShadow labelShadow = attributes.getLabelShadow();
            if (labelShadow == null) {
                labelShadow = TextShadow.OUTLINE;
            }
            float labelAlpha = alpha * getLabelAlpha(zoomLevel);

            if (drawIcon && hovered) {
                // If this is hovered, show with full alpha
                labelAlpha = 1f;
            }

            // Small enough alphas are turned into 255, so just don't even try to render them
            if (labelAlpha >= 0.01) {
                CustomColor color = labelColor.withAlpha(labelAlpha);

                BufferedFontRenderer.getInstance()
                        .renderText(
                                poseStack,
                                bufferSource,
                                StyledText.fromString(label),
                                0,
                                yOffset,
                                color,
                                HorizontalAlignment.CENTER,
                                VerticalAlignment.MIDDLE,
                                labelShadow,
                                TEXT_SCALE);
                yOffset += labelHeight + SPACING;
            }
        }

        // Draw level, if applicable
        if (hovered && level != 0 && (drawIcon || drawLabel)) {
            CustomColor labelColor = attributes.getLabelColor();
            if (labelColor == null) {
                labelColor = CommonColors.WHITE;
            }
            TextShadow labelShadow = attributes.getLabelShadow();
            if (labelShadow == null) {
                labelShadow = TextShadow.OUTLINE;
            }
            CustomColor color = labelColor.withAlpha(alpha);

            BufferedFontRenderer.getInstance()
                    .renderText(
                            poseStack,
                            bufferSource,
                            StyledText.fromString("[Lv " + level + "]"),
                            0,
                            yOffset,
                            color,
                            HorizontalAlignment.CENTER,
                            VerticalAlignment.MIDDLE,
                            labelShadow,
                            TEXT_SCALE);
        }

        poseStack.popPose();
    }

    private boolean hasIcon(String iconId) {
        return !(iconId == null || iconId.equals(MapIcon.NO_ICON_ID));
    }

    private boolean hasLabel(String label) {
        return label != null && !label.isEmpty();
    }

    private float calculateVisibility(float min, float max, float fade, float zoomLevel) {
        float startFadeIn = min - fade;
        float stopFadeIn = min + fade;
        float startFadeOut = max - fade;
        float stopFadeOut = max + fade;

        // If min or max is at the extremes, do not apply fading
        if (min <= 1) {
            startFadeIn = 0;
            stopFadeIn = 0;
        }
        if (max >= 100) {
            startFadeOut = 101;
            stopFadeOut = 101;
        }

        if (zoomLevel < startFadeIn) {
            return 0;
        }
        if (zoomLevel < stopFadeIn) {
            // The visibility should be linearly interpolated between 0 and 1 for values
            // between startFadeIn and stopFadeIn.
            return (zoomLevel - startFadeIn) / (fade * 2);
        }

        if (zoomLevel < startFadeOut) {
            return 1;
        }

        if (zoomLevel < stopFadeOut) {
            // The visibility should be linearly interpolated between 1 and 0 for values
            // between startFadeIn and stopFadeIn.
            return 1 - (zoomLevel - startFadeOut) / (fade * 2);
        }

        return 0;
    }

    private float getIconAlpha(float zoomLevel) {
        MapVisibility iconVisibility = attributes.getIconVisibility();
        if (iconVisibility == null) {
            // If no visibility is specified, always show
            return 1f;
        }
        return calculateVisibility(
                iconVisibility.getMin(), iconVisibility.getMax(), iconVisibility.getFade(), zoomLevel);
    }

    private float getLabelAlpha(float zoomLevel) {
        MapVisibility labelVisibility = attributes.getLabelVisibility();
        if (labelVisibility == null) {
            // If no visibility is specified, always show
            return 1f;
        }
        return calculateVisibility(
                labelVisibility.getMin(), labelVisibility.getMax(), labelVisibility.getFade(), zoomLevel);
    }

    private int getLabelHeight(float scale) {
        return (int) (FontRenderer.getInstance().getFont().lineHeight * scale * TEXT_SCALE);
    }
}
