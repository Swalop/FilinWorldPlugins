package ru.filinworld.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import java.sql.*;
import java.util.*;

public class DatabaseManager {

    private Connection connection;
    private final FilinPrivates plugin;
    private final String type; // "sqlite" или "mysql"

    public DatabaseManager(FilinPrivates plugin, String type, String host, int port, String db, String user, String pass) {
        this.plugin = plugin;
        this.type = type;
        try {
            if (type.equalsIgnoreCase("mysql")) {
                connection = DriverManager.getConnection(
                        "jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=false", user, pass);
            } else {
                connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/regions.db");
            }
            createTables();
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка подключения к БД! Использую YAML.");
            e.printStackTrace();
        }
    }

    private void createTables() throws SQLException {
        Statement s = connection.createStatement();
        s.executeUpdate("CREATE TABLE IF NOT EXISTS regions (" +
                "name VARCHAR(64) PRIMARY KEY, " +
                "owner VARCHAR(36), " +
                "world VARCHAR(64), " +
                "x1 INT, z1 INT, x2 INT, z2 INT, y1 INT, y2 INT)");
        s.executeUpdate("CREATE TABLE IF NOT EXISTS members (" +
                "region VARCHAR(64), player VARCHAR(36), type VARCHAR(16), " +
                "PRIMARY KEY (region, player))");
        s.executeUpdate("CREATE TABLE IF NOT EXISTS flags (" +
                "region VARCHAR(64), flag VARCHAR(32), value INT, " +
                "PRIMARY KEY (region, flag))");
        s.close();
    }

    public void saveAll() {
        if (connection == null) return;
        try {
            Statement s = connection.createStatement();
            s.executeUpdate("DELETE FROM regions");
            s.executeUpdate("DELETE FROM members");
            s.executeUpdate("DELETE FROM flags");
            s.close();

            PreparedStatement psRegion = connection.prepareStatement(
                    "INSERT INTO regions VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
            PreparedStatement psMember = connection.prepareStatement(
                    "INSERT INTO members VALUES (?, ?, ?)");
            PreparedStatement psFlag = connection.prepareStatement(
                    "INSERT INTO flags VALUES (?, ?, ?)");

            for (String name : ClaimCommand.regionOwners.keySet()) {
                UUID owner = ClaimCommand.regionOwners.get(name);
                Object[] b = ClaimCommand.regionBounds.get(name);
                psRegion.setString(1, name);
                psRegion.setString(2, owner.toString());
                psRegion.setString(3, (String)b[6]);
                psRegion.setInt(4, (int)b[0]); psRegion.setInt(5, (int)b[1]);
                psRegion.setInt(6, (int)b[2]); psRegion.setInt(7, (int)b[3]);
                psRegion.setInt(8, (int)b[4]); psRegion.setInt(9, (int)b[5]);
                psRegion.executeUpdate();

                for (String m : ClaimCommand.regionMembers.getOrDefault(name, new ArrayList<>()))
                { psMember.setString(1, name); psMember.setString(2, m); psMember.setString(3, "member"); psMember.executeUpdate(); }
                for (String c : ClaimCommand.regionCoOwners.getOrDefault(name, new ArrayList<>()))
                { psMember.setString(1, name); psMember.setString(2, c); psMember.setString(3, "coowner"); psMember.executeUpdate(); }

                HashMap<String, Boolean> fl = ClaimCommand.regionFlags.get(name);
                if (fl != null) {
                    for (String f : fl.keySet())
                    { psFlag.setString(1, name); psFlag.setString(2, f); psFlag.setInt(3, fl.get(f) ? 1 : 0); psFlag.executeUpdate(); }
                }
            }
            psRegion.close(); psMember.close(); psFlag.close();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void loadAll() {
        if (connection == null) return;
        try {
            Statement s = connection.createStatement();
            ResultSet rs = s.executeQuery("SELECT * FROM regions");
            while (rs.next()) {
                String name = rs.getString("name");
                ClaimCommand.regionOwners.put(name, UUID.fromString(rs.getString("owner")));
                ClaimCommand.playerRegions.computeIfAbsent(UUID.fromString(rs.getString("owner")), k -> new ArrayList<>()).add(name);
                Object[] b = new Object[]{rs.getInt("x1"), rs.getInt("z1"), rs.getInt("x2"), rs.getInt("z2"),
                        rs.getInt("y1"), rs.getInt("y2"), rs.getString("world")};
                ClaimCommand.regionBounds.put(name, b);
                ClaimCommand.indexRegion(name, b);
                ClaimCommand.regionMembers.put(name, new ArrayList<>());
                ClaimCommand.regionCoOwners.put(name, new ArrayList<>());
                ClaimCommand.regionFlags.put(name, new HashMap<>());
            }
            rs = s.executeQuery("SELECT * FROM members");
            while (rs.next()) {
                String region = rs.getString("region"), player = rs.getString("player"), type = rs.getString("type");
                if (type.equals("coowner")) ClaimCommand.regionCoOwners.get(region).add(player);
                else ClaimCommand.regionMembers.get(region).add(player);
            }
            rs = s.executeQuery("SELECT * FROM flags");
            while (rs.next()) {
                ClaimCommand.regionFlags.get(rs.getString("region")).put(rs.getString("flag"), rs.getInt("value") == 1);
            }
            s.close();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void close() {
        try { if (connection != null) connection.close(); } catch (SQLException ignored) {}
    }
}