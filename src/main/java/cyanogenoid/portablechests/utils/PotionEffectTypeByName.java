package cyanogenoid.portablechests.utils;

import org.bukkit.potion.PotionEffectType;

import java.util.Map;

public class PotionEffectTypeByName {
    private static final Map<String, PotionEffectType> potionEffectTypeMap = Map.ofEntries(
            Map.entry("speed", PotionEffectType.SPEED),
            Map.entry("slowness", PotionEffectType.SLOW),
            Map.entry("haste", PotionEffectType.FAST_DIGGING),
            Map.entry("mining_fatigue", PotionEffectType.SLOW_DIGGING),
            Map.entry("strength", PotionEffectType.INCREASE_DAMAGE),
            Map.entry("instant_health", PotionEffectType.HEAL),
            Map.entry("instant_damage", PotionEffectType.HARM),
            Map.entry("jump_boost", PotionEffectType.JUMP),
            Map.entry("nausea", PotionEffectType.CONFUSION),
            Map.entry("regeneration", PotionEffectType.REGENERATION),
            Map.entry("resistance", PotionEffectType.DAMAGE_RESISTANCE),
            Map.entry("fire_resistance", PotionEffectType.FIRE_RESISTANCE),
            Map.entry("water_breathing", PotionEffectType.WATER_BREATHING),
            Map.entry("invisibility", PotionEffectType.INVISIBILITY),
            Map.entry("blindness", PotionEffectType.BLINDNESS),
            Map.entry("night_vision", PotionEffectType.NIGHT_VISION),
            Map.entry("hunger", PotionEffectType.HUNGER),
            Map.entry("weakness", PotionEffectType.WEAKNESS),
            Map.entry("poison", PotionEffectType.POISON),
            Map.entry("wither", PotionEffectType.WITHER),
            Map.entry("health_boost", PotionEffectType.HEALTH_BOOST),
            Map.entry("absorption", PotionEffectType.ABSORPTION),
            Map.entry("saturation", PotionEffectType.SATURATION),
            Map.entry("glowing", PotionEffectType.GLOWING),
            Map.entry("levitation", PotionEffectType.LEVITATION),
            Map.entry("luck", PotionEffectType.LUCK),
            Map.entry("unluck", PotionEffectType.UNLUCK),
            Map.entry("slow_falling", PotionEffectType.SLOW_FALLING),
            Map.entry("conduit_power", PotionEffectType.CONDUIT_POWER),
            Map.entry("dolphins_grace", PotionEffectType.DOLPHINS_GRACE),
            Map.entry("bad_omen", PotionEffectType.BAD_OMEN),
            Map.entry("hero_of_the_village", PotionEffectType.HERO_OF_THE_VILLAGE),
            Map.entry("darkness", PotionEffectType.DARKNESS)
    );

    public static PotionEffectType get(String key) {
        return potionEffectTypeMap.get(key);
    }
}
