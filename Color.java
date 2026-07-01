final class Color {
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String RESET = "\u001B[0m";

    static String wrap(String s, String code, boolean enabled) {
        return enabled ? code + s + RESET : s;
    }
    
    static String red() { return RED; }
    static String yellow() { return YELLOW; }
    static String blue() { return BLUE; }
}
