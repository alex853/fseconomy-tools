package net.simforge.fseconomy.tools.app;

import net.simforge.fseconomy.tools.feeder.FSECachedAssignments;
import net.simforge.fseconomy.tools.lib.FSEAssignment;
import net.simforge.fseconomy.tools.lib.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TransatlanticVIPAssignments implements Task {
    private static final Logger log = LoggerFactory.getLogger(TransatlanticVIPAssignments.class);

    private static final Collection<String> icaos = Arrays.asList(
            "EGLL", "LPPT", 
            "KJFK", "KLGA", "KEWR", "KOAK", "KLGB", "KMRY", 
            "PHNL", "PHTO");

    private long lastCheck;
    private final Set<String> notifiedAssignments = new TreeSet<>();

    public TransatlanticVIPAssignments() {
    }

    @Override
    public void process() {
        if (!Utils.timeComes(lastCheck)) {
            return;
        }

        lastCheck = System.currentTimeMillis();
        final List<FSEAssignment> assignments;
        try {
            assignments = FSECachedAssignments.loadOutgoingAssignments(icaos, Tools.ONE_HOUR);
        } catch (IOException e) {
            log.error("Unable to retrieve assignments list", e);
            return;
        }

        final List<FSEAssignment> filtered = assignments.stream()
                .filter(a -> a.getDistance() >= 2000)
                .filter(a -> a.getPay() > 10000)
                .filter(a -> a.getAmount() <= 10)
                .filter(a -> "passengers".equals(a.getUnitType()))
                .filter(a -> !notifiedAssignments.contains(a.getId()))
                .sorted(Comparator.comparingInt(a -> -a.getPay()))
                .collect(Collectors.toList());

        filtered.forEach(a -> {
            final String assignment = Tools.toString(a);
            log.info(assignment);
            TrelloSender.addToQueue("[FSE/Transatlantic] " + assignment, null);
            notifiedAssignments.add(a.getId());
        });
    }
}
