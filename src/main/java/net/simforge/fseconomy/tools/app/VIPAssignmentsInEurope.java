package net.simforge.fseconomy.tools.app;

import net.simforge.fseconomy.tools.feeder.FSECachedAssignments;
import net.simforge.fseconomy.tools.lib.Conditions;
import net.simforge.fseconomy.tools.lib.FSEAssignment;
import net.simforge.fseconomy.tools.lib.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class VIPAssignmentsInEurope implements Task {
    private static final Logger log = LoggerFactory.getLogger(VIPAssignmentsInEurope.class);

    private final int maxPax;

    private long lastCheck;
    private final Set<String> sentNotifications = new TreeSet<>();

    public VIPAssignmentsInEurope(final int maxPax) {
        this.maxPax = maxPax;
    }

    @Override
    public void process() {
        if (!Utils.timeComes(lastCheck)) {
            return;
        }

        lastCheck = System.currentTimeMillis();
        final List<FSEAssignment> assignments;
        try {
            final Set<String> majorAirportsInEU = Tools.getMajorAirports().stream()
                    .filter(Conditions::isIcaoInEUOrNear)
                    .filter(Conditions::noDigitsInIcao)
                    .collect(Collectors.toSet());
            assignments = FSECachedAssignments.loadOutgoingAssignments(majorAirportsInEU, Tools.ONE_HOUR);
        } catch (IOException e) {
            log.error("Unable to retrieve assignments list", e);
            return;
        }

        final List<FSEAssignment> suitableAssignments = assignments.stream()
                .filter(a -> a.getPay() >= 15000)
                .filter(a -> a.getAmount() <= maxPax)
                .filter(a -> "passengers".equals(a.getUnitType()))
                .filter(a -> Conditions.noDigitsInIcao(a.getToIcao()))
                .sorted(Comparator.comparingInt(a -> -a.getPay()))
                .collect(Collectors.toList());

        suitableAssignments.forEach(a -> {
            final String name = "[FSE] [Assignments in Europe]    "
                    + a.getLocation() + " -> " + a.getToIcao() + ",     "
                    + (int)a.getDistance() + "nm,     "
                    + "$" + Tools.formatPrice(a.getPay());
            final String description = Tools.toString(a);
            final String msg = name + " / " + description;
            log.info(msg);
            if (!sentNotifications.contains(msg)) {
                TrelloSender.addToQueue(name, description);
                sentNotifications.add(msg);
            }
        });
    }
}
