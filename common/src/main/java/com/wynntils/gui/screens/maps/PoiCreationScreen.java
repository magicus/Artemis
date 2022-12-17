/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.gui.screens.maps;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.managers.Managers;
import com.wynntils.features.user.map.MapFeature;
import com.wynntils.gui.render.FontRenderer;
import com.wynntils.gui.render.HorizontalAlignment;
import com.wynntils.gui.render.RenderUtils;
import com.wynntils.gui.render.Texture;
import com.wynntils.gui.render.VerticalAlignment;
import com.wynntils.gui.screens.TextboxScreen;
import com.wynntils.gui.screens.WynntilsScreenWrapper;
import com.wynntils.gui.widgets.TextInputBoxWidget;
import com.wynntils.mc.objects.CommonColors;
import com.wynntils.mc.objects.CustomColor;
import com.wynntils.mc.utils.McUtils;
import com.wynntils.wynn.model.map.poi.CustomPoi;
import com.wynntils.wynn.model.map.poi.PoiLocation;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import org.lwjgl.glfw.GLFW;

public class PoiCreationScreen extends Screen implements TextboxScreen {
    private static final Pattern COORDINATE_PATTERN = Pattern.compile("[-+]?\\d+");

    private static final List<Texture> POI_ICONS = List.of(
            Texture.FLAG,
            Texture.DIAMOND,
            Texture.FIREBALL,
            Texture.SIGN,
            Texture.STAR,
            Texture.WALL,
            Texture.CHEST_T1,
            Texture.CHEST_T2,
            Texture.CHEST_T3,
            Texture.CHEST_T4,
            Texture.FARMING,
            Texture.FISHING,
            Texture.MINING,
            Texture.WOODCUTTING);

    private TextInputBoxWidget focusedTextInput;

    private TextInputBoxWidget nameInput;
    private TextInputBoxWidget xInput;
    private TextInputBoxWidget yInput;
    private TextInputBoxWidget zInput;
    private TextInputBoxWidget colorInput;

    private Button saveButton;

    private int selectedIconIndex = 0;
    private CustomPoi.Visibility selectedVisiblity = CustomPoi.Visibility.DEFAULT;
    private CustomColor colorCache = CommonColors.WHITE;

    private final MainMapScreen oldMapScreen;
    private CustomPoi oldPoi;
    private PoiLocation setupLocation;
    private boolean firstSetup = false;

    private PoiCreationScreen(MainMapScreen oldMapScreen) {
        super(new TextComponent("Poi Creation Screen"));
        this.oldMapScreen = oldMapScreen;

        this.firstSetup = true;
    }

    private PoiCreationScreen(MainMapScreen oldMapScreen, PoiLocation setupLocation) {
        this(oldMapScreen);

        this.setupLocation = setupLocation;
        this.firstSetup = true;
    }

    private PoiCreationScreen(MainMapScreen oldMapScreen, CustomPoi poi) {
        this(oldMapScreen);

        this.oldPoi = poi;
        this.firstSetup = true;
    }

    public static Screen create(MainMapScreen oldMapScreen) {
        return WynntilsScreenWrapper.create(new PoiCreationScreen(oldMapScreen));
    }

    public static Screen create(MainMapScreen oldMapScreen, PoiLocation setupLocation) {
        return WynntilsScreenWrapper.create(new PoiCreationScreen(oldMapScreen, setupLocation));
    }

    public static Screen create(MainMapScreen oldMapScreen, CustomPoi poi) {
        return WynntilsScreenWrapper.create(new PoiCreationScreen(oldMapScreen, poi));
    }

