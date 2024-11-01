package net.simforge.fseconomy.tools.app;

import net.simforge.fseconomy.tools.feeder.FSECachedAssignments;
import net.simforge.fseconomy.tools.feeder.FSERequests;
import net.simforge.fseconomy.tools.lib.Conditions;
import net.simforge.fseconomy.tools.lib.FSEAircraft;
import net.simforge.fseconomy.tools.lib.FSEAssignment;
import net.simforge.fseconomy.tools.lib.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class VIPAssignmentsByMakeModel implements Task {
    private static final Logger log = LoggerFactory.getLogger(VIPAssignmentsByMakeModel.class);

    private final String makeModel;
    private final int maxPax;
    private final String icaoCode;

    private long lastCheck;
    private final Set<String> sentNotifications = new TreeSet<>();

    public VIPAssignmentsByMakeModel(final String makeModel,
                                     final int maxPax,
                                     final String icaoCode) {
        this.makeModel = makeModel;
        this.maxPax = maxPax;
        this.icaoCode = icaoCode;
    }

    @Override
    public void process() {
        if (!Utils.timeComes(lastCheck)) {
            return;
        }

        lastCheck = System.currentTimeMillis();
        final List<FSEAircraft> aircrafts;
        try {
            aircrafts = FSERequests.loadAircraftByMakeModel(makeModel);
        } catch (IOException e) {
            log.error("Unable to retrieve aircraft for sale list", e);
            return;
        }

        final List<FSEAircraft> availableAircrafts = aircrafts.stream()
                .filter(a -> !"In Flight".equals(a.getLocation()))
                .filter(a -> Conditions.isIcaoInEUOrNear(a.getLocation()))
                .filter(a -> "Not rented.".equals(a.getRentedBy()))
                .filter(a -> !a.isNeedsRepair())
                .filter(a -> a.getFeeOwed() == 0)
                .filter(a -> (a.getRentalDry() > 0) || (a.getRentalWet() > 0))
                .collect(Collectors.toList());

        final Set<String> locations = availableAircrafts.stream()
                .map(FSEAircraft::getLocation)
                .collect(Collectors.toSet());

        final List<FSEAssignment> assignments;
        try {
            assignments = FSECachedAssignments.loadOutgoingAssignments(locations, Tools.ONE_HOUR);
        } catch (IOException e) {
            log.error("Unable to retrieve assignments list", e);
            return;
        }

        final List<FSEAssignment> suitableAssignments = assignments.stream()
                .filter(a -> a.getPay() > 15000)
                .filter(a -> a.getAmount() <= maxPax)
                .filter(a -> "passengers".equals(a.getUnitType()))
                .filter(a -> Conditions.noDigitsInIcao(a.getToIcao()))
                .sorted(Comparator.comparingInt(a -> -a.getPay()))
                .collect(Collectors.toList());

        suitableAssignments.forEach(a -> {
            final FSEAircraft aircraft = availableAircrafts.stream()
                    .filter(ac -> ac.getLocation().equals(a.getLocation()))
                    .findFirst().get();
            final String name = "[" + icaoCode + " / " + aircraft.getRegistration() + "] "
                    + a.getLocation() + "-" + a.getToIcao() + ", "
                    + (int)a.getDistance() + "nm, "
                    + "$" + Tools.formatPrice(a.getPay());
            final String description = Tools.toString(a);
            final String msg = name + " / " + description;
            log.info(msg);
            if (!sentNotifications.contains(msg)) {
                TrelloSender.addToQueue(name, description);
                sentNotifications.add(msg);
            }
        });
    }
}
