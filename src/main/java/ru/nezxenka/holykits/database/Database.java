package ru.nezxenka.holykits.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Database {
    private final JavaPlugin plugin;
    private Connection connection;
    private String url;
    private String user;
    private String password;
    private String storageType;

    public Database(JavaPlugin plugin) {
        this.plugin = plugin;
        this.loadConfig();
    }

    private void loadConfig() {
        File configFile = new File(this.plugin.getDataFolder(), "db.yml");
        if (!configFile.exists()) {
            this.plugin.saveResource("db.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        this.storageType = config.getString("storage-type", "mysql").toLowerCase();

        if ("mysql".equals(this.storageType)) {
            String host = config.getString("mysql.host");
            int port = config.getInt("mysql.port");
            String database = config.getString("mysql.database");
            this.user = config.getString("mysql.username");
            this.password = config.getString("mysql.password");
            boolean useSSL = config.getBoolean("mysql.useSSL");
            this.url = String.format("jdbc:mysql://%s:%d/%s?useSSL=%b", host, port, database, useSSL);
        } else {
            String filename = config.getString("sqlite.filename", "kits.db");
            File dbFile = new File(this.plugin.getDataFolder(), filename);
            this.url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            this.user = null;
            this.password = null;
        }
    }

    private boolean ensureConnection() {
        try {
            if (this.connection != null && !this.connection.isClosed() && this.connection.isValid(2)) {
                return true;
            } else {
                this.disconnect();
                return this.connect();
            }
        } catch (SQLException var2) {
            this.plugin.getLogger().log(Level.SEVERE, "Ошибка при проверке соединения", var2);
            return false;
        }
    }

    public boolean connect() {
        try {
            if ("mysql".equals(this.storageType)) {
                this.connection = DriverManager.getConnection(this.url, this.user, this.password);
            } else {
                this.connection = DriverManager.getConnection(this.url);
            }
            this.connection.setAutoCommit(true);
            this.createTables();
            this.plugin.getLogger().info("Успешное подключение к " + this.storageType.toUpperCase());
            return true;
        } catch (SQLException var2) {
            this.plugin.getLogger().log(Level.SEVERE, "Ошибка SQL при подключении к " + this.storageType.toUpperCase(), var2);
            return false;
        }
    }

    private void createTables() throws SQLException {
        Statement stmt = this.connection.createStatement();

        try {
            if ("mysql".equals(this.storageType)) {
                stmt.execute("CREATE TABLE IF NOT EXISTS kit_cooldowns (player_uuid VARCHAR(36) NOT NULL, kit_name VARCHAR(32) NOT NULL, cooldown_end BIGINT NOT NULL, PRIMARY KEY (player_uuid, kit_name))");
                stmt.execute("CREATE TABLE IF NOT EXISTS kits_status (id INT PRIMARY KEY CHECK (id = 1), disabled BOOLEAN NOT NULL DEFAULT FALSE, disabled_until BIGINT NOT NULL DEFAULT 0)");
                stmt.execute("INSERT IGNORE INTO kits_status (id, disabled, disabled_until) VALUES (1, FALSE, 0)");
            } else {
                stmt.execute("CREATE TABLE IF NOT EXISTS kit_cooldowns (player_uuid VARCHAR(36) NOT NULL, kit_name VARCHAR(32) NOT NULL, cooldown_end BIGINT NOT NULL, PRIMARY KEY (player_uuid, kit_name))");
                stmt.execute("CREATE TABLE IF NOT EXISTS kits_status (id INT PRIMARY KEY CHECK (id = 1), disabled BOOLEAN NOT NULL DEFAULT FALSE, disabled_until BIGINT NOT NULL DEFAULT 0)");
                stmt.execute("INSERT OR IGNORE INTO kits_status (id, disabled, disabled_until) VALUES (1, FALSE, 0)");
            }
        } catch (Throwable var5) {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (Throwable var4) {
                    var5.addSuppressed(var4);
                }
            }

            throw var5;
        }

        if (stmt != null) {
            stmt.close();
        }

    }

    public void toggleKits(boolean disabled) {
        if (this.ensureConnection()) {
            String sql = "UPDATE kits_status SET disabled = ?, disabled_until = 0 WHERE id = 1";

            try {
                PreparedStatement stmt = this.connection.prepareStatement(sql);

                try {
                    stmt.setBoolean(1, disabled);
                    stmt.executeUpdate();
                } catch (Throwable var7) {
                    if (stmt != null) {
                        try {
                            stmt.close();
                        } catch (Throwable var6) {
                            var7.addSuppressed(var6);
                        }
                    }

                    throw var7;
                }

                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException var8) {
                this.plugin.getLogger().log(Level.SEVERE, "Ошибка при переключении статуса наборов", var8);
            }

        }
    }

    public boolean setKitToggleTime(String timeString) {
        if (!this.ensureConnection()) {
            return false;
        } else if (timeString.equals("-1")) {
            String sql = "UPDATE kits_status SET disabled = TRUE, disabled_until = 0 WHERE id = 1";

            try {
                PreparedStatement stmt = this.connection.prepareStatement(sql);

                try {
                    stmt.executeUpdate();
                } catch (Throwable var14) {
                    if (stmt != null) {
                        try {
                            stmt.close();
                        } catch (Throwable var11) {
                            var14.addSuppressed(var11);
                        }
                    }

                    throw var14;
                }

                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException var15) {
                this.plugin.getLogger().log(Level.SEVERE, "Ошибка при установке бесконечного отключения наборов", var15);
            }

            return true;
        } else {
            long secondsToAdd = this.parseTimeString(timeString);
            if (secondsToAdd <= 0L) {
                return false;
            } else {
                long disabledUntil = Instant.now().getEpochSecond() + secondsToAdd;
                String sql = "UPDATE kits_status SET disabled = TRUE, disabled_until = ? WHERE id = 1";

                try {
                    PreparedStatement stmt = this.connection.prepareStatement(sql);

                    try {
                        stmt.setLong(1, disabledUntil);
                        stmt.executeUpdate();
                    } catch (Throwable var12) {
                        if (stmt != null) {
                            try {
                                stmt.close();
                            } catch (Throwable var10) {
                                var12.addSuppressed(var10);
                            }
                        }

                        throw var12;
                    }

                    if (stmt != null) {
                        stmt.close();
                    }

                    return true;
                } catch (SQLException var13) {
                    this.plugin.getLogger().log(Level.SEVERE, "Ошибка при установке времени отключения наборов", var13);
                    return false;
                }
            }
        }
    }

    public String getTimeKitToggled() {
        if (!this.ensureConnection()) {
            return "0 сек.";
        } else {
            long disabledUntil = this.getDisabledUntilTimestamp();
            if (disabledUntil == 0L) {
                return "∞ (навсегда)";
            } else if (disabledUntil < 0L) {
                return "0 сек.";
            } else {
                long currentTime = Instant.now().getEpochSecond();
                if (disabledUntil <= currentTime) {
                    this.toggleKits(false);
                    return "0 сек.";
                } else {
                    long remainingSeconds = disabledUntil - currentTime;
                    return this.formatDuration(remainingSeconds);
                }
            }
        }
    }

    public boolean isKitsDisabled() {
        if (!this.ensureConnection()) {
            return false;
        } else {
            try {
                String sql = "SELECT disabled, disabled_until FROM kits_status WHERE id = 1";
                PreparedStatement stmt = this.connection.prepareStatement(sql);

                boolean var13;
                label91: {
                    boolean var9;
                    label92: {
                        label93: {
                            label101: {
                                try {
                                    ResultSet rs = stmt.executeQuery();
                                    if (rs.next()) {
                                        boolean disabled = rs.getBoolean("disabled");
                                        long disabledUntil = rs.getLong("disabled_until");
                                        if (disabledUntil == 0L) {
                                            var13 = disabled;
                                            break label101;
                                        }

                                        if (disabledUntil <= 0L) {
                                            var13 = disabled;
                                            break label91;
                                        }

                                        long currentTime = Instant.now().getEpochSecond();
                                        if (currentTime >= disabledUntil) {
                                            this.toggleKits(false);
                                            var9 = false;
                                            break label93;
                                        }

                                        var9 = true;
                                        break label92;
                                    }
                                } catch (Throwable var11) {
                                    if (stmt != null) {
                                        try {
                                            stmt.close();
                                        } catch (Throwable var10) {
                                            var11.addSuppressed(var10);
                                        }
                                    }

                                    throw var11;
                                }

                                if (stmt != null) {
                                    stmt.close();
                                }

                                return false;
                            }

                            if (stmt != null) {
                                stmt.close();
                            }

                            return var13;
                        }

                        if (stmt != null) {
                            stmt.close();
                        }

                        return var9;
                    }

                    if (stmt != null) {
                        stmt.close();
                    }

                    return var9;
                }

                if (stmt != null) {
                    stmt.close();
                }

                return var13;
            } catch (SQLException var12) {
                this.plugin.getLogger().log(Level.SEVERE, "Ошибка при проверке статуса наборов", var12);
                return false;
            }
        }
    }

    private long getDisabledUntilTimestamp() {
        String sql = "SELECT disabled_until FROM kits_status WHERE id = 1";

        try {
            PreparedStatement stmt = this.connection.prepareStatement(sql);

            long var4;
            label54: {
                try {
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        var4 = rs.getLong("disabled_until");
                        break label54;
                    }
                } catch (Throwable var7) {
                    if (stmt != null) {
                        try {
                            stmt.close();
                        } catch (Throwable var6) {
                            var7.addSuppressed(var6);
                        }
                    }

                    throw var7;
                }

                if (stmt != null) {
                    stmt.close();
                }

                return -1L;
            }

            if (stmt != null) {
                stmt.close();
            }

            return var4;
        } catch (SQLException var8) {
            this.plugin.getLogger().log(Level.SEVERE, "Ошибка при получении времени отключения наборов", var8);
            return -1L;
        }
    }

    public void setCooldown(UUID playerUuid, String kitName, long cooldownEnd) {
        if (this.ensureConnection()) {
            String sql;
            if ("mysql".equals(this.storageType)) {
                sql = "INSERT INTO kit_cooldowns (player_uuid, kit_name, cooldown_end) VALUES (?,?,?) ON DUPLICATE KEY UPDATE cooldown_end = VALUES(cooldown_end)";
            } else {
                sql = "INSERT OR REPLACE INTO kit_cooldowns (player_uuid, kit_name, cooldown_end) VALUES (?,?,?)";
            }

            try {
                PreparedStatement stmt = this.connection.prepareStatement(sql);

                try {
                    stmt.setString(1, playerUuid.toString());
                    stmt.setString(2, kitName);
                    stmt.setLong(3, cooldownEnd);
                    stmt.executeUpdate();
                } catch (Throwable var10) {
                    if (stmt != null) {
                        try {
                            stmt.close();
                        } catch (Throwable var9) {
                            var10.addSuppressed(var9);
                        }
                    }

                    throw var10;
                }

                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException var11) {
                this.plugin.getLogger().log(Level.SEVERE, "Ошибка при установке кулдауна для " + playerUuid, var11);
            }

        }
    }

    public long getCooldown(UUID playerUuid, String kitName) {
        if (!this.ensureConnection()) {
            return 0L;
        } else {
            String sql = "SELECT cooldown_end FROM kit_cooldowns WHERE player_uuid = ? AND kit_name = ?";

            try {
                PreparedStatement stmt = this.connection.prepareStatement(sql);

                long var6;
                label59: {
                    try {
                        stmt.setString(1, playerUuid.toString());
                        stmt.setString(2, kitName);
                        ResultSet rs = stmt.executeQuery();
                        if (rs.next()) {
                            var6 = rs.getLong("cooldown_end");
                            break label59;
                        }
                    } catch (Throwable var9) {
                        if (stmt != null) {
                            try {
                                stmt.close();
                            } catch (Throwable var8) {
                                var9.addSuppressed(var8);
                            }
                        }

                        throw var9;
                    }

                    if (stmt != null) {
                        stmt.close();
                    }

                    return 0L;
                }

                if (stmt != null) {
                    stmt.close();
                }

                return var6;
            } catch (SQLException var10) {
                this.plugin.getLogger().log(Level.SEVERE, "Ошибка при получении кулдауна для " + playerUuid, var10);
                return 0L;
            }
        }
    }

    public void clearCooldown(UUID playerUuid, String kitName) {
        if (this.ensureConnection()) {
            String sql = "DELETE FROM kit_cooldowns WHERE player_uuid = ? AND kit_name = ?";

            try {
                PreparedStatement stmt = this.connection.prepareStatement(sql);

                try {
                    stmt.setString(1, playerUuid.toString());
                    stmt.setString(2, kitName);
                    stmt.executeUpdate();
                } catch (Throwable var8) {
                    if (stmt != null) {
                        try {
                            stmt.close();
                        } catch (Throwable var7) {
                            var8.addSuppressed(var7);
                        }
                    }

                    throw var8;
                }

                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException var9) {
                this.plugin.getLogger().log(Level.SEVERE, "Ошибка при очистке кулдауна для " + playerUuid, var9);
            }

        }
    }

    private long parseTimeString(String timeString) {
        if (timeString != null && !timeString.isEmpty()) {
            long totalSeconds = 0L;
            String[] parts = timeString.split(" ");
            String[] var5 = parts;
            int var6 = parts.length;

            for(int var7 = 0; var7 < var6; ++var7) {
                String part = var5[var7];
                if (part.length() >= 2) {
                    char unit = part.charAt(part.length() - 1);
                    String numberStr = part.substring(0, part.length() - 1);

                    try {
                        long value = Long.parseLong(numberStr);
                        switch(unit) {
                            case 'd':
                                totalSeconds += value * 24L * 60L * 60L;
                                break;
                            case 'h':
                                totalSeconds += value * 60L * 60L;
                                break;
                            case 'm':
                                totalSeconds += value * 60L;
                                break;
                            case 's':
                                totalSeconds += value;
                        }
                    } catch (NumberFormatException var13) {
                    }
                }
            }

            return totalSeconds;
        } else {
            return 0L;
        }
    }

    private String formatDuration(long seconds) {
        long days = seconds / 86400L;
        long hours = seconds % 86400L / 3600L;
        long minutes = seconds % 3600L / 60L;
        long secs = seconds % 60L;
        StringBuilder builder = new StringBuilder();
        if (days > 0L) {
            builder.append(days).append(" д.");
            if (hours > 0L || minutes > 0L || secs > 0L) {
                builder.append(", ");
            }
        }

        if ((hours > 0L || days > 0L && (minutes > 0L || secs > 0L)) && hours > 0L) {
            builder.append(hours).append(" ч.");
            if (minutes > 0L || secs > 0L) {
                builder.append(", ");
            }
        }

        if ((minutes > 0L || (days > 0L || hours > 0L) && secs > 0L) && minutes > 0L) {
            builder.append(minutes).append(" мин.");
            if (secs > 0L) {
                builder.append(", ");
            }
        }

        if (secs > 0L || days == 0L && hours == 0L && minutes == 0L) {
            builder.append(secs).append(" сек.");
        }

        String result = builder.toString();
        if (result.endsWith(", ")) {
            result = result.substring(0, result.length() - 2);
        }

        return result;
    }

    public void disconnect() {
        try {
            if (this.connection != null && !this.connection.isClosed()) {
                this.connection.close();
                this.plugin.getLogger().info("Отключено от " + this.storageType.toUpperCase() + " базы данных");
            }
        } catch (SQLException var2) {
            this.plugin.getLogger().log(Level.SEVERE, "Ошибка при отключении от базы данных", var2);
        }

    }

    public boolean isConnected() {
        try {
            return this.connection != null && !this.connection.isClosed();
        } catch (SQLException var2) {
            return false;
        }
    }

    public String getStorageType() {
        return this.storageType;
    }
}