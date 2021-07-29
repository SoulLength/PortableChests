package cyanogenoid.portablechests;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

public class Database {
    private static final String configFilePath = PortableChests.instance.getDataFolder() + File.separator + "inventories.yml";

    public static void saveContent(UUID uuid, String content) {
        try {
            File file = new File(configFilePath);
            file.createNewFile();
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            config.set(uuid.toString(), content);
            config.save(file);
        } catch (IOException e) {
            PortableChests.instance.getLogger().log(Level.SEVERE, e.getMessage());
        }
    }

    public static String loadContent(String uuid) {
        File file = new File(configFilePath);
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        return config.getString(uuid);
    }
}
