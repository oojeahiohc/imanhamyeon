package com.imanhamyeon.ai;

import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

@Repository
public class ApiUsageStore {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private final JdbcTemplate jdbc;
    private final int dailyRequests;
    private final long dailyTokens;

    public ApiUsageStore(JdbcTemplate jdbc,
            @Value("${GROQ_DAILY_REQUEST_LIMIT:50}") int dailyRequests,
            @Value("${GROQ_DAILY_TOKEN_LIMIT:50000}") long dailyTokens) {
        this.jdbc = jdbc;
        this.dailyRequests = dailyRequests;
        this.dailyTokens = dailyTokens;
    }

    public LocalDate reserveCall() {
        LocalDate day = LocalDate.now(KOREA);
        int affected = jdbc.update("""
                INSERT INTO api_usage_daily(
                    usage_date, request_count, used_tokens, updated_at)
                SELECT ?, 1, 0, now()
                WHERE ? > 0 AND ? > 0
                ON CONFLICT (usage_date) DO UPDATE
                SET request_count = api_usage_daily.request_count + 1,
                    updated_at = now()
                WHERE api_usage_daily.request_count < ?
                  AND api_usage_daily.used_tokens < ?
                """, day, dailyRequests, dailyTokens,
                dailyRequests, dailyTokens);
        if (affected == 0) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "AI 서비스 사용량 한도에 도달했습니다.");
        }
        return day;
    }

    public void addTokens(LocalDate day, int totalTokens) {
        if (totalTokens > 0) {
            jdbc.update("""
                    UPDATE api_usage_daily
                    SET used_tokens = used_tokens + ?, updated_at = now()
                    WHERE usage_date = ?
                    """, totalTokens, day);
        }
    }
}