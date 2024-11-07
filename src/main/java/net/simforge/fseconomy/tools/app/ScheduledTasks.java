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

    private final Collection<Task> tasks = Arrays.asList(
            new LongHaulVIPAssignments(),
            new CheapestAircraftForSale(
                    "Cessna 404 Titan",
                    "Cessna Citation CJ4 (MSFS)",
                    "Cessna Citation Longitude",
                    "Cessna Citation X"),
            new VIPAssignmentsByMakeModel("Cessna Citation CJ4 (MSFS)", 10, "C25C"),
            new VIPAssignmentsByMakeModel("Cessna Citation Longitude", 8, "C700"),
            new PlanAircraftItinerary("G-SDRY", 10, 4, 10)
    );

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
