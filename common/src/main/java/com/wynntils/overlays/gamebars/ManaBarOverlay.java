/*
 * Copyright © Wynntils 2022-2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.overlays.gamebars;

import com.wynntils.core.components.Models;
import com.wynntils.core.config.Config;
import com.wynntils.core.config.RegisterConfig;
import com.wynntils.core.consumers.overlays.OverlayPosition;
import com.wynntils.core.consumers.overlays.OverlaySize;
import com.wynntils.handlers.bossbar.TrackedBar;
import com.wynntils.handlers.bossbar.type.BossBarProgress;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.ManaTexture;
import com.wynntils.utils.render.type.VerticalAlignment;
import com.wynntils.utils.type.CappedValue;

public class ManaBarOverlay extends OverflowableBarOverlay {
    @RegisterConfig(i18nKey = "overlay.wynntils.manaBar.manaTexture")
    public final Config<ManaTexture> manaTexture = new Config<>(ManaTexture.A);

    public ManaBarOverlay() {
        this(
                new OverlayPosition(
                        -29,
                        52,
                        VerticalAlignment.BOTTOM,
                        HorizontalAlignment.CENTER,
                        OverlayPosition.AnchorSection.BOTTOM_MIDDLE),
                new OverlaySize(81, 21));
    }

    protected ManaBarOverlay(OverlayPosition overlayPosition, OverlaySize overlaySize) {
        super(overlayPosition, overlaySize, CommonColors.LIGHT_BLUE);
    }

    @Override
    public float textureHeight() {
        return manaTexture.get().getHeight();
    }

    @Override
    public BossBarProgress progress() {
        CappedValue mana = Models.CharacterStats.getMana();
        return new BossBarProgress(mana, (float) mana.getProgress());
    }

    @Override
    protected Class<? extends TrackedBar> getTrackedBarClass() {
        return null;
    }

    @Override
    public String icon() {
        return "✺";
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    protected void onConfigUpdate(Config<?> configHolder) {
        Models.CharacterStats.hideMana(!this.shouldDisplayOriginal.get());
    }

    @Override
    protected Texture getTexture() {
        return Texture.MANA_BAR;
    }

    @Override
    protected Texture getOverflowTexture() {
        return Texture.MANA_BAR_OVERFLOW;
    }

    @Override
    protected int getTextureY1() {
        return manaTexture.get().getTextureY1();
    }

    @Override
    protected int getTextureY2() {
        return manaTexture.get().getTextureY2();
    }
}
