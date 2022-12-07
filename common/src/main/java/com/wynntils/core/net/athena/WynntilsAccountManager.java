/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.net.athena;

import com.google.gson.JsonObject;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.managers.CoreManager;
import com.wynntils.core.net.Reference;
import com.wynntils.core.net.api.ApiRequester;
import com.wynntils.core.net.api.RequestResponse;
import com.wynntils.mc.utils.ComponentUtils;
import com.wynntils.mc.utils.McUtils;
import java.math.BigInteger;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Locale;
import javax.crypto.SecretKey;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.Crypt;
import org.apache.commons.codec.binary.Hex;

public class WynntilsAccountManager extends CoreManager {
    private static String token;
    private static boolean loggedIn = false;

    private static final HashMap<String, String> encodedConfigs = new HashMap<>();
    private static final HashMap<String, String> md5Verifications = new HashMap<>();

    public static void init() {
        login();
    }

    private static void login() {
        if (loggedIn) return;

        doLogin();

        if (!loggedIn) {
            MutableComponent failed = new TextComponent(
                            "Welps! Trying to connect and set up the Wynntils Account with your data has failed. "
                                    + "Most notably, cloud config syncing will not work. To try this action again, run ")
                    .withStyle(ChatFormatting.GREEN);
            failed.append(new TextComponent("/wynntils reload")
                    .withStyle(Style.EMPTY
                            .withColor(ChatFormatting.AQUA)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/wynntils reload"))));

            if (McUtils.player() == null) {
                WynntilsMod.error(ComponentUtils.getUnformatted(failed));
                return;
            }

            McUtils.sendMessageToClient(failed);
        }
    }

    public static boolean isLoggedIn() {
        return loggedIn;
    }

    public static String getToken() {
        return token;
    }

    private static void doLogin() {
        String[] secretKey = new String[1]; // it's an array for the lambda below be able to set its value

        // generating secret key
        RequestResponse response = ApiRequester.get(Reference.URLs.getAthenaAuthGetPublicKey(), "getPublicKey");
        response.handleJsonObject(json -> {
            if (!json.has("publicKeyIn")) return false;
            secretKey[0] = parseAndJoinPublicKey(json.get("publicKeyIn").getAsString());
            return true;
        });

        // response

        JsonObject authParams = new JsonObject();
        authParams.addProperty("username", McUtils.mc().getUser().getName());
        authParams.addProperty("key", secretKey[0]);
        authParams.addProperty(
                "version", String.format("A%s %s", WynntilsMod.getVersion(), WynntilsMod.getModLoader()));

        RequestResponse response2 =
                ApiRequester.post(Reference.URLs.getAthenaAuthResponse(), authParams, "responseEncryption");
        response2.handleJsonObject(json -> {
            if (!json.has("authToken")) return false;
            token = json.get("authToken").getAsString(); /* md5 hashes*/
            JsonObject hashes = json.getAsJsonObject("hashes");
            hashes.entrySet()
                    .forEach(
                            (k) -> md5Verifications.put(k.getKey(), k.getValue().getAsString())); /* configurations*/
            JsonObject configFiles = json.getAsJsonObject("configFiles");
            configFiles
                    .entrySet()
                    .forEach((k) -> encodedConfigs.put(k.getKey(), k.getValue().getAsString()));
            loggedIn = true;
            WynntilsMod.info("Successfully connected to Athena!");
            return true;
        });
    }

    private static String parseAndJoinPublicKey(String key) {
        try {
            byte[] publicKeyBy = Hex.decodeHex(key.toCharArray());

            SecretKey secretkey = Crypt.generateSecretKey();

            PublicKey publicKey = Crypt.byteToPublicKey(publicKeyBy);

            String s1 = (new BigInteger(Crypt.digestData("", publicKey, secretkey))).toString(16);

            McUtils.mc()
                    .getMinecraftSessionService()
                    .joinServer(
                            McUtils.mc().getUser().getGameProfile(),
                            McUtils.mc().getUser().getAccessToken(),
                            s1.toLowerCase(Locale.ROOT));

            byte[] secretKeyEncrypted = Crypt.encryptUsingKey(publicKey, secretkey.getEncoded());

            return Hex.encodeHexString(secretKeyEncrypted);
        } catch (Exception ex) {
            WynntilsMod.error("Failed to parse public key.", ex);
            return "";
        }
    }
}
