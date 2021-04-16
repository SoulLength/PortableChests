package cyanogenoid.portablechests;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.Comparator;
import java.util.stream.IntStream;

public final class PortableChests extends JavaPlugin {
    private static NamespacedKey UNIQUE_KEY;
    private static NamespacedKey CONTENT_KEY;
    private static NamespacedKey NESTING_KEY;

    private static boolean ALLOW_STACKING;
    private static int MAX_NESTING;
    private static String NESTING_LIMIT_MESSAGE;

    public static Map<String, Boolean> containersConfigMap = new HashMap<>();

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        ALLOW_STACKING = this.getConfig().getBoolean("allow-stacking");
        MAX_NESTING = this.getConfig().getInt("max-nesting");
        NESTING_LIMIT_MESSAGE  = this.getConfig().getString("nesting-limit-message");

        UNIQUE_KEY = new NamespacedKey(this, "UNIQUE");
        CONTENT_KEY = new NamespacedKey(this, "CONTENT");
        NESTING_KEY = new NamespacedKey(this, "NESTING");

        containersConfigMap.put("Barrel", this.getConfig().getBoolean("portable-barrels"));
        containersConfigMap.put("BlastFurnace", this.getConfig().getBoolean("portable-blast-furnaces"));
        containersConfigMap.put("BrewingStand", this.getConfig().getBoolean("portable-brewing-stands"));
        containersConfigMap.put("Chest", this.getConfig().getBoolean("portable-chests"));
        containersConfigMap.put("Dispenser", this.getConfig().getBoolean("portable-dispensers"));
        containersConfigMap.put("Dropper", this.getConfig().getBoolean("portable-droppers"));
        containersConfigMap.put("FurnaceFurnace", this.getConfig().getBoolean("portable-furnaces"));
        containersConfigMap.put("Hopper", this.getConfig().getBoolean("portable-hoppers"));
        containersConfigMap.put("Smoker", this.getConfig().getBoolean("portable-smokers"));

        containersConfigMap.put("DoubleChest", this.getConfig().getBoolean("portable-chests"));
        containersConfigMap.put("ShulkerBox", false);

        getServer().getPluginManager().registerEvents(new EventsListener(), this);
    }

    @Override
    public void onDisable() { }

    public static ItemStack makePortableContainer(Inventory inventory, ItemStack originalItemStack) {
        ItemStack inventoryItemStack = new ItemStack(originalItemStack);
        ItemMeta meta = inventoryItemStack.getItemMeta();
        if (!ALLOW_STACKING) meta.getPersistentDataContainer().set(UNIQUE_KEY, PersistentDataType.STRING, UUID.randomUUID().toString());
        meta.getPersistentDataContainer().set(CONTENT_KEY, PersistentDataType.STRING, encodeInventory(inventory));
        meta.getPersistentDataContainer().set(NESTING_KEY, PersistentDataType.INTEGER, getTotalContainerNesting(inventory) + 1);
        meta.setLore(Collections.singletonList(ChatColor.translateAlternateColorCodes('&',"&a"+Arrays.stream(inventory.getContents()).filter(Objects::nonNull).count() + " stacks inside")));
        inventoryItemStack.setItemMeta(meta);
        return inventoryItemStack;
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

    public static Boolean isPortableContainer(ItemStack item) {
        return getItemStackContentData(item) != null;
    }

    public static Boolean canNestItemStack(ItemStack itemStack) { return getItemStackNestingData(itemStack) < MAX_NESTING; }
    public static Boolean canNestItemStack(ItemStack itemStack, HumanEntity player) {
        if (canNestItemStack(itemStack)) return true;
        if (!NESTING_LIMIT_MESSAGE.isEmpty()) player.sendMessage(NESTING_LIMIT_MESSAGE);
        return false;
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

    private static Integer getTotalContainerNesting(Inventory inventory) {
        return Arrays.stream(inventory.getContents()).filter(PortableChests::isPortableContainer).map(PortableChests::getItemStackNestingData).max(Comparator.naturalOrder()).orElse(-1);
    }
}
