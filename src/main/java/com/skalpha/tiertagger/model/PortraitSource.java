package com.skalpha.tiertagger.model;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.Locale;

public final class PortraitSource {
    public static final String FALLBACK="https://afritiers.co.za/images/cracked-player.png?v=20261024-visage";
    private PortraitSource(){}
    public static String forPlayer(JsonObject row){
        JsonObject linked=row.has("linkedAccount")&&row.get("linkedAccount").isJsonObject()?row.getAsJsonObject("linkedAccount"):new JsonObject();
        if(text(row,"accountType").equals("cracked")||text(linked,"accountType").equals("cracked"))return FALLBACK;
        String verified=uuid(text(row,"minecraftProfileUuid"));
        boolean paid=!verified.isEmpty()||text(row,"accountType").equals("paid")||text(linked,"accountType").equals("paid");
        if(!paid)return FALLBACK;
        for(String raw:List.of(verified,text(linked,"minecraftUuid"),text(row,"uuid"))){String id=uuid(raw);if(!id.isEmpty())return "https://visage.surgeplay.com/bust/256/"+id+".png";}
        return FALLBACK;
    }
    public static boolean allowed(String value){return FALLBACK.equals(value)||value!=null&&value.matches("https://visage\\.surgeplay\\.com/bust/256/[a-f0-9]{32}\\.png");}
    private static String uuid(String value){String id=value.replace("-","").toLowerCase(Locale.ROOT);return id.matches("[a-f0-9]{32}")?id:"";}
    private static String text(JsonObject row,String key){return row.has(key)&&row.get(key).isJsonPrimitive()&&row.get(key).getAsJsonPrimitive().isString()?row.get(key).getAsString():"";}
}
