package github.scarsz.discordsupportbot.sql;

import github.scarsz.discordsupportbot.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.*;
import java.util.UUID;

public class Database {

    private static final File FILE = new File("support.db");

    private final Logger logger = LoggerFactory.getLogger(Database.class);
    private final Connection connection;

    public Database() throws SQLException, ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");

        // load from file to in-memory db if one exists
        if (FILE.exists()) {
            loadFromFile(FILE);
        }

        //TODO tables

        Application.get().getDatabase().logger.info("Finished database initialization");
    }

    public static PreparedStatement sql(String sql, Object... parameters) throws SQLException {
        PreparedStatement statement = Application.get().getDatabase().connection.prepareStatement(sql);
        for (int i = 0; i < parameters.length; i++) statement.setObject(i + 1, parameters[i]);
        return statement;
    }

    public static <T> T get(UUID uuid, String database, String column) {
        try {
            ResultSet result = Database.sql("SELECT `" + column + "` FROM `" + database + "` WHERE `uuid` = ?", uuid).executeQuery();
            if (result.next()) {
                //noinspection unchecked
                return (T) result.getObject(column);
            } else {
                return null;
            }
        } catch (SQLException e) {
            Application.get().getDatabase().logger.error("Failed to retrieve " + column + " for " + uuid + " in " + database + ": " + e.getMessage(), e);
            return null;
        }
    }

    public static void update(UUID uuid, String database, String column, Object value) {
        try {
            Database.sql("UPDATE `" + database + "` SET " + column + " = ? WHERE `uuid` = ?", value, uuid).executeUpdate();
        } catch (SQLException e) {
            Application.get().getDatabase().logger.error("Failed to update " + column + " for " + uuid + " in " + database + ": " + e.getMessage(), e);
        }
    }

    public void loadFromFile() throws SQLException {
        loadFromFile(FILE);
    }

    public void loadFromFile(File file) throws SQLException {
        logger.info("Loading " + file + " into in-memory database...");
        long time = System.currentTimeMillis();
        connection.createStatement().executeUpdate("restore from " + file.getPath());
        time = System.currentTimeMillis() - time;
        logger.info("Finished in " + time + "ms");
    }

    public void saveToFile() throws SQLException {
        saveToFile(FILE);
    }

    public void saveToFile(File file) throws SQLException {
        logger.info("Writing database to file...");
        long time = System.currentTimeMillis();
        connection.createStatement().executeUpdate("backup to " + file.getPath());
        time = System.currentTimeMillis() - time;
        logger.info("Finished in " + time + "ms");
    }

}
