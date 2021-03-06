package github.scarsz.discordsupportbot.discord;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.awt.*;

public class Bot extends ListenerAdapter {

    private final Logger logger = LoggerFactory.getLogger(Bot.class);
    private JDA jda;

    public Bot(String token) throws LoginException, InterruptedException {
        logger.info("Bot token is " + token);
        jda = new JDABuilder()
                .setToken(token)
                .setEnableShutdownHook(false) // we have our own shutdown hook
                .addEventListener(this)
                .build().awaitReady();

        logger.info("Finished JDA initialization");
    }

    public void shutdown() {
        if (jda != null && jda.getStatus() != JDA.Status.SHUTDOWN) {
            jda.getTextChannelById("549492346706984960").sendMessage(new EmbedBuilder().setColor(Color.RED).setTitle("Offline").build()).complete();
            jda.shutdown();
        }
    }

    public JDA getJda() {
        return jda;
    }

}
