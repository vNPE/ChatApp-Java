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

public class Logger{
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss.SSS Z");
    private final Path file = Paths.get("server-log.txt");
    private final AtomicReference<Instant> lastInstant = new AtomicReference<>(null);

    public Logger(){
        try{
           if(file.getParent() != null)
               Files.createDirectories(file.getParent());
        }
        catch (IOException e){
            throw new RuntimeException("Failed to initialize log file", e);  
        }
    }
    
    private synchronized void log(String level, String message){
        Instant now = Instant.now();
        Instant prev = lastInstant.getAndSet(now);
        long elapsedMillis = prev == null ? 0L: Duration.between(prev, now).toMillis();

        String timestamp = ZonedDateTime.ofInstant(now, ZoneId.systemDefault()).format(fmt);
        String line = String.format("%s [%s] (+%dms) %s%n", timestamp, level, elapsedMillis, message);

        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardOpenOption.CREATE,StandardOpenOption.APPEND)){
            writer.write(line);
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    public void info(String msg){
        log("INFO", msg);
    }
    public void warning(String msg){
        log("WARNING", msg);
    }
    public void error(String msg){
        log("ERROR", msg);
    }
}
