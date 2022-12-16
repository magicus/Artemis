/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.gui.screens;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.managers.Managers;
import com.wynntils.gui.render.FontRenderer;
import com.wynntils.gui.render.HorizontalAlignment;
import com.wynntils.gui.render.RenderUtils;
import com.wynntils.gui.render.Texture;
import com.wynntils.gui.render.VerticalAlignment;
import com.wynntils.gui.widgets.BackButton;
import com.wynntils.gui.widgets.DialogueHistoryButton;
import com.wynntils.gui.widgets.PageSelectorButton;
import com.wynntils.gui.widgets.QuestButton;
import com.wynntils.gui.widgets.QuestInfoButton;
import com.wynntils.gui.widgets.ReloadButton;
import com.wynntils.gui.widgets.SortOrderWidget;
import com.wynntils.mc.event.MenuEvent;
import com.wynntils.mc.objects.CommonColors;
import com.wynntils.mc.utils.McUtils;
import com.wynntils.utils.StringUtils;
import com.wynntils.wynn.event.QuestBookReloadedEvent;
import com.wynntils.wynn.model.quests.QuestInfo;
import com.wynntils.wynn.model.quests.QuestSortOrder;
import com.wynntils.wynn.model.quests.QuestStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class WynntilsQuestBookScreen extends WynntilsMenuListScreen<QuestInfo, QuestButton> {
    private static final List<Component> RELOAD_TOOLTIP = List.of(
            new TranslatableComponent("screens.wynntils.wynntilsQuestBook.reload.name").withStyle(ChatFormatting.WHITE),
            new TranslatableComponent("screens.wynntils.wynntilsQuestBook.reload.description")
                    .withStyle(ChatFormatting.GRAY));

    private QuestInfo trackingRequested = null;
    private boolean miniQuestMode = false;
    private QuestSortOrder questSortOrder = QuestSortOrder.LEVEL;

    public WynntilsQuestBookScreen() {
        super(new TranslatableComponent("screens.wynntils.wynntilsQuestBook.name"));

        // Only register this once
        WynntilsMod.registerEventListener(this);
    }

    @Override
    public void onClose() {
        McUtils.mc().keyboardHandler.setSendRepeatsToGui(false);
        WynntilsMod.unregisterEventListener(this);
        super.onClose();
    }

    /** This is called on every resize. Re-registering widgets are required, re-creating them is not.
     * */
    @Override
    protected void init() {
        McUtils.mc().keyboardHandler.setSendRepeatsToGui(true);

        Managers.Quest.rescanQuestBook(true, true);

        super.init();

        this.addRenderableWidget(new BackButton(
                (int) ((Texture.QUEST_BOOK_BACKGROUND.width() / 2f - 16) / 2f),
                65,
                Texture.BACK_ARROW.width() / 2,
                Texture.BACK_ARROW.height(),
                new WynntilsMenuScreen()));

        this.addRenderableWidget(new ReloadButton(
                Texture.QUEST_BOOK_BACKGROUND.width() - 21,
                11,
                (int) (Texture.RELOAD_BUTTON.width() / 2 / 1.7f),
                (int) (Texture.RELOAD_BUTTON.height() / 1.7f),
                () -> Managers.Quest.rescanQuestBook(!miniQuestMode, miniQuestMode)));
        this.addRenderableWidget(new PageSelectorButton(
                Texture.QUEST_BOOK_BACKGROUND.width() / 2 + 50 - Texture.FORWARD_ARROW.width() / 2,
                Texture.QUEST_BOOK_BACKGROUND.height() - 25,
                Texture.FORWARD_ARROW.width() / 2,
                Texture.FORWARD_ARROW.height(),
                false,
                this));
        this.addRenderableWidget(new PageSelectorButton(
                Texture.QUEST_BOOK_BACKGROUND.width() - 50,
                Texture.QUEST_BOOK_BACKGROUND.height() - 25,
                Texture.FORWARD_ARROW.width() / 2,
                Texture.FORWARD_ARROW.height(),
                true,
                this));
        this.addRenderableWidget(new DialogueHistoryButton(
                (int) (Texture.QUEST_BOOK_BACKGROUND.width() / 2f - 30),
                15,
                Texture.DIALOGUE_BUTTON.width(),
                Texture.DIALOGUE_BUTTON.height()));
        this.addRenderableWidget(new QuestInfoButton(
                (int) (Texture.QUEST_BOOK_BACKGROUND.width() / 4f),
                12,
                Texture.QUESTS_BUTTON.width(),
                Texture.QUESTS_BUTTON.height(),
                this));

        this.addRenderableWidget(new SortOrderWidget(
                Texture.QUEST_BOOK_BACKGROUND.width() / 2 + 1,
                11,
                (int) (Texture.SORT_DISTANCE.width() / 1.7f),
                (int) (Texture.SORT_DISTANCE.height() / 2 / 1.7f),
                this));

        reloadElements();
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackgroundTexture(poseStack);

        // Make 0, 0 the top left corner of the rendered quest book background
        poseStack.pushPose();
        final float translationX = getTranslationX();
        final float translationY = getTranslationY();
        poseStack.translate(translationX, translationY, 1f);

        renderTitle(poseStack, I18n.get("screens.wynntils.wynntilsQuestBook.quests"));

        renderVersion(poseStack);

        renderButtons(poseStack, mouseX, mouseY, partialTick);

        if (elements.isEmpty()) {
            renderNoQuestsHelper(poseStack);
        }

        renderDescription(poseStack);

        renderPageInfo(poseStack, currentPage + 1, maxPage + 1);

        poseStack.popPose();

        renderTooltip(poseStack, mouseX, mouseY);
    }

    @SubscribeEvent
    public void onQuestsReloaded(QuestBookReloadedEvent.QuestsReloaded event) {
        if (miniQuestMode) return;

        this.setQuests(getSortedQuests());
        setTrackingRequested(null);
        reloadElements();
    }

    @SubscribeEvent
    public void onMiniQuestsReloaded(QuestBookReloadedEvent.MiniQuestsReloaded event) {
        if (!miniQuestMode) return;

        this.setQuests(getSortedQuests());
        setTrackingRequested(null);
        reloadElements();
    }

    // FIXME: We only need this hack to stop the screen from closing when tracking Quest.
    //        Adding a proper way to add quests with scripted container queries would mean this can get removed.
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onMenuClose(MenuEvent.MenuClosedEvent event) {
        if (McUtils.mc().screen != this) return;

        event.setCanceled(true);
    }

    private static void renderNoQuestsHelper(PoseStack poseStack) {
        FontRenderer.getInstance()
                .renderAlignedTextInBox(
                        poseStack,
                        I18n.get("screens.wynntils.wynntilsQuestBook.tryReload"),
                        Texture.QUEST_BOOK_BACKGROUND.width() / 2f + 15f,
                        Texture.QUEST_BOOK_BACKGROUND.width() - 15f,
                        0,
                        Texture.QUEST_BOOK_BACKGROUND.height(),
                        Texture.QUEST_BOOK_BACKGROUND.width() / 2f - 30f,
                        CommonColors.BLACK,
                        HorizontalAlignment.Center,
                        VerticalAlignment.Middle,
                        FontRenderer.TextShadow.NONE);
    }

    protected void renderTooltip(PoseStack poseStack, int mouseX, int mouseY) {
        List<Component> tooltipLines = List.of();

        if (this.hovered instanceof ReloadButton) {
            tooltipLines = RELOAD_TOOLTIP;
        }

        if (this.hovered instanceof QuestButton questButton) {
            QuestInfo questInfo = questButton.getQuestInfo();

            tooltipLines = QuestInfo.generateTooltipForQuest(questInfo);

            tooltipLines.add(new TextComponent(""));

            if (questInfo.isTrackable()) {
                if (questInfo.equals(Managers.Quest.getTrackedQuest())) {
                    tooltipLines.add(new TextComponent("Left click to stop tracking it!")
                            .withStyle(ChatFormatting.RED)
                            .withStyle(ChatFormatting.BOLD));
                } else {
                    tooltipLines.add(new TextComponent("Left click to track it!")
                            .withStyle(ChatFormatting.GREEN)
                            .withStyle(ChatFormatting.BOLD));
                }
            }

            tooltipLines.add(new TextComponent("Middle click to view on map!")
                    .withStyle(ChatFormatting.YELLOW)
                    .withStyle(ChatFormatting.BOLD));
            tooltipLines.add(new TextComponent("Right to open on the wiki!")
                    .withStyle(ChatFormatting.GOLD)
                    .withStyle(ChatFormatting.BOLD));
        }

        if (this.hovered instanceof DialogueHistoryButton) {
            tooltipLines = List.of(
                    new TextComponent("[>] ")
                            .withStyle(ChatFormatting.GOLD)
                            .append(new TranslatableComponent("screens.wynntils.wynntilsQuestBook.dialogueHistory.name")
                                    .withStyle(ChatFormatting.BOLD)
                                    .withStyle(ChatFormatting.GOLD)),
                    new TranslatableComponent("screens.wynntils.wynntilsQuestBook.dialogueHistory.description")
                            .withStyle(ChatFormatting.GRAY),
                    new TextComponent(""),
                    new TranslatableComponent("screens.wynntils.wynntilsMenu.leftClickToSelect")
                            .withStyle(ChatFormatting.GREEN));
        }

        if (this.hovered instanceof QuestInfoButton) {
            tooltipLines = new ArrayList<>();

            if (miniQuestMode) {
                tooltipLines.add(new TranslatableComponent("screens.wynntils.wynntilsQuestBook.miniQuestInfo.name"));
            } else {
                tooltipLines.add(new TranslatableComponent("screens.wynntils.wynntilsQuestBook.questInfo.name"));
            }

            for (int i = 1; i <= 100; i += 25) {
                int minLevel = i;
                int maxLevel = i + 24;

                long count = elements.stream()
                        .filter(questInfo ->
                                questInfo.getSortLevel() >= minLevel && questInfo.getSortLevel() <= maxLevel)
                        .count();
                long completedCount = elements.stream()
                        .filter(questInfo -> questInfo.getStatus() == QuestStatus.COMPLETED
                                && questInfo.getSortLevel() >= minLevel
                                && questInfo.getSortLevel() <= maxLevel)
                        .count();

                tooltipLines.add(new TextComponent("- Lv. " + minLevel + "-" + maxLevel)
                        .append(new TextComponent(" [" + completedCount + "/" + count + "]")
                                .withStyle(ChatFormatting.GRAY))
                        .append(" ")
                        .append(getPercentageComponent((int) completedCount, (int) count, 5)));
            }

            long count = elements.stream()
                    .filter(questInfo -> questInfo.getSortLevel() >= 101)
                    .count();
            long completedCount;

            if (count > 0) {
                completedCount = elements.stream()
                        .filter(questInfo ->
                                questInfo.getStatus() == QuestStatus.COMPLETED && questInfo.getSortLevel() >= 101)
                        .count();
                tooltipLines.add(new TextComponent("- Lv. 101+")
                        .append(new TextComponent(" [" + completedCount + "/" + count + "]")
                                .withStyle(ChatFormatting.GRAY))
                        .append(" ")
                        .append(getPercentageComponent((int) completedCount, (int) count, 5)));
            }

            count = elements.size();
            completedCount = elements.stream()
                    .filter(questInfo -> questInfo.getStatus() == QuestStatus.COMPLETED)
                    .count();

            tooltipLines.add(new TextComponent(""));
            tooltipLines.add(new TextComponent(this.miniQuestMode ? "Total Mini-Quests: " : "Total Quests: ")
                    .withStyle(ChatFormatting.AQUA)
                    .append(new TextComponent("[" + completedCount + "/" + count + "]")
                            .withStyle(ChatFormatting.DARK_AQUA)));
            tooltipLines.add(getPercentageComponent((int) completedCount, (int) count, 15));
            tooltipLines.add(new TextComponent(""));

            if (!this.miniQuestMode) {
                tooltipLines.add(new TranslatableComponent("screens.wynntils.wynntilsQuestBook.questInfo.click")
                        .withStyle(ChatFormatting.GREEN));
            } else {
                tooltipLines.add(new TranslatableComponent("screens.wynntils.wynntilsQuestBook.miniQuestInfo.click")
                        .withStyle(ChatFormatting.GREEN));
            }
        }

        if (this.hovered instanceof SortOrderWidget) {
            switch (questSortOrder) {
                case LEVEL -> tooltipLines = List.of(
                        new TranslatableComponent("screens.wynntils.wynntilsQuestBook.sort.level.name"),
                        new TranslatableComponent("screens.wynntils.wynntilsQuestBook.sort.level.description"));
                case DISTANCE -> tooltipLines = List.of(
                        new TranslatableComponent("screens.wynntils.wynntilsQuestBook.sort.distance.name"),
                        new TranslatableComponent("screens.wynntils.wynntilsQuestBook.sort.distance.description"));
                case ALPHABETIC -> tooltipLines = List.of(
                        new TranslatableComponent("screens.wynntils.wynntilsQuestBook.sort.alphabetical.name"),
                        new TranslatableComponent("screens.wynntils.wynntilsQuestBook.sort.alphabetical.description"));
            }
        }

        if (tooltipLines.isEmpty()) return;

        RenderUtils.drawTooltipAt(
                poseStack,
                mouseX,
                mouseY,
                100,
                tooltipLines,
                FontRenderer.getInstance().getFont(),
                true);
    }

    private void renderDescription(PoseStack poseStack) {
        FontRenderer.getInstance()
                .renderAlignedTextInBox(
                        poseStack,
                        I18n.get("screens.wynntils.wynntilsQuestBook.description1"),
                        20,
                        Texture.QUEST_BOOK_BACKGROUND.width() / 2f - 10,
                        80,
                        Texture.QUEST_BOOK_BACKGROUND.width() / 2f - 30,
                        CommonColors.BLACK,
                        HorizontalAlignment.Left,
                        FontRenderer.TextShadow.NONE);

        FontRenderer.getInstance()
                .renderAlignedTextInBox(
                        poseStack,
                        I18n.get("screens.wynntils.wynntilsQuestBook.description2"),
                        20,
                        Texture.QUEST_BOOK_BACKGROUND.width() / 2f - 10,
                        170,
                        Texture.QUEST_BOOK_BACKGROUND.width() / 2f - 30,
                        CommonColors.BLACK,
                        HorizontalAlignment.Left,
                        FontRenderer.TextShadow.NONE);
    }

    @Override
    protected void reloadElementsList(String searchText) {
        List<QuestInfo> newQuests = getSortedQuests();
        elements = newQuests.stream()
                .filter(questInfo -> StringUtils.partialMatch(questInfo.getQuest().getName(), searchText))
                .collect(Collectors.toList());

        this.maxPage = Math.max(
                0,
                (elements.size() / getElementsPerPage() + (elements.size() % getElementsPerPage() != 0 ? 1 : 0)) - 1);
    }

    private Component getPercentageComponent(int count, int totalCount, int tickCount) {
        int percentage = Math.round((float) count / totalCount * 100);
        ChatFormatting foregroundColor;
        ChatFormatting braceColor;

        if (percentage < 25) {
            braceColor = ChatFormatting.DARK_RED;
            foregroundColor = ChatFormatting.RED;
        } else if (percentage < 75) {
            braceColor = ChatFormatting.GOLD;
            foregroundColor = ChatFormatting.YELLOW;
        } else {
            braceColor = ChatFormatting.DARK_GREEN;
            foregroundColor = ChatFormatting.GREEN;
        }

        StringBuilder insideText = new StringBuilder(foregroundColor.toString());
        insideText.append("|".repeat(tickCount)).append(percentage).append("%").append("|".repeat(tickCount));
        int insertAt =
                Math.min(insideText.length(), Math.round((insideText.length() - 2) * (float) count / totalCount) + 2);
        insideText.insert(insertAt, ChatFormatting.DARK_GRAY);

        return new TextComponent("[")
                .withStyle(braceColor)
                .append(new TextComponent(insideText.toString()))
                .append(new TextComponent("]").withStyle(braceColor));
    }

    private List<QuestInfo> getSortedQuests() {
        return miniQuestMode ? Managers.Quest.getMiniQuests(questSortOrder) : Managers.Quest.getQuests(questSortOrder);
    }

    private void setQuests(List<QuestInfo> quests) {
        this.elements = new ArrayList<>(quests);
        this.maxPage = Math.max(
                0,
                (elements.size() / getElementsPerPage() + (elements.size() % getElementsPerPage() != 0 ? 1 : 0)) - 1);
    }

    public void setTrackingRequested(QuestInfo questInfo) {
        this.trackingRequested = questInfo;
    }

    public QuestInfo getTrackingRequested() {
        return trackingRequested;
    }

    @Override
    protected QuestButton getButtonFromElement(int i) {
        int offset = i % getElementsPerPage();
        return new QuestButton(
                Texture.QUEST_BOOK_BACKGROUND.width() / 2 + 15,
                offset * 13 + 25,
                Texture.QUEST_BOOK_BACKGROUND.width() / 2 - 37,
                9,
                elements.get(i),
                this);
    }

    public boolean isMiniQuestMode() {
        return miniQuestMode;
    }

    public void setMiniQuestMode(boolean miniQuestMode) {
        this.miniQuestMode = miniQuestMode;

        this.setQuests(getSortedQuests());
        this.setCurrentPage(0);

        Managers.Quest.rescanQuestBook(!this.miniQuestMode, this.miniQuestMode);
    }

    public QuestSortOrder getQuestSortOrder() {
        return questSortOrder;
    }

    public void setQuestSortOrder(QuestSortOrder newSortOrder) {
        if (newSortOrder == null) {
            throw new IllegalStateException("Tried to set quest order to null");
        }

        this.questSortOrder = newSortOrder;
        setQuests(getSortedQuests());
        this.setCurrentPage(0);
    }
}
