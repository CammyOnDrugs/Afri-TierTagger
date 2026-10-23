package com.skalpha.tiertagger.kit;

import java.util.HashMap;
import java.util.Map;

/** An ephemeral, client-observed snapshot. Missing remote items are unknown, not absent. */
public record KitSnapshot(boolean completeInventory, Map<String, Integer> items, Map<Slot, String> worn) {
    public enum Slot { HEAD, CHEST, LEGS, FEET }

    public KitSnapshot {
        items = Map.copyOf(items);
        worn = Map.copyOf(worn);
    }

    public int count(String item) {
        return items.getOrDefault(item, 0);
    }

    public boolean has(String item) {
        return count(item) > 0;
    }

    public boolean exact(String item, int count) {
        return completeInventory && count(item) == count;
    }

    public boolean absent(String item) {
        return exact(item, 0);
    }

    public boolean armor(String material) {
        return armor(material, false);
    }

    public boolean armor(String material, boolean allowWornElytra) {
        return (material + "_helmet").equals(worn.get(Slot.HEAD))
            && ((material + "_chestplate").equals(worn.get(Slot.CHEST))
                || allowWornElytra && "elytra".equals(worn.get(Slot.CHEST)))
            && (material + "_leggings").equals(worn.get(Slot.LEGS))
            && (material + "_boots").equals(worn.get(Slot.FEET));
    }

    public static Builder builder(boolean completeInventory) {
        return new Builder(completeInventory);
    }

    public static final class Builder {
        private final boolean completeInventory;
        private final Map<String, Integer> items = new HashMap<>();
        private final Map<Slot, String> worn = new HashMap<>();

        private Builder(boolean completeInventory) {
            this.completeInventory = completeInventory;
        }

        public Builder item(String item, int count) {
            if (count > 0) items.merge(item, count, Integer::sum);
            return this;
        }

        public Builder item(String item) {
            return item(item, 1);
        }

        public Builder worn(Slot slot, String item) {
            worn.put(slot, item);
            return this;
        }

        public KitSnapshot build() {
            return new KitSnapshot(completeInventory, items, worn);
        }
    }
}
