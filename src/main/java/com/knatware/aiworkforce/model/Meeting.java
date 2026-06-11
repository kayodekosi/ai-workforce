package com.knatware.aiworkforce.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * A scheduled meeting / calendar event. Has an organiser and a set of attendee
 * staff (AI or human). Used by the scheduling service, which checks attendee
 * availability for conflicts.
 */
@Entity
@Table(name = "meeting")
public class Meeting {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    @Column(length = 1000)
    private String description;

    private Instant startTime;
    private Instant endTime;
    private String location;        // room name or video link

    @ManyToOne
    private Staff organiser;

    @ManyToMany
    @JoinTable(name = "meeting_attendees",
        joinColumns = @JoinColumn(name = "meeting_id"),
        inverseJoinColumns = @JoinColumn(name = "staff_id"))
    private Set<Staff> attendees = new HashSet<>();

    public Meeting() { }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }
    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Staff getOrganiser() { return organiser; }
    public void setOrganiser(Staff organiser) { this.organiser = organiser; }
    public Set<Staff> getAttendees() { return attendees; }
    public void setAttendees(Set<Staff> attendees) { this.attendees = attendees; }
}
