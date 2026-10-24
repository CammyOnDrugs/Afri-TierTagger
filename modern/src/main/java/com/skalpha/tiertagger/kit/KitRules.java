package com.skalpha.tiertagger.kit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

                                                                                               
public final class KitRules {
    static final String BED = "#bed";
    static final String SHULKER = "#shulker";
    static final String HEAD = "#head";
    static final String FLAME_BOW = "#flame_bow";
    static final String SPLASH_REGENERATION = "#splash_regeneration";
    static final String SPLASH_HEALING_II = "#splash_healing_ii";
    static final String FIRE_RESISTANCE = "#fire_resistance_potion";
    static final String SPEED = "#speed_potion";
    static final String STRENGTH = "#strength_potion";

    private KitRules() {}

                                                                                                       
    public static Optional<String> infer(KitSnapshot s) {
        List<String> matches = new ArrayList<>();
        boolean diamond = s.armor("diamond");
        boolean netherite = s.armor("netherite");

        if (diamond && s.has("crossbow") && s.has("diamond_axe")) matches.add("axe");
        if (diamond && s.has(HEAD) && s.has("cobweb") && s.has("diamond_pickaxe")
            && s.has("oak_planks") && s.has("lava_bucket")) matches.add("uhc");
        if (netherite && s.exact("netherite_sword", 2) && s.has("experience_bottle")
            && s.exact("totem_of_undying", 1) && s.has("netherite_axe")) matches.add("smp");
        if (diamond && s.has("cooked_beef") && s.has("netherite_sword")
            && s.has(SPLASH_REGENERATION) && s.has(SPLASH_HEALING_II)) matches.add("diapot");
        if (netherite && s.has(BED)) matches.add("bed");
        if (s.armor("chainmail") && s.has("golden_pickaxe") && s.has("creeper_spawn_egg")) matches.add("creeper");
        if (diamond && s.has("chorus_fruit") && s.has("netherite_pickaxe")) matches.add("diasmp");
        if (netherite && s.has("tnt_minecart") && s.has("firework_rocket") && s.has("rail")
            && s.exact(SHULKER, 2)) matches.add("cart");
        if (diamond && s.has("diamond_pickaxe") && s.has(FLAME_BOW)
            && s.exact(FIRE_RESISTANCE, 2) && s.exact("ender_pearl", 80)) matches.add("ogv");
        if ("diamond_helmet".equals(s.worn().get(KitSnapshot.Slot.HEAD))
            && "diamond_chestplate".equals(s.worn().get(KitSnapshot.Slot.CHEST))
            && "iron_leggings".equals(s.worn().get(KitSnapshot.Slot.LEGS))
            && "diamond_boots".equals(s.worn().get(KitSnapshot.Slot.FEET))
            && s.has("diamond_sword")) matches.add("speed");
        if (netherite && s.has("respawn_anchor") && s.has("end_crystal") && s.has("glowstone")) matches.add("vanilla");
        if (diamond && s.has("end_crystal") && s.absent("respawn_anchor")) matches.add("diavanilla");
        if (s.armor("netherite", true) && s.exact("mace", 2)
            && s.exact("totem_of_undying", 2) && s.has("elytra")) matches.add("mace");
        if (netherite && s.exact("mace", 1) && s.exact("totem_of_undying", 2)
            && s.has("netherite_spear") && s.absent("elytra")) matches.add("spear");
        if (netherite && s.has("netherite_sword") && s.exact(SPLASH_HEALING_II, 22)
            && s.exact(SPEED, 5) && s.exact(STRENGTH, 5)
            && s.exact("totem_of_undying", 2)) matches.add("nethpot");

        if (matches.size() > 1) return Optional.empty();
        if (matches.size() == 1) return Optional.of(matches.getFirst());
                                                                                                     
        return diamond && s.has("diamond_sword") ? Optional.of("sword") : Optional.empty();
    }
}
