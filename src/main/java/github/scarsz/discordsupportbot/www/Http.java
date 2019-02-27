package github.scarsz.discordsupportbot.www;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static spark.Spark.*;

public class Http {

    private final Logger logger = LoggerFactory.getLogger(Http.class);

    public Http() {
        port(80);
        staticFiles.location("/static");

        // gzip where possible
        after((request, response) -> response.header("Content-Encoding", "gzip"));

        // logging
        afterAfter((request, response) -> {
            String ip = request.ip();
            String method = request.requestMethod();
            String location = request.url() + (StringUtils.isNotBlank(request.queryString()) ? "?" + request.queryString() : "");
            logger.info(ip + " " + method + " " + location + " -> " + response.status());
        });

        // login
        get("/login", (request, response) -> {
            return "session: " + request.session().id();
        });

        get("/configure", (request, response) -> {
            UUID target = StringUtils.isNotBlank(request.queryString())
                    ? UUID.fromString(request.queryString())
                    : null;
            request.session().attribute("configure", target);

            return "";
        });

        logger.info("Finished HTTP initialization");
    }

    public void shutdown() {
        stop();

        logger.info("HTTP server stopped");
    }

}
