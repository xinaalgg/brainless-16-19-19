package dev.tempoross.assist;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;

final class EncounterScene {
    static final int REGION = 12076;
    private static final WorldPoint SHRINE = new WorldPoint(3041, 2873, 0);
    private static final WorldPoint WEST_CRATE = new WorldPoint(3037, 2850, 0);
    final Set<TileObject> objects = Collections.newSetFromMap(new IdentityHashMap<>());
    WorldView view;
    private final Client client;
    EncounterScene(Client client) { this.client = client; }
    void clear() { objects.clear(); view = null; }
    void enter(WorldView next) {
        clear();
        view = next;
        for (Tile[] column : view.getScene().getTiles()[view.getPlane()]) {
            for (Tile tile : column) {
                if (tile == null) continue;
                add(tile.getGroundObject());
                add(tile.getDecorativeObject());
                add(tile.getWallObject());
                for (GameObject object : tile.getGameObjects()) add(object);
            }
        }
    }

    void add(TileObject obj) {
        if (obj == null || view == null || obj.getWorldView() != view || obj.getPlane() != view.getPlane()) return;
        ObjectComposition def = client.getObjectDefinition(obj.getId());
        if (def == null) return;
        if (def.getImpostorIds() != null || objectTask(obj.getId()) != Rotation.Task.NONE || hasAction(def.getActions())) objects.add(obj);
    }

    static boolean hasAction(String[] actions) {
        if (actions != null) for (String op : actions) if (op != null && !op.isEmpty()) return true;
        return false;
    }
    ObjectComposition definition(TileObject obj) {
        ObjectComposition def = client.getObjectDefinition(obj.getId());
        return def != null && def.getImpostorIds() != null ? def.getImpostor() : def;
    }

    static Rotation.Task objectTask(int id) {
        switch (id) {
            case ObjectID.TEMPOROSS_SHRINE_FIRE: return Rotation.Task.COOK;
            case ObjectID.TEMPOROSS_WATER_PUMP: return Rotation.Task.FILL_BUCKETS;
            case ObjectID.TEMPOROSS_CRATE_BUCKET: return Rotation.Task.TAKE_BUCKET;
            case ObjectID.TEMPOROSS_CRATE_HARPOON: return Rotation.Task.TAKE_HARPOON;
            case ObjectID.TEMPOROSS_CRATE_ROPE: return Rotation.Task.TAKE_ROPE;
            case ObjectID.TEMPOROSS_CRATE_AMMUNITION_EMPTY_1:
            case ObjectID.TEMPOROSS_CRATE_AMMUNITION_EMPTY_2:
            case ObjectID.TEMPOROSS_CRATE_AMMUNITION_EMPTY_3:
            case ObjectID.TEMPOROSS_CRATE_AMMUNITION_EMPTY_4:
            case ObjectID.TEMPOROSS_CRATE_AMMUNITION_50_1:
            case ObjectID.TEMPOROSS_CRATE_AMMUNITION_50_2:
            case ObjectID.TEMPOROSS_CRATE_AMMUNITION_50_3:
            case ObjectID.TEMPOROSS_CRATE_AMMUNITION_50_4:
            case ObjectID.TEMPOROSS_CRATE_AMMUNITION_100_1:
            case ObjectID.TEMPOROSS_CRATE_AMMUNITION_100_2:
            case ObjectID.TEMPOROSS_CRATE_AMMUNITION_100_3:
            case ObjectID.TEMPOROSS_CRATE_AMMUNITION_100_4: return Rotation.Task.LOAD;
            default: return Rotation.Task.NONE;
        }
    }
    static Rotation.Task npcTask(int id) {
        switch (id) {
            case NpcID.TEMPOROSS_HARPOONFISH_FISHINGSPOT_NORTH:
            case NpcID.TEMPOROSS_HARPOONFISH_FISHINGSPOT_SPECIAL: return Rotation.Task.FISH;
            case NpcID.TEMPOROSS_NPC_CRATE_AMMUNITION_1:
            case NpcID.TEMPOROSS_NPC_CRATE_AMMUNITION_2:
            case NpcID.TEMPOROSS_NPC_CRATE_AMMUNITION_3:
            case NpcID.TEMPOROSS_NPC_CRATE_AMMUNITION_4: return Rotation.Task.LOAD;
            default: return Rotation.Task.NONE;
        }
    }

