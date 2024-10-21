package net.simforge.fseconomy.tools.lib;

public class FSEAssignment {
    private final String id;
    private final String location;
    private final String toIcao;
    private final String fromIcao;
    private final int amount;
    private final String unitType;
    private final String commodity;
    private final int pay;
    private final String expires;
    private final String expireDateTime;
    private final boolean express;
    private final boolean ptAssignment;
    private final String type;
    private final String aircraftId;

    public FSEAssignment(final String id,
                         final String location,
                         final String toIcao,
                         final String fromIcao,
                         final String amount,
                         final String unitType,
                         final String commodity,
                         final String pay,
                         final String expires,
                         final String expireDateTime,
                         final String express,
                         final String ptAssignment,
                         final String type,
                         final String aircraftId) {
        this.id = id;
        this.location = location;
        this.toIcao = toIcao;
        this.fromIcao = fromIcao;
        this.amount = Integer.parseInt(amount);
        this.unitType = unitType;
        this.commodity = commodity;
        this.pay = (int) Double.parseDouble(pay);
        this.expires = expires;
        this.expireDateTime = expireDateTime;
        this.express = "TRUE".equals(express);
        this.ptAssignment = "TRUE".equals(ptAssignment);
        this.type = type;
        this.aircraftId = aircraftId;
    }

    public String getId() {
        return id;
    }

    public String getLocation() {
        return location;
    }

    public String getToIcao() {
        return toIcao;
    }

    public String getFromIcao() {
        return fromIcao;
    }

    public int getAmount() {
        return amount;
    }

    public String getUnitType() {
        return unitType;
    }

    public String getCommodity() {
        return commodity;
    }

    public int getPay() {
        return pay;
    }

    public String getExpires() {
        return expires;
    }

    public String getExpireDateTime() {
        return expireDateTime;
    }

    public boolean isExpress() {
        return express;
    }

    public boolean isPtAssignment() {
        return ptAssignment;
    }

    public String getType() {
        return type;
    }

    public int getMass() {
        return type.equals("kg")
                ? amount
                : (type.equals("passengers") ? 77 * amount : 0);
    }
    public String getAircraftId() {
        return aircraftId;
    }
}
