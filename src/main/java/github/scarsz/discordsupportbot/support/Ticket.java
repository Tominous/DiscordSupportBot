package github.scarsz.discordsupportbot.support;

import github.scarsz.discordsupportbot.Application;
import github.scarsz.discordsupportbot.sql.Database;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class Ticket extends ListenerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);

    private final UUID uuid;

    public Ticket(UUID uuid) {
        this.uuid = uuid;

        Application.get().getBot().getJda().addEventListener(this);
    }

    @Override
    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
        if (!event.getChannel().equals(getChannel())) return;

        //TODO
    }

    // getters/setters

    public String getAuthorId() {
        return Database.get(uuid, "tickets", "author");
    }
    public User getAuthor() {
        String id = getAuthorId();
        return id != null
                ? Application.get().getBot().getJda().getUserById(id)
                : null;
    }
    public void setAuthor(String id) {
        Database.update(uuid, "tickets", "author", id);
    }
    public void setAuthor(User user) {
        setAuthor(user.getId());
    }
    public void setAuthor(Member member) {
        setAuthor(member.getUser().getId());
    }

    public String getChannelId() {
        return Database.get(uuid, "tickets", "channel");
    }
    public TextChannel getChannel() {
        String id = getChannelId();
        return id != null
                ? Application.get().getBot().getJda().getTextChannelById(id)
                : null;
    }
    public void setChannel(String id) {
        Database.update(uuid, "tickets", "channel", id);
    }
    public void setChannel(TextChannel channel) {
        setChannel(channel.getId());
    }

    public Status getStatus() {
        return Status.valueOf(Database.get(uuid, "tickets", "status"));
    }
    public void setStatus(Status status) {
        Database.update(uuid, "tickets", "status", status.name());
    }

    public Long getTime() {
        return Database.get(uuid, "tickets", "time");
    }
    public void setTime(long time) {
        Database.update(uuid, "tickets", "time", time);
    }

}
