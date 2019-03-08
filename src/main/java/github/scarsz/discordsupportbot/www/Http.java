package github.scarsz.discordsupportbot.www;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import github.scarsz.discordsupportbot.Application;
import net.dv8tion.jda.core.entities.User;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.QueryParamsMap;
import spark.Session;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static spark.Spark.*;

public class Http {

    private final Gson gson = new Gson();
    private final Logger logger = LoggerFactory.getLogger(Http.class);
    private final String oauthUrl;

    public Http(String clientId, String secret) {
        oauthUrl = "https://discordapp.com/api/oauth2/authorize?client_id=" + clientId + "&redirect_uri=https%3A%2F%2Fsupport.scarsz.me%2Foauth&response_type=code&scope=identify&state=%state%";

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

        get("/configure", (request, response) -> {
            Session session = request.session();

            if (StringUtils.isNotBlank(request.queryString()) && request.queryString().split("-").length == 5) {
                UUID uuid = UUID.fromString(request.queryString());
                session.attribute("configure", uuid);
            }
            UUID target = session.attribute("configure");

            if (target == null) {
                response.status(400);
                return "No helpdesk UUID given to configure. Try @mentioning the bot in a guild that you have administrator permission in.";
            }

            // log users in if necessary
            if (!sessionIsLoggedIn(session)) {
                response.redirect("/configure/login");
                return null;
            }

            logger.info(session.attribute("discord.name") + "#" + session.attribute("discord.discriminator") + " is configuring helpdesk " + target);
            return session.attribute("discord.name") + "#" + session.attribute("discord.discriminator") + " is configuring helpdesk " + target;
        });

        // login
        get("/configure/login", (request, response) -> {
            if (!sessionIsLoggedIn(request.session())) {
                // session does not have a discord ID associated with it, have them complete OAuth login
                String state = String.valueOf(request.session().hashCode());
                response.redirect(oauthUrl.replace("%state%", state));
                request.session().attribute("oauth.state", state);
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

        get("/oauth", (request, response) -> {
            Session session = request.session();
            QueryParamsMap query = request.queryMap();

            String code = query.value("code");
            if (StringUtils.isBlank(code) || code.length() != 30) {
                response.status(400);
                return "Invalid OAuth code";
            }
            session.attribute("oauth.code", code);

            String state = query.value("state");
            String expected = session.attribute("oauth.state");
            if (!state.equals(expected)) {
                response.status(403);
                return "Invalid state";
            }

            HttpRequest authRequest = HttpRequest.post("https://discordapp.com/api/oauth2/token")
                    .userAgent("DiscordSupportBot (\"support.scarsz.me, 1.0) OAuth")
                    .form(new HashMap<String, String>() {{
                        put("client_id", clientId);
                        put("client_secret", secret);
                        put("grant_type", "authorization_code");
                        put("code", code);
                        put("redirect_uri", "https://support.scarsz.me/oauth");
                        put("scope", "identify");
                    }});
            if (authRequest.code() != 200) {
                response.status(502);
                return "Discord returned code " + authRequest.code() + " for OAuth flow: " + authRequest.body();
            }
            Map<String, Object> json = gson.fromJson(authRequest.body(), new TypeToken<Map<String, Object>>(){}.getType());
            System.out.println(json);
            session.attribute("oauth.token", json.get("access_token"));
            session.attribute("oauth.refresh", json.get("refresh_token")); // not implemented
            session.attribute("oauth.expires", json.get("expires_in")); // not implemented

            HttpRequest userRequest = HttpRequest.get("https://discordapp.com/api/users/@me")
                    .userAgent("DiscordSupportBot (\"support.scarsz.me, 1.0) OAuth")
                    .authorization("Bearer " + session.attribute("oauth.token"));
            if (authRequest.code() != 200) {
                response.status(502);
                return "Discord returned code " + authRequest.code() + " for token grant: " + authRequest.body();
            }
            json = gson.fromJson(userRequest.body(), new TypeToken<Map<String, Object>>(){}.getType());
            System.out.println(json);
            session.attribute("discord.id", json.get("id"));
            session.attribute("discord.name", json.get("username"));
            session.attribute("discord.discriminator", json.get("discriminator"));
            session.attribute("discord.avatar", "https://cdn.discordapp.com/avatars/" + json.get("id") + "/" + json.get("avatar") + ".png");
            session.attribute("discord.avatarId", json.get("avatar"));

            response.type("text/html");
            return "<html><head><meta http-equiv=\"refresh\" content=\"3;url=http://support.scarsz.me/configure\" /></head><body>Hello " + session.attribute("discord.name") + "! Redirecting...</body></html>";
        });

        get("/logout", (request, response) -> {
            Session session = request.session(false);
            if (session != null) {
                response.removeCookie("JSESSIONID");
                response.status(200);
                response.type("text/html");
                return "<html><head><meta http-equiv=\"refresh\" content=\"3;url=http://support.scarsz.me/\" /></head><body>You have been successfully logged out.</body></html>";
            } else {
                response.status(400);
                return "<html><head><meta http-equiv=\"refresh\" content=\"3;url=http://support.scarsz.me/\" /></head><body>You were already logged out.</body></html>";
            }
        });

        logger.info("Finished HTTP initialization");
    }

    private boolean sessionIsLoggedIn(Session session) {
        String discordId = session.attribute("discord.id");
        return StringUtils.isNumeric(discordId);
    }

    private User getUserById(Session session) {
        return getUserById((String) session.attribute("discord.id"));
    }

    private User getUserById(String userId) {
        return Application.get().getBot().getJda().getUserById(userId);
    }

    public void shutdown() {
        stop();

        logger.info("HTTP server stopped");
    }

}