    @Override
    protected void init() {
        super.init();

        // region Name
        this.addRenderableWidget(
                nameInput = new TextInputBoxWidget(
                        this.width / 2 - 100,
                        this.height / 2 - 50,
                        150,
                        20,
                        (s) -> updateSaveStatus(),
                        this,
                        nameInput));
        if (oldPoi != null && firstSetup) {
            nameInput.setTextBoxInput(oldPoi.getName());
        }

        if (firstSetup) {
            setFocusedTextInput(nameInput);
        }
        // endregion

        // region Coordinates
        this.addRenderableWidget(
                xInput = new TextInputBoxWidget(
                        this.width / 2 - 85,
                        this.height / 2 - 5,
                        35,
                        20,
                        s -> {
                            xInput.setRenderColor(
                                    COORDINATE_PATTERN.matcher(s).matches() ? CommonColors.GREEN : CommonColors.RED);
                            updateSaveStatus();
                        },
                        this,
                        xInput));
        this.addRenderableWidget(
                yInput = new TextInputBoxWidget(
                        this.width / 2 - 35,
                        this.height / 2 - 5,
                        35,
                        20,
                        s -> {
                            yInput.setRenderColor(
                                    COORDINATE_PATTERN.matcher(s).matches() ? CommonColors.GREEN : CommonColors.RED);
                            updateSaveStatus();
                        },
                        this,
                        yInput));
        ;
        this.addRenderableWidget(
                zInput = new TextInputBoxWidget(
                        this.width / 2 + 15,
                        this.height / 2 - 5,
                        35,
                        20,
                        s -> {
                            zInput.setRenderColor(
                                    COORDINATE_PATTERN.matcher(s).matches() ? CommonColors.GREEN : CommonColors.RED);
                            updateSaveStatus();
                        },
                        this,
                        zInput));
        if (firstSetup) {
            if (oldPoi != null) {
                xInput.setTextBoxInput(String.valueOf(oldPoi.getLocation().getX()));
                Optional<Integer> y = oldPoi.getLocation().getY();
                yInput.setTextBoxInput(y.isPresent() ? String.valueOf(y) : "");
                zInput.setTextBoxInput(String.valueOf(oldPoi.getLocation().getZ()));
            } else if (setupLocation != null) {
                xInput.setTextBoxInput(String.valueOf(setupLocation.getX()));
                Optional<Integer> y = setupLocation.getY();
                yInput.setTextBoxInput(y.isPresent() ? String.valueOf(y) : "");
                zInput.setTextBoxInput(String.valueOf(setupLocation.getZ()));
            }
        }

        // endregion

        // region Icon
        this.addRenderableWidget(
                new Button(this.width / 2 - 100, this.height / 2 + 40, 20, 20, new TextComponent("<"), (button) -> {
                    if (selectedIconIndex - 1 < 0) {
                        selectedIconIndex = POI_ICONS.size() - 1;
                    } else {
                        selectedIconIndex--;
                    }
                }));
        this.addRenderableWidget(
                new Button(this.width / 2 - 40, this.height / 2 + 40, 20, 20, new TextComponent(">"), (button) -> {
                    if (selectedIconIndex + 1 >= POI_ICONS.size()) {
                        selectedIconIndex = 0;
                    } else {
                        selectedIconIndex++;
                    }
                }));
        if (oldPoi != null && firstSetup) {
            int index = POI_ICONS.indexOf(oldPoi.getIcon());
            selectedIconIndex = index == -1 ? 0 : index;
        }
        // endregion

        // region Color
        this.addRenderableWidget(
                colorInput = new TextInputBoxWidget(
                        this.width / 2 - 10,
                        this.height / 2 + 40,
                        100,
                        20,
                        (s) -> {
                            CustomColor color = CustomColor.fromHexString(s);

                            if (color == CustomColor.NONE) {
                                // Default to white
                                colorCache = CommonColors.WHITE;
                                colorInput.setRenderColor(CommonColors.RED);
                            } else {
                                colorCache = color;
                                colorInput.setRenderColor(CommonColors.GREEN);
                            }

                            updateSaveStatus();
                        },
                        this,
                        colorInput));
        if (oldPoi != null && firstSetup) {
            colorInput.setTextBoxInput(String.valueOf(oldPoi.getColor().toHexString()));
        } else if (colorInput.getTextBoxInput().isEmpty()) {
            colorInput.setTextBoxInput("#FFFFFF");
        }
        // endregion

        // region Visibility
        this.addRenderableWidget(
                new Button(this.width / 2 - 100, this.height / 2 + 90, 20, 20, new TextComponent("<"), (button) -> {
                    selectedVisiblity = CustomPoi.Visibility.values()[
                            (selectedVisiblity.ordinal() - 1 + CustomPoi.Visibility.values().length)
                                    % CustomPoi.Visibility.values().length];
                }));
        this.addRenderableWidget(
                new Button(this.width / 2 + 80, this.height / 2 + 90, 20, 20, new TextComponent(">"), (button) -> {
                    selectedVisiblity = CustomPoi.Visibility.values()[
                            (selectedVisiblity.ordinal() + 1 + CustomPoi.Visibility.values().length)
                                    % CustomPoi.Visibility.values().length];
                }));
        if (oldPoi != null && firstSetup) {
            selectedVisiblity = oldPoi.getVisibility();
        }
        // endregion

        // region Screen Interactions
        this.addRenderableWidget(
                saveButton = new Button(
                        this.width / 2 + 50,
                        this.height / 2 + 140,
                        100,
                        20,
                        new TranslatableComponent("screens.wynntils.poiCreation.save"),
                        (button) -> {
                            savePoi();
                            this.onClose();
                        }));

        this.addRenderableWidget(new Button(
                this.width / 2 - 150,
                this.height / 2 + 140,
                100,
                20,
                new TranslatableComponent("screens.wynntils.poiCreation.cancel"),
                (button) -> this.onClose()));
        // endregion

        updateSaveStatus();
        firstSetup = false;
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);

        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        I18n.get("screens.wynntils.poiCreation.waypointName") + ":",
                        this.width / 2f - 100,
                        this.height / 2f - 60,
                        CommonColors.WHITE,
                        HorizontalAlignment.Left,
                        VerticalAlignment.Top,
                        FontRenderer.TextShadow.NORMAL);

        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        I18n.get("screens.wynntils.poiCreation.coordinates") + ":",
                        this.width / 2f - 100,
                        this.height / 2f - 15,
                        CommonColors.WHITE,
                        HorizontalAlignment.Left,
                        VerticalAlignment.Top,
                        FontRenderer.TextShadow.NORMAL);
        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        "X",
                        this.width / 2f - 95,
                        this.height / 2f,
                        CommonColors.WHITE,
                        HorizontalAlignment.Left,
                        VerticalAlignment.Top,
                        FontRenderer.TextShadow.NORMAL);
        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        "Y",
                        this.width / 2f - 45,
                        this.height / 2f,
                        CommonColors.WHITE,
                        HorizontalAlignment.Left,
                        VerticalAlignment.Top,
                        FontRenderer.TextShadow.NORMAL);
        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        "Z",
                        this.width / 2f + 5,
                        this.height / 2f,
                        CommonColors.WHITE,
                        HorizontalAlignment.Left,
                        VerticalAlignment.Top,
                        FontRenderer.TextShadow.NORMAL);

        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        I18n.get("screens.wynntils.poiCreation.icon") + ":",
                        this.width / 2f - 100,
                        this.height / 2f + 30,
                        CommonColors.WHITE,
                        HorizontalAlignment.Left,
                        VerticalAlignment.Top,
                        FontRenderer.TextShadow.NORMAL);

        renderIcon(poseStack);

        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        I18n.get("screens.wynntils.poiCreation.color") + ":",
                        this.width / 2f - 10,
                        this.height / 2f + 30,
                        CommonColors.WHITE,
                        HorizontalAlignment.Left,
                        VerticalAlignment.Top,
                        FontRenderer.TextShadow.NORMAL);

        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        I18n.get("screens.wynntils.poiCreation.CustomPoi.Visibility") + ":",
                        this.width / 2f - 100,
                        this.height / 2f + 80,
                        CommonColors.WHITE,
                        HorizontalAlignment.Left,
                        VerticalAlignment.Top,
                        FontRenderer.TextShadow.NORMAL);
        FontRenderer.getInstance()
                .renderAlignedTextInBox(
                        poseStack,
                        I18n.get(selectedVisiblity.getTranslationKey()),
                        this.width / 2f - 100,
                        this.width / 2f + 100,
                        this.height / 2f + 90,
                        this.height / 2f + 110,
                        0,
                        CommonColors.WHITE,
                        HorizontalAlignment.Center,
                        VerticalAlignment.Middle,
                        FontRenderer.TextShadow.NORMAL);
    }

    private void renderIcon(PoseStack poseStack) {
        float[] color = colorCache.asFloatArray();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(color[0], color[1], color[2], 1);

        RenderUtils.drawTexturedRect(
                poseStack, POI_ICONS.get(selectedIconIndex), this.width / 2f - 70, this.height / 2f + 40);

        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return (focusedTextInput != null && focusedTextInput.charTyped(codePoint, modifiers))
                || super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // When tab is pressed, focus the next text box
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            int index = focusedTextInput == null ? 0 : children().indexOf(focusedTextInput);
            int actualIndex = Math.max(index, 0) + 1;

            // Try to find next text input
            // From index - end
            for (int i = actualIndex; i < children().size(); i++) {
                if (children().get(i) instanceof TextInputBoxWidget textInputBoxWidget) {
                    setFocusedTextInput(textInputBoxWidget);
                    return true;
                }
            }

            // From 0 - index
            for (int i = 0; i < Math.min(actualIndex, children().size()); i++) {
                if (children().get(i) instanceof TextInputBoxWidget textInputBoxWidget) {
                    setFocusedTextInput(textInputBoxWidget);
                    return true;
                }
            }
        }

        return (focusedTextInput != null && focusedTextInput.keyPressed(keyCode, scanCode, modifiers))
                || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public TextInputBoxWidget getFocusedTextInput() {
        return focusedTextInput;
    }

    @Override
    public void setFocusedTextInput(TextInputBoxWidget focusedTextInput) {
        this.focusedTextInput = focusedTextInput;
    }

    @Override
    public void onClose() {
        McUtils.mc().setScreen(oldMapScreen);
    }

    private void updateSaveStatus() {
        if (saveButton == null) return;

        saveButton.active = !nameInput.getTextBoxInput().isBlank()
                && CustomColor.fromHexString(colorInput.getTextBoxInput()) != CustomColor.NONE
                && COORDINATE_PATTERN.matcher(xInput.getTextBoxInput()).matches()
                && (COORDINATE_PATTERN.matcher(yInput.getTextBoxInput()).matches()
                        || yInput.getTextBoxInput().isEmpty())
                && COORDINATE_PATTERN.matcher(zInput.getTextBoxInput()).matches();
    }

    private void savePoi() {
        CustomPoi poi = new CustomPoi(
                new PoiLocation(
                        Integer.parseInt(xInput.getTextBoxInput()),
                        yInput.getTextBoxInput().isEmpty() ? null : Integer.parseInt(yInput.getTextBoxInput()),
                        Integer.parseInt(zInput.getTextBoxInput())),
                nameInput.getTextBoxInput(),
                CustomColor.fromHexString(colorInput.getTextBoxInput()),
                POI_ICONS.get(selectedIconIndex),
                selectedVisiblity);

        if (oldPoi != null) {
            MapFeature.INSTANCE.customPois.remove(oldPoi);
        }

        MapFeature.INSTANCE.customPois.add(poi);

        Managers.Config.saveConfig();
    }
}
