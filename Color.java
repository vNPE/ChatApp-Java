final class Color {
    static final String RESET = "\u001B[0m";
    static String wrap(String s, String code, boolean enabled) {
        return enabled ? code + s + RESET : s;
    }
}
