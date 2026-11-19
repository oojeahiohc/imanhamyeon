package com.imanhamyeon.record;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "daily_records")
public class DailyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "goal_date", nullable = false)
    private LocalDate goalDate;

    @Column(name = "concern_text", nullable = false, columnDefinition = "text")
    private String concernText;

    @Column(nullable = false, length = 24)
    private String status;

    @Column(name = "selected_at")
    private Instant selectedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DailyRecord() {}

    public DailyRecord(Long memberId, LocalDate goalDate, String concernText) {
        this.memberId = memberId;
        this.goalDate = goalDate;
        this.concernText = concernText;
        this.status = "CHOICE_PENDING";
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getMemberId() { return memberId; }
    public LocalDate getGoalDate() { return goalDate; }
    public String getConcernText() { return concernText; }
    public String getStatus() { return status; }
    public Instant getSelectedAt() { return selectedAt; }

    public void confirmSelection() {
        if (!"CHOICE_PENDING".equals(status)) {
            throw new IllegalStateException("이미 선택을 확정한 기록");
        }
        status = "NOT_COMPLETED";
        selectedAt = Instant.now();
    }

    public void changeStatus(String next) { status = next; }
}