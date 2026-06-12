package com.knatware.aiworkforce.controller;

import com.knatware.aiworkforce.model.*;
import com.knatware.aiworkforce.repository.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** Admin configuration of the company, departments, and levels. */
@RestController
@RequestMapping("/api/company")
public class CompanyController {

    private final CompanyRepository companies;
    private final DepartmentRepository departments;
    private final LevelRepository levels;
    private final StaffRepository staff;

    public CompanyController(CompanyRepository companies, DepartmentRepository departments,
                             LevelRepository levels, StaffRepository staff) {
        this.companies = companies; this.departments = departments;
        this.levels = levels; this.staff = staff;
    }

    @GetMapping public List<Company> company() { return companies.findAll(); }

    @PutMapping("/{id}")
    public Company update(@PathVariable Long id, @RequestBody Company body) {
        body.setId(id);
        return companies.save(body);
    }

    @GetMapping("/departments") public List<Department> departments() { return departments.findAll(); }
    @PostMapping("/departments") public Department addDepartment(@RequestBody Department d) { d.setId(null); return departments.save(d); }

    /** Delete a department; any staff in it are unassigned (set to no department). */
    @DeleteMapping("/departments/{id}")
    public java.util.Map<String, Object> deleteDepartment(@PathVariable Long id) {
        int unassigned = 0;
        for (Staff s : staff.findAll()) {
            if (s.getDepartment() != null && id.equals(s.getDepartment().getId())) {
                s.setDepartment(null);
                staff.save(s);
                unassigned++;
            }
        }
        departments.deleteById(id);
        return java.util.Map.of("deleted", true, "staffUnassigned", unassigned);
    }

    @GetMapping("/levels") public List<Level> levels() { return levels.findAll(); }
    @PostMapping("/levels") public Level addLevel(@RequestBody Level l) { l.setId(null); return levels.save(l); }

    /** Delete a level; any staff on it are unassigned (set to no level). */
    @DeleteMapping("/levels/{id}")
    public java.util.Map<String, Object> deleteLevel(@PathVariable Long id) {
        int unassigned = 0;
        for (Staff s : staff.findAll()) {
            if (s.getLevel() != null && id.equals(s.getLevel().getId())) {
                s.setLevel(null);
                staff.save(s);
                unassigned++;
            }
        }
        levels.deleteById(id);
        return java.util.Map.of("deleted", true, "staffUnassigned", unassigned);
    }
}
