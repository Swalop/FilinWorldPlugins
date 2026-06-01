package ru.filinworld.plugin;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
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
    private DatabaseManager db;
    private LuckPerms luckPerms;
    private boolean useDatabase = false;

    public static FilinPrivates getInstance() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        loadConfigFile();

        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) luckPerms = provider.getProvider();

        String dbType = config.getString("database.type", "yaml");
        if (dbType.equalsIgnoreCase("sqlite")) {
            db = new DatabaseManager(this, "sqlite", "", 0, "", "", "");
            useDatabase = true;
        } else if (dbType.equalsIgnoreCase("mysql")) {
            db = new DatabaseManager(this, "mysql",
                    config.getString("database.host", "localhost"),
                    config.getInt("database.port", 3306),
                    config.getString("database.database", "filin"),
                    config.getString("database.user", "root"),
                    config.getString("database.password", ""));
            useDatabase = true;
        }

        if (useDatabase) {
            db.loadAll();
            getLogger().info("БД: " + dbType.toUpperCase() + ". Загружено: " + ClaimCommand.regionOwners.size());
        } else {
            loadDataFile();
            loadRegionsFromYaml();
        }

        ClaimCommand cmd = new ClaimCommand();
        getCommand("rg").setExecutor(cmd);
        getCommand("rg").setTabCompleter(new ClaimTabCompleter());

        getServer().getPluginManager().registerEvents(new ProtectionListener(), this);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        getServer().getPluginManager().registerEvents(new RegionEnterListener(), this);
        getServer().getPluginManager().registerEvents(new ParticleListener(), this);

        getServer().getScheduler().runTaskTimer(this, this::saveAll, 6000L, 6000L);

        getLogger().info("FilinPrivates v" + getDescription().getVersion() + " запущен!");
    }

    @Override
    public void onDisable() {
        saveAll();
        if (db != null) db.close();
        getLogger().info("FilinPrivates выключен.");
    }
    public void saveAll() {
        if (useDatabase) {
            db.saveAll();
        } else {
            saveRegionsToYaml();
        }
    }

    public void reloadConfigValues() {
        loadConfigFile();
        getLogger().info("Конфиг перезагружен!");
    }

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
                config.set("database.type", "yaml");
                config.set("database.host", "localhost");
                config.set("database.port", 3306);
                config.set("database.database", "filin");
                config.set("database.user", "root");
                config.set("database.password", "");
                config.set("limits.default.max-claims", 3);
                config.set("limits.default.max-size", 50000);
                config.set("limits.vip.max-claims", 5);
                config.set("limits.vip.max-size", 100000);
                config.set("limits.premium.max-claims", 7);
                config.set("limits.premium.max-size", 200000);
                config.set("limits.admin.max-claims", 20);
                config.set("limits.admin.max-size", 500000);
                config.save(configFile);
            } catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        ClaimCommand.maxClaims = config.getInt("settings.max-claims-per-player", 3);
        ClaimCommand.maxSize = config.getInt("settings.max-claim-size", 50000);
        ClaimCommand.defaultYMin = config.getInt("settings.default-y-min", 0);
        ClaimCommand.defaultYMax = config.getInt("settings.default-y-max", 255);
    }
    public int getMaxClaims(Player player) {
        if (luckPerms == null) return config.getInt("limits.default.max-claims", ClaimCommand.maxClaims);
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return config.getInt("limits.default.max-claims", ClaimCommand.maxClaims);
        String group = user.getPrimaryGroup();
        if (group == null) group = "default";
        return config.getInt("limits." + group + ".max-claims", ClaimCommand.maxClaims);
    }

    public int getMaxSize(Player player) {
        if (luckPerms == null) return config.getInt("limits.default.max-size", ClaimCommand.maxSize);
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return config.getInt("limits.default.max-size", ClaimCommand.maxSize);
        String group = user.getPrimaryGroup();
        if (group == null) group = "default";
        return config.getInt("limits." + group + ".max-size", ClaimCommand.maxSize);
    }

    private void loadDataFile() {
        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void loadRegionsFromYaml() {
        if (dataConfig == null || !dataConfig.contains("regionOwners")) return;
        for (String key : dataConfig.getConfigurationSection("regionOwners").getKeys(false))
            ClaimCommand.regionOwners.put(key, UUID.fromString(dataConfig.getString("regionOwners." + key)));
        if (dataConfig.contains("playerRegions"))
            for (String key : dataConfig.getConfigurationSection("playerRegions").getKeys(false))
                ClaimCommand.playerRegions.put(UUID.fromString(key), dataConfig.getStringList("playerRegions." + key));
        if (dataConfig.contains("regionBounds"))
            for (String key : dataConfig.getConfigurationSection("regionBounds").getKeys(false)) {
                List<?> raw = dataConfig.getList("regionBounds." + key);
                Object[] b = new Object[7];
                for (int i = 0; i < raw.size(); i++) b[i] = raw.get(i);
                ClaimCommand.regionBounds.put(key, b);
            }
        for (String rn : ClaimCommand.regionBounds.keySet())
            ClaimCommand.indexRegion(rn, ClaimCommand.regionBounds.get(rn));
        if (dataConfig.contains("regionMembers"))
            for (String key : dataConfig.getConfigurationSection("regionMembers").getKeys(false))
                ClaimCommand.regionMembers.put(key, dataConfig.getStringList("regionMembers." + key));
        if (dataConfig.contains("regionCoOwners"))
            for (String key : dataConfig.getConfigurationSection("regionCoOwners").getKeys(false))
                ClaimCommand.regionCoOwners.put(key, dataConfig.getStringList("regionCoOwners." + key));
        getLogger().info("YAML: загружено " + ClaimCommand.regionOwners.size() + " приватов.");
    }

    private void saveRegionsToYaml() {
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