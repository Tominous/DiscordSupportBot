package github.scarsz.discordsupportbot;

import com.github.kevinsawicki.http.HttpRequest;
import github.scarsz.discordsupportbot.discord.Bot;
import github.scarsz.discordsupportbot.log.DiscordLoggingHandler;
import github.scarsz.discordsupportbot.sql.Database;
import github.scarsz.discordsupportbot.www.Http;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

public class Application extends Thread {

    private static Application instance = null;

    private final Logger logger = LoggerFactory.getLogger(Application.class);
    private Database database;
    private Bot bot;
    private Http http;

    public Application(String token) {
        java.util.logging.Logger.getLogger("").addHandler(new DiscordLoggingHandler(this));

        logger.info("Initializing support bot application");

        // if a previous instance exists already, shut it down and wait for completion
        if (instance != null) {
            logger.info("Previous application instance found, shutting it down...");
            instance.start();
            try {
                instance.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            logger.info("Finished shutting previous instance down");
        }
        instance = this;
        logger.info("Instance set");

        try {
            Runtime.getRuntime().addShutdownHook(this);
            logger.info("Added runtime shutdown hook");
        } catch (Exception e) {
            logger.error("Failed to add shutdown hook, not starting");
            System.exit(2);
        }

        try {
            database = new Database();
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Failed to initialize database");
            e.printStackTrace();
            System.exit(3);
        }

        try {
            bot = new Bot(token);
        } catch (Exception e) {
            logger.error("Failed to connect to Discord");
            e.printStackTrace();
            System.exit(4);
        }

        try {
            http = new Http();
        } catch (Exception e) {
            logger.error("Failed to initialize HTTP server");
            e.printStackTrace();
            System.exit(5);
        }

        logger.info("Completely finished initialization");

        logger.info("Bot is in " + bot.getJda().getGuilds().size() + " guilds");
    }

    /**
     * This method runs <strong>only</strong> as a result of the JVM starting shutdown hooks.
     */
    @Override
    public void run() {
        if (database != null) {
            try {
                database.saveToFile();
            } catch (SQLException e) {
                logger.error("Failed to save SQL database to file, not good. ):");
                e.printStackTrace();
            }
        }

        if (bot != null) {
            bot.shutdown();
        }

        if (http != null) {
            http.shutdown();
        }

        // this is sent to System.out because if it goes thru SLF4J, the message doesn't show until after the JVM dies
        System.out.println("Finished shutdown sequence");
    }

    public static Application get() {
        return instance;
    }

    public Database getDatabase() {
        return database;
    }

    public Bot getBot() {
        return bot;
    }

    public Http getHttp() {
        return http;
    }

}
