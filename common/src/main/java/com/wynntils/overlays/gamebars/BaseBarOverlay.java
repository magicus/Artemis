/*
 * Copyright © Wynntils 2022-2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.overlays.gamebars;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.overlays.Overlay;
import com.wynntils.core.consumers.overlays.OverlayPosition;
import com.wynntils.core.consumers.overlays.OverlaySize;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.RegisterConfig;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.bossbar.TrackedBar;
import com.wynntils.handlers.bossbar.event.BossBarAddedEvent;
import com.wynntils.handlers.bossbar.type.BossBarProgress;
import com.wynntils.mc.event.TickEvent;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.buffered.BufferedFontRenderer;
import com.wynntils.utils.render.buffered.BufferedRenderUtils;
import com.wynntils.utils.render.type.TextShadow;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public abstract class BaseBarOverlay extends Overlay {
    @RegisterConfig(i18nKey = "feature.wynntils.gameBarsOverlay.overlay.baseBar.textShadow")
    public final Config<TextShadow> textShadow = new Config<>(TextShadow.OUTLINE);

    @RegisterConfig(i18nKey = "feature.wynntils.gameBarsOverlay.overlay.baseBar.flip")
    public final Config<Boolean> flip = new Config<>(false);

    @RegisterConfig(i18nKey = "feature.wynntils.gameBarsOverlay.overlay.baseBar.animationTime")
    public final Config<Float> animationTime = new Config<>(2f);

    @RegisterConfig(i18nKey = "feature.wynntils.gameBarsOverlay.overlay.baseBar.shouldDisplayOriginal")
    public final Config<Boolean> shouldDisplayOriginal = new Config<>(false);

    // hacky override of custom color
    @RegisterConfig(i18nKey = "feature.wynntils.gameBarsOverlay.overlay.baseBar.textColor")
    public final Config<CustomColor> textColor = new Config<>(CommonColors.WHITE);

    protected float currentProgress = 0f;

    protected BaseBarOverlay(OverlayPosition position, OverlaySize size, CustomColor textColor) {
        super(position, size);
        this.textColor.updateConfig(textColor);
    }

    protected float textureHeight() {
        return Texture.UNIVERSAL_BAR.height() / 2f;
    }

    protected abstract BossBarProgress progress();

    protected abstract Class<? extends TrackedBar> getTrackedBarClass();

    protected abstract String icon();

    protected abstract boolean isActive();

    @SubscribeEvent
    public void onBossBarAdd(BossBarAddedEvent event) {
        if (!event.getTrackedBar().getClass().equals(getTrackedBarClass())) return;

        if (!shouldDisplayOriginal.get()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (!Models.WorldState.onWorld() || !isActive()) return;

        if (animationTime.get() == 0) {
            currentProgress = progress().progress();
            return;
        }

        currentProgress -=
                (animationTime.get() * 0.1f) * (currentProgress - progress().progress());
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, float partialTicks, Window window) {
        if (!Models.WorldState.onWorld() || !isActive()) return;

        float barHeight = textureHeight() * (this.getWidth() / 81);
        float renderY = getModifiedRenderY(barHeight + 10);

        BossBarProgress barProgress = progress();

        String text = String.format(
                "%s %s %s",
                barProgress.value().current(), icon(), barProgress.value().max());
        renderText(poseStack, bufferSource, renderY, text);

        float renderedProgress = Math.round((flip.get() ? -1 : 1) * currentProgress * 100) / 100f;
        renderBar(poseStack, bufferSource, renderY + 10, barHeight, renderedProgress);
    }

    protected float getModifiedRenderY(float renderedHeight) {
        return switch (this.getRenderVerticalAlignment()) {
            case TOP -> this.getRenderY();
            case MIDDLE -> this.getRenderY() + (this.getHeight() - renderedHeight) / 2;
            case BOTTOM -> this.getRenderY() + this.getHeight() - renderedHeight;
        };
    }

    @Override
    protected void onConfigUpdate(Config<?> config) {}

    protected void renderBar(
            PoseStack poseStack, MultiBufferSource bufferSource, float renderY, float renderHeight, float progress) {
        Texture universalBarTexture = Texture.UNIVERSAL_BAR;

        BufferedRenderUtils.drawColoredProgressBar(
                poseStack,
                bufferSource,
                universalBarTexture,
                this.textColor.get(),
                this.getRenderX(),
                renderY,
                this.getRenderX() + this.getWidth(),
                renderY + renderHeight,
                0,
                0,
                universalBarTexture.width(),
                universalBarTexture.height(),
                progress);
    }

    protected void renderText(PoseStack poseStack, MultiBufferSource bufferSource, float renderY, String text) {
        BufferedFontRenderer.getInstance()
                .renderAlignedTextInBox(
                        poseStack,
                        bufferSource,
                        StyledText.fromString(text),
                        this.getRenderX(),
                        this.getRenderX() + this.getWidth(),
                        renderY,
                        0,
                        this.textColor.get(),
                        this.getRenderHorizontalAlignment(),
                        this.textShadow.get());
    }
}
