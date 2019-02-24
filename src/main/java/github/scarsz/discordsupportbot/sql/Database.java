package github.scarsz.discordsupportbot.sql;

import github.scarsz.discordsupportbot.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    private static final File FILE = new File("support.db");

    private final Logger logger = LoggerFactory.getLogger(Application.class);
    private final Connection connection;

    public Database() throws SQLException, ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");

        // load from file to in-memory db if one exists
        if (FILE.exists()) {
            loadFromFile(FILE);
        }

        //TODO tables

        logger.info("Finished database initialization");
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
