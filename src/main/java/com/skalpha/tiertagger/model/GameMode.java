package com.skalpha.tiertagger.model;

import it.unimi.dsi.fastutil.Pair;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

public record GameMode(String id, String title, int color) {
   public static final GameMode NONE = new GameMode("annoying_long_id_that_no_one_will_ever_use_just_to_make_sure", "§cNone§r", 11184810);
   public static final GameMode OVERALL = new GameMode("overall", "§eOverall§r", 16766720);

   public static GameMode byId(String id) {
      if (id == null) {
         return NONE;
      } else {
         String lower = id.toLowerCase(Locale.ROOT);

         String canonical = switch (lower) {
            case "ogv", "og vanilla", "ogvanilla" -> "ogv";
            case "minecart", "cart" -> "cart";
            case "diamond_spear", "spearmace", "spear mace", "spear" -> "spear";
            case "dia_smp", "diasmp" -> "diasmp";
            case "pot", "diapot", "dia pot", "dia_pot", "diamondpot", "diamond pot" -> "diapot";
            case "op", "nethop", "neth_pot", "neth pot", "netherite pot", "netheritepot", "nethpot" -> "nethpot";
            case "dia_vanilla", "diavanilla", "dia_crystal" -> "diavanilla";
            case "crystal", "crystalpvp", "crystal pvp" -> "vanilla";
            default -> lower;
         };

         String title = switch (canonical) {
            case "rocketmace" -> "§bRocketMace§r";
            case "holefight" -> "§bHoleFight§r";
            case "axe" -> "§aAxe§r";
            case "mace" -> "§bMace§r";
            case "nethpot" -> "§7NethOp§r";
            case "smp" -> "§2SMP§r";
            case "sword" -> "§bSword§r";
            case "uhc" -> "§cUHC§r";
            case "vanilla" -> "§dVanilla§r";
            case "bed" -> "§cBed§r";
            case "bow" -> "§8Bow§r";
            case "creeper" -> "§aCreeper§r";
            case "debuff" -> "§8Debuff§r";
            case "diavanilla" -> "§bDiaVanilla§r";
            case "diasmp" -> "§dDiaSMP§r";
            case "elytra" -> "§7Elytra§r";
            case "manhunt" -> "§cManhunt§r";
            case "cart" -> "§6Cart§r";
            case "ogv" -> "§6OG Vanilla§r";
            case "speed" -> "§3Speed§r";
            case "trident" -> "§2Trident§r";
            case "spear" -> "§5Spearmace§r";
            case "diapot" -> "§cDiaPot§r";
            case "crystal" -> "§dCrystal§r";
            case "overall" -> "§eOverall§r";
            default -> canonical;
         };

         int color = switch (canonical) {
            case "bed" -> 16711680;
            case "spear" -> 8388736;
            case "diapot" -> 16711680;
            case "overall" -> 16766720;
            default -> 16777215;
         };
         return new GameMode(canonical, title, color);
      }
   }

   public boolean isNone() {
      return this.id.equals(NONE.id);
   }

   public boolean isOverall() {
      return this.id.equals(OVERALL.id);
   }

   public Optional<String> icon() {
      Pair<String, TextColor> pair = this.iconAndColor();
      return pair != null && pair.right() != null ? Optional.of(pair.left()) : Optional.empty();
   }

   public Text asStyled(boolean withIcon) {
      Text base = Text.literal(this.title);
      return (Text)(withIcon && this.icon().isPresent() ? Text.literal(this.icon().get().toString()).append(base) : base);
   }

   public static volatile int iconStyle;
   private Pair<String, TextColor> iconAndColor() {
      int index=java.util.List.of("sword","axe","vanilla","uhc","diapot","nethpot","smp","mace","spear","diasmp","diavanilla","cart","speed","creeper","bed","ogv","rocketmace","holefight").indexOf(this.id);
      if(index<0)return null;
      int style=Math.max(0,Math.min(3,iconStyle));
      return Pair.of(new String(Character.toChars(0x10FFB6+style*18+index)),TextColor.fromRgb(0xFFFFFF));
   }
}
