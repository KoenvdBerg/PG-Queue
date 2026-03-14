package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PGQueue {

    private final Connection conn;
    private final int batchSize;
    private final String tableName;

    public PGQueue(Connection conn, int batchSize, String tableName) {
        this.conn = conn;
        this.batchSize = batchSize;
        this.tableName = tableName;
    }

    public List<DomainQueue> pollQueue(Status toProcess) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(String.format("""
                WITH messages AS MATERIALIZED (
                    SELECT * FROM %s
                    WHERE status = %d
                    ORDER BY updated_at
                    LIMIT %d
                    FOR UPDATE SKIP LOCKED
                )
                UPDATE %s
                SET status = 2, updated_at = NOW()
                WHERE id = ANY(SELECT id FROM messages)
                returning *;
                """, tableName, toProcess.intValue(), batchSize, tableName));

        ResultSet rs = stmt.executeQuery();

        ArrayList<DomainQueue> messages = new ArrayList<>();
        while (rs.next()) {
            long id = rs.getLong(1);
            int status = rs.getInt(2);
            Instant updatedAt = rs.getTimestamp(3).toInstant();
            String payload = rs.getString(4);
            messages.add(new DomainQueue(id, status, updatedAt, payload));
        }
        stmt.close();
        return messages;
    }

    public void commit(List<Long> ids) throws SQLException {
        var sql = String.format("update domain_queue set status = 3, updated_at = NOW() where id IN (%s)", ids.stream()
                .map(v -> "?")
                .collect(Collectors.joining(", ")));
        PreparedStatement preparedStatement = conn.prepareStatement(sql);
        for (int i = 1; i <= ids.size(); i++) {
            preparedStatement.setObject(i, ids.get(i - 1));
        }
        preparedStatement.executeUpdate();
        preparedStatement.close();
    }

    public enum Status {
        PENDING, PROCESSING, DONE;

        public int intValue() {
            return switch (this) {
                case PENDING -> 1;
                case PROCESSING -> 2;
                case DONE -> 3;
            };
        }
    }
}
