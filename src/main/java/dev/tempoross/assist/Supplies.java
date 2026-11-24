package dev.tempoross.assist;

import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.ItemID;
import static net.runelite.api.gameval.ItemID.*;

final class Supplies {
    private static final int[] HARPOONS = { HARPOON, HUNTING_BARBED_HARPOON, DRAGON_HARPOON,
        INFERNAL_HARPOON, INFERNAL_HARPOON_EMPTY, CRYSTAL_HARPOON, CRYSTAL_HARPOON_INACTIVE,
        TRAILBLAZER_HARPOON, TRAILBLAZER_HARPOON_EMPTY, TRAILBLAZER_HARPOON_NO_INFERNAL,
        TRAILBLAZER_RELOADED_HARPOON, TRAILBLAZER_RELOADED_HARPOON_EMPTY, TRAILBLAZER_RELOADED_HARPOON_NO_INFERNAL };
    static Rotation.Bag read(ItemContainer inv, ItemContainer worn, boolean barehanded) {
        boolean hasHarpoon = barehanded;
        for (int id : HARPOONS) hasHarpoon |= inv.contains(id) || (worn != null && worn.contains(id));
        boolean outfit = worn != null && worn.contains(SPIRIT_ANGLER_HAT) && worn.contains(SPIRIT_ANGLER_TOP)
            && worn.contains(SPIRIT_ANGLER_LEGS) && worn.contains(SPIRIT_ANGLER_BOOTS);
        return new Rotation.Bag(inv.count(TEMPOROSS_RAW_HARPOONFISH), inv.count(TEMPOROSS_HARPOONFISH),
            inv.count(TEMPOROSS_CRYSTALLISED_HARPOONFISH), inv.count(BUCKET_EMPTY), inv.count(BUCKET_WATER),
            28 - inv.count(), outfit || inv.contains(ItemID.ROPE), hasHarpoon);
    }
}
