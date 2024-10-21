package net.simforge.fseconomy.tools.lib;

import net.simforge.commons.io.Csv;

public class FSEAircraft {
    private final int serialNumber;
    private final String location;

    public FSEAircraft(final int serialNumber,
                       final String location) {
        this.serialNumber = serialNumber;
        this.location = location;
    }

    public int getSerialNumber() {
        return serialNumber;
    }

    public String getLocation() {
        return location;
    }

    public static FSEAircraft read(final Csv csv, final int row) {
        final String serialNumber = csv.value(row, "SerialNumber");
        final String location = csv.value(row, "Location");

        return new FSEAircraft(Integer.parseInt(serialNumber),
                location);
    }
}
