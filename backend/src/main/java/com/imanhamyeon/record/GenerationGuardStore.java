package com.imanhamyeon.record;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class GenerationGuardStore {

    private final JdbcTemplate jdbc;

    public GenerationGuardStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Instant reserve(Long memberId, LocalDate date) {
        jdbc.update("""
                DELETE FROM generation_guards
                WHERE member_id = ? AND goal_date = ?
                  AND succeeded_at IS NULL
                  AND started_at < now() - interval '10 minutes'
                """, memberId, date);

        Instant started = Instant.now().truncatedTo(ChronoUnit.MICROS);
        int inserted = jdbc.update("""
                INSERT INTO generation_guards(member_id, goal_date, started_at)
                VALUES (?, ?, ?)
                ON CONFLICT DO NOTHING
                """, memberId, date, Timestamp.from(started));
        return inserted == 1 ? started : null;
    }

    public boolean markSuccess(Long memberId, LocalDate date, Instant started) {
        return jdbc.update("""
                UPDATE generation_guards
                SET succeeded_at = now()
                WHERE member_id = ? AND goal_date = ?
                  AND started_at = ? AND succeeded_at IS NULL
                """, memberId, date, Timestamp.from(started)) == 1;
    }

    public void release(Long memberId, LocalDate date, Instant started) {
        jdbc.update("""
                DELETE FROM generation_guards
                WHERE member_id = ? AND goal_date = ?
                  AND started_at = ? AND succeeded_at IS NULL
                """, memberId, date, Timestamp.from(started));
    }

    public State state(Long memberId, LocalDate date) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT succeeded_at, started_at FROM generation_guards
                WHERE member_id = ? AND goal_date = ?
                """, memberId, date);
        if (rows.isEmpty()) {
            return new State(false, false);
        }
        boolean succeeded = rows.get(0).get("succeeded_at") != null;
        Instant started = ((Timestamp) rows.get(0).get("started_at")).toInstant();
        boolean active = Instant.now().isBefore(started.plusSeconds(600));
        return new State(succeeded, !succeeded && active);
    }

    public record State(boolean succeeded, boolean inProgress) {}
}