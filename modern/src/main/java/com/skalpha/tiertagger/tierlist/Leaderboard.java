package com.skalpha.tiertagger.tierlist;

import com.skalpha.tiertagger.model.PlayerInfo;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class Leaderboard {
    public static final List<String> MODES=List.of("overall","sword","axe","mace","uhc","smp","diapot","vanilla","nethpot","ogv","creeper","speed","cart","bed","diasmp","spear","diavanilla");
    private Leaderboard(){}
    public static List<PlayerInfo> rows(Collection<PlayerInfo> source,String mode,String query){
        if(!MODES.contains(mode))throw new IllegalArgumentException("Unknown gamemode");
        String filter=query.trim().toLowerCase(Locale.ROOT);
        Comparator<PlayerInfo> order=Comparator.comparingInt(p->p.overall()>0?p.overall():Integer.MAX_VALUE);
        if(!mode.equals("overall"))order=Comparator.<PlayerInfo,Boolean>comparing(p->p.rankings().get(mode).retired()).thenComparingInt(p->tierOrder(p.rankings().get(mode).tierName())).thenComparing(order);
        return source.stream().filter(p->p.name().toLowerCase(Locale.ROOT).contains(filter)).filter(p->mode.equals("overall")||p.rankings().containsKey(mode)).sorted(order).toList();
    }
    public static int positionColor(int position){return switch(position){case 1->0xFFD700;case 2->0xC0C0C0;case 3->0xCD7F32;default->0xAAAAAA;};}
    private static int tierOrder(String value){String tier=value.replaceFirst("^R","");return tier.matches("[HL]T[1-5]")?(tier.charAt(2)-'0')*2+(tier.startsWith("LT")?1:0):999;}
}
