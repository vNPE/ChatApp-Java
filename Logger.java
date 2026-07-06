import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    public enum LogLevel { INFO, WARNING, ERROR }

    private static final Path FILE = Path.of("server-log.txt");
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss.SSS Z");

    private volatile boolean consoleEnabled = false;
    private volatile boolean ansiColorEnabled = false;

    private volatile Long lastInstantMillis = null;

    public Logger() {
        try {
            if (FILE.getParent() != null) Files.createDirectories(FILE.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize log file", e);
        }
    }

    public void setConsole(boolean enabled, boolean ansi) {
        this.consoleEnabled = enabled;
        this.ansiColorEnabled = ansi;
    }

    private void log(LogLevel level, String message) {
        Instant now = Instant.now();
        Long prev = lastInstantMillis;
        lastInstantMillis = now.toEpochMilli();

        long elapsedMillis = (prev == null) ? 0L : Duration.between(Instant.ofEpochMilli(prev), now).toMillis();

        String timestamp = ZonedDateTime.ofInstant(now, ZoneId.systemDefault()).format(fmt);
        String line = String.format("%s [%s] (+%dms) %s%n", timestamp, level, elapsedMillis, message);

        try (BufferedWriter writer = Files.newBufferedWriter(
                FILE,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        )) {
            writer.write(line);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (consoleEnabled) {
            String label = level + ": ";
            String code = switch (level) {
                case INFO -> Color.blue();
                case WARNING -> Color.yellow();
                case ERROR -> Color.red();
            };
            System.out.println(Color.wrap(label, code, ansiColorEnabled) + message);
        }
    }

    public void info(String msg) { log(LogLevel.INFO, msg); }
    public void warning(String msg) { log(LogLevel.WARNING, msg); }
    public void error(String msg) { log(LogLevel.ERROR, msg); }
}
