package net.afritiers.velocity;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import com.skalpha.tiertagger.kit.*;
import java.util.*;

final class KitObserver {
    private final Map<Integer,UUID> entities=new HashMap<>();
    private final Map<UUID,Map<EquipmentSlot,ItemStack>> equipment=new HashMap<>();
    private List<ItemStack> inventory=List.of();
    void accept(PacketSendEvent event){
        var type=event.getPacketType();
        if(type==PacketType.Play.Server.SPAWN_ENTITY){var packet=new WrapperPlayServerSpawnEntity(event);if(packet.getEntityType().getName().toString().equals("minecraft:player"))packet.getUUID().ifPresent(id->entities.put(packet.getEntityId(),id));}
        else if(type==PacketType.Play.Server.DESTROY_ENTITIES){for(int id:new WrapperPlayServerDestroyEntities(event).getEntityIds()){UUID uuid=entities.remove(id);if(uuid!=null)equipment.remove(uuid);}}
        else if(type==PacketType.Play.Server.ENTITY_EQUIPMENT){var packet=new WrapperPlayServerEntityEquipment(event);UUID id=entities.get(packet.getEntityId());if(id!=null){var worn=equipment.computeIfAbsent(id,key->new EnumMap<>(EquipmentSlot.class));for(var piece:packet.getEquipment())worn.put(piece.getSlot(),piece.getItem().copy());}}
        else if(type==PacketType.Play.Server.WINDOW_ITEMS){var packet=new WrapperPlayServerWindowItems(event);if(packet.getWindowId()==0&&packet.getItems().size()>=46)inventory=new ArrayList<>(packet.getItems());else if(!inventory.isEmpty()&&packet.getItems().size()>=36){List<ItemStack> list=packet.getItems();for(int i=0;i<36;i++)inventory.set(9+i,list.get(list.size()-36+i));}}
        else if(type==PacketType.Play.Server.SET_SLOT){var packet=new WrapperPlayServerSetSlot(event);if(packet.getWindowId()==0&&!inventory.isEmpty()&&packet.getSlot()>=0&&packet.getSlot()<inventory.size())inventory.set(packet.getSlot(),packet.getItem().copy());else if(packet.getWindowId()==-2)inventory=List.of();}
        else if(type==PacketType.Play.Server.RESPAWN){entities.clear();equipment.clear();inventory=List.of();}
    }
    String infer(UUID subject,UUID viewer){
        boolean complete=subject.equals(viewer)&&!inventory.isEmpty();KitSnapshot.Builder snapshot=KitSnapshot.builder(complete);
        if(complete){for(int i=5;i<inventory.size();i++)add(snapshot,inventory.get(i));worn(snapshot,KitSnapshot.Slot.HEAD,inventory.get(5));worn(snapshot,KitSnapshot.Slot.CHEST,inventory.get(6));worn(snapshot,KitSnapshot.Slot.LEGS,inventory.get(7));worn(snapshot,KitSnapshot.Slot.FEET,inventory.get(8));}
        else {var items=equipment.get(subject);if(items==null)return null;items.forEach((slot,item)->{add(snapshot,item);switch(slot){case HELMET->worn(snapshot,KitSnapshot.Slot.HEAD,item);case CHEST_PLATE->worn(snapshot,KitSnapshot.Slot.CHEST,item);case LEGGINGS->worn(snapshot,KitSnapshot.Slot.LEGS,item);case BOOTS->worn(snapshot,KitSnapshot.Slot.FEET,item);default->{}}});}
        return KitRules.infer(snapshot.build()).orElse(null);
    }
    private static String id(ItemStack item){return item==null||item.isEmpty()?"air":item.getType().getName().toString().replace("minecraft:","");}
    private static void worn(KitSnapshot.Builder snapshot,KitSnapshot.Slot slot,ItemStack item){snapshot.worn(slot,id(item));}
    private static void add(KitSnapshot.Builder snapshot,ItemStack item){
        if(item==null||item.isEmpty())return;String id=id(item);int count=item.getAmount();snapshot.item(id,count);
        if(id.endsWith("_bed"))snapshot.item("#bed",count);if(id.endsWith("shulker_box"))snapshot.item("#shulker",count);if(id.equals("player_head"))snapshot.item("#head",count);
        var enchantments=item.getComponentOr(ComponentTypes.ENCHANTMENTS,null);if(id.equals("bow")&&enchantments!=null&&enchantments.getEnchantments().entrySet().stream().anyMatch(entry->entry.getValue()>0&&entry.getKey().getName().toString().equals("minecraft:flame")))snapshot.item("#flame_bow",count);
        var potion=item.getComponentOr(ComponentTypes.POTION_CONTENTS,null);if(potion==null||potion.getPotion()==null)return;String name=potion.getPotion().getName().toString();
        if(name.contains("fire_resistance"))snapshot.item("#fire_resistance_potion",count);if(name.contains("swiftness"))snapshot.item("#speed_potion",count);if(name.contains("strength"))snapshot.item("#strength_potion",count);
        if(id.equals("splash_potion")&&name.contains("regeneration"))snapshot.item("#splash_regeneration",count);if(id.equals("splash_potion")&&name.contains("strong_healing"))snapshot.item("#splash_healing_ii",count);
    }
}
