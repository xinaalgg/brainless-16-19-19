package dev.tempoross.assist;

import java.awt.Shape;
import java.awt.geom.Area;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;

final class ActorGeometryChecks {
    static void run() {
        WorldView view = AssistChecks.stub(WorldView.class,Map.of("isTopLevel",true,"getTileSettings",new byte[4][12][12]));
        Map<String,Object> camera = new HashMap<>();
        camera.put("getWorldView",view); camera.put("getCameraX",640); camera.put("getCameraFpX",640f);
        camera.put("getViewportWidth",800); camera.put("getViewportHeight",600); camera.put("getScale",620);
        Client client = AssistChecks.stub(Client.class,camera);
        AABB box = AssistChecks.stub(AABB.class,Map.of("getExtremeX",100,"getExtremeY",90,"getExtremeZ",40));
        Map<String,Object> model = new HashMap<>();
        model.put("getAABB",box); model.put("getVerticesCount",6); model.put("getFaceCount",2);
        model.put("getVerticesX",new float[]{-80,-70,-60,60,70,80});
        model.put("getVerticesZ",new float[]{0,10,0,0,10,0});
        model.put("getVerticesY",new float[]{-60,0,60,-60,0,60});
        model.put("getFaceIndices1",new int[]{0,3}); model.put("getFaceIndices2",new int[]{1,4});
        model.put("getFaceIndices3",new int[]{2,5}); model.put("getFaceColors3",new int[]{1,1});
        Model m = AssistChecks.stub(Model.class,model);
        LocalPoint location = new LocalPoint(640,1024,-1);
        NPC npc = AssistChecks.stub(NPC.class,Map.of("getModel",m,"getWorldView",view,"getLocalLocation",location));
        for (boolean gpu : new boolean[]{false,true}) {
            camera.put("isGpu",gpu);
            for (int yaw : new int[]{0,40,75}) {
                camera.put("getCameraYaw",yaw); camera.put("getCameraFpYaw",(float)(Math.PI*yaw/1024));
                for (boolean boundingBox : new boolean[]{false,true}) {
                    model.put("useBoundingBox",boundingBox);
                    // RuneLite's reference model pick calculation is used only as an independent test oracle.
                    Shape reference = Perspective.getClickbox(client,view,m,0,location.getX(),location.getY(),0);
                    Area expected = new Area(reference);
                    if (expected.isEmpty()) throw new AssertionError("Empty test projection");
                    Area difference = new Area(ActorClickbox.shape(client,npc));
                    difference.exclusiveOr(expected);
                    AssistChecks.eq(true,difference.isEmpty());
                }
            }
        }
    }
}
