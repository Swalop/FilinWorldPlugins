package ru.filinworld.plugin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class FilinPrivates extends JavaPlugin {

    private static FilinPrivates instance;
    private File dataFile;
    private FileConfiguration dataConfig;
    private File configFile;
    private FileConfiguration config;

    public static FilinPrivates getInstance() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        loadConfigFile();
        loadDataFile();
        loadRegions();

        ClaimCommand cmd = new ClaimCommand();
        getCommand("rg").setExecutor(cmd);
        getCommand("rg").setTabCompleter(new ClaimTabCompleter());

        getServer().getPluginManager().registerEvents(new ProtectionListener(), this);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        getServer().getPluginManager().registerEvents(new RegionEnterListener(), this);
        getServer().getPluginManager().registerEvents(new ParticleListener(), this);

        getLogger().info("FilinPrivates v" + getDescription().getVersion() + " запущен!");
    }

    @Override
    public void onDisable() {
        saveRegions();
        getLogger().info("FilinPrivates выключен. Приватов сохранено: " + ClaimCommand.regionOwners.size());
    }

    public void reloadConfigValues() {
        loadConfigFile();
        getLogger().info("Конфиг перезагружен!");
    }

    // ---------- CONFIG ----------
    private void loadConfigFile() {
        configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            try {
                configFile.createNewFile();
                config = YamlConfiguration.loadConfiguration(configFile);
                config.set("settings.max-claims-per-player", 3);
                config.set("settings.max-claim-size", 50000);
                config.set("settings.default-y-min", 0);
                config.set("settings.default-y-max", 255);
                config.save(configFile);
            } catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        ClaimCommand.maxClaims = config.getInt("settings.max-claims-per-player", 3);
        ClaimCommand.maxSize = config.getInt("settings.max-claim-size", 50000);
        ClaimCommand.defaultYMin = config.getInt("settings.default-y-min", 0);
        ClaimCommand.defaultYMax = config.getInt("settings.default-y-max", 255);
    }

    // ---------- DATA ----------
    private void loadDataFile() {
        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void loadRegions() {
        if (dataConfig == null || !dataConfig.contains("regionOwners")) return;

        for (String key : dataConfig.getConfigurationSection("regionOwners").getKeys(false)) {
            ClaimCommand.regionOwners.put(key, UUID.fromString(dataConfig.getString("regionOwners." + key)));
        }
        if (dataConfig.contains("playerRegions")) {
            for (String key : dataConfig.getConfigurationSection("playerRegions").getKeys(false)) {
                ClaimCommand.playerRegions.put(UUID.fromString(key), dataConfig.getStringList("playerRegions." + key));
            }
        }
        if (dataConfig.contains("regionBounds")) {
            for (String key : dataConfig.getConfigurationSection("regionBounds").getKeys(false)) {
                List<?> raw = dataConfig.getList("regionBounds." + key);
                Object[] bounds = new Object[7];
                for (int i = 0; i < raw.size(); i++) bounds[i] = raw.get(i);
                ClaimCommand.regionBounds.put(key, bounds);
            }
        }
        if (dataConfig.contains("regionMembers")) {
            for (String key : dataConfig.getConfigurationSection("regionMembers").getKeys(false)) {
                ClaimCommand.regionMembers.put(key, dataConfig.getStringList("regionMembers." + key));
            }
        }
        if (dataConfig.contains("regionCoOwners")) {
            for (String key : dataConfig.getConfigurationSection("regionCoOwners").getKeys(false)) {
                ClaimCommand.regionCoOwners.put(key, dataConfig.getStringList("regionCoOwners." + key));
            }
        }
        getLogger().info("Загружено приватов: " + ClaimCommand.regionOwners.size());
    }

    private void saveRegions() {
        if (dataConfig == null) return;
        for (String key : dataConfig.getKeys(false)) dataConfig.set(key, null);

        for (String key : ClaimCommand.regionOwners.keySet())
            dataConfig.set("regionOwners." + key, ClaimCommand.regionOwners.get(key).toString());
        for (UUID id : ClaimCommand.playerRegions.keySet())
            dataConfig.set("playerRegions." + id.toString(), ClaimCommand.playerRegions.get(id));
        for (String key : ClaimCommand.regionBounds.keySet())
            dataConfig.set("regionBounds." + key, Arrays.asList(ClaimCommand.regionBounds.get(key)));
        for (String key : ClaimCommand.regionMembers.keySet())
            dataConfig.set("regionMembers." + key, ClaimCommand.regionMembers.get(key));
        for (String key : ClaimCommand.regionCoOwners.keySet())
            dataConfig.set("regionCoOwners." + key, ClaimCommand.regionCoOwners.get(key));

        try { dataConfig.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }
}