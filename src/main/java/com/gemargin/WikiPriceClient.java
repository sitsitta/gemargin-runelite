package com.gemargin;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class WikiPriceClient
{
    private static final String PRICES_URL = "https://prices.runescape.wiki/api/v1/osrs/latest";
    private static final String USER_AGENT = "gemargin-runelite-plugin - @gemargin";
    private static final long CACHE_TTL_MS = 5 * 60 * 1000;

    private final OkHttpClient client;
    private final Map<Integer, int[]> priceCache = new ConcurrentHashMap<>();
    private long lastFetchTime = 0;

    public WikiPriceClient(OkHttpClient client)
    {
        this.client = client;
    }

    public int[] getPrice(int itemId)
    {
        if (System.currentTimeMillis() - lastFetchTime > CACHE_TTL_MS)
        {
            refreshPrices();
        }
        return priceCache.get(itemId);
    }

    public void refreshPrices()
    {
        Request request = new Request.Builder()
            .url(PRICES_URL)
            .header("User-Agent", USER_AGENT)
            .build();

        client.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.warn("Failed to fetch Wiki prices: {}", e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException
            {
                try (response)
                {
                    if (!response.isSuccessful())
                    {
                        return;
                    }
                    String body = response.body().string();
                    JsonObject json = new Gson().fromJson(body, JsonObject.class);
                    JsonObject data = json.getAsJsonObject("data");

                    for (Map.Entry<String, JsonElement> entry : data.entrySet())
                    {
                        int id = Integer.parseInt(entry.getKey());
                        JsonObject item = entry.getValue().getAsJsonObject();

                        int high = item.has("high") && !item.get("high").isJsonNull()
                            ? item.get("high").getAsInt() : 0;
                        int low = item.has("low") && !item.get("low").isJsonNull()
                            ? item.get("low").getAsInt() : 0;

                        priceCache.put(id, new int[]{high, low});
                    }
                    lastFetchTime = System.currentTimeMillis();
                    log.debug("Refreshed Wiki prices: {} items", priceCache.size());
                }
            }
        });
    }
}
