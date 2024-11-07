package net.simforge.fseconomy.tools.lib;

import net.simforge.commons.misc.Geo;
import net.simforge.fseconomy.tools.feeder.FSECachedAssignments;
import net.simforge.refdata.airports.Airport;
import net.simforge.refdata.airports.Airports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static net.simforge.fseconomy.tools.lib.Tools.ONE_HOUR;
import static net.simforge.fseconomy.tools.lib.Tools.d1;

// todo ak green jobs
// todo ak cargo jobs
// todo ak cyclic support - ability to return back to airport and not to go into infinite cycle
// todo ak look for close airports
// todo ak region filter settings
// todo ak switcher for filtering or non-filtering by digits in icao code
public class FindAssignments {
    private static final Logger log = LoggerFactory.getLogger(FindAssignments.class);

    public static List<Assignment> find(final Params params) {
        final List<String> icaosInVicinity = Tools.icaoInRadius(params.getStartingLocation(), params.getLookingInVicinityRadiusNm()).stream()
                .filter(icao -> isIcaoMatchesDigitsCondition(icao, params))
                .collect(Collectors.toList());
        try {
            FSECachedAssignments.loadOutgoingAssignments(icaosInVicinity, params.getRefreshPeriod());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final List<Assignment> assignments = new ArrayList<>();
        buildAvailableLegs(params, params.getStartingLocation()).forEach(leg -> assignments.add(Assignment.startWithLeg(leg, params)));
        icaosInVicinity.forEach(icaoInVicinity -> {
            buildAvailableLegs(params, icaoInVicinity).forEach(leg -> {
                final Leg ferryLeg = Leg.ferryLeg(params.getStartingLocation(), icaoInVicinity);
                final Assignment assignment = Assignment.startWithLeg(ferryLeg, params).addLeg(leg);
                assignments.add(assignment);
            });
        });

        final List<Assignment> results = prolongAssignments(params, assignments);
        results.sort(Comparator.comparingInt(assignment -> -assignment.getTotalPay()));
        return results;
    }

    // ignore   digits-in-icao   result
    // true     true             false
    // true     false            true
    // false    true             true
    // false    false            true
    private static boolean isIcaoMatchesDigitsCondition(String icao, Params params) {
        return !params.isIgnoreIcaosWithDigits()
                || (params.isIgnoreIcaosWithDigits() && Conditions.noDigitsInIcao(icao));
    }

    public static class Params {
        private final String startingLocation;
        private int maxPax;
        private int maxLegs = 3;
        private int maxWorkingHours = 12;
        private int lookingInVicinityRadiusNm = 0; // 0 means disabled
        private boolean ignoreIcaosWithDigits = false;

        private Params(final String startingLocation) {
            this.startingLocation = startingLocation;
        }

        public static Params of(String startingLocation) {
            return new Params(startingLocation);
        }

        public String getStartingLocation() {
            return startingLocation;
        }

        public int getMaxPax() {
            return maxPax;
        }

        public Params withMaxPax(final int maxPax) {
            this.maxPax = maxPax;
            return this;
        }

        public int getLookingInVicinityRadiusNm() {
            return lookingInVicinityRadiusNm;
        }

        public Params withLookingInVicinityRadiusNm(final int radiusNm) {
            this.lookingInVicinityRadiusNm = radiusNm;
            return this;
        }

        public boolean isIgnoreIcaosWithDigits() {
            return ignoreIcaosWithDigits;
        }

        public Params withIgnoreIcaosWithDigits(final boolean ignore) {
            this.ignoreIcaosWithDigits = ignore;
            return this;
        }

        public long getRefreshPeriod() {
            return 3*ONE_HOUR;
        }

        public int getMaxLegs() {
            return maxLegs;
        }

        public Params withMaxLegs(final int maxLegs) {
            this.maxLegs = maxLegs;
            return this;
        }

        public double getMaxWorkingHours() {
            return maxWorkingHours;
        }

        public Params withMaxWorkingHours(final int maxWorkingHours) {
            this.maxWorkingHours = maxWorkingHours;
            return this;
        }

        public double getTurnaroundDurationHours() {
            return 0.5;
        }
    }

    private static List<Assignment> prolongAssignments(final Params params, final List<Assignment> assignments) {
        log.info("[Prolong] Starting for {} assignments", assignments.size());

        final List<Assignment> prolongedAssignments = new ArrayList<>();

        final Set<String> icaosToPreload = assignments.stream()
                .map(Assignment::getFinishingIcao)
                .filter(icao -> isIcaoMatchesDigitsCondition(icao, params))
                .collect(Collectors.toSet());
        log.info("[Prolong] Icaos to preload {}", icaosToPreload);
        try {
            FSECachedAssignments.loadOutgoingAssignments(icaosToPreload, params.getRefreshPeriod());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        assignments.forEach(assignment -> {
            final Set<String> alreadyUsedAssignmentIds = assignment.getLegs().stream()
                    .map(l -> l.fseAssignment.getId())
                    .collect(Collectors.toSet());

            final List<Leg> legs = buildAvailableLegs(params, assignment.getFinishingIcao());
            legs.forEach(leg -> {
                if (alreadyUsedAssignmentIds.contains(leg.fseAssignment.getId())) {
                    log.warn("[Prolong] Already flown assignment {} - {}, ignoring to avoid cycles", leg.fseAssignment.getFromIcao(), leg.fseAssignment.getToIcao());
                    return;
                }

                final Assignment prolongedAssignment = assignment.addLeg(leg);
                if (!prolongedAssignment.isWithinLimits()) {
                    return;
                }
                prolongedAssignments.add(prolongedAssignment);
            });
        });

        final List<Assignment> nextAssignments = !prolongedAssignments.isEmpty()
                ? prolongAssignments(params, prolongedAssignments)
                : new ArrayList<>();

        return join(nextAssignments, assignments);
    }

    private static List<Leg> buildAvailableLegs(final Params params, final String icaoFrom) {
        final List<FSEAssignment> fseAssignments;
        try {
            fseAssignments = FSECachedAssignments.loadOutgoingAssignments(
                    Collections.singletonList(icaoFrom),
                    params.getRefreshPeriod());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final List<FSEAssignment> vips = fseAssignments.stream()
                .filter(a -> "VIP".equals(a.getType()))
                .filter(a -> "passengers".equals(a.getUnitType()))
                .filter(a -> a.getAmount() <= params.getMaxPax())
//                .filter(a -> a.getToIcao().startsWith("E") || a.getToIcao().startsWith("L") || a.getToIcao().startsWith("D") || a.getToIcao().startsWith("G"))
                .filter(a -> Conditions.noDigitsInIcao(a.getToIcao())) // no digits in icao code
                .collect(Collectors.toList());

        return vips.stream()
                .map(Leg::build)
                .collect(Collectors.toList());
    }

    public static class Assignment {
        private final LinkedList<Leg> legs = new LinkedList<>();
        private final Params params;

        private Assignment(final List<Leg> legs, final Params params) {
            this.legs.addAll(legs);
            this.params = params;
        }

        public static Assignment startWithLeg(final Leg leg, final Params params) {
            return new Assignment(Collections.singletonList(leg), params);
        }

        public List<Leg> getLegs() {
            return Collections.unmodifiableList(legs);
        }

        public Assignment addLeg(final Leg leg) {
            return new Assignment(add(legs, leg), params);
        }

        public boolean isWithinLimits() {
            return legs.size() <= params.getMaxLegs()
                    && getDurationHours() <= params.getMaxWorkingHours();
        }

        public String getStartingIcao() {
            return legs.getFirst().getFromIcao();
        }

        public String getFinishingIcao() {
            return legs.getLast().getToIcao();
        }

        public double getDurationHours() {
            return legs.stream().mapToDouble(leg -> {
                final Optional<Airport> fromAirport = Airports.get().findByIcao(leg.getFromIcao());
                final Optional<Airport> toAirport = Airports.get().findByIcao(leg.getToIcao());
                final double distance = Geo.distance(fromAirport.get().getCoords(), toAirport.get().getCoords());
                return calcLegDurationHours(distance) + params.getTurnaroundDurationHours();
            }).sum() + params.getTurnaroundDurationHours();
        }

        public int getTotalPay() {
            return legs.stream().mapToInt(Leg::getPay).sum();
        }

        public String toString() {
            final List<String> route = new ArrayList<>();
            route.add(legs.get(0).getFromIcao());
            legs.forEach(leg -> route.add(leg.getToIcao()));

            return String.format("Assignment: %s leg(s), %s hours, $%s, route %s",
                    legs.size(),
                    d1.format(getDurationHours()),
                    getTotalPay(),
                    String.join(" -> ", route));
        }
    }

    public static class Leg {
        private final String fromIcao;
        private final String toIcao;
        private final boolean isFerryLeg;
        private final FSEAssignment fseAssignment;

        private Leg(final String fromIcao,
                    final String toIcao,
                    final boolean isFerryLeg,
                    final FSEAssignment fseAssignment) {
            this.fromIcao = fromIcao;
            this.toIcao = toIcao;
            this.isFerryLeg = isFerryLeg;
            this.fseAssignment = fseAssignment;
        }

        public static Leg build(final FSEAssignment fseAssignment) {
            return new Leg(fseAssignment.getLocation(), fseAssignment.getToIcao(), false, fseAssignment);
        }

        public static Leg ferryLeg(final String fromIcao, final String toIcao) {
            return new Leg(fromIcao, toIcao, true, null);
        }

        public String getFromIcao() {
            return fromIcao;
        }

        public String getToIcao() {
            return toIcao;
        }

        public int getPay() {
            return isFerryLeg ? 0 : fseAssignment.getPay();
        }
    }

    private static double calcLegDurationHours(final double distance) {
        return distance / 380 + 0.5;
    }

    private static <T> List<T> join(final List<T>... lists) {
        final List<T> result = new ArrayList<>();
        Arrays.stream(lists).forEach(result::addAll);
        return result;
    }

    private static <T> List<T> add(final List<T> list, final T element) {
        final List<T> result = new ArrayList<>(list);
        result.add(element);
        return result;
    }
}
