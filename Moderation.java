import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Moderation {
    private final List<String> bannedSubstrings;

    public Moderation(Path bannedFile) throws IOException {
        List<String> tmp = new ArrayList<>();
        for (String line : Files.readAllLines(bannedFile)) {
            String s = line.trim();
            if (s.isEmpty() || s.startsWith("#")) continue;
            tmp.add(s.toLowerCase());
        }
        this.bannedSubstrings = List.copyOf(tmp);
    }

    public boolean isBannedName(String name) {
        if (name == null) return false;
        String n = name.toLowerCase();

        for (String banned : bannedSubstrings) {
            if (!banned.isEmpty() && n.contains(banned)) return true;
        }
        return false;
    }
}

