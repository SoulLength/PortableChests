package cyanogenoid.portablechests;

import cyanogenoid.portablechests.utils.Constants;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

public class Database {
    private static final String dataDir = PortableChests.instance.getDataFolder() + File.separator + Constants.DB_DIR;
    private static final String invFilePath = dataDir + File.separator + Constants.INVENTORIES_FILE;

    public static void saveContent(UUID uuid, String content) {
        try {
            File dir = new File(dataDir);
            File file = new File(invFilePath);
            if (file.exists() || (dir.mkdirs() && file.createNewFile())) {
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                config.set(uuid.toString(), content);
                config.save(file);
            }
        } catch (IOException e) {
            PortableChests.instance.getLogger().log(Level.SEVERE, e.getMessage());
        }
    }

    public static String loadContent(String uuid) {
        File file = new File(invFilePath);
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        return config.getString(uuid);
    }
}
