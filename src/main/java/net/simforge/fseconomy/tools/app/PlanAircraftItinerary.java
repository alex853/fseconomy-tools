package net.simforge.fseconomy.tools.app;

import net.simforge.fseconomy.tools.feeder.FSERequests;
import net.simforge.fseconomy.tools.lib.FSEAircraft;
import net.simforge.fseconomy.tools.lib.FindAssignments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class PlanAircraftItinerary implements Task {
    private static final Logger log = LoggerFactory.getLogger(PlanAircraftItinerary.class);

    private final String aircraftRegistration;
    private final int maxPax;
    private final int maxLegs;
    private final int maxHours;

    private long lastCheck;
    private final int[] lastSentBestPrice;

    private static final int ferryWithinVicinityNm = 100;

    public PlanAircraftItinerary(final String aircraftRegistration,
                                 final int maxPax,
                                 final int maxLegs,
                                 final int maxHours) {
        this.aircraftRegistration = aircraftRegistration;
        this.maxPax = maxPax;
        this.maxLegs = maxLegs;
        this.maxHours = maxHours;
        this.lastSentBestPrice = new int[maxLegs+1];
        Arrays.fill(lastSentBestPrice, 1, maxLegs+1, -1);
    }

    @Override
    public void process() {
        if (!Utils.timeComes(lastCheck)) {
            return;
        }

        lastCheck = System.currentTimeMillis();
        final Optional<FSEAircraft> aircraft;
        try {
            aircraft = FSERequests.loadAircraftByRegistration(aircraftRegistration);
        } catch (IOException e) {
            log.error("error while loading aircraft info", e);
            return;
        }

        if (!aircraft.isPresent()) {
            log.warn("aircraft {} not found", aircraftRegistration);
            TrelloSender.addToQueue("[FSE] [" + aircraftRegistration + "] AIRCRAFT NOT FOUND", "");
            return;
        }

        final String aircraftLocation = aircraft.get().getLocation();
        if ("In Flight".equals(aircraftLocation)) {
            log.warn("aircraft {} is flying", aircraftRegistration);
            TrelloSender.addToQueue("[FSE] [" + aircraftRegistration + "] Aircraft is flying", "");
            return;
        }

        for (int currMaxLegs = 1; currMaxLegs <= maxLegs; currMaxLegs++) {
            final FindAssignments.Params params = FindAssignments.Params.of(aircraftLocation)
                    .withMaxPax(maxPax)
                    .withMaxLegs(currMaxLegs)
                    .withMaxWorkingHours(maxHours)
                    .withLookingInVicinityRadiusNm(ferryWithinVicinityNm)
                    .withIgnoreIcaosWithDigits(true);
            final List<FindAssignments.Assignment> assignments = FindAssignments.find(params);

            if (assignments.isEmpty() && lastSentBestPrice[currMaxLegs] != 0) {
                TrelloSender.addToQueue("[FSE] [Aircraft Itinerary] [" + aircraftRegistration + "] No assignment with " + currMaxLegs + " leg(s) found", "");
                lastSentBestPrice[currMaxLegs] = 0;
                return;
            }

            final FindAssignments.Assignment bestPriceAssignment = assignments.get(0);
            if (lastSentBestPrice[currMaxLegs] == bestPriceAssignment.getTotalPay()) {
                return;
            }

            final String name = String.format("[FSE] [Aircraft Itinerary] [%s] L%d | Found %s",
                    aircraftRegistration,
                    currMaxLegs,
                    bestPriceAssignment.toString());
            TrelloSender.addToQueue(name, "");
            lastSentBestPrice[currMaxLegs] = bestPriceAssignment.getTotalPay();
        }
    }
}
