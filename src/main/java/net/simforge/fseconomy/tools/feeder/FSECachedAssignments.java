package net.simforge.fseconomy.tools.feeder;

import net.simforge.commons.io.Csv;
import net.simforge.commons.io.IOHelper;
import net.simforge.fseconomy.tools.lib.FSEAssignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static java.util.stream.Collectors.groupingBy;

public class FSECachedAssignments {
    private static final Logger log = LoggerFactory.getLogger(FSEFeeder.class);

    public static final File ASSIGNMENTS_PER_LOCATION_STORAGE = new File("./data/assignments-per-location");

    public static List<FSEAssignment> loadOutgoingAssignments(final Collection<String> icaos,
                                                              final long refreshPeriod) throws IOException {
        final List<String> icaosToDownload = new ArrayList<>();

        icaos.forEach(icao -> {
            File file = findCachedCsv(icao, refreshPeriod);
            if (file == null) {
                icaosToDownload.add(icao);
            }
        });

        final List<String> remainedIcaos = new ArrayList<>(icaosToDownload);
        final List<String> icaosWithAssignments = new ArrayList<>();
        while (!remainedIcaos.isEmpty()) {
            log.info("[Jobs-Loader] Remained " + remainedIcaos.size() + " ICAO(s) to load");

            StringBuilder requestParams = new StringBuilder("query=icao&search=jobsfrom&icaos=");
            boolean firstIcao = true;
            while (!remainedIcaos.isEmpty() && requestParams.length() < 400) {
                if (firstIcao) {
                    firstIcao = false;
                } else {
                    requestParams.append("-");
                }
                requestParams.append(remainedIcaos.remove(0));
            }

            final Csv csv = FSEFeeder.loadCsv(requestParams.toString());

            List<FSEAssignment> assignments = parseCsv(csv);
            Map<String, List<FSEAssignment>> icao2assignment = assignments.stream()
                    .collect(groupingBy(FSEAssignment::getLocation));
            icao2assignment.forEach(FSECachedAssignments::saveToCsv);
            icaosWithAssignments.addAll(icao2assignment.keySet());
        }

        icaosToDownload.removeAll(icaosWithAssignments);
        if (!icaosToDownload.isEmpty()) {
            log.info("[Jobs-Loader] Saving empty assignments file for " + icaosToDownload);
            icaosToDownload.forEach(icao -> saveToCsv(icao, Collections.emptyList()));
        }

        List<FSEAssignment> assignments = new ArrayList<>();
        icaos.forEach(icao -> assignments.addAll(loadFromCsv(icao, refreshPeriod)));
        return assignments;
    }

    private static File findCachedCsv(String icao, long refreshPeriod) {
        final File icaoFolder = new File(ASSIGNMENTS_PER_LOCATION_STORAGE, icao);
        if (!icaoFolder.exists()) {
            return null;
        }

        final File[] files = icaoFolder.listFiles();
        if (files == null) {
            return null;
        }

        return Arrays.stream(files)
                .filter(file -> {
                    String tss = file.getName().split("\\.")[0];
                    long ts = Long.parseLong(tss);
                    long now = System.currentTimeMillis();
                    long threshold = now - refreshPeriod;
                    return ts > threshold;
                })
                .findFirst()
                .orElse(null);
    }

    private static List<FSEAssignment> parseCsv(final Csv csv) {
        List<FSEAssignment> result = new ArrayList<>();

        for (int i = 0; i < csv.rowCount(); i++) {
            if (csv.rowWidth(i) < 7) {
                continue;
            }

            String id = csv.value(i, 0);
            String location = csv.value(i, 1);
            String toIcao = csv.value(i, 2);
            String fromIcao = csv.value(i, 3);
            String amount = csv.value(i, 4);
            String unitType = csv.value(i, 5);
            String commodity = csv.value(i, 6);
            String pay = csv.value(i, 7);
            String expires = csv.value(i, 8);
            String expireDateTime = csv.value(i, 9);
            String express = csv.value(i, 10);
            String ptAssignment = csv.value(i, 11);
            String type = csv.value(i, 12);
            String aircraftId = csv.value(i, 13);

            FSEAssignment assignment = new FSEAssignment(
                    id,
                    location,
                    toIcao,
                    fromIcao,
                    amount,
                    unitType,
                    commodity,
                    pay,
                    expires,
                    expireDateTime,
                    express,
                    ptAssignment,
                    type,
                    aircraftId);
            result.add(assignment);
        }

        return result;
    }

    private static void saveToCsv(String icao, List<FSEAssignment> assignments) {
        Csv csv = Csv.empty();
        csv.addColumn("Id");
        csv.addColumn("Location");
        csv.addColumn("ToIcao");
        csv.addColumn("FromIcao");
        csv.addColumn("Amount");
        csv.addColumn("UnitType");
        csv.addColumn("Commodity");
        csv.addColumn("Pay");
        csv.addColumn("Expires");
        csv.addColumn("ExpireDateTime");
        csv.addColumn("Express");
        csv.addColumn("PtAssignment");
        csv.addColumn("Type");
        csv.addColumn("AircraftId");

        assignments.forEach(a -> {
            final int row = csv.addRow();
            csv.set(row, "Id", a.getId());
            csv.set(row, "Location", a.getLocation());
            csv.set(row, "ToIcao", a.getToIcao());
            csv.set(row, "FromIcao", a.getFromIcao());
            csv.set(row, "Amount", String.valueOf(a.getAmount()));
            csv.set(row, "UnitType", a.getUnitType());
            csv.set(row, "Commodity", a.getCommodity());
            csv.set(row, "Pay", String.valueOf(a.getPay()));
            csv.set(row, "Expires", a.getExpires());
            csv.set(row, "ExpireDateTime", a.getExpireDateTime());
            csv.set(row, "Express", a.isExpress() ? "TRUE" : "FALSE");
            csv.set(row, "PtAssignment", a.isPtAssignment() ? "TRUE" : "FALSE");
            csv.set(row, "Type", a.getType());
            csv.set(row, "AircraftId", a.getAircraftId());
        });

        File file = new File(ASSIGNMENTS_PER_LOCATION_STORAGE, icao + "/" + System.currentTimeMillis() + ".csv");
        //noinspection ResultOfMethodCallIgnored
        file.getParentFile().mkdirs();
        try {
            IOHelper.saveFile(file, csv.getContent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<FSEAssignment> loadFromCsv(String icao, long refreshPeriod) {
        final File file = findCachedCsv(icao, refreshPeriod);
        if (file == null) {
            return new ArrayList<>();
        }
        final String content;
        try {
            content = IOHelper.loadFile(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return parseCsv(Csv.fromContent(content));
    }
}
