package com.knatware.aiworkforce.controller;

import com.knatware.aiworkforce.model.*;
import com.knatware.aiworkforce.repository.StaffRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin operations on staff: list, onboard, update/configure, put on leave,
 * reactivate, offboard. AI and human staff are managed through the same API.
 */
@RestController
@RequestMapping("/api/staff")
public class StaffController {

    private final StaffRepository staff;

    public StaffController(StaffRepository staff) { this.staff = staff; }

    @GetMapping
    public List<Staff> list() { return staff.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<Staff> get(@PathVariable Long id) {
        return staff.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /** Onboard a new staff member (AI or human) with full configuration. */
    @PostMapping
    public Staff onboard(@RequestBody Staff body) {
        body.setId(null);
        resolveRefs(body);
        return staff.save(body);
    }

    /** Update / reconfigure an existing staff member. */
    @PutMapping("/{id}")
    public ResponseEntity<Staff> update(@PathVariable Long id, @RequestBody Staff body) {
        return staff.findById(id).map(existing -> {
            body.setId(id);
            resolveRefs(body);
            return ResponseEntity.ok(staff.save(body));
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Replace detached association stubs (carrying only an id from the client)
     * with managed entities, so JPA persists the relationships correctly.
     * Guards against self-reporting.
     */
    private void resolveRefs(Staff body) {
        if (body.getReportsTo() != null && body.getReportsTo().getId() != null) {
            Long mgrId = body.getReportsTo().getId();
            if (mgrId.equals(body.getId())) {
                body.setReportsTo(null); // can't report to self
            } else {
                body.setReportsTo(staff.findById(mgrId).orElse(null));
            }
        }
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<Staff> putOnLeave(@PathVariable Long id) {
        return setStatus(id, StaffStatus.ON_LEAVE);
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<Staff> activate(@PathVariable Long id) {
        return setStatus(id, StaffStatus.ACTIVE);
    }

    @PostMapping("/{id}/offboard")
    public ResponseEntity<Staff> offboard(@PathVariable Long id) {
        return setStatus(id, StaffStatus.OFFBOARDED);
    }

    /** Permanently remove a staff member. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable Long id) {
        if (!staff.existsById(id)) return ResponseEntity.notFound().build();
        // clear any reporting lines pointing at this staff to avoid orphan references
        staff.findAll().forEach(other -> {
            if (other.getReportsTo() != null && id.equals(other.getReportsTo().getId())) {
                other.setReportsTo(null);
                staff.save(other);
            }
        });
        staff.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<Staff> setStatus(Long id, StaffStatus status) {
        return staff.findById(id).map(s -> {
            s.setStatus(status);
            return ResponseEntity.ok(staff.save(s));
        }).orElse(ResponseEntity.notFound().build());
    }
}
