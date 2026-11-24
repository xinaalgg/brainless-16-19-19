package dev.tempoross.assist;

import java.awt.Shape;
import java.util.Locale;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;

final class Target {
    final Rotation.Task task;
    final NPC npc;
    final TileObject object;
    final String option;
    final int id;

    Target(Rotation.Task task, NPC npc, TileObject object, String option) {
        this.task = task;
        this.npc = npc;
        this.object = object;
        this.option = option;
        this.id = npc == null ? object.getId() : npc.getId();
    }
    Object identity() { return npc == null ? object : npc; }
    WorldPoint location() { return npc == null ? object.getWorldLocation() : npc.getWorldLocation(); }
    Shape clickbox(Client client) {
        return npc == null ? object.getClickbox() : ActorClickbox.shape(client, npc);
    }
    boolean matches(MenuEntry entry) {
        if (entry == null || !option.equalsIgnoreCase(entry.getOption())) return false;
        if (npc != null) {
            int opcode = entry.getType().getId();
            return opcode >= MenuAction.NPC_FIRST_OPTION.getId() && opcode <= MenuAction.NPC_FIFTH_OPTION.getId()
                && entry.getNpc() == npc && npc.getId() == id;
        }
        MenuAction type = entry.getType();
        if (!(type == MenuAction.GAME_OBJECT_FIRST_OPTION || type == MenuAction.GAME_OBJECT_SECOND_OPTION
            || type == MenuAction.GAME_OBJECT_THIRD_OPTION || type == MenuAction.GAME_OBJECT_FOURTH_OPTION
            || type == MenuAction.GAME_OBJECT_FIFTH_OPTION)) return false;
        Point tile = object instanceof GameObject ? ((GameObject)object).getSceneMinLocation()
            : new Point(object.getLocalLocation().getSceneX(), object.getLocalLocation().getSceneY());
        return entry.getWorldViewId() == object.getWorldView().getId() && entry.getIdentifier() == id
            && entry.getParam0() == tile.getX() && entry.getParam1() == tile.getY();
    }

    static String option(Rotation.Task task, String[] options) {
        if (options == null) return null;
        for (String option : options) {
            if (option == null) continue;
            String verb = option.toLowerCase(Locale.ROOT).split("[- ]", 2)[0];
            switch (task) {
                case FISH: if (verb.equals("harpoon")) return option; break;
                case COOK: if (verb.equals("cook")) return option; break;
                case LOAD: if (verb.equals("fill") || verb.equals("load")) return option; break;
                case FILL_BUCKETS: if (verb.equals("use") || verb.equals("pump") || verb.equals("fill")) return option; break;
                case TAKE_BUCKET: case TAKE_ROPE: case TAKE_HARPOON:
                    if (verb.equals("take") || verb.equals("search")) return option;
                    break;
                default: return null;
            }
        }
        return null;
    }
}
