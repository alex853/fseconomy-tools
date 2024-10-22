package net.simforge.fseconomy.tools.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;

@Component
public class ScheduledTasks {
    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

    private final Collection<Task> tasks = Arrays.asList(new Task[] {
            new TransatlanticVIPAssignments(),
            new CheapestAircraftForSale("Cessna 404 Titan", "Cessna Citation Longitude", "Cessna Citation X")
    });

    @Scheduled(fixedRate = 60000)
    public void processTasks() {
        tasks.forEach(Task::process);
    }

    @Scheduled(fixedRate = 60000)
    public void sendTrello() {
        final TrelloSender.Message msg = TrelloSender.pollNext();
        if (msg == null) {
            return;
        }

        log.warn("Creating Trello card {}", msg);
        TrelloSender.send(msg);
    }
}
