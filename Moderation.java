import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

public class Moderation {
    // Stores each banned entry in a canonical form (lowercase, accents removed, etc.)
    private final List<String> bannedSubstrings;

    // Matches Unicode combining marks (accents/diacritics) after NFKD normalization.
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    private static String canonicalize(String s) {
        if (s == null) return null;

        // Decompose characters so diacritics become separate code points (e.g., ì => i + combining mark).
        s = Normalizer.normalize(s, Normalizer.Form.NFKD);

        // Case-fold using a stable locale so matching is case-insensitive.
        s = s.toLowerCase(Locale.ROOT);

        // Strip diacritics (turn è/é/ê into e, à into a, etc.).
        s = DIACRITICS.matcher(s).replaceAll("");

        // Remove separators/punctuation and underscores/hyphens so variants like "foo-bar" and "foo bar"
        // map to the same canonical text for substring matching.
        s = s.replaceAll("[\\p{Punct}_\\-]+", "");

        // Collapse all whitespace by removing it.
        s = s.replaceAll("\\s+", "");

        return s;
    }

    public Moderation(Path bannedFile) throws IOException {
        List<String> tmp = new ArrayList<>();
        for (String line : Files.readAllLines(bannedFile)) {
            String s = line.trim();
            if (s.isEmpty() || s.startsWith("#")) continue;
            tmp.add(canonicalize(s));
        }
        // Make the banned list immutable after loading/normalizing.
        bannedSubstrings = List.copyOf(tmp);
    }

    public boolean isBannedName(String name) {
        if (name == null) return false;

        // Canonicalize the input the same way the banned entries were canonicalized.
        String n = canonicalize(name);
        if (n == null || n.isEmpty()) return false;

        // Simple substring match against the canonical banned entries.
        for (String banned : bannedSubstrings) {
            if (!banned.isEmpty() && n.contains(banned)) return true;
        }
        return false;
    }
}
