package dev.tempoross.assist;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("temporossassist")
public interface AssistConfig extends Config {
    @ConfigItem(keyName = "color", name = "Highlight color", description = "Solid fill for the current target")
    default Color color() {
        return new Color(0x00E676);
    }
    @ConfigItem(keyName = "barehanded", name = "Barehanded fishing", description = "Use your unlocked barbarian fishing method without collecting a harpoon", position = 1)
    default boolean barehanded() { return false; }
}
