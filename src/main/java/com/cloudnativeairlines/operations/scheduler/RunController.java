package com.cloudnativeairlines.operations.scheduler;

import com.cloudnativeairlines.operations.scheduler.ScheduleService.RunSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/runs")
public class RunController {

    private final ScheduleService schedule;

    public RunController(ScheduleService schedule) {
        this.schedule = schedule;
    }

    public record BuildRunRequest(@NotNull Instant start, Integer horizonDays) {
    }

    @PostMapping("/{runId}")
    @ResponseStatus(HttpStatus.CREATED)
    public RunSummary build(@PathVariable String runId,
            @Valid @RequestBody BuildRunRequest request) {
        return schedule.buildRun(runId, request.start(), request.horizonDays());
    }

    @GetMapping("/{runId}")
    public RunSummary get(@PathVariable String runId) {
        return schedule.getRun(runId);
    }

    @DeleteMapping("/{runId}")
    public ResponseEntity<Void> reset(@PathVariable String runId) {
        schedule.resetRun(runId);
        return ResponseEntity.noContent().build();
    }
}
