package com.gemargin;

import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.client.config.ConfigManager;

public class OfferStateManager
{
    private static final String CONFIG_GROUP = "gemargin";
    private static final String KEY_PREFIX = "offer.";

    private final ConfigManager configManager;
    private String playerName;

    private int deltaQty;
    private int deltaSpent;

    public OfferStateManager(ConfigManager configManager)
    {
        this.configManager = configManager;
    }

    public void setPlayerName(String playerName)
    {
        this.playerName = playerName;
    }

    public int getDeltaQty()
    {
        return deltaQty;
    }

    public int getDeltaSpent()
    {
        return deltaSpent;
    }

    public int getAvgPrice()
    {
        if (deltaQty == 0)
        {
            return 0;
        }
        return deltaSpent / deltaQty;
    }

    public TransactionState evaluate(int slot, GrandExchangeOffer offer)
    {
        deltaQty = 0;
        deltaSpent = 0;

        GrandExchangeOfferState state = offer.getState();

        if (state == GrandExchangeOfferState.EMPTY)
        {
            clearSaved(slot);
            return TransactionState.SKIP;
        }

        SavedOffer saved = loadSaved(slot);

        if (saved == null)
        {
            saveCurrent(slot, offer);
            if (offer.getQuantitySold() > 0)
            {
                return TransactionState.SKIP;
            }
            return TransactionState.NEW;
        }

        if (saved.getItemId() != offer.getItemId())
        {
            saveCurrent(slot, offer);
            if (offer.getQuantitySold() > 0)
            {
                return TransactionState.SKIP;
            }
            return TransactionState.NEW;
        }

        int qtyDiff = offer.getQuantitySold() - saved.getQuantitySold();
        int spentDiff = offer.getSpent() - saved.getSpent();

        if (qtyDiff <= 0)
        {
            if (state == GrandExchangeOfferState.CANCELLED_BUY
                || state == GrandExchangeOfferState.CANCELLED_SELL)
            {
                clearSaved(slot);
                return TransactionState.CANCELLED;
            }
            return TransactionState.NO_CHANGE;
        }

        deltaQty = qtyDiff;
        deltaSpent = spentDiff;
        saveCurrent(slot, offer);

        if (state == GrandExchangeOfferState.BOUGHT
            || state == GrandExchangeOfferState.SOLD)
        {
            clearSaved(slot);
            return TransactionState.COMPLETED;
        }

        return TransactionState.UPDATED;
    }

    private String configKey(int slot)
    {
        return KEY_PREFIX + playerName + "." + slot;
    }

    private SavedOffer loadSaved(int slot)
    {
        String raw = configManager.getConfiguration(CONFIG_GROUP, configKey(slot));
        return SavedOffer.deserialize(raw);
    }

    private void saveCurrent(int slot, GrandExchangeOffer offer)
    {
        SavedOffer s = new SavedOffer(
            offer.getItemId(),
            offer.getQuantitySold(),
            offer.getTotalQuantity(),
            offer.getPrice(),
            offer.getSpent(),
            offer.getState()
        );
        configManager.setConfiguration(CONFIG_GROUP, configKey(slot), s.serialize());
    }

    private void clearSaved(int slot)
    {
        configManager.unsetConfiguration(CONFIG_GROUP, configKey(slot));
    }
}
