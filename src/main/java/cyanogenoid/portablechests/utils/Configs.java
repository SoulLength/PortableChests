package cyanogenoid.portablechests.utils;

import cyanogenoid.portablechests.PortableChests;
import it.unimi.dsi.fastutil.Pair;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Configs {
    public final Map<String, Boolean> CONTAINERS = new HashMap<>();
    public final boolean ALLOW_STACKING;
    public final boolean SNEAKY_BREAK_DROPS;
    public final int MAX_NESTING;
    public final int SHULKER_MAX_NESTING;
    public final Enchantment REQUIRED_ENCHANTMENT;
    public final Integer REQUIRED_ENCHANTMENT_LEVEL;
    public final Boolean IGNORE_CUSTOM_NAMED;
    public final List<String> CREATE_IN_WORLDS;
    public final List<String> PLACE_IN_WORLDS;
    public final boolean ALLOW_BUNDLES;
    
    public final String NESTING_LIMIT_MESSAGE;
    public final String WORLD_CANNOT_PLACE_MESSAGE;
    public final String BUNDLE_CANNOT_PLACE_MESSAGE;

    private final Logger logger;
    private final FileConfiguration config;

    private Pair<Enchantment, Integer> getReqEnchantment() {
        ConfigurationSection required_enchantment = this.config.getConfigurationSection("enchantment-required");
        if (required_enchantment != null) {
            String enchantment_name = required_enchantment.getKeys(false).iterator().next();
            try {
                Pair<Enchantment, Integer> result = Pair.of((Enchantment) Enchantment.class.getField(enchantment_name).get(null), required_enchantment.getInt(enchantment_name));
                this.logger.log(Level.INFO, "Enchantment required: " + PlainTextComponentSerializer.plainText().serialize(REQUIRED_ENCHANTMENT.displayName(REQUIRED_ENCHANTMENT_LEVEL)));
                return result;
            } catch (Exception e) {
                this.logger.log(Level.SEVERE, enchantment_name + " enchantment not found.");
                return null;
            }
        }
        this.logger.log(Level.INFO, "No enchantment required.");
        return null;
    }
    
    public Configs() {
        this.logger = PortableChests.instance.getLogger();
        this.config = PortableChests.instance.getConfig();

        ALLOW_STACKING = this.config.getBoolean("allow-stacking");
        SNEAKY_BREAK_DROPS = this.config.getBoolean("sneaky-break-drops");
        MAX_NESTING = this.config.getInt("max-nesting");
        SHULKER_MAX_NESTING = this.config.getInt("shulker-max-nesting");

        NESTING_LIMIT_MESSAGE = this.config.getString("nesting-limit-message");

        IGNORE_CUSTOM_NAMED = this.config.getBoolean("ignore-custom-named");

        CREATE_IN_WORLDS = this.config.getStringList("create-in-worlds");
        PLACE_IN_WORLDS = this.config.getStringList("place-in-worlds");
        ALLOW_BUNDLES = this.config.getBoolean("allow-bundles");
        WORLD_CANNOT_PLACE_MESSAGE = this.config.getString("world-cannot-place-message");
        BUNDLE_CANNOT_PLACE_MESSAGE = this.config.getString("bundle-cannot-place-message");

        CONTAINERS.put("Barrel", this.config.getBoolean("portable-barrels"));
        CONTAINERS.put("BlastFurnace", this.config.getBoolean("portable-blast-furnaces"));
        CONTAINERS.put("BrewingStand", this.config.getBoolean("portable-brewing-stands"));
        CONTAINERS.put("Chest", this.config.getBoolean("portable-chests"));
        CONTAINERS.put("Dispenser", this.config.getBoolean("portable-dispensers"));
        CONTAINERS.put("Dropper", this.config.getBoolean("portable-droppers"));
        CONTAINERS.put("FurnaceFurnace", this.config.getBoolean("portable-furnaces"));
        CONTAINERS.put("Hopper", this.config.getBoolean("portable-hoppers"));
        CONTAINERS.put("Smoker", this.config.getBoolean("portable-smokers"));
        CONTAINERS.put("DoubleChest", this.config.getBoolean("portable-chests"));
        CONTAINERS.put("ShulkerBox", true);

        Pair<Enchantment, Integer> enchantmentConfig = this.getReqEnchantment();
        REQUIRED_ENCHANTMENT = enchantmentConfig == null ? null : enchantmentConfig.key();
        REQUIRED_ENCHANTMENT_LEVEL = enchantmentConfig == null ? null : enchantmentConfig.value();
    }
}
