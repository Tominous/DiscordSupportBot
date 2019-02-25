package github.scarsz.discordsupportbot.log;

import github.scarsz.discordsupportbot.Application;
import github.scarsz.discordsupportbot.util.TimeUtil;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageHistory;
import net.dv8tion.jda.core.entities.TextChannel;
import org.apache.commons.collections4.ListUtils;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

public class DiscordLoggingHandler extends Handler {

    private final Application application;
    private final List<LogRecord> queue = new LinkedList<>();

    public DiscordLoggingHandler(Application application) {
        this.application = application;
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::flush, 1, 1, TimeUnit.SECONDS);
    }

    private void purgeChannel() {
        MessageHistory history = getChannel().getHistory();
        while (true) {
            List<Message> retrieved = history.retrievePast(100).complete();
            if (retrieved.size() == 0) break;
        }

        for (List<Message> partition : ListUtils.partition(history.getRetrievedHistory(), 100)) {
            if (partition.size() == 1) {
                getChannel().deleteMessageById(partition.get(0).getId()).queue();
            } else {
                getChannel().deleteMessages(partition).queue();
            }
        }
    }

    private boolean wasReady = false;
    public boolean isReady() {
        boolean status = application.getBot() != null && application.getBot().getJda().getStatus() == JDA.Status.CONNECTED;
        if (status && !wasReady) {
            wasReady = true;
            purgeChannel();
        }
        return status;
    }

    public TextChannel getChannel() {
        return isReady() ? application.getBot().getJda().getTextChannelById("549492346706984960") : null;
    }

    private static final Date DATE = new Date();
    private static final Map<String, Function<String, String>> LOG_MAPPINGS = new HashMap<String, Function<String, String>>() {{
        put("spark", clazz -> "Spark");
        put("org.eclipse.jetty", clazz -> "Jetty");
        put("net.dv8tion.jda", clazz -> "JDA");
        put("github.scarsz.discordsupportbot", clazz -> {
            String[] split = clazz.split("\\.");
            return split[split.length - 1];
        });
    }};
    public String format(LogRecord record) {
        String symbol = record.getLevel() == Level.INFO
                ? "|"
                : record.getLevel() == Level.WARNING
                        ? "+"
                        : record.getLevel() == Level.SEVERE
                                ? "-"
                                : "!";

        DATE.setTime(record.getMillis());

        Function<String, String> function = LOG_MAPPINGS.entrySet().stream()
                .filter(entry -> record.getSourceClassName().startsWith(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
        String loggerName = function != null ? function.apply(record.getSourceClassName()) : record.getSourceClassName();

        return symbol + " [" + TimeUtil.timestamp(record.getMillis()) + "] " + loggerName + " > " + record.getMessage();
    }

    public String build(List<LogRecord> records) {
        if (records.size() == 0) return "```\n```";

        Level level = records.get(0).getLevel();
        String language = level == Level.INFO ? "yaml" : "diff";
        return "```" + language + "\n" +
                records.stream()
                        .map(this::format)
                        .collect(Collectors.joining("\n")) +
                "\n```" + (level == Level.SEVERE ? "<@95088531931672576>" : "");
    }

    @Override
    public void publish(LogRecord record) {
        queue.add(record);
    }

    @Override
    public void flush() {
        if (isReady()) {
            List<LogRecord> records = new LinkedList<>(queue);
            queue.clear();

            Level lastLevel = null;
            List<LogRecord> recordsInGroup = new LinkedList<>();
            for (LogRecord record : records) {
                if (lastLevel == null) {
                    lastLevel = record.getLevel();
                }

                if (lastLevel == record.getLevel()) {
                    recordsInGroup.add(record);
                } else {
                    getChannel().sendMessage(build(recordsInGroup)).queue();
                    recordsInGroup.clear();
                    recordsInGroup.add(record);
                    lastLevel = record.getLevel();
                }
            }

            if (recordsInGroup.size() > 0) {
                getChannel().sendMessage(build(recordsInGroup)).queue();
            }
        }
    }

    @Override
    public void close() throws SecurityException {}

}
