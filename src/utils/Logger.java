package utils;

/**
 * Created by Stefan Croes
 */
public class Logger {
    private static boolean printDebug = true;

    private Logger() {
    }

    public static boolean isPrintDebug() {
        return printDebug;
    }

    public static void setPrintDebug(boolean printDebug) {
        Logger.printDebug = printDebug;
    }

    public static void debug(String msg) {
        if (printDebug) System.out.println(msg);
    }

    public static void debug(Object obj) {
        if (printDebug) System.out.println(obj.toString());
    }
}
