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

    public CompanyController(CompanyRepository companies, DepartmentRepository departments, LevelRepository levels) {
        this.companies = companies; this.departments = departments; this.levels = levels;
    }

    @GetMapping public List<Company> company() { return companies.findAll(); }

    @PutMapping("/{id}")
    public Company update(@PathVariable Long id, @RequestBody Company body) {
        body.setId(id);
        return companies.save(body);
    }

    @GetMapping("/departments") public List<Department> departments() { return departments.findAll(); }
    @PostMapping("/departments") public Department addDepartment(@RequestBody Department d) { d.setId(null); return departments.save(d); }

    @GetMapping("/levels") public List<Level> levels() { return levels.findAll(); }
    @PostMapping("/levels") public Level addLevel(@RequestBody Level l) { l.setId(null); return levels.save(l); }
}
