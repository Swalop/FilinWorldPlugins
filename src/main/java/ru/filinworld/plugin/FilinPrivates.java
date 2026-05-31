package ru.filinworld.plugin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class FilinPrivates extends JavaPlugin {

    private File dataFile;
    private FileConfiguration dataConfig;

    @Override
    public void onEnable() {
        loadData();
        loadRegions();
        getCommand("rg").setExecutor(new ClaimCommand());
        getLogger().info("FilinPrivates запущен!");
    }

    @Override
    public void onDisable() {
        saveRegions();
        getLogger().info("FilinPrivates выключен!");
    }

    private void loadData() {
        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void loadRegions() {
        if (dataConfig.contains("privates")) {
            for (String key : dataConfig.getConfigurationSection("privates").getKeys(false)) {
                UUID id = UUID.fromString(key);
                ClaimCommand.privates.put(id, dataConfig.getString("privates." + key));
            }
        }
        if (dataConfig.contains("members")) {
            for (String key : dataConfig.getConfigurationSection("members").getKeys(false)) {
                UUID id = UUID.fromString(key);
                ClaimCommand.members.put(id, dataConfig.getStringList("members." + key));
            }
        }
        getLogger().info("Загружено приватов: " + ClaimCommand.privates.size());
    }

    private void saveRegions() {
        for (UUID id : ClaimCommand.privates.keySet()) {
            dataConfig.set("privates." + id.toString(), ClaimCommand.privates.get(id));
        }
        for (UUID id : ClaimCommand.members.keySet()) {
            dataConfig.set("members." + id.toString(), ClaimCommand.members.get(id));
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}