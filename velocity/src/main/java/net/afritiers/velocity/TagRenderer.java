package net.afritiers.velocity;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.*;
import com.velocitypowered.api.proxy.Player;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

final class TagRenderer extends PacketListenerAbstract {
    private final AfriTiersVelocity plugin;
    private final Map<UUID,View> views=new ConcurrentHashMap<>();
    private record Team(Component name,Component prefix,Component suffix,NameTagVisibility visibility,CollisionRule collision,NamedTextColor color,OptionData options){
        static Team from(ScoreBoardTeamInfo info){return new Team(info.getDisplayName(),info.getPrefix(),info.getSuffix(),info.getTagVisibility(),info.getCollisionRule(),info.getColor(),info.getOptionData());}
        ScoreBoardTeamInfo packet(){return new ScoreBoardTeamInfo(name,prefix,suffix,visibility,collision,color,options);}
    }
    private static final Team EMPTY=new Team(Component.empty(),Component.empty(),Component.empty(),NameTagVisibility.ALWAYS,CollisionRule.ALWAYS,NamedTextColor.WHITE,OptionData.NONE);
    private record TabName(Component original,Component rendered){}
    private static final class View {
        final Map<String,Team> teams=new HashMap<>(),rendered=new HashMap<>();
        final Map<String,String> members=new HashMap<>();
        final Map<String,String> fakeTeams=new HashMap<>();
        final Map<UUID,TabName> tab=new HashMap<>();
        final KitObserver kits=new KitObserver();
    }
    TagRenderer(AfriTiersVelocity plugin){super(PacketListenerPriority.HIGHEST);this.plugin=plugin;}
    void reset(UUID player){views.remove(player);}
    private View view(Player player){return views.computeIfAbsent(player.getUniqueId(),id->new View());}
    private Component tag(Player viewer,View view,UUID id,String name){
        if(plugin.modded.contains(viewer.getUniqueId()))return Component.empty();
        return plugin.rankings.find(id,name).map(profile->plugin.rankings.tag(profile,plugin.prefs(viewer),plugin.packLoaded.contains(viewer.getUniqueId()),view.kits.infer(id,viewer.getUniqueId()))).orElse(Component.empty());
    }
    @Override public void onPacketSend(PacketSendEvent event){
        if(!(event.getPlayer() instanceof Player viewer))return;View view=view(viewer);
        synchronized(view){
            try{view.kits.accept(event);}catch(RuntimeException ignored){}
            if(event.getPacketType()!=PacketType.Play.Server.TEAMS)return;
            WrapperPlayServerTeams packet=new WrapperPlayServerTeams(event);String name=packet.getTeamName();if(name.startsWith("aft_"))return;
            switch(packet.getTeamMode()){
                case CREATE,UPDATE->packet.getTeamInfo().ifPresent(info->view.teams.put(name,Team.from(info)));
                case REMOVE->{view.teams.remove(name);view.members.entrySet().removeIf(entry->entry.getValue().equals(name));}
                default->{}
            }
            if(packet.getTeamMode()==TeamMode.CREATE||packet.getTeamMode()==TeamMode.ADD_ENTITIES)for(String member:packet.getPlayers())view.members.put(member,name);
            if(packet.getTeamMode()==TeamMode.REMOVE_ENTITIES)for(String member:packet.getPlayers())view.members.remove(member,name);
            if(packet.getTeamMode()==TeamMode.CREATE||packet.getTeamMode()==TeamMode.ADD_ENTITIES){
                List<String> filtered=new ArrayList<>();
                for(String member:packet.getPlayers()){
                    UUID id=viewer.getTabList().getEntries().stream().filter(entry->entry.getProfile().getName().equals(member)).map(entry->entry.getProfile().getId()).findFirst().orElse(null);
                    if(id==null||!plugin.prefs(viewer).nameTags||tag(viewer,view,id,member).equals(Component.empty()))filtered.add(member);
                }
                packet.setPlayers(filtered);
            }
            event.getTasksAfterSend().add(()->refresh(viewer));
        }
    }
    void refresh(Player viewer){
        if(!viewer.isActive()||viewer.getCurrentServer().isEmpty())return;View view=view(viewer);User user=PacketEvents.getAPI().getPlayerManager().getUser(viewer);if(user==null)return;
        synchronized(view){
            var prefs=plugin.prefs(viewer);Set<String> wanted=new HashSet<>();Set<UUID> present=new HashSet<>();
            for(var entry:viewer.getTabList().getEntries()){
                UUID id=entry.getProfile().getId();String name=entry.getProfile().getName();present.add(id);
                Component tier=tag(viewer,view,id,name),display=entry.getDisplayNameComponent().orElse(null);
                TabName prior=view.tab.get(id);Component base=prior!=null&&Objects.equals(display,prior.rendered())?prior.original():display;
                Component decorated=prefs.playerList&&!tier.equals(Component.empty())?tier.append(base==null?Component.text(name):base):base;
                if(!Objects.equals(display,decorated))entry.setDisplayName(decorated);
                view.tab.put(id,new TabName(base,decorated));
                if(!prefs.nameTags||tier.equals(Component.empty()))continue;
                String fake="aft_"+id.toString().replace("-","").substring(0,12);wanted.add(name);
                view.fakeTeams.put(name,fake);
                Team backend=view.teams.getOrDefault(view.members.get(name),EMPTY);
                NameTagVisibility visibility=visibility(backend.visibility(),view.members.get(viewer.getUsername()),view.members.get(name));
                Team rendered=new Team(backend.name(),tier.append(backend.prefix()),backend.suffix(),visibility,backend.collision(),backend.color(),backend.options());
                Team previous=view.rendered.get(name);
                if(!rendered.equals(previous)){
                    if(previous==null&&view.members.containsKey(name))user.sendPacketSilently(new WrapperPlayServerTeams(view.members.get(name),TeamMode.REMOVE_ENTITIES,(ScoreBoardTeamInfo)null,List.of(name)));
                    user.sendPacketSilently(new WrapperPlayServerTeams(fake,previous==null?TeamMode.CREATE:TeamMode.UPDATE,rendered.packet(),previous==null?List.of(name):List.of()));
                    view.rendered.put(name,rendered);
                }
            }
            view.tab.keySet().removeIf(id->!present.contains(id));
            for(String name:new ArrayList<>(view.rendered.keySet()))if(!wanted.contains(name))remove(viewer,user,view,name);
        }
    }
    static NameTagVisibility visibility(NameTagVisibility original,String viewerTeam,String subjectTeam){
        boolean same=viewerTeam!=null&&viewerTeam.equals(subjectTeam);
        return switch(original){case HIDE_FOR_OTHER_TEAMS->same?NameTagVisibility.ALWAYS:NameTagVisibility.NEVER;case HIDE_FOR_OWN_TEAM->same?NameTagVisibility.NEVER:NameTagVisibility.ALWAYS;default->original;};
    }
    private void remove(Player viewer,User user,View view,String name){
        String fake=view.fakeTeams.remove(name);
        if(fake!=null){
            user.sendPacketSilently(new WrapperPlayServerTeams(fake,TeamMode.REMOVE,(ScoreBoardTeamInfo)null,List.of()));
            String original=view.members.get(name);if(original!=null)user.sendPacketSilently(new WrapperPlayServerTeams(original,TeamMode.ADD_ENTITIES,(ScoreBoardTeamInfo)null,List.of(name)));
        }
        view.rendered.remove(name);
    }
    void restore(Player viewer){View view=views.get(viewer.getUniqueId());if(view==null)return;User user=PacketEvents.getAPI().getPlayerManager().getUser(viewer);if(user==null)return;
        synchronized(view){for(String name:new ArrayList<>(view.rendered.keySet()))remove(viewer,user,view,name);for(var entry:viewer.getTabList().getEntries()){TabName original=view.tab.get(entry.getProfile().getId());if(original!=null&&Objects.equals(entry.getDisplayNameComponent().orElse(null),original.rendered()))entry.setDisplayName(original.original());}}
    }
}
