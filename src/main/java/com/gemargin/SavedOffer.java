package com.gemargin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.runelite.api.GrandExchangeOfferState;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SavedOffer
{
    private int itemId;
    private int quantitySold;
    private int totalQuantity;
    private int price;
    private int spent;
    private GrandExchangeOfferState state;

    public String serialize()
    {
        return itemId + ":" + quantitySold + ":" + totalQuantity + ":"
            + price + ":" + spent + ":" + state.name();
    }

    public static SavedOffer deserialize(String s)
    {
        if (s == null || s.isEmpty())
        {
            return null;
        }
        String[] parts = s.split(":");
        if (parts.length != 6)
        {
            return null;
        }
        return new SavedOffer(
            Integer.parseInt(parts[0]),
            Integer.parseInt(parts[1]),
            Integer.parseInt(parts[2]),
            Integer.parseInt(parts[3]),
            Integer.parseInt(parts[4]),
            GrandExchangeOfferState.valueOf(parts[5])
        );
    }
}
