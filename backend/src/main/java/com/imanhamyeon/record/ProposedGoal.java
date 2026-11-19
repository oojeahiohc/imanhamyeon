package com.imanhamyeon.record;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "proposed_goals")
public class ProposedGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "daily_record_id", nullable = false)
    private Long dailyRecordId;

    @Column(nullable = false)
    private short position;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "action_text", nullable = false, length = 500)
    private String actionText;

    @Column(name = "is_selected", nullable = false)
    private boolean selected;

    @Column(name = "is_completed", nullable = false)
    private boolean completed;

    protected ProposedGoal() {}

    public ProposedGoal(Long dailyRecordId, short position,
            String title, String actionText) {
        this.dailyRecordId = dailyRecordId;
        this.position = position;
        this.title = title;
        this.actionText = actionText;
    }

    public Long getId() { return id; }
    public Long getDailyRecordId() { return dailyRecordId; }
    public short getPosition() { return position; }
    public String getTitle() { return title; }
    public String getActionText() { return actionText; }
    public boolean isSelected() { return selected; }
    public boolean isCompleted() { return completed; }

    public void select() { selected = true; }

    public void changeCompleted(boolean value) {
        if (!selected) {
            throw new IllegalStateException("선택하지 않은 목표");
        }
        completed = value;
    }
}