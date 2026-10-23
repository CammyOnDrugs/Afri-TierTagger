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

   public Optional<Character> icon() {
      Pair<Character, TextColor> pair = this.iconAndColor();
      return pair != null && pair.right() != null ? Optional.of((Character)pair.left()) : Optional.empty();
   }

   public Text asStyled(boolean withIcon) {
      Text base = Text.literal(this.title);
      return (Text)(withIcon && this.icon().isPresent() ? Text.literal(this.icon().get().toString()).append(base) : base);
   }

   private Pair<Character, TextColor> iconAndColor() {
      String var1 = this.id;

      return switch (var1) {
         case "axe" -> Pair.of('\ue701', TextColor.fromFormatting(Formatting.GREEN));
         case "mace" -> Pair.of('\ue702', TextColor.fromRgb(65535));
         case "nethpot" -> Pair.of('\ue703', TextColor.fromRgb(8947848));
         case "smp" -> Pair.of('\ue705', TextColor.fromRgb(5635925));
         case "sword" -> Pair.of('\ue706', TextColor.fromRgb(10812912));
         case "uhc" -> Pair.of('\ue707', TextColor.fromFormatting(Formatting.RED));
         case "vanilla" -> Pair.of('\ue708', TextColor.fromFormatting(Formatting.LIGHT_PURPLE));
         case "bed" -> Pair.of('\ue801', TextColor.fromRgb(16711680));
         case "creeper" -> Pair.of('\ue803', TextColor.fromFormatting(Formatting.GREEN));
         case "diasmp" -> Pair.of('\ue806', TextColor.fromRgb(9201291));
         case "cart" -> Pair.of('\ue809', TextColor.fromRgb(16753920));
         case "ogv" -> Pair.of('\ue810', TextColor.fromFormatting(Formatting.GOLD));
         case "speed" -> Pair.of('\ue811', TextColor.fromRgb(4434385));
         case "spear" -> Pair.of('\ue813', TextColor.fromRgb(8388736));
         case "diapot" -> Pair.of('\ue704', TextColor.fromRgb(16711680));
         default -> null;
      };
   }
}
