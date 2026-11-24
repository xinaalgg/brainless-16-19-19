package dev.tempoross.assist;

import java.awt.*;
import java.awt.geom.Area;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.*;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.*;

final class TargetOverlay extends Overlay {
    private final Client client;
    private final TemporossAssistPlugin plugin;
    private final AssistConfig config;

    @Inject TargetOverlay(Client client, TemporossAssistPlugin plugin, AssistConfig config) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }
    @Override public Dimension render(Graphics2D g) {
        Target target = plugin.visible();
        if (target == null || client.isMenuOpen() || client.isWidgetSelected() || client.getCameraMode() != 0) return null;
        Shape shape = target.clickbox(client);
        if (shape == null) return null;
        Area area = new Area(shape);
        Rectangle viewport = new Rectangle(client.getViewportXOffset(), client.getViewportYOffset(),
            client.getViewportWidth(), client.getViewportHeight());
        area.intersect(new Area(viewport));
        if (area.isEmpty()) return null;
        EncounterScene scene = plugin.scene();
        Rectangle extent = area.getBounds();
        for (NPC npc : scene.view.npcs()) {
            if (npc == target.npc) continue;
            NPCComposition def = EncounterScene.definition(npc);
            if (def == null || !def.isInteractible() || !EncounterScene.hasAction(def.getActions())) continue;
            subtract(area, ActorClickbox.shape(client, npc, extent));
        }
        for (Player player : scene.view.players()) {
            if (player != client.getLocalPlayer()) subtract(area, ActorClickbox.shape(client, player, extent));
        }
        for (TileObject obj : scene.objects) {
            if (obj == target.object) continue;
            ObjectComposition def = scene.definition(obj);
            if (def != null && EncounterScene.hasAction(def.getActions())) subtract(area, obj.getClickbox());
        }
        Set<Widget> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        clipWidgets(area, client.getWidgetRoots(), viewport, seen);
        net.runelite.api.Point mouse = client.getMouseCanvasPosition();
        if (area.contains(mouse.getX(), mouse.getY())) {
            MenuEntry[] menu = client.getMenu().getMenuEntries();
            if (menu.length == 0 || !target.matches(menu[menu.length - 1])) return null;
        }
        paint(g, area, config.color());
        return null;
    }
    static void subtract(Area area, Shape blocker) {
        if (blocker != null && area.intersects(blocker.getBounds2D())) area.subtract(new Area(blocker));
    }
    static void paint(Graphics2D g, Area area, Color color) {
        Graphics2D overlay = (Graphics2D) g.create();
        try {
            overlay.setColor(new Color(color.getRGB()));
            overlay.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            overlay.fill(area);
        } finally { overlay.dispose(); }
    }
    private static void clipWidgets(Area area, Widget[] widgets, Rectangle clip, Set<Widget> seen) {
        if (widgets == null) return;
        for (Widget w : widgets) {
            if (w == null || w.isSelfHidden() || !seen.add(w)) continue;
            Rectangle box = w.getBounds().intersection(clip);
            if (box.isEmpty() || !area.intersects(box)) continue;
            // The scene viewport is itself a full-size input widget.
            if (w.getContentType() == 1337) continue;
            if (w.getNoClickThrough() || w.getOnOpListener() != null || EncounterScene.hasAction(w.getActions())) {
                subtract(area, box);
                continue;
            }
            clipWidgets(area, w.getDynamicChildren(), box, seen);
            clipWidgets(area, w.getStaticChildren(), box, seen);
            clipWidgets(area, w.getNestedChildren(), box, seen);
        }
    }
}
