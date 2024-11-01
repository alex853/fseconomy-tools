package net.simforge.fseconomy.tools.lib;

import net.simforge.commons.io.Csv;

public class FSEAircraft {
    private final int serialNumber;
    private final String makeModel;
    private final String registration;
    private final String location;
    private final float salePrice;
    private final float rentalDry;
    private final float rentalWet;
    private final String rentedBy;
    private final boolean needsRepair;
    private final float feeOwed;

    public FSEAircraft(final int serialNumber,
                       final String makeModel,
                       final String registration,
                       final String location,
                       final float salePrice,
                       final float rentalDry,
                       final float rentalWet,
                       final String rentedBy,
                       final boolean needsRepair,
                       final float feeOwed) {
        this.serialNumber = serialNumber;
        this.makeModel = makeModel;
        this.registration = registration;
        this.location = location;
        this.salePrice = salePrice;
        this.rentalDry = rentalDry;
        this.rentalWet = rentalWet;
        this.rentedBy = rentedBy;
        this.needsRepair = needsRepair;
        this.feeOwed = feeOwed;
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

    public float getRentalDry() {
        return rentalDry;
    }

    public float getRentalWet() {
        return rentalWet;
    }

    public String getRentedBy() {
        return rentedBy;
    }

    public boolean isNeedsRepair() {
        return needsRepair;
    }

    public float getFeeOwed() {
        return feeOwed;
    }

    public static FSEAircraft read(final Csv csv, final int row) {
        final String serialNumber = csv.value(row, "SerialNumber");
        final String makeModel = csv.value(row, "MakeModel");
        final String registration = csv.value(row, "Registration");
        final String location = csv.value(row, "Location");
        final String salePrice = csv.value(row, "SalePrice");
        final String rentalDry = csv.value(row, "RentalDry");
        final String rentalWet = csv.value(row, "RentalWet");
        final String rentedBy = csv.value(row, "RentedBy");
        final String needsRepair = csv.value(row, "NeedsRepair");
        final String feeOwed = csv.value(row, "FeeOwed");

        return new FSEAircraft(
                Integer.parseInt(serialNumber),
                makeModel,
                registration,
                location,
                Float.parseFloat(salePrice),
                Float.parseFloat(rentalDry),
                Float.parseFloat(rentalWet),
                rentedBy,
                Integer.parseInt(needsRepair) == 1,
                Float.parseFloat(feeOwed));
    }
}
