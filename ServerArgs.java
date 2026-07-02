public final class ServerArgs {
    public final boolean verbose;
    public final boolean color;
    public final boolean help;

    private ServerArgs(boolean verbose, boolean color, boolean help) {
        this.verbose = verbose;
        this.color = color;
        this.help = help;
    }

    public static ServerArgs parse(String[] args) {
        boolean verbose = false;
        boolean color = false;
        boolean help = false;

        for (String arg : args) {
            switch (arg) {
                case "-v", "--verbose" -> verbose = true;
                case "-c", "--color" -> color = true;
                case "-h", "--help" -> help = true;
                default -> throw new IllegalArgumentException(
                        "Unknown argument: " + arg + ". Use -h for help."
                );
            }
        }
        return new ServerArgs(verbose, color, help);
    }

    public static void printHelp() {
        System.out.println(
                "Available flags:\n" +
                "  -v, --verbose   Prints everything that gets added to the log to the console.\n" +
                "  -c, --color     Uses ANSI colors for console log output.\n" +
                "  -h, --help      Prints this help message."
        );
    }
}
