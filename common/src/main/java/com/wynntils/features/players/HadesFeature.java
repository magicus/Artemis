/*
 * Copyright © Wynntils 2022-2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.players;

import com.wynntils.core.components.Models;
import com.wynntils.core.components.Services;
import com.wynntils.core.config.Category;
import com.wynntils.core.config.Config;
import com.wynntils.core.config.ConfigCategory;
import com.wynntils.core.config.RegisterConfig;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.hades.protocol.enums.SocialType;

@ConfigCategory(Category.PLAYERS)
public class HadesFeature extends Feature {
    @RegisterConfig
    public final Config<Boolean> getOtherPlayerInfo = new Config<>(true);

    @RegisterConfig
    public final Config<Boolean> shareWithParty = new Config<>(true);

    @RegisterConfig
    public final Config<Boolean> shareWithFriends = new Config<>(true);

    @RegisterConfig
    public final Config<Boolean> shareWithGuild = new Config<>(true);

    @Override
    protected void onConfigUpdate(Config<?> config) {
        switch (config.getFieldName()) {
            case "getOtherPlayerInfo" -> {
                if (getOtherPlayerInfo.get()) {
                    Services.Hades.tryResendWorldData();
                } else {
                    Services.Hades.resetHadesUsers();
                }
            }
            case "shareWithParty" -> {
                if (shareWithParty.get()) {
                    Models.Party.requestData();
                } else {
                    Services.Hades.resetSocialType(SocialType.PARTY);
                }
            }
            case "shareWithFriends" -> {
                if (shareWithFriends.get()) {
                    Models.Friends.requestData();
                } else {
                    Services.Hades.resetSocialType(SocialType.FRIEND);
                }
            }
            case "shareWithGuild" -> {
                // TODO
            }
        }
    }
}
