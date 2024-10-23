package net.simforge.fseconomy.tools.app;

import net.simforge.fseconomy.tools.feeder.FSERequests;
import net.simforge.fseconomy.tools.lib.FSEAircraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class CheapestAircraftForSale implements Task {
    private static final Logger log = LoggerFactory.getLogger(CheapestAircraftForSale.class);

    private final String[] makeModels;

    private long lastCheck;
    private final Map<String, String> notifiedMakeModels = new HashMap<>();

    public CheapestAircraftForSale(String... makeModels) {
        this.makeModels = makeModels;
    }

    @Override
    public void process() {
        if (!Utils.timeComes(lastCheck)) {
            return;
        }

        lastCheck = System.currentTimeMillis();
        final List<FSEAircraft> aircrafts;
        try {
            aircrafts = FSERequests.loadAircraftForSale();
        } catch (IOException e) {
            log.error("Unable to retrieve aircraft for sale list", e);
            return;
        }

        Arrays.stream(makeModels).forEach(makeModel -> processMakeModel(makeModel, aircrafts));
    }

    private void processMakeModel(final String makeModel,
                                  final List<FSEAircraft> aircrafts) {
        log.info("processing aircraft for sale - {}", makeModel);
        final List<FSEAircraft> filtered = aircrafts.stream()
                .filter(a -> makeModel.equals(a.getMakeModel()))
                .sorted((a1, a2) -> Float.compare(a1.getSalePrice(), a2.getSalePrice()))
                .collect(Collectors.toList());

        final StringBuilder description = new StringBuilder();
        FSEAircraft cheapest = null;
        for (int i = 0; i < Math.min(3, filtered.size()); i++) {
            final FSEAircraft aircraft = filtered.get(i);
            if (cheapest == null) {
                cheapest = aircraft;
            }
            final String info = (i+1) + ") " + formatSalePrice(aircraft) + " - " + aircraft.getRegistration() + " at " + aircraft.getLocation();
            log.info(info);
            description.append(info).append('\n');
        }

        final String name = "[" + makeModel + "] " + (cheapest != null ? formatSalePrice(cheapest) : "nothing");

        final String msg = description.toString();
        final String lastMsg = notifiedMakeModels.get(makeModel);
        if (Objects.equals(msg, lastMsg)) {
            return;
        }

        TrelloSender.addToQueue(name, description.toString());
        notifiedMakeModels.put(makeModel, msg);
    }

    private String formatSalePrice(FSEAircraft aircraft) {
        return (int) (aircraft.getSalePrice() / 1000) + "k";
    }
}
