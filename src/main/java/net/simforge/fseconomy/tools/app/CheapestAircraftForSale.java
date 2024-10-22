package net.simforge.fseconomy.tools.app;

import net.simforge.fseconomy.tools.feeder.FSERequests;
import net.simforge.fseconomy.tools.lib.FSEAircraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
        final List<FSEAircraft> filtered = aircrafts.stream().filter(a -> makeModel.equals(a.getMakeModel())).collect(Collectors.toList());
        final StringBuilder sb = new StringBuilder();
        filtered.sort((a1, a2) -> Float.compare(a1.getSalePrice(), a2.getSalePrice()));
        for (int i = 0; i < Math.min(3, filtered.size()); i++) {
            final FSEAircraft aircraft = filtered.get(i);
            final String info = aircraft.getSalePrice() + " (" + aircraft.getRegistration() + ")             ";
            log.info("top {} - {}", (i+1), info);
            sb.append(info);
        }

        final String msg = sb.toString();
        final String lastMsg = notifiedMakeModels.get(makeModel);
        if (Objects.equals(msg, lastMsg)) {
            return;
        }

        TrelloSender.addToQueue("[" + makeModel + "]             " + msg, null);
        notifiedMakeModels.put(makeModel, msg);
    }
}
