import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicReference;

public class Logger {
    public enum LogLevel { INFO, WARNING, ERROR }

    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss.SSS Z");
    private final java.nio.file.Path file = java.nio.file.Paths.get("server-log.txt");

    private boolean consoleEnabled = false;
    private boolean ansiColorEnabled = false;

    private final java.util.concurrent.atomic.AtomicReference<java.time.Instant> lastInstant =
            new java.util.concurrent.atomic.AtomicReference<>(null);

    public Logger() {
        try {
            if (file.getParent() != null) java.nio.file.Files.createDirectories(file.getParent());
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to initialize log file", e);
        }
    }

    public void setConsole(boolean enabled, boolean ansi) {
        this.consoleEnabled = enabled;
        this.ansiColorEnabled = ansi;
    }

    private synchronized void log(LogLevel level, String message) {
        Instant now = Instant.now();
        Instant prev = lastInstant.getAndSet(now);
        long elapsedMillis = prev == null ? 0L : Duration.between(prev, now).toMillis();

        String timestamp = ZonedDateTime.ofInstant(now, ZoneId.systemDefault()).format(fmt);
        String line = String.format("%s [%s] (+%dms) %s%n", timestamp, level, elapsedMillis, message);

        try (BufferedWriter writer = Files.newBufferedWriter(file,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND)) {
            writer.write(line);
        } catch (java.io.IOException e) {
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
