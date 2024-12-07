package net.simforge.fseconomy.tools.cli;

import net.simforge.commons.io.Csv;
import net.simforge.commons.io.IOHelper;
import net.simforge.commons.misc.Geo;
import net.simforge.commons.misc.Misc;
import net.simforge.fseconomy.tools.feeder.FSEFeeder;
import net.simforge.refdata.airports.Airport;
import net.simforge.refdata.airports.Airports;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class LookingForEmptyFBO {
    public static void main(String[] args) {
        final String centerIcao = "EGLL";
        final double radius = 200;
        final int runwaySizeLimit = 100;

        final Geo.Coords centerCoords = Airports.get().findByIcao(centerIcao).get().getCoords();
        final Collection<Airport> all = Airports.get().getAllAirports();
        final List<Airport> withinRadius = all.stream()
                .filter(a -> Geo.distance(centerCoords, a.getCoords()) < radius)
                .collect(Collectors.toList());
        System.out.println("Found " + withinRadius.size() + " airports");

        withinRadius.sort(Comparator.comparingInt(Airport::getRunwaySize));
        Collections.reverse(withinRadius);

        withinRadius.forEach(airport -> processAirport(airport, runwaySizeLimit));
    }

    private static void processAirport(Airport airport, int runwaySizeLimit) {
        try {
            final int runwaySize = airport.getRunwaySize();
            final int slots = runwaySize > 3500 ? 3 : (runwaySize > 1000 ? 2 : 1);
            System.out.print(airport.getIcao() + "\t" + runwaySize + "\t" + slots + " slots\t");

            File file = new File("./data/fbo-check/" + airport.getIcao());
            if (file.exists()) {
                System.out.println(IOHelper.loadFile(file));
                return;
            }

            if (runwaySize < runwaySizeLimit) {
                System.out.println("ignored");
                return;
            }

            System.out.println();
            final Csv csv = FSEFeeder.loadCsv("query=icao&search=fbo&icao=" + airport.getIcao());
            int busySlots = 0;
            for (int i = 0; i < csv.rowCount(); i++) {
                if (csv.rowWidth(i) == 0) {
                    continue;
                }
                final String owner = csv.value(i, "Name");
                if ("System".equals(owner)) {
                    continue;
                }
                final String lots = csv.value(i, "Lots");
                busySlots += Integer.parseInt(lots);
            }

            String status;
            int freeSlots = slots - busySlots;
            if (freeSlots == slots) {
                status = "FREE All, " + slots + " slots";
            } else if (freeSlots == 0) {
                status = "BUSY All";
            } else {
                status = "FREE " + freeSlots + " slots, BUSY " + busySlots + " slots";
            }
            System.out.println(status);
            file.getParentFile().mkdirs();
            IOHelper.saveFile(file, status);
        } catch (IOException e) {
            System.out.println("ERROR HAPPENED");
        }
    }
}
