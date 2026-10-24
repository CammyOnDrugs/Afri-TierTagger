package com.skalpha.tiertagger.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class KitRulesTest {
    record Fixture(String mode, KitSnapshot snapshot) {
        @Override public String toString() { return mode; }
    }

    private static KitSnapshot.Builder armor(String material) {
        return KitSnapshot.builder(true)
            .worn(KitSnapshot.Slot.HEAD, material + "_helmet")
            .worn(KitSnapshot.Slot.CHEST, material + "_chestplate")
            .worn(KitSnapshot.Slot.LEGS, material + "_leggings")
            .worn(KitSnapshot.Slot.FEET, material + "_boots");
    }

    static Stream<Fixture> kits() {
        return Stream.of(
            new Fixture("sword", armor("diamond").item("diamond_sword").build()),
            new Fixture("axe", armor("diamond").item("crossbow").item("diamond_axe").build()),
            new Fixture("uhc", armor("diamond").item(KitRules.HEAD).item("cobweb").item("diamond_pickaxe")
                .item("oak_planks").item("lava_bucket").build()),
            new Fixture("smp", armor("netherite").item("netherite_sword", 2).item("experience_bottle")
                .item("totem_of_undying").item("netherite_axe").build()),
            new Fixture("diapot", armor("diamond").item("cooked_beef").item("netherite_sword")
                .item(KitRules.SPLASH_REGENERATION).item(KitRules.SPLASH_HEALING_II).build()),
            new Fixture("bed", armor("netherite").item(KitRules.BED).build()),
            new Fixture("creeper", armor("chainmail").item("golden_pickaxe").item("creeper_spawn_egg").build()),
            new Fixture("diasmp", armor("diamond").item("chorus_fruit").item("netherite_pickaxe").build()),
            new Fixture("cart", armor("netherite").item("tnt_minecart").item("firework_rocket")
                .item("rail").item(KitRules.SHULKER, 2).build()),
            new Fixture("ogv", armor("diamond").item("diamond_pickaxe").item(KitRules.FLAME_BOW)
                .item(KitRules.FIRE_RESISTANCE, 2).item("ender_pearl", 80).build()),
            new Fixture("speed", armor("diamond").worn(KitSnapshot.Slot.LEGS, "iron_leggings").item("diamond_sword").build()),
            new Fixture("vanilla", armor("netherite").item("respawn_anchor").item("end_crystal").item("glowstone").build()),
            new Fixture("diavanilla", armor("diamond").item("end_crystal").build()),
            new Fixture("mace", armor("netherite").item("mace", 2).item("totem_of_undying", 2).item("elytra").build()),
            new Fixture("spear", armor("netherite").item("mace").item("totem_of_undying", 2).item("netherite_spear").build()),
            new Fixture("nethpot", armor("netherite").item("netherite_sword").item(KitRules.SPLASH_HEALING_II, 22)
                .item(KitRules.SPEED, 5).item(KitRules.STRENGTH, 5).item("totem_of_undying", 2).build())
        );
    }

    @ParameterizedTest(name = "identifies {0}")
    @MethodSource("kits")
    void identifiesEveryRequestedCompleteKit(Fixture fixture) {
        assertEquals(Optional.of(fixture.mode()), KitRules.infer(fixture.snapshot()));
    }

    @ParameterizedTest(name = "missing item in {0} falls back")
    @MethodSource("kits")
    void deletingAnyRequiredObservedItemFallsBack(Fixture fixture) {
        KitSnapshot s = fixture.snapshot();
        for (String item : s.items().keySet()) {
            Map<String, Integer> less = new HashMap<>(s.items());
            less.remove(item);
            assertEquals(Optional.empty(), KitRules.infer(new KitSnapshot(true, less, s.worn())), fixture.mode() + ": " + item);
        }
    }

    @ParameterizedTest(name = "missing armor in {0} falls back")
    @MethodSource("kits")
    void deletingAnyRequiredWornArmorFallsBack(Fixture fixture) {
        KitSnapshot s = fixture.snapshot();
        for (KitSnapshot.Slot slot : s.worn().keySet()) {
            Map<KitSnapshot.Slot, String> less = new HashMap<>(s.worn());
            less.remove(slot);
            assertEquals(Optional.empty(), KitRules.infer(new KitSnapshot(true, s.items(), less)), fixture.mode() + ": " + slot);
        }
    }

    @Test
    void emptyOrGenericArmorIsUnknown() {
        assertTrue(KitRules.infer(KitSnapshot.builder(true).build()).isEmpty());
        assertTrue(KitRules.infer(armor("diamond").build()).isEmpty());
        assertTrue(KitRules.infer(armor("netherite").build()).isEmpty());
        assertTrue(KitRules.infer(armor("netherite").item("netherite_sword").build()).isEmpty());
    }

    @Test
    void specificDiamondKitTakesPrecedenceOverGenericSword() {
        assertEquals(Optional.of("uhc"), KitRules.infer(armor("diamond").item("diamond_sword")
            .item(KitRules.HEAD).item("cobweb").item("diamond_pickaxe").item("oak_planks").item("lava_bucket").build()));
        assertEquals(Optional.of("ogv"), KitRules.infer(armor("diamond").item("diamond_sword")
            .item("diamond_pickaxe").item(KitRules.FLAME_BOW).item(KitRules.FIRE_RESISTANCE, 2).item("ender_pearl", 80).build()));
    }

    @Test
    void conflictingSpecificKitsAreUnknownEvenWhenSwordMatches() {
        assertTrue(KitRules.infer(armor("diamond").item("diamond_sword").item("crossbow").item("diamond_axe")
            .item("chorus_fruit").item("netherite_pickaxe").build()).isEmpty());
        assertTrue(KitRules.infer(armor("netherite").item(KitRules.BED)
            .item("respawn_anchor").item("end_crystal").item("glowstone").build()).isEmpty());
    }

    @Test
    void carriedIronLeggingsDoNotChangeWornKit() {
        assertEquals(Optional.of("sword"), KitRules.infer(armor("diamond").item("diamond_sword").item("iron_leggings").build()));
        assertTrue(KitRules.infer(armor("diamond").worn(KitSnapshot.Slot.HEAD, "iron_leggings").item("diamond_sword").build()).isEmpty());
    }

    @Test
    void crystalKitRequiresKnownAnchorAbsence() {
        KitSnapshot known = armor("diamond").item("end_crystal").build();
        assertEquals(Optional.of("diavanilla"), KitRules.infer(known));
        assertTrue(KitRules.infer(new KitSnapshot(false, known.items(), known.worn())).isEmpty());
        assertTrue(KitRules.infer(armor("diamond").item("end_crystal").item("respawn_anchor").build()).isEmpty());
    }

    @Test
    void remoteObservedCountsNeverProveExactInventoryCounts() {
        kits().filter(f -> switch (f.mode()) {
            case "smp", "cart", "ogv", "mace", "spear", "nethpot", "diavanilla" -> true;
            default -> false;
        }).forEach(f -> {
            KitSnapshot s = f.snapshot();
            assertTrue(KitRules.infer(new KitSnapshot(false, s.items(), s.worn())).isEmpty(), f.mode());
        });
    }

    @Test
    void simpleRemoteEquipmentSignaturesWork() {
        for (String mode : new String[] {"sword", "axe", "bed", "creeper", "diasmp", "speed"}) {
            KitSnapshot s = kits().filter(f -> f.mode().equals(mode)).findFirst().orElseThrow().snapshot();
            assertEquals(Optional.of(mode), KitRules.infer(new KitSnapshot(false, s.items(), s.worn())));
        }
    }

    @Test
    void absentRemoteHiddenItemsDoNotCreateAnInventorySignature() {
        assertTrue(KitRules.infer(new KitSnapshot(false, Map.of("netherite_sword", 1), armor("netherite").build().worn())).isEmpty());
        assertTrue(KitRules.infer(new KitSnapshot(false, Map.of("diamond_pickaxe", 1), armor("diamond").build().worn())).isEmpty());
    }

    @Test
    void exactCountsDistinguishMaceAndSpearAndSmp() {
        assertTrue(KitRules.infer(armor("netherite").item("mace", 3).item("totem_of_undying", 2).item("elytra").build()).isEmpty());
        assertTrue(KitRules.infer(armor("netherite").item("mace").item("totem_of_undying", 2)
            .item("netherite_spear").item("elytra").build()).isEmpty());
        assertTrue(KitRules.infer(armor("netherite").item("netherite_sword", 2).item("experience_bottle")
            .item("totem_of_undying", 2).item("netherite_axe").build()).isEmpty());
    }

    @Test
    void equippedElytraIsAcceptedOnlyForMaceSignature() {
        assertEquals(Optional.of("mace"), KitRules.infer(armor("netherite").worn(KitSnapshot.Slot.CHEST, "elytra")
            .item("mace", 2).item("totem_of_undying", 2).item("elytra").build()));
        assertTrue(KitRules.infer(armor("netherite").worn(KitSnapshot.Slot.CHEST, "elytra").item(KitRules.BED).build()).isEmpty());
    }

    @Test
    void ordinaryHealingOrWaterDoesNotStandInForSplashHealingTwo() {
        assertTrue(KitRules.infer(armor("diamond").item("cooked_beef").item("netherite_sword")
            .item(KitRules.SPLASH_REGENERATION).item("potion").build()).isEmpty());
    }

    @Test
    void genericGoldenAppleOrBowDoesNotStandInForSpecialHeadOrFlame() {
        assertTrue(KitRules.infer(armor("diamond").item("golden_apple").item("cobweb")
            .item("diamond_pickaxe").item("oak_planks").item("lava_bucket").build()).isEmpty());
        assertTrue(KitRules.infer(armor("diamond").item("diamond_pickaxe").item("bow")
            .item(KitRules.FIRE_RESISTANCE, 2).item("ender_pearl", 80).build()).isEmpty());
    }

    @Test
    void opRequiresAllPotionAndTotemCounts() {
        KitSnapshot s = kits().filter(f -> f.mode().equals("nethpot")).findFirst().orElseThrow().snapshot();
        for (String item : new String[] {KitRules.SPLASH_HEALING_II, KitRules.SPEED, KitRules.STRENGTH, "totem_of_undying"}) {
            for (int delta : new int[] {-1, 1}) {
                Map<String, Integer> changed = new HashMap<>(s.items());
                changed.put(item, changed.get(item) + delta);
                assertTrue(KitRules.infer(new KitSnapshot(true, changed, s.worn())).isEmpty(), item + " " + delta);
            }
        }
    }
}
