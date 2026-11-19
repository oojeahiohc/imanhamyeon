package com.imanhamyeon.record;

import java.time.LocalDate;
import java.util.List;

public final class RecordDtos {

    private RecordDtos() {}

    public record GoalView(Long id, short position, String title,
            String actionText, boolean isSelected, boolean isCompleted) {
        public static GoalView of(ProposedGoal goal) {
            return new GoalView(goal.getId(), goal.getPosition(),
                    goal.getTitle(), goal.getActionText(),
                    goal.isSelected(), goal.isCompleted());
        }
    }

    public record RecordView(Long id, LocalDate goalDate,
            String concernText, String status, List<GoalView> goals) {}

    public static RecordView of(DailyRecord record, List<ProposedGoal> proposed) {
        List<GoalView> goals = proposed.stream()
                .filter(goal -> "CHOICE_PENDING".equals(record.getStatus())
                        || goal.isSelected())
                .map(GoalView::of)
                .toList();
        return new RecordView(record.getId(), record.getGoalDate(),
                record.getConcernText(), record.getStatus(), goals);
    }
}