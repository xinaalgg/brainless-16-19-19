package dev.tempoross.assist;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import net.runelite.api.*;
import net.runelite.api.coords.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.*;
import net.runelite.api.widgets.Widget;
import static dev.tempoross.assist.AssistChecks.*;

/** Exercise normal tick, menu, render and teardown entry points against one synthetic instance. */
final class SceneChecks {
    static void run() {
        Map<String,Object> world = new HashMap<>(), state = new HashMap<>(), actor = new HashMap<>();
        Client client = stub(Client.class,state);
        WorldView view = stub(WorldView.class,world);
        WorldPoint shore = new WorldPoint(3041,2873,0);
        LocalPoint local = new LocalPoint(192,192,-1);
        world.put("getId",-1);
        world.put("isInstance",true);
        int[][][] chunks = new int[4][13][13];
        chunks[0][0][0] = ((3040 / 8) << 14) | ((2872 / 8) << 3);
        world.put("getInstanceTemplateChunks",chunks);
        Player player = stub(Player.class,Map.of("getWorldView",view,"getLocalLocation",local,"getWorldLocation",shore,
            "getAnimation",-1));
        List<Player> players = new ArrayList<>(); players.add(player);
        world.put("players",stub(IndexedObjectSet.class,Map.of("iterator",fn(a -> players.iterator()))));
        NPCComposition fishDef = stub(NPCComposition.class,Map.of("getActions",new String[]{"Harpoon"}));
        actor.put("getId",net.runelite.api.gameval.NpcID.TEMPOROSS_HARPOONFISH_FISHINGSPOT_NORTH);
        actor.put("getLocalLocation",local); actor.put("getWorldLocation",shore);
        actor.put("getIndex",2); actor.put("getWorldView",view); actor.put("getTransformedComposition",fishDef);
        NPC fish = stub(NPC.class,actor);
        List<NPC> npcs = new ArrayList<>(); npcs.add(fish);
        world.put("npcs",stub(IndexedObjectSet.class,Map.of("iterator",fn(a -> npcs.iterator()),
            "byIndex",fn(a -> npcs.isEmpty() ? null : fish))));
        Map<String,Object> object = new HashMap<>();
        object.put("getId",net.runelite.api.gameval.ObjectID.TEMPOROSS_SHRINE_FIRE);
        object.put("getWorldView",view); object.put("getWorldLocation",shore); object.put("getLocalLocation",local);
        object.put("getSceneMinLocation",new net.runelite.api.Point(1,1)); object.put("getClickbox",new Rectangle(2,2,20,12));
        GameObject shrine = stub(GameObject.class,object);
        Tile tile = stub(Tile.class,Map.of("getGameObjects",new GameObject[]{shrine}));
        world.put("getScene",stub(Scene.class,Map.of("getTiles",new Tile[][][]{{{tile}}})));
        state.put("getLocalPlayer",player); state.put("getWorldView",view); state.put("getTopLevelWorldView",view);
        state.put("getGameState",GameState.LOGGED_IN); state.put("getTickCount",10);
        state.put("getObjectDefinition",stub(ObjectComposition.class,Map.of("getActions",new String[]{"Cook-at"})));
        state.put("getWidget",stub(Widget.class,Map.of()));
        int[] raw = {0};
        ItemContainer inv = stub(ItemContainer.class,Map.of(
            "contains",fn(a -> (int)a[0] == net.runelite.api.gameval.ItemID.ROPE || (int)a[0] == net.runelite.api.gameval.ItemID.HARPOON),
            "count",fn(a -> a == null ? 6+raw[0] : (int)a[0] == net.runelite.api.gameval.ItemID.BUCKET_WATER ? 4
                : (int)a[0] == net.runelite.api.gameval.ItemID.TEMPOROSS_RAW_HARPOONFISH ? raw[0] : 0)));
        state.put("getItemContainer",fn(a -> (int)a[0] == net.runelite.api.gameval.InventoryID.INV ? inv : null));
        state.put("getViewportWidth",32); state.put("getViewportHeight",32);
        state.put("getMouseCanvasPosition",new net.runelite.api.Point(-1,-1));
        state.put("getWidgetRoots",new Widget[]{stub(Widget.class,Map.of("getBounds",new Rectangle(15,2,10,12),
            "getNoClickThrough",true))});
        TemporossAssistPlugin plugin = new TemporossAssistPlugin();
        AssistConfig config = new AssistConfig(){};
        set(plugin,"client",client); set(plugin,"scene",new EncounterScene(client)); set(plugin,"config",config);
        TargetOverlay overlay = new TargetOverlay(client,plugin,config);
        plugin.onGameTick(new GameTick()); eq(fish,plugin.visible().npc);
        plugin.onMenuOptionClicked(new MenuOptionClicked(npcEntry(fish,"Harpoon",MenuAction.NPC_FIRST_OPTION)));
        eq(null,plugin.visible()); npcs.clear(); eq(null,plugin.visible());
        npcs.add(fish); raw[0] = 8; state.put("getTickCount",11);
        plugin.onGameTick(new GameTick()); eq(shrine,plugin.visible().object);
        BufferedImage rendered = render(overlay); eq(0xff00e676,rendered.getRGB(8,7));
        eq(0,rendered.getRGB(18,7)); eq(0,rendered.getRGB(1,7));
        object.put("getClickbox",new Rectangle(1,19,12,8)); rendered = render(overlay);
        eq(0,rendered.getRGB(8,7)); eq(0xff00e676,rendered.getRGB(5,23));
        MenuEntry click = stub(MenuEntry.class,Map.of("getType",MenuAction.GAME_OBJECT_FIRST_OPTION,
            "getOption","Cook-at","getIdentifier",shrine.getId(),"getParam0",1,"getParam1",1,"getWorldViewId",-1));
        plugin.onMenuOptionClicked(new MenuOptionClicked(click)); eq(0,render(overlay).getRGB(5,23));
        players.add(stub(Player.class,Map.of())); plugin.onGameTick(new GameTick()); eq(null,plugin.visible());
        players.remove(1); raw[0]=0; plugin.onGameTick(new GameTick()); eq(fish,plugin.visible().npc);
        state.put("getGameState",GameState.LOGIN_SCREEN); plugin.onGameTick(new GameTick()); eq(null,plugin.visible());
    }
    private static Function<Object[],Object> fn(Function<Object[],Object> f) { return f; }
    static void set(Object target,String name,Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true); field.set(target,value);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static BufferedImage render(TargetOverlay overlay) {
        BufferedImage out = new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = out.createGraphics();
        try { overlay.render(graphics); } finally { graphics.dispose(); }
        return out;
    }
}
