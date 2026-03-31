package com.gemargin;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("gemargin")
public interface GeMarginConfig extends Config
{
    @ConfigSection(
        name = "Cloud Sync",
        description = "Sync trades to gemargin.com",
        position = 0
    )
    String cloudSyncSection = "cloudSync";

    @ConfigItem(
        keyName = "apiKey",
        name = "API Key",
        description = "Your gemargin.com API key. Get one at gemargin.com/settings",
        section = cloudSyncSection,
        secret = true,
        position = 0
    )
    default String apiKey()
    {
        return "";
    }

    @ConfigSection(
        name = "Overlay",
        description = "GE overlay settings",
        position = 1
    )
    String overlaySection = "overlay";

    @ConfigItem(
        keyName = "showGeOverlay",
        name = "Show GE Overlay",
        description = "Show margin, ROI, and GP/hr on the GE interface",
        section = overlaySection,
        position = 0
    )
    default boolean showGeOverlay()
    {
        return true;
    }

    @ConfigSection(
        name = "Session",
        description = "Session tracker settings",
        position = 2
    )
    String sessionSection = "session";

    @ConfigItem(
        keyName = "showSessionOverlay",
        name = "Show Session Profit",
        description = "Show running profit/loss overlay",
        section = sessionSection,
        position = 0
    )
    default boolean showSessionOverlay()
    {
        return true;
    }
}
