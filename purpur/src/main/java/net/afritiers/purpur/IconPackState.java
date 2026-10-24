package net.afritiers.purpur;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.event.player.PlayerResourcePackStatusEvent.Status;
final class IconPackState {
    enum Outcome { WAITING, READY, REJECTED }
    private final Set<UUID> loaded=new HashSet<>();
    Outcome accept(UUID id,Status status){
        if(status==Status.SUCCESSFULLY_LOADED)loaded.add(id);
        else if(status!=Status.ACCEPTED&&status!=Status.DOWNLOADED)loaded.remove(id);
        if(!loaded.isEmpty())return Outcome.READY;
        return status==Status.ACCEPTED||status==Status.DOWNLOADED?Outcome.WAITING:Outcome.REJECTED;
    }
}
