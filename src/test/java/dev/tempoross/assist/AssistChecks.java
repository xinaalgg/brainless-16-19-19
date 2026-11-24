package dev.tempoross.assist;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import net.runelite.api.*;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.externalplugins.ExternalPluginManager;
import net.runelite.client.plugins.PluginDescriptor;
import static dev.tempoross.assist.Rotation.Task.*;

public final class AssistChecks {
    private static int groups;
    public static void main(String[] args) throws Exception {
        check("fresh start, mid-run attach, reset", AssistChecks::start);
        check("opening cook at eight and load at sixteen", AssistChecks::opening);
        check("remaining nineteen-fish batches and spirit intervals", AssistChecks::laterBatches);
        check("double catch, early cooking, overshoot, full backpack", AssistChecks::inventory);
        check("drops, ambiguous load, partial deposits and retries", AssistChecks::deposits);
        check("equipment, rope, buckets, water and tether", AssistChecks::supplies);
        check("exact option, NPC identity, world view and object tile", AssistChecks::menus);
        check("click acknowledgement, long travel, interruption and despawn", AssistChecks::acknowledgement);
        check("solid color, holes, borders, clipping and occlusion", AssistChecks::pixels);
        check("NPC geometry matches RuneLite CPU and GPU projections at three angles", ActorGeometryChecks::run);
        check("RuneLite registration, menu event and next render", AssistChecks::liveEvents);
        check("solo instance entry, scene selection, object fill, UI clipping and logout", SceneChecks::run);
        DevelopmentSmoke.run();
        System.out.println("PASS RuneLite plugin manager load, start and stop (offline client fixture)");
        System.out.println("Passed " + (groups + 1) + " check groups.");
    }
    private static void check(String name, Runnable body) {
        body.run(); groups++; System.out.println("PASS " + name);
    }
    static void eq(Object want, Object got) {
        if (!Objects.equals(want, got)) throw new AssertionError("Expected " + want + ", observed " + got);
    }
    private static Rotation.Bag fish(int raw, int cooked) {
        return new Rotation.Bag(raw,cooked,0,0,4,24-raw-cooked,true,true);
    }
    private static final class Run {
        Rotation r = new Rotation(); int pts;
        Rotation.Task sample(int raw, int cooked) { return r.update(fish(raw,cooked),pts,false,false,false); }
        Rotation.Task deposit(int left, int quantity) {
            pts += 65 * quantity;
            return r.update(fish(0,left),pts,true,false,false);
        }
        void begin() { eq(FISH,sample(0,0)); }
        void firstLoad() { begin(); eq(LOAD,sample(0,16)); eq(FISH,deposit(0,16)); }
    }
    private static void start() {
        Run r = new Run(); r.pts = 35; eq(NONE,r.sample(7,0));
        r.r.reset(); r.pts = 0; r.begin();
    }
    private static void opening() {
        Run r = new Run(); r.begin();
        eq(FISH,r.sample(7,0)); eq(COOK,r.sample(8,0)); eq(COOK,r.sample(1,7));
        eq(FISH,r.sample(0,8)); eq(COOK,r.sample(8,8)); eq(LOAD,r.sample(0,16));
        eq(LOAD,r.deposit(11,5)); eq(FISH,r.deposit(0,11));
    }
    private static void laterBatches() {
        Run r = new Run(); r.firstLoad();
        for (int batch = 0; batch < 3; batch++) {
            eq(FISH,r.sample(18,0)); eq(COOK,r.sample(19,0)); eq(LOAD,r.sample(0,19));
            eq(NONE,r.deposit(0,19)); eq(NONE,r.sample(0,0));
            eq(NONE,r.r.update(fish(0,0),r.pts,false,true,false));
            eq(batch == 2 ? NONE : FISH,r.sample(0,0));
        }
    }
    private static void inventory() {
        Run r = new Run(); r.begin(); eq(COOK,r.sample(9,0)); eq(FISH,r.sample(0,9));
        eq(true,r.r.allowDoubleCatch(fish(6,8))); eq(false,r.r.allowDoubleCatch(fish(7,8)));
        eq(COOK,r.sample(8,9)); eq(NONE,r.sample(0,17)); eq(LOAD,r.sample(0,16));
        eq(COOK,r.r.update(new Rotation.Bag(3,4,0,0,4,0,true,true),100,false,false,false));
        eq(NONE,r.r.update(new Rotation.Bag(0,0,0,0,4,0,true,true),100,false,false,false));
    }
    private static void deposits() {
        Run r = new Run(); r.begin(); eq(LOAD,r.sample(0,16)); eq(FISH,r.sample(0,12));
        eq(LOAD,r.sample(0,16)); eq(LOAD,r.deposit(12,4));
        eq(LOAD,r.sample(0,10)); eq(FISH,r.deposit(0,10));
        eq(COOK,r.sample(2,0)); eq(LOAD,r.sample(0,2)); eq(FISH,r.deposit(0,2));
        eq(FISH,r.sample(16,0));
        Run bad = new Run(); bad.begin(); eq(LOAD,bad.sample(0,16));
        eq(NONE,bad.r.update(fish(0,0),40,true,false,false));
        eq(NONE,bad.sample(1,0));
        Run delayed = new Run(); delayed.begin(); eq(LOAD,delayed.sample(0,16));
        eq(NONE,delayed.r.update(fish(0,0),0,true,false,false));
        eq(FISH,delayed.r.update(fish(0,0),16*65,true,false,false));
        eq(FISH,delayed.r.update(fish(17,0),16*65,false,false,false));
        Run early = new Run(); early.begin(); eq(LOAD,early.sample(0,16));
        eq(LOAD,early.r.update(fish(0,16),16*65,true,false,false));
        eq(FISH,early.r.update(fish(0,0),16*65,true,false,false));
    }
    private static void supplies() {
        Rotation r = new Rotation();
        eq(TAKE_ROPE,r.update(new Rotation.Bag(0,0,0,0,0,20,false,false),0,false,false,false));
        eq(TAKE_HARPOON,r.update(new Rotation.Bag(0,0,0,0,0,20,true,false),0,false,false,false));
        eq(TAKE_BUCKET,r.update(new Rotation.Bag(0,0,0,1,0,20,true,true),0,false,false,false));
        eq(FILL_BUCKETS,r.update(new Rotation.Bag(0,0,0,4,0,20,true,true),0,false,false,false));
        eq(NONE,r.update(fish(0,0),0,false,false,true));
        eq(FISH,r.update(fish(0,0),0,false,false,false));
    }
    @SuppressWarnings("unchecked")
    static <T> T stub(Class<T> type, Map<String,Object> values) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(obj, method, args) -> {
            String name = method.getName();
            if (name.equals("equals")) return obj == args[0];
            if (name.equals("hashCode")) return System.identityHashCode(obj);
            if (name.equals("toString")) return type.getSimpleName() + " fixture";
            if (values.containsKey(name)) {
                Object value = values.get(name);
                if (value instanceof java.util.function.Function)
                    return ((java.util.function.Function<Object[],Object>) value).apply(args);
                return value;
            }
            Class<?> t = method.getReturnType();
            if (t == boolean.class) return false;
            if (t == int.class) return 0;
            if (t == long.class) return 0L;
            if (t == float.class) return 0f;
            if (t == double.class) return 0d;
            return null;
        });
    }
    private static void menus() {
        NPC a = stub(NPC.class,Map.of("getId",10565)), b = stub(NPC.class,Map.of("getId",10565));
        Target t = new Target(FISH,a,null,"Harpoon");
        eq(true,t.matches(npcEntry(a,"Harpoon",MenuAction.NPC_FIRST_OPTION)));
        eq(false,t.matches(npcEntry(b,"Harpoon",MenuAction.NPC_FIRST_OPTION)));
        eq(false,t.matches(npcEntry(a,"Examine",MenuAction.EXAMINE_NPC)));
        eq(false,t.matches(npcEntry(a,"Harpoon",MenuAction.CANCEL)));
        WorldView v = stub(WorldView.class,Map.of("getId",7));
        GameObject obj = stub(GameObject.class,Map.of("getId",41236,"getWorldView",v,"getSceneMinLocation",new net.runelite.api.Point(20,30)));
        Target cook = new Target(COOK,null,obj,"Cook-at");
        eq(true,cook.matches(objectEntry(7,20))); eq(false,cook.matches(objectEntry(8,20)));
        eq(false,cook.matches(objectEntry(7,21)));
        eq("Fill",Target.option(LOAD,new String[]{"Check-ammo","Fill"}));
        eq(null,Target.option(LOAD,new String[]{"Check-ammo"}));
    }
    static MenuEntry npcEntry(NPC npc, String op, MenuAction type) {
        return stub(MenuEntry.class,Map.of("getNpc",npc,"getOption",op,"getType",type));
    }
    private static MenuEntry objectEntry(int world,int x) {
        return stub(MenuEntry.class,Map.of("getOption","Cook-at","getIdentifier",41236,"getWorldViewId",world,
            "getType",MenuAction.GAME_OBJECT_FIRST_OPTION,"getParam0",x,"getParam1",30));
    }
    private static void acknowledgement() {
        Object spot = new Object(); Acknowledgement ack = new Acknowledgement();
        ack.accept(spot,FISH,1); eq(true,ack.hidden(spot,FISH,1,false));
        for (int t = 2; t < 30; t++) eq(true,ack.hidden(spot,FISH,t,true));
        eq(true,ack.hidden(spot,FISH,33,false)); eq(false,ack.hidden(spot,FISH,34,false));
        ack.accept(spot,FISH,35); eq(false,ack.hidden(new Object(),FISH,36,false));
        ack.accept(spot,FISH,38); eq(false,ack.hidden(spot,COOK,38,false));
    }
    private static void pixels() {
        Area area = new Area(new Rectangle(2,2,23,8));
        area.add(new Area(new Rectangle(4,9,3,18)));
        area.subtract(new Area(new Rectangle(8,4,6,3)));
        area.intersect(new Area(new Rectangle(0,0,20,24)));
        TargetOverlay.subtract(area,new Rectangle(16,2,2,12));
        TargetOverlay.subtract(area,new Rectangle(4,21,3,1));
        BufferedImage buffer = new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = buffer.createGraphics();
        try { TargetOverlay.paint(g,area,new Color(0,230,118,4)); } finally { g.dispose(); }
        for (int y=0;y<32;y++) for (int x=0;x<32;x++) {
            boolean painted = ((x>=2 && x<20 && y>=2 && y<10) || (x>=4 && x<7 && y>=9 && y<24))
                && !(x>=8 && x<14 && y>=4 && y<7) && !(x>=16 && x<18) && y!=21;
            eq(painted ? 0xff00e676 : 0,buffer.getRGB(x,y));
        }
    }
    private static void liveEvents() {
        // Test doubles are confined to the test source set. No client or game receives input.
        if (!TemporossAssistPlugin.class.isAnnotationPresent(PluginDescriptor.class)) throw new AssertionError();
        ExternalPluginManager.loadBuiltin(TemporossAssistPlugin.class);
        TemporossAssistPlugin plugin = new TemporossAssistPlugin();
        NPC npc = stub(NPC.class,Map.of("getId",10565,"getIndex",7));
        // Bind the fake world to the same entity identity as the menu event.
        java.util.HashMap<String,Object> values = new java.util.HashMap<>();
        NPC spot = stub(NPC.class,values);
        IndexedObjectSet<?> spots = stub(IndexedObjectSet.class,Map.of("byIndex",spot));
        WorldView view = stub(WorldView.class,Map.of("npcs",spots));
        values.put("getId",10565); values.put("getWorldView",view); values.put("getIndex",7);
        Client client = stub(Client.class,Map.of("getTickCount",18));
        EncounterScene scene = new EncounterScene(client); scene.view = view;
        try {
            for (String name : List.of("client","scene","target")) {
                java.lang.reflect.Field field = TemporossAssistPlugin.class.getDeclaredField(name);
                field.setAccessible(true);
                field.set(plugin,name.equals("client") ? client : name.equals("scene") ? scene : new Target(FISH,spot,null,"Harpoon"));
            }
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        EventBus bus = new EventBus(ex -> { throw new AssertionError(ex); }); bus.register(plugin);
        bus.post(new MenuOptionClicked(npcEntry(npc,"Harpoon",MenuAction.NPC_FIRST_OPTION)));
        eq(spot,plugin.visible().npc);
        MenuOptionClicked chosen = new MenuOptionClicked(npcEntry(spot,"Harpoon",MenuAction.NPC_FIRST_OPTION));
        bus.post(chosen);
        eq(false,chosen.isConsumed()); eq(null,plugin.visible());
        bus.unregister(plugin);
    }
}
