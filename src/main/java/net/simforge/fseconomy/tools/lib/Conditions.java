package net.simforge.fseconomy.tools.lib;

public class Conditions {
    public static boolean isIcaoInEU(final String icao) {
        return icao.startsWith("E") || icao.startsWith("L");
    }

    public static boolean isIcaoInEUOrNear(final String icao) {
        return icao.startsWith("E") || icao.startsWith("L") || icao.startsWith("B") || icao.startsWith("G");
    }

    public static boolean noDigitsInIcao(String icao) {
        return !icao.matches(".*\\d.*");
    }
}
