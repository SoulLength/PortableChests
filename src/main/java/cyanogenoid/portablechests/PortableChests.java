package cyanogenoid.portablechests;

import cyanogenoid.portablechests.listeners.BlockListener;
import cyanogenoid.portablechests.listeners.InventoryListener;
import cyanogenoid.portablechests.utils.Configs;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bstats.bukkit.Metrics;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public final class PortableChests extends JavaPlugin {
    private static NamespacedKey UNIQUE_KEY;
    private static NamespacedKey NESTING_KEY;

    public static PortableChests instance;
    public static Configs configs;


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
            if (!effects.isEmpty())
                new PenaltyMonitor(effects).runTaskTimer(PortableChests.instance, 0, getConfig().getInt("penalty-update"));
        } else getLogger().log(Level.INFO, "No penalties.");
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(getResource("config.yml")), StandardCharsets.UTF_8);
        double expectedVersion = YamlConfiguration.loadConfiguration(isr).getDouble("config-version");

        double configVersion = getConfig().getDouble("config-version");
        if (configVersion < expectedVersion) {
            getLogger().log(Level.SEVERE, "Config file outdated! Some settings might not be loaded correctly.");
            getLogger().log(Level.SEVERE, "Remove the configuration file and restart the server to load the new version.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (Bukkit.getMinecraftVersion().equals("1.21.2") || Bukkit.getMinecraftVersion().equals("1.21.3")) {
            getLogger().log(Level.WARNING, "!! WARNING !!");
            getLogger().log(Level.WARNING, "!! BUNDLES CANNOT BE SUPPORTED in this server version !!");
            getLogger().log(Level.WARNING, "!! DISABLE BUNDLES CRAFTING to avoid duping and various glitches !!");
            getLogger().log(Level.WARNING, "!! or consider upgrading to 1.21.4 !!");
        }

        int bStatsID = 25302;
        new Metrics(this, bStatsID);

        getServer().getPluginManager().registerEvents(new BlockListener(), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(), this);

        UNIQUE_KEY = new NamespacedKey(this, "UNIQUE");
        NESTING_KEY = new NamespacedKey(this, "NESTING");

        instance = this;
        configs = new Configs();
        this.initPenalties();
    }

    public static ItemStack makePortableContainer(Inventory inventory, ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return itemStack;

        long count = Arrays.stream(inventory.getContents()).filter(Objects::nonNull).count();

        meta.displayName(getItemStackDisplayName(itemStack)
                .append(Component.text(" (" + count + (count == 1 ? " Stack)" : " Stacks)"), NamedTextColor.DARK_GRAY)));

        List<Component> lore = Arrays.stream(inventory.getContents())
                .filter(Objects::nonNull)
                .limit(4)
                .map(item -> (getItemStackDisplayName(item).append(Component.text(" x" + item.getAmount())).style(Style.style(NamedTextColor.GRAY))))
                .collect(Collectors.toList());
        if (count > lore.size())
            lore.add(Component.text("and " + (count - lore.size()) + " more...", NamedTextColor.GRAY, TextDecoration.ITALIC));
        meta.lore(lore);

        String encodedInventory = encodeInventory(inventory);
        UUID uuid = configs.ALLOW_STACKING ? UUID.nameUUIDFromBytes(encodedInventory.getBytes()) : UUID.randomUUID();
        meta.getPersistentDataContainer().set(UNIQUE_KEY, PersistentDataType.STRING, uuid.toString());
        Database.saveContent(uuid, encodedInventory);

        setItemMetaNestingData(meta, inventory);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public static Component getItemStackDisplayName(ItemStack itemStack) {
        TextDecoration.State italicState = itemStack.displayName().equals(new ItemStack(itemStack.getType()).displayName()) ? TextDecoration.State.FALSE : TextDecoration.State.TRUE;
        return Component.text(PlainTextComponentSerializer.plainText().serialize(itemStack.displayName()).replaceAll("[\\[\\]]", ""))
                .decoration(TextDecoration.ITALIC, italicState);
    }

    public static void removeBlockDisplayNameMark(BlockState blockState) {
        Nameable nameableBlockState = (Nameable) blockState;
        Component customNameComponent = nameableBlockState.customName();
        if (customNameComponent != null) {
            String cleanCustomName = PlainTextComponentSerializer.plainText().serialize(customNameComponent)
                    .replaceAll("[\\[\\]]", "")
                    .replaceAll(" \\(\\d{1,2} (Stack|Stacks)\\)", "");
            nameableBlockState.customName(null);
            Component originalNameComponent = new ItemStack(blockState.getType()).displayName();
            String originalName = PlainTextComponentSerializer.plainText().serialize(originalNameComponent).replaceAll("[\\[\\]]", "");
            if (!cleanCustomName.equals(originalName)) {
                nameableBlockState.customName(Component.text(cleanCustomName));
            }
            blockState.update();
        }
    }

    public static Boolean shouldIgnoreCustomNamed(BlockState blockState) {
        return configs.IGNORE_CUSTOM_NAMED && blockState instanceof Nameable && ((Nameable) blockState).customName() != null;
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
        return configs.CONTAINERS.getOrDefault(object.getClass().getSimpleName().replace("Craft", ""), false);
    }

    public static Boolean isContainer(Inventory inventory) {
        return inventory.getHolder() != null && isContainer(inventory.getHolder());
    }

    public static Boolean isPortableContainer(ItemStack itemStack) {
        return getItemStackNestingData(itemStack) > -1;
    }

    public static Boolean containsPortableContainers(List<ItemStack> itemsList) {
        if (itemsList == null || itemsList.isEmpty()) return false;
        Deque<List<ItemStack>> stack = new ArrayDeque<>();
        stack.push(itemsList);
        while (!stack.isEmpty()) {
            List<ItemStack> currentList = stack.pop();
            for (ItemStack itemStack : currentList) {
                if (itemStack == null) continue;
                Material type = itemStack.getType();
                if (type == Material.BUNDLE) {
                    ItemMeta meta = itemStack.getItemMeta();
                    if (meta instanceof BundleMeta) {
                        List<ItemStack> bundleItems = ((BundleMeta) meta).getItems();
                        if (!bundleItems.isEmpty()) {
                            stack.push(bundleItems);
                        }
                    }
                } else {
                    int nestingLevel = getItemStackNestingData(itemStack);
                    if (nestingLevel > (type.name().contains("SHULKER_BOX") ? 0 : -1)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static Boolean canNestItemStack(Inventory inventory, ItemStack itemStack) {
        if (inventory.getType().equals(InventoryType.SHULKER_BOX))
            return getItemStackNestingData(itemStack) < configs.SHULKER_MAX_NESTING;
        if (itemStack.getType().name().contains("SHULKER_BOX")) {
            return getItemStackNestingData(itemStack) - configs.SHULKER_MAX_NESTING - 1 < configs.MAX_NESTING;
        }
        return getItemStackNestingData(itemStack) < configs.MAX_NESTING;
    }

    public static Boolean hasRequiredEnchantment(ItemStack itemStack) {
        if (configs.REQUIRED_ENCHANTMENT == null) return true;
        if (itemStack != null) return itemStack.getEnchantmentLevel(configs.REQUIRED_ENCHANTMENT) >= configs.REQUIRED_ENCHANTMENT_LEVEL;
        return false;
    }

    public static Boolean canCreateInWorld(World world) {
        return configs.CREATE_IN_WORLDS.contains(world.getName());
    }

    public static boolean canPlaceInWorld(World world) {
        return configs.PLACE_IN_WORLDS.contains(world.getName());
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
        if (item == null || item.getItemMeta() == null || !item.getItemMeta().getPersistentDataContainer().has(NESTING_KEY, PersistentDataType.INTEGER))
            return -1;
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
