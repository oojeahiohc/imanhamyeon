package com.imanhamyeon.record;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProposedGoalRepository extends JpaRepository<ProposedGoal, Long> {
    List<ProposedGoal> findByDailyRecordIdOrderByPositionAsc(Long dailyRecordId);
}