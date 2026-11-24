package dev.tempoross.assist;

import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.*;
import net.runelite.client.ui.overlay.OverlayManager;
import okhttp3.OkHttpClient;

final class DevelopmentSmoke {
    static void run() throws Exception {
        Client client = AssistChecks.stub(Client.class, Map.of("getGameState",GameState.LOGIN_SCREEN));
        OkHttpClient offline = new OkHttpClient.Builder().addInterceptor(chain -> {
            throw new IOException("Offline test fixture");
        }).build();
        RuntimeConfigLoader configs = new RuntimeConfigLoader(offline) {
            @Override public RuntimeConfig get() { return new RuntimeConfig(); }
        };
        Injector injector = Guice.createInjector(new RuneLiteModule(offline, () -> client, configs,
            true,false,true,new File(System.getProperty("java.io.tmpdir"),"unused-session"),null,null,false,true));
        RuneLite.setInjector(injector);
        try {
            injector.getInstance(ConfigManager.class).load();
            PluginManager manager = injector.getInstance(PluginManager.class);
            List<Plugin> loaded = manager.loadPlugins(List.of(TemporossAssistPlugin.class),null);
            AssistChecks.eq(1,loaded.size());
            manager.loadDefaultPluginConfiguration(loaded);
            Plugin plugin = loaded.get(0);
            OverlayManager overlays = injector.getInstance(OverlayManager.class);
            SwingUtilities.invokeAndWait(() -> {
                try {
                    AssistChecks.eq(true,manager.startPlugin(plugin));
                    AssistChecks.eq(true,overlays.anyMatch(o -> o instanceof TargetOverlay));
                    AssistChecks.eq(true,manager.stopPlugin(plugin));
                    AssistChecks.eq(false,overlays.anyMatch(o -> o instanceof TargetOverlay));
                } catch (PluginInstantiationException e) { throw new AssertionError(e); }
            });
        } finally {
            injector.getInstance(ScheduledExecutorService.class).shutdownNow();
            offline.dispatcher().executorService().shutdownNow();
            offline.connectionPool().evictAll();
        }
    }
}
