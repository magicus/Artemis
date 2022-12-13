/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.net;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wynntils.core.WynntilsMod;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.http.HttpRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public abstract class NetResult {
    private static final Map<HttpRequest, CompletableFuture<Void>> PROCESS_FUTURES = new HashMap<>();

    protected final HttpRequest request;

    protected NetResult(HttpRequest request) {
        this.request = request;
    }

    public void handleInputStream(Consumer<InputStream> handler, Consumer<Throwable> onError) {
        doHandle(handler, onError);
    }

    public void handleInputStream(Consumer<InputStream> handler) {
        handleInputStream(handler, onError -> {
            WynntilsMod.warn("Error while reading resource");
        });
    }

    public void handleReader(Consumer<Reader> handler, Consumer<Throwable> onError) {
        handleInputStream(is -> handler.accept(new InputStreamReader(is)), onError);
    }

    public void handleReader(Consumer<Reader> handler) {
        handleInputStream(is -> handler.accept(new InputStreamReader(is)));
    }

    public void handleJsonObject(Consumer<JsonObject> handler, Consumer<Throwable> onError) {
        handleReader(reader -> handler.accept(JsonParser.parseReader(reader).getAsJsonObject()), onError);
    }

    public void handleJsonObject(Consumer<JsonObject> handler) {
        handleJsonObject(handler, onError -> {
            WynntilsMod.warn("Error while reading resource");
        });
    }

    public void handleJsonArray(Consumer<JsonArray> handler, Consumer<Throwable> onError) {
        handleReader(reader -> handler.accept(JsonParser.parseReader(reader).getAsJsonArray()), onError);
    }

    public void handleJsonArray(Consumer<JsonArray> handler) {
        handleJsonArray(handler, onError -> {
            WynntilsMod.warn("Error while reading resource");
        });
    }

    private void doHandle(Consumer<InputStream> onCompletion, Consumer<Throwable> onError) {
        CompletableFuture<InputStream> inputStreamAsync = getInputStreamFuture();
        CompletableFuture<Void> newFuture = inputStreamAsync
                .thenAccept((is) -> onCompletion.accept(is))
                .exceptionally(e -> {
                    // FIXME: fix error handling correctly!
                    onError.accept(e);
                    return null;
                });

        CompletableFuture<Void> newFuture1 = newFuture.whenComplete((ignored, exc) -> {
            PROCESS_FUTURES.remove(request);
        });
        PROCESS_FUTURES.put(request, newFuture1);
    }

    protected abstract CompletableFuture<InputStream> getInputStreamFuture();
}
