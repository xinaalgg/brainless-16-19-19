package dev.tempoross.assist;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(name = "Brainless 16/19/19", description = "Colors the current target for the solo Tempoross 16/19/19 rotation", tags = {"tempoross", "solo", "fishing"})
public class TemporossAssistPlugin extends Plugin {
    @Inject private Client client;
    @Inject private AssistConfig config;
    @Inject private OverlayManager overlays;
    @Inject private TargetOverlay overlay;
    private final Rotation rotation = new Rotation();
    private final Acknowledgement ack = new Acknowledgement();
    private EncounterScene scene;
    private Target target, loadIntent;
    private Rotation.Bag previous;
    private WorldPoint lastPosition;
    private boolean suppressed;
    private int loadTick;

    @Provides AssistConfig provideConfig(ConfigManager configs) { return configs.getConfig(AssistConfig.class); }
    @Override protected void startUp() { scene = new EncounterScene(client); overlays.add(overlay); }
    @Override protected void shutDown() { overlays.remove(overlay); reset(); }
    private void reset() {
        ack.clear();
        rotation.reset();
        if (scene != null) scene.clear();
        target = loadIntent = null;
        previous = null;
        lastPosition = null;
        suppressed = false;
    }
    EncounterScene scene() { return scene; }
    Target visible() { return !suppressed && scene != null && scene.valid(target) ? target : null; }

    @Subscribe public void onGameStateChanged(GameStateChanged e) {
        if (e.getGameState() != GameState.LOGGED_IN) reset();
    }

    @Subscribe public void onGameTick(GameTick event) {
        Player player = client.getLocalPlayer();
        WorldView view = client.getTopLevelWorldView();
        if (!inSoloEncounter(player, view)) { reset(); return; }
        if (scene.view != view) { reset(); scene.enter(view); }
        if (scene.finished()) { reset(); return; }
        ItemContainer inv = client.getItemContainer(InventoryID.INV);
        if (inv == null) { target = null; return; }
        Rotation.Bag bag = Supplies.read(inv, client.getItemContainer(InventoryID.WORN), config.barehanded());
        int tick = client.getTickCount();
        WorldPoint here = player.getWorldLocation();
        boolean moved = lastPosition != null && !lastPosition.equals(here);
        boolean nearLoad = scene.valid(loadIntent) && here.distanceTo(loadIntent.location()) <= 3;
        boolean lostFish = previous != null && bag.fish() < previous.fish();
        if (nearLoad && lostFish) loadTick = tick;
        if (moved && scene.valid(loadIntent)) loadTick = tick;
        if (tick - loadTick > 5) { nearLoad = false; loadIntent = null; }

        Rotation.Task next = rotation.update(bag, client.getVarbitValue(VarbitID.TEMPOROSS_CURRENT_POINTS), nearLoad,
            scene.poolOpen(), client.getVarbitValue(VarbitID.TEMPOROSS_TETHERED) != 0);
        boolean keepCurrent = target != null && ack.applies(target.identity(), next);
        Target candidate = scene.choose(next, player, target, keepCurrent, rotation.allowDoubleCatch(bag));
        if (candidate == null) {
            target = null;
            ack.clear();
            suppressed = false;
        } else {
            boolean hasIntent = ack.applies(candidate.identity(), next);
            boolean progress = progress(next, previous, bag);
            boolean gathering = next == Rotation.Task.TAKE_BUCKET || next == Rotation.Task.TAKE_ROPE || next == Rotation.Task.TAKE_HARPOON;
            if (gathering && progress) ack.clear();
            boolean working = observedWork(candidate, player, progress);
            if (!gathering && working && !hasIntent) ack.accept(candidate.identity(), next, tick);
            suppressed = ack.hidden(candidate.identity(), next, tick, moved || working);
            target = candidate;
        }
        lastPosition = here;
        previous = bag;
    }

    private boolean inSoloEncounter(Player player, WorldView view) {
        if (client.getGameState() != GameState.LOGGED_IN || player == null || view == null || !view.isInstance()
            || player.getWorldView() != view || view.getPlane() != 0) return false;
        if (WorldPoint.fromLocalInstance(client, player.getLocalLocation()).getRegionID() != EncounterScene.REGION) return false;
        Widget hud = client.getWidget(InterfaceID.TemporossHud.CONTENT);
        if (hud == null || hud.isHidden()) return false;
        for (Player other : view.players()) if (other != player) return false;
        return true;
    }

