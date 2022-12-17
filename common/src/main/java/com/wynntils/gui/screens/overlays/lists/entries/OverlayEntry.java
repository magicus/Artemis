/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.gui.screens.overlays.lists.entries;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.features.overlays.Overlay;
import com.wynntils.core.managers.Managers;
import com.wynntils.gui.render.FontRenderer;
import com.wynntils.gui.render.HorizontalAlignment;
import com.wynntils.gui.render.RenderUtils;
import com.wynntils.gui.render.VerticalAlignment;
import com.wynntils.gui.screens.overlays.OverlayManagementScreen;
import com.wynntils.gui.screens.overlays.lists.OverlayList;
import com.wynntils.mc.objects.CommonColors;
import com.wynntils.mc.objects.CustomColor;
import com.wynntils.mc.utils.McUtils;
import java.util.List;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;

public class OverlayEntry extends ContainerObjectSelectionList.Entry<OverlayEntry> {
    private static final float PADDING = 2.4f;
    private static final CustomColor ENABLED_COLOR = new CustomColor(0, 116, 0, 255);
    private static final CustomColor DISABLED_COLOR = new CustomColor(60, 60, 60, 255);
    private static final CustomColor DISABLED_FEATURE_COLOR = new CustomColor(120, 0, 0, 255);
    private static final CustomColor ENABLED_COLOR_BORDER = new CustomColor(0, 220, 0, 255);
    private static final CustomColor DISABLED_COLOR_BORDER = new CustomColor(0, 0, 0, 255);
    private static final CustomColor DISABLED_FEATURE_COLOR_BORDER = new CustomColor(255, 0, 0, 255);

    private final Overlay overlay;

    public OverlayEntry(Overlay overlay) {
        this.overlay = overlay;
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return ImmutableList.of();
    }

    @Override
    public void render(
            PoseStack poseStack,
            int index,
            int top,
            int left,
            int width,
            int height,
            int mouseX,
            int mouseY,
            boolean isMouseOver,
            float partialTick) {
        poseStack.pushPose();
        poseStack.translate(left + PADDING, top + PADDING, 0);

        boolean enabled = Managers.Overlay.isEnabled(this.overlay);
        int y = index != 0 ? 2 : 0;

        CustomColor borderColor = overlay.isParentEnabled()
                ? (enabled ? ENABLED_COLOR_BORDER : DISABLED_COLOR_BORDER)
                : DISABLED_FEATURE_COLOR_BORDER;
        RenderUtils.drawRect(poseStack, borderColor.withAlpha(100), 0, y, 0, width - PADDING, height - y - PADDING);

        CustomColor rectColor =
                overlay.isParentEnabled() ? (enabled ? ENABLED_COLOR : DISABLED_COLOR) : DISABLED_FEATURE_COLOR;
        RenderUtils.drawRectBorders(poseStack, rectColor, 0, y, width - PADDING, height - PADDING, 1, 2);

        poseStack.translate(0, 0, 1);
        String translatedName = this.overlay.getTranslatedName();
        float renderHeightForOverlayName =
                FontRenderer.getInstance().calculateRenderHeight(List.of(translatedName), width);
        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        translatedName,
                        3,
                        (OverlayList.getItemHeight() - renderHeightForOverlayName / 2f) / 2f - PADDING / 2f,
                        width - PADDING,
                        CommonColors.WHITE,
                        HorizontalAlignment.Left,
                        VerticalAlignment.Top,
                        FontRenderer.TextShadow.NORMAL);

        poseStack.popPose();
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return ImmutableList.of();
    }

    public Overlay getOverlay() {
        return overlay;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!overlay.isParentEnabled()) return false;

        // right click
        if (button == 1) {
            Managers.Config.getConfigHolders().stream()
                    .filter(configHolder -> configHolder.getParent() == overlay
                            && configHolder.getFieldName().equals("userEnabled"))
                    .findFirst()
                    .ifPresent(configHolder -> configHolder.setValue(!overlay.isEnabled()));
            Managers.Config.saveConfig();
            return true;
        }

        if (!overlay.isEnabled()) return false;

        McUtils.mc().setScreen(OverlayManagementScreen.create(this.overlay));
        return true;
    }
}
