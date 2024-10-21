package net.simforge.fseconomy.tools.lib;

import net.simforge.commons.io.Csv;
import net.simforge.commons.misc.Geo;
import net.simforge.commons.misc.Str;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

public class Tools {
    public static final DecimalFormat d1 = new DecimalFormat("0.0");

    public static final long ONE_HOUR = 60*60*1000L;
    public static final long SIX_HOURS = 6 * ONE_HOUR;
    public static final long ONE_DAY = 24 * ONE_HOUR;

    private static final Map<String, Geo.Coords> airports = new HashMap<>();
    private static final Set<String> majorAirports = new TreeSet<>();

    static {
        Csv airportsCsv;
        try {
            airportsCsv = Csv.load(new File("./data/icaodata.csv"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (int i = 0; i < airportsCsv.rowCount(); i++) {
            String _icao = airportsCsv.value(i, 0);
            String latStr = airportsCsv.value(i, 1);
            String lonStr = airportsCsv.value(i, 2);
            String type = airportsCsv.value(i, 3);
            String sizeStr = airportsCsv.value(i, 4);

            airports.put(_icao, new Geo.Coords(Double.parseDouble(latStr), Double.parseDouble(lonStr)));

            if ("civil".equals(type)) {
                if (Integer.parseInt(sizeStr) >= 3000) {
                    majorAirports.add(_icao);
                }
            }

        }
    }

    public static List<String> icaoInRadius(String icao, double radiusNm) {
        Geo.Coords centerCoords = airports.get(icao);
        List<String> icaoInRadius = new ArrayList<>();
        for (Map.Entry<String, Geo.Coords> airportEntry : airports.entrySet()) {
            Geo.Coords eachCoords = airportEntry.getValue();
            if (Geo.distance(centerCoords, eachCoords) < radiusNm) {
                icaoInRadius.add(airportEntry.getKey());
            }
        }

        return icaoInRadius;
    }

    public static Set<String> getMajorAirports() {
        return Collections.unmodifiableSet(majorAirports);
    }

    public static void print(FSEAssignment assignment) {
        System.out.println(toString(assignment));
    }

    public static String toString(FSEAssignment assignment) {
        Geo.Coords locationCoords = airports.get(assignment.getLocation());
        Geo.Coords toIcaoCoords = airports.get(assignment.getToIcao());
        int dist = -1;
        int payPerDist = -1;
        int bearing = -1;
        if (locationCoords != null && toIcaoCoords != null) {
            dist = (int) Geo.distance(locationCoords, toIcaoCoords);
            payPerDist = assignment.getPay() / (dist != 0 ? dist : 1);
            bearing = (int) Geo.bearing(locationCoords, toIcaoCoords);
        }

        return Str.ar(String.valueOf(assignment.getPay()), 10) + "\t"
                + Str.ar(String.valueOf(payPerDist), 6) + "\t"
                + Str.al(assignment.getLocation(), 4) + "\t"
                + Str.al(assignment.getToIcao(), 4) + "\t"
                + Str.z(bearing, 3) + "\t"
                + Str.ar(String.valueOf(dist), 6) + "\t"
                + Str.ar(String.valueOf(assignment.getAmount()), 6) + "\t"
                + assignment.getCommodity();
    }
}
