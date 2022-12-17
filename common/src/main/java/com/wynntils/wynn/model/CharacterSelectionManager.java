/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wynn.model;

import com.wynntils.core.managers.Manager;
import com.wynntils.gui.screens.CharacterSelectorScreen;
import com.wynntils.gui.screens.WynntilsScreenWrapper;
import com.wynntils.mc.event.ContainerSetContentEvent;
import com.wynntils.mc.event.MenuEvent;
import com.wynntils.mc.event.ScreenOpenedEvent;
import com.wynntils.mc.utils.ComponentUtils;
import com.wynntils.mc.utils.ItemUtils;
import com.wynntils.wynn.objects.ClassInfo;
import com.wynntils.wynn.objects.ClassType;
import com.wynntils.wynn.utils.ContainerUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

public final class CharacterSelectionManager extends Manager {
    private static final Pattern NEW_CLASS_ITEM_NAME_PATTERN = Pattern.compile("§l§a\\[\\+\\] Create a new character");
    private static final Pattern CLASS_ITEM_NAME_PATTERN = Pattern.compile("§l§6\\[>\\] Select (.+)");
    private static final Pattern CLASS_ITEM_CLASS_PATTERN =
            Pattern.compile("§e- §r§7Class: (§r)?(§c§l☠§r)?(§6§l❂§r)?(§3§l⛏§r)?(§5§l⚔§r)?(\\s)?(§r)?§f(?<name>.+)");
    private static final Pattern CLASS_ITEM_LEVEL_PATTERN = Pattern.compile("§e- §r§7Level: §r§f(\\d+)");
    private static final Pattern CLASS_ITEM_XP_PATTERN = Pattern.compile("§e- §r§7XP: §r§f(\\d+)%");
    private static final Pattern CLASS_ITEM_SOUL_POINTS_PATTERN = Pattern.compile("§e- §r§7Soul Points: §r§f(\\d+)");
    private static final Pattern CLASS_ITEM_FINISHED_QUESTS_PATTERN =
            Pattern.compile("§e- §r§7Finished Quests: §r§f(\\d+)/\\d+");

    private static final String DEFAULT_CLASS_NAME = "This Character";

    private static final int EDIT_BUTTON_SLOT = 8;

    private CharacterSelectorScreen currentScreen;
    private int containerId = -1;
    private int firstNewCharacterSlot = -1;
    private final List<ClassInfo> classInfoList = new ArrayList<>();

    public CharacterSelectionManager() {
        super(List.of());
    }

    @SubscribeEvent
    public void onScreenOpened(ScreenOpenedEvent event) {
        if (event.getScreen() instanceof WynntilsScreenWrapper wynntilsScreenWrapper) {
            if (wynntilsScreenWrapper.getDelegate() instanceof CharacterSelectorScreen characterSelectorScreen) {
                currentScreen = characterSelectorScreen;

                currentScreen.setClassInfoList(classInfoList);
                currentScreen.setFirstNewCharacterSlot(firstNewCharacterSlot);
            }
        }
    }

    @SubscribeEvent
    public void onMenuOpened(MenuEvent.MenuOpenedEvent event) {
        if (!ComponentUtils.getCoded(event.getTitle()).equals("§8§lSelect a Character")) {
            return;
        }

        containerId = event.getContainerId();
    }

    @SubscribeEvent
    public void onContainerItemsSet(ContainerSetContentEvent.Pre event) {
        if (event.getContainerId() != containerId) {
            return;
        }

        classInfoList.clear();

        List<ItemStack> items = event.getItems();
        for (int i = 0; i < items.size(); i++) {
            ItemStack item = items.get(i);
            String itemName = ComponentUtils.getCoded(item.getHoverName());
            Matcher classItemMatcher = CLASS_ITEM_NAME_PATTERN.matcher(itemName);
            if (classItemMatcher.matches()) {
                ClassInfo classInfo = getClassInfoFromItem(item, i, classItemMatcher.group(1));
                classInfoList.add(classInfo);
                continue;
            }

            if (firstNewCharacterSlot == -1
                    && NEW_CLASS_ITEM_NAME_PATTERN.matcher(itemName).matches()) {
                firstNewCharacterSlot = i;
            }
        }

        if (currentScreen != null) {
            currentScreen.setClassInfoList(classInfoList);
            currentScreen.setFirstNewCharacterSlot(firstNewCharacterSlot);
        }
    }

    private ClassInfo getClassInfoFromItem(ItemStack item, int slot, String className) {
        ClassType classType = null;
        int level = 0;
        int xp = 0;
        int soulPoints = 0;
        int finishedQuests = 0;
        for (String line : ItemUtils.getLore(item)) {
            Matcher matcher = CLASS_ITEM_CLASS_PATTERN.matcher(line);

            if (matcher.matches()) {
                String classTypeString = matcher.group("name");
                classType = ClassType.fromName(classTypeString);
                if (DEFAULT_CLASS_NAME.equals(className)) {
                    className = classTypeString;
                }
                continue;
            }

            matcher = CLASS_ITEM_LEVEL_PATTERN.matcher(line);
            if (matcher.matches()) {
                level = Integer.parseInt(matcher.group(1));
                continue;
            }

            matcher = CLASS_ITEM_XP_PATTERN.matcher(line);
            if (matcher.matches()) {
                xp = Integer.parseInt(matcher.group(1));
                continue;
            }

            matcher = CLASS_ITEM_SOUL_POINTS_PATTERN.matcher(line);
            if (matcher.matches()) {
                soulPoints = Integer.parseInt(matcher.group(1));
                continue;
            }

            matcher = CLASS_ITEM_FINISHED_QUESTS_PATTERN.matcher(line);
            if (matcher.matches()) {
                finishedQuests = Integer.parseInt(matcher.group(1));
            }
        }

        return new ClassInfo(className, item, slot, classType, level, xp, soulPoints, finishedQuests);
    }

    public void playWithCharacter(int slot) {
        ContainerUtils.clickOnSlot(
                slot,
                currentScreen.getActualClassSelectionScreen().getMenu().containerId,
                GLFW.GLFW_MOUSE_BUTTON_LEFT,
                currentScreen.getActualClassSelectionScreen().getMenu().getItems());
    }

    public void deleteCharacter(int slot) {
        ContainerUtils.clickOnSlot(
                slot,
                currentScreen.getActualClassSelectionScreen().getMenu().containerId,
                GLFW.GLFW_MOUSE_BUTTON_RIGHT,
                currentScreen.getActualClassSelectionScreen().getMenu().getItems());
    }

    public void editCharacters(AbstractContainerMenu menu) {
        ContainerUtils.clickOnSlot(EDIT_BUTTON_SLOT, menu.containerId, GLFW.GLFW_MOUSE_BUTTON_LEFT, menu.getItems());
    }

    public void createNewClass() {
        ContainerUtils.clickOnSlot(
                currentScreen.getFirstNewCharacterSlot(),
                currentScreen.getActualClassSelectionScreen().getMenu().containerId,
                GLFW.GLFW_MOUSE_BUTTON_LEFT,
                currentScreen.getActualClassSelectionScreen().getMenu().getItems());
    }
}
