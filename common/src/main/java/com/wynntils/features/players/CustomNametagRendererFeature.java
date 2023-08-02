/*
 * Copyright © Wynntils 2022-2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.players;

import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.persisted.config.RegisterConfig;
import com.wynntils.core.text.PartStyle;
import com.wynntils.core.text.StyledText;
import com.wynntils.mc.event.EntityNameTagRenderEvent;
import com.wynntils.mc.event.PlayerNametagRenderEvent;
import com.wynntils.mc.event.RenderLevelEvent;
import com.wynntils.models.gear.type.GearInfo;
import com.wynntils.models.players.WynntilsUser;
import com.wynntils.models.players.type.AccountType;
import com.wynntils.screens.gearviewer.GearViewerScreen;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.wynn.ItemUtils;
import com.wynntils.utils.wynn.RaycastUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@ConfigCategory(Category.PLAYERS)
public class CustomNametagRendererFeature extends Feature {
    // how much larger account tags should be relative to gear lines
    private static final float ACCOUNT_TYPE_MULTIPLIER = 1.5f;
    private static final float NAMETAG_HEIGHT = 0.25875f;
    private static final String WYNNTILS_LOGO = "⛨"; // Well, at least it's a shield...

    @RegisterConfig
    public final Config<Boolean> hideAllNametags = new Config<>(false);

    @RegisterConfig
    public final Config<Boolean> hidePlayerNametags = new Config<>(false);

    @RegisterConfig
    public final Config<Boolean> hideNametagBackground = new Config<>(false);

    @RegisterConfig
    public final Config<Boolean> showGearOnHover = new Config<>(true);

    @RegisterConfig
    public final Config<Boolean> showWynntilsMarker = new Config<>(true);

    @RegisterConfig
    public final Config<Float> customNametagScale = new Config<>(0.5f);

    private Player hitPlayerCache = null;

    @SubscribeEvent
    public void onPlayerNameTagRender(PlayerNametagRenderEvent event) {
        if (Models.Player.isNpc(event.getEntity())) return;

        if (hidePlayerNametags.get()) {
            event.setCanceled(true);
            return;
        }

        // If we are viewing this player's gears, do not show plus info
        if (McUtils.mc().screen instanceof GearViewerScreen gearViewerScreen
                && gearViewerScreen.getPlayer() == event.getEntity()) {
            return;
        }

        List<CustomNametag> nametags = new ArrayList<>();

        if (showGearOnHover.get()) {
            addGearNametags(event, nametags);
        }

        addAccountTypeNametag(event, nametags);

        // need to handle the rendering ourselves
        if (!nametags.isEmpty()) {
            event.setCanceled(true);
            drawNametags(event, nametags);
        }
    }

    @SubscribeEvent
    public void onEntityNameTagRender(EntityNameTagRenderEvent event) {
        if (hideAllNametags.get()) {
            event.setCanceled(true);
            return;
        }

        if (hideNametagBackground.get()) {
            event.setBackgroundOpacity(0f);
        }
    }

    @SubscribeEvent
    public void onRenderLevel(RenderLevelEvent.Pre event) {
        Optional<Player> hitPlayer = RaycastUtils.getHoveredPlayer();
        hitPlayerCache = hitPlayer.orElse(null);
    }

    private void addGearNametags(PlayerNametagRenderEvent event, List<CustomNametag> nametags) {
        LocalPlayer player = McUtils.player();

        if (hitPlayerCache != event.getEntity()) return;

        if (!Models.Player.isLocalPlayer(player)) return;

        ItemStack heldItem = hitPlayerCache.getMainHandItem();
        MutableComponent handComp = getItemComponent(heldItem);
        if (handComp != null) nametags.add(new CustomNametag(handComp, customNametagScale.get()));

        for (ItemStack armorStack : hitPlayerCache.getArmorSlots()) {
            MutableComponent armorComp = getItemComponent(armorStack);
            if (armorComp != null) nametags.add(new CustomNametag(armorComp, customNametagScale.get()));
        }
    }

    private static MutableComponent getItemComponent(ItemStack itemStack) {
        if (itemStack == null || itemStack == ItemStack.EMPTY) return null;

        // This must specifically NOT be normalized; the ֎ is significant
        String gearName = StyledText.fromComponent(itemStack.getHoverName()).getStringWithoutFormatting();
        MutableComponent description = ItemUtils.getNonGearDescription(itemStack, gearName);
        if (description != null) return description;

        GearInfo gearInfo = Models.Gear.getGearInfoFromApiName(gearName);
        if (gearInfo == null) return null;

        return Component.literal(gearInfo.name()).withStyle(gearInfo.tier().getChatFormatting());
    }

    private void addAccountTypeNametag(PlayerNametagRenderEvent event, List<CustomNametag> nametags) {
        WynntilsUser user = Models.Player.getUser(event.getEntity().getUUID());
        if (user == null) {
            if (!nametags.isEmpty()) {
                // We will cancel vanilla rendering, so we must add back the normal vanilla base nametag
                Component realName = event.getDisplayName();
                nametags.add(new CustomNametag(realName, 1f));
            }
            return;
        }

        AccountType accountType = user.accountType();
        if (accountType.getComponent() != null) {
            nametags.add(
                    new CustomNametag(accountType.getComponent(), customNametagScale.get() * ACCOUNT_TYPE_MULTIPLIER));
        }

        if (!showWynntilsMarker.get()) return;

        // Add an appropriate Wynntils marker
        Component realName = event.getDisplayName();
        Component vanillaNametag;

        StyledText styledText = StyledText.fromComponent(realName);
        if (styledText.getString(PartStyle.StyleType.NONE).startsWith("[")) {
            vanillaNametag = Component.literal(WYNNTILS_LOGO)
                    .withStyle(ChatFormatting.DARK_GRAY)
                    .append(realName);
        } else {
            vanillaNametag = Component.literal(WYNNTILS_LOGO + " ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(realName);
        }
        nametags.add(new CustomNametag(vanillaNametag, 1f));
    }

    private void drawNametags(PlayerNametagRenderEvent event, List<CustomNametag> nametags) {
        // calculate color of nametag box
        int backgroundColor =
                hideNametagBackground.get() ? 0 : ((int) (McUtils.options().getBackgroundOpacity(0.25F) * 255f) << 24);

        float yOffset = 0f;
        for (CustomNametag nametag : nametags) {
            // move rendering up to fit the next line, plus a small gap
            yOffset += nametag.nametagScale() * NAMETAG_HEIGHT;

            RenderUtils.renderCustomNametag(
                    event.getPoseStack(),
                    event.getBuffer(),
                    event.getPackedLight(),
                    backgroundColor,
                    event.getEntityRenderDispatcher(),
                    event.getEntity(),
                    nametag.nametagComponent(),
                    event.getFont(),
                    nametag.nametagScale(),
                    yOffset);
        }
    }

    private record CustomNametag(Component nametagComponent, float nametagScale) {}
}
