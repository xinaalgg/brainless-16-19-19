package dev.tempoross.assist;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import net.runelite.api.AABB;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.Perspective;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.model.Jarvis;

/** Project the model pick bounds using the public 1.13.8 model/coordinate APIs. */
final class ActorClickbox {
    static Shape shape(Client client, Actor actor) {
        return shape(client, actor, null);
    }
    static Shape shape(Client client, Actor actor, Rectangle interest) {
        Model m = actor.getModel();
        if (m == null) return null;
        LocalPoint p = actor.getLocalLocation();
        WorldView view = actor.getWorldView();
        int orientation = actor.getCurrentOrientation();
        int height = Perspective.getFootprintTileHeight(client, p, view.getPlane(), actor.getFootprintSize())
            - actor.getAnimationHeightOffset();
        AABB b = m.getAABB(orientation);
        if (b == null) return null;
        float[] x = new float[8], y = new float[8], z = new float[8];
        for (int i = 0; i < 8; i++) {
            x[i] = b.getCenterX() + ((i & 1) == 0 ? -b.getExtremeX() : b.getExtremeX());
            y[i] = b.getCenterZ() + ((i & 2) == 0 ? -b.getExtremeZ() : b.getExtremeZ());
            z[i] = b.getCenterY() + ((i & 4) == 0 ? -b.getExtremeY() : b.getExtremeY());
        }
        int[] sx = new int[8], sy = new int[8];
        Perspective.modelToCanvas(client, view, 8, p.getX(), p.getY(), height, 0, x, y, z, sx, sy);
        for (int i = 0; i < 8; i++) if (sx[i] == Integer.MIN_VALUE || sy[i] == Integer.MIN_VALUE) return null;
        Shape bounds = Jarvis.convexHull(sx, sy);
        if (interest != null && (bounds == null || !bounds.intersects(interest))) return null;
        if (bounds == null || m.useBoundingBox()) return bounds;
        int vertices = m.getVerticesCount();
        sx = new int[vertices];
        sy = new int[vertices];
        Perspective.modelToCanvas(client, view, vertices, p.getX(), p.getY(), height, orientation,
            m.getVerticesX(), m.getVerticesZ(), m.getVerticesY(), sx, sy);
        java.awt.geom.Path2D faces = new java.awt.geom.Path2D.Double(java.awt.geom.Path2D.WIND_NON_ZERO);
        int[] fa = m.getFaceIndices1(), fb = m.getFaceIndices2(), fc = m.getFaceIndices3(), colors = m.getFaceColors3();
        for (int i = 0; i < m.getFaceCount(); i++) {
            if (colors[i] == -2) continue;
            int a = fa[i], d = fb[i], c = fc[i];
            int left = Math.min(sx[a], Math.min(sx[d], sx[c]));
            int top = Math.min(sy[a], Math.min(sy[d], sy[c]));
            if (left == Integer.MIN_VALUE || top == Integer.MIN_VALUE) continue;
            int right = Math.max(sx[a], Math.max(sx[d], sx[c]));
            int bottom = Math.max(sy[a], Math.max(sy[d], sy[c]));
            faces.append(new Rectangle(left - 5, top - 5, right - left + 10, bottom - top + 10), false);
        }
        Area area = new Area(bounds);
        area.intersect(new Area(faces));
        return area;
    }
}
