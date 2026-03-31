package com.gemargin;

import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SessionTracker
{
    @Getter
    private long totalProfit = 0;
    @Getter
    private int flipCount = 0;
    @Getter
    private final Instant startTime = Instant.now();

    @Getter
    private final List<TradeRecord> trades = new ArrayList<>();

    public void recordTrade(int itemId, String itemName, int qty, int price, boolean buying)
    {
        TradeRecord record = new TradeRecord(itemId, itemName, qty, price, buying, Instant.now());
        trades.add(record);

        if (!buying)
        {
            for (int i = trades.size() - 2; i >= 0; i--)
            {
                TradeRecord prev = trades.get(i);
                if (prev.getItemId() == itemId && prev.isBuying())
                {
                    int sellTotal = qty * price;
                    int tax = Math.min((int) (price * 0.02), 5_000_000) * qty;
                    int profit = sellTotal - tax - (prev.getPrice() * qty);
                    totalProfit += profit;
                    flipCount++;
                    record.setProfit(profit);
                    break;
                }
            }
        }
    }

    public void reset()
    {
        totalProfit = 0;
        flipCount = 0;
        trades.clear();
    }

    public String getSessionDuration()
    {
        long seconds = Instant.now().getEpochSecond() - startTime.getEpochSecond();
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        if (hours > 0)
        {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class TradeRecord
    {
        private int itemId;
        private String itemName;
        private int qty;
        private int price;
        private boolean buying;
        private Instant timestamp;
        @lombok.Setter
        private long profit;

        public TradeRecord(int itemId, String itemName, int qty, int price, boolean buying, Instant timestamp)
        {
            this(itemId, itemName, qty, price, buying, timestamp, 0);
        }
    }
}
