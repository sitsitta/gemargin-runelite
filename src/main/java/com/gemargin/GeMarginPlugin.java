package com.gemargin;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GrandExchangeOfferChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import okhttp3.OkHttpClient;

import javax.inject.Inject;
import java.awt.image.BufferedImage;

@Slf4j
@PluginDescriptor(
    name = "GE Margin",
    description = "Free GE profit tracker with live margins, session stats, and cloud sync to gemargin.com",
    tags = {"ge", "grand exchange", "flipping", "margin", "profit", "money making"}
)
public class GeMarginPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private GeMarginConfig config;

    @Inject
    private ConfigManager configManager;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private ItemManager itemManager;

    @Inject
    private OkHttpClient okHttpClient;

    private OfferStateManager offerStateManager;
    private GeMarginApi api;
    private WikiPriceClient wikiPriceClient;
    private SessionTracker sessionTracker;
    private GeOverlay geOverlay;
    private GeMarginPanel panel;
    private NavigationButton navButton;

    private int loginTick = -1;
    private static final int LOGIN_BURST_TICKS = 2;

    @Override
    protected void startUp()
    {
        offerStateManager = new OfferStateManager(configManager);
        api = new GeMarginApi(okHttpClient);
        wikiPriceClient = new WikiPriceClient(okHttpClient);
        sessionTracker = new SessionTracker();

        api.setApiKey(config.apiKey());

        geOverlay = new GeOverlay(client, config, wikiPriceClient, itemManager);
        overlayManager.add(geOverlay);

        panel = new GeMarginPanel(sessionTracker, api);
        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/icon.png");
        navButton = NavigationButton.builder()
            .tooltip("GE Margin")
            .icon(icon != null ? icon : new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB))
            .priority(5)
            .panel(panel)
            .build();
        clientToolbar.addNavigation(navButton);

        wikiPriceClient.refreshPrices();

        log.info("GE Margin plugin started");
    }

    @Override
    protected void shutDown()
    {
        overlayManager.remove(geOverlay);
        clientToolbar.removeNavigation(navButton);

        api.flush();

        log.info("GE Margin plugin stopped");
    }

    @Provides
    GeMarginConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(GeMarginConfig.class);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGGED_IN)
        {
            String playerName = client.getLocalPlayer().getName();
            offerStateManager.setPlayerName(playerName);
            loginTick = client.getTickCount();

            api.setApiKey(config.apiKey());
        }
        else if (event.getGameState() == GameState.LOGIN_SCREEN)
        {
            loginTick = -1;
        }
    }

    @Subscribe
    public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged event)
    {
        if (loginTick >= 0 && client.getTickCount() - loginTick <= LOGIN_BURST_TICKS)
        {
            return;
        }

        int slot = event.getSlot();
        GrandExchangeOffer offer = event.getOffer();

        TransactionState state = offerStateManager.evaluate(slot, offer);

        if (state != TransactionState.UPDATED && state != TransactionState.COMPLETED)
        {
            return;
        }

        int itemId = offer.getItemId();
        int qty = offerStateManager.getDeltaQty();
        int price = offerStateManager.getAvgPrice();
        boolean buying = offer.getState() == GrandExchangeOfferState.BUYING
            || offer.getState() == GrandExchangeOfferState.BOUGHT;

        String itemName = itemManager.getItemComposition(itemId).getName();

        sessionTracker.recordTrade(itemId, itemName, qty, price, buying);
        panel.update();

        if (api.isConfigured())
        {
            String worldType = client.getWorldType().contains(WorldType.DEADMAN)
                ? "DEADMAN" : "REGULAR";

            TradePayload payload = new TradePayload(
                itemId, price, qty, buying, worldType,
                System.currentTimeMillis() / 1000
            );
            api.queueTrade(payload);
        }

        log.debug("{} {}x {} @ {} gp", buying ? "Bought" : "Sold", qty, itemName, price);
    }
}
