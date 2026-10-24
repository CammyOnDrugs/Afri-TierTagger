package com.skalpha.tiertagger.kit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;

                                                                                                              
public final class KitIdentifier {
    private static final EquipmentSlot[] OBSERVED_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
        EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
    };
    private static final Map<UUID, Optional<String>> CACHE = new HashMap<>();
    private static final Map<String, UUID> NAMES = new HashMap<>();
    private static ClientWorld cachedWorld;
    private static long cachedTick = Long.MIN_VALUE;

    private KitIdentifier() {}

    public static Optional<String> inferredMode(UUID id) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (id == null || !prepare(client)) return Optional.empty();
        return CACHE.computeIfAbsent(id, uuid -> {
            PlayerEntity player = client.world.getPlayerByUuid(uuid);
            return player == null ? Optional.empty() : KitRules.infer(snapshot(player, player == client.player));
        });
    }

    public static Optional<String> byName(String name) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (name == null || !prepare(client)) return Optional.empty();
        if (NAMES.isEmpty()) {
                                                                                       
            for (PlayerEntity player : client.world.getPlayers()) {
                NAMES.put(player.getGameProfile().name().toLowerCase(Locale.ROOT), player.getUuid());
            }
        }
        return inferredMode(NAMES.get(name.toLowerCase(Locale.ROOT)));
    }

                                                                               
    public static void clear() {
        CACHE.clear();
        NAMES.clear();
        cachedWorld = null;
        cachedTick = Long.MIN_VALUE;
    }

    private static boolean prepare(MinecraftClient client) {
        if (!client.isOnThread()) return false;
        if (client.world == null || client.player == null) {
            clear();
            return false;
        }
        long tick = client.world.getTime();
        if (cachedWorld != client.world || cachedTick != tick) {
            CACHE.clear();
            NAMES.clear();
            cachedWorld = client.world;
            cachedTick = tick;
        }
        return true;
    }

    private static KitSnapshot snapshot(PlayerEntity player, boolean complete) {
        KitSnapshot.Builder b = KitSnapshot.builder(complete);
        if (complete) {
                                                                                           
            for (ItemStack stack : player.getInventory().getMainStacks()) addStack(b, stack);
        }
        for (EquipmentSlot slot : OBSERVED_SLOTS) {
            ItemStack stack = player.getEquippedStack(slot);
            if (!complete || slot != EquipmentSlot.MAINHAND) addStack(b, stack);
            KitSnapshot.Slot wornSlot = switch (slot) {
                case HEAD -> KitSnapshot.Slot.HEAD;
                case CHEST -> KitSnapshot.Slot.CHEST;
                case LEGS -> KitSnapshot.Slot.LEGS;
                case FEET -> KitSnapshot.Slot.FEET;
                default -> null;
            };
            if (wornSlot != null) b.worn(wornSlot, itemId(stack));
        }
        return b.build();
    }

    private static void addStack(KitSnapshot.Builder b, ItemStack stack) {
        if (stack.isEmpty()) return;
        int count = stack.getCount();
        b.item(itemId(stack), count);
        if (stack.isIn(ItemTags.BEDS)) b.item(KitRules.BED, count);
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
            b.item(KitRules.SHULKER, count);
        }
        String customName = Optional.ofNullable(stack.get(DataComponentTypes.CUSTOM_NAME))
            .map(text -> text.getString().replaceAll("§.", "").toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", ""))
            .orElse("");
        if (stack.isOf(Items.PLAYER_HEAD) || (stack.isOf(Items.GOLDEN_APPLE)
            || stack.isOf(Items.ENCHANTED_GOLDEN_APPLE)) && customName.contains("goldenhead")) {
            b.item(KitRules.HEAD, count);
        }
        ItemEnchantmentsComponent enchantments = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (stack.isOf(Items.BOW) && enchantments != null
            && enchantments.getEnchantmentEntries().stream()
                .anyMatch(entry -> entry.getKey().matchesKey(Enchantments.FLAME) && entry.getIntValue() > 0)) {
            b.item(KitRules.FLAME_BOW, count);
        }
        if (!stack.isOf(Items.POTION) && !stack.isOf(Items.SPLASH_POTION) && !stack.isOf(Items.LINGERING_POTION)) return;
        PotionContentsComponent potion = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (potion == null) return;
        Set<String> types = new HashSet<>();
        for (StatusEffectInstance effect : potion.getEffects()) {
            if (effect.getEffectType().equals(StatusEffects.FIRE_RESISTANCE)) types.add(KitRules.FIRE_RESISTANCE);
            if (effect.getEffectType().equals(StatusEffects.SPEED)) types.add(KitRules.SPEED);
            if (effect.getEffectType().equals(StatusEffects.STRENGTH)) types.add(KitRules.STRENGTH);
            if (stack.isOf(Items.SPLASH_POTION)) {
                if (effect.getEffectType().equals(StatusEffects.REGENERATION)) types.add(KitRules.SPLASH_REGENERATION);
                if (effect.getEffectType().equals(StatusEffects.INSTANT_HEALTH) && effect.getAmplifier() == 1) {
                    types.add(KitRules.SPLASH_HEALING_II);
                }
            }
        }
        types.forEach(type -> b.item(type, count));
    }

    private static String itemId(ItemStack stack) {
        var id = Registries.ITEM.getId(stack.getItem());
        return "minecraft".equals(id.getNamespace()) ? id.getPath() : id.toString();
    }
}
