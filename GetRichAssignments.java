package net.simforge.fse;

import net.simforge.commons.io.Csv;
import net.simforge.commons.io.IOHelper;
import net.simforge.commons.misc.Geo;
import net.simforge.commons.misc.Misc;
import net.simforge.commons.misc.Str;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Deprecated
public class GetRichAssignments {
    public static void main(String[] args) throws IOException {
        String centerIcao = "WSSS";
        double centerRadius = 200;
//        String centerIcao = "CYFB";
//        double centerRadius = 500;

        Csv airportsCsv = Csv.load(new File("./fseconomy/data/icaodata.csv"));
        Map<String, Geo.Coords> airports = new HashMap<String, Geo.Coords>();
        for (int i = 0; i < airportsCsv.rowCount(); i++) {
            String icao = airportsCsv.value(i, 0);
            String latStr = airportsCsv.value(i, 1);
            String lonStr = airportsCsv.value(i, 2);
            airports.put(icao, new Geo.Coords(Double.parseDouble(latStr), Double.parseDouble(lonStr)));
        }

        Geo.Coords centerCoords = airports.get(centerIcao);
        List<String> icaoInRadius = new ArrayList<String>();
        for (Map.Entry<String, Geo.Coords> airportEntry : airports.entrySet()) {
            Geo.Coords eachCoords = airportEntry.getValue();
            if (Geo.distance(centerCoords, eachCoords) < centerRadius) {
                icaoInRadius.add(airportEntry.getKey());
            }
        }

        System.out.println("Found " + icaoInRadius.size() + " ICAO(s)");
        System.out.println("IcaoInRadius: " + icaoInRadius);

        List<String> downloadedData = new ArrayList<String>();
        while (!icaoInRadius.isEmpty()) {
            String url = "http://www.fseconomy.net:81/data?userkey=XCHOXGAKWC&format=csv&query=icao&search=jobsfrom&icaos=";
            boolean firstIcao = true;
            while (!icaoInRadius.isEmpty() && url.length() < 500) {
                if (firstIcao) {
                    firstIcao = false;
                } else {
                    url = url + "-";
                }
                url = url + icaoInRadius.remove(0);
            }

            System.out.println("URL " + url);
            String content = IOHelper.download(url);
            downloadedData.add(content);
            System.out.println("Remained " + icaoInRadius.size() + " ICAO(s)");
            Misc.sleep(15000);
        }

        System.out.println("Downloaded parts: " + downloadedData.size());

        for (String content : downloadedData) {
            Csv csv = Csv.fromContent(content);
            for (int i = 0; i < csv.rowCount(); i++) {
                if (csv.rowWidth(i) < 7) {
                    continue;
                }

                String location = csv.value(i, 1);
                String toIcao = csv.value(i, 2);
                String payStr = csv.value(i, 7);
                int pay = (int) Double.parseDouble(payStr);

                Geo.Coords locationCoords = airports.get(location);
                Geo.Coords toIcaoCoords = airports.get(toIcao);
                int dist = -1;
                int payPerDist = -1;
                if (locationCoords != null && toIcaoCoords != null) {
                    dist = (int) Geo.distance(locationCoords, toIcaoCoords);
                    payPerDist = pay / dist;
                }

                if (pay > 10000) {
                    System.out.println(Str.ar(String.valueOf(pay), 10) + "\t" + Str.al(location, 4) + "\t" + Str.al(toIcao, 4) + "\t" + Str.ar(String.valueOf(dist), 6) + "\t" + Str.ar(String.valueOf(payPerDist), 6));
                }
            }
        }
    }
}
