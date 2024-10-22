package net.simforge.fseconomy.tools.lib;

import net.simforge.commons.io.Csv;

public class FSEAircraft {
    private final int serialNumber;
    private final String makeModel;
    private final String registration;
    private final String location;
    private final float salePrice;

    public FSEAircraft(final int serialNumber,
                       final String makeModel,
                       final String registration,
                       final String location,
                       final float salePrice) {
        this.serialNumber = serialNumber;
        this.makeModel = makeModel;
        this.registration = registration;
        this.location = location;
        this.salePrice = salePrice;
    }

    public int getSerialNumber() {
        return serialNumber;
    }

    public String getMakeModel() {
        return makeModel;
    }

    public String getRegistration() {
        return registration;
    }

    public String getLocation() {
        return location;
    }

    public float getSalePrice() {
        return salePrice;
    }

    public static FSEAircraft read(final Csv csv, final int row) {
        final String serialNumber = csv.value(row, "SerialNumber");
        final String makeModel = csv.value(row, "MakeModel");
        final String registration = csv.value(row, "Registration");
        final String location = csv.value(row, "Location");
        final String salePrice = csv.value(row, "SalePrice");

        return new FSEAircraft(
                Integer.parseInt(serialNumber),
                makeModel,
                registration,
                location,
                Float.parseFloat(salePrice));
    }
}
