package com.imanhamyeon.record;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.imanhamyeon.ai.GroqClient;
import com.imanhamyeon.auth.CurrentMember;
import com.imanhamyeon.member.Member;

@Service
public class DailyRecordService {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

    private final CurrentMember currentMember;
    private final DailyRecordRepository records;
    private final ProposedGoalRepository goals;
    private final GenerationGuardStore guards;
    private final GroqClient groq;
    private final TransactionTemplate transactions;

    public DailyRecordService(CurrentMember currentMember,
            DailyRecordRepository records, ProposedGoalRepository goals,
            GenerationGuardStore guards, GroqClient groq,
            TransactionTemplate transactions) {
        this.currentMember = currentMember;
        this.records = records;
        this.goals = goals;
        this.guards = guards;
        this.groq = groq;
        this.transactions = transactions;
    }

    public RecordDtos.RecordView generate(Authentication authentication,
            String concernText, Boolean confirmed) {
        Member member = currentMember.require(authentication);
        if (member.getPrivacyAgreedAt() == null
                || member.getAiAgreedAt() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "필수 동의가 확인되지 않습니다.");
        }
        if (!Boolean.TRUE.equals(confirmed)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "고민 내용을 최종 확인해 주세요.");
        }
        if (concernText == null || concernText.isBlank()
                || concernText.replaceAll("\\s", "").length() < 10
                || concernText.length() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "고민은 공백 제외 10자 이상, 전체 1000자 이하로 입력해 주세요.");
        }

        LocalDate date = LocalDate.now(KOREA);
        Instant started = guards.reserve(member.getId(), date);
        if (started == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "오늘의 목표 생성이 이미 진행 중이거나 완료되었습니다.");
        }

        try {
            if (groq.hasSeriousRisk(concernText)) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                        "위험 신호가 감지되어 목표 생성을 중단했습니다. "
                        + "긴급한 상황에는 119 또는 가까운 응급실을 이용하고, "
                        + "정신건강 전문가에게 도움을 요청해 주세요.");
            }
            List<GroqClient.SuggestedGoal> suggestions =
                    groq.suggestGoals(concernText);

            return transactions.execute(status -> {
                if (!guards.markSuccess(member.getId(), date, started)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "요청이 만료되었습니다. 다시 시도해 주세요.");
                }
                DailyRecord saved = records.saveAndFlush(
                        new DailyRecord(member.getId(), date, concernText));
                List<ProposedGoal> inserted = goals.saveAll(
                        java.util.stream.IntStream.range(0, suggestions.size())
                                .mapToObj(i -> new ProposedGoal(saved.getId(),
                                        (short) (i + 1),
                                        suggestions.get(i).title(),
                                        suggestions.get(i).actionText()))
                                .toList());
                goals.flush();
                return RecordDtos.of(saved, inserted);
            });
        } catch (RuntimeException error) {
            guards.release(member.getId(), date, started);
            throw error;
        }
    }

    public TodayView today(Authentication authentication) {
        Member member = currentMember.require(authentication);
        LocalDate date = LocalDate.now(KOREA);
        Optional<DailyRecord> record =
                records.findByMemberIdAndGoalDate(member.getId(), date);
        GenerationGuardStore.State state = guards.state(member.getId(), date);

        RecordDtos.RecordView data = record.map(value ->
                RecordDtos.of(value,
                        goals.findByDailyRecordIdOrderByPositionAsc(value.getId())))
                .orElse(null);

        return new TodayView(
                !state.succeeded() && !state.inProgress() && data == null,
                state.inProgress(),
                data);
    }

    public record TodayView(boolean canGenerate, boolean inProgress,
            RecordDtos.RecordView record) {}
    
    @Transactional(readOnly = true)
    public RecordDtos.RecordView detail(
            Authentication authentication, Long recordId) {
        Member member = currentMember.require(authentication);
        DailyRecord record = records.findByIdAndMemberId(recordId, member.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "기록을 찾을 수 없습니다."));
        return RecordDtos.of(record,
                goals.findByDailyRecordIdOrderByPositionAsc(recordId));
    }

    @Transactional
    public RecordDtos.RecordView select(Authentication authentication,
            Long recordId, List<Long> goalIds) {
        Member member = currentMember.require(authentication);
        if (goalIds == null || goalIds.isEmpty() || goalIds.size() > 3
                || goalIds.stream().anyMatch(id -> id == null)
                || new HashSet<>(goalIds).size() != goalIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "서로 다른 목표를 1개에서 3개 선택해 주세요.");
        }

        DailyRecord record = records.findForUpdate(recordId, member.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "기록을 찾을 수 없습니다."));
        if (!"CHOICE_PENDING".equals(record.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "이미 선택을 확정했습니다.");
        }

        List<ProposedGoal> proposed =
                goals.findByDailyRecordIdOrderByPositionAsc(recordId);
        Set<Long> chosen = Set.copyOf(goalIds);
        long matching = proposed.stream()
                .filter(goal -> chosen.contains(goal.getId()))
                .count();
        if (matching != goalIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "해당 기록에 속한 목표만 선택할 수 있습니다.");
        }

        for (ProposedGoal goal : proposed) {
            if (chosen.contains(goal.getId())) goal.select();
        }
        record.confirmSelection();
        return RecordDtos.of(record, proposed);
    }
}