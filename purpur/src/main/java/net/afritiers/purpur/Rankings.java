package net.afritiers.purpur;

import com.google.gson.*;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class Rankings {
    public static final List<String> MODES=List.of("overall","sword","axe","mace","uhc","smp","diapot","vanilla","nethpot","ogv","creeper","speed","cart","bed","diasmp","spear","diavanilla");
    public record Tier(String mode,String level,boolean retired){}
    public record Profile(String uuid,String name,String region,int points,int position,List<Tier> tiers){}
    private volatile List<Profile> profiles=List.of();
    public static String mode(String input){return switch(input.toLowerCase(Locale.ROOT)){
        case "pot","dia_pot"->"diapot";case "op","nethop"->"nethpot";case "minecart"->"cart";
        case "dia_smp"->"diasmp";case "spearmace","diamond_spear"->"spear";case "crystal"->"vanilla";case "dia_vanilla"->"diavanilla";default->input.toLowerCase(Locale.ROOT);};}
    public static int tierValue(Tier tier){try{return Integer.parseInt(tier.level().replaceAll("[^0-9]",""))*2+(tier.level().startsWith("LT")?1:0);}catch(Exception error){return 999;}}
    public static List<Profile> parse(String raw){
        var rows=JsonParser.parseString(raw).getAsJsonObject().getAsJsonArray("players");List<Profile> profiles=new ArrayList<>();
        for(var element:rows){var row=element.getAsJsonObject();List<Tier> tiers=new ArrayList<>();
            for(var item:row.getAsJsonArray("tiers")){var t=item.getAsJsonObject();String mode=mode(t.get("mode").getAsString()),level=t.get("level").getAsString();
                if(MODES.contains(mode)&&!mode.equals("overall")&&level.matches("(?:R)?[HL]T[1-5]"))tiers.add(new Tier(mode,level.replaceFirst("^R",""),t.has("retired")&&t.get("retired").getAsBoolean()));}
            profiles.add(new Profile(string(row,"uuid"),string(row,"name"),string(row,"region"),row.has("totalPoints")?row.get("totalPoints").getAsInt():0,0,List.copyOf(tiers)));
        }
        List<Profile> ranked=profiles.stream().filter(p->!p.tiers().isEmpty()).sorted(Comparator.comparingInt(Profile::points).reversed()).toList();
        Map<Profile,Integer> positions=new IdentityHashMap<>();for(int i=0;i<ranked.size();i++)positions.put(ranked.get(i),i+1);
        return profiles.stream().map(p->new Profile(p.uuid(),p.name(),p.region(),p.points(),positions.getOrDefault(p,0),p.tiers())).toList();
    }
    private static String string(JsonObject row,String key){return row.has(key)&&!row.get(key).isJsonNull()?row.get(key).getAsString():"";}
    public void update(String raw){profiles=parse(raw);}
    public List<Profile> all(){return profiles;}
    public Optional<Profile> find(UUID uuid,String name){
        String id=uuid==null?"":uuid.toString().replace("-","");
        Optional<Profile> found=profiles.stream().filter(p->!id.isEmpty()&&p.uuid().replace("-","").equalsIgnoreCase(id)).findFirst();
        return found.isPresent()?found:profiles.stream().filter(p->p.name().equalsIgnoreCase(name)).findFirst();
    }
    public static int positionColor(int position){return switch(position){case 1->0xFFD700;case 2->0xC0C0C0;case 3->0xCD7F32;default->0xAAAAAA;};}
    private static String icon(String mode){return switch(mode){case "axe"->"\uE701";case "mace"->"\uE702";case "nethpot"->"\uE703";case "diapot"->"\uE704";case "smp"->"\uE705";case "sword"->"\uE706";case "uhc"->"\uE707";case "vanilla"->"\uE708";case "diavanilla"->"\uE814";case "bed"->"\uE801";case "creeper"->"\uE803";case "diasmp"->"\uE806";case "cart"->"\uE809";case "ogv"->"\uE810";case "speed"->"\uE811";case "spear"->"\uE813";default->"";};}
    public Component tag(Profile profile,Preferences settings,boolean resourcePack,String inferred){
        if(!settings.enabled)return Component.empty();
        List<Tier> tiers=profile.tiers().stream().filter(t->settings.showRetired||!t.retired()).sorted(Comparator.comparing(Tier::retired).thenComparingInt(Rankings::tierValue)).toList();
        if(tiers.isEmpty())return Component.empty();
        String selected=settings.kitIdentifier&&inferred!=null?inferred:settings.gameMode;
        Tier chosen=tiers.stream().filter(t->t.mode().equals(selected)).findFirst().orElse(null);
        if(selected.equals("overall")||settings.highest.equals("always")||(chosen==null&&settings.highest.equals("not_found")))chosen=tiers.getFirst();
        if(chosen==null)return Component.empty();
        Component tag=Component.empty();
        if(resourcePack&&profile.position()>=1&&profile.position()<=3)tag=tag.append(Component.text(""+(char)(0xE900+profile.position())+" ",WHITE));
        if(settings.rankNumbers&&profile.position()>0)tag=tag.append(Component.text("#"+profile.position()+" ",TextColor.color(settings.podiumColors?positionColor(profile.position()):0xAAAAAA)));
        String modeLabel=resourcePack?icon(chosen.mode()):chosen.mode();
        int color=settings.colors.getOrDefault(chosen.retired()?"retired":chosen.level(),0xAAAAAA);
        return tag.append(Component.text(modeLabel+" ",WHITE)).append(Component.text((chosen.retired()?"R":"")+chosen.level(),TextColor.color(color))).append(Component.text(" | ",GRAY));
    }
}
