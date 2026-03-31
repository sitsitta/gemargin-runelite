package com.gemargin;

import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class TradePayload
{
    private int itemId;
    private int price;
    private int qty;
    private boolean buying;
    private String worldType;
    private long timestamp;

    @Data
    @AllArgsConstructor
    public static class Batch
    {
        private List<TradePayload> trades;

        public String toJson()
        {
            return new Gson().toJson(this);
        }
    }
}
