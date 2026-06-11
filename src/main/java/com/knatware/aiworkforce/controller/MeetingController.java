package com.knatware.aiworkforce.controller;

import com.knatware.aiworkforce.model.Meeting;
import com.knatware.aiworkforce.service.MeetingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Calendar / meeting scheduling. List meetings, schedule new ones (with
 * automatic conflict detection on human attendees), and cancel them.
 */
@RestController
@RequestMapping("/api/meetings")
public class MeetingController {

    private final MeetingService meetings;

    public MeetingController(MeetingService meetings) { this.meetings = meetings; }

    @GetMapping
    public List<Meeting> list() { return meetings.all(); }

    @PostMapping
    public ResponseEntity<?> schedule(@RequestBody MeetingService.ScheduleRequest req) {
        MeetingService.ScheduleResult result = meetings.schedule(req);
        if (!result.ok()) {
            return ResponseEntity.status(409).body(java.util.Map.of(
                    "ok", false,
                    "message", "Scheduling conflict for: " + String.join(", ", result.conflicts()),
                    "conflicts", result.conflicts()));
        }
        return ResponseEntity.ok(result.meeting());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        meetings.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
