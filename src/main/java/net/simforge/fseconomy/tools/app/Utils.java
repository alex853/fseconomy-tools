package net.simforge.fseconomy.tools.app;

import net.simforge.fseconomy.tools.lib.Tools;

public class Utils {
    static boolean timeComes(long time) {
        return System.currentTimeMillis() - time >= Tools.ONE_HOUR;
    }
}