    static NPCComposition definition(NPC npc) {
        NPCComposition def = npc.getTransformedComposition();
        return def == null ? npc.getComposition() : def;
    }
    boolean poolOpen() {
        for (NPC npc : view.npcs()) {
            if (npc.getId() != NpcID.TEMPOROSS_P2_FISHINGSPOT) continue;
            NPCComposition def = definition(npc);
            if (def != null && Target.option(Rotation.Task.FISH, def.getActions()) != null) return true;
        }
        return false;
    }
    boolean finished() {
        for (NPC npc : view.npcs()) {
            switch (npc.getId()) {
                case NpcID.TEMPOROSS_INSTANCE_HOST_W_VICTORY: case NpcID.TEMPOROSS_INSTANCE_HOST_N_VICTORY:
                case NpcID.TEMPOROSS_INSTANCE_HOST_E_VICTORY: case NpcID.TEMPOROSS_INSTANCE_HOST_S_VICTORY: return true;
                default: break;
            }
        }
        return false;
    }
    boolean valid(Target t) {
        if (t == null || view == null) return false;
        if (t.npc != null) return t.npc.getWorldView() == view && view.npcs().byIndex(t.npc.getIndex()) == t.npc
            && t.npc.getId() == t.id;
        return objects.contains(t.object);
    }
    Target choose(Rotation.Task task, Player player, Target previous, boolean keepCurrent, boolean allowDouble) {
        if (task == Rotation.Task.NONE) return null;
        int best = Integer.MAX_VALUE;
        Target chosen = null;
        for (NPC npc : view.npcs()) {
            if (npcTask(npc.getId()) != task) continue;
            if (!allowDouble && npc.getId() == NpcID.TEMPOROSS_HARPOONFISH_FISHINGSPOT_SPECIAL) continue;
            WorldPoint p = WorldPoint.fromLocalInstance(client, npc.getLocalLocation());
            int distance = task == Rotation.Task.FISH ? p.distanceTo(SHRINE) : p.distanceTo(WEST_CRATE);
            if (distance > (task == Rotation.Task.FISH ? 20 : 8)) continue;
            NPCComposition def = definition(npc);
            String option = def == null ? null : Target.option(task, def.getActions());
            if (option == null) continue;
            int rank = task == Rotation.Task.FISH ? player.getWorldLocation().distanceTo(npc.getWorldLocation()) : distance;
            if (keepCurrent && previous.npc == npc) rank -= 100;
            if (npc.getId() == NpcID.TEMPOROSS_HARPOONFISH_FISHINGSPOT_SPECIAL) rank -= 1000;
            rank = rank * 2 - (previous != null && previous.npc == npc ? 1 : 0);
            if (rank < best) {
                best = rank;
                chosen = new Target(task, npc, null, option);
            }
        }
        for (TileObject obj : objects) {
            if (objectTask(obj.getId()) != task) continue;
            ObjectComposition def = definition(obj);
            String option = def == null ? null : Target.option(task, def.getActions());
            if (option == null) continue;
            WorldPoint p = WorldPoint.fromLocalInstance(client, obj.getLocalLocation());
            int distance = p.distanceTo(task == Rotation.Task.COOK ? SHRINE : WEST_CRATE);
            if (distance > (task == Rotation.Task.COOK ? 5 : 14)) continue;
            int rank = distance * 2 - (previous != null && previous.object == obj ? 1 : 0);
            if (rank < best) {
                best = rank;
                chosen = new Target(task, null, obj, option);
            }
        }
        return chosen;
    }
}
