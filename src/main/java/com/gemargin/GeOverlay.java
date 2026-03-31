package com.gemargin;

import net.runelite.api.Client;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.ComponentID;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;
import java.awt.*;

public class GeOverlay extends Overlay
{
    private final Client client;
    private final GeMarginConfig config;
    private final WikiPriceClient wikiPriceClient;
    private final ItemManager itemManager;
    private final PanelComponent panelComponent = new PanelComponent();

    @Inject
    public GeOverlay(Client client, GeMarginConfig config, WikiPriceClient wikiPriceClient, ItemManager itemManager)
    {
        this.client = client;
        this.config = config;
        this.wikiPriceClient = wikiPriceClient;
        this.itemManager = itemManager;
        setPosition(OverlayPosition.TOP_RIGHT);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.showGeOverlay())
        {
            return null;
        }

        Widget geOfferContainer = client.getWidget(ComponentID.GRAND_EXCHANGE_OFFER_CONTAINER);
        if (geOfferContainer == null || geOfferContainer.isHidden())
        {
            return null;
        }

        int itemId = getSelectedItemId();
        if (itemId <= 0)
        {
            return null;
        }

        int[] prices = wikiPriceClient.getPrice(itemId);
        if (prices == null || prices[0] == 0 || prices[1] == 0)
        {
            return null;
        }

        int buyPrice = prices[0];
        int sellPrice = prices[1];
        int margin = buyPrice - sellPrice;
        int tax = Math.min((int) (buyPrice * 0.02), 5_000_000);
        int netMargin = margin - tax;
        double roi = sellPrice > 0 ? (double) netMargin / sellPrice * 100 : 0;

        panelComponent.getChildren().clear();
        panelComponent.setPreferredSize(new Dimension(180, 0));

        panelComponent.getChildren().add(TitleComponent.builder()
            .text("GE Margin")
            .color(new Color(255, 215, 0))
            .build());

        panelComponent.getChildren().add(LineComponent.builder()
            .left("Buy (instant)")
            .right(formatGp(buyPrice))
            .rightColor(Color.WHITE)
            .build());

        panelComponent.getChildren().add(LineComponent.builder()
            .left("Sell (instant)")
            .right(formatGp(sellPrice))
            .rightColor(Color.WHITE)
            .build());

        panelComponent.getChildren().add(LineComponent.builder()
            .left("Margin")
            .right(formatGp(netMargin))
            .rightColor(netMargin > 0 ? Color.GREEN : Color.RED)
            .build());

        panelComponent.getChildren().add(LineComponent.builder()
            .left("ROI")
            .right(String.format("%.1f%%", roi))
            .rightColor(roi > 0 ? Color.GREEN : Color.RED)
            .build());

        panelComponent.getChildren().add(LineComponent.builder()
            .left("Tax")
            .right(formatGp(tax))
            .rightColor(new Color(255, 165, 0))
            .build());

        return panelComponent.render(graphics);
    }

    private int getSelectedItemId()
    {
        // Read from the GE offers array — find the first active offer
        GrandExchangeOffer[] offers = client.getGrandExchangeOffers();
        if (offers != null)
        {
            for (GrandExchangeOffer offer : offers)
            {
                if (offer != null && offer.getItemId() > 0
                    && offer.getState() != GrandExchangeOfferState.EMPTY)
                {
                    return offer.getItemId();
                }
            }
        }
        return -1;
    }

    private String formatGp(int amount)
    {
        if (Math.abs(amount) >= 10_000_000)
        {
            return String.format("%.1fM", amount / 1_000_000.0);
        }
        if (Math.abs(amount) >= 100_000)
        {
            return String.format("%.0fK", amount / 1_000.0);
        }
        if (Math.abs(amount) >= 10_000)
        {
            return String.format("%.1fK", amount / 1_000.0);
        }
        return String.format("%,d", amount);
    }
}
