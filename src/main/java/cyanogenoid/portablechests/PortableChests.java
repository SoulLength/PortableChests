package cyanogenoid.portablechests;

import cyanogenoid.portablechests.listeners.BlockListener;
import cyanogenoid.portablechests.listeners.InventoryListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.InventoryType;
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
    private static NamespacedKey NESTING_KEY;

    private static final Map<String, Boolean> containersConfigMap = new HashMap<>();

    private static boolean ALLOW_STACKING;
    private static int MAX_NESTING;
    private static int SHULKER_MAX_NESTING;
    private static Enchantment REQUIRED_ENCHANTMENT;
    private static Integer REQUIRED_ENCHANTMENT_LEVEL;
    private static Boolean IGNORE_CUSTOM_NAMED;
    private static List<String> CREATE_IN_WORLDS;
    private static List<String> PLACE_IN_WORLDS;

    public static String NESTING_LIMIT_MESSAGE;
    public static String CANNOT_PLACE_MESSAGE;

    public static PortableChests instance;

    private void initSettings() {
        ALLOW_STACKING = getConfig().getBoolean("allow-stacking");
        MAX_NESTING = getConfig().getInt("max-nesting");
        SHULKER_MAX_NESTING = getConfig().getInt("shulker-max-nesting");

        NESTING_LIMIT_MESSAGE  = getConfig().getString("nesting-limit-message");

        IGNORE_CUSTOM_NAMED = getConfig().getBoolean("ignore-custom-named");

        CREATE_IN_WORLDS = getConfig().getStringList("create-in-worlds");
        PLACE_IN_WORLDS = getConfig().getStringList("place-in-worlds");
        CANNOT_PLACE_MESSAGE = getConfig().getString("cannot-place-message");

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
    }

    private void initPenalties() {
        ConfigurationSection penalties = getConfig().getConfigurationSection("penalties");
        if (penalties != null) {
            Collection<PotionEffect> effects = new ArrayList<>();
            penalties.getKeys(false).forEach((key) -> {
                PotionEffectType effectType;
                try {
                    effectType = (PotionEffectType) PotionEffectType.class.getField(key).get(null);
                } catch (Exception e) {
                    getLogger().log(Level.SEVERE, key + " penalty effect not found.");
                    return;
                }
                int level = penalties.getInt(key);
                effects.add(new PotionEffect(effectType, getConfig().getInt("penalty-duration"), level));
                getLogger().log(Level.INFO, "Penalty: " + key + " " + level);
            });
            if (!effects.isEmpty()) new PenaltyMonitor(effects).runTaskTimer(this, 0, getConfig().getInt("penalty-update"));
        } else getLogger().log(Level.INFO, "No penalties.");
    }

    private void initReqEnchantment() {
        ConfigurationSection required_enchantment = getConfig().getConfigurationSection("enchantment-required");
        if (required_enchantment != null) {
            String enchantment_name = required_enchantment.getKeys(false).iterator().next();
            try {
                REQUIRED_ENCHANTMENT = (Enchantment) Enchantment.class.getField(enchantment_name).get(null) ;
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, enchantment_name + " enchantment not found.");
                return;
            }
            REQUIRED_ENCHANTMENT_LEVEL = required_enchantment.getInt(enchantment_name);
            getLogger().log(Level.INFO, "Enchantment required: " + PlainTextComponentSerializer.plainText().serialize(REQUIRED_ENCHANTMENT.displayName(REQUIRED_ENCHANTMENT_LEVEL)));
        } else getLogger().log(Level.INFO, "No enchantment required.");
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        double configVersion = getConfig().getDouble("config-version");
        if (configVersion < 1.2)  {
            getLogger().log(Level.SEVERE, "Config file outdated! Some settings might not be loaded correctly.");
            getLogger().log(Level.SEVERE, "Remove the configuration file and restart the server to load the new version.");
        }

        initSettings();
        initPenalties();
        initReqEnchantment();

        getServer().getPluginManager().registerEvents(new BlockListener(), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(), this);

        UNIQUE_KEY = new NamespacedKey(this, "UNIQUE");
        NESTING_KEY = new NamespacedKey(this, "NESTING");

        instance = this;
    }

    @Override
    public void onDisable() { }

    public static ItemStack makePortableContainer(Inventory inventory, ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return itemStack;

        long count = Arrays.stream(inventory.getContents()).filter(Objects::nonNull).count();
        List<Component> lore = Arrays.stream(inventory.getContents())
                                  .filter(Objects::nonNull)
                                  .limit(4)
                                  .map(item -> (getItemStackDisplayName(item, false).append(Component.text(" x" + item.getAmount())).style(Style.style(NamedTextColor.GRAY))))
                                  .collect(Collectors.toList());
        if (count > lore.size()) lore.add(Component.text("and " + (count - lore.size()) + " more...", NamedTextColor.GRAY, TextDecoration.ITALIC));
        meta.lore(lore);

        String encodedInventory = encodeInventory(inventory);
        UUID uuid = ALLOW_STACKING ? UUID.nameUUIDFromBytes(encodedInventory.getBytes()) : UUID.randomUUID();
        meta.getPersistentDataContainer().set(UNIQUE_KEY, PersistentDataType.STRING, uuid.toString());
        Database.saveContent(uuid , encodedInventory);

        meta.displayName(getItemStackDisplayName(itemStack, true)
                .append(Component.text(" (" + count + (count == 1 ? " Stack)" : " Stacks)"), NamedTextColor.DARK_GRAY, TextDecoration.ITALIC)));
        setItemMetaNestingData(meta, inventory);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public static Component getItemStackDisplayName(ItemStack itemStack, boolean filterMark) {
        String displayName = PlainTextComponentSerializer.plainText().serialize(itemStack.displayName()).replaceAll("[\\[\\]]", "");
        return Component.text(filterMark ? displayName.replaceAll(" \\(\\d{1,2} (Stack|Stacks)\\)", "") : displayName).style(Style.style());
    }

    public static void removeBlockDisplayNameMark(BlockState blockState) {
        Component customNameComponent = ((Nameable) blockState).customName();
        if (customNameComponent == null) return;

        String customName = PlainTextComponentSerializer.plainText().serialize(customNameComponent).replaceAll("[\\[\\]]", "");
        String resultName = customName.replaceAll(" \\(\\d{1,2} (Stack|Stacks)\\)", "");
        ((Nameable) blockState).customName(Component.text(resultName).style(Style.style()));
        blockState.update();
    }

    public static Boolean shouldIgnoreCustomNamed(BlockState blockState) {
        return IGNORE_CUSTOM_NAMED && blockState instanceof Nameable && ((Nameable) blockState).customName() != null;
    }

    public static void setShulkerBoxNestingData(Inventory inventory, ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;
        setItemMetaNestingData(meta, inventory);
        itemStack.setItemMeta(meta);
    }

    public static void fillPortableContainer(Inventory blockInventory, ItemStack blockItemStack) throws InvalidConfigurationException {
        String encodedInventory = getItemStackContentData(blockItemStack);
        if (encodedInventory != null) blockInventory.setContents(decodeInventory(encodedInventory, blockInventory));
    }

    public static Boolean isContainer(Object object) {
        return containersConfigMap.getOrDefault(object.getClass().getSimpleName().replace("Craft",""), false);
    }
    public static Boolean isContainer(Inventory inventory) {
        return inventory.getHolder() != null && isContainer(inventory.getHolder());
    }

    public static Boolean isPortableContainer(ItemStack itemStack) {
        return getItemStackNestingData(itemStack) > -1;
    }

    public static Boolean containsPenaltyContainer(Inventory inventory) {
        return Arrays.stream(inventory.getContents())
                     .filter(Objects::nonNull)
                     .anyMatch(itemStack -> getItemStackNestingData(itemStack) > (itemStack.getType().name().contains("SHULKER_BOX") ? 0 : -1));
    }

    public static Boolean canNestItemStack(Inventory inventory, ItemStack itemStack) {
        if (inventory.getType().equals(InventoryType.SHULKER_BOX)) return getItemStackNestingData(itemStack) < SHULKER_MAX_NESTING;
        return getItemStackNestingData(itemStack) < MAX_NESTING;
    }

    public static Boolean hasRequiredEnchantment(ItemStack itemStack) {
        if (REQUIRED_ENCHANTMENT == null) return true;
        if (itemStack != null) return itemStack.getEnchantmentLevel(REQUIRED_ENCHANTMENT) >= REQUIRED_ENCHANTMENT_LEVEL;
        return false;
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
        if (item == null || item.getItemMeta() == null) return null;
        return Database.loadContent(item.getItemMeta().getPersistentDataContainer().get(UNIQUE_KEY, PersistentDataType.STRING));
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
                     .filter(Objects::nonNull)
                     .filter(PortableChests::isPortableContainer)
                     .map(PortableChests::getItemStackNestingData)
                     .max(Comparator.naturalOrder()).orElse(-1);
    }
}
