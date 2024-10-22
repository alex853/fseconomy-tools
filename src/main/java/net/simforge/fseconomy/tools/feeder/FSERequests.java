package net.simforge.fseconomy.tools.feeder;

import net.simforge.commons.io.Csv;
import net.simforge.fseconomy.tools.lib.FSEAircraft;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FSERequests {
    public static Optional<FSEAircraft> loadAircraftByRegistration(final String aircraftRegistration) throws IOException {
        final Csv data = FSEFeeder.loadCsv("query=aircraft&search=registration&aircraftreg=" + aircraftRegistration);
        return Optional.of(FSEAircraft.read(data, 0));
    }

    public static List<FSEAircraft> loadAircraftForSale() throws IOException {
        final Csv data = FSEFeeder.loadCsv("query=aircraft&search=forsale");
        final List<FSEAircraft> result = new ArrayList<>();
        for (int row = 0; row < data.rowCount(); row++) {
            if (data.rowWidth(row) != 25) {
                continue;
            }
            result.add(FSEAircraft.read(data, row));
        }
        return result;
    }
}
