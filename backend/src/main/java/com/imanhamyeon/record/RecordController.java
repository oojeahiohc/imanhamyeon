package com.imanhamyeon.record;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/daily-records")
public class RecordController {

    private final DailyRecordService service;

    public RecordController(DailyRecordService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GeneratedResponse generate(@RequestBody GenerateRequest body,
            Authentication authentication) {
        RecordDtos.RecordView record = service.generate(authentication,
                body.concernText(), body.confirmed());
        return new GeneratedResponse(record.id(), record.goalDate(),
                record.status(), record.goals());
    }

    @GetMapping("/today")
    public DailyRecordService.TodayView today(Authentication authentication) {
        return service.today(authentication);
    }

    public record GenerateRequest(String concernText, Boolean confirmed) {}

    public record GeneratedResponse(Long recordId, LocalDate goalDate,
            String status, List<RecordDtos.GoalView> goals) {}
    
    @GetMapping("/{id}")
    public RecordDtos.RecordView detail(
            @PathVariable("id") Long id, Authentication authentication) {
        return service.detail(authentication, id);
    }

    @PostMapping("/{id}/selection")
    public SelectionResponse select(
            @PathVariable("id") Long id,
            @RequestBody SelectionRequest body,
            Authentication authentication) {

        RecordDtos.RecordView record =
                service.select(authentication, id, body.goalIds());

        return new SelectionResponse(
                record.id(), record.status(), record.goals());
    }

    public record SelectionRequest(List<Long> goalIds) {}

    public record SelectionResponse(Long id, String status,
            List<RecordDtos.GoalView> goals) {}
}