package dev.tempoross.assist;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public final class DevelopmentClient {
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(TemporossAssistPlugin.class);
        RuneLite.main(args);
    }
}
