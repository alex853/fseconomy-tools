package net.simforge.fseconomy.tools.feeder;

import net.simforge.commons.io.Csv;
import net.simforge.commons.io.IOHelper;
import net.simforge.commons.legacy.misc.Settings;
import net.simforge.commons.misc.Misc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static net.simforge.fseconomy.tools.lib.Tools.d1;

public class FSEFeeder {
    private static final Logger log = LoggerFactory.getLogger(FSEFeeder.class);

    private static final String accessKey = Settings.get("fse.feeder.key");

    private static final long minimalTimeBetweenRequests = 15000;
    private static long lastFeederRequestAt;

    public synchronized static Csv loadCsv(final String request) throws IOException {
        int attempt = 1;
        while (true) {
            if (lastFeederRequestAt > 0) {
                long timeSince = System.currentTimeMillis() - lastFeederRequestAt;
                if (timeSince < minimalTimeBetweenRequests) {
                    final long timeToWait = minimalTimeBetweenRequests - timeSince;
                    log.info("[FSE-Feeder] Waiting for " + d1.format(timeToWait / 1000.0) + " secs");
                    Misc.sleep(timeToWait);
                }
            }

            final String url = String.format("https://server.fseconomy.net/data?userkey=%s&format=csv&%s", accessKey, request);
            log.info("[FSE-Feeder] Loading URL " + url);

            lastFeederRequestAt = System.currentTimeMillis();
            final String content = IOHelper.download(url);

            if (content.contains("many requests in 60 second period")) {
                attempt++;
                if (attempt == 3) {
                    throw new RuntimeException("Too many requests in 60 second period found and it was not solved in 3 attempts");
                }
                log.info("[FSE-Feeder] Too many requests in 60 second period found, sleeping for 70 seconds and repeating...");
                Misc.sleep(70000);
            } else {
                return Csv.fromContent(content);
            }
        }
    }
}
