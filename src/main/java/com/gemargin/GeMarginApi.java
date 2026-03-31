package com.gemargin;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class GeMarginApi
{
    private static final String API_BASE = "https://api.gemargin.com";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final int MAX_BATCH_SIZE = 100;

    private final OkHttpClient client;
    private String apiKey;

    private final List<TradePayload> queue = Collections.synchronizedList(new ArrayList<>());

    public GeMarginApi(OkHttpClient client)
    {
        this.client = client;
    }

    public void setApiKey(String apiKey)
    {
        this.apiKey = apiKey;
    }

    public boolean isConfigured()
    {
        return apiKey != null && !apiKey.isEmpty();
    }

    public void queueTrade(TradePayload trade)
    {
        queue.add(trade);
        flush();
    }

    public void flush()
    {
        if (!isConfigured() || queue.isEmpty())
        {
            return;
        }

        List<TradePayload> batch;
        synchronized (queue)
        {
            int size = Math.min(queue.size(), MAX_BATCH_SIZE);
            batch = new ArrayList<>(queue.subList(0, size));
            queue.subList(0, size).clear();
        }

        TradePayload.Batch payload = new TradePayload.Batch(batch);

        Request request = new Request.Builder()
            .url(API_BASE + "/api/plugin/trades")
            .header("X-API-Key", apiKey)
            .header("Content-Type", "application/json")
            .post(RequestBody.create(JSON, payload.toJson()))
            .build();

        client.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.warn("Failed to sync trades to gemargin.com: {}", e.getMessage());
                synchronized (queue)
                {
                    queue.addAll(0, batch);
                }
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                try (response)
                {
                    if (!response.isSuccessful())
                    {
                        log.warn("gemargin.com API returned {}: {}", response.code(), response.body().string());
                        if (response.code() >= 500)
                        {
                            synchronized (queue)
                            {
                                queue.addAll(0, batch);
                            }
                        }
                    }
                    else
                    {
                        log.debug("Synced {} trades to gemargin.com", batch.size());
                    }
                }
                catch (IOException e)
                {
                    log.warn("Error reading response: {}", e.getMessage());
                }
            }
        });
    }
}
