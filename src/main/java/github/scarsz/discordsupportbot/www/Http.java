package github.scarsz.discordsupportbot.www;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.*;

public class Http {

    private final Logger logger = LoggerFactory.getLogger(Http.class);

    public Http() {
        //TODO

        port(80);
        get("/hello", (request, response) -> "Hello world");

        logger.info("Finished HTTP initialization");
    }

    public void shutdown() {
        stop();

        logger.info("HTTP server stopped");
    }

}
