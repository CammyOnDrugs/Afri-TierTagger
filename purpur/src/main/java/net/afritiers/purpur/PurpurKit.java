package net.afritiers.purpur;
import com.skalpha.tiertagger.kit.*;
import java.util.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.enchantments.Enchantment;
final class PurpurKit {
    static String snapshot(Player player){
        var b=KitSnapshot.builder(true);var inventory=player.getInventory();
        for(ItemStack item:inventory.getStorageContents())add(b,item);for(ItemStack item:inventory.getArmorContents())add(b,item);add(b,inventory.getItemInOffHand());
        worn(b,KitSnapshot.Slot.HEAD,inventory.getHelmet());worn(b,KitSnapshot.Slot.CHEST,inventory.getChestplate());worn(b,KitSnapshot.Slot.LEGS,inventory.getLeggings());worn(b,KitSnapshot.Slot.FEET,inventory.getBoots());return KitRules.infer(b.build()).orElse(null);
    }
    private static String id(ItemStack stack){return stack==null?"air":stack.getType().getKey().getKey();}
    private static void worn(KitSnapshot.Builder b,KitSnapshot.Slot slot,ItemStack item){b.worn(slot,id(item));}
    private static void add(KitSnapshot.Builder b,ItemStack item){
        if(item==null||item.getType().isAir())return;String id=id(item);int count=item.getAmount();b.item(id,count);
        if(id.endsWith("_bed"))b.item("#bed",count);if(id.endsWith("shulker_box"))b.item("#shulker",count);if(id.equals("player_head"))b.item("#head",count);
        if(id.equals("bow")&&item.getEnchantmentLevel(Enchantment.FLAME)>0)b.item("#flame_bow",count);
        if(!(item.getItemMeta() instanceof PotionMeta potion))return;var effects=new ArrayList<>(potion.getCustomEffects());if(potion.getBasePotionType()!=null)effects.addAll(potion.getBasePotionType().getPotionEffects());Set<String> found=new HashSet<>();
        for(var effect:effects){if(effect.getType().equals(PotionEffectType.FIRE_RESISTANCE))found.add("#fire_resistance_potion");if(effect.getType().equals(PotionEffectType.SPEED))found.add("#speed_potion");if(effect.getType().equals(PotionEffectType.STRENGTH))found.add("#strength_potion");if(id.equals("splash_potion")){if(effect.getType().equals(PotionEffectType.REGENERATION))found.add("#splash_regeneration");if(effect.getType().equals(PotionEffectType.INSTANT_HEALTH)&&effect.getAmplifier()==1)found.add("#splash_healing_ii");}}
        found.forEach(type->b.item(type,count));
    }
}
