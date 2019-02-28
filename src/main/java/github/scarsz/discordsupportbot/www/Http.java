package github.scarsz.discordsupportbot.www;

import github.scarsz.discordsupportbot.Application;
import net.dv8tion.jda.core.entities.User;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Session;

import java.util.UUID;

import static spark.Spark.*;

public class Http {

    private final Logger logger = LoggerFactory.getLogger(Http.class);
    private final String oauthUrl;

    public Http(String clientId, String secret) {
        oauthUrl = "https://discordapp.com/api/oauth2/authorize?client_id=" + clientId + "&redirect_uri=https%3A%2F%2Fsupport.scarsz.me%2Foauth&response_type=code&scope=identify";

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

        before("/configure", (request, response) -> {
            if (!sessionHasDiscordId(request.session())) {
                response.redirect("/configure/login");
            }
        });

        get("/configure", (request, response) -> {
            UUID target = StringUtils.isNotBlank(request.queryString())
                    ? UUID.fromString(request.queryString())
                    : request.session().attribute("configure");
            request.session().attribute("configure", target);
            System.out.println("called");
            return null;
        });

        // login
        get("/configure/login", (request, response) -> {
            if (!sessionHasDiscordId(request.session())) {
                // session does not have a discord ID associated with it, have them complete OAuth login
                response.redirect(oauthUrl.replace("%state%", String.valueOf(request.session().hashCode())));
                return "You are being teleported to the Discord OAuth page...";
            } else {
                if (getUserById(request.session()) == null) {
                    response.status(409);
                    return "409 Conflict: No user found - the Support bot is not in any guilds that you are a part of and thus can't find your user profile.";
                } else {
                    response.redirect("/configure");
                    return "You are being teleported to the configuration page...";
                }
            }
        });

        logger.info("Finished HTTP initialization");
    }

    private boolean sessionHasDiscordId(Session session) {
        String discordId = session.attribute("discordId");
        return StringUtils.isNotBlank(discordId);
    }

    private User getUserById(Session session) {
        return getUserById((String) session.attribute("discordId"));
    }

    private User getUserById(String userId) {
        return Application.get().getBot().getJda().getUserById(userId);
    }

    public void shutdown() {
        stop();

        logger.info("HTTP server stopped");
    }

}
