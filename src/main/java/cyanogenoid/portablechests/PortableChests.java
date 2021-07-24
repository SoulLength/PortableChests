package cyanogenoid.portablechests;

import org.apache.commons.lang.WordUtils;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public final class PortableChests extends JavaPlugin {
    private static NamespacedKey UNIQUE_KEY;
    private static NamespacedKey CONTENT_KEY;
    private static NamespacedKey NESTING_KEY;

    private static final Map<String, Boolean> containersConfigMap = new HashMap<>();

    private static boolean ALLOW_STACKING;
    private static int MAX_NESTING;
    private static String REQUIRED_ENCHANTMENT;
    private static Integer REQUIRED_ENCHANTMENT_LEVEL;
    private static Boolean IGNORE_CUSTOM_NAMED;
    private static List<String> CREATE_IN_WORLDS;
    private static List<String> PLACE_IN_WORLDS;

    public static String NESTING_LIMIT_MESSAGE;
    public static String CANNOT_PLACE_MESSAGE;

    public static PortableChests instance;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        String configVersion = getConfig().getString("config-version");
        if (configVersion == null)  {
            getLogger().log(Level.SEVERE, "Config file outdated! Some settings might not be loaded correctly.");
            getLogger().log(Level.SEVERE, "Remove the configuration file and restart the server to load the new version.");
        }

        ALLOW_STACKING = getConfig().getBoolean("allow-stacking");
        MAX_NESTING = getConfig().getInt("max-nesting");
        NESTING_LIMIT_MESSAGE  = getConfig().getString("nesting-limit-message");

        IGNORE_CUSTOM_NAMED = getConfig().getBoolean("ignore-custom-named");

        CREATE_IN_WORLDS = getConfig().getStringList("create-in-worlds");
        PLACE_IN_WORLDS = getConfig().getStringList("place-in-worlds");
        CANNOT_PLACE_MESSAGE = getConfig().getString("cannot-place-message");

        UNIQUE_KEY = new NamespacedKey(this, "UNIQUE");
        CONTENT_KEY = new NamespacedKey(this, "CONTENT");
        NESTING_KEY = new NamespacedKey(this, "NESTING");

        containersConfigMap.put("Barrel", getConfig().getBoolean("portable-barrels"));
        containersConfigMap.put("BlastFurnace", getConfig().getBoolean("portable-blast-furnaces"));
        containersConfigMap.put("BrewingStand", getConfig().getBoolean("portable-brewing-stands"));
        containersConfigMap.put("Chest", getConfig().getBoolean("portable-chests"));
        containersConfigMap.put("Dispenser", getConfig().getBoolean("portable-dispensers"));
        containersConfigMap.put("Dropper", getConfig().getBoolean("portable-droppers"));
        containersConfigMap.put("FurnaceFurnace", getConfig().getBoolean("portable-furnaces"));
        containersConfigMap.put("Hopper", getConfig().getBoolean("portable-hoppers"));
        containersConfigMap.put("Smoker", getConfig().getBoolean("portable-smokers"));

        containersConfigMap.put("DoubleChest", getConfig().getBoolean("portable-chests"));
        containersConfigMap.put("ShulkerBox", true);

        ConfigurationSection penalties = getConfig().getConfigurationSection("penalties");
        if (penalties != null) {
            Collection<PotionEffect> effects = new ArrayList<>();
            penalties.getKeys(false).forEach((key) -> {
                PotionEffectType effectType = PotionEffectType.getByName(key);
                int level = penalties.getInt(key);

                if (effectType == null) getLogger().log(Level.SEVERE, key + " penalty effect not found.");
                else {
                    effects.add(new PotionEffect(effectType, getConfig().getInt("penalty-duration"), level));
                    getLogger().log(Level.INFO, "Penalty: " + key + " " + level);
                }
            });
            if (effects.size() != 0) new PenaltyMonitor(effects).runTaskTimer(this, 0, getConfig().getInt("penalty-update"));
        } else getLogger().log(Level.INFO, "No penalties.");

        ConfigurationSection required_enchantment = getConfig().getConfigurationSection("enchantment-required");
        if (required_enchantment != null) {
            REQUIRED_ENCHANTMENT = required_enchantment.getKeys(false).iterator().next();
            REQUIRED_ENCHANTMENT_LEVEL = required_enchantment.getInt(REQUIRED_ENCHANTMENT);
            getLogger().log(Level.INFO, "Enchantment required: " + REQUIRED_ENCHANTMENT + " " + REQUIRED_ENCHANTMENT_LEVEL);
        } else getLogger().log(Level.INFO, "No enchantment required.");

        getServer().getPluginManager().registerEvents(new cyanogenoid.portablechests.listeners.BlockListener(), this);
        getServer().getPluginManager().registerEvents(new cyanogenoid.portablechests.listeners.InventoryListener(), this);

        instance = this;
    }

    @Override
    public void onDisable() { }

    public static ItemStack makePortableContainer(Inventory inventory, ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return itemStack;

        long count = Arrays.stream(inventory.getContents()).filter(Objects::nonNull).count();
        List<String> lore = Arrays.stream(inventory.getContents())
                                  .filter(Objects::nonNull)
                                  .limit(4)
                                  .map(item -> ChatColor.GRAY + getItemStackDisplayName(item, false) + " x" + item.getAmount())
                                  .collect(Collectors.toList());
        if (count > lore.size()) lore.add(ChatColor.GRAY + ChatColor.ITALIC.toString() + "and " + (count - lore.size()) + " more...");
        meta.setLore(lore);

        if (!ALLOW_STACKING) meta.getPersistentDataContainer().set(UNIQUE_KEY, PersistentDataType.STRING, UUID.randomUUID().toString());
        meta.getPersistentDataContainer().set(CONTENT_KEY, PersistentDataType.STRING, encodeInventory(inventory));
        meta.setDisplayName(getItemStackDisplayName(itemStack, true) + ChatColor.ITALIC + "" + ChatColor.GOLD + " (" + count + (count == 1 ? " Stack)" : " Stacks)"));

        setItemMetaNestingData(meta, inventory);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public static String getItemStackDisplayName(ItemStack itemStack, boolean filterMark) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null && !meta.getDisplayName().equals(""))
            return ChatColor.stripColor(filterMark ? meta.getDisplayName().replaceAll(" \\(\\d{1,2} (Stack|Stacks)\\)", "")
                                                   : meta.getDisplayName());
        return WordUtils.capitalizeFully(itemStack.getType().name().replace("_", " "));
    }

    public static void removeBlockDisplayNameMark(BlockState blockState) {
        Nameable nameableBlock = (Nameable) blockState;
        if (nameableBlock.getCustomName() == null) return;

        String resultName = ChatColor.stripColor(nameableBlock.getCustomName().replaceAll(" \\(\\d{1,2} (Stack|Stacks)\\)", ""));
        nameableBlock.setCustomName(WordUtils.capitalizeFully(blockState.getType().name().replace("_", " ")).equals(resultName) ? null : resultName);

        blockState.update();
    }

    public static Boolean shouldIgnoreCustomNamed(BlockState blockState) {
        return IGNORE_CUSTOM_NAMED && blockState instanceof Nameable && ((Nameable) blockState).getCustomName() != null;
    }

    public static void setShulkerBoxNestingData(Inventory inventory, ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;
        setItemMetaNestingData(meta, inventory);
        itemStack.setItemMeta(meta);
    }

    public static void fillPortableContainer(Inventory blockInventory, ItemStack blockItemStack) throws InvalidConfigurationException {
        String encodedInventory = getItemStackContentData(blockItemStack);
        blockInventory.setContents(decodeInventory(encodedInventory, blockInventory));
    }

    public static Boolean isContainer(Object object) {
        return containersConfigMap.getOrDefault(object.getClass().getSimpleName().replace("Craft",""), false);
    }
    public static Boolean isContainer(Inventory inventory) {
        return inventory.getHolder() != null && isContainer(inventory.getHolder());
    }

    public static Boolean isPortableContainer(ItemStack itemStack) {
        if (itemStack == null) return false;
        Integer nestingData = getItemStackNestingData(itemStack);
        if (itemStack.getType().equals(Material.SHULKER_BOX)) return nestingData > 0;
        return nestingData != -1;
    }
    public static Boolean containsPortableContainer(Inventory inventory) {
        return Arrays.stream(inventory.getContents())
                     .anyMatch(PortableChests::isPortableContainer);
    }

    public static Boolean canNestItemStack(ItemStack itemStack) {
        return getItemStackNestingData(itemStack) < MAX_NESTING;
    }

    public static Boolean hasRequiredEnchantment(ItemStack itemStack) {
        if (REQUIRED_ENCHANTMENT == null) return true;
        if (itemStack == null) return false;
        Enchantment foundEnchantment = itemStack.getEnchantments()
                                                .keySet()
                                                .stream()
                                                .filter(enchantment -> enchantment.getName().equals(REQUIRED_ENCHANTMENT))
                                                .findFirst().orElse(null);
        if (foundEnchantment == null) return false;
        return itemStack.getEnchantments().get(foundEnchantment) >= REQUIRED_ENCHANTMENT_LEVEL;
    }

    public static Boolean canCreateInWorld(World world) {
        return CREATE_IN_WORLDS.contains(world.getName());
    }

    public static boolean canPlaceInWorld(World world) {
        return PLACE_IN_WORLDS.contains(world.getName());
    }



    private static String encodeInventory(Inventory inventory) {
        YamlConfiguration config = new YamlConfiguration();
        IntStream.range(0, inventory.getSize()).forEach(index -> config.set(Integer.toString(index), inventory.getContents()[index]));
        return config.saveToString();
    }

    private static ItemStack[] decodeInventory(String encodedInventory, Inventory inventory) throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(encodedInventory);

        ItemStack[] content = new ItemStack[inventory.getSize()];
        IntStream.range(0, inventory.getSize()).forEach(index -> content[index] = config.getItemStack(Integer.toString(index), null));

        return content;
    }

    private static String getItemStackContentData(ItemStack item) {
        if (item == null || item.getItemMeta() == null || !item.getItemMeta().getPersistentDataContainer().has(CONTENT_KEY, PersistentDataType.STRING)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(CONTENT_KEY, PersistentDataType.STRING);
    }

    private static Integer getItemStackNestingData(ItemStack item) {
        if (item == null || item.getItemMeta() == null || !item.getItemMeta().getPersistentDataContainer().has(NESTING_KEY, PersistentDataType.INTEGER)) return -1;
        return item.getItemMeta().getPersistentDataContainer().get(NESTING_KEY, PersistentDataType.INTEGER);
    }

    private static void setItemMetaNestingData(ItemMeta itemMeta, Inventory inventory) {
        itemMeta.getPersistentDataContainer().set(NESTING_KEY, PersistentDataType.INTEGER, getTotalContainerNesting(inventory) + 1);
    }

    private static Integer getTotalContainerNesting(Inventory inventory) {
        return Arrays.stream(inventory.getContents())
                     .filter(PortableChests::isPortableContainer)
                     .map(PortableChests::getItemStackNestingData)
                     .max(Comparator.naturalOrder()).orElse(-1);
    }
}
