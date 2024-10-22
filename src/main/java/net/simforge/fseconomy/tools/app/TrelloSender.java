package net.simforge.fseconomy.tools.app;

import net.simforge.commons.legacy.misc.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TrelloSender {
    private static final Logger log = LoggerFactory.getLogger(TrelloSender.class);

    public static void send(final Message message) {
        send(message.getName(), message.getDescription());
    }

    public static void send(final String name, final String description) {
        try {
            final String url = String.format("https://api.trello.com/1/cards?idList=%s&key=%s&token=%s&name=%s&desc=%s&pos=top",
                    Settings.get("trello.idList"),
                    Settings.get("trello.apiKey"),
                    Settings.get("trello.token"),
                    URLEncoder.encode(name),
                    URLEncoder.encode(description != null ? description : ""));

            final HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Accept", "application/json");

            final int responseCode = con.getResponseCode();

            if (responseCode != 200) {
                log.error("Unable to create card {}, response code {}", name, responseCode);
            }
        } catch (final IOException e) {
            log.error("Error while creating card {} to Trello", name, e);
        }
    }

    private static Queue<Message> queue = new ConcurrentLinkedQueue<>();

    public static Message pollNext() {
        return queue.poll();
    }

    public static void addToQueue(final String name, final String description) {
        queue.add(new Message(name, description));
    }

    public static class Message {
        private final String name;
        private final String description;

        public Message(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return "Message: name '" + name + "', description '" + description + "'";
        }
    }
}
