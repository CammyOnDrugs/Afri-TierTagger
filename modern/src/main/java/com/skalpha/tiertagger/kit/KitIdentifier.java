package com.skalpha.tiertagger.kit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.ShulkerBoxBlock;

                                                                                                              
public final class KitIdentifier {
    private static final EquipmentSlot[] OBSERVED_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
        EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
    };
    private static final Map<UUID, Optional<String>> CACHE = new HashMap<>();
    private static final Map<String, UUID> NAMES = new HashMap<>();
    private static ClientLevel cachedWorld;
    private static long cachedTick = Long.MIN_VALUE;

    private KitIdentifier() {}

    public static Optional<String> inferredMode(UUID id) {
        Minecraft client = Minecraft.getInstance();
        if (id == null || !prepare(client)) return Optional.empty();
        return CACHE.computeIfAbsent(id, uuid -> {
            Player player = client.level.getPlayerByUUID(uuid);
            return player == null ? Optional.empty() : KitRules.infer(snapshot(player, player == client.player));
        });
    }

    public static Optional<String> byName(String name) {
        Minecraft client = Minecraft.getInstance();
        if (name == null || !prepare(client)) return Optional.empty();
        if (NAMES.isEmpty()) {
                                                                                       
            for (Player player : client.level.players()) {
                NAMES.put(player.getGameProfile().name().toLowerCase(Locale.ROOT), player.getUUID());
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

    private static boolean prepare(Minecraft client) {
        if (!client.isSameThread()) return false;
        if (client.level == null || client.player == null) {
            clear();
            return false;
        }
        long tick = client.level.getGameTime();
        if (cachedWorld != client.level || cachedTick != tick) {
            CACHE.clear();
            NAMES.clear();
            cachedWorld = client.level;
            cachedTick = tick;
        }
        return true;
    }

    private static KitSnapshot snapshot(Player player, boolean complete) {
        KitSnapshot.Builder b = KitSnapshot.builder(complete);
        if (complete) {
                                                                                           
            for (ItemStack stack : player.getInventory().getNonEquipmentItems()) addStack(b, stack);
        }
        for (EquipmentSlot slot : OBSERVED_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
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
        if (stack.is(ItemTags.BEDS)) b.item(KitRules.BED, count);
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
            b.item(KitRules.SHULKER, count);
        }
        String customName = Optional.ofNullable(stack.get(DataComponents.CUSTOM_NAME))
            .map(text -> text.getString().replaceAll("§.", "").toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", ""))
            .orElse("");
        if (stack.is(Items.PLAYER_HEAD) || (stack.is(Items.GOLDEN_APPLE)
            || stack.is(Items.ENCHANTED_GOLDEN_APPLE)) && customName.contains("goldenhead")) {
            b.item(KitRules.HEAD, count);
        }
        ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);
        if (stack.is(Items.BOW) && enchantments != null
            && enchantments.entrySet().stream()
                .anyMatch(entry -> entry.getKey().is(Enchantments.FLAME) && entry.getIntValue() > 0)) {
            b.item(KitRules.FLAME_BOW, count);
        }
        if (!stack.is(Items.POTION) && !stack.is(Items.SPLASH_POTION) && !stack.is(Items.LINGERING_POTION)) return;
        PotionContents potion = stack.get(DataComponents.POTION_CONTENTS);
        if (potion == null) return;
        Set<String> types = new HashSet<>();
        for (MobEffectInstance effect : potion.getAllEffects()) {
            if (effect.getEffect().equals(MobEffects.FIRE_RESISTANCE)) types.add(KitRules.FIRE_RESISTANCE);
            if (effect.getEffect().equals(MobEffects.SPEED)) types.add(KitRules.SPEED);
            if (effect.getEffect().equals(MobEffects.STRENGTH)) types.add(KitRules.STRENGTH);
            if (stack.is(Items.SPLASH_POTION)) {
                if (effect.getEffect().equals(MobEffects.REGENERATION)) types.add(KitRules.SPLASH_REGENERATION);
                if (effect.getEffect().equals(MobEffects.INSTANT_HEALTH) && effect.getAmplifier() == 1) {
                    types.add(KitRules.SPLASH_HEALING_II);
                }
            }
        }
        types.forEach(type -> b.item(type, count));
    }

    private static String itemId(ItemStack stack) {
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return "minecraft".equals(id.getNamespace()) ? id.getPath() : id.toString();
    }
}
