/*
 * Copyright © Wynntils 2022-2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.overlays;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.overlays.Overlay;
import com.wynntils.core.consumers.overlays.OverlayPosition;
import com.wynntils.core.consumers.overlays.OverlaySize;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.RegisterConfig;
import com.wynntils.handlers.scoreboard.event.ScoreboardSegmentAdditionEvent;
import com.wynntils.models.territories.GuildAttackScoreboardPart;
import com.wynntils.models.territories.TerritoryAttackTimer;
import com.wynntils.utils.render.TextRenderSetting;
import com.wynntils.utils.render.TextRenderTask;
import com.wynntils.utils.render.buffered.BufferedFontRenderer;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.Comparator;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class TerritoryAttackTimerOverlay extends Overlay {
    @RegisterConfig
    public final Config<Boolean> disableAttackTimersOnScoreboard = new Config<>(true);

    @RegisterConfig
    public final Config<TextShadow> textShadow = new Config<>(TextShadow.OUTLINE);

    private TextRenderSetting textRenderSetting;

    public TerritoryAttackTimerOverlay() {
        super(
                new OverlayPosition(
                        165,
                        -5,
                        VerticalAlignment.TOP,
                        HorizontalAlignment.RIGHT,
                        OverlayPosition.AnchorSection.TOP_RIGHT),
                new OverlaySize(200, 110));

        updateTextRenderSetting();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onScoreboardSegmentChange(ScoreboardSegmentAdditionEvent event) {
        if (disableAttackTimersOnScoreboard.get()
                && event.getSegment().getScoreboardPart() instanceof GuildAttackScoreboardPart) {
            event.setCanceled(true);
        }
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, float partialTicks, Window window) {
        BufferedFontRenderer.getInstance()
                .renderTextsWithAlignment(
                        poseStack,
                        bufferSource,
                        this.getRenderX(),
                        this.getRenderY(),
                        Models.GuildAttackTimer.getAttackTimers().stream()
                                .sorted(Comparator.comparing(TerritoryAttackTimer::asSeconds))
                                .map(territoryAttackTimer ->
                                        new TextRenderTask(territoryAttackTimer.asString(), textRenderSetting))
                                .toList(),
                        this.getWidth(),
                        this.getHeight(),
                        this.getRenderHorizontalAlignment(),
                        this.getRenderVerticalAlignment());
    }

    @Override
    public void renderPreview(PoseStack poseStack, MultiBufferSource bufferSource, float partialTicks, Window window) {
        BufferedFontRenderer.getInstance()
                .renderTextWithAlignment(
                        poseStack,
                        bufferSource,
                        this.getRenderX(),
                        this.getRenderY(),
                        new TextRenderTask(
                                ChatFormatting.GRAY + "Detlas" + ChatFormatting.YELLOW + " (High)" + ChatFormatting.AQUA
                                        + " 02:31",
                                textRenderSetting),
                        this.getWidth(),
                        this.getHeight(),
                        this.getRenderHorizontalAlignment(),
                        this.getRenderVerticalAlignment());
    }

    @Override
    protected void onConfigUpdate(Config<?> config) {
        updateTextRenderSetting();
    }

    private void updateTextRenderSetting() {
        textRenderSetting = TextRenderSetting.DEFAULT
                .withMaxWidth(this.getWidth())
                .withHorizontalAlignment(this.getRenderHorizontalAlignment())
                .withTextShadow(textShadow.get());
    }
}
