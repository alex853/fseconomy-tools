package net.simforge.fseconomy.tools.app;

import net.simforge.fseconomy.tools.feeder.FSERequests;
import net.simforge.fseconomy.tools.lib.FSEAircraft;
import net.simforge.fseconomy.tools.lib.Tools;
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
        final List<FSEAircraft> filtered = aircrafts.stream()
                .filter(a -> makeModel.equals(a.getMakeModel()))
                .sorted((a1, a2) -> {
                    final int saleComparison = Float.compare(a1.getSalePrice(), a2.getSalePrice());
                    if (saleComparison == 0) {
                        return a1.getRegistration().compareTo(a2.getRegistration());
                    } else {
                        return saleComparison;
                    }
                })
                .collect(Collectors.toList());

        float salePriceSum = 0;
        int salePriceCount = 0;
        for (int i = 1; i < Math.min(3, filtered.size()); i++) {
            final FSEAircraft aircraft = filtered.get(i);
            salePriceSum += aircraft.getSalePrice();
            salePriceCount++;
        }
        final Float avgSalePrice = salePriceCount != 0 ? salePriceSum / salePriceCount : null;

        final StringBuilder description = new StringBuilder();
        FSEAircraft cheapest = null;
        for (int i = 0; i < Math.min(3, filtered.size()); i++) {
            final FSEAircraft aircraft = filtered.get(i);
            if (cheapest == null) {
                cheapest = aircraft;
            }
            final String info = (i+1) + ") " + Tools.formatPrice(aircraft.getSalePrice()) + " - " + aircraft.getRegistration() + " at " + aircraft.getLocation();
            log.info(info);
            description.append(info).append('\n');
        }

        final String percentDiscount = cheapest != null && avgSalePrice != null
                ? "-" + (int)((1 - cheapest.getSalePrice()/avgSalePrice)*100) + "%"
                : "(no discount info)";
        final String name = "[" + makeModel + "] " + (cheapest != null
                ? Tools.formatPrice(cheapest.getSalePrice()) + " " + percentDiscount
                : "nothing");

        final String msg = description.toString();
        final String lastMsg = notifiedMakeModels.get(makeModel);
        if (Objects.equals(msg, lastMsg)) {
            return;
        }

        TrelloSender.addToQueue(name, description.toString());
        notifiedMakeModels.put(makeModel, msg);
    }

}
