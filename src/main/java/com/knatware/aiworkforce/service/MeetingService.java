package com.knatware.aiworkforce.service;

import com.knatware.aiworkforce.model.Meeting;
import com.knatware.aiworkforce.model.Staff;
import com.knatware.aiworkforce.repository.MeetingRepository;
import com.knatware.aiworkforce.repository.StaffRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Schedules meetings and checks attendee availability. A conflict is any
 * existing meeting whose time range overlaps the proposed one and shares at
 * least one attendee. AI staff are always available, so they never conflict —
 * only human attendees can clash.
 */
@Service
public class MeetingService {

    private final MeetingRepository meetings;
    private final StaffRepository staff;

    public MeetingService(MeetingRepository meetings, StaffRepository staff) {
        this.meetings = meetings;
        this.staff = staff;
    }

    public List<Meeting> all() { return meetings.findAllByOrderByStartTimeAsc(); }

    public record ScheduleRequest(String title, String description, Instant startTime,
                                  Instant endTime, String location,
                                  Long organiserId, List<Long> attendeeIds) { }

    public record ScheduleResult(boolean ok, Meeting meeting, List<String> conflicts) { }

    /**
     * Schedule a meeting, rejecting it if any HUMAN attendee already has an
     * overlapping meeting. Returns the clashing attendee names if so.
     */
    public ScheduleResult schedule(ScheduleRequest req) {
        List<String> conflicts = findConflicts(req.attendeeIds(), req.startTime(), req.endTime(), null);
        if (!conflicts.isEmpty()) {
            return new ScheduleResult(false, null, conflicts);
        }
        Meeting m = new Meeting();
        m.setTitle(req.title());
        m.setDescription(req.description());
        m.setStartTime(req.startTime());
        m.setEndTime(req.endTime());
        m.setLocation(req.location());
        if (req.organiserId() != null) staff.findById(req.organiserId()).ifPresent(m::setOrganiser);
        if (req.attendeeIds() != null) {
            for (Long id : req.attendeeIds()) staff.findById(id).ifPresent(m.getAttendees()::add);
        }
        return new ScheduleResult(true, meetings.save(m), List.of());
    }

    public void cancel(Long id) { meetings.deleteById(id); }

    /** Names of human attendees who already have an overlapping meeting. */
    private List<String> findConflicts(List<Long> attendeeIds, Instant start, Instant end, Long excludeMeetingId) {
        List<String> clashes = new ArrayList<>();
        if (attendeeIds == null || start == null || end == null) return clashes;
        Set<Long> wanted = Set.copyOf(attendeeIds);

        for (Meeting existing : meetings.findAll()) {
            if (excludeMeetingId != null && existing.getId().equals(excludeMeetingId)) continue;
            if (existing.getStartTime() == null || existing.getEndTime() == null) continue;
            boolean overlaps = start.isBefore(existing.getEndTime()) && end.isAfter(existing.getStartTime());
            if (!overlaps) continue;
            for (Staff a : existing.getAttendees()) {
                // AI staff are always available — only humans can conflict.
                if (a.getType() != null && a.getType().name().equals("HUMAN")
                        && wanted.contains(a.getId())
                        && !clashes.contains(a.getFullName())) {
                    clashes.add(a.getFullName());
                }
            }
        }
        return clashes;
    }
}
