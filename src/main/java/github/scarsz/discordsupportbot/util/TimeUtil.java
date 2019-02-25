package github.scarsz.discordsupportbot.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeUtil {

    private static Date date = new Date();
    private static SimpleDateFormat format = new SimpleDateFormat("EEE, d. MMM yyyy HH:mm:ss z");

    public static String timestamp() {
        return timestamp(System.currentTimeMillis());
    }

    public static String timestamp(long millis) {
        date.setTime(millis);
        return format.format(date);
    }

}

