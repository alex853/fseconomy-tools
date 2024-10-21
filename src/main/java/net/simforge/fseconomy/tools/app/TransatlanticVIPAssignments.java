package net.simforge.fseconomy.tools.app;

import net.simforge.commons.misc.Geo;
import net.simforge.fseconomy.tools.feeder.FSECachedAssignments;
import net.simforge.fseconomy.tools.lib.FSEAssignment;
import net.simforge.fseconomy.tools.lib.Tools;
import net.simforge.refdata.airports.Airport;
import net.simforge.refdata.airports.Airports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TransatlanticVIPAssignments implements Task {
    private static final Logger log = LoggerFactory.getLogger(TransatlanticVIPAssignments.class);

    private static final Collection<String> icaos = Arrays.asList("EGLL", "KJFK", "KLGA", "KEWR");

    private long lastCheck;
    private final Set<String> notifiedAssignments = new TreeSet<>();

    public TransatlanticVIPAssignments() {
    }

    @Override
    public void process() {
        if (!timeComes(lastCheck)) {
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
                .filter(a -> distance(a) >= 2000)
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

    private double distance(final FSEAssignment a) {
        final Airports airports = Airports.get();
        final Optional<Airport> fromIcao = airports.findByIcao(a.getLocation());
        final Optional<Airport> toIcao = airports.findByIcao(a.getToIcao());

        if (!fromIcao.isPresent() || !toIcao.isPresent()) {
            return 0;
        }

        return Geo.distance(fromIcao.get().getCoords(), toIcao.get().getCoords());
    }

    private boolean timeComes(long time) {
        return System.currentTimeMillis() - time >= Tools.ONE_HOUR;
    }
}
