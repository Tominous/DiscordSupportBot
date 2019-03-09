package github.scarsz.discordsupportbot.support;

import github.scarsz.discordsupportbot.support.prompt.Prompt;
import net.dv8tion.jda.core.entities.Category;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class Helpdesk {

    private final UUID uuid;
    private final Configuration config;
    private final List<Prompt> prompts = new LinkedList<>();
    private final List<Ticket> tickets = new ArrayList<>();

    public Helpdesk(UUID uuid, Configuration config, List<Ticket> tickets) {
        this.uuid = uuid;
        this.config = config;

        // TODO load prompts/tickets
    }

    public Category getCategory() {
        return config.getCategory();
    }

    public TextChannel getChannel() {
        return config.getChannel();
    }

}