    private static boolean progress(Rotation.Task task, Rotation.Bag old, Rotation.Bag now) {
        if (old == null) return false;
        switch (task) {
            case FISH: return now.fish() > old.fish();
            case COOK: return now.raw < old.raw && now.cooked > old.cooked;
            case FILL_BUCKETS: return now.water > old.water;
            case TAKE_BUCKET: return now.emptyBuckets > old.emptyBuckets;
            case TAKE_ROPE: return now.ropeReady && !old.ropeReady;
            case TAKE_HARPOON: return now.harpoonReady && !old.harpoonReady;
            default: return false;
        }
    }
    private boolean observedWork(Target t, Player p, boolean progress) {
        boolean near = p.getWorldLocation().distanceTo(t.location()) <= 3;
        if (!near) return false;
        switch (t.task) {
            case FISH:
                return (p.getInteracting() == t.npc && t.npc != null && p.getAnimation() != -1)
                    || (progress && ack.applies(t.identity(), t.task))
                    || (ack.applies(t.identity(), t.task) && fishingAnimation(p.getAnimation()));
            case COOK:
                return progress || p.getAnimation() == AnimationID.HUMAN_COOKING
                    || p.getAnimation() == AnimationID.HUMAN_COOKING_LOOP;
            case LOAD:
                return loadIntent != null && loadIntent.identity() == t.identity() && previous != null
                    && client.getTickCount() == loadTick;
            case FILL_BUCKETS: return progress;
            default: return false;
        }
    }
    private static boolean fishingAnimation(int anim) {
        switch (anim) {
            case AnimationID.HUMAN_HARPOON: case AnimationID.HUMAN_HARPOON_BARBED:
            case AnimationID.HUMAN_HARPOON_DRAGON: case AnimationID.HUMAN_HARPOON_INFERNAL:
            case AnimationID.HUMAN_HARPOON_CRYSTAL: case AnimationID.HUMAN_HARPOON_TRAILBLAZER:
            case AnimationID.HUMAN_HARPOON_TRAILBLAZER_RELOADED: case AnimationID.BRUT_PLAYER_HAND_FISHING_READY:
                return true;
            default: return false;
        }
    }
    @Subscribe public void onMenuOptionClicked(MenuOptionClicked e) {
        if (scene == null || scene.view == null) return;
        MenuEntry entry = e.getMenuEntry();
        if (scene.valid(target) && target.matches(entry)) {
            ack.accept(target.identity(), target.task, client.getTickCount());
            suppressed = true;
        } else if (!passive(entry.getType())) {
            ack.clear();
            suppressed = false;
        }
        if (passive(entry.getType())) return;
        loadIntent = null;
        if (!"Fill".equalsIgnoreCase(entry.getOption()) && !"Load".equalsIgnoreCase(entry.getOption())) return;
        NPC npc = entry.getNpc();
        if (npc != null && EncounterScene.npcTask(npc.getId()) == Rotation.Task.LOAD) {
            Target load = new Target(Rotation.Task.LOAD, npc, null, entry.getOption());
            if (load.matches(entry)) loadIntent = load;
        } else {
            for (TileObject obj : scene.objects) {
                if (EncounterScene.objectTask(obj.getId()) != Rotation.Task.LOAD) continue;
                Target load = new Target(Rotation.Task.LOAD, null, obj, entry.getOption());
                if (load.matches(entry)) { loadIntent = load; break; }
            }
        }
        loadTick = client.getTickCount();
    }
    private static boolean passive(MenuAction action) {
        return action == MenuAction.CANCEL || action == MenuAction.EXAMINE_NPC || action == MenuAction.EXAMINE_OBJECT
            || action == MenuAction.EXAMINE_ITEM || action == MenuAction.EXAMINE_ITEM_GROUND;
    }
    @Subscribe public void onGameObjectSpawned(GameObjectSpawned e) { scene.add(e.getGameObject()); }
    @Subscribe public void onGameObjectDespawned(GameObjectDespawned e) { scene.objects.remove(e.getGameObject()); }
    @Subscribe public void onWallObjectSpawned(WallObjectSpawned e) { scene.add(e.getWallObject()); }
    @Subscribe public void onWallObjectDespawned(WallObjectDespawned e) { scene.objects.remove(e.getWallObject()); }
    @Subscribe public void onDecorativeObjectSpawned(DecorativeObjectSpawned e) { scene.add(e.getDecorativeObject()); }
    @Subscribe public void onDecorativeObjectDespawned(DecorativeObjectDespawned e) { scene.objects.remove(e.getDecorativeObject()); }
    @Subscribe public void onGroundObjectSpawned(GroundObjectSpawned e) { scene.add(e.getGroundObject()); }
    @Subscribe public void onGroundObjectDespawned(GroundObjectDespawned e) { scene.objects.remove(e.getGroundObject()); }
}
