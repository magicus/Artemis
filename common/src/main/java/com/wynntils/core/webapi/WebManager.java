/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.webapi;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wynntils.core.managers.CoreManager;
import com.wynntils.core.net.api.ApiRequester;
import com.wynntils.core.webapi.account.WynntilsAccount;
import com.wynntils.wynn.netresources.ItemProfilesManager;
import com.wynntils.wynn.netresources.SplashManager;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Provides and loads web content on demand */
public final class WebManager extends CoreManager {

    public static final Gson gson = new Gson();

    public static void init() {
        ApiUrls.tryReloadApiUrls();
        WynntilsAccount.setupUserAccount();

        SplashManager.init();

        ItemProfilesManager.loadCommonObjects();
    }

    public static void reset() {
        ApiUrls.reset();

        ItemProfilesManager.reset();
    }

    /**
     * Request all online players to WynnAPI
     *
     * @return a {@link HashMap} who the key is the server and the value is an array containing all
     *     players on it
     * @throws IOException thrown by URLConnection
     */
    public static Map<String, List<String>> getOnlinePlayers() throws IOException {
        if (ApiUrls.getApiUrls() == null || !ApiUrls.getApiUrls().hasKey("OnlinePlayers")) return new HashMap<>();

        URLConnection st = ApiRequester.generateURLRequestWithWynnApiKey(ApiUrls.getApiUrls().get("OnlinePlayers"));
        InputStreamReader stInputReader = new InputStreamReader(st.getInputStream(), StandardCharsets.UTF_8);
        JsonObject main = JsonParser.parseReader(stInputReader).getAsJsonObject();

        if (!main.has("message")) {
            main.remove("request");

            Type type = new TypeToken<LinkedHashMap<String, ArrayList<String>>>() {}.getType();

            return gson.fromJson(main, type);
        } else {
            return new HashMap<>();
        }
    }

}
