import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Db {
    private final HikariDataSource ds;

    public Db() {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:postgresql://127.0.0.1:5432/chatdb");
        cfg.setUsername("chatapp");
        cfg.setPassword("chat");
        cfg.setMaximumPoolSize(10);
        this.ds = new HikariDataSource(cfg);
    }

    public record Msg(String sender, String body, Timestamp createdAt) {}

    public void insertMessage(String sender, String body) throws SQLException {
        String sql = "INSERT INTO public.messages(sender, body) VALUES (?, ?)";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, sender);
            ps.setString(2, body);
            ps.executeUpdate();
        }
    }

    public List<Msg> lastMessages(int limit) throws SQLException {
        String sql = """
            SELECT sender, body, created_at
            FROM public.messages
            ORDER BY created_at DESC
            LIMIT ?
        """;

        List<Msg> out = new ArrayList<>();
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Msg(rs.getString(1), rs.getString(2), rs.getTimestamp(3)));
                }
            }
        }
        return out;
    }
}
