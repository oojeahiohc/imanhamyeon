package com.imanhamyeon.record;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface DailyRecordRepository extends JpaRepository<DailyRecord, Long> {
    Optional<DailyRecord> findByMemberIdAndGoalDate(Long memberId, LocalDate goalDate);
    Optional<DailyRecord> findByIdAndMemberId(Long id, Long memberId);
    Page<DailyRecord> findByMemberIdOrderByGoalDateDesc(Long memberId, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
           SELECT record FROM DailyRecord record
           WHERE record.id = :id AND record.memberId = :memberId
           """)
    Optional<DailyRecord> findForUpdate(
            @Param("id") Long id, @Param("memberId") Long memberId);
}